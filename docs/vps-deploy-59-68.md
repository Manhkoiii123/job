# Deploy Job Portal lên VPS — Steps 59 → 68 (cho người mới)

Tài liệu đi theo series video:

| # | Video | Việc làm |
|---|-------|----------|
| **59** | Preparing VPS for Deployment | Thuê VPS, SSH, firewall, user |
| **60** | Installing Docker, Compose, Git on VPS | Cài tool chạy container |
| **61** | Pushing Docker Image to Docker Hub | Build local → đẩy registry |
| **62** | Pushing Docker Compose to GitHub | Lưu cấu hình deploy trên Git |
| **63** | Deploying Application on VPS | Pull image + `compose up` trên server |
| **64** | Pointing Domain to VPS | DNS A record → IP VPS |
| **65** | What is Nginx | Lý thuyết reverse proxy |
| **66** | Installing and Configuring Nginx | Nginx trỏ domain → Gateway `:8080` |
| **67** | What are SSL Certificates | HTTPS là gì |
| **68** | Let's Encrypt SSL | Certbot cấp chứng chỉ miễn phí |

File mẫu trong repo:

| File | Vai trò |
|------|---------|
| `scripts/push-images-dockerhub.ps1` | Tag + push image lên Docker Hub |
| `deploy/docker-compose.vps.yml` | Compose cho VPS (image từ Hub) |
| `deploy/.env.production.example` | Secret production |
| `deploy/nginx/job-portal.conf` | Nginx reverse proxy |
| [`docs/cicd-github-actions.md`](cicd-github-actions.md) | CI/CD: auto test + Jib push Hub (+ SSH deploy tuỳ chọn) |
| `.github/workflows/ci.yml` | CI — `mvn verify` |
| `.github/workflows/cd-dockerhub.yml` | CD — push image Docker Hub |

```text
Máy dev                          Docker Hub                    VPS
────────                         ──────────                    ───
mvn/jib build image
     │
     ▼
docker push youruser/job-portal-*:latest ──► registry
                                                   │
GitHub: deploy/*.yml, nginx, .env.example          │ docker pull
                                                   ▼
                                            docker compose up -d
                                                   │
Internet ──► Domain ──► Nginx :443 ──► Gateway 127.0.0.1:8080
```

---

# STEP 59 — Chuẩn bị VPS

## 59.A. Lý thuyết

**VPS** = máy ảo trên cloud (DigitalOcean, AWS Lightsail, Vultr, Contabo…).  
Bạn có IP public, SSH vào như máy Linux thật.

Cần tối thiểu (khuyến nghị học):

- Ubuntu 22.04 LTS  
- 2–4 GB RAM (stack microservices + Kafka nặng; 4GB+ tốt hơn)  
- 2 vCPU  
- 40GB disk  

## 59.B. Các bước làm

1. Tạo VPS, lưu **IP** (ví dụ `203.0.113.10`).  
2. SSH lần đầu (user `root` hoặc user cloud cung cấp):

```bash
ssh root@203.0.113.10
```

| Phần | Ý nghĩa |
|------|---------|
| `ssh` | Secure Shell — đăng nhập từ xa |
| `root@` | User trên server |
| `203.0.113.10` | IP public VPS |

3. Cập nhật hệ thống:

```bash
apt update && apt upgrade -y
```

| Lệnh | Ý nghĩa |
|------|---------|
| `apt update` | Làm mới danh sách package |
| `apt upgrade -y` | Cài bản mới; `-y` = không hỏi Yes |

4. Tạo user deploy (không làm việc hàng ngày bằng root):

```bash
adduser deploy
usermod -aG sudo deploy
```

| Lệnh | Ý nghĩa |
|------|---------|
| `adduser deploy` | Tạo user mới |
| `usermod -aG sudo deploy` | Cho phép dùng `sudo` |

5. Firewall (UFW) — chỉ mở SSH + HTTP/HTTPS:

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status
```

| Dòng | Ý nghĩa |
|------|---------|
| `allow OpenSSH` | Giữ cổng 22 để không tự khóa mình |
| `80/tcp` | HTTP (certbot + redirect) |
| `443/tcp` | HTTPS |
| `ufw enable` | Bật firewall |

**Không** mở public `5432` (Postgres), `8761` (Eureka), `9092` (Kafka) ra internet.

## 59.C. Checkpoint 59

- [ ] SSH vào được VPS  
- [ ] Có user `deploy` + sudo  
- [ ] UFW: 22, 80, 443  

---

# STEP 60 — Cài Docker, Docker Compose, Git trên VPS

## 60.A. Lý thuyết

| Tool | Việc |
|------|------|
| **Docker** | Chạy container từ image |
| **Compose** | Bật cả cụm bằng 1 file YAML |
| **Git** | Clone repo chứa `docker-compose.vps.yml`, nginx, init SQL |

## 60.B. Cài Git

```bash
sudo apt install -y git
git --version
```

## 60.C. Cài Docker Engine (Ubuntu)

Theo docs Docker (rút gọn):

```bash
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
```

| Phần | Ý nghĩa |
|------|---------|
| `ca-certificates` | Tin chứng chỉ HTTPS khi tải package |
| `curl` | Tải file từ URL |
| `gpg --dearmor` | Lưu key kho Docker dạng binary |

Thêm repo Docker rồi cài:

```bash
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

| Package | Ý nghĩa |
|---------|---------|
| `docker-ce` | Docker Engine |
| `docker-compose-plugin` | Lệnh `docker compose` (plugin v2) |

Cho user `deploy` chạy Docker không cần root:

```bash
sudo usermod -aG docker deploy
```

Đăng xuất/SSH lại rồi kiểm tra:

```bash
docker version
docker compose version
docker run --rm hello-world
```

## 60.D. Checkpoint 60

- [ ] `docker version` có Server  
- [ ] `docker compose version` OK  
- [ ] `git --version` OK  

---

# STEP 61 — Push Docker image lên Docker Hub

## 61.A. Lý thuyết

**Docker Hub** = registry công khai (giống “GitHub cho image”).  
VPS **không** cần build Java nặng; chỉ `docker pull` image bạn đã push.

```text
Local: job-portal/api-gateway:latest
   │  docker tag
   ▼
Docker Hub: youruser/job-portal-api-gateway:latest
   │  docker pull (trên VPS)
   ▼
VPS chạy container
```

## 61.B. Trên máy dev (Windows)

1. Đăng ký https://hub.docker.com → nhớ **username**.  
2. Login:

```powershell
docker login
```

| Prompt | Ý nghĩa |
|--------|---------|
| Username | Docker Hub user |
| Password | Dùng **Access Token** (khuyến nghị) thay mật khẩu account |

3. Build image local (đã có sẵn script):

```powershell
.\scripts\docker-build-images.ps1
# hoặc Dockerfile:
.\scripts\docker-build-images-dockerfile.ps1
```

4. Tag + push tất cả:

```powershell
$env:DOCKERHUB_USER = "youruser"
.\scripts\push-images-dockerhub.ps1
```

### Giải thích script `push-images-dockerhub.ps1` từng dòng

```powershell
$ErrorActionPreference = "Stop"
```
→ Gặp lỗi thì dừng hẳn (không push dở).

```powershell
if (-not $env:DOCKERHUB_USER) { throw "..." }
```
→ Bắt buộc có biến môi trường tên Hub user.

```powershell
$local = "job-portal/${name}:latest"
$remote = "${User}/job-portal-${name}:latest"
```
→ `local` = tên image Jib tạo.  
→ `remote` = quy ước Hub: `user/job-portal-eureka-server:latest`.

```powershell
docker tag $local $remote
```
→ Gắn thêm tên (alias) cho cùng một image ID — **không** copy file lớn lại.

```powershell
docker push $remote
```
→ Upload layer lên Docker Hub.

## 61.C. Kiểm tra

Vào Hub web → thấy repo `job-portal-api-gateway`, …  
Hoặc:

```powershell
docker pull youruser/job-portal-api-gateway:latest
```

## 61.D. Checkpoint 61

- [ ] `docker login` OK  
- [ ] Push được ít nhất Gateway + Eureka  
- [ ] Hub hiện tag `latest`  

---

# STEP 62 — Đưa Docker Compose (và deploy config) lên GitHub

## 62.A. Lý thuyết

VPS cần file:

- `deploy/docker-compose.vps.yml` — khai báo service + image Hub  
- `docker/postgres/init/*.sql` — tạo DB lần đầu  
- `deploy/nginx/job-portal.conf` — cấu hình Nginx  
- **Không** commit `.env` thật (secret)

GitHub = nơi VPS `git clone` / `git pull` cấu hình.

## 62.B. Các bước

1. Repo đã có các file trên (trong project này).  
2. Commit & push (máy dev):

```bash
git add deploy/ scripts/push-images-dockerhub.ps1 docker/postgres/init
git status
git commit -m "Add VPS deploy compose, nginx, Docker Hub push script"
git push origin main
```

| Lệnh | Ý nghĩa |
|------|---------|
| `git add ...` | Stage file |
| `git commit -m "..."` | Lưu snapshot + message |
| `git push origin main` | Đẩy lên GitHub nhánh `main` |

3. File secret chỉ dùng mẫu:

```bash
# Có trong repo
deploy/.env.production.example

# Trên VPS sẽ copy thành .env (không push Git)
```

### Giải thích `.env.production.example` từng dòng

```env
POSTGRES_USER=postgres
```
→ User Postgres trong container.

```env
POSTGRES_PASSWORD=change_me_strong_password
```
→ Đổi mật khẩu mạnh trên VPS.

```env
JWT_SECRET=change_me_long_random_secret_at_least_32_chars
```
→ Secret ký JWT — User Service + Gateway phải cùng giá trị.

```env
GEMINI_API_KEY=your_gemini_key
```
→ Key AI service (nếu dùng).

## 62.C. Checkpoint 62

- [ ] GitHub có thư mục `deploy/`  
- [ ] Không có `.env` production trên Git  

---

# STEP 63 — Deploy ứng dụng trên VPS

## 63.A. Lý thuyết

Trên VPS:

1. Clone repo (lấy compose + init SQL)  
2. Tạo `.env`  
3. `docker compose pull` (kéo image Hub)  
4. `docker compose up -d`  

Gateway chỉ bind `127.0.0.1:8080` → **chỉ Nginx trên máy** gọi được, internet không đụng thẳng Gateway (an toàn hơn).

## 63.B. Các bước trên VPS

```bash
sudo mkdir -p /opt/job-portal
sudo chown deploy:deploy /opt/job-portal
cd /opt/job-portal

git clone https://github.com/YOUR_USER/YOUR_REPO.git .
# hoặc clone rồi cp deploy + docker/postgres
```

| Lệnh | Ý nghĩa |
|------|---------|
| `/opt/job-portal` | Thư mục chuẩn đặt app trên Linux |
| `chown deploy:deploy` | User deploy sở hữu thư mục |
| `git clone ... .` | Clone vào thư mục hiện tại |

Chuẩn bị env + biến Hub user:

```bash
cd /opt/job-portal
cp deploy/.env.production.example .env
nano .env   # sửa password, JWT, GEMINI, thêm dòng:
```

Thêm vào `.env`:

```env
DOCKERHUB_USER=youruser
```

Chạy stack (từ root repo để path `./docker/postgres/init` đúng — hoặc chỉnh volume):

```bash
# Đặt compose dùng init SQL: copy hoặc symlink
# Cách đơn giản: chạy từ root repo đã clone

export DOCKERHUB_USER=youruser
docker compose -f deploy/docker-compose.vps.yml --env-file .env pull
docker compose -f deploy/docker-compose.vps.yml --env-file .env up -d
docker compose -f deploy/docker-compose.vps.yml ps
```

> Volume init trong `docker-compose.vps.yml` là `../docker/postgres/init` (tính từ thư mục `deploy/`). Chạy lệnh `compose` từ **root repo**.

### Giải thích vài dòng quan trọng trong `deploy/docker-compose.vps.yml`

```yaml
image: ${DOCKERHUB_USER}/job-portal-api-gateway:latest
```
→ Image lấy từ Hub, không build trên VPS.

```yaml
ports:
  - "127.0.0.1:8080:8080"
```
→ `127.0.0.1:` = chỉ lắng nghe localhost máy VPS.  
→ Internet **không** vào trực tiếp `:8080`; phải qua Nginx.

```yaml
restart: unless-stopped
```
→ Container tự restart khi máy reboot / process chết (trừ khi bạn `stop` tay).

```yaml
SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
```
→ Lấy từ file `.env` — không hardcode trong YAML.

## 63.C. Kiểm tra

```bash
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/jobs
docker compose -f deploy/docker-compose.vps.yml logs -f api-gateway
```

## 63.D. Checkpoint 63

- [ ] `compose ps` các service Up  
- [ ] `curl 127.0.0.1:8080/api/jobs` có response  
- [ ] Postgres/Kafka **không** publish port ra `0.0.0.0`  

---

# STEP 64 — Trỏ domain về VPS

## 64.A. Lý thuyết

**DNS A record** = tên miền → IPv4.

```text
api.yourdomain.com  →  A  →  203.0.113.10
```

TTL thấp (300s) lúc mới cấu hình để đổi nhanh.

## 64.B. Các bước

1. Mua/thuê domain (Namecheap, Cloudflare, GoDaddy…).  
2. Trong DNS panel, tạo:

| Type | Name | Value |
|------|------|--------|
| A | `api` | IP VPS |
| A | `@` (optional) | IP VPS nếu muốn root domain |

3. Đợi propagate (vài phút–vài giờ). Kiểm tra:

```bash
# trên máy bất kỳ
nslookup api.yourdomain.com
ping api.yourdomain.com
```

| Lệnh | Ý nghĩa |
|------|---------|
| `nslookup` | Hỏi DNS tên → IP |
| IP trùng VPS | DNS đã đúng |

## 64.C. Checkpoint 64

- [ ] `nslookup` ra đúng IP VPS  
- [ ] Chưa cần HTTPS — STEP 66–68  

---

# STEP 65 — Nginx là gì? (lý thuyết)

## 65.A. Vai trò trong kiến trúc này

**Nginx** đứng trước:

```text
Client → :443 Nginx → reverse proxy → 127.0.0.1:8080 API Gateway → microservices
```

Nginx làm:

1. **Reverse proxy** — nhận HTTP(S), chuyển tiếp vào app  
2. **TLS termination** — HTTPS kết thúc ở Nginx; nội bộ HTTP localhost  
3. (Tuỳ chọn) load balance, cache, rate limit, static files  

**Không** thay Eureka/Gateway — chỉ là “cửa chính” của server.

## 65.B. Thuật ngữ

| Từ | Nghĩa |
|----|--------|
| `server_name` | Domain Nginx nhận |
| `listen 80` | Cổng HTTP |
| `location /` | Rule đường dẫn |
| `proxy_pass` | Chuyển request tới upstream |

---

# STEP 66 — Cài và cấu hình Nginx trên VPS

## 66.A. Cài đặt

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
sudo systemctl status nginx
```

| Lệnh | Ý nghĩa |
|------|---------|
| `enable` | Bật cùng lúc máy boot |
| `start` | Chạy ngay |
| `status` | Xem active/inactive |

## 66.B. Đặt config site

```bash
sudo cp /opt/job-portal/deploy/nginx/job-portal.conf /etc/nginx/sites-available/job-portal
sudo nano /etc/nginx/sites-available/job-portal
# sửa server_name api.yourdomain.com
```

Bật site, tắt default nếu cần:

```bash
sudo ln -s /etc/nginx/sites-available/job-portal /etc/nginx/sites-enabled/job-portal
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

| Lệnh | Ý nghĩa |
|------|---------|
| `ln -s ... sites-enabled` | Enable site bằng symlink |
| `nginx -t` | Test cú pháp config (bắt buộc trước reload) |
| `reload` | Nạp config mới, không cắt hết kết nối đang có |

## 66.C. Giải thích từng dòng `deploy/nginx/job-portal.conf`

```nginx
server {
```
→ Một khối “ảo host” xử lý request khớp `server_name`.

```nginx
    listen 80;
    listen [::]:80;
```
→ Lắng nghe HTTP IPv4 và IPv6 cổng 80.

```nginx
    server_name api.yourdomain.com;
```
→ Chỉ nhận request có Host header đúng domain này.

```nginx
    location / {
```
→ Mọi path (`/api/...`, `/auth/...`) đều vào đây.

```nginx
        proxy_pass http://127.0.0.1:8080;
```
→ Chuyển tiếp tới API Gateway (Compose bind localhost:8080).

```nginx
        proxy_http_version 1.1;
```
→ Dùng HTTP/1.1 với upstream (giữ connection tốt hơn).

```nginx
        proxy_set_header Host $host;
```
→ Giữ Host gốc (`api.yourdomain.com`) cho app phía sau.

```nginx
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```
→ Truyền IP client thật (Gateway/log cần biết ai gọi).

```nginx
        proxy_set_header X-Forwarded-Proto $scheme;
```
→ Báo app request originally `http` hay `https` (sau STEP 68 rất quan trọng).

```nginx
        proxy_set_header Connection "";
```
→ Tránh giữ Connection header lạ khi proxy.

```nginx
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
```
→ Timeout nối/gửi/đọc upstream (API chậm không cắt quá sớm).

```nginx
        client_max_body_size 20m;
```
→ Cho upload tối đa 20MB (resume/file nếu có).

```nginx
    }
}
```
→ Đóng `location` và `server`.

## 66.D. Kiểm tra HTTP

```bash
curl -I http://api.yourdomain.com/api/jobs
```

## 66.E. Checkpoint 66

- [ ] `nginx -t` OK  
- [ ] Domain HTTP vào được Gateway  

---

# STEP 67 — SSL Certificate là gì? (lý thuyết)

## 67.A. HTTP vs HTTPS

| | HTTP | HTTPS |
|---|------|-------|
| Port | 80 | 443 |
| Mã hóa | Không | TLS mã hóa |
| Tin cậy | Dễ bị nghe lén / giả mạo | Trình duyệt hiện ổ khóa |

**SSL/TLS certificate** = file chứng nhận “domain này thuộc về server đang trả HTTPS”, do **CA** (Certificate Authority) ký.

## 67.B. Let's Encrypt

- CA miễn phí, tự động  
- Certbot xin cert + (thường) cấu hình Nginx  
- Cert hết hạn ~90 ngày → renew tự động bằng timer  

---

# STEP 68 — Cài Let's Encrypt cho domain

## 68.A. Cài Certbot

```bash
sudo apt install -y certbot python3-certbot-nginx
```

| Package | Ý nghĩa |
|---------|---------|
| `certbot` | Client Let's Encrypt |
| `python3-certbot-nginx` | Plugin sửa config Nginx giúp bạn |

## 68.B. Xin chứng chỉ

**DNS đã trỏ đúng** + Nginx `server_name` đúng domain:

```bash
sudo certbot --nginx -d api.yourdomain.com
```

| Phần | Ý nghĩa |
|------|---------|
| `--nginx` | Dùng plugin Nginx |
| `-d api.yourdomain.com` | Domain xin cert |

Làm theo prompt: email, agree ToS, chọn redirect HTTP→HTTPS (**2** khuyến nghị).

Certbot sẽ:

1. Challenge chứng minh bạn sở hữu domain (HTTP-01)  
2. Ghi cert vào `/etc/letsencrypt/live/...`  
3. Sửa Nginx listen 443 + `ssl_certificate`  
4. Thêm redirect 80 → 443  

## 68.C. Kiểm tra HTTPS

```bash
curl -I https://api.yourdomain.com/api/jobs
sudo certbot renew --dry-run
```

| Lệnh | Ý nghĩa |
|------|---------|
| `curl -I https://...` | Thử HTTPS |
| `renew --dry-run` | Thử gia hạn giả lập (đảm bảo timer OK) |

## 68.D. Checkpoint 68

- [ ] Trình duyệt mở `https://api.yourdomain.com` có ổ khóa  
- [ ] `dry-run` renew thành công  

---

# Quy trình vận hành sau khi lên production

## Deploy bản mới

```bash
# Máy dev
.\scripts\docker-build-images.ps1
$env:DOCKERHUB_USER="youruser"
.\scripts\push-images-dockerhub.ps1

# VPS
cd /opt/job-portal
git pull
docker compose -f deploy/docker-compose.vps.yml --env-file .env pull
docker compose -f deploy/docker-compose.vps.yml --env-file .env up -d
```

## Xem log

```bash
docker compose -f deploy/docker-compose.vps.yml logs -f api-gateway
docker compose -f deploy/docker-compose.vps.yml logs -f job-portal-notification-service
```

## Smoke test production

| URL | Kỳ vọng |
|-----|---------|
| `https://api.yourdomain.com/api/jobs` | 200 |
| `https://api.yourdomain.com/auth/login` | JWT |
| Mail / Kafka | Giống local (MailHog nội bộ; production nên SMTP thật) |

> Production thật: thay MailHog bằng SMTP (SendGrid/SES) — đổi `MAIL_HOST`/`MAIL_PORT`/auth trong `.env`.

---

# Checklist 59 → 68

```text
[ ] 59  VPS Ubuntu, SSH, user deploy, UFW 22/80/443
[ ] 60  Docker + compose plugin + Git
[ ] 61  docker login + push-images-dockerhub.ps1
[ ] 62  GitHub có deploy/ + không commit .env
[ ] 63  VPS: clone, .env, compose pull && up -d
[ ] 64  DNS A record → IP VPS
[ ] 65  Hiểu Nginx = reverse proxy + TLS termination
[ ] 66  Cài Nginx + job-portal.conf + nginx -t
[ ] 67  Hiểu HTTPS / certificate / CA
[ ] 68  certbot --nginx -d api.yourdomain.com + renew dry-run
```

---

# Lỗi hay gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|------------|-------------|------------|
| SSH timeout sau `ufw enable` | Quên allow OpenSSH | Console provider mở port 22 / disable ufw tạm |
| `docker pull` denied | Image private / sai user | `docker login` trên VPS hoặc repo public |
| Compose không tạo DB | Volume cũ / sai path init | Kiểm tra `./docker/postgres/init`; `down -v` (mất data) |
| Domain không vào | DNS chưa propagate / sai IP | `nslookup` |
| 502 Bad Gateway | Gateway chưa up / sai proxy_pass | `curl 127.0.0.1:8080`; xem `compose ps` |
| Certbot fail | DNS chưa trỏ / port 80 bị chặn | Fix DNS + UFW 80 |
| JWT fail production | `.env` JWT khác lúc signup | Đồng bộ `JWT_SECRET` mọi service |

---

# Nhớ 1 câu / step

| Step | 1 câu |
|------|-------|
| 59 | VPS = máy Linux có IP public |
| 60 | Docker + Git là dụng cụ chạy & lấy config |
| 61 | Hub giữ image; VPS chỉ pull |
| 62 | GitHub giữ compose/nginx; không giữ secret |
| 63 | `compose up` trên VPS = bật cả hệ microservices |
| 64 | DNS A = tên miền trỏ IP |
| 65 | Nginx = cửa chính reverse proxy |
| 66 | `proxy_pass` → Gateway localhost |
| 67 | SSL = mã hóa + chứng thực domain |
| 68 | Let's Encrypt + Certbot = HTTPS miễn phí |

**Chốt:** Dev build & push image → VPS pull & compose → Nginx + Let's Encrypt đưa domain HTTPS vào API Gateway.
