# 🎫 Microservice Bán Vé Tàu (xxxx Ticketing System)

Hệ thống bán vé xây dựng theo kiến trúc **microservices** với Spring Boot 3 / Spring Cloud,
thiết kế để xử lý các bài toán đặc thù của bán vé tải cao (flash sale): chống bán quá vé
(oversell), tranh chấp tồn kho, idempotency, và giao dịch phân tán qua Saga.

> Java 21 · Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · Kafka · MySQL · Redis · Docker

---

## 📑 Mục lục

- [Tổng quan kiến trúc](#-tổng-quan-kiến-trúc)
- [Các service & cổng](#-các-service--cổng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Luồng nghiệp vụ chính (Saga)](#-luồng-nghiệp-vụ-chính-saga)
- [Yêu cầu môi trường](#-yêu-cầu-môi-trường)
- [Khởi chạy nhanh (Docker)](#-khởi-chạy-nhanh-docker)
- [Chạy khi phát triển (local)](#-chạy-khi-phát-triển-local)
- [Các điểm truy cập](#-các-điểm-truy-cập)
- [Tài liệu chi tiết](#-tài-liệu-chi-tiết)
- [Ghi chú bảo mật](#-ghi-chú-bảo-mật)

---

## 🏗 Tổng quan kiến trúc

```
                       ┌─────────────┐
   Client ───────────▶ │   Gateway   │ :8080  (routing + JWT + rate limit)
                       └──────┬──────┘
                              │ lb:// (qua Eureka)
        ┌──────────────┬──────┴───────┬──────────────┐
        ▼              ▼              ▼              ▼
   user/event     ticket/inventory   order        payment / booking
        │              │              │              │
        └──── MySQL (mỗi service 1 DB) · Redis (cache/lock) · Kafka (event) ─────┘

   Nền tảng:  Discovery (Eureka :8761)  ·  Config Server (:8888)
   Quan sát:  Prometheus · Grafana · ELK · Zipkin
```

Nguyên tắc thiết kế:
- **Database-per-service** — mỗi service sở hữu database riêng, không chia sẻ schema.
- **Cấu hình tập trung** — mọi cấu hình nằm ở `environment/config-repo`, phục vụ qua Config Server.
- **Giao tiếp 2 kiểu** — đồng bộ (OpenFeign) cho việc nhanh, bất đồng bộ (Kafka) cho luồng dài.
- **Khả chịu lỗi** — Resilience4j (circuit breaker, retry, bulkhead) bọc mọi lời gọi liên service.

## 🧩 Các service & cổng

| Service | Cổng | Vai trò |
|---------|------|---------|
| `xxxx-discovery` | 8761 | Service registry (Eureka) — danh bạ service |
| `xxxx-config` | 8888 | Config Server — cấu hình tập trung |
| `xxxx-gateway` | 8080 | API Gateway — cổng vào duy nhất, JWT, rate limit |
| `xxxx-user-service` | 8086 | Người dùng, đăng nhập, nhân viên |
| `xxxx-event-service` | 8087 | Sự kiện |
| `xxxx-ticket-service` | 8084 | Loại vé & chi tiết vé |
| `xxxx-inventory-service` | 8085 | Tồn kho vé (reserve/release, distributed lock) |
| `xxxx-order-service` | 8082 | Điều phối Saga đặt hàng |
| `xxxx-payment-service` | 8083 | Thanh toán (VNPay) |
| `xxxx-booking-service` | 8081 | Đặt chỗ |
| `xxxx-common` | — | Thư viện dùng chung (event, hằng số, response chuẩn) |

## 🛠 Công nghệ sử dụng

| Nhóm | Công nghệ | Mục đích |
|------|-----------|----------|
| Nền tảng | Spring Boot 3.3.5, Java 21 | Khung ứng dụng cho từng service |
| Hạ tầng MS | Eureka, Spring Cloud Config, Spring Cloud Gateway | Discovery, config, routing |
| Giao tiếp | OpenFeign, Apache Kafka | Đồng bộ (REST) + bất đồng bộ (event/Saga) |
| Độ bền | Resilience4j | Circuit breaker, retry, bulkhead, fallback |
| Dữ liệu | MySQL 8, JPA/Hibernate | Lưu trữ quan hệ, ORM |
| Hiệu năng | Redis | Cache, đếm tồn kho, distributed lock, rate limit |
| Bảo mật | JWT (jjwt) | Xác thực stateless tại gateway |
| Quan sát | Prometheus, Grafana, ELK, Zipkin | Metrics, log tập trung, tracing |
| Vận hành | Docker, Docker Compose, Maven | Đóng gói & build |

> Giải thích cặn kẽ "vì sao dùng" và lý thuyết từng công nghệ: xem [`docs/cong-nghe-giai-thich.md`](docs/cong-nghe-giai-thich.md).

## 🔄 Luồng nghiệp vụ chính (Saga)

Đặt vé là một **Saga** điều phối bởi `order-service`, nối các service qua sự kiện Kafka, có
bù trừ (compensation) khi thất bại:

```
order.placed ─▶ inventory giữ vé ─▶ inventory.reserved
            ─▶ payment xử lý ─▶ payment.completed
            ─▶ order.confirmed ─▶ booking tạo chỗ
```

- Hết vé → `inventory.reserve-failed` → hủy đơn.
- Thanh toán lỗi → `payment.failed` → `order.cancelled` (kèm trả vé về kho).

Chi tiết bài toán tải cao & cách xử lý (oversell, idempotency, distributed lock, bucket,
waiting room): xem [`docs/cong-nghe-giai-thich.md`](docs/cong-nghe-giai-thich.md) — Phần II.

## ✅ Yêu cầu môi trường

- Java 21 (Eclipse Temurin khuyến nghị)
- Maven 3.9+
- Docker & Docker Compose
- RAM cấp cho Docker: tối thiểu 8GB (chạy đầy đủ cả observability)

## 🚀 Khởi chạy nhanh (Docker)

```bash
# 1. Build toàn bộ
mvn clean package -DskipTests

# 2. Dựng cả hệ thống
docker-compose up -d

# 3. Kiểm tra
docker-compose ps
```

Thứ tự khởi động được Docker Compose tự xử lý qua `depends_on` + healthcheck:
hạ tầng (MySQL/Redis/Kafka) → discovery → config → gateway → các service nghiệp vụ.

Dừng hệ thống:
```bash
docker-compose down        # giữ dữ liệu
docker-compose down -v     # xóa luôn volume (mất dữ liệu)
```

## 💻 Chạy khi phát triển (local)

Chỉ chạy hạ tầng bằng Docker, còn service đang sửa thì chạy bằng Maven cho nhanh:

```bash
# 1. Hạ tầng
docker-compose up -d mysql redis zookeeper kafka

# 2. Theo thứ tự: discovery → config → gateway → service nghiệp vụ
cd xxxx-discovery && mvn spring-boot:run
cd xxxx-config    && mvn spring-boot:run
cd xxxx-gateway   && mvn spring-boot:run
cd xxxx-order-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Chạy test một module:
```bash
mvn -pl xxxx-inventory-service test
```

## 🌐 Các điểm truy cập

| Thành phần | URL |
|------------|-----|
| API Gateway | http://localhost:8080 |
| Swagger UI (gom mọi service) | http://localhost:8080/swagger-ui.html |
| Eureka Dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin123) |
| Kibana | http://localhost:5601 |
| Zipkin | http://localhost:9411 |
| MySQL | localhost:3316 (root/root123) |
| Redis | localhost:6319 |
| Kafka (external) | localhost:9094 |

## 📚 Tài liệu chi tiết

| Tài liệu | Nội dung |
|----------|----------|
| [`docs/architecture.md`](docs/architecture.md) | Sơ đồ kiến trúc, giao tiếp, ERD, observability, luồng VNPay |
| [`docs/cong-nghe-giai-thich.md`](docs/cong-nghe-giai-thich.md) | Giải thích công nghệ (kiểu thầy giảng) + bài toán bán vé tải cao |
| [`howtostart.md`](howtostart.md) | Hướng dẫn khởi động & troubleshooting |

## 🔐 Ghi chú bảo mật

> ⚠️ Dự án đang chứa một số giá trị nhạy cảm **hardcode** phục vụ môi trường dev:
> mật khẩu MySQL (`root123`), Grafana (`admin123`), JWT secret mặc định,
> `encrypt.key` của Config Server, và `secret-key` VNPay.
>
> **Trước khi triển khai lên VPS/production**, cần:
> - Tách các secret này ra biến môi trường / secret manager.
> - Chỉ expose cổng gateway (8080) ra internet, các service khác để trong mạng nội bộ.
> - Đổi `vnpay.return-url` và callback URL sang domain public thật.

---

## 📂 Cấu trúc thư mục

```
xxxx-microservices/
├── docker-compose.yml          # Dựng toàn bộ hệ thống
├── pom.xml                     # Maven parent (multi-module)
├── environment/                # Cấu hình hạ tầng + config-repo
│   ├── config-repo/            # ⭐ Nguồn cấu hình tập trung của Config Server
│   ├── prometheus/ grafana/ logstash/ init-db/
├── docs/                       # Tài liệu kiến trúc & công nghệ
├── xxxx-common/                # Thư viện dùng chung
├── xxxx-discovery/ xxxx-config/ xxxx-gateway/    # Nền tảng
└── xxxx-{user,event,ticket,inventory,order,payment,booking}-service/
```
