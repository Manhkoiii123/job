# Kafka — kiến thức phỏng vấn (lý thuyết chuyên sâu + ví dụ Job Portal)

Tài liệu ôn phỏng vấn: **khái niệm bắt buộc** → **lý thuyết sâu** → **ví dụ trong repo** → **câu hỏi hay gặp**.  
Gắn với flow thật: Application Service publish → topic `application.status.changed` → Notification Service consume → email.

Code liên quan:

- Producer: `ApplicationStatusChangedPublisher` + `KafkaProducerConfig`
- Consumer: `ApplicationEventConsumer` + `KafkaConsumerConfig`
- Event: `ApplicationStatusChangedEvent`
- Hướng dẫn code: [`notification-service-kafka.md`](notification-service-kafka.md)

---

## 0. Checklist kiến thức cần biết (theo level)

### Junior — phải nói được

| Chủ đề | Bạn cần trả lời được |
|--------|----------------------|
| Vì sao dùng message queue / Kafka | Decouple, async, không chặn HTTP |
| Topic / Partition / Offset | “Hàng chờ có ngăn, vị trí đọc” |
| Producer / Consumer / Consumer Group | Ai gửi, ai nhận, scale thế nào |
| Sync vs Async trong hệ thống | Employer nhận 200 trước khi email xong |
| At-least-once cơ bản | Có thể nhận trùng → idempotent |
| Key message | Cùng key → cùng partition → thứ tự |

### Mid — nên nắm

| Chủ đề | Bạn cần trả lời được |
|--------|----------------------|
| Replication, ISR, leader/follower | Độ bền dữ liệu |
| ACKS (`0`, `1`, `all`) | Trade-off tốc độ vs an toàn |
| Consumer lag | Consumer chậm hơn producer |
| Rebalance | Member join/leave group |
| Idempotent producer | Tránh duplicate khi retry produce |
| Exactly-once (EOS) ý tưởng | Transactional produce + read-process-write |
| Dead letter / retry | Xử lý message lỗi |
| Compacted topic vs log retention | Use-case khác nhau |

### Senior — đào sâu

| Chủ đề | Bạn cần trả lời được |
|--------|----------------------|
| Ordering guarantees | Chỉ trong **một partition** |
| Hot partition | Key kém → lệch tải |
| Outbox pattern | DB + Kafka nhất quán |
| Poison message | Message mãi fail |
| Backpressure / lag alert | Observability |
| Schema Registry / Avro / Protobuf | Evolve contract |
| Kafka vs RabbitMQ vs Pulsar | Khi nào chọn gì |
| KRaft vs ZooKeeper | Kiến trúc control plane |

---

## 1. Kafka giải quyết bài toán gì?

### 1.1. Vấn đề monolith / sync cascade

Nếu Application Service **gọi thẳng** Notification (HTTP/Feign) khi đổi status:

```text
Employer đợi: DB update + Feign email SMTP (chậm / timeout / SMTP down)
→ UX xấu, coupling chặt, retry khó
```

### 1.2. Với Kafka (project của bạn)

```text
Employer đợi: DB update + history + publish Kafka (nhanh)
Email: Notification Service làm sau, độc lập
SMTP down → không làm fail PATCH status (nếu publish đã OK)
```

**Từ khóa phỏng vấn:** *temporal decoupling*, *spatial decoupling*, *backpressure isolation*.

---

## 2. Kiến trúc lõi (lý thuyết)

### 2.1. Topic

- Logical channel: tên ví dụ `application.status.changed`
- Producer ghi vào topic; consumer subscribe topic
- **Không** phải “queue 1-1” kiểu classic: nhiều consumer group có thể đọc **cùng** data (fan-out)

### 2.2. Partition

- Topic chia thành N partition (0..N-1)
- **Đơn vị song song**: 1 partition tại 1 thời điểm chỉ được **1 consumer trong 1 group** xử lý
- Scale consumer ≤ số partition (trong cùng group)

```text
topic application.status.changed
  ├─ partition 0  →  messages...
  ├─ partition 1
  └─ partition 2
```

### 2.3. Offset

- Mỗi message trong partition có offset tăng dần (0, 1, 2, …)
- Consumer nhớ “đã đọc tới đâu” (committed offset)
- Tăng offset ≠ “đã xử lý xong business” trừ khi bạn commit **sau** xử lý thành công

### 2.4. Broker / Cluster

- Broker = 1 process Kafka
- Cluster = nhiều broker
- Topic partition được **replicate** sang broker khác (độ bền)

### 2.5. Producer & Consumer

| Vai trò | Trong project |
|---------|----------------|
| Producer | `job-portal-application-service` |
| Consumer | `job-portal-notification-service` |
| Message | `ApplicationStatusChangedEvent` (JSON) |

---

## 3. Consumer Group — câu hỏi vàng phỏng vấn

### 3.1. Định nghĩa

**Consumer Group** = tập consumer cùng `group.id`, cùng subscribe topic → Kafka **chia partition** giữa họ.

```text
group = notification-service
  consumer A  → partition 0,1
  consumer B  → partition 2
```

- Thêm instance cùng group → **scale ngang** (throughput)
- Group **khác** đọc cùng topic → mỗi group có offset riêng (như nhiều “subscriber”)

### 3.2. Ví dụ project

```java
@KafkaListener(
    topics = "application.status.changed",
    groupId = "notification-service",
    ...
)
```

- Chỉ có **một** group `notification-service` → mỗi event email **một lần** (per partition assignment)
- Nếu sau này có Analytics Service group `analytics-service` subscribe cùng topic → cũng nhận đủ event (fan-out), không tranh offset với notification

### 3.3. Rebalance

Khi member join/leave/crash → Kafka **reassign** partition.  
Trong lúc rebalance: có thể pause consume → cần xử lý session timeout / max.poll.interval.

---

## 4. Message key & thứ tự (ordering)

### 4.1. Luật vàng

> **Thứ tự chỉ được đảm bảo trong một partition**, không phải toàn topic.

### 4.2. Code trong project

```java
String key = String.valueOf(event.getApplicationId());
kafkaTemplate.send(topic, key, event);
```

| Không có key | Có key = applicationId |
|--------------|-------------------------|
| Round-robin / sticky partitioner | Hash(key) → **cùng application** vào **cùng partition** |
| Event status của cùng đơn có thể lệch thứ tự giữa partition | `PENDING→SHORTLISTED` rồi `SHORTLISTED→HIRED` giữ thứ tự tương đối |

**Câu trả lời phỏng vấn hay:**  
“Em dùng `applicationId` làm key để mọi đổi status của một đơn được xử lý tuần tự, tránh email báo HIRED trước SHORTLISTED.”

### 4.3. Hot partition

Nếu 90% traffic một `applicationId` / một companyId → một partition quá tải.  
Mitigation: key design, tăng partition (cẩn thận), hoặc key compound có độ phân tán hơn khi không cần order chặt.

---

## 5. Delivery semantics (at-most / at-least / exactly-once)

### 5.1. Ba mức

| Semantics | Ý nghĩa | Thực tế |
|-----------|---------|---------|
| **At-most-once** | Có thể mất, không trùng | Commit offset trước khi xử lý |
| **At-least-once** | Có thể trùng, ít mất | Xử lý rồi mới commit (mặc định phổ biến) |
| **Exactly-once** | “Một lần” về mặt hiệu ứng | Kafka transactions + idempotent consumer/outbox |

### 5.2. Project đang ở đâu?

Flow hiện tại gần **at-least-once**:

1. Producer `send` (có thể retry → duplicate produce nếu chưa bật idempotent producer cấu hình đầy đủ)
2. Consumer xử lý email + ghi log
3. Offset commit (Spring Kafka mặc định sau listener return thành công)

**Hệ quả:** Listener chạy 2 lần → có thể **gửi 2 email**.  
**Cách trả lời phỏng vấn + hướng harden:**

- Idempotency key: `(applicationId, newStatus, changedAt)` unique trong `notification_log`
- Hoặc dedupe table trước khi `mailSender.send`
- Producer: `enable.idempotence=true` (idempotent producer)

### 5.3. Exactly-once (nói ở mức phỏng vấn)

Kafka EOS thường =:

- Idempotent producer + transactional ID
- Consumer `read_committed`
- Pattern *consume–transform–produce* trong một transaction

Email SMTP **không** nằm trong Kafka transaction → “exactly-once email” thực tế = **idempotent side-effect** (chỉ gửi nếu chưa SENT).

---

## 6. Producer sâu hơn

### 6.1. ACKS

| `acks` | Ý nghĩa |
|--------|---------|
| `0` | Fire-and-forget — nhanh, có thể mất |
| `1` | Leader ghi xong là OK |
| `all` / `-1` | Đủ ISR confirm — bền hơn, chậm hơn |

### 6.2. Idempotent producer

Producer gắn PID + sequence number → broker bỏ duplicate do retry mạng.  
**Không** giải quyết duplicate do application gọi `publish()` hai lần.

### 6.3. Batching & linger.ms / compression

Gom nhiều record một request → throughput cao.  
Phỏng vấn: biết trade-off latency vs throughput.

### 6.4. Ví dụ Spring trong repo

```java
config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
```

- Serialize POJO → JSON bytes  
- Tắt type header → consumer phải biết class (`VALUE_DEFAULT_TYPE`) — hợp contract cố định giữa 2 service

---

## 7. Consumer sâu hơn

### 7.1. Poll loop

Consumer không “được push”; nó **poll** batch record, xử lý, commit.

### 7.2. Commit mode

| Mode | Ý nghĩa |
|------|---------|
| Auto commit | Định kỳ commit — dễ at-most-once nếu crash giữa chừng |
| Sync/async commit thủ công | Kiểm soát at-least-once |
| Spring `@KafkaListener` | Thường commit sau method thành công (AckMode tùy config) |

### 7.3. Lag

```text
lag = log-end-offset - consumer-committed-offset
```

Lag tăng = consumer chậm / down / xử lý nặng (SMTP).  
Alert lag = skill mid/senior.

### 7.4. `max.poll.interval.ms` / `session.timeout.ms`

Xử lý quá lâu → bị kick khỏi group → rebalance → record có thể được giao cho instance khác → **duplicate risk**.

---

## 8. Độ bền & replication

### 8.1. Replication factor

Partition có 1 leader + (RF-1) follower.  
`min.insync.replicas` + `acks=all` → không mất data khi 1 broker chết (trong ngưỡng).

### 8.2. ISR (In-Sync Replicas)

Follower theo kịp leader mới ở trong ISR.  
ACKS=all chờ ISR — nếu ISR co lại có thể không nhận produce (phụ thuộc config).

### 8.3. Retention

- Time-based (`retention.ms`) hoặc size-based  
- Log compacted topic: giữ bản ghi mới nhất theo key (để changelog / state) — **không** phải use-case email event thông thường

Topic status change của bạn: **append-only event log** + retention đủ dài để replay/debug.

---

## 9. Thiết kế event (contract) — rất hay bị hỏi

### 9.1. Event trong project

`ApplicationStatusChangedEvent` mang:

- ID nghiệp vụ (`applicationId`, `candidateId`, …)
- **Denormalized** fields: `candidateEmail`, `jobTitle`, `companyName`
- `previousStatus` / `newStatus` / `changedAt`

### 9.2. Fat event vs Thin event

| Fat (project đang dùng) | Thin |
|-------------------------|------|
| Consumer ít Feign | Consumer phải gọi User/Job lại |
| Event lớn hơn, gắn phiên bản dữ liệu lúc phát | Luôn data mới nhất nhưng coupling runtime |
| Rủi ro: email cũ nếu user đổi email sau event | Rủi ro: User Service down → không gửi được |

**Trả lời phỏng vấn:**  
“Em chọn fat event cho notification vì email cần snapshot lúc đổi status; giảm phụ thuộc sync lúc consume.”

### 9.3. Schema evolution

Đổi field JSON có thể phá consumer.  
Production: Schema Registry + Avro/Protobuf + compatibility BACKWARD/FORWARD.

---

## 10. Consistency: DB rồi mới Kafka (và ngược lại)

### 10.1. Thứ tự trong `updateStatus`

```text
1) save Application (DB)
2) save History (DB)
3) kafkaTemplate.send(...)
4) return HTTP 200
```

**Rủi ro:** DB commit xong, publish Kafka fail → status đã đổi nhưng **không có email**.  
**Rủi ro ngược (nếu publish trước commit):** email gửi nhưng transaction DB rollback → **email sai**.

### 10.2. Outbox pattern (câu trả lời senior)

```text
Cùng DB transaction:
  - update application
  - insert outbox_event
Async relay (Debezium / poller):
  - đọc outbox → Kafka
  - đánh dấu published
```

→ Không mất event giữa DB và Kafka (at-least-once tới Kafka + idempotent consumer).

### 10.3. Inbox / idempotent consumer

Bảng `notification_log` / `processed_event(event_id UNIQUE)` trước khi side-effect.

---

## 11. So sánh nhanh (câu hỏi “Tại sao không dùng X?”)

| Công nghệ | Khi nào |
|-----------|---------|
| **Kafka** | Event stream, high throughput, replay, nhiều subscriber |
| **RabbitMQ** | Routing phức tạp, task queue classic, protocol AMQP |
| **Redis Pub/Sub** | Không lưu bền / không consumer group offset chuẩn |
| **Feign sync** | Cần kết quả ngay, consistency chặt, traffic thấp |
| **DB polling** | Đơn giản nhưng kém realtime, tốn DB |

**Với email sau đổi status:** Kafka (hoặc queue) phù hợp hơn Feign sync.

---

## 12. Ví dụ cụ thể end-to-end (kể chuyện trong phỏng vấn)

> “Khi employer gọi `PATCH /api/applications/{id}/status` với `SHORTLISTED`, Application Service cập nhật DB và history, build `ApplicationStatusChangedEvent` (kèm email ứng viên lấy từ User Service), publish Kafka với key=`applicationId`. API trả success ngay. Notification Service cùng group `notification-service` consume topic `application.status.changed`, dựng HTML, gửi SMTP (MailHog local), ghi `notification_log` SENT/FAILED. Nhờ async, SMTP chậm không block employer.”

Điểm cộng nếu nhắc:

- Key → ordering per application  
- Consumer group → scale / fan-out  
- At-least-once → cần idempotent gửi mail  
- Có thể nâng cấp Outbox  

---

## 13. Câu hỏi phỏng vấn mẫu + gợi ý trả lời

**Q1. Kafka khác gì message queue truyền thống?**  
A: Log phân tán, partition song song, retention/replay, nhiều consumer group đọc cùng data.

**Q2. Tăng consumer trong group mà throughput không tăng?**  
A: Số consumer useful ≤ số partition; kiểm tra rebalance, hot partition, xử lý chậm.

**Q3. Làm sao đảm bảo thứ tự event?**  
A: Cùng key → cùng partition; một consumer/partition; tránh multi-thread xử lý loạn trong partition.

**Q4. Message bị xử lý 2 lần thì sao?**  
A: At-least-once bình thường; idempotent consumer / unique constraint / dedupe.

**Q5. Producer gửi thành công nhưng consumer không thấy?**  
A: Sai topic, sai cluster, ACL, consumer group offset đã ở cuối (`latest`), deserialize lỗi, listener exception liên tục.

**Q6. `enable.auto.commit=true` nguy hiểm thế nào?**  
A: Commit trước khi xử lý xong → crash → mất message (at-most-once).

**Q7. Làm gì với message lỗi mãi?**  
A: Retry có giới hạn + DLQ (dead letter topic) + alert; không block partition mãi.

**Q8. Exactly-once có nghĩa email chỉ 1 lần?**  
A: Kafka EOS không bao trùm SMTP; cần idempotent business side-effect.

**Q9. ZooKeeper / KRaft?**  
A: Metadata/quorum. KRaft thay ZK cho metadata Kafka mới; broker vẫn lưu partition data.

**Q10. Thiết kế topic: 1 topic lớn hay nhiều topic?**  
A: Theo bounded context / fan-out / retention / ACL. Status change → 1 topic domain event là hợp lý.

---

## 14. Thuật ngữ flashcard (học thuộc)

| Term | 1 câu |
|------|-------|
| Topic | Kênh logical chứa message |
| Partition | Đơn vị song song + đơn vị order |
| Offset | Vị trí message trong partition |
| Consumer Group | Nhóm chia partition, mỗi record “một lần” trong group |
| Rebalance | Gán lại partition khi membership đổi |
| Lag | Độ chậm của consumer |
| ISR | Replica đang sync với leader |
| ACKS | Điều kiện broker xác nhận produce |
| Idempotent producer | Retry produce không tạo duplicate broker-side |
| Outbox | Ghi event cùng transaction DB rồi relay Kafka |
| DLQ | Topic chứa message xử lý fail |
| Fat event | Event mang đủ data cho consumer |

---

## 15. Lộ trình ôn 3 ngày

**Ngày 1:** Topic/partition/offset/group + kể lại flow Job Portal.  
**Ngày 2:** ACKS, replication, delivery semantics, key & ordering.  
**Ngày 3:** Outbox, idempotent consumer, lag, DLQ, so sánh Rabbit/Kafka + trả lời Q1–Q10.

---

## 16. Map sang file trong repo (để demo interviewer)

| Khái niệm | File |
|-----------|------|
| Event contract | `common-lib/.../ApplicationStatusChangedEvent.java` |
| Produce + key | `ApplicationStatusChangedPublisher.java` |
| Producer JSON | `KafkaProducerConfig.java` |
| Consume | `ApplicationEventConsumer.java` |
| Consumer JSON | `KafkaConsumerConfig.java` |
| Side-effect + log | `EmailNotificationServiceImpl.java` |
| Infra | `docker-compose.yml` (kafka, zookeeper, mailhog) |

---

**Chốt phỏng vấn một câu:**  
Kafka là *distributed commit log* giúp service phát event và nhiều consumer xử lý độc lập; order theo partition, scale theo partition, độ tin cậy theo replication/ACKS, và “đúng một lần” ở business nhờ idempotency — không ảo tưởng SMTP nằm trong Kafka transaction.
