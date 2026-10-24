# Spring Cloud cho người mới — Steps 38 → 42

Tài liệu này dành cho người **mới tiếp xúc** microservices / Spring Cloud.  
Đọc từ trên xuống: hiểu bài toán → hiểu từng công nghệ → biết project làm gì → biết chạy thử.

---

## Phần 0 — Bắt đầu từ con số 0

### 0.1. Monolith vs Microservices (hiểu bằng ví dụ)

**Monolith (1 app lớn)**  
Giống một siêu thị: bán hàng, kho, kế toán đều trong **một tòa nhà**.  
Muốn sửa kế toán cũng phải “mở cả siêu thị”.

**Microservices (nhiều app nhỏ)**  
Giống chuỗi cửa hàng chuyên biệt:

| Cửa hàng | Trong project |
|----------|----------------|
| Quản lý tài khoản | User Service (`:5001`) |
| Hồ sơ công ty | Company Service (`:5002`) |
| Đăng tin tuyển dụng | Job Service (`:5003`) |
| CV ứng viên | Resume Service (`:5004`) |
| Nộp đơn ứng tuyển | Application Service (`:5005`) |
| Việc đã lưu | Preferences Service (`:5006`) |

Mỗi service = một chương trình Spring Boot riêng, database riêng, port riêng.

### 0.2. Khi tách ra thì phát sinh 3 đau đầu

1. **Không biết “cửa hàng kia đang ở đâu”** (IP/port đổi liên tục)  
   → cần **Eureka** (bước 38)

2. **Mỗi cửa hàng giữ sổ cấu hình riêng** (mật khẩu DB, secret…) → khó quản lý  
   → cần **Config Server** (bước 39–40)

3. **Khách hàng phải nhớ 6 địa chỉ** + tự gửi “tôi là ai” dễ giả mạo  
   → cần **API Gateway + JWT** (bước 41)

4. **Cửa hàng A gọi cửa hàng B** bằng code dài dòng / hardcode  
   → cần **OpenFeign** (bước 42)

### 0.3. Bản đồ cả hệ thống (nhìn một lần cho nhớ)

```text
Người dùng (Postman / App)
        │
        │  Chỉ cần nhớ 1 địa chỉ: http://localhost:8080
        ▼
┌───────────────────┐
│   API Gateway     │  ← cổng bảo vệ: kiểm tra thẻ JWT
│   port 8080       │  ← rồi chuyển request đúng service
└─────────┬─────────┘
          │
    ┌─────┴──────┬──────────┬──────────┐
    ▼            ▼          ▼          ▼
 User:5001   Job:5003  App:5005   ... các service khác
    │            │          │
    └────────────┴────┬─────┘
                      │  “Tôi đây / Bạn ở đâu?”
                      ▼
              ┌───────────────┐
              │ Eureka :8761  │  ← danh bạ điện thoại của hệ thống
              └───────────────┘
                      ▲
              ┌───────┴───────┐
              │ Config :8888  │  ← tủ hồ sơ cấu hình chung
              └───────────────┘
```

### 0.4. Thứ tự bật máy (nhớ kỹ)

Luôn bật theo thứ tự này:

```text
1) Eureka        (danh bạ phải có trước)
2) Config Server (tủ config phải có trước)
3) Các business services (5001→5006)
4) API Gateway   (cổng vào cuối cùng)
```

Giống mở cửa hàng: trước hết phải có **danh bạ thành phố** và **quy định chung**, rồi mới mở từng cửa hàng, cuối cùng mở **cổng chính đón khách**.

### 0.5. Version đang dùng

| Thứ | Version |
|-----|---------|
| Spring Boot | 4.0.7 |
| Spring Cloud | 2025.1.2 |
| Java | 17 |

### 0.6. Bản đồ file change theo từng bước (tra cứu nhanh)

Khi học code, đừng chỉ đọc lý thuyết — **mở đúng file** tương ứng step:

| Step | Việc làm | File / folder chính (đã tạo hoặc sửa) |
|------|----------|----------------------------------------|
| **38** | Eureka Server + Client | `cloud/eureka-server/**`, `pom.xml` (BOM Cloud), mỗi `services/*/pom.xml` + `*Application.java` + `application.yaml` (block `eureka`) |
| **39** | Config Server + file config tập trung | `cloud/config-server/**`, đặc biệt `…/resources/config/*.yaml` |
| **40** | Service nối Config Server | mỗi `services/*/pom.xml` (`starter-config`), mỗi `services/*/…/application.yaml` (`spring.config.import`) |
| **41** | API Gateway + JWT filter + routes | `cloud/api-gateway/**`, `…/config/api-gateway.yaml` (routes), `JwtAuthGlobalFilter.java` |
| **42** | OpenFeign thay RestClient | `services/*/…/client/*Client.java`, `@EnableFeignClients`, `pom.xml` (`openfeign` + `loadbalancer`), chỗ gọi client trong `*ServiceImpl` |

Chi tiết từng file nằm ở **cuối tài liệu** → mục **“Bản đồ triển khai code (file-by-file)”**.

**Muốn làm theo tay từ A → Z?** → nhảy tới mục **“Triển khai từ đầu đến cuối”** (sau STEP 42).

---

# STEP 38 — Eureka: Đăng ký mọi microservice vào “danh bạ”

## 38.A. Giải thích cho người mới

### Ví dụ đời thường

Bạn có 6 cửa hàng, địa chỉ hay đổi.  
Thay vì ai cũng nhớ số nhà, mọi người **đăng ký sổ danh bạ trung tâm**:

> “Tôi là Job Service, đang ở số 5003.”

Khi Application Service muốn gọi Job Service, nó hỏi danh bạ:

> “Job Service đang ở đâu?”  
> → Danh bạ trả: “localhost:5003” (hoặc IP thật).

**Eureka Server** = cuốn danh bạ đó.  
**Eureka Client** = mỗi cửa hàng tự ghi tên vào sổ và định kỳ gọi điện: “Tôi vẫn mở cửa” (heartbeat).

### Từ khóa cần nhớ

| Từ | Nghĩa đơn giản |
|----|----------------|
| **Service Registry** | Kho đăng ký “ai đang online, ở đâu” |
| **Registration** | Service báo danh lần đầu khi start |
| **Heartbeat** | Ping định kỳ “tôi còn sống” |
| **Discovery** | Service hỏi “X đang ở đâu?” |
| **Instance** | Một process đang chạy của service |

### Eureka **không** làm gì?

- Không xử lý business (không apply job, không login)
- Không thay JWT
- Request thật **không đi xuyên Eureka** — Eureka chỉ cho địa chỉ, rồi hai service gọi nhau trực tiếp

## 38.B. Trong project đã làm gì?

### Eureka Server

```text
cloud/eureka-server/
```

- Port: **8761**
- Annotation: `@EnableEurekaServer`
- Dashboard: http://localhost:8761

Cấu hình quan trọng:

```yaml
eureka:
  client:
    register-with-eureka: false  # server không tự đăng ký vào chính nó
    fetch-registry: false
```

### Mỗi microservice là Client

Ví dụ Job Service:

1. Thêm dependency `spring-cloud-starter-netflix-eureka-client`
2. `@EnableDiscoveryClient` trên class `main`
3. Khai báo tên + địa chỉ Eureka:

```yaml
spring:
  application:
    name: job-portal-job-service   # tên trên danh bạ

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Bảng tên đăng ký (bắt buộc khớp khi gọi)

| Service | Tên trên Eureka | Port |
|---------|-----------------|------|
| User | `job-user-service` | 5001 |
| Company | `job-portal-company-service` | 5002 |
| Job | `job-portal-job-service` | 5003 |
| Resume | `job-portal-resume-service` | 5004 |
| Application | `job-portal-application-service` | 5005 |
| Preferences | `job-portal-preferences-service` | 5006 |

> Sai một ký tự trong `name` → Feign/Gateway báo *No instances available*.

## 38.C. Setup checklist (làm tay nếu tạo mới)

1. Tạo module `cloud/eureka-server`
2. Dependency eureka-server + `@EnableEurekaServer`
3. Port 8761
4. Mỗi service: dependency eureka-client + `@EnableDiscoveryClient`
5. Mỗi service: `spring.application.name` + `defaultZone`
6. Start Eureka → start services → xem dashboard có chữ **UP**

## 38.D. Kiểm tra đã hiểu chưa?

- [ ] Biết Eureka là danh bạ, không phải API business
- [ ] Biết heartbeat để làm gì
- [ ] Biết vì sao `spring.application.name` quan trọng
- [ ] Mở được http://localhost:8761 và thấy service

---

# STEP 39 — Config Server: tủ hồ sơ cấu hình tập trung

## 39.A. Giải thích cho người mới

### Ví dụ đời thường

Mỗi cửa hàng tự giữ một cuốn sổ:

- Mật khẩu két
- Số điện thoại nhà cung cấp
- Giờ mở cửa

Khi công ty đổi mật khẩu két → phải sửa **6 cuốn sổ**. Dễ sót, dễ lộ.

**Config Server** = **một tủ hồ sơ trung tâm** ở trụ sở:

- Shared rules (chung cho mọi cửa hàng)
- Riêng từng cửa hàng (DB của cửa hàng đó)

Cửa hàng khi mở cửa chỉ hỏi:

> “Tôi là Job Service, cho tôi bộ cấu hình hôm nay.”

### Native vs Git (đừng rối)

| Kiểu | Ý nghĩa | Project này |
|------|---------|-------------|
| **Native** | File yaml nằm trong máy Config Server | ✅ Đang dùng |
| **Git** | Config nằm trên GitHub/GitLab | Thường dùng production |

Local học thì **native** dễ nhất: không cần repo Git riêng.

## 39.B. Trong project đã làm gì?

```text
cloud/config-server/
  src/main/resources/application.yaml
  src/main/resources/config/
    application.yaml                      ← dùng chung
    job-user-service.yaml                 ← riêng từng service
    job-portal-company-service.yaml
    job-portal-job-service.yaml
    ...
    api-gateway.yaml
```

- Port Config Server: **8888**
- `@EnableConfigServer`
- Profile `native` + `search-locations: classpath:/config`

### Shared config thường chứa gì?

- Địa chỉ Eureka
- `jwt.secret` (bí mật ký/verify token)
- Cấu hình JPA chung

### Per-service thường chứa gì?

- `server.port`
- `spring.datasource.url` / username / password

## 39.C. Cách Config Server chọn đúng file

Service khai báo:

```yaml
spring:
  application:
    name: job-user-service
```

Config Server hiểu: lấy

1. `application.yaml` (chung)
2. `job-user-service.yaml` (riêng) — cái riêng ghi đè/bổ sung cái chung

Bạn có thể xem thử trên trình duyệt:

```text
http://localhost:8888/job-user-service/default
```

Thấy JSON chứa property → Config Server đang phục vụ đúng.

## 39.D. Setup checklist

1. Tạo `cloud/config-server`
2. Dependency `spring-cloud-config-server` (+ eureka-client nếu muốn đăng ký Eureka)
3. `@EnableConfigServer`
4. `spring.profiles.active=native`
5. Đặt file yaml vào `src/main/resources/config/`
6. Start `:8888` và mở URL `/{application}/default`

## 39.E. Kiểm tra đã hiểu chưa?

- [ ] Biết Config Server giải quyết “config rải rác”
- [ ] Biết `application.yaml` shared vs `service-name.yaml`
- [ ] Biết native = đọc file local/classpath
- [ ] Gọi được URL config trên trình duyệt

---

# STEP 40 — Service kết nối Config Server (tách config khỏi code)

## 40.A. Giải thích cho người mới

### “Decoupling” nghĩa là gì?

**Coupled (dính chặt):**  
Muốn đổi password DB → sửa file trong project → build lại jar → deploy lại.

**Decoupled (tách rời):**  
Password nằm trên Config Server.  
Service chỉ biết: “Tên tôi là X, Config Server ở đây.”  
Đổi password → sửa tủ config → restart service (thậm chí có thể refresh, tùy nâng cao).

### Bootstrap tối thiểu là gì?

Mỗi service vẫn cần **một ít** config local để **biết đường đến Config Server**:

```yaml
spring:
  application:
    name: job-portal-job-service
  config:
    import: optional:configserver:http://localhost:8888
```

| Phần | Giải thích siêu ngắn |
|------|----------------------|
| `name` | Tôi là ai (để lấy đúng file) |
| `config.import` | Lấy thêm config từ đâu |
| `optional:` | Config Server chưa kịp lên thì vẫn cố start (dùng fallback local) |

> Spring Boot mới **không** bắt buộc `bootstrap.yml` cũ nữa. Dùng `spring.config.import`.

## 40.B. Trong project đã làm gì?

Mỗi service trong `services/*/src/main/resources/application.yaml`:

- Có `spring.config.import=optional:configserver:http://localhost:8888`
- Vẫn có **fallback** datasource/port local (để học/dev đỡ phụ thuộc tuyệt đối)

Dependency thêm:

```xml
spring-cloud-starter-config
```

## 40.C. Luồng khi một service khởi động

```text
1. Đọc tên: job-portal-job-service
2. Gọi Config Server xin config
3. Merge: shared + riêng service (+ local fallback)
4. Đăng ký Eureka: “Tôi UP ở port …”
5. Sẵn sàng nhận request
```

## 40.D. Setup checklist

1. Thêm `spring-cloud-starter-config` vào từng service
2. Thêm `spring.config.import`
3. Đảm bảo `spring.application.name` khớp tên file trên Config Server
4. Start Config trước service
5. Đổi một giá trị trên Config Server → restart service → thấy thay đổi

## 40.E. Kiểm tra đã hiểu chưa?

- [ ] Biết service không cần nhồi hết config vào jar
- [ ] Biết vai trò `optional:configserver:...`
- [ ] Biết vì sao Config Server phải start trước service

---

# STEP 41 — API Gateway + JWT: một cổng vào và bảo vệ

## 41.A. Giải thích cho người mới

### Ví dụ đời thường

Không có cổng bảo vệ:

- Khách phải nhớ 6 cửa sau
- Ai cũng có thể đeo bảng tên giả “Tôi là user 1”

Có **cổng chính (API Gateway)**:

1. Khách chỉ vào **một cổng** (`:8080`)
2. Bảo vệ kiểm **thẻ vào cửa (JWT)**
3. Bảo vệ ghi phiếu nội bộ: “Khách này là userId=12, role=JOB_SEEKER”
4. Chỉ đường khách đến đúng phòng (route)

Microservice bên trong **tin phiếu nội bộ** (`X-User-Id`) vì phiếu do cổng phát, không phải khách tự viết.

### JWT là gì? (siêu ngắn)

Sau khi login thành công, User Service phát một **chuỗi token** (JWT):

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

Token giống **vé đã ký số**:

- Có thông tin: userId, email, role
- Có chữ ký bằng `jwt.secret`
- Hết hạn sau một thời gian

Gateway dùng **cùng secret** để kiểm: vé thật hay vé giả.

### JWT và Eureka khác nhau thế nào?

| Câu hỏi | Công cụ |
|---------|---------|
| Service B đang ở máy nào? | Eureka |
| Người gọi API là ai? | JWT + Gateway |

Hai thứ **không thay thế** nhau.

## 41.B. Trong project đã làm gì?

```text
cloud/api-gateway/
  ApiGatewayApplication.java
  security/JwtAuthGlobalFilter.java
```

- Port Gateway: **8080**
- Artifact: `spring-cloud-starter-gateway-server-webflux`
- Config route prefix mới: `spring.cloud.gateway.server.webflux.routes`

### Routing (chuyển đường)

Ví dụ ý tưởng:

```text
/auth/**              → job-user-service
/api/jobs/**          → job-portal-job-service
/api/applications/**  → job-portal-application-service
...
```

`uri: lb://job-portal-job-service` nghĩa là:

> Hỏi Eureka lấy địa chỉ thật của Job Service, rồi forward request.

### `JwtAuthGlobalFilter` làm gì? (theo thứ tự)

1. **Xóa** header `X-User-Id` / `X-User-Role` / `X-User-Email` nếu client tự gửi (chống giả mạo)
2. Nếu URL **public** → cho qua không cần token
3. Nếu URL **protected**:
   - Không có `Authorization: Bearer ...` → **401**
   - Token sai/hết hạn → **401**
   - Token đúng → gắn lại `X-User-Id`, `X-User-Role`, `X-User-Email` từ claims → forward

### Public vs Protected (trong filter hiện tại)

**Public (không cần login)**

- `/auth/**` (login, signup)
- Xem job / company / catalog (GET)

**Protected (cần JWT)**

- Apply job, quản lý resume, saved jobs, hầu hết thao tác ghi dữ liệu…

## 41.C. Cách gọi API đúng (người mới hay sai chỗ này)

### Sai

```http
POST http://localhost:5005/api/applications
X-User-Id: 12
```

(Gọi thẳng service + tự bịa user id)

### Đúng

```http
# 1) Login qua Gateway
POST http://localhost:8080/auth/login

# 2) Lấy jwt trong response

# 3) Gọi API qua Gateway
POST http://localhost:8080/api/applications
Authorization: Bearer <jwt>
Content-Type: application/json

{ "jobId": 1, "resumeId": 2 }
```

## 41.D. Setup checklist

1. Tạo `cloud/api-gateway`
2. Dependency gateway-server-webflux + eureka + config + jjwt
3. Khai báo routes `lb://...`
4. Viết GlobalFilter verify JWT
5. Đồng bộ `jwt.secret` với User Service
6. Client chỉ gọi `:8080`

## 41.E. Kiểm tra đã hiểu chưa?

- [ ] Biết Gateway là cổng vào duy nhất
- [ ] Biết JWT dùng để chứng minh danh tính
- [ ] Biết vì sao phải xóa `X-User-Id` spoof từ client
- [ ] Gọi được login + một API protected qua `:8080`

---

# STEP 42 — OpenFeign: gọi service khác bằng “khai báo”

## 42.A. Giải thích cho người mới

### Vấn đề

Application Service cần lấy thông tin Job.  
Cách cũ (RestClient): tự ghép URL, tự `.get()`, `.retrieve()`, `.body()`… dài và dễ sai.

### OpenFeign là gì?

Bạn chỉ viết **interface** giống Controller Spring:

```java
@FeignClient(name = "job-portal-job-service")
public interface JobClient {
    @GetMapping("/api/jobs/{jobId}")
    JobResponse getJobById(@PathVariable("jobId") Long jobId);
}
```

Spring Cloud **tự tạo implementation**:

1. Nhìn `name` → hỏi Eureka địa chỉ Job Service  
2. Gọi HTTP `GET /api/jobs/{id}`  
3. Trả về object Java

Giống đặt món: bạn ghi phiếu “1 phở bò”, bếp (Feign + Eureka) lo chế biến và mang tới.

### Feign cần LoadBalancer

Dependency thêm:

```xml
spring-cloud-starter-openfeign
spring-cloud-starter-loadbalancer
```

LoadBalancer giúp chọn instance khi có nhiều bản copy cùng service.

## 42.B. Trong project đã làm gì?

Các class `*Client` chuyển từ RestClient → interface Feign.

Ví dụ Application Service có:

- `JobClient`
- `CompanyClient`
- `ResumeClient`
- `UserClient`

Main class có:

```java
@EnableFeignClients
```

Gọi trong service bình thường như bean:

```java
jobClient.getJobById(jobId);
```

## 42.C. Header với Feign

Khi cần gửi identity nội bộ:

```java
@GetMapping("/api/companies/my")
CompanyResponse getCompanyByOwnerId(@RequestHeader("X-User-Id") Long ownerId);
```

Lưu ý: đây là **gọi service-to-service**.  
Request từ người dùng bên ngoài vẫn nên đi qua Gateway để có JWT.

## 42.D. Setup checklist

1. Thêm openfeign + loadbalancer
2. `@EnableFeignClients`
3. Tạo interface `@FeignClient(name = "...")`
4. `name` khớp Eureka
5. Path khớp controller bên kia
6. Inject interface vào `@Service` và gọi

## 42.E. Lỗi người mới hay gặp

| Hiện tượng | Nguyên nhân thường gặp |
|------------|------------------------|
| `No instances available for ...` | Service chưa UP / sai tên Feign |
| 404 | Path Feign khác path Controller |
| Connection refused | Service đích chưa start / Eureka chưa thấy |

Bắt lỗi bằng `feign.FeignException`.

## 42.F. Kiểm tra đã hiểu chưa?

- [ ] Biết Feign = khai báo interface, không viết HTTP thủ công
- [ ] Biết Feign nhờ Eureka tìm địa chỉ
- [ ] Biết `name` trong `@FeignClient` phải đúng tên đăng ký

---

# Triển khai từ đầu đến cuối

Đây là **công thức làm thực tế**: làm đúng thứ tự dưới đây.  
Mỗi giai xong thì đánh dấu trước khi sang bước kế — đừng nhảy cóc.

> Giả định: 6 business service (user/company/job/resume/application/preferences) **đã có sẵn** business code.  
> Phần này chỉ thêm Spring Cloud (Eureka → Config → Gateway → Feign).

```text
Chuẩn bị → 38 Eureka → 39 Config Server → 40 Nối Config
         → 41 Gateway+JWT → 42 Feign → Build → Start → Smoke test
```

---

## Giai đoạn 0 — Chuẩn bị môi trường

1. Cài **JDK 17+**, **Maven**, **PostgreSQL** đang chạy.
2. Tạo đủ database (ví dụ):
   - `job_portal_user`, `job_portal_company`, `job_portal_job`,
   - `job_portal_resume`, `job_portal_application`, `job_portal_preferences`
3. Mở `pom.xml` (root):
   - Thêm property `spring-cloud.version` = `2025.1.2`
   - Import BOM `spring-cloud-dependencies`
   - Đảm bảo có module `cloud` và `services`
4. Tạo `cloud/pom.xml` (aggregator) với 3 module:
   - `eureka-server`
   - `config-server`
   - `api-gateway`

**Checkpoint 0:** `mvn -q -DskipTests validate` không lỗi module.

---

## Giai đoạn 1 — STEP 38: Eureka

### 1.1. Tạo Eureka Server

1. Tạo module `cloud/eureka-server`
2. `pom.xml`: dependency `spring-cloud-starter-netflix-eureka-server`
3. Class main:

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication { ... }
```

4. `application.yaml`:

```yaml
server:
  port: 8761
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

5. Start Eureka → mở http://localhost:8761 (dashboard trống cũng OK).

### 1.2. Đăng ký từng business service

Lặp **6 lần** (mỗi service):

1. `pom.xml` → thêm `spring-cloud-starter-netflix-eureka-client`
2. Class `*Application` → thêm `@EnableDiscoveryClient`
3. `application.yaml` → khai báo:

```yaml
spring:
  application:
    name: <tên-trên-eureka>   # ví dụ job-portal-job-service
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Bảng tên bắt buộc khớp:**

| Service | `spring.application.name` | Port |
|---------|---------------------------|------|
| User | `job-user-service` | 5001 |
| Company | `job-portal-company-service` | 5002 |
| Job | `job-portal-job-service` | 5003 |
| Resume | `job-portal-resume-service` | 5004 |
| Application | `job-portal-application-service` | 5005 |
| Preferences | `job-portal-preferences-service` | 5006 |

**Checkpoint 38:** Start Eureka + 1–2 service → dashboard thấy status **UP**.

---

## Giai đoạn 2 — STEP 39: Config Server

1. Tạo module `cloud/config-server`
2. Dependency: `spring-cloud-config-server` (+ `eureka-client` nếu muốn nó cũng lên dashboard)
3. Class main: `@EnableConfigServer` (+ `@EnableDiscoveryClient` tùy chọn)
4. `application.yaml` Config Server:

```yaml
server:
  port: 8888
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config
```

5. Tạo folder `cloud/config-server/src/main/resources/config/` với các file:

| File | Nội dung chính |
|------|----------------|
| `application.yaml` | Shared: Eureka URL, `jwt.secret`, JPA chung |
| `job-user-service.yaml` | port + datasource user |
| `job-portal-company-service.yaml` | port + datasource company |
| `job-portal-job-service.yaml` | port + datasource job |
| `job-portal-resume-service.yaml` | port + datasource resume |
| `job-portal-application-service.yaml` | port + datasource application |
| `job-portal-preferences-service.yaml` | port + datasource preferences |
| `api-gateway.yaml` | port `8080` + routes (có thể viết tạm, hoàn thiện ở bước 41) |

**Checkpoint 39:** Start Config (`:8888`) → mở

```text
http://localhost:8888/job-user-service/default
```

thấy JSON property (không 404).

---

## Giai đoạn 3 — STEP 40: Mỗi service nối Config Server

Với **mỗi** business service:

1. `pom.xml` → thêm `spring-cloud-starter-config`
2. `application.yaml` → thêm:

```yaml
spring:
  application:
    name: <khớp file trên Config Server>
  config:
    import: optional:configserver:http://localhost:8888
```

3. Giữ fallback local (`server.port`, `datasource`) để dễ học/dev.

**Checkpoint 40:**

1. Start: Eureka → Config → 1 service  
2. Service start thành công  
3. Đổi 1 giá trị trên Config Server (vd. `show-sql`) → restart service → thấy hiệu lực

---

## Giai đoạn 4 — STEP 41: API Gateway + JWT

### 4.1. Tạo module Gateway

1. Module `cloud/api-gateway`
2. Dependencies tối thiểu:
   - `spring-cloud-starter-gateway-server-webflux`
   - `spring-cloud-starter-netflix-eureka-client`
   - `spring-cloud-starter-config`
   - `spring-cloud-starter-loadbalancer`
   - `jjwt-api` / `jjwt-impl` / `jjwt-jackson`
3. Class main: `@SpringBootApplication` + `@EnableDiscoveryClient`
4. Local `application.yaml`: `name: api-gateway`, `config.import`, port fallback `8080`

### 4.2. Khai báo routes (trên Config hoặc local)

Trong `config/api-gateway.yaml` (hoặc local yaml), pattern:

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: auth-service
              uri: lb://job-user-service
              predicates:
                - Path=/auth/**
            - id: job-service-jobs
              uri: lb://job-portal-job-service
              predicates:
                - Path=/api/jobs/**
            # ... tương tự companies, resumes, applications, saved-jobs
```

`lb://...` = hỏi Eureka rồi forward.

### 4.3. Viết `JwtAuthGlobalFilter`

File: `cloud/api-gateway/.../security/JwtAuthGlobalFilter.java`

Logic bắt buộc:

1. **Xóa** `X-User-Id` / `X-User-Role` / `X-User-Email` nếu client tự gửi  
2. Path public → cho qua  
3. Path protected → bắt `Authorization: Bearer …`  
4. Verify JWT bằng **cùng** `jwt.secret` với User Service  
5. Token OK → gắn lại header nội bộ `X-User-*` rồi forward

**Checkpoint 41:**

| Thử | Kỳ vọng |
|-----|---------|
| `GET http://localhost:8080/api/jobs` | 200 (public) |
| `POST http://localhost:8080/api/applications` (không token) | **401** |
| `POST http://localhost:8080/auth/login` | Trả JWT |

---

## Giai đoạn 5 — STEP 42: OpenFeign

Chỉ làm cho service **có gọi chéo** (company, job, application, preferences):

1. `pom.xml` → thêm:
   - `spring-cloud-starter-openfeign`
   - `spring-cloud-starter-loadbalancer`
2. `*Application` → `@EnableFeignClients`
3. Tạo interface trong package `client`, ví dụ:

```java
@FeignClient(name = "job-portal-job-service")
public interface JobClient {
    @GetMapping("/api/jobs/{jobId}")
    JobResponse getJobById(@PathVariable("jobId") Long jobId);
}
```

4. Trong `*ServiceImpl` / `AdminGuard`: inject interface, gọi như bean bình thường  
5. Bắt lỗi bằng `feign.FeignException` (không còn RestClient cũ)

**Checkpoint 42:** Apply job / saved-job / admin check chạy được qua Feign (không hardcode `localhost:500x`).

---

## Giai đoạn 6 — Build toàn bộ

Từ root project:

```bash
mvn -DskipTests clean install
```

Thấy `BUILD SUCCESS` rồi mới sang giai start.

---

## Giai đoạn 7 — Start theo đúng thứ tự

**Luôn** bật theo hàng này (mỗi app 1 terminal / 1 Run Configuration):

```text
1. eureka-server                         (:8761)
2. config-server                         (:8888)
3. job-user-service                      (:5001)
4. job-portal-company-service            (:5002)
5. job-portal-job-service                (:5003)
6. job-portal-resume-service             (:5004)
7. job-portal-application-service        (:5005)
8. job-portal-preferences                (:5006)  # app name trên Eureka: job-portal-preferences-service
9. api-gateway                           (:8080)
```

Chờ mỗi cái log “Started …” rồi mới bật cái sau.

---

## Giai đoạn 8 — Smoke test (xác nhận cả chuỗi)

Làm lần lượt:

| # | Việc | URL / hành động | OK khi |
|---|------|-----------------|--------|
| 1 | Eureka | http://localhost:8761 | Thấy đủ service **UP** |
| 2 | Config | http://localhost:8888/job-user-service/default | JSON config |
| 3 | Public API | `GET http://localhost:8080/api/jobs` | 200 |
| 4 | Login | `POST http://localhost:8080/auth/login` + body email/password | Có JWT |
| 5 | Protected không token | `POST http://localhost:8080/api/applications` | **401** |
| 6 | Protected có JWT | Cùng API + header `Authorization: Bearer <jwt>` | Không còn 401 vì thiếu token |

Nếu bước 6 còn lỗi business (thiếu job/resume…) thì đó là data — **Gateway + JWT đã OK**.

---

## Checklist tổng (in ra đánh dấu)

```text
[ ] 0  JDK / Maven / Postgres / BOM Cloud / cloud/pom.xml
[ ] 38 Eureka Server :8761
[ ] 38 Mỗi service: eureka-client + @EnableDiscoveryClient + application.name
[ ] 39 Config Server :8888 + folder config/*.yaml
[ ] 40 Mỗi service: starter-config + spring.config.import
[ ] 41 api-gateway module + routes lb://
[ ] 41 JwtAuthGlobalFilter + jwt.secret đồng bộ
[ ] 42 openfeign + loadbalancer + @EnableFeignClients
[ ] 42 *Client interfaces + ServiceImpl dùng Feign
[ ] Build SUCCESS
[ ] Start đúng thứ tự
[ ] Smoke test 1→6 pass
```

---

## Công thức nhớ 1 câu / step

| Step | Nhớ 1 câu |
|------|-----------|
| **38 Eureka** | Ai online thì ghi vào danh bạ |
| **39 Config Server** | Config để một chỗ |
| **40 Connect Config** | Service hỏi config khi start |
| **41 Gateway + JWT** | Một cổng vào + kiểm vé |
| **42 Feign** | Gọi service khác bằng interface |

## Sơ đồ liên hệ công nghệ (đừng nhầm)

```text
Người dùng
   │ JWT
   ▼
Gateway ──routing──► Microservice A
                         │ Feign
                         ▼
                    Microservice B
                         ▲
                    Eureka (địa chỉ)
Config Server ──config──► A, B, Gateway
```

---

# Bản đồ triển khai code (file-by-file)

Phần này trả lời câu hỏi: **“Mỗi bước đổi file nào?”**  
Ký hiệu: **[NEW]** = tạo mới · **[EDIT]** = sửa file có sẵn

---

## STEP 38 — Eureka

### Shared / parent

| File | Loại | Việc làm |
|------|------|----------|
| `pom.xml` (root) | [EDIT] | Thêm `spring-cloud.version`, import BOM `spring-cloud-dependencies`, module `cloud` |
| `cloud/pom.xml` | [NEW] | Parent aggregator: `eureka-server`, `config-server`, `api-gateway` |

### Eureka Server (module mới)

| File | Loại | Việc làm |
|------|------|----------|
| `cloud/eureka-server/pom.xml` | [NEW] | Dependency `spring-cloud-starter-netflix-eureka-server` |
| `cloud/eureka-server/…/EurekaServerApplication.java` | [NEW] | `@EnableEurekaServer` |
| `cloud/eureka-server/…/application.yaml` | [NEW] | Port `8761`, tắt self-register |

### Mỗi business service (Client)

Lặp lại pattern cho 6 service: user, company, job, resume, application, preferences.

| File | Loại | Việc làm |
|------|------|----------|
| `services/<svc>/pom.xml` | [EDIT] | Thêm `spring-cloud-starter-netflix-eureka-client` |
| `services/<svc>/…/*Application.java` | [EDIT] | Thêm `@EnableDiscoveryClient` |
| `services/<svc>/…/application.yaml` | [EDIT] | `spring.application.name` + `eureka.client.service-url.defaultZone` |

Ví dụ mở nhanh:

- `services/job-user-service/src/main/java/…/JobUserServiceApplication.java`
- `services/job-portal-job-service/src/main/java/…/JobPortalJobServiceApplication.java`

---

## STEP 39 — Config Server

| File | Loại | Việc làm |
|------|------|----------|
| `cloud/config-server/pom.xml` | [NEW] | `spring-cloud-config-server` (+ eureka-client) |
| `cloud/config-server/…/ConfigServerApplication.java` | [NEW] | `@EnableConfigServer` (+ `@EnableDiscoveryClient`) |
| `cloud/config-server/…/application.yaml` | [NEW] | Port `8888`, `native`, `search-locations: classpath:/config` |
| `cloud/config-server/…/config/application.yaml` | [NEW] | **Shared**: Eureka URL, `jwt.secret`, JPA chung… |
| `cloud/config-server/…/config/job-user-service.yaml` | [NEW] | Port / datasource user |
| `cloud/config-server/…/config/job-portal-company-service.yaml` | [NEW] | Port / datasource company |
| `cloud/config-server/…/config/job-portal-job-service.yaml` | [NEW] | Port / datasource job |
| `cloud/config-server/…/config/job-portal-resume-service.yaml` | [NEW] | Port / datasource resume |
| `cloud/config-server/…/config/job-portal-application-service.yaml` | [NEW] | Port / datasource application |
| `cloud/config-server/…/config/job-portal-preferences-service.yaml` | [NEW] | Port / datasource preferences |
| `cloud/config-server/…/config/api-gateway.yaml` | [NEW] | Port gateway + **routes** `lb://…` |

> Tên file yaml **phải khớp** `spring.application.name` của service.

---

## STEP 40 — Service kết nối Config Server

Với **mỗi** service trong `services/`:

| File | Loại | Việc làm |
|------|------|----------|
| `services/<svc>/pom.xml` | [EDIT] | Thêm `spring-cloud-starter-config` |
| `services/<svc>/src/main/resources/application.yaml` | [EDIT] | Thêm `spring.config.import: optional:configserver:http://localhost:8888` |

Ví dụ đã có trong repo:

```text
services/job-user-service/src/main/resources/application.yaml
services/job-portal-company-service/src/main/resources/application.yaml
services/job-portal-job-service/src/main/resources/application.yaml
services/job-portal-resume-service/src/main/resources/application.yaml
services/job-portal-application-service/src/main/resources/application.yaml
services/job-portal-preferences/src/main/resources/application.yaml
```

Local vẫn giữ fallback `server.port` / `datasource` để chạy học tập khi Config chưa lên.

---

## STEP 41 — API Gateway + JWT

| File | Loại | Việc làm |
|------|------|----------|
| `cloud/api-gateway/pom.xml` | [NEW] | `gateway-server-webflux` + eureka + config + loadbalancer + jjwt |
| `cloud/api-gateway/…/ApiGatewayApplication.java` | [NEW] | `@SpringBootApplication` + `@EnableDiscoveryClient` |
| `cloud/api-gateway/…/security/JwtAuthGlobalFilter.java` | [NEW] | Verify JWT, strip spoof headers, gắn `X-User-*` |
| `cloud/api-gateway/…/application.yaml` | [NEW] | Bootstrap tối thiểu + `config.import` |
| `cloud/config-server/…/config/api-gateway.yaml` | [EDIT/NEW] | Routes: `/auth/**`, `/api/jobs/**`, … → `lb://service-name` |

Luồng đọc code nên theo thứ tự:

1. `api-gateway.yaml` (route đi đâu?)  
2. `JwtAuthGlobalFilter.java` (ai được vào?)  
3. Controller trong service đích (business thật)

---

## STEP 42 — OpenFeign

### Dependency + bật Feign

| File | Loại | Việc làm |
|------|------|----------|
| `services/<svc-có-gọi-chéo>/pom.xml` | [EDIT] | `openfeign` + `loadbalancer` |
| `…/*Application.java` | [EDIT] | `@EnableFeignClients` |

Áp dụng cho service có gọi service khác: **company, job, application, preferences** (user/resume thường không cần Feign nếu không gọi chéo).

### Interface Feign (thay RestClient)

| Service | File client |
|---------|-------------|
| Application | `…/client/JobClient.java`, `CompanyClient.java`, `ResumeClient.java`, `UserClient.java` |
| Job | `…/client/CompanyClient.java`, `UserClient.java` |
| Company | `…/client/UserClient.java` |
| Preferences | `…/client/JobClient.java`, `CompanyClient.java` |

### Chỗ dùng client (business)

| File (ví dụ) | Việc làm |
|--------------|----------|
| `…/ApplicationServiceImpl.java` | Inject Feign client, bắt `FeignException` thay RestClient |
| `…/JobServiceImpl.java` | Gọi `CompanyClient` / `UserClient` |
| `…/SavedJobServiceImpl.java` | Gọi `JobClient` / `CompanyClient` |
| `…/AdminGuard.java` (job/company) | Gọi `UserClient` kiểm admin |

Mỗi `@FeignClient(name = "...")` → `name` phải trùng bảng Eureka ở step 38.

---

## Cách “đi tuần” code như người mới

```text
1) Mở cloud/eureka-server          → hiểu danh bạ
2) Mở cloud/config-server/config   → hiểu config chung/riêng
3) Mở 1 service application.yaml   → thấy import Config + Eureka
4) Mở cloud/api-gateway            → thấy cổng + JWT filter
5) Mở 1 *Client.java + ServiceImpl → thấy Feign gọi chéo
```

Đừng cố đọc hết 6 service cùng lúc: chọn **Application Service** là đủ thấy gần như mọi layer (JWT header, Feign, Eureka name).

---

Nếu bạn mới học: hãy **chạy được login + GET jobs + một API có JWT** trước, rồi mới đào sâu từng layer. Hiểu đúng “ai giải quyết bài toán nào” quan trọng hơn nhớ hết annotation.
