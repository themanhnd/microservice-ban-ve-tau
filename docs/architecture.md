# Kiến trúc tổng quan - xxxx Microservices

Tài liệu này mô tả kiến trúc tổng thể của hệ thống bán vé (ticketing) xây dựng theo
mô hình microservices với Spring Boot 3.3.5 / Spring Cloud 2023.0.3.

> Các sơ đồ dùng định dạng [Mermaid](https://mermaid.js.org/). Xem trực tiếp trong
> trình xem Markdown hỗ trợ Mermaid.

## 1. Kiến trúc tổng thể

```mermaid
flowchart TB
    Client([Client / Browser])

    subgraph EDGE[Tang Edge]
        GW["xxxx-gateway<br/>:8080<br/>routing + JWT auth"]
    end

    subgraph PLATFORM[Tang nen tang]
        DISC["xxxx-discovery<br/>Eureka :8761"]
        CFG["xxxx-config<br/>Config Server :8888"]
    end

    subgraph BUSINESS[Cac service nghiep vu]
        USER["user-service :8086"]
        EVENT["event-service :8087"]
        TICKET["ticket-service :8084"]
        INV["inventory-service :8085"]
        BOOK["booking-service :8081"]
        ORDER["order-service :8082"]
        PAY["payment-service :8083"]
    end

    subgraph INFRA[Ha tang du lieu]
        MYSQL[("MySQL :3316<br/>7 databases")]
        REDIS[("Redis :6319")]
        KAFKA{{"Kafka :9094"}}
    end

    Client --> GW
    GW -->|lb://| USER & EVENT & TICKET & INV & BOOK & ORDER & PAY

    BUSINESS -.dang ky.-> DISC
    GW -.dang ky.-> DISC
    CFG -.dang ky.-> DISC
    BUSINESS -.lay config.-> CFG

    BUSINESS --> MYSQL
    BOOK & ORDER & TICKET & INV & USER & EVENT --> REDIS
    GW --> REDIS
    ORDER & PAY & INV & BOOK <--> KAFKA
```

## 2. Giao tiep giua cac service

Hai kieu giao tiep: dong bo qua Feign (REST + load-balance qua Eureka) va bat dong bo qua Kafka.

```mermaid
flowchart LR
    subgraph SYNC[Dong bo - Feign REST]
        ORDER1[order-service] -->|PaymentServiceClient| PAY1[payment-service]
        BOOK1[booking-service] -->|TicketServiceClient| TICKET1[ticket-service]
        BOOK1 -->|EventServiceClient| EVENT1[event-service]
    end
```

Moi Feign client deu co fallback (Resilience4j circuit breaker) - neu service dich chet
thi co hanh vi du phong thay vi sap theo.

## 3. Luong Saga dat ve (qua Kafka)

Saga dieu phoi boi order-service, co bu tru (compensation) khi that bai.

```mermaid
sequenceDiagram
    participant O as order-service
    participant K as Kafka
    participant I as inventory-service
    participant P as payment-service
    participant B as booking-service

    O->>K: order.placed
    K->>I: order.placed
    alt Con hang
        I->>K: inventory.reserved
        K->>O: inventory.reserved
        Note over P: thanh toan xu ly
        P->>K: payment.completed
        K->>O: payment.completed
        O->>K: order.confirmed
        K->>B: order.confirmed (tao booking)
    else Het hang
        I->>K: inventory.reserve-failed
        K->>O: inventory.reserve-failed
        O->>O: huy don
    end
    alt Thanh toan loi
        P->>K: payment.failed
        K->>O: payment.failed
        O->>K: order.cancelled (bu tru: release inventory)
    end
```

### Danh sach Kafka topic

| Topic | Producer | Consumer |
|-------|----------|----------|
| `order.placed` | order-service | inventory-service |
| `inventory.reserved` | inventory-service | order-service |
| `inventory.reserve-failed` | inventory-service | order-service |
| `payment.completed` | payment-service | order-service |
| `payment.failed` | payment-service | order-service |
| `order.confirmed` | order-service | booking-service |
| `order.cancelled` | order-service | (bu tru inventory) |

## 4. Thu tu khoi dong & phu thuoc

```mermaid
flowchart LR
    A["1. Ha tang<br/>MySQL, Redis, Kafka"] --> B["2. discovery<br/>:8761"]
    B --> C["3. config<br/>:8888"]
    C --> D["4. gateway<br/>:8080"]
    C --> E["5. 7 service nghiep vu"]
    D -.can.-> B
    E -.can.-> B
```

## 5. Vai tro cac thanh phan

| Nhom | Service | Port | Vai tro |
|------|---------|------|---------|
| Edge | xxxx-gateway | 8080 | Cua ngo duy nhat, routing, xac thuc JWT |
| Nen tang | xxxx-discovery | 8761 | Service registry (Eureka) |
| Nen tang | xxxx-config | 8888 | Cau hinh tap trung (Config Server) |
| Nghiep vu | xxxx-user-service | 8086 | Nguoi dung, dang nhap, nhan vien |
| Nghiep vu | xxxx-event-service | 8087 | Su kien |
| Nghiep vu | xxxx-ticket-service | 8084 | Loai ve, chi tiet ve |
| Nghiep vu | xxxx-inventory-service | 8085 | Ton kho ve (reserve/release) |
| Nghiep vu | xxxx-booking-service | 8081 | Dat cho |
| Nghiep vu | xxxx-order-service | 8082 | Dieu phoi saga dat hang |
| Nghiep vu | xxxx-payment-service | 8083 | Thanh toan (VNPay) |
| Ha tang | MySQL / Redis / Kafka | 3316 / 6319 / 9094 | Luu tru / cache / message bus |
| Quan sat | Prometheus, Grafana, ELK, Zipkin | - | Metrics, log, tracing |

## 6. Cau hinh tap trung (Config Server)

Cac service lay cau hinh tu Config Server luc khoi dong (`spring.config.import`).
Nguon cau hinh duy nhat nam o `environment/config-repo/`:

- `application.yml` - cau hinh chung cho moi service (Eureka, actuator, resilience4j...)
- `xxxx-<ten-service>-dev.yml` - cau hinh rieng tung service (port, datasource, kafka...)

Config Server doc thu muc nay qua `search-locations`, va co the override bang bien moi
truong `CONFIG_SEARCH_LOCATIONS` khi deploy len VPS.

## 7. So do ERD (database-per-service)

Moi service so huu mot database rieng (database-per-service). KHONG co khoa ngoai vat ly
xuyen service - cac lien ket giua database khac nhau chi la lien ket logic qua ID
(ve duong gach `..>`). Khoa ngoai vat ly chi ton tai trong cung mot database.

```mermaid
erDiagram
    %% ===== user_db =====
    USERS {
        Long id PK
        String username UK
        String email UK
        String password_hash
        String full_name
        String phone
        String status
    }
    EMPLOYEES {
        Long id PK
        String username
    }

    %% ===== event_db =====
    EVENTS {
        Long id PK
        String name
        String venue
        datetime start_date
        datetime end_date
        EventStatus status
        Integer capacity
        Boolean deleted
    }

    %% ===== ticket_db =====
    TICKET {
        Long id PK
        String name
        datetime start_time
        datetime end_time
        Integer status
    }
    TICKET_ITEM {
        Long id PK
        Long activity_id FK
        String name
        Integer stock_initial
        Integer stock_available
        BigDecimal price_original
        BigDecimal price_flash
        Integer status
    }

    %% ===== inventory_db =====
    INVENTORY_ALLOT_DETAIL {
        Long id PK
        String order_id
        Long ticket_detail_id
        String type
        String sku_id
        String inventor_no UK
        Integer inventor_num
        Long version_id
    }
    INVENTORY_BUCKET_CONFIG {
        Long id PK
    }

    %% ===== order_db =====
    TICKER_ORDER {
        Long id PK
        String order_no UK
        String user_id
        Long ticket_detail_id
        Integer quantity
        BigDecimal total_amount
        String status
        String saga_status
        String payment_transaction_id
        String correlation_id
        Long version_id
    }
    ORDER_QUEUE {
        Long id PK
    }
    ORDER_DEDUCTION {
        Long id PK
    }

    %% ===== payment_db =====
    PAYMENT_TRANSACTION {
        Long id PK
        String transaction_id UK
        String order_id
        String user_id
        BigDecimal amount
        String payment_method
        String status
        String idempotency_key UK
        Long version
    }

    %% ===== booking_db =====
    BOOKING {
        Long id PK
        String booking_no UK
        Long user_id
        Long ticket_id
        Long ticket_detail_id
        Long event_id
        Integer quantity
        BigDecimal total_amount
        BookingStatus status
        String order_no
    }

    %% Quan he trong cung database (khoa ngoai vat ly)
    TICKET ||--o{ TICKET_ITEM : "co nhieu"

    %% Lien ket logic xuyen service (chi qua ID, khong co FK vat ly)
    TICKER_ORDER }o..|| TICKET_ITEM : "ticket_detail_id"
    TICKER_ORDER ||..o| PAYMENT_TRANSACTION : "order_no / order_id"
    TICKER_ORDER ||..o{ INVENTORY_ALLOT_DETAIL : "order_id"
    BOOKING }o..|| TICKER_ORDER : "order_no"
    BOOKING }o..|| USERS : "user_id"
    BOOKING }o..|| TICKET : "ticket_id"
    BOOKING }o..|| EVENTS : "event_id"
```

### Banh xa databases

| Database | Service so huu | Bang chinh |
|----------|----------------|------------|
| `user_db` | user-service | users, employees |
| `event_db` | event-service | events |
| `ticket_db` | ticket-service | ticket, ticket_item |
| `inventory_db` | inventory-service | inventory_allot_detail, inventory_bucket_config |
| `order_db` | order-service | ticker_order, order_queue, order_deduction |
| `payment_db` | payment-service | payment_transaction |
| `booking_db` | booking-service | booking |

> Diem dang chu y: nhieu bang dung `@Version` (optimistic locking) va cac cot
> `inventor_no` / `idempotency_key` lam khoa idempotency - phuc vu xu ly concurrent
> va chong xu ly trung trong luong saga.

## 8. So do Observability (giam sat)

He thong dung 3 tru cot quan sat: metrics (Prometheus + Grafana), logs (ELK), va
distributed tracing (Zipkin).

```mermaid
flowchart TB
    subgraph SERVICES[Cac service - Spring Boot Actuator + Micrometer]
        SVC["gateway + config + discovery<br/>+ 7 service nghiep vu"]
    end

    subgraph METRICS[Metrics]
        PROM["Prometheus :9090<br/>scrape /actuator/prometheus moi 10s"]
        GRAF["Grafana :3000<br/>(admin/admin123)"]
    end

    subgraph LOGS[Logs - ELK]
        LS["Logstash :5044<br/>(tcp json_lines)"]
        ES[("Elasticsearch :9200")]
        KB["Kibana :5601"]
    end

    subgraph TRACING[Tracing]
        ZIP["Zipkin :9411"]
    end

    SVC -->|pull metrics| PROM
    PROM --> GRAF

    SVC -->|push logs<br/>traceId/spanId| LS
    LS --> ES
    ES --> KB
    ZIP -->|luu trace| ES

    SVC -->|push spans<br/>/api/v2/spans| ZIP
```

### Co che thu thap

| Tru cot | Cong cu | Co che | Ghi chu |
|---------|---------|--------|---------|
| Metrics | Prometheus -> Grafana | PULL: scrape `/actuator/prometheus` moi 10s | Datasource Grafana tro toi `prometheus:9090` |
| Logs | Logstash -> Elasticsearch -> Kibana | PUSH: log gui qua TCP 5044 dang json_lines | Index theo `xxxx-logs-<service>-<ngay>` |
| Tracing | Zipkin (luu vao Elasticsearch) | PUSH: span gui toi `/api/v2/spans` | Tuong quan qua `traceId` / `spanId` trong MDC |

> Log va trace duoc lien ket qua `traceId`/`spanId` (Logstash trich tu MDC), cho phep
> nhay tu mot dong log sang trace tuong ung de debug xuyen service.

## 9. Luong thanh toan VNPay

VNPay tach lam 2 kenh tra ket qua doc lap:
- **IPN callback** (`POST /api/payment/vnpay-callback`): VNPay goi server-to-server. Day la
  kenh DUY NHAT cap nhat trang thai va publish event Kafka (payment.completed/failed).
- **Return URL** (`GET /api/payment/vnpay-return`): trinh duyet nguoi dung redirect ve sau
  khi thanh toan. CHI doc va hien thi trang thai, KHONG cap nhat DB, KHONG publish event.

Tach 2 kenh la dung chuan: trang thai tien luon dua tren IPN (server-to-server, dang tin
cay), khong phu thuoc nguoi dung co bam quay lai hay khong.

```mermaid
sequenceDiagram
    actor U as User
    participant O as order-service
    participant P as payment-service
    participant V as VNPay Gateway
    participant K as Kafka

    Note over O,P: 1. Khoi tao thanh toan (Feign, dong bo)
    O->>P: POST /api/payment/initiate (orderId, amount)
    P->>P: idempotency check theo orderId
    P->>P: tao transaction PENDING -> PROCESSING
    P->>P: VnPayService tao URL + ky HMAC-SHA512
    P-->>O: paymentUrl + transactionId

    Note over U,V: 2. Nguoi dung thanh toan
    U->>V: mo paymentUrl, nhap thong tin
    V->>V: xu ly giao dich

    Note over V,K: 3. IPN callback (server-to-server) - kenh quyet dinh
    V->>P: POST /api/payment/vnpay-callback (vnp_SecureHash...)
    P->>P: validateSignature (HMAC-SHA512)
    alt Chu ky sai
        P-->>V: INVALID_SIGNATURE
    else responseCode = 00 (thanh cong)
        P->>P: status = COMPLETED
        P->>K: publish payment.completed
        P-->>V: SUCCESS
    else responseCode != 00 (that bai)
        P->>P: status = FAILED
        P->>K: publish payment.failed
        P-->>V: FAILED
    end

    Note over U,P: 4. Return URL (trinh duyet) - chi hien thi
    V->>U: redirect ve vnp_ReturnUrl
    U->>P: GET /api/payment/vnpay-return
    P-->>U: trang thai giao dich (chi doc)
```

Sau buoc 3, event `payment.completed` / `payment.failed` di vao saga o muc 3 (order-service
tieu thu de xac nhan hoac huy don).

### Diem can xu ly truoc khi len VPS

| Van de | Vi tri | Anh huong |
|--------|--------|-----------|
| `secret-key` co gia tri mac dinh hardcode | `VnPayService` | Bao mat - phai dua ra bien moi truong / secret |
| `vnp_IpAddr` co dinh `127.0.0.1` | `VnPayService.createPaymentUrl` | VNPay co the tu choi/sai IP that |
| `return-url` mac dinh `127.0.0.1:8080`, config-repo lai ghi `localhost:3000/payment/return` | `VnPayService` vs `xxxx-payment-service-dev.yml` | Khong khop - tren VPS phai la domain public that |
| callback URL phai PUBLIC de VNPay goi vao | gateway `public-endpoints` | Tren VPS can mo `/api/payment/vnpay-callback` ra internet (da khai bao public, bo qua JWT) |
| `findTransactionByTxnRef` dung `findAll().stream()` | `PaymentServiceImpl` | Quet toan bang - cham khi du lieu lon, nen luu `txnRef` thanh cot rieng co index |

> `vnpay.return-url` va `vnp_ReturnUrl` tro toi gateway (`/api/payment/vnpay-return`) - dung,
> vi gateway la cua ngo public duy nhat. Tren VPS thay `127.0.0.1`/`localhost` bang domain that.
