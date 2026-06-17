# 📋 Kế hoạch công việc (Work Plan)

> File này để theo dõi tiến độ và biết tiếp tục từ đâu. Tick `[x]` khi xong.
> Cập nhật lần cuối: 2026-06-17

---

## ✅ Đã hoàn thành

- [x] **Vá lỗ hổng Config Server** — sửa `search-locations`, mount `environment/config-repo`
      vào Docker, sửa `spring.config.import` để tôn trọng `SPRING_CLOUD_CONFIG_URI`, sửa mật
      khẩu MySQL trong 7 file config-repo.
- [x] **Tài liệu kiến trúc** — `docs/architecture.md` đã cập nhật theo code/config hiện tại: gateway routes, Docker profiles, Saga, outbox, local-vs-Docker port.
- [x] **Tài liệu công nghệ** — `docs/cong-nghe-giai-thich.md` (lý thuyết + bài toán tải cao).
- [x] **README.md** — tổng quan dự án, module, API chính, biến môi trường và ghi chú triển khai đã đồng bộ lại.
- [x] **Chuẩn hóa tài liệu chạy dự án** — cập nhật `howtostart.md` theo Docker Compose hiện tại và cảnh báo khác biệt port local/Docker.
- [x] **Flash sale #1: Khóa Redis an toàn (owner-safe)** — `DistributedLockService` (SET NX +
      Lua release theo token), 11 unit test.
- [x] **Flash sale #2: Tồn kho từ DB + tự phục hồi Redis** — `sumQuantityByType`,
      `initializeStock` (idempotent), `reserveStock` tự rehydrate, 7 unit test.
- [x] **Auth #1: Bỏ nhập JWT gateway thủ công trên frontend** — `user-service` cấp access token
      và refresh token sau login; frontend tự refresh khi `401`, logout sẽ revoke refresh token.
- [x] **Auth #2: Nâng cấp password hashing** — BCrypt cost 12, user cũ SHA-256 được migrate
      mềm sang BCrypt sau lần login thành công.
- [x] **Auth #3: Role-based gateway authorization** — JWT có issuer/roles; gateway yêu cầu
      `ADMIN` cho endpoint nhân viên và các request ghi dữ liệu quản trị.

---

## 🔜 Việc còn lại

### Nhóm A — Lộ trình flash sale (đi sâu kỹ thuật tải cao)

- [x] **A1. Nối bucket vào đường giữ vé (sharding hot row)** — Ưu tiên: Trung bình
  - Mục tiêu: thay vì 1 key Redis + 1 khóa cho mỗi `ticketDetailId`, chia tồn kho thành nhiều
    bucket để nhiều request giữ vé song song không tranh nhau.
  - Việc cần làm:
    - [x] Chọn bucket khi giữ vé (ví dụ hash theo `orderId`/`userId` → bucket index).
    - [x] Key Redis theo bucket: `stock:available:{ticketDetailId}:{bucketIndex}`.
    - [x] Khóa theo bucket: `lock:inventory:{ticketDetailId}:{bucketIndex}`.
    - [x] Logic "nạp nguồn" (back-to-source) khi một bucket cạn — dùng `InventoryBucketConfigEntity`
          (`backSourceProportion`, `backSourceStep`, `thresholdValue`).
    - [x] Cập nhật `initializeStock` để chia tồn kho ban đầu vào N bucket.
    - [x] Unit test: giữ vé song song trên nhiều bucket, tổng không vượt tồn kho.
  - File liên quan: `InventoryServiceImpl`, `InventoryBucketConfigEntity`, `InventoryBucketConfigRepository`.

- [x] **A2. Kích hoạt waiting room (hàng đợi công bằng)** — Ưu tiên: Trung bình
  - Mục tiêu: khi tải cao, cấp token cho người dùng vào hàng đợi thay vì xử lý thẳng.
  - Việc cần làm:
    - [x] Cấp token khi user vào mua → lưu `OrderQueueEntity` (status WAITING).
    - [x] Worker lấy đơn từ hàng đợi theo `priority`/thứ tự, giới hạn số đơn PROCESSING đồng thời.
    - [x] `placeOrder()` chuyển sang mô hình "nhận token → xếp hàng" thay vì tạo đơn ngay.
    - [x] Cơ chế hết hạn token (status EXPIRED) cho người chờ quá lâu.
    - [x] Unit test cho thứ tự ưu tiên và giới hạn đồng thời.
  - File liên quan: `OrderServiceImpl`, `OrderQueueEntity`, `OrderRepository`.

### Nhóm B — Bảo mật (làm TRƯỚC khi lên VPS) — Ưu tiên: CAO

- [x] **B1. Tách secret hardcode ra biến môi trường**
  - [x] MySQL password — docker-compose + config-repo.
  - [x] Grafana admin password — docker-compose.
  - [x] JWT secret mặc định — `xxxx-gateway/application.yml`.
  - [x] `encrypt.key` của Config Server — `xxxx-config/application.yml`.
  - [x] VNPay `secret-key` — `VnPayService`.
  - [x] Tạo file `.env.example` ghi danh sách biến cần set (không chứa giá trị thật).
- [x] **B2. Chỉ expose gateway (8080) ra internet** — các service khác để mạng nội bộ Docker.
- [x] **B3. Auth đúng chuẩn hơn**
  - [x] User-service cấp JWT sau login, không yêu cầu frontend nhập token gateway thủ công.
  - [x] Access token ngắn hạn + refresh token rotate/revoke.
  - [x] Refresh token chỉ lưu hash trong database.
  - [x] Gateway validate issuer và role.

- [x] **B4. Hardening auth tiếp theo**
  - [x] Thêm seed/bootstrap admin bằng biến môi trường.
  - [x] Thêm integration test cho login → refresh → logout.
  - [x] Thêm rate limit riêng cho login/refresh.

- [x] **B5. Chuyển đúng mô hình Gateway authenticate, Service authorize** — Ưu tiên: CAO
  - Mục tiêu: gateway chỉ xác thực JWT và forward token; từng service tự verify JWT lại và tự
    quyết định quyền theo nghiệp vụ. Không phụ thuộc hoàn toàn vào rule role ở gateway.
  - Nguyên tắc:
    - [x] Gateway validate JWT (`signature`, `issuer`, `expiry`) cho endpoint private.
    - [x] Gateway strip mọi header identity do client gửi: `X-User-Id`, `X-User-Email`, `X-User-Roles`.
    - [x] Gateway forward nguyên `Authorization: Bearer <token>` xuống service.
    - [x] `X-User-*` nếu còn dùng thì chỉ là metadata phụ cho log/debug, không phải nguồn phân quyền chính.
    - [x] Service verify JWT lại trước khi authorize.
  - Việc cần làm:
    - [x] Tạo shared JWT validation/auth helper trong `xxxx-common` nếu phù hợp, hoặc module security chung
          để tránh copy-paste parser ở từng service.
    - [x] Chuẩn hóa claim JWT: `sub` = user id, `email`, `roles`, `iss`, `exp`, `jti`.
    - [x] Sửa `xxxx-gateway`: bỏ rule authorization chi tiết theo path/method, giữ public/private guard,
          strip header giả mạo và forward `Authorization`.
    - [x] Thêm `SecurityConfig` + `JwtAuthenticationFilter` + principal nội bộ cho các service quan trọng.
    - [x] Map role sang Spring authority: `ADMIN` → `ROLE_ADMIN`, `USER` → `ROLE_USER`.
    - [x] Dùng `@EnableMethodSecurity` và `@PreAuthorize` cho endpoint cần quyền.
    - [x] Chuẩn hóa response `401 Unauthorized` và `403 Forbidden`.
  - Rule endpoint ưu tiên:
    - [x] `xxxx-user-service`: `/api/employees/**` cần `ADMIN`; `/api/users/{id}` là chính user đó hoặc `ADMIN`.
    - [x] `xxxx-event-service`: create/update/delete event cần `ADMIN`.
    - [x] `xxxx-ticket-service`: create/update/delete ticket và ticket detail cần `ADMIN`.
    - [x] `xxxx-inventory-service`: initialize stock, sửa tồn kho, bucket config cần `ADMIN`.
    - [x] `xxxx-order-service`: user chỉ xem/hủy order của mình; `ADMIN` xem tất cả.
    - [x] `xxxx-booking-service`: user chỉ xem booking của mình; `ADMIN` xem tất cả.
    - [x] `xxxx-payment-service`: VNPay callback/return vẫn public; transaction/admin endpoint cần `ADMIN`
          hoặc owner check.
  - Test bắt buộc:
    - [x] Gateway reject endpoint private khi thiếu/invalid token.
    - [x] Gateway strip `X-User-Roles: ADMIN` giả từ client.
    - [x] Service reject request gọi thẳng không có JWT.
    - [x] Service reject user thường gọi endpoint admin.
    - [x] Service accept admin gọi endpoint admin.
    - [x] Owner rule: user A không xem được order/booking/payment của user B.
    - [x] Public endpoint vẫn chạy: login/register/refresh, VNPay callback/return, event listing nếu public.
  - Tài liệu:
    - [x] Cập nhật `docs/architecture.md` flow auth mới.
    - [x] Cập nhật `docs/cong-nghe-giai-thich.md` phần Gateway authentication vs Service authorization.
    - [x] Cập nhật lại mục này sau khi triển khai xong.

### Nhóm C — Sửa lỗi VNPay (chặn chạy thật) — Ưu tiên: CAO

- [x] **C1. Thống nhất `return-url`** — code và config-repo cùng dùng gateway return URL,
      cấu hình bằng `VNPAY_RETURN_URL` để đổi sang domain public trên VPS.
- [x] **C2. `vnp_IpAddr`** — lấy IP thật của request thay vì cố định `127.0.0.1`.
- [x] **C3. `findTransactionByTxnRef`** — bỏ quét toàn bảng. Lưu
      `txnRef` thành cột riêng có index, thêm query `findByTxnRef`.

### Nhóm D — Vận hành & chất lượng — Ưu tiên: Thấp

- [x] **D1. Tách docker-compose theo profile** (infra / observability / platform / business)
      để máy dev nhẹ hơn.
- [x] **D2. Reconciliation job** — đối soát Redis vs MySQL định kỳ (chống lệch khi service
      chết giữa chừng).
- [x] **D3. Dọn warning null-safety** của Spring `@NonNull` (chỉ cảnh báo, không lỗi).
- [ ] **D4. Bổ sung integration test** với Testcontainers (MySQL + Redis + Kafka thật).

### Nhóm E — Hoàn thiện flow end-to-end order/payment/booking — Ưu tiên: CAO

- [x] **E1. Trigger payment sau inventory reserved**
      - Sau khi nhận `inventory.reserved`, `order-service` gọi `payment-service /api/payment/initiate`.
      - Lưu `paymentTransactionId`, `paymentUrl` vào order để frontend checkout được.
      - Nếu payment initiate fail, chuyển order sang huỷ có compensation.
- [x] **E2. Bổ sung checkout/status response cho frontend**
      - Mở rộng `OrderStatusResponse`/`OrderResponse` để trả `paymentTransactionId`, `paymentUrl`,
        `queueStatus`, `failureReason` khi cần.
      - Frontend có thể poll endpoint status để biết đang queue, được thanh toán, hay đã confirm.
- [x] **E3. Compensation inventory qua Kafka**
      - Thêm consumer `order.cancelled` trong `inventory-service`.
      - Khi `compensationRequired=true`, release stock idempotent cho order đã reserve.
- [x] **E4. Hoàn thiện booking lifecycle gắn với order**
      - `booking-service` nhận `order.confirmed` sẽ upsert booking `CONFIRMED` theo `orderNo`.
      - Khi order huỷ, booking liên quan (nếu đã tạo) chuyển `CANCELLED`.
- [x] **E5. Đồng bộ waiting-room expiry với order status**
      - Khi token queue hết hạn, order chuyển sang `EXPIRED` kèm failure reason rõ ràng.
      - Frontend không bị kẹt ở `QUEUED` vô thời hạn.
- [x] **E6. Test bắt buộc cho flow end-to-end**
      - `inventory.reserved` -> payment initiated -> order status có `paymentUrl`.
      - `payment.failed` -> publish `order.cancelled` -> inventory release.
      - `payment.completed` -> order confirmed -> booking confirmed/upsert.
      - waiting-room token expire -> order expire.
---

## 🧭 Gợi ý thứ tự tiếp theo

- **Nếu mục tiêu học/hoàn thiện kỹ thuật tải cao:** A1 → A2.
- **Nếu mục tiêu sớm deploy lên VPS:** B1 → C1/C2/C3 → B2 → D1.

---

## 🛠 Lệnh hay dùng (ghi nhớ)

```bash
# Maven nằm trong IntelliJ (chưa có trong PATH):
# "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd"

# Build + test một module
mvn -pl xxxx-inventory-service test

# Cài common vào local repo (lần đầu)
mvn -N install -DskipTests           # parent pom
mvn -pl xxxx-common install -DskipTests

# Chạy hạ tầng cho dev
docker-compose --profile infra up -d

# Lưu thay đổi lên git (tracking đã set sẵn)
git add -A
git commit -m "..."
git push
```


### Nhóm F — Độ tin cậy checkout/order saga — Ưu tiên: RẤT CAO

- [x] **F1. Idempotency key cho /api/orders/place**
  - Nhận header `Idempotency-Key` từ frontend.
  - Lưu key theo user/order để double-click hoặc retry không tạo nhiều order.
  - Trả lại order đã tạo nếu request lặp cùng key.
- [x] **F2. Payment timeout auto-cancel**
  - Worker định kỳ tìm order `PAYMENT_PROCESSING` quá X phút.
  - Chuyển order sang `CANCELLED` và phát `order.cancelled` để release inventory.
- [x] **F3. Idempotent consumers cho Kafka at-least-once**
  - Guard `inventory.reserved`, `inventory.reserve-failed`, `payment.completed`, `payment.failed` theo trạng thái order.
  - Booking `order.confirmed` upsert theo `orderNo` và đảm bảo unique ở DB.
- [x] **F4. Endpoint checkout rõ ràng cho frontend**
  - Thêm `GET /api/orders/{orderNo}/checkout` trả `status`, `queuePosition`, `paymentUrl`, `expiresAt`, `failureReason`.
  - Frontend dùng endpoint này để polling/redirect thay vì tự ghép nhiều API.
- [x] **F5. Outbox/retry/DLQ cho Kafka publish**
  - Đã mở rộng outbox sang các service có producer: `order-service`, `inventory-service`, `payment-service`.
  - Producer chỉ ghi event vào outbox trong transaction DB; publisher định kỳ mới gửi Kafka sau commit.
  - Worker retry theo `nextRetryAt`, tăng `attemptCount`, lưu `lastError` để dễ vận hành.
  - Trạng thái `FAILED` đóng vai trò DLQ nội bộ trong DB khi publish lỗi quá số lần cho phép.
  - Các lớp chính: `OrderOutboxPublisher`, `InventoryOutboxPublisher`, `PaymentOutboxPublisher`.
  - Test đã bao phủ enqueue event, publish thành công và retry/fail path cho từng service.

### Ghi chú triển khai tiếp theo

- [ ] **Migration DB chính thức**
  - Hiện entity mới dựa vào JPA auto-DDL; trước production cần thêm Flyway/Liquibase migration cho bảng outbox, cột order timeout/idempotency và unique index booking/order.
- [x] **Vận hành DLQ nội bộ**
  - Đã có admin endpoint cho `order-service`, `payment-service`, `inventory-service` để xem `FAILED`, replay và ignore có lưu lý do audit.
- [x] **Metrics/alerting cho outbox**
  - Đã có endpoint tổng hợp metric admin, metric Actuator `app.outbox.*` và rule cảnh báo Prometheus cho trường hợp `FAILED`, retry nhiều hoặc kẹt quá lâu.


### Nhóm G — Việc cần làm tiếp sau F5 — Ưu tiên: CAO

- [x] **G1. Thêm migration DB chính thức cho saga/outbox**
  - Đã thêm Flyway migration đầu tiên cho bảng outbox của `order-service`, `inventory-service`, `payment-service`.
  - Đã thêm migration saga cho các cột `idempotencyKey`, `paymentExpiresAt`, `paymentTransactionId`, `paymentUrl`, `failureReason` của order/payment.
  - Đã thêm migration unique index cho booking theo `orderNo` và index truy vấn outbox theo `status/nextAttemptAt`.
  - Đã bật Flyway theo kiểu `baseline-on-migrate` trong config dev của các service liên quan để an toàn hơn với schema đã tồn tại.
  - Đã thêm `OrderFlywayMigrationIntegrationTest` để chạy migration order-service trên MySQL thật với schema cũ tối thiểu.
  - Đã thêm `PaymentFlywayMigrationIntegrationTest`, `InventoryFlywayMigrationIntegrationTest`, `BookingFlywayMigrationIntegrationTest` để kiểm tra migration còn lại trên MySQL thật.
- [x] **G2. Admin endpoint/job để vận hành DLQ nội bộ**
  - Liệt kê outbox record `FAILED` theo service/topic/thời gian.
  - Cho phép replay record đã fail sau khi sửa lỗi hạ tầng.
  - Có thao tác ignore/resolve kèm lý do để phục vụ audit.
- [x] **G3. Metrics và alerting cho outbox**
  - Expose metric số lượng `PENDING`, `RETRY`, `FAILED` theo service/topic.
  - Theo dõi tuổi record cũ nhất và số lần retry cao nhất.
  - Bổ sung alert khi có `FAILED` hoặc outbox bị kẹt quá ngưỡng.
- [ ] **G4. Integration test với Testcontainers**
  - Đã bắt đầu bằng `InventoryReserveIntegrationTest`: chạy MySQL + Redis thật, tạo 20 request giữ vé đồng thời trên tồn kho 10 và assert không oversell.
  - Đã thêm `OrderOutboxIntegrationTest`: chạy MySQL + Kafka thật, lưu record outbox `PENDING`, publish qua worker và assert record chuyển `PUBLISHED` + Kafka nhận event thật.
  - `OrderOutboxIntegrationTest` cũng đã phủ nhánh lỗi: event deserialize lỗi sẽ không mất record mà chuyển `RETRY`, tăng `attemptCount` và giữ `lastError`.
  - Đã thêm `PaymentCallbackIntegrationTest`: chạy MySQL thật, gọi VnPay callback thành công 2 lần và assert chỉ có 1 outbox `payment.completed`.
  - Đã thêm `OrderPaymentConsumerIdempotencyIntegrationTest`: chạy MySQL thật, xử lý duplicate `inventory.reserved`/`payment.completed`/`payment.failed` và assert không gọi payment lặp, không sinh outbox trùng.
  - Chạy MySQL + Kafka + Redis thật để kiểm tra transaction DB commit trước khi publish Kafka.
  - Test retry khi Kafka tạm thời down và publish lại khi Kafka phục hồi.
  - Test consumer idempotent khi Kafka deliver duplicate event.
- [x] **G5. Rà soát encoding/tài liệu còn mojibake**
  - Đã chuẩn hóa `README.md`, `howtostart.md`, `docs/architecture.md` theo trạng thái code hiện tại.
  - Đã sửa đoạn mojibake cuối `docs/cong-nghe-giai-thich.md`.
- [x] **G6. Security/rate limit cho endpoint checkout/order**
  - Đã thêm route rate limit riêng cho `POST /api/orders/place` ở Gateway, ưu tiên key theo user đã xác thực và fallback về IP.
  - Quyền xem `GET /api/orders/{orderNo}/checkout` đã được guard bằng `@orderAuthorization.canAccessOrder(...)`.
  - Error response bảo mật hiện vẫn theo handler thống nhất; nếu cần có thể siết thêm ở vòng sau.

### Ghi chú cho lần làm tiếp

- Ưu tiên làm G1 trước nếu chuẩn bị deploy production, vì entity hiện tại đang phụ thuộc JPA auto-DDL.
- Sau G1, làm G2 + G3 để có khả năng vận hành/replay khi Kafka publish fail.
- G4 nên làm trước khi refactor lớn tiếp theo để bắt lỗi end-to-end sớm.

### Roadmap tương lai — Cân nhắc Debezium CDC (chưa ưu tiên ngay)

- [ ] **H1. Đánh giá có nên chuyển từ outbox polling sang Debezium hay không**
  - Hiện tại hệ thống đã có outbox tự triển khai khá đầy đủ: ghi DB trong transaction, worker publish, retry, DLQ nội bộ, metrics, admin replay/ignore và integration test.
  - Vì vậy Debezium **chưa phải ưu tiên số 1 ở giai đoạn này**; chỉ nên cân nhắc khi hệ thống tăng mạnh số lượng service producer hoặc tải publish Kafka lớn hơn.

- [ ] **H2. Làm PoC Debezium cho 1 service trước, không thay toàn hệ ngay**
  - Ưu tiên thử trên `order-service` hoặc `payment-service` vì đây là 2 service có outbox quan trọng nhất trong Saga.
  - Giữ nguyên bảng outbox, chỉ thay worker polling bằng Debezium Outbox/Event Router để so sánh độ phức tạp và độ ổn định.

- [ ] **H3. So sánh chi phí vận hành trước khi quyết định chuyển đổi**
  - Đánh giá thêm hạ tầng cần có: Kafka Connect, Debezium connector, config connector, monitoring connector, schema/history topic.
  - So sánh các tiêu chí: độ dễ debug, effort local dev, độ phức tạp production, thông lượng event, effort bảo trì code Java hiện tại.

- [ ] **H4. Chỉ chuyển hẳn sang Debezium khi có lợi ích rõ ràng hơn mô hình hiện tại**
  - Nếu outbox polling hiện tại vẫn đáp ứng tốt SLA, dễ vận hành và dễ debug thì tiếp tục giữ kiến trúc hiện tại.
  - Nếu PoC cho thấy Debezium giảm tải polling đáng kể, đơn giản hóa publish path và team đã sẵn sàng vận hành Kafka Connect thì mới lập kế hoạch migrate từng service.
