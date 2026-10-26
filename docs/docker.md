# Docker cho Job Portal — Steps 51 → 54 (cho người mới)

Tài liệu này đi theo 4 video + **code đã có trong repo**:

| # | Video | Việc làm trong project |
|---|-------|------------------------|
| **51** | Installing Docker and Understanding the Basics | Cài Docker Desktop, nắm image/container |
| **52** | Build Docker Images with Google Jib | Cấu hình `jib-maven-plugin` (root + 1 module) |
| **53** | Build Docker Images for All Microservices | Jib cho 10 module + env URL cho Docker |
| **54** | Multi-Container Orchestration with Docker Compose | `docker-compose.yml` + Postgres init + `.env` |

Muốn hiểu **từng dòng code** → kéo xuống mục **「Phụ lục — Giải thích từng dòng code」** (cuối file).

---

## File đã có trong repo (đối chiếu khi học)

| File | Vai trò |
|------|---------|
| `pom.xml` (root) | `pluginManagement` Jib + base image |
| `cloud/*/pom.xml`, `services/*/pom.xml` | Bật Jib + `jib.container.port` |
| `cloud/config-server/.../config/*.yaml` | URL DB/Eureka/JWT có `${ENV:default}` |
| `docker-compose.yml` | Chạy cả cụm |
| `docker/postgres/init/01-databases.sql` | Tạo 6 database |
| `.env.example` | Mẫu biến môi trường |
| `scripts/docker-build-images.ps1` | Build hết image bằng **Jib** |
| `docker/Dockerfile.jvm` | Dockerfile multi-stage (**không Jib**) |
| `docker/Dockerfile.from-jar` | Dockerfile mỏng: chỉ copy jar có sẵn |
| `docker-compose.dockerfile.yml` | Override Compose để `build:` bằng Dockerfile |
| `scripts/docker-build-images-dockerfile.ps1` | Build hết image bằng Dockerfile |

### Hai cách tạo image (chọn 1)

| Cách | Lệnh | Khi nào |
|------|------|---------|
| **A. Jib** (video 52–53) | `.\scripts\docker-build-images.ps1` | Không viết Dockerfile |
| **B. Docker thường** | `.\scripts\docker-build-images-dockerfile.ps1` | Học Dockerfile / CI thuần Docker |

Compose chạy giống nhau: `docker compose up -d` (cùng tên image `job-portal/...`).

### Chạy nhanh (khi đã hiểu)

```powershell
# 1) Bật Docker Desktop
copy .env.example .env
# sửa GEMINI_API_KEY trong .env

# Cách A — Jib
.\scripts\docker-build-images.ps1

# — hoặc — Cách B — Dockerfile thường
.\scripts\docker-build-images-dockerfile.ps1

docker compose up -d
```

Smoke: http://localhost:8761 · `GET http://localhost:8080/api/jobs`

---

## Phần 0 — Vì sao cần Docker?

**Trước Docker:** mỗi máy JDK/Postgres khác nhau → “chạy được trên máy tôi”.  
**Với Docker:** đóng app + runtime vào **image** → chạy **container** giống nhau.

```text
Client → localhost:8080 → api-gateway (container)
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
   eureka-server        config-server         business services
         │                    │                    │
         └────────────────────┴──────────► postgres
                      network: job-net
```

| Image | Module Maven | Port |
|-------|--------------|------|
| `job-portal/eureka-server` | `cloud/eureka-server` | 8761 |
| `job-portal/config-server` | `cloud/config-server` | 8888 |
| `job-portal/api-gateway` | `cloud/api-gateway` | 8080 |
| `job-portal/job-user-service` | `services/job-user-service` | 5001 |
| `job-portal/job-portal-company-service` | `services/job-portal-company-service` | 5002 |
| `job-portal/job-portal-job-service` | `services/job-portal-job-service` | 5003 |
| `job-portal/job-portal-resume-service` | `services/job-portal-resume-service` | 5004 |
| `job-portal/job-portal-application-service` | `services/job-portal-application-service` | 5005 |
| `job-portal/job-portal-preferences-service` | `services/job-portal-preferences` | 5006 |
| `job-portal/job-portal-ai-service` | `services/job-portal-ai-service` | 5007 |
| `postgres:16` | (image sẵn) | 5432 |

`common-lib` = thư viện → **không** tạo Docker image.

---

# STEP 51 — Cài Docker + kiến thức nền

## 51.A. Lý thuyết ngắn

| Từ | Nghĩa đơn giản |
|----|----------------|
| **Image** | Công thức đóng sẵn (app + JRE) |
| **Container** | Một process đang chạy từ image |
| **Volume** | Ổ lưu data bền (Postgres) |
| **Network** | Nội bộ để container gọi nhau bằng **tên service** |
| `-p 8080:8080` | Cổng máy host → cổng trong container |

## 51.B. Các bước làm

1. Cài [Docker Desktop](https://www.docker.com/products/docker-desktop/), bật WSL2 nếu được hỏi.  
2. Đảm bảo icon cá voi **Running**.  
3. Kiểm tra:

```powershell
docker version
docker run --rm hello-world
```

4. Nhớ vài lệnh:

```powershell
docker images
docker ps
docker ps -a
docker logs <container>
docker stop <container>
docker rm <container>
```

## 51.C. Checkpoint

- [ ] `docker version` có Client **và** Server  
- [ ] `hello-world` chạy OK  
- [ ] Phân biệt image ≠ container  

---

# STEP 52 — Build image bằng Google Jib (1 module trước)

## 52.A. Lý thuyết

**Dockerfile cũ:** viết file → `docker build` (cần daemon).  
**Jib:** plugin Maven đóng Java app thành image, **không cần Dockerfile**.

| Goal | Khi dùng |
|------|----------|
| `jib:dockerBuild` | Đẩy vào Docker Desktop local (**học / Compose**) |
| `jib:buildTar` | Xuất `.tar` (không cần daemon; dùng để kiểm tra plugin) |
| `jib:build` | Đẩy registry (Docker Hub / GHCR) |

## 52.B. Bước 1 — Khai báo Jib chung ở root `pom.xml`

**File:** `pom.xml`

```xml
<properties>
    <jib-maven-plugin.version>3.5.1</jib-maven-plugin.version>
    <!-- ECR Public: tránh 401 / rate limit Docker Hub khi kéo base image -->
    <jib.from.image>public.ecr.aws/docker/library/eclipse-temurin:17-jre</jib.from.image>
    <jib.container.port>8080</jib.container.port>
</properties>
```

Trong `<pluginManagement>`:

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>${jib-maven-plugin.version}</version>
    <configuration>
        <from>
            <image>${jib.from.image}</image>
        </from>
        <to>
            <image>job-portal/${project.artifactId}</image>
            <tags>
                <tag>${project.version}</tag>
                <tag>latest</tag>
            </tags>
        </to>
        <container>
            <ports>
                <port>${jib.container.port}</port>
            </ports>
            <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
        </container>
    </configuration>
</plugin>
```

Giải thích:

- `to.image` = `job-portal/` + `artifactId` của **module con**  
- Mỗi module chỉ việc **override port** + khai báo plugin (không copy lại cả block)

## 52.C. Bước 2 — Bật Jib trên 1 module (ví dụ Eureka)

**File:** `cloud/eureka-server/pom.xml`

```xml
<properties>
    <jib.container.port>8761</jib.container.port>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>com.google.cloud.tools</groupId>
            <artifactId>jib-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

Ví dụ User Service (`services/job-user-service/pom.xml`):

```xml
<properties>
    <jib.container.port>5001</jib.container.port>
</properties>
<!-- ... trong <build><plugins>: thêm jib-maven-plugin như trên ... -->
```

## 52.D. Bước 3 — Build thử 1 image

**Docker Desktop phải đang chạy** (cho `dockerBuild`).

```powershell
# Từ root repo
.\services\job-user-service\mvnw.cmd -f pom.xml `
  -pl cloud/eureka-server -am package jib:dockerBuild -DskipTests

docker images job-portal/eureka-server
```

Không bật được Docker? Kiểm tra plugin bằng tar:

```powershell
.\services\job-user-service\mvnw.cmd -f pom.xml `
  -pl cloud/eureka-server -am package jib:buildTar -DskipTests
# → cloud/eureka-server/target/jib-image.tar
```

## 52.E. Checkpoint 52

- [ ] Root dùng đúng artifact `jib-maven-plugin`  
- [ ] 1 module có `jib.container.port` + plugin  
- [ ] `jib:dockerBuild` hoặc `jib:buildTar` SUCCESS  

---

# STEP 53 — Jib cho tất cả microservices + URL Docker

## 53.A. Lý thuyết

Mỗi service = **một image** → deploy/scale độc lập.  
Trong Docker, **cấm** gọi `localhost` giữa các container — dùng tên service Compose (`eureka-server`, `postgres`, …).

| IDE (máy bạn) | Trong Compose |
|---------------|---------------|
| `localhost:8761` | `eureka-server:8761` |
| `localhost:8888` | `config-server:8888` |
| `localhost:5432` | `postgres:5432` |

## 53.B. Bước 1 — Gắn Jib đủ 10 module

Lặp pattern STEP 52 cho từng module + port:

| Module path | `jib.container.port` |
|-------------|----------------------|
| `cloud/eureka-server` | 8761 |
| `cloud/config-server` | 8888 |
| `cloud/api-gateway` | 8080 |
| `services/job-user-service` | 5001 |
| `services/job-portal-company-service` | 5002 |
| `services/job-portal-job-service` | 5003 |
| `services/job-portal-resume-service` | 5004 |
| `services/job-portal-application-service` | 5005 |
| `services/job-portal-preferences` | 5006 |
| `services/job-portal-ai-service` | 5007 |

## 53.B. Bước 2 — Config có thể override bằng env

**Shared** — `cloud/config-server/src/main/resources/config/application.yaml`:

```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}

jwt:
  secret: ${JWT_SECRET:asdfghjklpoiuytrewqzxcvbnmlkjhglpouhggfdsawqwertyyuiioplmnbvcxzasdfgh}
```

**Per-service DB** — ví dụ `config/job-user-service.yaml`:

```yaml
server:
  port: 5001

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/job_portal_user}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:123456789}
    driver-class-name: org.postgresql.Driver
```

IDE không set env → dùng default `localhost`.  
Compose set `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/...` → dùng host Docker.

**Eureka server** — `cloud/eureka-server/.../application.yaml`:

```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
```

Compose set `EUREKA_INSTANCE_HOSTNAME=eureka-server`.

**Quan trọng trong Compose:**  
`SPRING_CLOUD_CONFIG_OVERRIDE_SYSTEM_PROPERTIES=false`  
→ biến môi trường container **đè** được config từ Config Server (tránh vẫn dính `localhost`).

## 53.C. Bước 3 — Build hết image

**Cách 1 — script (khuyến nghị):**

```powershell
.\scripts\docker-build-images.ps1
```

Script làm gì (rút gọn):

```powershell
$Modules = @(
    "cloud/eureka-server",
    "cloud/config-server",
    "cloud/api-gateway",
    "services/job-user-service",
    "services/job-portal-company-service",
    "services/job-portal-job-service",
    "services/job-portal-resume-service",
    "services/job-portal-application-service",
    "services/job-portal-preferences",
    "services/job-portal-ai-service"
)

# build common-lib trước, rồi từng module:
# mvnw -pl $module -am package jib:dockerBuild -DskipTests
```

**Cách 2 — tay từng module:**

```powershell
.\services\job-user-service\mvnw.cmd -f pom.xml `
  -pl services/job-user-service -am package jib:dockerBuild -DskipTests
```

Kiểm tra:

```powershell
docker images "job-portal/*"
```

## 53.D. Checkpoint 53

- [ ] 10 image `job-portal/...:latest`  
- [ ] Config yaml dùng `${ENV:default}`  
- [ ] Hiểu IDE dùng localhost, Compose dùng tên service  

---

# STEP 54 — Docker Compose (cả hệ thống)

## 54.A. Lý thuyết

`docker-compose.yml` = bản vẽ: image nào, port nào, env nào, phụ thuộc ai, chung network/volume.  
Một lệnh: `docker compose up -d`.

Thứ tự logic:

```text
postgres → eureka-server → config-server → business (5001–5007) → api-gateway
```

## 54.B. Bước 1 — Init nhiều database

**File:** `docker/postgres/init/01-databases.sql`

```sql
CREATE DATABASE job_portal_user;
CREATE DATABASE job_portal_company;
CREATE DATABASE job_portal_job;
CREATE DATABASE job_portal_resume;
CREATE DATABASE job_portal_application;
CREATE DATABASE job_portal_preferences;
```

Chỉ chạy **lần đầu** khi volume Postgres trống. Muốn chạy lại init: `docker compose down -v` (xóa data).

## 54.C. Bước 2 — File `.env`

```powershell
copy .env.example .env
```

Nội dung mẫu (`.env.example`):

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=123456789
JWT_SECRET=asdfghjklpoiuytrewqzxcvbnmlkjhglpouhggfdsawqwertyyuiioplmnbvcxzasdfgh
GEMINI_API_KEY=replace_with_your_google_ai_studio_key
```

Không commit `.env` (đã có trong `.gitignore`).

## 54.D. Bước 3 — Viết / hiểu `docker-compose.yml`

### Env dùng chung (YAML anchor)

```yaml
name: job-portal

x-java-common-env: &java-common-env
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
  SPRING_CONFIG_IMPORT: optional:configserver:http://config-server:8888
  SPRING_CLOUD_CONFIG_OVERRIDE_SYSTEM_PROPERTIES: "false"
  SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-postgres}
  SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-123456789}
  JWT_SECRET: ${JWT_SECRET:-asdfghjklpoiuytrewqzxcvbnmlkjhglpouhggfdsawqwertyyuiioplmnbvcxzasdfgh}
```

### Postgres + Eureka + Config

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-123456789}
    ports: ["5432:5432"]
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
    networks: [job-net]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      retries: 12

  eureka-server:
    image: job-portal/eureka-server:latest
    environment:
      EUREKA_INSTANCE_HOSTNAME: eureka-server
    ports: ["8761:8761"]
    networks: [job-net]

  config-server:
    image: job-portal/config-server:latest
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    ports: ["8888:8888"]
    depends_on:
      eureka-server:
        condition: service_started
    networks: [job-net]
```

### Một business service (lặp pattern cho các service còn lại)

```yaml
  job-user-service:
    image: job-portal/job-user-service:latest
    environment:
      <<: *java-common-env
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/job_portal_user
    depends_on:
      postgres:
        condition: service_healthy
      config-server:
        condition: service_started
    networks: [job-net]
```

### AI + Gateway

```yaml
  job-portal-ai-service:
    image: job-portal/job-portal-ai-service:latest
    environment:
      <<: *java-common-env
      GEMINI_API_KEY: ${GEMINI_API_KEY:-}
    depends_on:
      config-server:
        condition: service_started
    networks: [job-net]

  api-gateway:
    image: job-portal/api-gateway:latest
    environment:
      <<: *java-common-env
    ports: ["8080:8080"]
    depends_on:
      - eureka-server
      - job-user-service
      - job-portal-job-service
    networks: [job-net]

networks:
  job-net:
    driver: bridge

volumes:
  pgdata:
```

> Bản đủ đủ service nằm ở root: `docker-compose.yml`.

## 54.E. Bước 4 — Up / down / log

```powershell
docker compose up -d
docker compose ps
docker compose logs -f api-gateway
docker compose logs -f job-user-service

# Sửa code 1 service → rebuild image → recreate container
.\services\job-user-service\mvnw.cmd -f pom.xml `
  -pl services/job-user-service -am package jib:dockerBuild -DskipTests
docker compose up -d --no-deps --force-recreate job-user-service

docker compose down        # giữ volume DB
docker compose down -v     # xóa cả data Postgres
```

## 54.F. Smoke test

| # | Việc | Kỳ vọng |
|---|------|---------|
| 1 | http://localhost:8761 | Service **UP** |
| 2 | http://localhost:8888/job-user-service/default | JSON config |
| 3 | `GET http://localhost:8080/api/jobs` | 200 |
| 4 | `POST http://localhost:8080/auth/login` | Có JWT |
| 5 | API protected không Bearer | 401 |

## 54.G. Checkpoint 54

- [ ] `docker compose up -d` không lỗi thiếu image  
- [ ] Eureka thấy service  
- [ ] Gọi được API qua `:8080`  

---

# Checklist triển khai từ đầu đến cuối

```text
[ ] 51  Docker Desktop Running + hello-world
[ ] 52/53  Chọn A (Jib) hoặc B (Dockerfile) — xem mục dưới
[ ] 53  Config/Eureka yaml: ${ENV:localhost-default}
[ ] 54  docker/postgres/init + .env + docker compose up -d + smoke test
```

---

# Nếu không dùng Jib → Docker bình thường (Dockerfile)

Jib = Maven tự đóng image.  
**Docker bình thường** = bạn viết `Dockerfile` + lệnh `docker build` (hoặc `compose build`).

```text
So sánh nhanh
─────────────
Jib:        mvn jib:dockerBuild     → image (không cần Dockerfile)
Dockerfile: docker build -f ...     → image (cần Dockerfile + Docker daemon)
Compose:    cùng docker compose up  (cùng tên image)
```

## Cách B1 — Multi-stage (khuyến nghị khi không Jib)

### File: `docker/Dockerfile.jvm`

```dockerfile
FROM public.ecr.aws/docker/library/maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common-lib ./common-lib
COPY cloud ./cloud
COPY services ./services
ARG MODULE
RUN mvn -B -f pom.xml -pl ${MODULE} -am package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
ARG ARTIFACT
ARG VERSION=0.0.1-SNAPSHOT
COPY --from=build /workspace/${MODULE}/target/${ARTIFACT}-${VERSION}.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Giải thích từng dòng (`Dockerfile.jvm`)

| Dòng / khối | Ý nghĩa |
|-------------|---------|
| `FROM ...maven... AS build` | Stage 1: image có Maven + JDK để **biên dịch** |
| `AS build` | Đặt tên stage = `build` (stage sau copy từ đây) |
| `WORKDIR /workspace` | Thư mục làm việc trong container build |
| `COPY pom.xml ./` | Copy POM root vào image |
| `COPY common-lib ./common-lib` | Copy thư viện chung (service phụ thuộc) |
| `COPY cloud ./cloud` | Copy module cloud |
| `COPY services ./services` | Copy module business |
| `ARG MODULE` | Tham số build: đường dẫn module Maven |
| `RUN mvn ... -pl ${MODULE} -am package` | Build đúng 1 module (+ dependency) → ra file `.jar` |
| `FROM ...eclipse-temurin:17-jre` | Stage 2: image **chỉ JRE** (nhỏ, để chạy) |
| `ARG MODULE` / `ARTIFACT` / `VERSION` | Khai báo lại ARG vì stage mới không giữ ARG cũ |
| `COPY --from=build ... jar /app/app.jar` | Chỉ lấy file jar từ stage build, bỏ JDK/Maven |
| `ENTRYPOINT ["java","-jar","/app/app.jar"]` | Lệnh mặc định khi `docker run` / Compose start |

### Build 1 image bằng tay

```powershell
docker build -f docker/Dockerfile.jvm `
  --build-arg MODULE=cloud/eureka-server `
  --build-arg ARTIFACT=eureka-server `
  -t job-portal/eureka-server:latest .
```

| Phần lệnh | Ý nghĩa |
|-----------|---------|
| `-f docker/Dockerfile.jvm` | Dùng file Dockerfile này |
| `--build-arg MODULE=...` | Truyền vào `ARG MODULE` |
| `--build-arg ARTIFACT=...` | Tên artifactId (tên file jar) |
| `-t job-portal/eureka-server:latest` | Đặt tên + tag image |
| `.` | **Build context** = root repo (Docker gửi source vào daemon) |

### Build cả cụm bằng Compose override

**File:** `docker-compose.dockerfile.yml` (rút gọn 1 service):

```yaml
services:
  eureka-server:
    build:
      context: .
      dockerfile: docker/Dockerfile.jvm
      args:
        MODULE: cloud/eureka-server
        ARTIFACT: eureka-server
```

| Key | Ý nghĩa |
|-----|---------|
| `build:` | Bảo Compose **tự docker build** thay vì chỉ kéo image có sẵn |
| `context: .` | Context = root repo |
| `dockerfile:` | Path tới Dockerfile |
| `args:` | Map thành `--build-arg` |

Lệnh:

```powershell
# Build hết service Java bằng Dockerfile
.\scripts\docker-build-images-dockerfile.ps1

# hoặc
docker compose -f docker-compose.yml -f docker-compose.dockerfile.yml build
docker compose -f docker-compose.yml -f docker-compose.dockerfile.yml up -d
```

`-f` hai lần = file gốc + file **ghi đè/bổ sung** `build:`.

### Bảng `MODULE` / `ARTIFACT` (Dockerfile)

| Service Compose | MODULE | ARTIFACT |
|-----------------|--------|----------|
| eureka-server | `cloud/eureka-server` | `eureka-server` |
| config-server | `cloud/config-server` | `config-server` |
| api-gateway | `cloud/api-gateway` | `api-gateway` |
| job-user-service | `services/job-user-service` | `job-user-service` |
| job-portal-company-service | `services/job-portal-company-service` | `job-portal-company-service` |
| job-portal-job-service | `services/job-portal-job-service` | `job-portal-job-service` |
| job-portal-resume-service | `services/job-portal-resume-service` | `job-portal-resume-service` |
| job-portal-application-service | `services/job-portal-application-service` | `job-portal-application-service` |
| job-portal-preferences-service | `services/job-portal-preferences` | `job-portal-preferences-service` |
| job-portal-ai-service | `services/job-portal-ai-service` | `job-portal-ai-service` |

> Preferences: **folder** Maven = `job-portal-preferences`, **artifactId** = `job-portal-preferences-service`.

---

## Cách B2 — Dockerfile mỏng (jar build sẵn trên máy)

Dễ hiểu nhất khi mới học: **Maven trên host** → Docker chỉ đóng jar.

### File: `docker/Dockerfile.from-jar`

```dockerfile
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Giải thích từng dòng

| Dòng | Ý nghĩa |
|------|---------|
| `FROM ...temurin:17-jre` | Nền chỉ cần JRE để chạy Spring Boot |
| `WORKDIR /app` | Thư mục làm việc trong container |
| `ARG JAR_FILE` | Đường dẫn jar **trong build context** |
| `COPY ${JAR_FILE} /app/app.jar` | Copy jar từ máy (context) vào image |
| `ENTRYPOINT ...` | Chạy jar khi container start |

### Các bước làm (Eureka)

```powershell
# 1) Build jar trên máy (root repo)
.\services\job-user-service\mvnw.cmd -f pom.xml `
  -pl cloud/eureka-server -am package -DskipTests

# 2) Context = thư mục module (root .dockerignore đang loại **/target)
cd cloud\eureka-server
docker build -f ../../docker/Dockerfile.from-jar `
  --build-arg JAR_FILE=target/eureka-server-0.0.1-SNAPSHOT.jar `
  -t job-portal/eureka-server:latest .
cd ..\..
```

Vì sao `cd` vào module?  
Root `.dockerignore` có `**/target` → nếu context = root, Docker **không gửi** file jar vào daemon → `COPY` fail. Multi-stage (`Dockerfile.jvm`) không bị vì jar sinh **trong** stage build.

---

## Jib vs Dockerfile — nên chọn gì?

| Tiêu chí | Jib | Dockerfile |
|----------|-----|------------|
| Cần viết Dockerfile | Không | Có |
| Cần Docker daemon lúc build | `dockerBuild`: có | Luôn có |
| Layer Java tối ưu sẵn | Có | Tự chỉnh multi-stage |
| Học Docker thuần | Í | Rõ `FROM`/`COPY`/`ENTRYPOINT` |
| CI phổ biến | Maven goal | `docker build` / BuildKit |

Project hỗ trợ **cả hai**: image cùng tên `job-portal/<artifact>:latest` → `docker-compose.yml` không đổi.

---

# Lỗi hay gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|------------|-------------|------------|
| `Cannot connect to the Docker daemon` | Desktop tắt | Bật Docker Desktop |
| `401 Unauthorized` kéo `eclipse-temurin` | Docker Hub anonymous | Dùng base `public.ecr.aws/docker/library/eclipse-temurin:17-jre` (đã cấu hình) |
| DB connection refused | Còn `localhost` trong container | Set `SPRING_DATASOURCE_URL=...@postgres:5432/...` + `OVERRIDE_SYSTEM_PROPERTIES=false` |
| `No instances available` | Service chưa UP / sai Eureka URL | `docker compose logs` + kiểm tra `EUREKA_CLIENT_...` |
| Init SQL không chạy | Volume cũ | `docker compose down -v` rồi `up` lại |
| Port đã chiếm | IDE đang chạy `:8080`/`:5432` | Stop app local hoặc đổi map port |
| AI lỗi start | Thiếu key | Điền `GEMINI_API_KEY` trong `.env` |

---

# Nhớ 1 câu / step

| Step | 1 câu |
|------|-------|
| **51** | Image = công thức, container = đang chạy |
| **52** | Jib = Maven tạo image, không cần Dockerfile |
| **53** | Mỗi microservice một image + hostname Docker |
| **54** | Compose = một YAML bật cả hệ thống |

Docker **không thay** Eureka / Config / Gateway / Feign — chỉ là cách **đóng gói và chạy** chúng.

---

# Phụ lục — Giải thích từng dòng code

Phần này đọc **chậm**: mỗi khối = file thật trong repo + chú thích từng dòng / từng key quan trọng.

---

## A. Root `pom.xml` — properties Jib

```xml
<jib-maven-plugin.version>3.5.1</jib-maven-plugin.version>
```
→ Version plugin Jib; gom một chỗ để đổi version dễ.

```xml
<jib.from.image>public.ecr.aws/docker/library/eclipse-temurin:17-jre</jib.from.image>
```
→ **Base image** (nền có sẵn JRE 17).  
→ Dùng mirror **ECR Public** thay vì `eclipse-temurin` trên Docker Hub (tránh lỗi 401 / rate limit khi kéo anonymous).

```xml
<jib.container.port>8080</jib.container.port>
```
→ Port mặc định trong image. Module con **ghi đè** (8761, 5001, …).

---

## B. Root `pom.xml` — cấu hình `jib-maven-plugin`

```xml
<groupId>com.google.cloud.tools</groupId>
<artifactId>jib-maven-plugin</artifactId>
```
→ Đúng tọa độ Maven của Google Jib (không nhầm `jib.maven.plugin`).

```xml
<version>${jib-maven-plugin.version}</version>
```
→ Lấy version từ property ở trên.

```xml
<from>
    <image>${jib.from.image}</image>
</from>
```
→ Image **nền** (FROM trong Dockerfile tương đương). App Java của bạn chồng lên nền này.

```xml
<to>
    <image>job-portal/${project.artifactId}</image>
```
→ Tên image **đích** khi build xong.  
→ `${project.artifactId}` = tên module đang build, ví dụ `eureka-server` → image `job-portal/eureka-server`.

```xml
    <tags>
        <tag>${project.version}</tag>
        <tag>latest</tag>
    </tags>
</to>
```
→ Gắn 2 tag: version project (`0.0.1-SNAPSHOT`) và `latest` (Compose hay dùng `:latest`).

```xml
<container>
    <ports>
        <port>${jib.container.port}</port>
    </ports>
```
→ Metadata “container lắng nghe port nào” (giống EXPOSE). App Spring vẫn phải `server.port` khớp.

```xml
    <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
</container>
```
→ Thời gian tạo image = lúc build (tiện xem image mới).

> Khối này nằm trong **`pluginManagement`**: chỉ là “mẫu”. Module con phải **khai báo plugin** trong `<build><plugins>` mới thật sự chạy Jib.

---

## C. Module `cloud/eureka-server/pom.xml`

```xml
<jib.container.port>8761</jib.container.port>
```
→ Ghi đè port mặc định 8080 → Eureka dùng **8761**.

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```
→ Plugin đóng Spring Boot jar (packaging bình thường).

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
</plugin>
```
→ **Bật Jib** cho module này. Không cần viết lại `<configuration>` — kế thừa từ root `pluginManagement`.  
→ Khi chạy `jib:dockerBuild`, Maven biết đóng module này thành `job-portal/eureka-server`.

---

## D. Lệnh Maven build image

```powershell
.\services\job-user-service\mvnw.cmd -f pom.xml `
  -pl cloud/eureka-server -am package jib:dockerBuild -DskipTests
```

| Phần | Nghĩa |
|------|--------|
| `mvnw.cmd` | Maven Wrapper — không cần cài Maven global |
| `-f pom.xml` | Dùng root POM của monorepo |
| `-pl cloud/eureka-server` | Chỉ build module này (**p**roject **l**ist) |
| `-am` | **a**lso **m**ake — build luôn dependency cần thiết (parent/cloud) |
| `package` | Compile + đóng jar trước |
| `jib:dockerBuild` | Goal Jib: đưa image vào Docker Desktop local |
| `-DskipTests` | Bỏ test cho nhanh khi đóng image |

```powershell
docker images job-portal/eureka-server
```
→ Liệt kê image vừa tạo trên máy.

---

## E. Config shared — `config/application.yaml`

```yaml
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```

| Phần | Nghĩa |
|------|--------|
| `eureka.client.service-url.defaultZone` | Địa chỉ Eureka Client phải gọi để đăng ký / discovery |
| `${A:B}` | Cú pháp Spring: lấy biến môi trường / property `A`, không có thì dùng default `B` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Tên env (Spring map từ property, chữ hoa + `_`) |
| Default `http://localhost:8761/eureka/` | Chạy IDE trên máy bạn |

Trong Compose, set env = `http://eureka-server:8761/eureka/` → container tìm Eureka theo **tên service**.

```yaml
jwt:
  secret: ${JWT_SECRET:asdfghjkl...}
```
→ Secret ký/verify JWT. Gateway và User Service phải **cùng một secret**.  
→ Compose/`.env` có thể đổi qua `JWT_SECRET` mà không sửa file yaml.

---

## F. Config per-service — `config/job-user-service.yaml`

```yaml
server:
  port: 5001
```
→ Port HTTP của User Service trong container / IDE.

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/job_portal_user}
```
→ Chuỗi JDBC. Default = Postgres trên máy (`localhost`).  
→ Compose: `jdbc:postgresql://postgres:5432/job_portal_user` (`postgres` = tên service trong compose).

```yaml
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:123456789}
```
→ User/password DB, cũng override được bằng env.

```yaml
    driver-class-name: org.postgresql.Driver
```
→ Driver JDBC PostgreSQL (cố định).

---

## G. Eureka server — `application.yaml`

```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:localhost}
```
→ Hostname mà Eureka “nhìn nhận” mình.  
→ IDE: `localhost`. Compose: `eureka-server` (khớp DNS nội bộ Docker).

```yaml
  client:
    register-with-eureka: false
    fetch-registry: false
```
→ Server **không** tự đăng ký vào chính nó, **không** kéo registry (đúng vai trò registry).

---

## H. `.env.example` / `.env`

```env
POSTGRES_USER=postgres
```
→ User Postgres; Compose đọc `${POSTGRES_USER}` đưa vào container DB + service Java.

```env
POSTGRES_PASSWORD=123456789
```
→ Password DB (đổi khi deploy thật).

```env
JWT_SECRET=asdfghjkl...
```
→ Cùng secret cho User Service + Gateway trong cụm Docker.

```env
GEMINI_API_KEY=replace_with_your_google_ai_studio_key
```
→ Key gọi Google Gemini cho `job-portal-ai-service`.  
→ **Copy** thành `.env` rồi sửa; không commit `.env`.

---

## I. `docker/postgres/init/01-databases.sql`

```sql
CREATE DATABASE job_portal_user;
CREATE DATABASE job_portal_company;
-- ... các DB còn lại
```
→ Mỗi microservice một database.  
→ File trong thư mục gắn vào `/docker-entrypoint-initdb.d` của image Postgres → **chỉ chạy khi volume lần đầu trống**.

---

## J. `docker-compose.yml` — giải thích từng khối

### J.1. Đầu file

```yaml
name: job-portal
```
→ Tên project Compose (prefix container/network cho dễ nhận).

```yaml
x-java-common-env: &java-common-env
```
→ `x-...` = extension field (Compose không tạo service từ key này).  
→ `&java-common-env` = **neo (anchor)** đặt tên cho map env, để tái sử dụng.

```yaml
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
```
→ Mọi Java service trỏ Eureka bằng hostname Docker `eureka-server`.

```yaml
  SPRING_CONFIG_IMPORT: optional:configserver:http://config-server:8888
```
→ Lấy config từ Config Server trong mạng Docker.  
→ `optional:` = Config chưa lên thì vẫn cố start (ít sập cứng hơn).

```yaml
  SPRING_CLOUD_CONFIG_OVERRIDE_SYSTEM_PROPERTIES: "false"
```
→ Cho phép **env của container đè** property từ Config Server.  
→ Quan trọng: nếu không, service có thể vẫn dùng `localhost` lấy từ file config.

```yaml
  SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-postgres}
```
→ `${VAR:-default}` là cú pháp **Compose** (khác Spring `${VAR:default}`).  
→ Lấy từ `.env`, không có thì `postgres`.

```yaml
  JWT_SECRET: ${JWT_SECRET:-asdfghjkl...}
```
→ Đưa secret vào mọi service dùng chung block này (gateway + business).

### J.2. Service `postgres`

```yaml
  postgres:
    image: postgres:16
```
→ Kéo image Postgres 16 có sẵn (không Jib).

```yaml
    container_name: job-postgres
```
→ Tên cố định cho dễ `docker logs job-postgres`.

```yaml
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-123456789}
```
→ User/password khởi tạo instance Postgres.

```yaml
    ports:
      - "5432:5432"
```
→ `host:container` — máy bạn `:5432` → vào Postgres trong hộp (IDE vẫn nối DB được nếu cần).

```yaml
    volumes:
      - pgdata:/var/lib/postgresql/data
```
→ Volume đặt tên `pgdata` → data DB **sống sót** sau `compose down`.

```yaml
      - ./docker/postgres/init:/docker-entrypoint-initdb.d:ro
```
→ Mount thư mục SQL init vào chỗ Postgres tự chạy script. `:ro` = read-only.

```yaml
    networks: [job-net]
```
→ Cùng mạng với các service Java → gọi được bằng tên `postgres`.

```yaml
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
```
→ Định kỳ kiểm tra Postgres đã sẵn sàng nhận kết nối chưa.

```yaml
      interval: 5s
      timeout: 5s
      retries: 12
```
→ Mỗi 5 giây thử; quá 5 giây = fail lần đó; thử tối đa 12 lần.

### J.3. Service `eureka-server`

```yaml
    image: job-portal/eureka-server:latest
```
→ Image do Jib build ở STEP 52–53.

```yaml
    environment:
      EUREKA_INSTANCE_HOSTNAME: eureka-server
```
→ Khớp DNS nội bộ; client gọi `http://eureka-server:8761`.

```yaml
    ports:
      - "8761:8761"
```
→ Mở dashboard Eureka trên trình duyệt máy host.

### J.4. Service `config-server`

```yaml
    depends_on:
      eureka-server:
        condition: service_started
```
→ Đợi container Eureka **đã start** rồi mới start Config (chưa đảm bảo Spring “Started” xong 100%).

### J.5. Service `job-user-service` (pattern cho business)

```yaml
    environment:
      <<: *java-common-env
```
→ `<<:` = merge; `*java-common-env` = **tham chiếu anchor** ở trên → dán cả block env chung vào đây.

```yaml
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/job_portal_user
```
→ Key riêng service này: DB name `job_portal_user`, host `postgres`.

```yaml
    depends_on:
      postgres:
        condition: service_healthy
```
→ Chỉ start khi healthcheck Postgres **pass** (an toàn hơn `service_started`).

```yaml
      config-server:
        condition: service_started
```
→ Config Server container đã lên (service sẽ import config từ đó).

### J.6. Service `job-portal-ai-service`

```yaml
      GEMINI_API_KEY: ${GEMINI_API_KEY:-}
```
→ Lấy key từ `.env`; `:-` không default → để trống nếu chưa set (AI có thể fail lúc gọi Gemini).

### J.7. Service `api-gateway`

```yaml
    ports:
      - "8080:8080"
```
→ **Cổng vào duy nhất** cho client bên ngoài.

```yaml
    depends_on:
      - eureka-server
      - job-user-service
      - job-portal-job-service
```
→ Gateway lên sau vài service cốt lõi (vẫn có thể cần đợi thêm vài giây cho Spring sẵn sàng).

### J.8. Network & volume cuối file

```yaml
networks:
  job-net:
    driver: bridge
```
→ Mạng bridge nội bộ; DNS tự resolve tên service → IP container.

```yaml
volumes:
  pgdata:
```
→ Khai báo volume named dùng ở Postgres.

---

## K. `scripts/docker-build-images.ps1` — từng dòng

```powershell
$ErrorActionPreference = "Stop"
```
→ Gặp lỗi lệnh → dừng script (không chạy tiếp mù quáng).

```powershell
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
```
→ `$PSScriptRoot` = thư mục chứa script (`scripts/`).  
→ Parent = **root repo**. `cd` về root trước khi Maven.

```powershell
$Mvnw = Join-Path $Root "services\job-user-service\mvnw.cmd"
if (-not (Test-Path $Mvnw)) { throw "..." }
```
→ Đường dẫn Maven Wrapper; không thấy file thì báo lỗi sớm.

```powershell
$Modules = @( "cloud/eureka-server", ... )
```
→ Danh sách module **có Jib** (không gồm `common-lib`).

```powershell
& $Mvnw -f pom.xml -pl common-lib -am package -DskipTests
```
→ Build thư viện chung trước (các service phụ thuộc).

```powershell
foreach ($module in $Modules) {
    & $Mvnw -f pom.xml -pl $module -am package jib:dockerBuild -DskipTests
```
→ Lặp từng module: package + đóng image vào Docker local.

```powershell
    if ($LASTEXITCODE -ne 0) { throw "Jib failed for $module" }
```
→ Maven fail → dừng ngay, biết module nào hỏng.

```powershell
docker images "job-portal/*"
```
→ In danh sách image vừa build để tự kiểm tra.

---

## M. Dockerfile thường (không Jib) — xem mục riêng

Chi tiết + giải thích từng dòng nằm ở section:

**「Nếu không dùng Jib → Docker bình thường (Dockerfile)」**

File liên quan: `docker/Dockerfile.jvm`, `docker/Dockerfile.from-jar`, `docker-compose.dockerfile.yml`.

---

## L. Cú pháp hay nhầm (Spring vs Compose)

| Nơi | Viết | Ý nghĩa |
|-----|------|---------|
| `application.yaml` (Spring) | `${NAME:default}` | Env/property `NAME`, thiếu → `default` |
| `docker-compose.yml` | `${NAME:-default}` | Biến shell/`.env` `NAME`, thiếu → `default` |
| Compose merge env | `<<: *anchor` | Copy block YAML đã neo bằng `&anchor` |

Nhớ: **hai hệ thống placeholder khác nhau** — đừng copy nhầm dấu `:` / `:-`.

