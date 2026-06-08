# Giải thích công nghệ trong dự án (dành cho người mới)

> Tài liệu này giải thích **vì sao dự án dùng từng công nghệ**, **công nghệ đó giải quyết
> bài toán gì**, và **một chút lý thuyết nền** — viết theo kiểu thầy giáo giảng cho học sinh,
> có ví dụ đời thường để dễ hình dung.

---

## Mục lục

1. [Bức tranh lớn: vì sao lại là Microservices?](#1-bức-tranh-lớn-vì-sao-lại-là-microservices)
2. [Java 21 & Spring Boot](#2-java-21--spring-boot)
3. [Eureka - Service Discovery (danh bạ)](#3-eureka---service-discovery)
4. [Spring Cloud Config - Cấu hình tập trung](#4-spring-cloud-config)
5. [Spring Cloud Gateway - Cổng vào](#5-spring-cloud-gateway)
6. [OpenFeign - Gọi service khác như gọi hàm](#6-openfeign)
7. [Resilience4j - Chống sập dây chuyền](#7-resilience4j)
8. [Apache Kafka - Giao tiếp bất đồng bộ & Saga](#8-apache-kafka)
9. [MySQL & JPA/Hibernate - Lưu trữ dữ liệu](#9-mysql--jpahibernate)
10. [Redis - Bộ nhớ đệm & Rate limiting](#10-redis)
11. [JWT - Xác thực người dùng](#11-jwt)
12. [Observability - Prometheus, Grafana, ELK, Zipkin](#12-observability)
13. [Docker & Docker Compose - Đóng gói & chạy](#13-docker--docker-compose)
14. [Các công cụ phụ trợ](#14-các-công-cụ-phụ-trợ)
15. [Tổng kết: bảng tra nhanh](#15-tổng-kết-bảng-tra-nhanh)

---

## 1. Bức tranh lớn: vì sao lại là Microservices?

**Ví dụ đời thường:** Hãy tưởng tượng một nhà hàng.

- **Cách cũ (Monolith - khối nguyên):** Một đầu bếp duy nhất làm tất cả: nấu món chính,
  pha chế, làm tráng miệng, rửa bát. Nếu anh ta ốm, cả nhà hàng đứng. Muốn nấu nhiều món
  hải sản hơn thì... không thể chỉ nhân bản "phần nấu hải sản" của anh ta được.

- **Cách mới (Microservices):** Mỗi việc giao cho một người chuyên trách: một người làm
  món chính, một người pha chế, một người tráng miệng. Ai bận thì gọi thêm người *cùng vị
  trí đó* (scale riêng). Một người nghỉ, những người khác vẫn làm việc.

**Microservices** là cách chia một ứng dụng lớn thành nhiều dịch vụ nhỏ, mỗi dịch vụ:
- Làm **một việc** rõ ràng (đặt vé, thanh toán, kho vé...).
- Có **database riêng**.
- **Tự deploy, tự scale** độc lập.

**Trong dự án này:** Hệ thống bán vé được chia thành các service: `user`, `event`, `ticket`,
`inventory`, `order`, `payment`, `booking`. Mỗi service là một "đầu bếp chuyên trách".

**Bài toán nó giải quyết:**
- Khi có flash sale, lượng đặt hàng tăng đột biến → chỉ cần scale `order-service` và
  `inventory-service`, không cần đụng tới phần còn lại.
- `payment-service` lỗi → không kéo sập `event-service`.
- Nhiều nhóm lập trình viên có thể làm song song trên các service khác nhau.

**Cái giá phải trả:** Microservices phức tạp hơn nhiều (phải có danh bạ, cấu hình tập trung,
giao tiếp mạng, giám sát...). Chính vì thế mới sinh ra hàng loạt công nghệ bên dưới.

---

## 2. Java 21 & Spring Boot

### Java 21
Là **ngôn ngữ lập trình** nền tảng. Bản 21 là phiên bản LTS (Long-Term Support - hỗ trợ dài
hạn), ổn định và được dùng cho hệ thống chạy thật.

### Spring Boot (3.3.5)
**Lý thuyết:** Spring là một "bộ khung" (framework) giúp viết ứng dụng Java doanh nghiệp.
Spring Boot là phiên bản "ăn liền" của Spring: nó tự cấu hình sẵn rất nhiều thứ
(*auto-configuration*), nhúng sẵn web server, để bạn chỉ việc tập trung viết logic.

**Ví dụ đời thường:** Mua xe lắp ráp sẵn (Spring Boot) thay vì mua từng con ốc về tự lắp
(Spring thuần). Bạn lên xe lái được ngay.

**Bài toán nó giải quyết trong dự án:** Mọi service đều là một ứng dụng Spring Boot. Nó cho
phép tạo nhanh một service chạy độc lập (có sẵn web server bên trong), kết nối database,
expose REST API... với rất ít code cấu hình.

**Khái niệm cốt lõi nên nhớ:**
- **Dependency Injection (DI - tiêm phụ thuộc):** Bạn không tự tạo đối tượng, Spring tạo và
  "đưa" cho bạn dùng. Giống như bạn không tự xây nhà máy điện, chỉ cắm phích vào ổ là có điện.
- **Starter:** Các gói `spring-boot-starter-*` gom sẵn thư viện cho một mục đích (web, jpa,
  redis...). Khai báo một dòng là có đủ.

---

## 3. Eureka - Service Discovery

(thư viện: `spring-cloud-starter-netflix-eureka` — module `xxxx-discovery`)

**Bài toán:** Trong microservices, các service chạy ở nhiều nơi, IP/port có thể thay đổi,
và có thể có nhiều bản sao (instance) của cùng một service. Làm sao `order-service` biết
`payment-service` đang ở địa chỉ nào?

**Ví dụ đời thường:** Eureka giống **tổng đài danh bạ điện thoại**. Khi một service khởi
động, nó "gọi điện báo danh" với tổng đài: *"Tôi là payment-service, tôi ở số 8083."*
Khi service khác cần gọi payment, nó hỏi tổng đài *"payment-service ở đâu?"* thay vì phải
nhớ địa chỉ cứng.

**Lý thuyết:** Đây là mẫu **Service Discovery** (khám phá dịch vụ).
- **Service Registry:** nơi lưu danh sách service (chính là Eureka Server - `xxxx-discovery`).
- **Register:** service tự đăng ký khi khởi động.
- **Heartbeat:** service định kỳ "báo còn sống". Nếu im lặng quá lâu, Eureka xóa nó khỏi danh bạ.

**Trong dự án:** `xxxx-discovery` (port 8761) là Eureka Server. Tất cả service khác là Eureka
Client. Nhờ đó gateway mới định tuyến được bằng `lb://xxxx-booking-service` (lb = load balance)
mà không cần biết IP thật.

---

## 4. Spring Cloud Config

(thư viện: `spring-cloud-starter-config` / config server — module `xxxx-config`)

**Bài toán:** Có gần 10 service, mỗi cái cần cấu hình (địa chỉ DB, Redis, Kafka...). Nếu mỗi
service tự giữ cấu hình riêng, khi đổi mật khẩu DB bạn phải sửa 10 chỗ và build lại 10 lần.

**Ví dụ đời thường:** Thay vì mỗi phòng trong tòa nhà có một bảng điện riêng, ta đặt **một
phòng kỹ thuật trung tâm** quản lý điện cho cả tòa. Cần đổi gì, vào một chỗ là xong.

**Lý thuyết:** Đây là mẫu **Externalized Configuration** (đưa cấu hình ra ngoài code) và
**Centralized Configuration** (tập trung một chỗ). Cấu hình tách khỏi mã nguồn → đổi cấu hình
không cần build lại ứng dụng.

**Trong dự án:** `xxxx-config` (port 8888) là Config Server. Nó đọc các file trong thư mục
`environment/config-repo/`:
- `application.yml`: cấu hình **chung** cho mọi service.
- `xxxx-<tên>-service-dev.yml`: cấu hình **riêng** từng service.

Khi một service khởi động, nó "tải" cấu hình của mình từ Config Server về (qua
`spring.config.import`). Một chỗ sửa, mọi service nhận được.

**Điểm bảo mật quan trọng:** Config Server giúp gom cấu hình, nhưng **secret không nên hardcode**
trong source code hoặc file cấu hình commit lên Git. Các giá trị như mật khẩu MySQL, JWT secret,
`encrypt.key` và secret VNPay được lấy từ biến môi trường (`MYSQL_ROOT_PASSWORD`, `JWT_SECRET`,
`ENCRYPT_KEY`, `VNPAY_SECRET_KEY`...). File `.env.example` chỉ ghi tên biến và giá trị mẫu, không
chứa secret thật.

> **Ví dụ đời thường:** `config-repo` giống bảng hướng dẫn vận hành; còn secret giống chìa khóa két.
> Bảng hướng dẫn có thể để cho đội kỹ thuật xem, nhưng chìa khóa két phải để trong két/biến môi
> trường/secret manager, không dán lên bảng.

---

## 5. Spring Cloud Gateway

(thư viện: `spring-cloud-starter-gateway` — module `xxxx-gateway`)

**Bài toán:** Client (app, web) không nên gọi thẳng vào 7 service ở 7 port khác nhau. Vừa
rối, vừa khó bảo mật, vừa lộ cấu trúc bên trong.

**Ví dụ đời thường:** Gateway giống **lễ tân / bảo vệ ở cổng tòa nhà**. Mọi khách đều vào
qua cổng chính. Lễ tân kiểm tra giấy tờ (xác thực), rồi chỉ đường tới đúng phòng ban
(định tuyến). Khách không tự ý đi cửa sau vào từng phòng.

**Lý thuyết:** Đây là mẫu **API Gateway**. Nó là **điểm vào duy nhất** (single entry point)
và đảm nhận các việc "cắt ngang" (cross-cutting concerns):
- **Routing (định tuyến):** `/api/booking/**` → booking-service, `/api/payment/**` → payment...
- **Authentication (xác thực):** kiểm tra JWT trước khi cho qua.
- **Rate limiting (giới hạn tần suất):** chặn spam (dùng Redis).
- **Load balancing:** chia tải giữa các instance (kết hợp Eureka).

**Trong dự án:** `xxxx-gateway` (port 8080) là cổng duy nhất ra ngoài. Nó định tuyến tới các
service qua tên Eureka (`lb://...`), kiểm tra JWT, và có danh sách `public-endpoints` (các
URL không cần đăng nhập như login, register, callback VNPay).

Khi chạy bằng Docker Compose, chỉ gateway publish port `8080` ra máy host/internet. MySQL, Redis,
Kafka, Config Server, Eureka, observability và các business service còn lại chỉ nằm trong Docker
network nội bộ. Cách này giảm bề mặt tấn công: người ngoài không thể gọi thẳng `payment-service`
hay truy cập MySQL, mà phải đi qua gateway để bị kiểm tra auth/rate limit.

---

## 6. OpenFeign

(thư viện: `spring-cloud-starter-openfeign`)

**Bài toán:** Khi `order-service` cần hỏi `payment-service`, nó phải gọi HTTP. Viết tay code
gọi HTTP (mở kết nối, ghép URL, parse JSON...) rất dài dòng và dễ sai.

**Ví dụ đời thường:** Feign giống **điện thoại có sẵn danh bạ phím nhanh**. Bạn chỉ cần khai
báo "tôi muốn gọi payment-service" bằng một interface Java, Feign lo hết phần quay số,
kết nối, nghe máy.

**Lý thuyết:** Đây là **Declarative REST Client** (client REST kiểu khai báo). Bạn chỉ *mô tả*
muốn gọi gì (qua interface + annotation), không cần *viết* cách gọi.

**Trong dự án:**
```
@FeignClient(name = "xxxx-payment-service", fallback = PaymentServiceClientFallback.class)
public interface PaymentServiceClient { ... }
```
- `name` là tên service trong Eureka → Feign tự tìm địa chỉ + chia tải.
- `fallback` là "phương án dự phòng" khi service kia chết (xem phần Resilience4j).

Các lời gọi đồng bộ trong dự án: order → payment, booking → ticket, booking → event.

---

## 7. Resilience4j

(thư viện: `resilience4j-spring-boot3`)

**Bài toán:** Trong hệ phân tán, một service chết có thể kéo theo cả dây chuyền. Ví dụ
payment chậm → order chờ → request dồn ứ → order cũng chết → gateway chết... Hiệu ứng
domino này gọi là **cascading failure** (sập dây chuyền).

**Ví dụ đời thường:** **Cầu dao điện (cầu chì)** trong nhà. Khi một thiết bị chập, cầu dao
tự ngắt để không cháy cả nhà. Sau một lúc, bạn thử bật lại xem đã ổn chưa.

**Lý thuyết - 3 cơ chế chính dùng trong dự án:**

- **Circuit Breaker (cầu dao):** Theo dõi tỉ lệ lỗi khi gọi một service. Nếu lỗi quá ngưỡng
  (dự án đặt 50%), nó "ngắt mạch": tạm dừng gọi service đó một thời gian (10s) và trả về
  fallback ngay, tránh chờ vô ích. Sau đó thử lại dè dặt (half-open).

- **Retry (thử lại):** Lỗi tạm thời (mạng chập chờn) thì thử lại vài lần (dự án: tối đa 3 lần,
  cách nhau 500ms) trước khi bỏ cuộc.

- **Bulkhead (vách ngăn khoang tàu):** Giới hạn số lời gọi đồng thời (dự án: 25). Giống tàu
  thủy chia thành nhiều khoang kín — một khoang thủng nước không tràn sang khoang khác.

**Trong dự án:** Mỗi Feign client đều có `fallback` — khi cầu dao ngắt hoặc service chết,
code fallback chạy thay (ví dụ trả về thông báo "tạm thời không thanh toán được") thay vì
để cả luồng treo. Cấu hình chung nằm trong `environment/config-repo/application.yml`.

---

## 8. Apache Kafka

(thư viện: `spring-kafka`)

**Bài toán:** Một số việc không nên bắt người dùng chờ. Khi đặt hàng, ta cần: trừ kho, xử lý
thanh toán, tạo booking... Nếu làm tuần tự và đồng bộ (gọi Feign chờ từng bước), người dùng
phải đợi rất lâu, và nếu một bước lỗi thì rối tung. Ngoài ra các service bị **ràng buộc chặt**
(coupling): order phải biết và chờ tất cả.

**Ví dụ đời thường:** Kafka giống **bưu điện / hộp thư**. Thay vì A phải gặp trực tiếp B mới
nói được chuyện (đồng bộ), A chỉ cần **gửi thư vào hòm thư** rồi đi làm việc khác. B rảnh lúc
nào thì lấy thư ra đọc và xử lý. A không cần chờ B, thậm chí không cần biết ai sẽ đọc thư.

**Lý thuyết:**
- **Message Broker / Event Streaming:** Kafka là nơi trung chuyển "sự kiện" (event).
- **Producer:** bên gửi sự kiện. **Consumer:** bên nhận. **Topic:** "hòm thư" theo chủ đề
  (ví dụ `order.placed`, `payment.completed`).
- **Bất đồng bộ (asynchronous):** gửi xong là xong, không chờ kết quả ngay.
- **Loose coupling (ràng buộc lỏng):** producer không cần biết ai sẽ tiêu thụ. Thêm consumer
  mới mà không sửa producer.

**Saga Pattern (rất quan trọng trong dự án này):**

Vì mỗi service có DB riêng, ta **không thể** dùng một transaction database duy nhất cho cả
luồng đặt hàng (không có "khóa" chung qua nhiều DB). Giải pháp là **Saga**: chia giao dịch
lớn thành nhiều bước nhỏ, mỗi bước ở một service, nối với nhau qua sự kiện Kafka. Nếu một
bước thất bại, các bước trước được **bù trừ** (compensation - làm thao tác ngược lại).

Luồng đặt vé trong dự án (Saga điều phối bởi order-service):
```
order.placed → inventory giữ vé → inventory.reserved
            → payment xử lý → payment.completed
            → order.confirmed → booking tạo chỗ
```
Nếu hết vé → `inventory.reserve-failed` → hủy đơn.
Nếu thanh toán lỗi → `payment.failed` → `order.cancelled` + **trả vé về kho** (bù trừ).

Đây là cách giữ dữ liệu nhất quán giữa nhiều database mà không cần transaction toàn cục.

---

## 9. MySQL & JPA/Hibernate

(thư viện: `spring-boot-starter-data-jpa`, `mysql-connector-j`)

### MySQL
**Bài toán:** Cần nơi lưu dữ liệu bền vững (đơn hàng, vé, người dùng...) một cách có cấu trúc.

**Lý thuyết:** MySQL là **cơ sở dữ liệu quan hệ (RDBMS)** — dữ liệu lưu trong các bảng có cột,
hàng, và quan hệ giữa các bảng. Hỗ trợ **ACID** (đảm bảo giao dịch đúng đắn, không mất dữ liệu).

**Trong dự án:** Áp dụng **Database-per-Service** — mỗi service một database riêng
(`order_db`, `payment_db`, `booking_db`...). Điều này giữ các service độc lập thật sự: đổi
schema của service này không ảnh hưởng service kia. (Đổi lại, không có khóa ngoại xuyên DB —
đó là lý do phải dùng Saga ở mục 8.)

### JPA / Hibernate
**Bài toán:** Trong Java ta làm việc với *đối tượng* (object), còn database làm việc với
*bảng* (table). Dịch qua lại giữa hai thế giới này (viết SQL, map kết quả về object) rất mệt.

**Ví dụ đời thường:** JPA/Hibernate là **người phiên dịch** giữa "tiếng Java" (đối tượng) và
"tiếng Database" (bảng). Bạn nói chuyện bằng object, nó tự dịch sang SQL.

**Lý thuyết:** Đây là **ORM (Object-Relational Mapping)**.
- **JPA** là *bộ tiêu chuẩn* (đặc tả). **Hibernate** là *bản hiện thực* phổ biến nhất của JPA.
- `@Entity` đánh dấu một class là một bảng; `@Id` là khóa chính; `@Column` là cột.

**Một kỹ thuật hay trong dự án - Optimistic Locking (`@Version`):**
Khi nhiều người cùng mua vé một lúc, có thể xảy ra tranh chấp dữ liệu. `@Version` thêm một cột
"phiên bản": ai lưu trước thì tăng version; người lưu sau thấy version đã đổi sẽ bị từ chối và
phải thử lại. Giống như "ai chỉnh sửa tài liệu Google Docs trước thì được, người sau bị báo
đã có thay đổi". Dùng cho `OrderEntity`, `InventoryAllotDetailEntity`...

---

## 10. Redis

(thư viện: `spring-boot-starter-data-redis`, `...-redis-reactive`)

**Bài toán:** Có những dữ liệu được đọc rất nhiều và cần cực nhanh; truy vấn MySQL liên tục
sẽ chậm và nặng. Ngoài ra cần một chỗ chung để đếm số request (chống spam).

**Ví dụ đời thường:** Redis là **cuốn sổ tay để ngay trên bàn**. Thông tin hay dùng thì ghi
ra sổ tay (đọc tức thì) thay vì mỗi lần phải xuống kho lưu trữ (MySQL) lục tìm.

**Lý thuyết:** Redis là **in-memory data store** — lưu dữ liệu trong RAM nên cực nhanh
(micro giây). Thường dùng làm:
- **Cache (bộ nhớ đệm):** lưu tạm kết quả hay dùng.
- **Rate limiting:** đếm số lần gọi API trong một khoảng thời gian.
- **Phối hợp giữa các instance:** vì nó là chỗ chung mọi service truy cập được.

**Trong dự án:**
- Các business service dùng Redis làm cache.
- `xxxx-gateway` dùng Redis (bản reactive) để **giới hạn tần suất** request — nhiều instance
  gateway cùng đếm trên một Redis nên giới hạn mới chính xác.

---

## 11. JWT

(thư viện: `jjwt` — trong `xxxx-gateway`)

**Bài toán:** Sau khi đăng nhập, làm sao server biết các request sau là của ai mà không bắt
đăng nhập lại mỗi lần? Và trong microservices, làm sao xác thực mà không cần lưu phiên
(session) tập trung?

**Ví dụ đời thường:** JWT giống **vé xem phim đã đóng dấu**. Sau khi mua vé (đăng nhập), bạn
cầm vé đi. Nhân viên chỉ cần nhìn con dấu là biết vé thật hay giả, không cần gọi về quầy hỏi.
Trên vé ghi sẵn: tên phim, suất chiếu, ghế (thông tin người dùng).

**Lý thuyết:** JWT (JSON Web Token) là một chuỗi gồm 3 phần: Header.Payload.Signature.
- **Payload** chứa thông tin (userId, vai trò, hạn dùng).
- **Signature** (chữ ký) được tạo bằng khóa bí mật của server. Nếu ai sửa nội dung, chữ ký
  sai ngay → phát hiện giả mạo.
- **Stateless (không trạng thái):** server không cần lưu phiên; mọi thông tin nằm trong token.
  Rất hợp với microservices vì không cần chia sẻ session giữa các service.

**Trong dự án:** Gateway kiểm tra JWT cho mọi request (trừ `public-endpoints` như login,
register, callback VNPay). Token hợp lệ mới được đi tiếp vào các service bên trong.

JWT chỉ an toàn nếu khóa ký đủ mạnh và được giữ bí mật. Vì vậy `gateway.jwt.secret` không còn có
giá trị mặc định hardcode trong code; khi chạy thật phải truyền `JWT_SECRET` từ môi trường. Nếu lộ
JWT secret, kẻ tấn công có thể tự ký token giả.

---

## 12. Observability

> "Observability" = khả năng quan sát: khi hệ thống có vấn đề, ta nhìn vào đâu để biết
> chuyện gì đang xảy ra? Với hàng chục service, không thể "đoán mò". Có **3 trụ cột**:
> Metrics (số liệu), Logs (nhật ký), Traces (dấu vết).

**Ví dụ đời thường:** Giống bảng điều khiển và hộp đen của máy bay. Phi công không nhìn thấy
động cơ, nhưng nhờ đồng hồ (metrics), nhật ký bay (logs), và đường bay ghi lại (traces) mà
biết mọi thứ có ổn không.

### Micrometer + Prometheus + Grafana (Metrics - số liệu)
- **Micrometer:** thư viện trong mỗi service, đo đạc các chỉ số (số request, độ trễ, bộ nhớ...)
  và phơi ra ở `/actuator/prometheus`.
- **Prometheus:** định kỳ (10s/lần trong dự án) "đi thu thập" (scrape) số liệu từ các service
  rồi lưu lại theo thời gian. Đây là mô hình **pull** (Prometheus chủ động kéo về).
- **Grafana:** vẽ số liệu thành biểu đồ đẹp, dễ nhìn, đặt cảnh báo.

**Bài toán:** Trả lời "service nào đang chậm? CPU/RAM thế nào? request lỗi tăng không?"

### ELK Stack (Logs - nhật ký)
**Elasticsearch + Logstash + Kibana:**
- **Logstash:** nhận log từ các service (qua TCP, định dạng json_lines), xử lý, gắn nhãn.
- **Elasticsearch:** lưu trữ và đánh chỉ mục log để **tìm kiếm cực nhanh**.
- **Kibana:** giao diện để tìm và xem log.

**Bài toán:** Với nhiều service, log nằm rải rác. ELK **gom log về một chỗ** (centralized
logging) để tìm kiếm xuyên suốt. Dự án còn gắn `traceId`/`spanId` vào log để nối với tracing.

### Zipkin (Distributed Tracing - dấu vết phân tán)
**Bài toán:** Một request đi qua gateway → order → inventory → payment... Nếu chậm, **chậm ở
khâu nào?** Khó biết nếu chỉ nhìn từng service riêng lẻ.

**Ví dụ đời thường:** Như mã vận đơn khi mua hàng online — bạn theo dõi gói hàng qua từng
trạm. Tracing gắn một **traceId** cho mỗi request, đi tới đâu cũng mang theo, nên ta dựng lại
được toàn bộ hành trình và biết khâu nào tốn thời gian.

**Lý thuyết:** Mỗi chặng là một **span**; nhiều span cùng một **traceId** ghép lại thành bức
tranh end-to-end. Dự án đẩy span tới Zipkin (`/api/v2/spans`), lưu vào Elasticsearch.

---

## 13. Docker & Docker Compose

**Bài toán:** "Máy tôi chạy được mà máy bạn lỗi!" — do khác phiên bản Java, thiếu thư viện,
khác cấu hình OS... Và chạy tay gần 20 thành phần (10 service + MySQL + Redis + Kafka + ELK...)
là cực hình.

**Ví dụ đời thường:**
- **Docker** giống **container vận chuyển hàng hóa tiêu chuẩn**. Đóng gói ứng dụng *cùng tất
  cả những thứ nó cần* (Java, thư viện, cấu hình) vào một "thùng" chạy giống hệt nhau ở mọi
  nơi — máy bạn, máy tôi, hay VPS.
- **Docker Compose** giống **bản nhạc cho dàn nhạc**: một file mô tả tất cả "nhạc công"
  (container) và cách họ phối hợp, khởi động theo thứ tự nào.

**Lý thuyết:**
- **Container:** đơn vị đóng gói nhẹ, cô lập, chứa app + môi trường chạy. Khác máy ảo (VM) ở
  chỗ container chia sẻ nhân hệ điều hành nên nhẹ và khởi động nhanh hơn nhiều.
- **Image:** "khuôn" để tạo container (xem `Dockerfile` của mỗi service).
- **docker-compose.yml:** khai báo nhiều service, mạng, volume, thứ tự phụ thuộc
  (`depends_on` + `healthcheck`).

**Trong dự án:** File `docker-compose.yml` ở thư mục gốc dựng toàn bộ hệ thống bằng một lệnh
`docker-compose up -d`, đảm bảo đúng thứ tự: hạ tầng → discovery → config → gateway → service.

Compose cũng là nơi nối biến môi trường vào container. Những secret bắt buộc dùng cú pháp
`${TEN_BIEN:?TEN_BIEN is required}` để nếu quên set thì container không khởi động âm thầm với
mật khẩu mặc định yếu. Với môi trường dev, tạo `.env` từ `.env.example`; với VPS/production, nên
set biến môi trường thật trên server hoặc dùng secret manager.

Một thay đổi quan trọng cho production là **không publish port nội bộ**. Trước đây có thể mở MySQL,
Redis, Kafka, Grafana... ra host để tiện dev; hiện tại compose chỉ publish `8080:8080` của gateway.
Muốn xem các công cụ nội bộ thì dùng SSH tunnel, VPN, hoặc profile dev riêng thay vì mở thẳng ra
internet.

---

## 14. Các công cụ phụ trợ

- **Maven** (`pom.xml`): công cụ quản lý thư viện và build dự án. Dự án dùng cấu trúc
  **multi-module** — một pom cha quản lý nhiều module con, dùng chung phiên bản thư viện.
- **Lombok:** giảm code lặp. Annotation như `@Data`, `@Builder` tự sinh getter/setter,
  constructor... lúc biên dịch. Giống "máy đánh chữ tự động" cho phần code nhàm chán.
- **SpringDoc OpenAPI (Swagger UI):** tự sinh tài liệu API và trang web để thử API trực tiếp.
  Dự án gom Swagger của mọi service về một chỗ tại gateway.
- **Spring Boot Actuator:** mở các endpoint giám sát (`/actuator/health`, `/actuator/prometheus`)
  để biết service còn sống không và cung cấp số liệu cho Prometheus.
- **Spring Validation:** kiểm tra dữ liệu đầu vào (`@Valid`, `@NotNull`...) trước khi xử lý.
- **xxxx-common:** thư viện nội bộ dùng chung (định nghĩa event, hằng số Kafka topic, kiểu
  ApiResponse chuẩn...) để các service nói "cùng một ngôn ngữ".

---

## 15. Tổng kết: bảng tra nhanh

| Công nghệ | Nhóm | Giải quyết bài toán gì |
|-----------|------|------------------------|
| Spring Boot | Nền tảng | Tạo nhanh từng service độc lập |
| Eureka | Hạ tầng MS | Danh bạ: service tìm nhau qua tên |
| Spring Cloud Config | Hạ tầng MS | Cấu hình tập trung một chỗ |
| Spring Cloud Gateway | Hạ tầng MS | Cổng vào duy nhất: routing, auth, rate limit |
| OpenFeign | Giao tiếp | Gọi service khác như gọi hàm (đồng bộ) |
| Resilience4j | Độ bền | Chống sập dây chuyền (cầu dao, retry, bulkhead) |
| Kafka | Giao tiếp | Sự kiện bất đồng bộ + điều phối Saga |
| MySQL | Dữ liệu | Lưu trữ quan hệ, mỗi service một DB |
| JPA/Hibernate | Dữ liệu | Dịch giữa object Java và bảng SQL |
| Redis | Hiệu năng | Cache nhanh + rate limiting cho gateway |
| JWT | Bảo mật | Xác thực không trạng thái (stateless) |
| Prometheus/Grafana | Quan sát | Đo & vẽ số liệu (metrics) |
| ELK | Quan sát | Gom & tìm kiếm log tập trung |
| Zipkin | Quan sát | Lần theo request xuyên service (tracing) |
| Docker/Compose | Vận hành | Đóng gói & chạy đồng nhất mọi nơi |

---

### Một câu tóm gọn cho cả hệ thống

> Người dùng vào qua **Gateway** (có **JWT** gác cửa). Gateway hỏi **Eureka** xem service ở
> đâu rồi chuyển tiếp. Mỗi service là một **Spring Boot** app, lấy cấu hình từ **Config Server**,
> lưu dữ liệu vào **MySQL** riêng (qua **JPA**), dùng **Redis** để tăng tốc. Việc nhanh thì
> gọi trực tiếp qua **Feign** (có **Resilience4j** làm cầu dao); việc dài hơi như đặt vé thì
> trao đổi qua **Kafka** theo mẫu **Saga**. Toàn bộ được đóng trong **Docker**, và được theo
> dõi bằng **Prometheus/Grafana** (số liệu), **ELK** (log), **Zipkin** (dấu vết).


---
---

# PHẦN II: BÀI TOÁN BÁN VÉ TẢI CAO & CÁCH DỰ ÁN XỬ LÝ

> Phần I giải thích "công nghệ là gì". Phần II này trả lời câu hỏi quan trọng hơn:
> **Bán vé tải cao (flash sale) khó ở chỗ nào, và dự án này dùng những kỹ thuật gì để xử lý?**
>
> Mỗi mục theo cấu trúc: **Bài toán → Vì sao khó → Dự án xử lý thế nào (code thật) → Lưu ý/giới hạn.**

## Bối cảnh: chuyện gì xảy ra khi mở bán 1.000 vé cho 100.000 người?

Tưởng tượng một concert hot: đúng 12h00 mở bán 1.000 vé, nhưng có 100.000 người cùng bấm
"Mua" trong vài giây. Đây là **flash sale** — và nó sinh ra một loạt bài toán mà ngày thường
không bao giờ gặp:

1. **Oversell (bán quá số vé)** — bán 1.001 vé khi chỉ có 1.000.
2. **Race condition (tranh chấp)** — nhiều request cùng sửa một con số tồn kho.
3. **Tải đột biến (spike)** — database "ngộp" vì 100.000 truy vấn/giây.
4. **Double-submit (bấm nhiều lần)** — một người bấm mua 5 lần vì sốt ruột.
5. **Lỗi giữa chừng** — đã giữ vé nhưng thanh toán lỗi → vé bị "kẹt".
6. **Trải nghiệm công bằng** — ai đến trước được mua trước.

Lần lượt từng cái nhé.

---

## Bài toán 1: Oversell & Race Condition (quan trọng nhất)

### Vì sao khó
Giả sử còn đúng **1 vé**. Hai người A và B bấm mua cùng lúc:

```
Thời điểm   Request A                Request B
  T1        đọc tồn kho = 1
  T2                                 đọc tồn kho = 1
  T3        thấy 1 >= 1 → OK
  T4                                 thấy 1 >= 1 → OK
  T5        ghi tồn kho = 0
  T6                                 ghi tồn kho = 0   <-- BÁN 2 VÉ TỪ 1 VÉ!
```

Đây là **race condition**: kết quả sai vì hai thao tác "đọc rồi ghi" xen kẽ nhau. Ngày
thường (ít người) gần như không xảy ra; nhưng flash sale thì xảy ra liên tục.

### Dự án xử lý thế nào

Dự án dùng **2 lớp phòng thủ** trong `InventoryServiceImpl.reserveStock()`:

**Lớp 1 — Redis Distributed Lock (khóa phân tán):**
```java
String lockKey = LOCK_KEY_PREFIX + ticketDetailId;       // "lock:inventory:123"
Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "locked", LOCK_TIMEOUT);    // 5 giây
if (locked == null || !locked) {
    throw new BusinessException("SYSTEM_BUSY", "System busy, please retry");
}
```
- `setIfAbsent` = "chỉ đặt nếu chưa có" (lệnh `SETNX` của Redis). Đây là thao tác **nguyên tử**
  (atomic): chỉ **một** request giành được khóa cho mỗi `ticketDetailId`, các request khác
  bị từ chối ngay với "SYSTEM_BUSY".
- **Ví dụ đời thường:** giống **chìa khóa phòng thử đồ duy nhất**. Ai cầm được chìa thì vào,
  người khác phải đợi. Vì cả hệ thống dùng chung một Redis, nên dù có nhiều instance
  inventory-service, tất cả vẫn tranh **cùng một** chìa → không ai chen ngang được.
- `LOCK_TIMEOUT = 5s`: khóa tự hết hạn sau 5 giây, phòng trường hợp service giữ khóa bị chết
  mà không kịp trả → tránh kẹt khóa vĩnh viễn (deadlock).

**Lớp 2 — Kiểm tra & trừ kho trong vùng khóa:**
```java
// (đang giữ khóa)
int availableStock = ...;                  // đọc tồn khả dụng từ Redis
if (availableStock < quantity) {
    return ...success(false)...;           // hết vé → từ chối
}
Long newAvailable = redisTemplate.opsForValue()
        .decrement(STOCK_AVAILABLE_KEY..., quantity);   // trừ NGUYÊN TỬ
```
Vì bước "đọc → kiểm tra → trừ" nằm gọn **bên trong khóa**, không request nào chen vào giữa
được. Race condition ở trên bị triệt tiêu. Cuối cùng khóa được trả trong khối `finally`
(luôn chạy kể cả khi có lỗi).

> **Lý thuyết liên quan:** đây là vùng **critical section** (đoạn tới hạn) — đoạn code mà tại
> một thời điểm chỉ được phép một luồng chạy. Trong một máy đơn, ta dùng `synchronized`; nhưng
> microservices chạy nhiều máy nên cần **khóa phân tán** (distributed lock) đặt ở Redis — chỗ
> chung mà mọi máy đều thấy.

### ✅ Đã nâng cấp: khóa an toàn theo chủ sở hữu (owner-safe)
Trước đây code trả khóa bằng `redisTemplate.delete(lockKey)` **không kiểm tra chủ sở hữu** —
rủi ro: nếu request A xử lý lâu hơn 5s, khóa A tự hết hạn, B giành được khóa, rồi A xong và
xóa luôn khóa của B → B mất khóa oan, hai request cùng vào vùng tới hạn.

Đã khắc phục bằng `DistributedLockService` (`com.xxxx.inventory.lock`):
- **Acquire**: `SET key <token-ngẫu-nhiên> NX PX ttl` — mỗi lần giữ khóa gắn một token định
  danh chủ sở hữu.
- **Release**: chạy **Lua script nguyên tử** "so token rồi mới xóa" — chỉ đúng chủ sở hữu mới
  xóa được khóa của mình:
  ```lua
  if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end
  ```
`reserveStock()` và `releaseStock()` đã chuyển sang dùng service này. Có 11 unit test bao phủ
các tình huống: giành/không giành được khóa, token duy nhất mỗi lần, trả khóa đúng/sai chủ
sở hữu, và đảm bảo luôn trả khóa kể cả khi action ném lỗi.

> Bước nâng cấp xa hơn (khi cần): dùng thư viện **Redisson** để có thêm auto-renew (gia hạn
> khóa khi tác vụ còn chạy) và reentrant lock.

---

## Bài toán 2: Tải đột biến lên Database

### Vì sao khó
MySQL rất tốt cho tính đúng đắn, nhưng mỗi truy vấn tốn vài mili giây và số kết nối có giới
hạn (thường vài chục - vài trăm). 100.000 request/giây dội thẳng vào MySQL sẽ làm nó nghẽn,
chậm, rồi sập. Mà flash sale chỉ "nóng" vài phút — không đáng để mua dàn DB khổng lồ.

### Dự án xử lý thế nào

**Đếm tồn kho trên Redis thay vì MySQL.** Tồn kho khả dụng được giữ ở key Redis
`stock:available:{ticketDetailId}`, và mỗi lần giữ vé chỉ là một lệnh `DECR` trong RAM
(micro-giây), thay vì `UPDATE` trên MySQL (mili-giây + tranh khóa hàng).

**Đọc có cache, có fallback** trong `getStockLevel()`:
```java
String cachedStock = redisTemplate.opsForValue().get(cacheKey);
if (cachedStock != null) {
    ... trả về ngay từ Redis (cache hit) ...
}
// cache miss → tính từ DB rồi cache lại
```
- **Ví dụ đời thường:** Redis là **bảng ghi số vé còn lại treo trước cửa rạp**. Khách nhìn
  bảng là biết ngay, không cần gõ cửa phòng kế toán (MySQL) hỏi từng lần.

**MySQL vẫn là "sổ cái" sự thật:** mỗi lần giữ/trả vé đều ghi một bản ghi
`InventoryAllotDetailEntity` (type = RESERVE / RELEASE). Redis lo tốc độ, MySQL lo lưu vết
lâu dài và đối soát.

> **Lý thuyết:** đây là mẫu **Cache-Aside** (đọc cache trước, miss thì xuống DB rồi nạp lại
> cache) kết hợp **read/write tách vai trò**: việc "nóng" (đếm) làm trên Redis, việc "bền"
> (lưu trữ) làm trên MySQL.

### ✅ Đã nâng cấp: tính tồn kho từ DB + tự phục hồi Redis
Trước đây `calculateTotalStockFromDb()` và `calculateAvailableStockFromDb()` **trả về 0**
(mới là khung) → nếu Redis mất dữ liệu, fallback sẽ hiểu nhầm "hết vé".

Đã khắc phục:
- Thêm truy vấn tổng hợp `sumQuantityByType(ticketDetailId, type)` trong repository.
- **Tổng tồn kho** = tổng các bản ghi `ALLOT`; **tồn khả dụng** = `ALLOT - RESERVE + RELEASE`
  (kẹp về 0 nếu âm). DB là sổ cái sự thật.
- Thêm API **nạp tồn kho khi mở bán** `POST /api/inventory/stock/initialize` (ghi bản ghi
  `ALLOT` + nạp Redis), **idempotent** nhờ kiểm tra đã có `ALLOT` chưa.
- `reserveStock()` giờ **tự phục hồi**: nếu key Redis trống (vừa khởi động/bị xóa), tự nạp lại
  từ DB rồi mới giữ vé — không còn hiểu nhầm hết vé.

Có 7 unit test bổ sung phủ: tính từ DB lúc cache miss, ghi cache lại, kẹp không âm, nạp tồn
kho idempotent, và tự phục hồi khi Redis trống.

---

## Bài toán 3: Idempotency - chống xử lý trùng

### Vì sao khó
Trên mạng, một request có thể bị gửi/nhận **nhiều lần**: người dùng bấm "Mua" 3 lần vì sốt
ruột; mạng chập chờn khiến client tự gửi lại; Kafka giao một message **ít nhất một lần**
(at-least-once) nên consumer có thể nhận trùng. Nếu mỗi lần nhận đều trừ kho/trừ tiền thì
hỏng.

**Ví dụ đời thường:** nút thang máy — bấm 10 lần thang vẫn chỉ đến một lần. Ta muốn các thao
tác quan trọng cũng "bấm nhiều lần, hiệu lực một lần". Tính chất đó gọi là **idempotency**
(tính lũy đẳng).

### Dự án xử lý thế nào (rất nhất quán, đây là điểm mạnh của dự án)

**Giữ vé — kiểm tra trùng theo (orderId + ticketDetailId):**
```java
Optional<...> existing = inventoryAllotDetailRepository
        .findByOrderIdAndTicketDetailId(orderId, ticketDetailId);
if (existing.isPresent()) {
    // đã giữ rồi → trả về kết quả cũ, KHÔNG trừ kho lần nữa
    return ...reservedQuantity(existing.get().getInventorNum())...;
}
```

**Thanh toán — khóa idempotency theo orderId** (`PaymentServiceImpl`):
```java
String idempotencyKey = request.getOrderId();
Optional<...> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
if (existing.isPresent()) {
    return ...existing transaction...;   // trả giao dịch cũ, không tạo mới
}
```

**Callback VNPay — chặn xử lý lại giao dịch đã xong:**
```java
if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
    return "ALREADY_PROCESSED";          // không bắn event Kafka lần 2
}
```

**Tra cứu giao dịch VNPay — dùng `txnRef` có index thay vì quét toàn bảng:**
```java
String txnRef = transactionId.substring(transactionId.length() - 12);
PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
        .transactionId(transactionId)
        .txnRef(txnRef)
        .build();

Optional<PaymentTransactionEntity> found = paymentRepository.findByTxnRef(txnRef);
```

VNPay gửi lại `vnp_TxnRef` trong callback/return URL. Nếu mỗi lần nhận callback lại
`findAll().stream()` để dò giao dịch thì bảng càng lớn càng chậm. Lưu `txnRef` thành cột riêng,
đặt unique index, rồi query `findByTxnRef` giúp database tìm đúng bản ghi bằng index.

**Địa chỉ IP và return URL trong VNPay:**
- `vnp_ReturnUrl` trỏ về gateway: `/api/payment/vnpay-return`, vì gateway là cửa public duy nhất.
- `VNPAY_RETURN_URL` phải đổi sang domain public thật khi lên VPS.
- `vnp_IpAddr` lấy từ request thật (`X-Forwarded-For`, `X-Real-IP`, fallback `remoteAddr`) thay vì
  cố định `127.0.0.1`, để VNPay nhận đúng IP phía người dùng/proxy.

**Chốt chặn ở tầng database (lớp bảo hiểm cuối):** các cột được đặt **UNIQUE** —
`inventor_no` (kho), `idempotency_key`, `transaction_id` & `txn_ref` (thanh toán), `order_no` (đơn hàng).
Kể cả nếu logic phía trên lọt, database vẫn từ chối bản ghi trùng.

> **Lý thuyết:** một thao tác idempotent là thao tác mà thực hiện 1 lần hay N lần đều cho
> **cùng một kết quả trạng thái**. Mẹo phổ biến: gắn cho mỗi thao tác một "khóa idempotency"
> duy nhất rồi kiểm tra trước khi làm.

---

## Bài toán 4: Giao dịch xuyên nhiều service (Saga + Compensation)

### Vì sao khó
Một lần đặt vé gồm: tạo đơn (order_db) → giữ kho (inventory_db) → thu tiền (payment_db) →
tạo chỗ (booking_db). Bốn database khác nhau → **không thể** dùng một transaction chung
("tất cả thành công hoặc tất cả hủy" kiểu cổ điển không áp dụng được qua nhiều DB). Vậy nếu
giữ kho xong nhưng thanh toán lỗi thì sao? Vé sẽ bị "giam" và không ai mua được.

### Dự án xử lý thế nào — Saga có bù trừ

`OrderServiceImpl` đóng vai **nhạc trưởng** (orchestrator). Nó không gọi trực tiếp tuần tự,
mà phản ứng theo từng sự kiện Kafka và cập nhật `sagaStatus`:

| Sự kiện nhận được | Hành động trong order-service | sagaStatus |
|-------------------|-------------------------------|------------|
| (bắt đầu) | tạo đơn PENDING, bắn `order.placed` | STARTED |
| `inventory.reserved` | đơn → INVENTORY_RESERVED | INVENTORY_OK |
| `inventory.reserve-failed` | đơn → CANCELLED | FAILED |
| `payment.completed` | đơn → CONFIRMED, bắn `order.confirmed` | COMPLETED |
| `payment.failed` | đơn → CANCELLED, **bắn `order.cancelled` với `compensationRequired=true`** | COMPENSATING |

**Điểm mấu chốt là compensation (bù trừ).** Khi thanh toán lỗi:
```java
// handlePaymentFailed(...)
OrderCancelledEvent event = new OrderCancelledEvent();
event.setCompensationRequired(true);     // <-- yêu cầu trả vé về kho
orderEventProducer.publishOrderCancelled(event);
```
inventory-service nghe `order.cancelled` này và **trả vé về kho** (`releaseStock` →
`INCR` lại Redis + ghi bản ghi RELEASE). Vé không bị giam.

- **Ví dụ đời thường:** đặt tour du lịch qua nhiều nhà cung cấp. Nếu khâu vé máy bay hỏng,
  bạn phải **hủy phòng khách sạn đã đặt** (thao tác ngược) để hoàn tiền. Saga chính là chuỗi
  "nếu bước sau hỏng thì làm ngược các bước trước".

> **Lý thuyết:** Saga thay "transaction toàn cục" bằng chuỗi **transaction cục bộ** (mỗi
> service tự lo DB của mình), nối bằng sự kiện. Mỗi bước có một **hành động bù trừ** để hoàn
> tác khi cần. Đổi lại tính nhất quán **tức thì** lấy nhất quán **sau cùng** (eventual
> consistency) — đơn có thể "đang xử lý" trong giây lát trước khi chốt.

### Phụ trợ: Kafka còn giúp "san tải" (load leveling)
Đặt vé qua Kafka là **bất đồng bộ**: order-service nhận yêu cầu, ghi đơn, bắn event rồi trả
lời người dùng ngay ("đơn đang xử lý"), không bắt họ đợi cả chuỗi kho-thanh toán-booking.
Lúc cao điểm, event dồn vào Kafka như **nước vào hồ chứa**, các consumer xử lý theo nhịp của
mình thay vì bị dội thẳng. Đây gọi là **load leveling** — Kafka làm "đệm" hấp thụ spike.

---

## Bài toán 5: Hàng đợi & phòng chờ công bằng (Waiting Room)

### Vì sao khó
Khi cầu vượt cung gấp trăm lần, cho tất cả vào cùng lúc là vô nghĩa (đa số sẽ thất vọng) và
nguy hiểm cho hệ thống. Cần một **phòng chờ có thứ tự**: ai vào trước phục vụ trước, số còn
lại xếp hàng hoặc bị từ chối sớm.

### Dự án đã chuẩn bị gì
Có entity `OrderQueueEntity` (`order_queue`) với các trường rõ ràng cho mục đích này:
- `token` (duy nhất) — "số thứ tự" của mỗi người trong hàng.
- `status`: 0=WAITING, 1=PROCESSING, 2=COMPLETED, 3=EXPIRED.
- `priority` — số nhỏ hơn = ưu tiên cao hơn.

Đây là khung cho mẫu **Queue-Based / Virtual Waiting Room**: thay vì xử lý ngay, người dùng
nhận một token vào hàng đợi, hệ thống "nhả" dần từng nhóm vào mua theo nhịp chịu tải được.

> **Ví dụ đời thường:** lấy số thứ tự ở ngân hàng. Bạn không chen vào quầy; bạn cầm số, ngồi
> chờ gọi tên. Quầy phục vụ đều đặn, không bị dồn.

### ⚠️ Giới hạn cần biết
Trong `OrderServiceImpl` mà tôi đọc, luồng `placeOrder()` hiện **xử lý thẳng** (tạo đơn + bắn
event ngay), **chưa thấy** dùng `order_queue` để chặn/xếp hàng. Nghĩa là "phòng chờ" mới có
**mô hình dữ liệu**, chưa có **logic vận hành**. Nếu mục tiêu là chịu flash sale thật, đây là
phần cần hiện thực hóa: cấp token khi vào, worker lấy từ hàng đợi theo `priority`/thứ tự, giới
hạn số đơn PROCESSING đồng thời.

---

## Bài toán 6: Tranh chấp ghi & phân mảnh tồn kho (Optimistic Lock + Bucket)

### Vì sao khó
Ngay cả khi đã có khóa, nếu *hàng nghìn* request cùng nhắm vào **một** dòng tồn kho, dòng đó
trở thành **điểm nóng** (hot row) — mọi người xếp hàng chờ đúng một ổ khóa, thông lượng bị bóp
nghẹt.

### Dự án xử lý / chuẩn bị thế nào

**(a) Optimistic Locking bằng `@Version`** — dùng ở `OrderEntity`,
`InventoryAllotDetailEntity`, `InventoryBucketConfigEntity`, `PaymentTransaction`, `Booking`.
- Cách hoạt động: mỗi bản ghi có cột `version`. Khi cập nhật, Hibernate thêm điều kiện
  `WHERE version = <giá trị đã đọc>`. Nếu ai đó đã sửa trước (version đổi), lệnh update không
  trúng dòng nào → ném lỗi → caller thử lại.
- **Ví dụ đời thường:** sửa chung một tài liệu Google Docs. Ai lưu trước thì được; người lưu
  sau bị báo "bản đã thay đổi, hãy tải lại". Không cần ai phải khóa cứng tài liệu.
- **Khác với khóa bi quan (pessimistic):** optimistic giả định "ít khi đụng nhau, cứ làm rồi
  kiểm tra lúc ghi" → nhẹ, không giữ khóa lâu; hợp với phần lớn cập nhật.

**(b) Bucket Config — phân mảnh tồn kho (sharding inventory):**
Entity `InventoryBucketConfigEntity` mô tả luật chia tồn kho thành nhiều "thùng" (bucket):
- `bucketNum`: số thùng — **kiểm soát mức độ song song**. Thay vì 1 dòng "1.000 vé", ta chia
  thành 10 thùng mỗi thùng 100 vé. 10 request có thể giữ vé ở 10 thùng khác nhau **cùng lúc**
  mà không tranh nhau → tăng thông lượng gấp nhiều lần.
- `maxDepthNum`/`minDepthNum`: dung lượng tối đa/tối thiểu mỗi thùng.
- `thresholdValue`, `backSourceProportion`, `backSourceStep`: luật **co/giãn** — khi một thùng
  cạn thì "nạp thêm" (back to source) từ nguồn chung; khi tồn thấp thì gộp/đóng bớt thùng.

> **Ví dụ đời thường:** thay vì **một quầy bán vé** với hàng dài 1.000 người, mở **10 quầy**
> mỗi quầy 100 vé. Khách chia ra 10 hàng, bán nhanh gấp 10. Khi một quầy hết, điều phối vé từ
> kho chung sang. Đây chính là tư tưởng "chia hot row thành nhiều bucket".

### ⚠️ Giới hạn cần biết
`InventoryServiceImpl` hiện chỉ **CRUD cấu hình bucket** (`getAllBucketConfigs`,
`createBucketConfig`), còn `reserveStock` vẫn dùng **một** key Redis + **một** khóa cho mỗi
`ticketDetailId` — tức **chưa** thực sự phân mảnh khi giữ vé. Bucket mới ở mức "thiết kế/cấu
hình", chưa nối vào đường giữ vé. Để khai thác, cần: chọn bucket (ví dụ theo hash userId),
giữ vé trên key Redis của bucket đó, và xử lý "nạp nguồn" khi bucket cạn.

---

## Tổng kết Phần II: bản đồ "bài toán → vũ khí"

| Bài toán tải cao | Kỹ thuật dùng | Trạng thái trong dự án |
|------------------|---------------|------------------------|
| Oversell / race condition | Redis distributed lock (owner-safe) + atomic DECR trong critical section | ✅ Đã nâng cấp khóa an toàn theo chủ sở hữu |
| Tải đột biến lên DB | Đếm tồn kho trên Redis + cache-aside; MySQL làm sổ cái | ✅ Có (fallback-from-DB + tự phục hồi Redis đã hoàn chỉnh) |
| Xử lý trùng (double submit, Kafka at-least-once) | Idempotency key + UNIQUE constraint nhiều tầng | ✅ Đã có, rất nhất quán |
| Giao dịch xuyên service | Saga orchestration + compensation (release stock) | ✅ Đã có |
| San tải spike | Kafka bất đồng bộ làm vùng đệm (load leveling) | ✅ Đã có |
| Phòng chờ công bằng | Queue + token + priority (waiting room) | 🟡 Mới có mô hình dữ liệu, chưa có logic |
| Hot row / thông lượng ghi | Optimistic lock (`@Version`) + bucket sharding | 🟡 `@Version` đã dùng; bucket mới ở mức cấu hình |
| Chống sập dây chuyền | Resilience4j (circuit breaker, retry, bulkhead, fallback) | ✅ Đã có |

**Ghi chú trung thực:** các mục 🟡 cho thấy dự án đã **thiết kế đúng hướng** cho tải cao (đã có
entity, cấu hình, cột version...) nhưng một số **logic vận hành chưa hoàn chỉnh** (waiting room
chưa chặn luồng, bucket chưa nối vào giữ vé, fallback tồn kho từ DB trả 0, lock chưa an toàn
tuyệt đối). Đây không phải lỗi sai — mà là các điểm cần hoàn thiện nếu muốn chịu tải flash sale
thật sự. Biết rõ ranh giới "đã có vs chưa có" sẽ giúp bạn ưu tiên đúng việc khi phát triển tiếp.

### Gợi ý thứ tự hoàn thiện (nếu mục tiêu là flash sale thật)
1. ~~**Khóa Redis an toàn** (token + Lua, hoặc Redisson)~~ — ✅ **ĐÃ XONG** (`DistributedLockService`).
2. ~~**Hiện thực tồn kho từ DB + nạp Redis lúc mở bán**~~ — ✅ **ĐÃ XONG** (`sumQuantityByType`, `initializeStock`, tự phục hồi trong `reserveStock`).
3. **Nối bucket vào đường giữ vé** — tăng thông lượng cho vé hot.
4. **Kích hoạt waiting room** — bảo vệ hệ thống và tạo trải nghiệm công bằng.

---

## Cập nhật 2026-06-02: Xác thực đúng chuẩn hơn cho microservices

Trước đó frontend phải nhập JWT gateway thủ công. Cách này khó dùng và không đúng vai trò của
hệ thống xác thực: người dùng không nên tự tạo/tự điền token; token phải được cấp sau khi đăng
nhập thành công.

### Cách mới đang chạy

1. Người dùng đăng nhập bằng email/password.
2. `user-service` kiểm tra password bằng BCrypt.
3. Nếu hợp lệ, `user-service` cấp:
   - `accessToken`: sống ngắn, dùng gọi API.
   - `refreshToken`: sống dài hơn, dùng lấy access token mới khi access token hết hạn.
4. Frontend tự lưu token và tự refresh khi gateway trả `401`.
5. Gateway validate JWT, đọc `roles`, rồi mới route request vào service nghiệp vụ.

### Vì sao dùng access token + refresh token?

Access token giống vé vào cổng trong thời gian ngắn. Nếu bị lộ, thời gian nguy hiểm nhỏ vì token
hết hạn nhanh. Refresh token giống phiếu đổi vé mới: sống dài hơn nhưng được lưu hash trong DB,
có thể revoke khi logout hoặc khi phát hiện bất thường.

### Vì sao password phải dùng BCrypt?

SHA-256 là hàm băm nhanh, phù hợp kiểm tra toàn vẹn dữ liệu, không phù hợp lưu mật khẩu. Kẻ tấn
công có thể thử hàng tỷ mật khẩu mỗi giây nếu lấy được hash. BCrypt chậm có chủ đích và có salt
riêng cho từng password, làm việc brute-force tốn kém hơn rất nhiều.

Dự án vẫn cho user cũ login bằng hash SHA-256, nhưng sau lần login thành công sẽ tự lưu lại bằng
BCrypt. Đây là cách migrate mềm: không bắt tất cả user đổi mật khẩu ngay, nhưng dần đưa dữ liệu
về chuẩn mới.

### Role và gateway authorization

JWT có claim `roles`. Gateway dùng claim này để chặn các API quản trị:
- `/api/employees/**` cần `ADMIN`.
- Các request ghi dữ liệu trên event, ticket, ticket detail, inventory cần `ADMIN`.

Frontend chỉ ẩn/hiện menu theo role để trải nghiệm dễ dùng hơn. Quyền thật sự vẫn phải chặn ở
backend/gateway, vì bất kỳ ai cũng có thể gọi API trực tiếp bằng Postman/curl.

### Gateway authenticate, Service authorize

JWT có claim `roles`, nhưng quyền nghiệp vụ không còn phụ thuộc hoàn toàn vào gateway. Mô hình mới chia rõ hai lớp:

- **Gateway authenticate**: kiểm tra JWT có chữ ký đúng, issuer đúng và chưa hết hạn trước khi cho request đi tiếp.
- **Gateway chống giả mạo identity**: xóa `X-User-Id`, `X-User-Email`, `X-User-Roles` do client gửi, rồi chỉ gắn metadata đã verify để log/debug.
- **Service authorize**: từng service verify lại `Authorization: Bearer <token>`, tạo principal nội bộ và dùng `@PreAuthorize` cho rule `ADMIN`/owner.
- **Shared security**: parser JWT, principal và filter dùng chung nằm trong `xxxx-common` để tránh mỗi service tự copy parser khác nhau.`r`n- **Public endpoint vẫn được kiểm thử**: login/register/refresh, VNPay callback/return và GET event listing chạy được khi không có JWT.

Cách này an toàn hơn vì nếu ai đó gọi thẳng vào service nội bộ, service vẫn tự reject request thiếu/invalid JWT. Gateway chỉ là lớp chặn đầu tiên, còn quyền thật sự nằm sát nghiệp vụ.
### Các biến môi trường auth

| Biến | Ý nghĩa |
|------|---------|
| `JWT_SECRET` | Khóa ký JWT, bắt buộc dài tối thiểu 32 ký tự. |
| `JWT_ISSUER` | Đơn vị phát hành token, mặc định `xxxx-user-service`. |
| `JWT_EXPIRATION_SECONDS` | Thời gian sống access token, mặc định 1800 giây. |
| `JWT_REFRESH_EXPIRATION_SECONDS` | Thời gian sống refresh token, mặc định 604800 giây. |

### Việc nên làm tiếp

- Thêm seed/bootstrap admin bằng biến môi trường để không phải sửa DB thủ công.
- Bổ sung integration test cho login -> refresh -> logout -> gateway authorization.
- Bổ sung test âm/dương cho service authorization: thiếu JWT, user thường gọi admin endpoint, owner A đọc tài nguyên của owner B.



---

## C?p nh?t 2026-06-08: v?n h�nh v� flash sale

### Docker Compose profiles

`docker-compose.yml` hi?n du?c t�ch th�nh 4 profile d? ch?y linh ho?t hon:

- `infra`: MySQL, Redis, Zookeeper, Kafka.
- `platform`: discovery, config, gateway.
- `business`: c�c service nghi?p v?.
- `observability`: Prometheus, Grafana, ELK, Zipkin.

Nh? v?y m�y dev c� th? ch? b?t `infra`, ho?c `infra + platform`; c�n khi c?n ch?y g?n production th� b?t
`infra + platform + business`. Observability l� nh�m t�y ch?n, m? khi c?n quan s�t s�u.

### Bucket sharding cho inventory hot

? du?ng gi? v�, `inventory-service` hi?n h? tr? chia t?n kho theo bucket khi c� `InventoryBucketConfigEntity`
 m?c d?nh:

- key t?n kho: `stock:available:{ticketDetailId}:{bucketIndex}`
- key kh�a: `lock:inventory:{ticketDetailId}:{bucketIndex}`
- ch?n bucket: hash theo `orderId`
- back-source: chuy?n b?t t?n t? bucket kh�c khi bucket hi?n t?i xu?ng g?n ngu?ng `thresholdValue`

� tu?ng l� bi?n 1 di?m n�ng th�nh nhi?u di?m nh? hon d? tang th�ng lu?ng khi flash sale.

### Reconciliation job Redis ? MySQL

`InventoryReconciliationJob` ch?y d?nh k? d? t�nh l?i t?n kho t? DB v� ghi l?i v�o Redis. M?c ti�u l� ch?ng l?ch
khi Redis restart, key b? x�a ho?c service ch?t gi?a ch?ng.

### Null-safety ? gateway

C�c package `filter` v� `exception` c?a gateway d� khai b�o `@NonNullApi` d? kh?p null-safety contract c?a
Spring WebFlux. Vi?c n�y kh�ng d?i h�nh vi runtime, nhung gi�p IDE/compiler b?t warning ? c�c method override.

### Waiting room cho flash sale

Waiting room la lop dem cong bang dat truoc luong request vao Saga. Thay vi ai bam nhanh hon thi chen thang
vao luong dat ve, he thong phat `queueToken` va xep hang truoc. Co 3 loi ich:

- Bao ve service nghiep vu khi tai dot bien.
- Giu so don `PROCESSING` dong thoi trong nguong he thong chiu duoc.
- Cong bang hon vi worker lay theo `priority` roi den thu tu vao hang.

Trong order-service hien tai:

- `placeOrder()` tao order trang thai `QUEUED` + queue item `WAITING`.
- Worker dinh ky chuyen tung lo item tu `WAITING` sang `PROCESSING`.
- Luc do moi publish `OrderPlacedEvent` de bat dau Saga reserve inventory -> payment -> booking.
- Token het han thi queue item chuyen `EXPIRED`.

Mo hinh nay giong khu xep hang truoc cua concert: ban khong duoc lao thang vao cua quet ve; ban lay so,
cho den luot, roi moi duoc dua vao khu xu ly tiep theo.

### Auth hardening bo sung

Ngoai viec cap JWT/refresh token, `user-service` hien co them 3 lop phong thu co ban:

- **Bootstrap admin** bang bien moi truong: giup moi truong moi co tai khoan quan tri ban dau ma khong can sua DB tay.
- **Rate limit login/refresh**: giam nguy co brute-force password va spam refresh token.
- **Test vong doi auth**: khoa lai luong `login -> refresh -> logout` de cac lan sua sau khong lam vo flow xac thuc.

### Auth hardening B5: Gateway authenticate, Service authorize

Sau B5, gateway khong con la noi duy nhat quyet dinh quyen chi tiet. Gateway validate JWT cho endpoint private, strip identity header gia mao va forward `Authorization` xuong service. Moi service quan trong co `JwtSecurityConfig`, `JwtAuthenticationFilter`, principal noi bo va `@PreAuthorize` de tu kiem tra rule `ADMIN` hoac owner.

Cac endpoint quan tri nhu employee, create/update/delete event-ticket-inventory can `ADMIN`. Cac endpoint order, booking, payment dung owner check de user chi doc/thao tac tai nguyen cua minh; `ADMIN` co quyen xem tong quat.
