# 📋 Kế hoạch công việc (Work Plan)

> File này để theo dõi tiến độ và biết tiếp tục từ đâu. Tick `[x]` khi xong.
> Cập nhật lần cuối: 2026-06-01

---

## ✅ Đã hoàn thành

- [x] **Vá lỗ hổng Config Server** — sửa `search-locations`, mount `environment/config-repo`
      vào Docker, sửa `spring.config.import` để tôn trọng `SPRING_CLOUD_CONFIG_URI`, sửa mật
      khẩu MySQL (`root` → `root123`) trong 7 file config-repo.
- [x] **Tài liệu kiến trúc** — `docs/architecture.md` (kiến trúc, giao tiếp, Saga, ERD,
      observability, luồng VNPay).
- [x] **Tài liệu công nghệ** — `docs/cong-nghe-giai-thich.md` (lý thuyết + bài toán tải cao).
- [x] **README.md** — tổng quan dự án.
- [x] **Đẩy lên Git** — remote `origin` = github.com/themanhnd/microservice-ban-ve-tau.
- [x] **Flash sale #1: Khóa Redis an toàn (owner-safe)** — `DistributedLockService` (SET NX +
      Lua release theo token), 11 unit test.
- [x] **Flash sale #2: Tồn kho từ DB + tự phục hồi Redis** — `sumQuantityByType`,
      `initializeStock` (idempotent), `reserveStock` tự rehydrate, 7 unit test.

---

## 🔜 Việc còn lại

### Nhóm A — Lộ trình flash sale (đi sâu kỹ thuật tải cao)

- [ ] **A1. Nối bucket vào đường giữ vé (sharding hot row)** — Ưu tiên: Trung bình
  - Mục tiêu: thay vì 1 key Redis + 1 khóa cho mỗi `ticketDetailId`, chia tồn kho thành nhiều
    bucket để nhiều request giữ vé song song không tranh nhau.
  - Việc cần làm:
    - [ ] Chọn bucket khi giữ vé (ví dụ hash theo `orderId`/`userId` → bucket index).
    - [ ] Key Redis theo bucket: `stock:available:{ticketDetailId}:{bucketIndex}`.
    - [ ] Khóa theo bucket: `lock:inventory:{ticketDetailId}:{bucketIndex}`.
    - [ ] Logic "nạp nguồn" (back-to-source) khi một bucket cạn — dùng `InventoryBucketConfigEntity`
          (`backSourceProportion`, `backSourceStep`, `thresholdValue`).
    - [ ] Cập nhật `initializeStock` để chia tồn kho ban đầu vào N bucket.
    - [ ] Unit test: giữ vé song song trên nhiều bucket, tổng không vượt tồn kho.
  - File liên quan: `InventoryServiceImpl`, `InventoryBucketConfigEntity`, `InventoryBucketConfigRepository`.

- [ ] **A2. Kích hoạt waiting room (hàng đợi công bằng)** — Ưu tiên: Trung bình
  - Mục tiêu: khi tải cao, cấp token cho người dùng vào hàng đợi thay vì xử lý thẳng.
  - Việc cần làm:
    - [ ] Cấp token khi user vào mua → lưu `OrderQueueEntity` (status WAITING).
    - [ ] Worker lấy đơn từ hàng đợi theo `priority`/thứ tự, giới hạn số đơn PROCESSING đồng thời.
    - [ ] `placeOrder()` chuyển sang mô hình "nhận token → xếp hàng" thay vì tạo đơn ngay.
    - [ ] Cơ chế hết hạn token (status EXPIRED) cho người chờ quá lâu.
    - [ ] Unit test cho thứ tự ưu tiên và giới hạn đồng thời.
  - File liên quan: `OrderServiceImpl`, `OrderQueueEntity`, `OrderRepository`.

### Nhóm B — Bảo mật (làm TRƯỚC khi lên VPS) — Ưu tiên: CAO

- [ ] **B1. Tách secret hardcode ra biến môi trường**
  - [ ] MySQL password (`root123`) — docker-compose + config-repo.
  - [ ] Grafana `admin123` — docker-compose.
  - [ ] JWT secret mặc định — `xxxx-gateway/application.yml`.
  - [ ] `encrypt.key` của Config Server — `xxxx-config/application.yml`.
  - [ ] VNPay `secret-key` — `VnPayService`.
  - [ ] Tạo file `.env.example` ghi danh sách biến cần set (không chứa giá trị thật).
- [ ] **B2. Chỉ expose gateway (8080) ra internet** — các service khác để mạng nội bộ Docker.

### Nhóm C — Sửa lỗi VNPay (chặn chạy thật) — Ưu tiên: CAO

- [ ] **C1. Thống nhất `return-url`** — code mặc định `127.0.0.1:8080/api/payment/vnpay-return`
      vs config-repo `localhost:3000/payment/return`. Phải khớp + dùng domain public trên VPS.
- [ ] **C2. `vnp_IpAddr`** — lấy IP thật của request thay vì cố định `127.0.0.1`.
- [ ] **C3. `findTransactionByTxnRef`** — đang `findAll().stream()` (quét toàn bảng). Lưu
      `txnRef` thành cột riêng có index, thêm query `findByTxnRef`.

### Nhóm D — Vận hành & chất lượng — Ưu tiên: Thấp

- [ ] **D1. Tách docker-compose theo profile** (infra / observability / platform / business)
      để máy dev nhẹ hơn.
- [ ] **D2. Reconciliation job** — đối soát Redis vs MySQL định kỳ (chống lệch khi service
      chết giữa chừng).
- [ ] **D3. Dọn warning null-safety** của Spring `@NonNull` (chỉ cảnh báo, không lỗi).
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
docker-compose up -d mysql redis zookeeper kafka

# Lưu thay đổi lên git (tracking đã set sẵn)
git add -A
git commit -m "..."
git push
```

## 📌 Trạng thái Git hiện tại

- Remote: `origin` → https://github.com/themanhnd/microservice-ban-ve-tau
- Nhánh: `main` (đã set upstream)
- Commit gần nhất: `feat(inventory): implement DB-based stock calculation and Redis rehydration`
