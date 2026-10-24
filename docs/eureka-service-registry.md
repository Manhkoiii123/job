# Service Registry & Discovery với Netflix Eureka

Tài liệu giải thích các khái niệm trong kiến trúc **Service Registry (Netflix Eureka)** dùng cho hệ thống Job Portal microservices (Spring Boot).

---

## 1. Bài toán cần giải quyết

Trong monolith, các module gọi nhau trong cùng process — không cần tìm địa chỉ.

Trong **microservices**, mỗi service chạy riêng (IP + port khác nhau), ví dụ:

| Service | Ví dụ địa chỉ |
|---------|----------------|
| User Service | `localhost:5001` |
| Company Service | `localhost:5002` |
| Job Service | `localhost:5003` |
| Application Service | `localhost:5005` |

Nếu hardcode URL như:

```yaml
services:
  user:
    url: http://localhost:5001
```

sẽ gặp vấn đề khi:

- đổi port / deploy lên nhiều máy
- scale nhiều instance cùng service
- instance chết / restart → địa chỉ thay đổi

→ Cần một **danh bạ luôn cập nhật** các service đang sống: **Service Registry**.

---

## 2. Service Registry (Netflix Eureka) là gì?

**Service Registry** = thư mục trung tâm lưu thông tin các microservice đang chạy:

- tên service (`spring.application.name`)
- IP / hostname
- port
- trạng thái (UP / DOWN)

Trong diagram, Eureka được gọi là:

> **LIVE DIRECTORY / ALWAYS UP-TO-DATE DIRECTORY**

Eureka (Netflix) là implementation phổ biến trong hệ sinh thái Spring Cloud.

Vai trò:

1. Nhận **đăng ký** từ các service
2. Nhận **heartbeat** để biết service còn sống
3. Trả lời câu hỏi **discovery**: “User Service đang ở đâu?”

Registry **không** xử lý business (apply job, tạo user…). Nó chỉ quản lý “ai đang online, ở địa chỉ nào”.

---

## 3. Registration & Heartbeat

### 3.1. Registration (đăng ký)

Khi **Job Listing Service** (Job Service) khởi động, nó nói với Eureka:

> “Hey, I'm running. Here's my name, IP address, and my port.”

Ví dụ thông tin gửi lên:

```text
name: job-portal-job-service
host: 192.168.1.10
port: 5003
status: UP
```

Sau khi đăng ký, Eureka thêm service vào directory.

Trong Spring Cloud thường cấu hình:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
```

### 3.2. Heartbeat (nhịp tim)

Service **định kỳ** gửi tín hiệu “tôi vẫn sống” lên Eureka (mặc định khoảng vài chục giây).

- Có heartbeat → vẫn giữ trong directory
- Mất heartbeat quá lâu → Eureka coi instance **DOWN** / loại khỏi danh sách

→ Directory luôn “live”, không chứa đầy xác service chết.

Trong diagram: mũi tên **REGISTRATION & HEARTBEAT** từ Job Listing Service → Service Registry.

---

## 4. Service Discovery (tìm kiếm service)

Khi **Application Service** cần gọi **User Service**, nó **không hardcode** IP/port cố định. Nó hỏi Eureka:

> “Where is the User Service right now?”

Eureka trả lời:

> “Here's the live address (IP, Port) of User Service.”

Ví dụ:

```text
User Service → 192.168.1.20:5001
```

Nếu có nhiều instance User Service, Eureka có thể trả danh sách; client (hoặc load balancer) chọn một instance.

Trong Spring thường dùng:

- `DiscoveryClient`
- hoặc `RestClient` / `WebClient` với load-balanced (`@LoadBalanced`) và gọi theo **tên service**:

```text
http://job-user-service/api/users/1
```

thay vì:

```text
http://localhost:5001/api/users/1
```

---

## 5. Direct Communication (giao tiếp trực tiếp)

Sau khi biết địa chỉ User Service, **Application Service gọi thẳng User Service**.

```text
Application Service  ←── DIRECT COMMUNICATION ──→  User Service
```

Điểm quan trọng:

| Bước | Ai tham gia |
|------|-------------|
| Tìm địa chỉ | Application → Eureka → Application |
| Gọi API thật (GET user, …) | Application → User **trực tiếp** |

Eureka **không** đứng giữa mọi request business. Nó chỉ là “danh bạ điện thoại”; sau khi có số, bạn gọi thẳng người kia.

---

## 6. Luồng tổng hợp (theo diagram)

```text
1. Job Service start
      → REGISTER + HEARTBEAT → Eureka

2. User Service start
      → REGISTER + HEARTBEAT → Eureka

3. Application Service cần user
      → hỏi Eureka: "User Service ở đâu?"
      ← Eureka trả IP:port

4. Application Service
      → gọi HTTP trực tiếp tới User Service
```

---

## 7. So với project Job Portal hiện tại

Hiện các service đang gọi nhau bằng URL hardcode trong `application.yaml`, ví dụ:

```yaml
services:
  user:
    url: http://localhost:5001
  job:
    url: http://localhost:5003
```

và dùng `RestClient` / `*Client` class.

| Cách hiện tại | Với Eureka |
|---------------|------------|
| Hardcode `localhost:port` | Gọi theo tên service |
| Đổi port phải sửa config nhiều chỗ | Eureka cập nhật địa chỉ live |
| Khó scale nhiều instance | Discovery + load balancing |

Eureka là bước hạ tầng tiếp theo khi hệ thống đi từ “nhiều service trên máy local” sang “cloud / nhiều instance”.

---

## 8. Liên quan tới JWT / `X-User-Id` (không nằm trong diagram nhưng hay nhầm)

| Thành phần | Vai trò |
|------------|---------|
| **Eureka** | Service A tìm được địa chỉ Service B |
| **JWT / Gateway** | Biết **user nào** đang gọi API |
| **`X-User-Id`** | Header nội bộ mang identity user xuống từng service |

Eureka **không** thay JWT. Hai thứ giải quyết hai bài toán khác nhau:

- Eureka → *service ở đâu?*
- Auth token → *ai đang request?*

Kiến trúc đầy đủ thường có thêm **API Gateway**: client gửi JWT → Gateway verify → gắn `X-User-Id` → forward tới service (service đó có thể được discovery qua Eureka).

---

## 9. Thuật ngữ nhanh

| Thuật ngữ | Ý nghĩa |
|-----------|---------|
| **Service Registry** | Kho đăng ký địa chỉ các service |
| **Eureka** | Implementation registry của Netflix (Spring Cloud Netflix) |
| **Registration** | Service báo “tôi online + địa chỉ” |
| **Heartbeat** | Ping định kỳ để chứng minh còn sống |
| **Discovery** | Service hỏi “X đang ở đâu?” |
| **Direct communication** | Sau discovery, gọi HTTP thẳng tới service đích |
| **Instance** | Một process đang chạy của một service |

---

## 10. Kết luận

Diagram mô tả 3 ý chính:

1. **Mọi service đăng ký + gửi heartbeat** lên Eureka → directory luôn cập nhật.
2. **Service cần gọi service khác thì hỏi Eureka** để lấy địa chỉ live.
3. **Traffic business đi trực tiếp** giữa hai service, không đi qua Eureka.

Đó là nền tảng **Service Registry & Discovery** trong kiến trúc microservices Spring Boot.

---

## 11. Các bước setup Eureka trong project Job Portal

Áp dụng cho cấu trúc hiện tại:

```text
02_job/
  cloud/                  ← đặt Eureka Server ở đây
  services/
    job-user-service
    job-portal-company-service
    job-portal-job-service
    job-portal-resume-service
    job-portal-application-service
    job-portal-preferences
  common-lib/
  pom.xml                 ← đã có spring-cloud.version = 2024.0.1
```

Port gợi ý:

| Thành phần | Port |
|------------|------|
| Eureka Server | `8761` |
| User Service | `5001` |
| Company Service | `5002` |
| Job Service | `5003` |
| Resume Service | `5004` |
| Application Service | `5005` |
| Preferences Service | `5006` |

---

### Bước 1 — Tạo module Eureka Server

Trong `cloud/pom.xml`, thêm module:

```xml
<modules>
    <module>eureka-server</module>
</modules>
```

Tạo thư mục:

```text
cloud/eureka-server/
  pom.xml
  src/main/java/com/manh/job/eureka/EurekaServerApplication.java
  src/main/resources/application.yaml
```

#### `cloud/eureka-server/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.manh.job</groupId>
        <artifactId>job-portal-cloud</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>eureka-server</artifactId>
    <name>eureka-server</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Main class

```java
package com.manh.job.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

#### `application.yaml` (Eureka Server)

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: localhost
  client:
    # Server không tự đăng ký vào chính nó
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
```

Chạy server rồi mở dashboard:

```text
http://localhost:8761
```

---

### Bước 2 — Biến mỗi microservice thành Eureka Client

Làm lần lượt cho: user, company, job, resume, application, preferences.

#### 2.1. Thêm dependency vào `pom.xml` của từng service

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

> Parent đã import `spring-cloud-dependencies` → không cần ghi version.

#### 2.2. Bật discovery trên main class

```java
@SpringBootApplication
@EnableDiscoveryClient
public class JobPortalJobServiceApplication {
    // ...
}
```

(`@EnableDiscoveryClient` optional với starter mới, nhưng nên ghi rõ.)

#### 2.3. Cấu hình `application.yaml` của từng service

Ví dụ **job-service**:

```yaml
server:
  port: 5003

spring:
  application:
    name: job-portal-job-service   # tên đăng ký lên Eureka (quan trọng)

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

Làm tương tự các service khác, chỉ đổi `server.port` và `spring.application.name`:

| Service | `spring.application.name` | Port |
|---------|---------------------------|------|
| User | `job-user-service` | 5001 |
| Company | `job-portal-company-service` | 5002 |
| Job | `job-portal-job-service` | 5003 |
| Resume | `job-portal-resume-service` | 5004 |
| Application | `job-portal-application-service` | 5005 |
| Preferences | `job-portal-preferences-service` | 5006 |

#### 2.4. Kiểm tra Registration

1. Start **Eureka Server** trước  
2. Start lần lượt các microservice  
3. Vào `http://localhost:8761` → thấy các service **UP**

Đây chính là bước **Registration & Heartbeat** trong diagram.

---

### Bước 3 — Gọi service theo tên (Discovery) thay vì hardcode URL

Hiện client đang hardcode:

```java
@Value("${services.job.url:http://localhost:5003}") String jobServiceUrl
```

Sau Eureka, gọi theo **tên đăng ký**:

```text
http://job-portal-job-service/api/jobs/{id}
```

#### Cách A — `RestClient` + `@LoadBalanced` (khuyến nghị với Spring mới)

1. Thêm dependency (nếu chưa có load balancer):

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

2. Config bean:

```java
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
```

3. Sửa client — bỏ URL localhost:

```java
@Component
public class JobClient {
    private final RestClient restClient;

    public JobClient(@LoadBalanced RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://job-portal-job-service").build();
    }

    public JobResponse getJobById(Long jobId) {
        return restClient.get()
                .uri("/api/jobs/{jobId}", jobId)
                .retrieve()
                .body(JobResponse.class);
    }
}
```

Load balancer hỏi Eureka → lấy IP:port live → **Direct Communication** tới Job Service.

#### Cách B — Giữ config, đổi value sang tên service

```yaml
services:
  job:
    url: http://job-portal-job-service
  user:
    url: http://job-user-service
  company:
    url: http://job-portal-company-service
  resume:
    url: http://job-portal-resume-service
```

Vẫn cần `@LoadBalanced` RestClient.Builder thì mới resolve được tên service.

---

### Bước 4 — Thứ tự chạy khi dev local

```text
1. Eureka Server          (:8761)
2. job-user-service       (:5001)
3. job-portal-company-service (:5002)
4. job-portal-job-service (:5003)
5. job-portal-resume-service  (:5004)
6. job-portal-application-service (:5005)
7. job-portal-preferences-service (:5006)
```

Checklist:

- [ ] Dashboard Eureka mở được  
- [ ] Mỗi service hiện trên dashboard  
- [ ] Application gọi Job/User/Company **không** còn phụ thuộc port hardcode  
- [ ] Tắt 1 service → sau vài heartbeat, Eureka đánh dấu DOWN  

---

### Bước 5 — (Tuỳ chọn) Tắt đăng ký khi chạy unit test

```yaml
# application-test.yaml
eureka:
  client:
    enabled: false
```

hoặc:

```yaml
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

---

### Bước 6 — Lỗi thường gặp

| Lỗi | Nguyên nhân / Cách xử lý |
|-----|---------------------------|
| Service không hiện trên dashboard | Sai `defaultZone`, hoặc Eureka chưa start |
| `No instances available for ...` | Tên `spring.application.name` lệch với URL client |
| Vẫn gọi được localhost cũ | Client chưa dùng `@LoadBalanced` / vẫn hardcode port |
| Loop / self-preservation warning | Dev local bình thường; có thể tắt `eureka.server.enable-self-preservation: false` (chỉ local) |
| Version Spring Cloud / Boot lệch | Kiểm tra BOM `spring-cloud-dependencies` khớp Boot đang dùng |

Self-preservation (local):

```yaml
# Chỉ dùng khi dev
eureka:
  server:
    enable-self-preservation: false
```

---

### Bước 7 — Mapping với diagram

| Bước setup | Ý trong diagram |
|------------|-----------------|
| Bước 1 | Tạo **Service Registry (LIVE DIRECTORY)** |
| Bước 2 | **Registration & Heartbeat** |
| Bước 3 | **Discovery** + **Direct Communication** |
| Dashboard `:8761` | Xem directory luôn cập nhật |

---

### Tóm tắt checklist setup

1. Tạo `cloud/eureka-server` + `@EnableEurekaServer` + port `8761`  
2. Mỗi service thêm `eureka-client` + `spring.application.name` + `defaultZone`  
3. Thêm `spring-cloud-starter-loadbalancer`  
4. Đổi `*Client` sang gọi `http://{service-name}/...` với `@LoadBalanced`  
5. Start Eureka trước, rồi start các service  
6. Verify trên `http://localhost:8761`  

> **Đã triển khai đầy đủ steps 38–42** trong code. Chi tiết vận hành xem thêm: [`spring-cloud-setup-38-42.md`](./spring-cloud-setup-38-42.md).
