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

### Docker Compose profiles

De may dev nhe hon va de tach ro surface khi deploy, `docker-compose.yml` da duoc chia theo 4 profile:

- `infra`: MySQL, Redis, Zookeeper, Kafka.
- `platform`: discovery, config, gateway.
- `business`: cac service nghiep vu.
- `observability`: Prometheus, Grafana, ELK, Zipkin.

Kieu tach nay giup local dev co the chi bat `infra` hoac `infra + platform`, trong khi VPS co the
bat `infra + platform + business`; observability chi bat khi can.

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

### Ghi chu moi cho inventory-service

Sau cap nhat flash sale A1, `inventory-service` khong con chi dung 1 key Redis `stock:available:{ticketDetailId}`
cho duong giu ve hot. Neu co bucket config mac dinh, service se chia ton kho thanh nhieu bucket:

- key ton kho: `stock:available:{ticketDetailId}:{bucketIndex}`
- key khoa: `lock:inventory:{ticketDetailId}:{bucketIndex}`

Bucket duoc chon theo hash `orderId`, giup cac request song song vao nhieu bucket khac nhau thay vi tranh
nhau tren 1 hot row duy nhat. Khi bucket dang xu ly sap can, service co co che `back-source` chuyen bot ton tu
bucket khac sang theo `thresholdValue`, `backSourceStep`, `minDepthNum` trong `InventoryBucketConfigEntity`.


### Ghi chu moi cho auth B5

Tu cap nhat B5, he thong dung mo hinh **Gateway authenticate, Service authorize**:

- `xxxx-gateway` la lop xac thuc dau vao: validate chu ky JWT, issuer va expiry cho endpoint private.
- Gateway xoa cac identity header do client tu gui (`X-User-Id`, `X-User-Email`, `X-User-Roles`) de chan gia mao quyen.
- Gateway forward nguyen `Authorization: Bearer <token>` xuong service; `X-User-*` chi con la metadata phu cho log/debug.
- Cac service nghiep vu tu verify JWT lai bang shared helper trong `xxxx-common`, tao principal noi bo va dung `@PreAuthorize` de quyet dinh quyen theo nghiep vu.
- Cac rule owner/admin nam trong service: user chi xem sua tai nguyen cua minh, `ADMIN` moi duoc thao tac quan tri.`r`n- Public endpoints vẫn đi qua public allow-list rõ ràng: `login/register/refresh`, VNPay callback/return và GET event listing.

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Gateway
    participant S as Business Service

    C->>G: Authorization: Bearer JWT
    G->>G: Validate signature/issuer/expiry
    G->>G: Strip spoofed X-User-* headers
    G->>S: Forward Authorization + verified metadata
    S->>S: Verify JWT again
    S->>S: @PreAuthorize owner/admin rule
    S-->>C: 200 / 401 / 403
```
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
        GRAF["Grafana :3000<br/>(admin password from env)"]
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

### Trang thai xu ly truoc khi len VPS

| Hang muc | Vi tri | Trang thai |
|--------|--------|------------|
| `secret-key` VNPay | `VnPayService`, config-repo | Lay tu `VNPAY_SECRET_KEY`, khong con default hardcode |
| `vnp_IpAddr` | `PaymentController` -> `VnPayService.createPaymentUrl` | Lay tu `X-Forwarded-For`, `X-Real-IP`, fallback `remoteAddr` |
| `return-url` | `xxxx-payment-service-dev.yml`, `VnPayService` | Dung `VNPAY_RETURN_URL`, default local qua gateway `/api/payment/vnpay-return` |
| callback URL public | gateway `public-endpoints` | `/api/payment/vnpay-callback` va `/api/payment/vnpay-return` bo qua JWT |
| Tra cuu transaction theo `txnRef` | `PaymentTransactionEntity`, `PaymentRepository` | Da co cot `txnRef` unique index va query `findByTxnRef` |

> `vnpay.return-url` va `vnp_ReturnUrl` tro toi gateway (`/api/payment/vnpay-return`) - dung,
> vi gateway la cua ngo public duy nhat. Tren VPS set `VNPAY_RETURN_URL` bang domain that.

## 10. Luong xac thuc & phan quyen hien tai

Cap nhat ngay 2026-06-02: luong xac thuc da chuyen sang mo hinh gateway-first dung hon cho
microservices. Frontend khong nhap JWT gateway thu cong nua; token duoc cap boi `user-service`
sau khi dang nhap va duoc gateway kiem tra o moi request can bao ve.

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Frontend
    participant GW as xxxx-gateway
    participant US as user-service
    participant SVC as business service

    U->>FE: dang nhap email/password
    FE->>GW: POST /api/users/login
    GW->>US: route public login
    US->>US: kiem tra password BCrypt
    US-->>FE: accessToken ngan han + refreshToken

    FE->>GW: request API kem Authorization: Bearer accessToken
    GW->>GW: verify signature, issuer, expiry, roles
    GW->>SVC: forward request + X-User-Id / X-User-Email / X-User-Roles

    alt accessToken het han
        FE->>GW: POST /api/users/refresh kem refreshToken
        GW->>US: route public refresh
        US->>US: revoke refresh token cu, tao token moi
        US-->>FE: accessToken moi + refreshToken moi
    end

    FE->>GW: POST /api/users/logout
    GW->>US: revoke refreshToken
```

### Nguyen tac bao mat da ap dung

| Hang muc | Cach xu ly |
|----------|------------|
| Password | Luu bang BCrypt cost 12. User cu dung SHA-256 duoc migrate sang BCrypt sau lan login thanh cong. |
| Access token | JWT ngan han, ky HMAC bang `JWT_SECRET`, co `issuer`, `jti`, `email`, `roles`. |
| Refresh token | Token random, chi luu hash trong DB, rotate moi lan refresh, revoke khi logout. |
| Gateway | La diem validate JWT va phan quyen role. Service nghiep vu nhan identity qua header noi bo. |
| Role | User mac dinh `USER`; endpoint quan tri/mutating can `ADMIN` tai gateway. |
| Config | `JWT_SECRET` bat buoc lay tu bien moi truong va dai toi thieu 32 ky tu. |

### Endpoint public lien quan auth

| Endpoint | Ghi chu |
|----------|---------|
| `POST /api/users/register` | Dang ky tai khoan, mat khau luu BCrypt. |
| `POST /api/users/login` | Dang nhap, cap access token va refresh token. |
| `POST /api/users/refresh` | Doi refresh token lay access token moi. |
| `POST /api/users/logout` | Thu hoi refresh token. |

### Endpoint can role ADMIN tai gateway

- Toan bo `/api/employees/**`.
- Cac request ghi du lieu (`POST`, `PUT`, `PATCH`, `DELETE`) tren `/api/events`, `/api/tickets`,
  `/api/ticket-details`, `/api/inventory`.

Day la lop phan quyen o edge. Neu muon chat hon nua cho production, buoc tiep theo nen them
kiem tra authorization trong tung service quan trong de phong truong hop service bi goi truc tiep
trong mang noi bo.

## 11. Waiting room cho flash sale

Sau cap nhat A2, `order-service` khong day thang moi request mua ve vao Saga nua. `placeOrder()` tao
`OrderEntity` trang thai `QUEUED`, cap `queueToken`, luu `OrderQueueEntity` status `WAITING` va tra token
cho client. Worker dinh ky trong `OrderServiceImpl.processWaitingRoomBatch()` moi lay cac item `WAITING`
theo `priority ASC, createdAt ASC`, gioi han so item `PROCESSING` dong thoi bang
`order.waiting-room.max-processing`, roi moi publish `OrderPlacedEvent` vao Kafka.

Token cho qua lau se duoc cap nhat thanh `EXPIRED` theo `order.waiting-room.token-ttl-minutes`. Khi saga thanh
cong hoac that bai, queue item duoc chuyen sang `COMPLETED` de dong vong doi waiting room.

## 12. Auth hardening trong user-service

`user-service` da bo sung 3 lop hardening nho truoc khi len VPS:

- `AdminBootstrapRunner`: co the tao tai khoan `ADMIN` ban dau tu bien moi truong khi bat
  `AUTH_BOOTSTRAP_ADMIN_ENABLED=true`.
- `AuthRateLimitService`: gioi han tan suat cho `login` va `refresh` theo cua so thoi gian ngan de
  giam brute-force/co gang spam refresh token.
- Auth lifecycle tests: kiem tra duong di `login -> refresh -> logout` o tang service de tranh hoi quy.
