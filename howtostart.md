# Hướng dẫn khởi chạy - xxxx Microservices

Tài liệu này phản ánh cấu hình hiện tại của repo: Maven multi-module, Docker Compose profiles,
Config Server đọc `environment/config-repo`, và Gateway là cổng public chính.

## 1. Yêu cầu

- Java 21
- Maven 3.9+
- Docker + Docker Compose v2 (`docker compose`)
- RAM Docker khuyến nghị 8GB+ nếu chạy full business + observability

## 2. Chuẩn bị biến môi trường

Tạo file `.env`:

```bash
cp .env.example .env
```

Cập nhật tối thiểu các biến sau trước khi chạy Docker Compose:

```text
MYSQL_ROOT_PASSWORD=...
JWT_SECRET=...                    # tối thiểu 32 ký tự
ENCRYPT_KEY=...
VNPAY_TMN_CODE=...
VNPAY_SECRET_KEY=...
VNPAY_RETURN_URL=http://localhost:8080/api/payment/vnpay-return
GRAFANA_ADMIN_PASSWORD=...        # cần khi bật profile observability
```

## 3. Build project

Build toàn bộ module từ thư mục gốc:

```bash
mvn clean package -DskipTests
```

Nếu chỉ cần cài parent/common cho phát triển local:

```bash
mvn -N install -DskipTests
mvn -pl xxxx-common install -DskipTests
```

## 4. Chạy bằng Docker Compose

### 4.1 Chạy hệ thống chính

```bash
docker compose --profile infra --profile platform --profile business up -d --build
```

Các profile đang có:

| Profile | Thành phần |
|---------|------------|
| `infra` | MySQL, Redis, Zookeeper, Kafka |
| `platform` | Discovery, Config Server, Gateway |
| `business` | Booking, Order, Payment, Ticket, Inventory, User, Event |
| `observability` | Prometheus, Grafana, Elasticsearch, Logstash, Kibana, Zipkin |

### 4.2 Bật observability khi cần

```bash
docker compose --profile observability up -d
```

Nếu muốn chạy tất cả cùng lúc:

```bash
docker compose --profile infra --profile platform --profile business --profile observability up -d --build
```

### 4.3 Kiểm tra trạng thái

```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

Gateway là cổng duy nhất được publish ra host trong compose hiện tại:

| Thành phần | URL từ máy host |
|------------|-----------------|
| API Gateway | `http://localhost:8080` |
| Swagger UI Gateway | `http://localhost:8080/swagger-ui.html` |
| Actuator Gateway | `http://localhost:8080/actuator/health` |

Discovery, Config Server, MySQL, Redis, Kafka và các business service hiện chỉ dùng trong Docker network `xxxx-network`.

## 5. Chạy local khi phát triển

### 5.1 Điểm cần chú ý về port

Các file `environment/config-repo/*-dev.yml` đang cấu hình service chạy trên host với port:

| Thành phần | Port dev trong config-repo |
|------------|----------------------------|
| MySQL | `localhost:3316` |
| Redis | `localhost:6319` |
| Kafka | `localhost:9094` |
| Discovery | `localhost:8761` |
| Config Server | `localhost:8888` |
| Gateway | `localhost:8080` |

Trong khi đó Docker Compose hiện tại **không publish** MySQL/Redis/Kafka ra các port `3316/6319/9094`.
Vì vậy nếu muốn chạy service bằng Maven trên host và dùng hạ tầng Docker, cần một trong hai cách:

- thêm compose override để publish `mysql:3306 -> 3316`, `redis:6379 -> 6319`, `kafka:9092 -> 9094`; hoặc
- sửa config dev tạm thời về port được expose trong môi trường của bạn.

### 5.2 Thứ tự chạy Maven

```bash
# 1. Chạy hạ tầng hoặc đảm bảo MySQL/Redis/Kafka đã truy cập được theo config dev
# docker compose --profile infra up -d   # chỉ đủ nếu bạn đã expose port bằng override

# 2. Discovery
cd xxxx-discovery
mvn spring-boot:run

# 3. Config Server
cd ../xxxx-config
mvn spring-boot:run

# 4. Gateway
cd ../xxxx-gateway
mvn spring-boot:run

# 5. Service nghiệp vụ cần debug
cd ../xxxx-order-service
mvn spring-boot:run
```

Các business service đều import config từ:

```text
optional:configserver:${SPRING_CLOUD_CONFIG_URI:http://localhost:8888}
```

## 6. API thường dùng

Tất cả gọi qua Gateway `http://localhost:8080`:

| Nghiệp vụ | Endpoint |
|----------|----------|
| Login | `POST /api/users/login` |
| Refresh token | `POST /api/users/refresh` |
| Đăng ký | `POST /api/users/register` |
| Thông tin user hiện tại | `GET /api/users/me` |
| Đặt vé | `POST /api/orders/place` |
| Theo dõi checkout | `GET /api/orders/{orderNo}/checkout` |
| Trạng thái order | `GET /api/orders/status/{orderNo}` |
| Khởi tạo thanh toán | `POST /api/payment/initiate` |
| VnPay IPN callback | `POST /api/payment/vnpay-callback` |
| VnPay return | `GET /api/payment/vnpay-return` |
| Tồn kho ticket detail | `GET /api/inventory/stock/{ticketDetailId}` |
| Nạp tồn kho | `POST /api/inventory/stock/initialize` |

`POST /api/orders/place` hỗ trợ header `Idempotency-Key` để retry/double-click không tạo trùng order.
Frontend nên polling `GET /api/orders/{orderNo}/checkout` để lấy `queuePosition`, `paymentUrl`, `expiresAt` và `failureReason`.

## 7. Dừng hệ thống

```bash
docker compose down
```

Xóa luôn volume dữ liệu:

```bash
docker compose down -v
```

## 8. Troubleshooting

### Compose báo thiếu biến môi trường

Kiểm tra `.env` đã tồn tại và có đủ các biến bắt buộc. Docker Compose đang dùng cú pháp `${VAR:?message}` nên thiếu biến sẽ fail ngay khi parse compose.

### Gateway không lên

```bash
docker compose logs gateway
```

Kiểm tra `JWT_SECRET`, Redis health và Config Server health.

### Config Server không lên

```bash
docker compose logs config
```

Kiểm tra `ENCRYPT_KEY`, Discovery health và mount `./environment/config-repo:/app/config-repo:ro`.

### Service không đăng ký Eureka

Trong Docker, các service dùng:

```text
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery:8761/eureka/
```

Trong local host, default là:

```text
http://localhost:8761/eureka/
```

### Service local không kết nối được DB/Redis/Kafka

Nguyên nhân phổ biến là compose không publish port ra host. So sánh lại config dev:

- MySQL: `localhost:3316`
- Redis: `localhost:6319`
- Kafka: `localhost:9094`

với port thực tế đang expose bằng `docker compose ps`.

### VnPay callback/return không đúng

- Local browser có thể dùng `VNPAY_RETURN_URL=http://localhost:8080/api/payment/vnpay-return`.
- Production/VPS phải dùng domain public thật có thể truy cập từ VnPay.
- Nếu chữ ký callback sai, kiểm tra `VNPAY_SECRET_KEY` và việc truyền đủ tham số `vnp_*`.

### Kafka publish bị kẹt

Order, inventory và payment đang dùng outbox nội bộ. Nếu Kafka lỗi kéo dài, record có thể chuyển sang `RETRY` hoặc `FAILED` trong bảng outbox của service tương ứng. Cần kiểm tra log publisher và DB outbox.

### Hết RAM

- Chạy trước `infra + platform`, chỉ bật business service cần test.
- Tắt `observability` nếu không cần.
- Tăng RAM Docker lên 8GB+.
