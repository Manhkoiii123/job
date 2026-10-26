# CI/CD Job Portal với GitHub Actions (cho người mới)

Tài liệu này giải thích **từ đầu đến cuối**: CI/CD là gì, file trong repo làm gì, và **từng dòng** trong workflow / script.

File liên quan:

| File | Vai trò |
|------|---------|
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | **CI** — biên dịch + test trên mỗi PR/push |
| [`.github/workflows/cd-dockerhub.yml`](../.github/workflows/cd-dockerhub.yml) | **CD** — Jib build & push image lên Docker Hub |
| [`scripts/ci-jib-push.sh`](../scripts/ci-jib-push.sh) | Script bash push tất cả service (CD gọi file này) |
| [`deploy/docker-compose.vps.yml`](../deploy/docker-compose.vps.yml) | VPS kéo image từ Hub (sau CD) |
| [`docs/vps-deploy-59-68.md`](vps-deploy-59-68.md) | Deploy thủ công trên VPS + Nginx + SSL |
| [`docs/docker.md`](docker.md) | Jib / Compose local |

```text
Developer                GitHub Actions              Docker Hub              VPS
─────────                ──────────────              ──────────              ───
git push / PR  ──►  CI: mvn verify
                         (fail → không merge)

git push main  ──►  CD: jib:build  ──────────►  user/job-portal-*:tag
workflow_dispatch
  + deploy_vps ──►  SSH compose pull/up  ─────────────────────────────►  containers mới
```

---

# 1. CI / CD là gì? (lý thuyết ngắn)

| Thuật ngữ | Nghĩa | Trong project này |
|-----------|--------|-------------------|
| **CI** (Continuous Integration) | Mỗi lần đổi code → máy tự **build + test** | `ci.yml` → `mvn verify` |
| **CD** (Continuous Delivery/Deploy) | Build xong → **đóng gói & đưa lên registry / server** | `cd-dockerhub.yml` → Jib push Hub (+ tuỳ chọn SSH VPS) |
| **Pipeline / Workflow** | Chuỗi bước tự động trên GitHub | file YAML trong `.github/workflows/` |
| **Runner** | Máy ảo GitHub chạy job (`ubuntu-latest`) | không phải máy bạn, không phải VPS |
| **Secret** | Token/password ẩn (Settings → Secrets) | `DOCKERHUB_TOKEN`, `VPS_SSH_KEY`… |

**Không nhầm:**

- CI/CD chạy trên **GitHub-hosted runner** (cloud của GitHub).
- `jib:dockerBuild` trên máy bạn → Docker Desktop local.
- CD dùng `jib:build` → đẩy thẳng lên **Docker Hub**, không cần Docker Desktop trên runner (Jib tự build).

---

# 2. Chuẩn bị Secrets trên GitHub

Repo → **Settings → Secrets and variables → Actions → New repository secret**.

| Secret | Bắt buộc cho | Giá trị |
|--------|--------------|---------|
| `DOCKERHUB_USERNAME` | CD | Username Docker Hub |
| `DOCKERHUB_TOKEN` | CD | Access Token Hub (Account Settings → Security → New Access Token), **không** dùng mật khẩu login thường |
| `VPS_HOST` | Deploy tuỳ chọn | IP hoặc domain VPS, vd. `203.0.113.10` |
| `VPS_USER` | Deploy tuỳ chọn | User SSH, vd. `deploy` |
| `VPS_SSH_KEY` | Deploy tuỳ chọn | Nội dung private key (PEM) tương ứng public key đã đưa vào VPS |
| `VPS_APP_DIR` | Deploy tuỳ chọn | Đường dẫn clone repo trên VPS, vd. `/home/deploy/job-portal` |

CI (`ci.yml`) **không** cần secret Docker/VPS.

---

# 3. File `ci.yml` — giải thích từng dòng

Đường dẫn: `.github/workflows/ci.yml`

```yaml
name: CI
```

| Dòng | Ý nghĩa |
|------|---------|
| `name: CI` | Tên workflow hiện trên tab **Actions** |

```yaml
on:
  push:
    branches: [main, master, develop]
  pull_request:
    branches: [main, master, develop]
```

| Phần | Ý nghĩa |
|------|---------|
| `on:` | Sự kiện kích hoạt workflow |
| `push` + `branches` | Ai đó push lên `main` / `master` / `develop` → chạy |
| `pull_request` | Mở/cập nhật PR vào các nhánh đó → chạy (kiểm trước khi merge) |

```yaml
concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

| Phần | Ý nghĩa |
|------|---------|
| `concurrency` | Tránh chạy chồng nhiều CI cùng 1 nhánh |
| `group` | Khoá theo tên workflow + ref (nhánh/PR) |
| `cancel-in-progress: true` | Push mới → huỷ CI cũ còn chạy (tiết kiệm phút Actions) |
| `${{ ... }}` | Biểu thức GitHub Actions (lấy biến runtime) |

```yaml
jobs:
  build-and-test:
    name: Maven verify
    runs-on: ubuntu-latest
    timeout-minutes: 45
```

| Phần | Ý nghĩa |
|------|---------|
| `jobs:` | Một workflow có 1 hoặc nhiều job |
| `build-and-test` | ID nội bộ của job |
| `name:` | Label hiển thị |
| `runs-on: ubuntu-latest` | Máy ảo Ubuntu do GitHub cấp |
| `timeout-minutes: 45` | Quá 45 phút → fail (tránh treo tốn quota) |

```yaml
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
```

| Phần | Ý nghĩa |
|------|---------|
| `steps:` | Các bước tuần tự trong job |
| `uses: actions/checkout@v4` | Action chính thức: `git clone` code commit đang build vào runner |

```yaml
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
```

| Phần | Ý nghĩa |
|------|---------|
| `setup-java@v4` | Cài JDK |
| `distribution: temurin` | Eclipse Temurin (cùng “họ” image Jib `eclipse-temurin`) |
| `java-version: "17"` | Khớp `<java.version>17</java.version>` trong root `pom.xml` |
| `cache: maven` | Cache `~/.m2` giữa các lần chạy → Maven nhanh hơn |

```yaml
      - name: Make Maven Wrapper executable
        run: chmod +x services/job-user-service/mvnw
```

| Phần | Ý nghĩa |
|------|---------|
| `run:` | Chạy lệnh shell trên runner |
| `chmod +x .../mvnw` | Trên Linux, file `mvnw` phải có quyền execute (Windows không cần) |
| Vì sao path này? | Repo chưa có `./mvnw` ở root; dùng wrapper có sẵn trong `job-user-service` + `-f pom.xml` root |

```yaml
      - name: Build and test (all modules)
        run: |
          ./services/job-user-service/mvnw -B -f pom.xml \
            clean verify \
            -DfailIfNoSpecifiedTests=false
```

| Phần | Ý nghĩa |
|------|---------|
| `\|` | Block nhiều dòng lệnh |
| `./.../mvnw` | Maven Wrapper — không cần cài Maven sẵn trên runner |
| `-B` | Batch mode: log gọn, phù hợp CI |
| `-f pom.xml` | Parent POM ở **root repo** |
| `clean` | Xoá `target/` cũ |
| `verify` | Compile + test + các check phase `verify` (chặt hơn `package`) |
| `-DfailIfNoSpecifiedTests=false` | Tránh fail khi Surefire không thấy test ở một số module aggregator |

**Kết quả mong đợi:** chấm xanh trên PR. Đỏ → sửa code trước khi merge.

---

# 4. File `cd-dockerhub.yml` — giải thích từng dòng

Đường dẫn: `.github/workflows/cd-dockerhub.yml`

```yaml
name: CD Docker Hub

on:
  push:
    branches: [main, master]
    tags: ["v*"]
  workflow_dispatch:
    inputs:
      deploy_vps:
        description: "SSH pull+up trên VPS sau khi push image"
        type: boolean
        default: false
```

| Phần | Ý nghĩa |
|------|---------|
| `push` `branches` | Merge/push `main`/`master` → build & push image |
| `tags: ["v*"]` | Tag kiểu `v1.0.0` → push image với tag đó |
| `workflow_dispatch` | Nút **Run workflow** trên UI Actions (chạy tay) |
| `inputs.deploy_vps` | Tick khi muốn SSH deploy VPS sau khi push Hub |

```yaml
env:
  IMAGE_PREFIX: job-portal-
```

Biến môi trường cấp workflow (tham chiếu naming; script dùng prefix tương tự).

```yaml
jobs:
  push-images:
    ...
    permissions:
      contents: read
```

Job chỉ cần đọc code (không ghi release). Nguyên tắc **least privilege**.

### Bước resolve tag

```yaml
      - name: Resolve image tags
        id: tags
        run: |
          SHORT_SHA="${GITHUB_SHA::7}"
          if [[ "${GITHUB_REF}" == refs/tags/* ]]; then
            TAG="${GITHUB_REF_NAME}"
          else
            TAG="latest"
          fi
          echo "short_sha=${SHORT_SHA}" >> "$GITHUB_OUTPUT"
          echo "tag=${TAG}" >> "$GITHUB_OUTPUT"
```

| Phần | Ý nghĩa |
|------|---------|
| `id: tags` | Các step sau lấy `${{ steps.tags.outputs.tag }}` |
| `GITHUB_SHA` | Hash commit full; `::7` = 7 ký tự đầu |
| `GITHUB_REF` | `refs/heads/main` hoặc `refs/tags/v1.0.0` |
| Tag từ git tag | Image tag = tên tag (`v1.0.0`) |
| Push nhánh | Image tag = `latest` |
| `$GITHUB_OUTPUT` | Cách GitHub Actions truyền output giữa các step |

Mỗi image nhận **2 tag**: `latest` (hoặc `v*`) **và** short SHA — dễ rollback: `docker pull user/job-portal-eureka-server:a1b2c3d`.

### Login Docker Hub

```yaml
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
```

| Phần | Ý nghĩa |
|------|---------|
| `docker/login-action` | `docker login` an toàn trên runner |
| `secrets.*` | Không in ra log (GitHub mask) |

Jib cũng nhận username/password qua `-Djib.to.auth.*` trong script (đủ để `jib:build` mà không phụ thuộc Docker daemon).

### Build common-lib rồi gọi script

```yaml
      - name: Build common-lib
        run: |
          ./services/job-user-service/mvnw -B -f pom.xml \
            -pl common-lib -am package -DskipTests

      - name: Jib push all services
        env:
          DOCKERHUB_USERNAME: ${{ secrets.DOCKERHUB_USERNAME }}
          DOCKERHUB_TOKEN: ${{ secrets.DOCKERHUB_TOKEN }}
          IMAGE_TAG: ${{ steps.tags.outputs.tag }}
          SHORT_SHA: ${{ steps.tags.outputs.short_sha }}
        run: bash scripts/ci-jib-push.sh
```

| Phần | Ý nghĩa |
|------|---------|
| `-pl common-lib -am` | Đóng jar thư viện dùng chung trước |
| `env:` | Truyền secret/tag vào process của script |
| `bash scripts/ci-jib-push.sh` | Loop từng microservice → `jib:build` |

### Job deploy VPS (tuỳ chọn)

```yaml
  deploy-vps:
    needs: push-images
    if: github.event_name == 'workflow_dispatch' && inputs.deploy_vps == true
```

| Phần | Ý nghĩa |
|------|---------|
| `needs: push-images` | Chỉ deploy sau khi push image thành công |
| `if: ...` | Chỉ khi bấm Run workflow + tick `deploy_vps` |
| Vì không auto mọi push? | An toàn hơn khi học; tránh deploy nhầm. Muốn auto: đổi `if` theo `vars.ENABLE_VPS_DEPLOY` |

```yaml
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            cd "${{ secrets.VPS_APP_DIR }}"
            export DOCKERHUB_USER="${{ secrets.DOCKERHUB_USERNAME }}"
            docker compose -f deploy/docker-compose.vps.yml --env-file deploy/.env pull
            docker compose -f deploy/docker-compose.vps.yml --env-file deploy/.env up -d
            docker image prune -f
```

| Phần | Ý nghĩa |
|------|---------|
| `appleboy/ssh-action` | SSH từ runner → VPS, chạy lệnh remote |
| `cd ...` | Vào thư mục đã `git clone` repo trên VPS |
| `DOCKERHUB_USER` | Khớp biến trong `docker-compose.vps.yml` (`${DOCKERHUB_USER}/job-portal-...`) |
| `compose ... pull` | Kéo image mới từ Hub |
| `compose ... up -d` | Recreate container với image mới (detached) |
| `image prune -f` | Xoá image cũ không dùng (giữ disk VPS) |

Trên VPS phải có sẵn: Docker, file `deploy/.env` (secret DB/JWT — **không** commit lên Git).

---

# 5. Script `ci-jib-push.sh` — giải thích từng khối

Đường dẫn: `scripts/ci-jib-push.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
```

| Phần | Ý nghĩa |
|------|---------|
| `#!/usr/bin/env bash` | Shebang — chạy bằng bash |
| `set -e` | Lệnh fail → dừng script |
| `set -u` | Dùng biến chưa gán → lỗi |
| `set -o pipefail` | Pipe fail nếu bất kỳ lệnh nào fail |

```bash
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
```

| Phần | Ý nghĩa |
|------|---------|
| `BASH_SOURCE[0]` | Đường dẫn file script đang chạy |
| `dirname` + `/..` | Lên **root repo** (script nằm trong `scripts/`) |
| `cd "$ROOT"` | Mọi lệnh Maven chạy từ root |

```bash
: "${DOCKERHUB_USERNAME:?Set DOCKERHUB_USERNAME}"
: "${DOCKERHUB_TOKEN:?Set DOCKERHUB_TOKEN}"
```

| Phần | Ý nghĩa |
|------|---------|
| `: "${VAR:?msg}"` | Nếu `VAR` trống → in msg và exit (bắt buộc secret) |

```bash
MODULES=(
  "cloud/eureka-server|eureka-server"
  ...
  "services/job-portal-preferences|job-portal-preferences-service"
  ...
)
```

| Phần | Ý nghĩa |
|------|---------|
| `module\|name` | Trái = path Maven `-pl`; phải = tên image trên Hub |
| Preferences | Folder `job-portal-preferences` nhưng **artifactId** / Hub name = `job-portal-preferences-service` |

Tên Hub cuối cùng:

```text
${DOCKERHUB_USERNAME}/job-portal-${name}:tag
ví dụ: myuser/job-portal-eureka-server:latest
       myuser/job-portal-job-portal-company-service:latest
```

(Khớp `scripts/push-images-dockerhub.ps1` và `deploy/docker-compose.vps.yml`.)

```bash
  "$MVNW" -B -f pom.xml \
    -pl "$module" -am package jib:build \
    -DskipTests \
    "-Djib.to.image=${image}" \
    "-Djib.to.tags=${TAGS}" \
    "-Djib.to.auth.username=${DOCKERHUB_USERNAME}" \
    "-Djib.to.auth.password=${DOCKERHUB_TOKEN}"
```

| Flag / goal | Ý nghĩa |
|-------------|---------|
| `-pl "$module"` | Chỉ module đó |
| `-am` | Also make: build dependency (common-lib, parent…) |
| `package` | Tạo jar trước khi đóng image |
| `jib:build` | Build image **và push registry** (không cần `docker` daemon) |
| `-DskipTests` | CD giả định CI đã test; đẩy image nhanh hơn |
| `-Djib.to.image=` | Ghi đè `<to><image>` trong pom (`job-portal/...` local) → tên Hub |
| `-Djib.to.tags=` | `latest,a1b2c3d` hoặc `v1.0.0,a1b2c3d` |
| `-Djib.to.auth.*` | Đăng nhập Hub cho Jib |

**Khác `jib:dockerBuild`:** `dockerBuild` chỉ đưa vào Docker local; `jib:build` dùng cho CI/CD registry.

---

# 6. Quy trình làm việc hàng ngày

### A. Mỗi Pull Request

1. Push branch → mở PR vào `main`.  
2. Workflow **CI** tự chạy.  
3. Xanh → review/merge. Đỏ → xem log Actions, sửa.

### B. Sau khi merge `main`

1. Workflow **CD Docker Hub** chạy.  
2. Image mới xuất hiện trên Hub: `docker.io/<user>/job-portal-*:latest`.  
3. Trên VPS (thủ công, xem [vps-deploy-59-68.md](vps-deploy-59-68.md)):

```bash
cd /path/to/repo
docker compose -f deploy/docker-compose.vps.yml --env-file deploy/.env pull
docker compose -f deploy/docker-compose.vps.yml --env-file deploy/.env up -d
```

### C. Deploy VPS bằng nút Actions

1. Tab **Actions** → **CD Docker Hub** → **Run workflow**.  
2. Tick **deploy_vps**.  
3. Đủ secret `VPS_*` → runner SSH và `compose pull/up`.

### D. Release có version

```bash
git tag v1.0.0
git push origin v1.0.0
```

CD gắn image tag `v1.0.0` (+ short SHA).

---

# 7. Chạy script push local (không qua Actions)

Git Bash / WSL từ root repo:

```bash
export DOCKERHUB_USERNAME=youruser
export DOCKERHUB_TOKEN=dckr_pat_xxx
export IMAGE_TAG=latest
export SHORT_SHA=$(git rev-parse --short HEAD)
bash scripts/ci-jib-push.sh
```

| Dòng | Ý nghĩa |
|------|---------|
| `export ...` | Biến môi trường cho process con |
| `git rev-parse --short HEAD` | Lấy short commit hash |
| `bash scripts/...` | Chạy cùng script CD dùng |

PowerShell tương đương: dùng `scripts/push-images-dockerhub.ps1` **sau** khi đã `jib:dockerBuild` local (khác đường: local Docker → tag → push).

---

# 8. Checklist bật CI/CD lần đầu

- [ ] Code đã trên GitHub (remote `origin`)  
- [ ] File `.github/workflows/ci.yml` và `cd-dockerhub.yml` nằm trên default branch  
- [ ] Secret `DOCKERHUB_USERNAME` + `DOCKERHUB_TOKEN`  
- [ ] Docker Hub tạo sẵn (hoặc để Jib tạo) repo/image namespace dưới user  
- [ ] Mở PR thử → thấy workflow **CI**  
- [ ] Merge `main` → thấy **CD Docker Hub** SUCCESS + image trên Hub  
- [ ] (Tuỳ chọn) Secret `VPS_*` + `deploy/.env` trên server → Run workflow + tick deploy  

---

# 9. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử |
|-------------|-------------|---------|
| CI đỏ `Permission denied: mvnw` | Quên `chmod +x` | Đã có step chmod; kiểm tra file không bị corrupt |
| CD đỏ `unauthorized: incorrect username/password` | Sai token Hub hoặc hết hạn | Tạo Access Token mới, update secret |
| CD đỏ Jib `401` khi pull base image | Rate limit Docker Hub base | Root pom đã dùng ECR Public `jib.from.image` — giữ nguyên |
| Compose VPS không thấy image mới | Sai `DOCKERHUB_USER` hoặc tên image | Đối chiếu `ci-jib-push.sh` ↔ `docker-compose.vps.yml` |
| Deploy SSH fail | Key/user/host/`VPS_APP_DIR` sai | SSH tay từ máy bạn thử cùng user/key |
| Job deploy không chạy sau push | Đúng thiết kế — chỉ `workflow_dispatch` + tick | Bấm Run workflow |

---

# 10. Bản đồ tên image (đối chiếu nhanh)

| Module Maven (`-pl`) | Image trên Hub |
|----------------------|----------------|
| `cloud/eureka-server` | `<user>/job-portal-eureka-server` |
| `cloud/config-server` | `<user>/job-portal-config-server` |
| `cloud/api-gateway` | `<user>/job-portal-api-gateway` |
| `services/job-user-service` | `<user>/job-portal-job-user-service` |
| `services/job-portal-company-service` | `<user>/job-portal-job-portal-company-service` |
| `services/job-portal-job-service` | `<user>/job-portal-job-portal-job-service` |
| `services/job-portal-resume-service` | `<user>/job-portal-job-portal-resume-service` |
| `services/job-portal-application-service` | `<user>/job-portal-job-portal-application-service` |
| `services/job-portal-preferences` | `<user>/job-portal-job-portal-preferences-service` |
| `services/job-portal-ai-service` | `<user>/job-portal-job-portal-ai-service` |
| `services/job-portal-notification-service` | `<user>/job-portal-job-portal-notification-service` |

> Tên có `job-portal-job-portal-...` vì script gắn prefix `job-portal-` lên **artifactId** đã có sẵn `job-portal-`. Giữ nguyên để khớp compose VPS hiện tại; đổi sau thì sửa **cùng lúc** script + compose + doc này.

---

# 11. Tóm tắt một câu

**CI** đảm bảo code compile/test xanh; **CD** đóng image bằng Jib và đẩy Docker Hub; VPS chỉ `pull` + `compose up` (tay hoặc SSH từ Actions).
