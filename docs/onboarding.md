# Onboarding cho người mới

Tài liệu này dành cho người mới tinh vào project và muốn biết:

- nên đọc file nào trước
- luồng request đi qua những service nào
- khi debug một lỗi thì lần theo file ở đâu
- module nào là “nền tảng”, module nào là “nghiệp vụ”

Mục tiêu của file này không phải thay thế comment trong code, mà là giúp bạn **không bị ngợp** khi mở repo lần đầu.

## 1. Đọc theo thứ tự nào?

Nếu bạn mới vào repo, nên đọc theo đúng thứ tự này:

1. `README.md`
2. `howtostart.md`
3. `docs/architecture.md`
4. `docs/cong-nghe-giai-thich.md`
5. `xxxx-gateway/src/main/resources/application.yml`
6. `xxxx-order-service/src/main/java/com/xxxx/order/controller/OrderController.java`
7. `xxxx-order-service/src/main/java/com/xxxx/order/service/impl/OrderServiceImpl.java`
8. `xxxx-inventory-service/src/main/java/com/xxxx/inventory/service/impl/InventoryServiceImpl.java`
9. `xxxx-payment-service/src/main/java/com/xxxx/payment/service/impl/PaymentServiceImpl.java`
10. `xxxx-booking-service/src/main/java/com/xxxx/booking/service/impl/BookingServiceImpl.java`
11. `xxxx-common/src/main/java/com/xxxx/common/constant/KafkaTopics.java`
12. các event trong `xxxx-common/src/main/java/com/xxxx/common/event/`

Lý do của thứ tự này:

- `README.md` cho bạn bức tranh tổng thể.
- `howtostart.md` giúp bạn biết môi trường chạy ra sao.
- `docs/architecture.md` giúp hiểu service nào nói chuyện với service nào.
- `application.yml` của gateway cho bạn thấy các route public đi về đâu.
- `OrderServiceImpl` là nơi điều phối Saga chính, rất đáng đọc sớm.

## 2. Repo này gồm những nhóm gì?

### Nhóm nền tảng

- `xxxx-discovery`: Eureka, giúp service tìm nhau qua tên.
- `xxxx-config`: Config Server, đọc config tập trung từ `environment/config-repo`.
- `xxxx-gateway`: cổng vào duy nhất, xử lý JWT, correlation id, rate limit, route request.
- `xxxx-common`: thư viện dùng chung, chứa event, response chuẩn, util, security common.

### Nhóm nghiệp vụ chính

- `xxxx-user-service`: đăng ký, đăng nhập, refresh token, employee.
- `xxxx-event-service`: quản lý sự kiện/chuyến tàu.
- `xxxx-ticket-service`: quản lý ticket và ticket detail.
- `xxxx-inventory-service`: quản lý tồn kho, reserve/release, distributed lock, bucket config.
- `xxxx-order-service`: điều phối checkout/Saga.
- `xxxx-payment-service`: tạo giao dịch thanh toán, tích hợp VnPay.
- `xxxx-booking-service`: tạo/xác nhận/hủy booking sau khi order hoàn tất.

## 3. Nếu chỉ muốn hiểu luồng đặt vé, đọc gì trước?

Đọc theo đúng chuỗi sau:

1. `xxxx-order-service/src/main/java/com/xxxx/order/controller/OrderController.java`
2. `xxxx-order-service/src/main/java/com/xxxx/order/service/impl/OrderServiceImpl.java`
3. `xxxx-order-service/src/main/java/com/xxxx/order/event/producer/OrderEventProducer.java`
4. `xxxx-order-service/src/main/java/com/xxxx/order/service/OrderOutboxPublisher.java`
5. `xxxx-inventory-service/src/main/java/com/xxxx/inventory/event/consumer/OrderPlacedEventConsumer.java`
6. `xxxx-inventory-service/src/main/java/com/xxxx/inventory/service/impl/InventoryServiceImpl.java`
7. `xxxx-inventory-service/src/main/java/com/xxxx/inventory/event/producer/InventoryEventProducer.java`
8. `xxxx-order-service/src/main/java/com/xxxx/order/event/consumer/InventoryEventConsumer.java`
9. `xxxx-payment-service/src/main/java/com/xxxx/payment/controller/PaymentController.java`
10. `xxxx-payment-service/src/main/java/com/xxxx/payment/service/impl/PaymentServiceImpl.java`
11. `xxxx-payment-service/src/main/java/com/xxxx/payment/service/VnPayService.java`
12. `xxxx-order-service/src/main/java/com/xxxx/order/event/consumer/PaymentEventConsumer.java`
13. `xxxx-booking-service/src/main/java/com/xxxx/booking/event/consumer/OrderConfirmedEventConsumer.java`
14. `xxxx-booking-service/src/main/java/com/xxxx/booking/service/impl/BookingServiceImpl.java`

## 4. Bản đồ luồng code: từ request tới booking

### Bước 1: frontend gọi đặt vé

- API: `POST /api/orders/place`
- Controller: `xxxx-order-service/src/main/java/com/xxxx/order/controller/OrderController.java`
- Service: `xxxx-order-service/src/main/java/com/xxxx/order/service/impl/OrderServiceImpl.java`

Việc xảy ra:

- gắn `userId` từ principal đã xác thực
- đọc `Idempotency-Key`
- tạo `orderNo`, `correlationId`, `queueToken`
- lưu order trạng thái `QUEUED`
- lưu queue item vào waiting room

### Bước 2: waiting room đẩy order vào Saga

- File: `xxxx-order-service/src/main/java/com/xxxx/order/service/impl/OrderServiceImpl.java`
- Worker: method scheduled trong cùng class

Việc xảy ra:

- dọn queue token hết hạn
- chọn một batch order từ `WAITING` sang `PROCESSING`
- phát event `order.placed`

### Bước 3: order-service ghi event vào outbox

- Producer: `xxxx-order-service/src/main/java/com/xxxx/order/event/producer/OrderEventProducer.java`
- Publisher: `xxxx-order-service/src/main/java/com/xxxx/order/service/OrderOutboxPublisher.java`
- Entity: `xxxx-order-service/src/main/java/com/xxxx/order/repository/entity/OrderEventOutboxEntity.java`

Việc xảy ra:

- không publish Kafka trực tiếp trong transaction nghiệp vụ
- event được lưu vào bảng outbox trước
- worker riêng lấy record `PENDING/RETRY` để publish Kafka

## 5. Vì sao project dùng outbox?

Nếu chỉ `save(order)` rồi publish Kafka ngay trong cùng flow, có 2 rủi ro:

- DB commit thành công nhưng Kafka lỗi → mất event
- Kafka publish thành công nhưng transaction DB rollback → state lệch giữa DB và Kafka

Outbox pattern xử lý bằng cách:

- ghi event vào DB trong cùng transaction nghiệp vụ
- commit xong mới có worker publish Kafka
- nếu Kafka lỗi thì retry, không mất event

File nên đọc:

- `xxxx-order-service/src/main/java/com/xxxx/order/service/OrderOutboxPublisher.java`
- `xxxx-payment-service/src/main/java/com/xxxx/payment/service/PaymentOutboxPublisher.java`
- `xxxx-inventory-service/src/main/java/com/xxxx/inventory/service/InventoryOutboxPublisher.java`

## 6. Inventory hoạt động ra sao?

File quan trọng nhất:

- `xxxx-inventory-service/src/main/java/com/xxxx/inventory/service/impl/InventoryServiceImpl.java`

Ý chính:

- DB là nguồn sự thật.
- Redis là lớp tăng tốc.
- reserve/release đều phải có lịch sử trong DB.
- nếu Redis mất key, service có thể tính lại từ DB.

Các method đáng chú ý:

- `getStockLevel(...)`: đọc tồn kho, ưu tiên Redis
- `initializeStock(...)`: nạp tồn kho ban đầu
- `reserveStock(...)`: giữ vé cho order
- `releaseStock(...)`: hoàn vé khi compensation
- `reconcileAllStockToRedis()`: khôi phục cache từ DB

### Distributed lock dùng ở đâu?

- File: `xxxx-inventory-service/src/main/java/com/xxxx/inventory/lock/DistributedLockService.java`

Mục tiêu:

- tránh nhiều request cùng sửa một bucket tồn kho cùng lúc
- tránh double-init stock
- chỉ đúng chủ sở hữu lock mới được unlock

## 7. Payment hoạt động ra sao?

File quan trọng:

- `xxxx-payment-service/src/main/java/com/xxxx/payment/controller/PaymentController.java`
- `xxxx-payment-service/src/main/java/com/xxxx/payment/service/impl/PaymentServiceImpl.java`
- `xxxx-payment-service/src/main/java/com/xxxx/payment/service/VnPayService.java`

Luồng:

- order-service gọi `initiatePayment()` để tạo transaction và lấy `paymentUrl`
- frontend redirect người dùng sang VnPay
- VnPay gọi callback/IPN
- payment-service verify chữ ký
- payment-service cập nhật transaction
- payment-service phát `payment.completed` hoặc `payment.failed`

## 8. Booking được tạo khi nào?

Không phải lúc user bấm đặt vé.

Booking thường được tạo/xác nhận khi:

- inventory đã giữ vé thành công
- payment đã hoàn tất thành công
- order-service phát event `order.confirmed`

File quan trọng:

- `xxxx-booking-service/src/main/java/com/xxxx/booking/event/consumer/OrderConfirmedEventConsumer.java`
- `xxxx-booking-service/src/main/java/com/xxxx/booking/service/impl/BookingServiceImpl.java`

Nhánh bù trừ:

- nếu order bị hủy thì `order.cancelled` sẽ làm booking chuyển sang `CANCELLED`

## 9. Gateway xử lý gì trước khi request vào service?

Đọc theo thứ tự này:

1. `xxxx-gateway/src/main/java/com/xxxx/gateway/filter/CorrelationIdFilter.java`
2. `xxxx-gateway/src/main/java/com/xxxx/gateway/filter/AuthenticationFilter.java`
3. `xxxx-gateway/src/main/java/com/xxxx/gateway/filter/RateLimitingConfig.java`
4. `xxxx-gateway/src/main/resources/application.yml`

Ý nghĩa:

- `CorrelationIdFilter`: tạo mã truy vết xuyên suốt hệ thống
- `AuthenticationFilter`: kiểm tra JWT và gắn metadata user đã xác thực
- `RateLimitingConfig`: chặn spam request theo IP
- `application.yml`: cho biết path nào route tới service nào

## 10. Khi debug lỗi, nên lần từ đâu?

### Lỗi đặt vé không chạy tiếp

Đọc theo thứ tự:

- `OrderController`
- `OrderServiceImpl`
- `OrderEventProducer`
- `OrderOutboxPublisher`
- log Kafka/outbox

### Lỗi giữ vé thất bại hoặc oversell

Đọc theo thứ tự:

- `OrderPlacedEventConsumer`
- `InventoryServiceImpl`
- `DistributedLockService`
- bảng lịch sử inventory / Redis key

### Lỗi thanh toán

Đọc theo thứ tự:

- `PaymentController`
- `PaymentServiceImpl`
- `VnPayService`
- `PaymentOutboxPublisher`

### Lỗi booking không tạo

Đọc theo thứ tự:

- `PaymentEventConsumer`
- `OrderServiceImpl.handlePaymentCompleted(...)`
- `OrderConfirmedEventConsumer`
- `BookingServiceImpl.confirmBookingFromOrder(...)`

## 11. Nếu chỉ có 30 phút để hiểu project

Đọc đúng 8 file này:

- `README.md`
- `docs/architecture.md`
- `xxxx-gateway/src/main/resources/application.yml`
- `xxxx-order-service/src/main/java/com/xxxx/order/service/impl/OrderServiceImpl.java`
- `xxxx-inventory-service/src/main/java/com/xxxx/inventory/service/impl/InventoryServiceImpl.java`
- `xxxx-payment-service/src/main/java/com/xxxx/payment/service/impl/PaymentServiceImpl.java`
- `xxxx-booking-service/src/main/java/com/xxxx/booking/service/impl/BookingServiceImpl.java`
- `xxxx-common/src/main/java/com/xxxx/common/constant/KafkaTopics.java`

## 12. Nếu muốn hiểu sâu hơn nữa

Sau khi đọc xong onboarding này, đi tiếp theo lộ trình:

- DTO request/response của từng service
- entity chính của order/payment/booking/ticket/user
- outbox publishers
- security config của từng service
- test của inventory/order/payment

## 13. Gợi ý cho người mới ngày đầu tiên

### Buổi 1

- đọc `README.md`
- đọc `docs/architecture.md`
- chạy hệ thống theo `howtostart.md`
- gọi thử login và đặt vé

### Buổi 2

- đọc gateway routes
- đọc order flow
- đọc inventory reserve/release

### Buổi 3

- đọc payment/VnPay flow
- đọc booking consumer
- đọc outbox publishers

## 14. Tài liệu liên quan

- `README.md`
- `howtostart.md`
- `docs/architecture.md`
- `docs/cong-nghe-giai-thich.md`
- `docs/ke-hoach-cong-viec.md`
