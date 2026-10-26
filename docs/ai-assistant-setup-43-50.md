# AI Assistant Microservice — Steps 43 → 50 (cho người mới)

Tài liệu này giải thích **từng bước** xây AI cho Job Portal: từ “AI là gì trong hệ thống này” → cấu hình Gemini → prompt → từng tính năng → test.

Code nằm ở module:

```text
services/job-portal-ai-service/
```

Đăng ký Eureka tên: **`job-portal-ai-service`** · Port: **`5007`** · Gateway path: **`/api/ai/**`**

---

## Phần 0 — Hình dung trước khi code

### 0.1. AI Service làm gì?

Các service cũ (Job, Resume, Application…) vẫn lo **dữ liệu và nghiệp vụ**.  
**AI Service** lo phần “thông minh”:

| Việc | Ai làm |
|------|--------|
| Lưu CV, tin tuyển dụng, đơn ứng tuyển | Domain services (đã có) |
| Phân tích CV, viết JD, chấm match, tìm theo nghĩa | **AI Service** (mới) |
| Model ngôn ngữ thật (Gemini) | Google cloud / API bên ngoài |

```text
Postman / App
      │  JWT
      ▼
 API Gateway :8080
      │  /api/ai/**
      ▼
 AI Service :5007  ──prompt──►  Google Gemini
      ▲
 Domain services có thể gọi AI (Feign) sau này
```

### 0.2. Vì sao tách riêng microservice?

1. API key Gemini **không rải** vào 6 service  
2. Đổi model / prompt **một chỗ**  
3. AI tốn tài nguyên → scale riêng  
4. Domain service vẫn chạy được khi AI tạm down (tùy bạn thiết kế fallback)

### 0.3. Gemini API key (bắt buộc)

AI service **luôn** gọi Google Gemini. Trước khi start:

1. Lấy key tại [Google AI Studio](https://aistudio.google.com/)
2. Set biến môi trường `GEMINI_API_KEY`

Không có key → service không khởi động / gọi AI sẽ lỗi.

---

## STEP 43 — Khởi tạo AI Assistant Microservice

### Ý nghĩa

Tạo một Spring Boot app mới, đăng ký Eureka, nối Config Server — **giống** các service khác, chưa cần Gemini.

### Đã làm trong repo

| File | Việc |
|------|------|
| `services/pom.xml` | Thêm module `job-portal-ai-service` |
| `services/job-portal-ai-service/pom.xml` | Web + Eureka + Config + Spring AI starters |
| `JobPortalAiServiceApplication.java` | `@SpringBootApplication` + `@EnableDiscoveryClient` |
| `application.yaml` | `name`, port `5007`, `config.import`, Eureka |
| `config-server/.../job-portal-ai-service.yaml` | Config tập trung |
| `api-gateway` routes | `Path=/api/ai/**` → `lb://job-portal-ai-service` |

### Checklist tự làm lại

1. Tạo module Maven  
2. Dependency webmvc + eureka-client + config  
3. `spring.application.name=job-portal-ai-service`  
4. Port 5007  
5. Route Gateway `/api/ai/**`  
6. Start Eureka → Config → AI service → thấy **UP** trên `:8761`

---

## STEP 44 — Cấu hình Google Gemini (Spring AI)

### Ý nghĩa

**Spring AI** = lớp trung gian giúp Java gọi LLM dễ hơn (không tự viết HTTP Gemini từ đầu).

Project dùng:

- Spring Boot **4.0.7**
- Spring AI **2.0.0**
- Starter chat: `spring-ai-starter-model-google-genai`
- Starter embedding: `spring-ai-starter-model-google-genai-embedding`

Root `pom.xml` import BOM:

```xml
<spring-ai.version>2.0.0</spring-ai.version>
```

### Abstraction trong code

```text
Controller → Service → AiCompletionClient
                              └─ GeminiAiCompletionClient  (ChatModel + EmbeddingModel)
```

Bạn **không** gọi Gemini rải rác trong controller — chỉ qua `AiCompletionClient`.

### Cấu hình Gemini

1. Lấy API key tại [Google AI Studio](https://aistudio.google.com/)  
2. Set biến môi trường:

```text
GEMINI_API_KEY=your_key_here
```

3. Config (local / Config Server) đã bật sẵn:

```yaml
spring:
  ai:
    model:
      chat: google-genai
      embedding:
        text: google-genai
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
```

4. Start AI service → response `provider` = `google-gemini`

### Bảo mật API key

- **Không** commit key vào Git  
- Để key trên Config Server / env / secret manager  
- Gateway vẫn cần JWT; AI service không public lộ key ra client

---

## STEP 45 — System Prompt (biến Gemini thành Job & Career Assistant)

### Prompt là gì? (người mới)

**Prompt** = hướng dẫn bạn đưa cho AI trước khi hỏi.

- Không có system prompt → AI trả lời kiểu “chat chung”  
- Có system prompt → AI đóng vai **trợ lý nghề nghiệp của portal này**

File: `prompt/SystemPrompts.java`

| Hằng số | Vai trò |
|---------|---------|
| `CAREER_ASSISTANT` | Chat tư vấn nghề nghiệp / portal |
| `RESUME_ANALYST` | Phân tích CV → JSON |
| `JOB_INSIGHT` | Sinh JD / lương / skill |
| `APPLICATION_SCREENER` | Chấm match ứng viên |
| `COVER_LETTER_WRITER` | Viết cover letter |
| `SEMANTIC_INTENT` | Hiểu câu tìm việc bằng ngôn ngữ tự nhiên |

### Vì sao bắt trả JSON?

Domain service / frontend cần **parse được** (score, list skill…), không chỉ đoạn văn dài.  
`AiJsonParser` còn gỡ markdown \`\`\`json nếu model hay bọc thêm.

---

## STEP 46 — AI Resume Analysis & Optimization

### API

```http
POST /api/ai/resumes/analyze
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "resumeText": "... nội dung CV ...",
  "targetJobDescription": "... optional JD để tư vấn sát hơn ..."
}
```

### Response (ý tưởng)

- `overallScore` — điểm tổng  
- `strengths` / `skillGaps` / `suggestions`  
- `extractedSkills` — skill AI rút ra  

### Luồng trong code

`ResumeAiController` → `ResumeAiServiceImpl` → system prompt `RESUME_ANALYST` → parse JSON.

---

## STEP 47 — AI Job Insights & Requirement Analyzer

### API

```http
POST /api/ai/jobs/insights
```

Body ví dụ: `title`, `rawNotes`, `city`, `experienceLevel`, …

### AI giúp employer

- Viết `generatedDescription`  
- Gợi ý `requirements` / `responsibilities` / `benefits`  
- Gợi ý khoảng lương  
- `recommendedSkills` / `recommendedTasks`

Employer vẫn **xem lại và sửa** trước khi đăng tin — AI hỗ trợ, không thay trách nhiệm pháp lý/nhân sự.

---

## STEP 48 — AI Screening ứng viên + Cover letter

### Screening

```http
POST /api/ai/applications/screen
```

Input: job title + requirements + resume (+ cover letter optional).

Output:

- `matchScore` (0–100)  
- `shortListStatus` — dùng enum `AiShortListStatus` trong common-lib  
- `tldrSummary` cho recruiter  
- matched / missing skills  

### Cover letter

```http
POST /api/ai/applications/cover-letter
```

Sinh thư xin việc từ highlights CV + tên job/company.

---

## STEP 49 — Semantic Search (tìm theo nghĩa)

### Keyword vs Semantic

| Keyword | Semantic |
|---------|----------|
| Chỉ khớp chữ “Java” | Hiểu “việc backend remote lương cao” ≈ Java/Spring remote |
| Dễ miss synonym | Dùng **embedding** (vector số) + so độ gần |

### Cách project làm

1. Embedding câu query + từng document (job text)  
2. Tính **cosine similarity** (`CosineSimilarity`)  
3. Đồng thời hỏi LLM hiểu intent → gợi ý role / skill / location  

API:

```http
POST /api/ai/search/semantic
```

Body gửi `query` + danh sách `documents` (id, title, text).  
AI service **không bắt buộc** đọc DB Job — caller (Job service / BFF) có thể gửi danh sách tin cần xếp hạng.

Embedding thật lấy từ Gemini Embedding model (`gemini-embedding-001`).

---

## STEP 50 — Testing tích hợp AI

### Test unit đã có

| Class | Kiểm tra gì |
|-------|-------------|
| `AiJsonParserTest` | Parse JSON + gỡ markdown fence |
| `CosineSimilarityTest` | Công thức similarity |

Chạy:

```bash
./mvnw -pl services/job-portal-ai-service -am test
```

### Checklist smoke qua Gateway (cần `GEMINI_API_KEY`)

1. Login lấy JWT  
2. `POST http://localhost:8080/api/ai/assistant/chat` + Bearer  
3. Thử `/api/ai/resumes/analyze`, `/api/ai/jobs/insights`, …  
4. Không token → **401**

---

## Triển khai từ đầu đến cuối (43→50)

```text
[ ] 43 Tạo module AI + Eureka + Config + Gateway route /api/ai/**
[ ] 44 Thêm Spring AI BOM + starters; GeminiAiCompletionClient
[ ] 45 Viết SystemPrompts cho từng use-case
[ ] 46 Resume analyze API
[ ] 47 Job insights API
[ ] 48 Application screen + cover letter API
[ ] 49 Semantic search (embed + cosine + intent JSON)
[ ] 50 Unit tests + smoke test qua Gateway với GEMINI_API_KEY
```

### Thứ tự start

```text
Eureka → Config → … domain services … → job-portal-ai-service → Gateway
```

---

## Bản đồ file (tra cứu nhanh)

| Step | File chính |
|------|------------|
| 43 | `services/job-portal-ai-service/**`, `config/.../job-portal-ai-service.yaml`, gateway routes |
| 44 | root `pom.xml` (spring-ai BOM), `ai/GeminiAiCompletionClient.java` |
| 45 | `prompt/SystemPrompts.java` |
| 46 | `controller/ResumeAiController.java`, `service/impl/ResumeAiServiceImpl.java` |
| 47 | `controller/JobAiController.java`, `service/impl/JobAiServiceImpl.java` |
| 48 | `controller/ApplicationAiController.java`, `service/impl/ApplicationAiServiceImpl.java` |
| 49 | `controller/SemanticSearchController.java`, `CosineSimilarity.java` |
| 50 | `src/test/java/...` |
| DTO chung | `common-lib/.../dto/ai/*` |

---

## Công thức nhớ

| Step | 1 câu |
|------|-------|
| 43 | Tạo “não AI” riêng trong hệ microservices |
| 44 | Nối dây tới Google Gemini qua Spring AI |
| 45 | Prompt = quy tắc ứng xử của AI |
| 46 | AI đọc CV và góp ý |
| 47 | AI giúp viết / phân tích tin tuyển dụng |
| 48 | AI chấm match + viết cover letter |
| 49 | Tìm việc theo **nghĩa**, không chỉ từ khóa |
| 50 | Test parser/similarity + smoke với Gemini thật |

---

Trước khi start AI service: set **`GEMINI_API_KEY`**, rồi gọi API qua Gateway `:8080/api/ai/**`.
