# Notification Service + Kafka — hướng dẫn cho người mới

Tài liệu đi **theo thứ tự viết code** (giống sơ đồ PENDING → SHORTLISTED).  
Mỗi bước: *làm gì* → *file nào* → *giải thích từng dòng / từng khối*.

Ôn phỏng vấn (lý thuyết chuyên sâu + Q&A): [`kafka-interview-guide.md`](kafka-interview-guide.md).

## Bức tranh tổng thể (1 phút)

```text
Employer PATCH /api/applications/{id}/status
        │  (đồng bộ — chờ response ngay)
        ▼
Application Service :5005
  1) UPDATE status trong DB
  2) Ghi application_status_history
  3) Publish ApplicationStatusChangedEvent → Kafka topic
  4) Trả 200 cho employer  ← employer xong việc ở đây
        │
        │  (bất đồng bộ — không chặn employer)
        ▼
Kafka :9092  topic = application.status.changed
        │
        ▼
Notification Service :5008
  5) @KafkaListener nhận event
  6) Dựng HTML email + gửi SMTP (MailHog local)
  7) Ghi notification_log (SENT / FAILED / SKIPPED)
        │
        ▼
Ứng viên nhận mail (xem UI MailHog http://localhost:8025)
```

| Thành phần | Port (project này) |
|------------|--------------------|
| API Gateway | 8080 |
| Application Service | 5005 |
| Notification Service | 5008 |
| Kafka | 9092 |
| MailHog SMTP / UI | 1025 / 8025 |

> Sơ đồ gốc dùng 9000/9006/9007 — cùng ý tưởng, port khác cho khớp Job Portal hiện tại.

---

# Thứ tự code (làm từ 1 → 8)

```text
1. Event DTO chung (common-lib)
2. Application Service: Kafka producer + history + publish khi đổi status
3. Notification Service: module skeleton
4. NotificationLog entity + repository
5. Kafka consumer config
6. EmailNotificationServiceImpl
7. ApplicationEventConsumer (@KafkaListener)
8. Controller xem log + config/Eureka/Gateway/Compose
```

---

## Bước 1 — Event DTO dùng chung

**File:** `common-lib/.../dto/event/ApplicationStatusChangedEvent.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusChangedEvent {
    private Long applicationId;
    private Long candidateId;
    // ...
    private String candidateEmail;
    private String jobTitle;
    private String companyName;
    private ApplicationStatus previousStatus;
    private ApplicationStatus newStatus;
    private LocalDateTime changedAt;
}
```

| Dòng / annotation | Ý nghĩa |
|-------------------|---------|
| `@Data` | Lombok: getter/setter/`toString` |
| `@Builder` | Cho phép `Event.builder().applicationId(1L).build()` |
| `applicationId` | Đơn ứng tuyển nào đổi status |
| `candidateEmail` | Mail người nhận (producer lấy từ User Service) |
| `jobTitle` / `companyName` | Để viết nội dung email đẹp |
| `previousStatus` / `newStatus` | Ví dụ `PENDING` → `SHORTLISTED` |
| `changedAt` | Thời điểm đổi |

**Vì sao để common-lib?**  
Producer và consumer **phải hiểu cùng một JSON**. Một class dùng chung → không copy-paste lệch field.

Cùng bước: enum `NotificationDeliveryStatus` (`SENT`, `FAILED`, `SKIPPED`) trong `common-lib/.../domain/`.

---

## Bước 2 — Application Service phát event (producer)

### 2.1 Dependency

**File:** `services/job-portal-application-service/pom.xml`

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

→ Thêm thư viện Kafka client cho Spring.

### 2.2 Cấu hình producer

**File:** `.../config/KafkaProducerConfig.java`

| Khối code | Ý nghĩa |
|-----------|---------|
| `@Configuration` | Class chứa bean Spring |
| `@Value("${spring.kafka.bootstrap-servers:...}")` | Địa chỉ Kafka (`localhost:9092` hoặc `kafka:29092` trong Docker) |
| `ProducerConfig.KEY_SERIALIZER... StringSerializer` | Key message = String (dùng `applicationId`) |
| `VALUE_SERIALIZER... JsonSerializer` | Value = JSON của event |
| `JsonSerializer.ADD_TYPE_INFO_HEADERS, false` | Không nhét header type Java (consumer tự map class) |
| `KafkaTemplate<...>` | API gửi message: `send(topic, key, value)` |
| `NewTopic` + `TopicBuilder` | Tự tạo topic nếu chưa có (dev tiện) |

### 2.3 Publisher

**File:** `.../event/ApplicationStatusChangedPublisher.java`

```java
kafkaTemplate.send(topic, key, event);
```

| Phần | Ý nghĩa |
|------|---------|
| `topic` | `application.status.changed` |
| `key` | `applicationId` — cùng application vào cùng partition (thứ tự) |
| `event` | Object vừa build |

### 2.4 History + publish trong business

**File:** `ApplicationServiceImpl.updateStatus(...)`

Thứ tự **quan trọng**:

```text
1. Lưu previous = application.getStatus()
2. setStatus(new) + save DB          ← sync
3. recordStatusHistory(...)          ← sync (bảng application_status_history)
4. publishStatusChanged(...)         ← gửi Kafka (async phía consumer)
5. return ResponseEntity OK          ← employer nhận success ngay
```

`publishStatusChanged` gọi Feign User/Job/Company để **điền email, title, company** vào event rồi `publisher.publish(event)`.

**File history:** `modal/ApplicationStatusHistory.java`  
→ Entity map bảng `application_status_history` (đúng sơ đồ bước 3).

### 2.5 application.yaml (application-service)

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
job:
  kafka:
    topics:
      application-status-changed: application.status.changed
```

| Key | Ý nghĩa |
|-----|---------|
| `bootstrap-servers` | Broker Kafka |
| `application-status-changed` | Tên topic — phải **khớp** consumer |

---

## Bước 3 — Tạo module Notification Service

**Folder:** `services/job-portal-notification-service/`  
**Port:** `5008`  
**Eureka name:** `job-portal-notification-service`

`pom.xml` gồm:

- `spring-boot-starter-webmvc` — REST xem log  
- `spring-boot-starter-data-jpa` + PostgreSQL — bảng `notification_log`  
- `spring-boot-starter-mail` — `JavaMailSender`  
- `spring-kafka` — consumer  
- eureka-client + config  

`JobPortalNotificationServiceApplication.java`:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class JobPortalNotificationServiceApplication { ... }
```

| Annotation | Ý nghĩa |
|------------|---------|
| `@SpringBootApplication` | Boot app chuẩn |
| `@EnableDiscoveryClient` | Đăng ký Eureka |

Thêm module vào `services/pom.xml`:

```xml
<module>job-portal-notification-service</module>
```

---

## Bước 4 — NotificationLog (DB của noti service)

**File:** `modal/NotificationLog.java`

| Field | Ý nghĩa |
|-------|---------|
| `recipientEmail` | Gửi tới ai |
| `subject` / `bodyPreview` | Nội dung (preview cắt ngắn) |
| `previousStatus` / `newStatus` | Copy từ event |
| `deliveryStatus` | `SENT` / `FAILED` / `SKIPPED` |
| `errorMessage` | Lý do fail (nếu có) |
| `createdAt` | Thời điểm xử lý |

**Repository:**

```java
List<NotificationLog> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
```

→ Tra cứu log theo đơn ứng tuyển (debug / UI admin).

---

## Bước 5 — Kafka consumer config

**File:** `config/KafkaConsumerConfig.java`

| Khối | Ý nghĩa |
|------|---------|
| `GROUP_ID_CONFIG = notification-service` | Consumer group — nhiều instance chia partition |
| `StringDeserializer` | Key |
| `JsonDeserializer` | Value → `ApplicationStatusChangedEvent` |
| `TRUSTED_PACKAGES` | Cho phép deserialize package `com.manh.job.dto.event` |
| `VALUE_DEFAULT_TYPE` | Class mặc định khi không có type header |
| `USE_TYPE_INFO_HEADERS = false` | Khớp producer (không gửi type header) |
| `AUTO_OFFSET_RESET = earliest` | Group mới: đọc từ đầu topic (dev) |
| `ConcurrentKafkaListenerContainerFactory` | Factory cho `@KafkaListener` |

---

## Bước 6 — Gửi email + ghi log

**File:** `service/impl/EmailNotificationServiceImpl.java`

Luồng `handleStatusChanged(event)`:

```text
1. buildSubject / buildHtmlBody từ event
2. Nếu thiếu email → SKIPPED + log DB
3. Nếu mail-enabled=false → SKIPPED (học không cần SMTP)
4. Tạo MimeMessage (HTML) → JavaMailSender.send
5. Thành công → SENT; exception → FAILED + errorMessage
```

| Dòng quan trọng | Ý nghĩa |
|-----------------|---------|
| `JavaMailSender` | Spring abstract SMTP |
| `MimeMessageHelper(..., true, "UTF-8")` | `true` = multipart/HTML |
| `helper.setText(html, true)` | `true` = nội dung HTML |
| `mailSender.send(message)` | Gửi thật tới MailHog / SMTP |
| `notificationLogRepository.save(...)` | Luôn ghi audit (kể cả fail) |

HTML mẫu (đúng ý sơ đồ):

> Your application for **Senior Developer** at **TechCorp** has been updated.  
> Status changed from **PENDING** → **SHORTLISTED**.

---

## Bước 7 — Consumer lắng nghe topic

**File:** `kafka/ApplicationEventConsumer.java`

```java
@KafkaListener(
    topics = "${job.kafka.topics.application-status-changed:application.status.changed}",
    groupId = "${spring.kafka.consumer.group-id:notification-service}",
    containerFactory = "applicationStatusKafkaListenerContainerFactory"
)
public void onStatusChanged(ApplicationStatusChangedEvent event) {
    emailNotificationService.handleStatusChanged(event);
}
```

| Phần | Ý nghĩa |
|------|---------|
| `@KafkaListener` | Spring tạo listener thread, poll Kafka |
| `topics` | Topic phải trùng producer |
| `groupId` | Tên group consumer |
| `containerFactory` | Dùng factory JSON đã cấu hình |
| Gọi `handleStatusChanged` | Tách I/O email khỏi lớp Kafka |

---

## Bước 8 — API xem log + hạ tầng

### Controller

`GET /api/notifications/logs`  
`GET /api/notifications/logs/application/{applicationId}`

→ Gateway route: `Path=/api/notifications/**` → `lb://job-portal-notification-service`.

### Config Server

`cloud/config-server/.../config/job-portal-notification-service.yaml`  
→ port 5008, datasource, kafka, mail.

### Docker Compose thêm

| Service | Việc |
|---------|------|
| `zookeeper` + `kafka` | Message broker |
| `mailhog` | SMTP giả (1025) + UI (8025) |
| `job-portal-notification-service` | Consumer |
| DB `job_portal_notification` | Trong `01-databases.sql` |

`KAFKA_BOOTSTRAP_SERVERS=kafka:29092` cho container Java.  
IDE local dùng `localhost:9092` (listener `PLAINTEXT_HOST`).

---

# Chạy thử (local)

1. Bật Docker: Kafka + MailHog + Postgres (hoặc `docker compose up -d kafka zookeeper mailhog postgres`).  
2. Tạo DB `job_portal_notification` nếu Postgres đã có volume cũ.  
3. Start: Eureka → Config → User/Job/Company → Application → Notification → Gateway.  
4. Employer: `PATCH http://localhost:8080/api/applications/{id}/status` + JWT + body `{"status":"SHORTLISTED"}`.  
5. Mở http://localhost:8025 xem email.  
6. `GET http://localhost:8080/api/notifications/logs` xem `SENT`.

---

# Checklist file (theo thứ tự code)

```text
[ ] common-lib: ApplicationStatusChangedEvent + NotificationDeliveryStatus
[ ] application-service: spring-kafka + KafkaProducerConfig + Publisher
[ ] application-service: ApplicationStatusHistory + updateStatus publish
[ ] notification-service: pom + Application main
[ ] notification-service: NotificationLog + Repository
[ ] notification-service: KafkaConsumerConfig
[ ] notification-service: EmailNotificationServiceImpl
[ ] notification-service: ApplicationEventConsumer
[ ] notification-service: NotificationLogController
[ ] config-server yaml + gateway route + compose kafka/mailhog/noti
```

---

# Lỗi hay gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|------------|-------------|------------|
| Producer lỗi connection | Kafka chưa chạy | `docker compose up -d kafka zookeeper` |
| Consumer không nhận | Sai topic / group / JSON type | Khớp topic; `USE_TYPE_INFO_HEADERS=false` hai bên |
| Email không thấy | Sai SMTP / MailHog tắt | MailHog `:1025` + UI `:8025` |
| `SKIPPED` trong log | Thiếu `candidateEmail` | User Service phải trả email |
| DB notification thiếu | Volume Postgres cũ | Tạo DB tay hoặc `compose down -v` |

---

**Nhớ 1 câu:** Employer chỉ chờ Application Service; email chạy **sau**, nhờ Kafka — đó là *event-driven*.
