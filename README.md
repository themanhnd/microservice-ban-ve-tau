# 🎫 Microservice Bán Vé Tàu (xxxx Ticketing System)

Hệ thống bán vé tàu theo kiến trúc **microservices** với Spring Boot 3, Spring Cloud,
Kafka, Redis và MySQL. Code hiện tại tập trung vào bài toán bán vé tải cao: chống bán quá vé,
giữ/hoàn tồn kho, idempotency khi checkout, Saga qua Kafka, outbox retry và thanh toán VnPay.

> Java 21 · Spring Boot 3.3.5 · Spring Cloud 2023.0.3 · Kafka · MySQL · Redis · Docker Compose

---

## 📑 Mục lục

- [Tổng quan kiến trúc](#-tổng-quan-kiến-trúc)
- [Module và vai trò](#-module-và-vai-trò)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Luồng đặt vé hiện tại](#-luồng-đặt-vé-hiện-tại)
- [API chính qua Gateway](#-api-chính-qua-gateway)
- [Yêu cầu môi trường](#-yêu-cầu-môi-trường)
- [Khởi chạy bằng Docker Compose](#-khởi-chạy-bằng-docker-compose)
- [Chạy local khi phát triển](#-chạy-local-khi-phát-triển)
- [Biến môi trường quan trọng](#-biến-môi-trường-quan-trọng)
- [Tài liệu chi tiết](#-tài-liệu-chi-tiết)
- [Ghi chú triển khai](#-ghi-chú-triển-khai)

---

## 🏗 Tổng quan kiến trúc

```text
Client / Frontend
      │
      ▼
xxxx-gateway :8080
(routing, JWT, correlation-id, Redis rate limit)
      │ lb:// qua Eureka
      ├── user-service      :8086  → user_db
      ├── event-service     :8087  → event_db + Redis cache
      ├── ticket-service    :8084  → ticket_db + Redis cache
      ├── inventory-service :8085  → inventory_db + Redis + Kafka
      ├── order-service     :8082  → order_db + Redis + Kafka + outbox
      ├── payment-service   :8083  → payment_db + Kafka + VnPay + outbox
      └── booking-service   :8081  → booking_db + Redis + Kafka

Nền tảng: xxxx-discovery (Eureka :8761), xxxx-config (Config Server :8888)
Hạ tầng: MySQL, Redis, Zookeeper/Kafka
Quan sát tùy chọn: Prometheus, Grafana, Elasticsearch, Logstash, Kibana, Zipkin
```

Nguyên tắc chính:

- **Database-per-service**: mỗi service sở hữu database riêng, khởi tạo trong `environment/init-db/init.sql`.
- **Config tập trung**: business service lấy cấu hình từ `environment/config-repo/*-dev.yml` qua Config Server.
- **Gateway là cổng public duy nhất trong Docker**: compose chỉ publish `8080:8080`; service/infra còn lại nằm trong network `xxxx-network`.
- **Saga bất đồng bộ**: `order-service`, `inventory-service`, `payment-service`, `booking-service` phối hợp qua Kafka event.
- **Outbox cho publish Kafka**: order/inventory/payment ghi event vào bảng outbox trong transaction DB, worker riêng publish và retry.

## 🧩 Module và vai trò

| Module | Port dev | Vai trò |
|--------|----------|---------|
| `xxxx-common` | — | Thư viện chung: response chuẩn, exception, Kafka topic, event DTO, header/correlation/idempotency util |
| `xxxx-discovery` | 8761 | Eureka Service Registry |
| `xxxx-config` | 8888 | Spring Cloud Config Server, đọc `environment/config-repo` khi chạy Docker |
| `xxxx-gateway` | 8080 | Gateway routing, JWT filter, correlation-id, Redis rate limiting, Swagger aggregation |
| `xxxx-user-service` | 8086 | Đăng ký/đăng nhập/refresh/logout, thông tin user, employee, bootstrap admin, login rate limit |
| `xxxx-event-service` | 8087 | CRUD sự kiện, cache Redis, warmup cache cho event sắp diễn ra |
| `xxxx-ticket-service` | 8084 | CRUD ticket và ticket detail, loại bỏ dữ liệu soft-delete khi đọc danh sách |
| `xxxx-inventory-service` | 8085 | Tính tồn kho từ DB, nạp Redis, reserve/release, bucket config, distributed lock, publish event tồn kho |
| `xxxx-order-service` | 8082 | Điều phối checkout Saga, hàng đợi xử lý, idempotency key, timeout thanh toán, outbox event |
| `xxxx-payment-service` | 8083 | Khởi tạo thanh toán VnPay, xác thực IPN/return, cập nhật transaction, publish payment event |
| `xxxx-booking-service` | 8081 | Tạo/xác nhận/hủy booking, consumer `order.confirmed` và `order.cancelled` |

## 🛠 Công nghệ sử dụng

| Nhóm | Công nghệ | Cách dùng trong code |
|------|-----------|----------------------|
| Nền tảng | Java 21, Spring Boot 3.3.5 | Parent Maven multi-module trong `pom.xml` |
| Spring Cloud | Eureka, Config, Gateway, OpenFeign | Discovery, config tập trung, route gateway, gọi đồng bộ giữa service |
| Messaging | Kafka + Zookeeper | Saga event: order, inventory, payment, booking |
| Độ tin cậy | Resilience4j, fallback, outbox retry | Circuit breaker/retry/bulkhead qua config; outbox DB cho publish Kafka an toàn hơn |
| Dữ liệu | MySQL 8, Spring Data JPA | Mỗi service một DB; dev đang dùng `ddl-auto: update` |
| Cache/lock | Redis | Cache dữ liệu đọc, tồn kho nhanh, distributed lock, rate limit |
| Bảo mật | JWT (JJWT) | User service cấp token, Gateway xác thực, downstream service vẫn có cấu hình JWT |
| API docs | springdoc-openapi | Swagger UI ở Gateway và route `/v3/api-docs/{service}` |
| Observability | Actuator, Prometheus, Grafana, ELK, Zipkin | Bật qua profile `observability`; logback có template gửi Logstash |
| Build/Run | Maven, Docker, Docker Compose profiles | Multi-module build và compose profile `infra/platform/business/observability` |

## 🔄 Luồng đặt vé hiện tại

Luồng checkout chính đi qua `POST /api/orders/place` và được `order-service` điều phối:

```text
Client đặt vé
  └─> order-service tạo order PROCESSING + queue token + idempotency key
      └─> ghi OrderPlacedEvent vào outbox
          └─> Kafka topic order.placed
              └─> inventory-service reserve tồn kho
                  ├─ thành công: InventoryReservedEvent → order-service gọi payment-service/initiate
                  │   └─ order lưu paymentUrl, chuyển PAYMENT_PROCESSING để client redirect VnPay
                  └─ thất bại: InventoryReserveFailedEvent → order CANCELLED

VnPay callback/return
  └─> payment-service xác thực chữ ký
      ├─ thành công: PaymentCompletedEvent → order CONFIRMED → OrderConfirmedEvent → booking CONFIRMED
      └─ thất bại: PaymentFailedEvent → order CANCELLED → OrderCancelledEvent → inventory release + booking cancel
```

Các cơ chế quan trọng:

- Header `Idempotency-Key` trên `POST /api/orders/place` giúp double-click/retry không tạo trùng đơn.
- `GET /api/orders/{orderNo}/checkout` trả trạng thái checkout, vị trí queue, `paymentUrl`, `expiresAt`, `failureReason` để frontend polling/redirect.
- Worker timeout trong order-service tự hủy đơn `PAYMENT_PROCESSING` quá hạn và phát `order.cancelled` để hoàn tồn kho.
- Consumer được guard theo trạng thái để chịu được Kafka at-least-once và duplicate event.

### Kafka topic đang dùng

| Topic | Producer | Consumer | Ý nghĩa |
|-------|----------|----------|---------|
| `order.placed` | order-service | inventory-service | Yêu cầu giữ tồn kho sau khi tạo order |
| `inventory.reserved` | inventory-service | order-service | Giữ tồn kho thành công, order bắt đầu tạo thanh toán |
| `inventory.reserve-failed` | inventory-service | order-service | Giữ tồn kho thất bại, order bị hủy |
| `payment.completed` | payment-service | order-service | Thanh toán thành công, order được xác nhận |
| `payment.failed` | payment-service | order-service | Thanh toán thất bại, order bị hủy và cần bù trừ |
| `order.confirmed` | order-service | booking-service | Tạo/xác nhận booking sau khi order thành công |
| `order.cancelled` | order-service | inventory-service, booking-service | Hoàn tồn kho đã giữ và hủy booking liên quan |

## 🌐 API chính qua Gateway

Gateway route các path sau tới service tương ứng:

| Path | Service |
|------|---------|
| `/api/users/**`, `/api/employees/**` | user-service |
| `/api/events/**` | event-service |
| `/api/tickets/**`, `/api/ticket-details/**` | ticket-service |
| `/api/inventory/**` | inventory-service |
| `/api/orders/**`, `/api/place-order/**` | order-service |
| `/api/payment/**` | payment-service |
| `/api/booking/**` | booking-service |
| `/v3/api-docs/{booking,order,payment,ticket,inventory,user,event}` | OpenAPI docs từng service |

Các endpoint đáng chú ý:

- `POST /api/users/login`, `POST /api/users/refresh`, `POST /api/users/register`, `GET /api/users/me`
- `POST /api/orders/place`, `GET /api/orders/{orderNo}/checkout`, `GET /api/orders/status/{orderNo}`
- `POST /api/payment/initiate`, `POST /api/payment/vnpay-callback`, `GET /api/payment/vnpay-return`, `GET /api/payment/{transactionId}`
- `GET /api/inventory/stock/{ticketDetailId}`, `POST /api/inventory/stock/initialize`, `POST /api/inventory/reserve`, `POST /api/inventory/release`

## ✅ Yêu cầu môi trường

- Java 21
- Maven 3.9+
- Docker và Docker Compose v2
- RAM Docker khuyến nghị: 8GB+ nếu bật full business + observability

## 🚀 Khởi chạy bằng Docker Compose

1. Tạo file `.env` từ mẫu:

```bash
cp .env.example .env
```

2. Sửa các secret bắt buộc trong `.env`:

```text
MYSQL_ROOT_PASSWORD
GRAFANA_ADMIN_PASSWORD
JWT_SECRET                 # tối thiểu 32 ký tự cho HMAC
ENCRYPT_KEY
VNPAY_TMN_CODE
VNPAY_SECRET_KEY
VNPAY_RETURN_URL           # local có thể dùng http://localhost:8080/api/payment/vnpay-return
```

3. Build toàn bộ module:

```bash
mvn clean package -DskipTests
```

4. Chạy hệ thống chính:

```bash
docker compose --profile infra --profile platform --profile business up -d
```

5. Nếu cần monitoring/log/tracing:

```bash
docker compose --profile observability up -d
```

6. Kiểm tra:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

> Lưu ý: compose hiện chỉ publish Gateway ra host ở `http://localhost:8080`. Discovery, Config, MySQL, Redis, Kafka và business service không expose port ra host trong file compose hiện tại.

## 💻 Chạy local khi phát triển

Có hai cách thường dùng:

### Cách 1: Chạy hạ tầng bằng Docker, service bằng Maven

```bash
# Chỉ chạy MySQL/Redis/Kafka/Zookeeper
docker compose --profile infra up -d

# Chạy nền tảng theo thứ tự
cd xxxx-discovery && mvn spring-boot:run
cd ../xxxx-config && mvn spring-boot:run
cd ../xxxx-gateway && mvn spring-boot:run

# Chạy service nghiệp vụ cần debug
cd ../xxxx-order-service && mvn spring-boot:run
```

Khi chạy service trực tiếp trên host, config-repo đang dùng các endpoint dev:

| Thành phần | Host/port dev |
|------------|---------------|
| MySQL | `localhost:3316` |
| Redis | `localhost:6319` |
| Kafka | `localhost:9094` |
| Eureka | `http://localhost:8761/eureka/` |
| Config Server | `http://localhost:8888` |

Nếu dùng compose hiện tại không publish các port này, cần tự chỉnh compose override hoặc chạy infra theo cách khác để expose `3316/6319/9094`.

### Cách 2: Chạy tất cả bằng Docker Compose

Dùng khi chỉ cần chạy hệ thống end-to-end và gọi qua Gateway:

```bash
docker compose --profile infra --profile platform --profile business up -d --build
```

## 🔑 Biến môi trường quan trọng

| Biến | Bắt buộc | Ý nghĩa |
|------|----------|---------|
| `MYSQL_ROOT_PASSWORD` | Có | Mật khẩu root MySQL, dùng cho các datasource trong Docker |
| `JWT_SECRET` | Có | Secret ký JWT, tối thiểu 32 ký tự |
| `JWT_ISSUER` | Không | Issuer JWT, mặc định `xxxx-user-service` |
| `JWT_EXPIRATION_SECONDS` | Không | Thời gian sống access token, mặc định 1800 giây |
| `JWT_REFRESH_EXPIRATION_SECONDS` | Không | Thời gian sống refresh token, mặc định 604800 giây |
| `ENCRYPT_KEY` | Có | Khóa mã hóa/giải mã của Config Server |
| `VNPAY_PAYMENT_URL` | Không | URL sandbox/production của VnPay |
| `VNPAY_TMN_CODE` | Có với payment | Mã terminal VnPay |
| `VNPAY_SECRET_KEY` | Có với payment | Secret key ký callback VnPay |
| `VNPAY_RETURN_URL` | Có với payment | URL người dùng quay lại sau khi thanh toán |
| `AUTH_BOOTSTRAP_ADMIN_*` | Không | Tạo tài khoản admin ban đầu nếu bật |
| `AUTH_RATE_LIMIT_*` | Không | Giới hạn số lần login theo cửa sổ thời gian |
| `GRAFANA_ADMIN_PASSWORD` | Có khi bật observability | Mật khẩu admin Grafana |

## 📚 Tài liệu chi tiết

| Tài liệu | Nội dung |
|----------|----------|
| [`howtostart.md`](howtostart.md) | Hướng dẫn chạy, profile Docker Compose, troubleshooting |
| [`docs/architecture.md`](docs/architecture.md) | Kiến trúc, luồng Saga, topic Kafka, startup/deploy notes |
| [`docs/cong-nghe-giai-thich.md`](docs/cong-nghe-giai-thich.md) | Giải thích công nghệ và lý do sử dụng |
| [`docs/ke-hoach-cong-viec.md`](docs/ke-hoach-cong-viec.md) | Trạng thái công việc và backlog kỹ thuật |
| [`docs/onboarding.md`](docs/onboarding.md) | Lộ trình đọc repo cho người mới và bản đồ luồng code |

## 🔐 Ghi chú triển khai

- Không commit `.env`; luôn tạo từ `.env.example` và đổi secret trước khi chạy.
- `VNPAY_RETURN_URL` khi chạy production phải là URL public thật, ví dụ `https://your-domain.example/api/payment/vnpay-return`.
- `ddl-auto: update` chỉ phù hợp dev; trước production cần Flyway/Liquibase migration cho các bảng outbox, idempotency, timeout, booking unique index.
- Cần vận hành outbox/DLQ nội bộ: theo dõi record `PENDING`, `RETRY`, `FAILED`, có cơ chế replay/ignore khi Kafka lỗi kéo dài.
- Nếu expose trực tiếp service/infra ngoài Docker network, cần bổ sung firewall, credential riêng và TLS phù hợp.
