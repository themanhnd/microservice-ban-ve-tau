# 📋 Kế hoạch công việc (Work Plan)

> File này để theo dõi tiến độ và biết tiếp tục từ đâu. Tick `[x]` khi xong.
> Cập nhật lần cuối: 2026-06-02

---

## ✅ Đã hoàn thành

- [x] **Vá lỗ hổng Config Server** — sửa `search-locations`, mount `environment/config-repo`
      vào Docker, sửa `spring.config.import` để tôn trọng `SPRING_CLOUD_CONFIG_URI`, sửa mật
      khẩu MySQL trong 7 file config-repo.
- [x] **Tài liệu kiến trúc** — `docs/architecture.md` (kiến trúc, giao tiếp, Saga, ERD,
      observability, luồng VNPay).
- [x] **Tài liệu công nghệ** — `docs/cong-nghe-giai-thich.md` (lý thuyết + bài toán tải cao).
- [x] **README.md** — tổng quan dự án.
- [x] **Đẩy lên Git** — remote `origin` = github.com/themanhnd/microservice-ban-ve-tau.
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

## 📌 Trạng thái Git hiện tại

- Remote: `origin` → https://github.com/themanhnd/microservice-ban-ve-tau
- Nhánh: `main` (đã set upstream)
- Commit gần nhất: `feat(inventory): implement DB-based stock calculation and Redis rehydration`
