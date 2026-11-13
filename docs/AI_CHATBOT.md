# Taktak AI — Backend API

> Cập nhật phạm vi13/11/2026: Android AI UI đã được triển khai và QA một luồng USB
> bằng fixture, xem [QA_AI_ANDROID.md](QA_AI_ANDROID.md). Người dùng vừa duyệt
> triển khai backend/V7 lên VPS và push main; chưa yêu cầu build APK trong lượt này.
> Các ghi chú backend-only/chưa deploy bên dưới mô tả cổng nghiệm thu trước đó;
> kết quả triển khai thực tế xem [DEPLOY_AI_20261113.md](DEPLOY_AI_20261113.md).

Phạm vi người dùng duyệt13/11/2026: AI-01–08, chỉ backend Java17/Boot3.
**Android AI Chat UI — CHƯA TRIỂN KHAI.** Không deploy/migrate production,
không commit/push trong nhiệm vụ này. Không thay JWT/refresh/FCM/chat bạn bè.

## Kiến trúc và luồng xử lý

AiChatController → AiChatService → AiRepository + AiProvider/GeminiProvider.
GeminiKeyManager quản lý credential health/quota group trong RAM, đồng bộ bằng
monitor. REST generateContent chính thức được dùng với java.net.http Java17:
không thêm SDK, kiểm soát redirect/timeout/body/retry ở một lớp. JDBC repository
và TransactionTemplate theo module chat sẵn có, giữ Spring Data JPA đang dùng
cho các phần khác; không thêm entities/repositories JPA trùng chức năng.

JWT bắt buộc cả status và lịch sử. Actor lấy từ JWT subject, không nhận userId.
Một user chỉ xem/xóa/gửi trong hội thoại của chính mình; người khác nhận404.
Chat cần `shareWithGoogle=true`: frontend phải hiển thị thông báo trước khi gửi.
Chỉ nội dung AI chat/lịch sử gần đây được gửi cho Google; không đọc ảnh, hồ sơ,
danh bạ hoặc chat1–1 Taktak để đưa vào prompt. Không tools/web/image calling.

Service giới hạn concurrency trước khi bắt đầu transaction. User-row lock
NOWAIT tuần tự hóa các chat của cùng tài khoản, kể cả tạo hội thoại mới; conflict
409 AI_BUSY. Provider thành công mới lưu cả user+assistant và request ledger
trong cùng giao dịch; lỗi upstream rollback, không có fake answer. Idempotency
theo `(user,clientRequestId)`; cùng payload trả đúng response đã lưu, khác payload
409. Giữ nguyên conversationId của request gốc khi retry, kể cả giá trị null.

Đánh đổi: transaction/DB connection giữ trong lúc gọi provider, tối đa deadline
và chỉ2call mặc định. Phù hợp một instance nhỏ; không tuyên bố tối ưu multi-node.
Google không cung cấp exactly-once generation ở đây: crash sau khi Google trả
lời nhưng trước commit có thể dẫn tới tính phí lại khi client gửi lại. Không
retry transport timeout tự động. Idempotency ledger cũng bị xóa khi xóa hội thoại.

## API contract

Base `/api/ai`, JSON, Bearer access JWT hiện có; response no-store.

| Method/path | Chức năng |
|---|---|
| GET `/status` | availability + privacyNotice, không có metadata/key |
| POST `/conversations` | tạo hội thoại, trả id/title/createdAt/updatedAt |
| GET `/conversations?page=0` | items/hasNext,20mục, mới nhất trước |
| GET `/conversations/{id}/messages?page=0` | items/hasNext,20tin, mới nhất trước |
| DELETE `/conversations/{id}` |204, xóa cứng lịch sử và ledger thuộc chủ |
| POST `/chat` | tạo/gửi hội thoại và nhận assistant response |

```json
{
  "conversationId": null,
  "clientRequestId": "11111111-1111-4111-8111-111111111111",
  "message": "Xin chào Taktak AI",
  "shareWithGoogle": true
}
```

Response200:
```json
{
  "id": "22222222-2222-4222-8222-222222222222",
  "conversationId": "33333333-3333-4333-8333-333333333333",
  "role": "assistant",
  "content": "Xin chào!",
  "createdAt": "2026-11-13T13:00:00Z",
  "inputTokens": 20,
  "outputTokens": 5
}
```

Frontend Java Retrofit: thêm DTO tương ứng, dùng ApiClient/SessionManager hiện
có; giữ clientRequestId khi retry, tạo UUID mới khi đổi message. Đọc Retry-After,
không tự gửi vòng lặp 429/503. Xóa local history view khi DELETE thành công;
không nhúng key/file secret. Dùng `/status` hiển thị disabled/config/unavailable
và privacyNotice. Không render content thành HTML tự thực thi; đó là văn bản.

## Schema và lưu/xóa dữ liệu

V7__ai_chat.sql additive: ai_conversations(owner,title,timestamps), ai_messages
(role/content/token usage), ai_requests(request hash/response reference).
FK owner cascade khi xóa tài khoản; xóa hội thoại cascade tin nhắn/ledger.
Index owner+updated và conversation+timestamp; unique request role ngăn trùng.
Không sửa V1–V6. Không áp V7 lên VPS trong phiên này.

Tối đa100hội thoại/user và1000tin/hội thoại. Lưu cho tới khi chủ xóa hoặc xóa
tài khoản; chưa có retention tự động được duyệt. Xóa DB không xóa ngay các bản
backup hoặc dữ liệu provider đã nhận. Không mã hóa đầu cuối; backend đọc được
và gửi chat AI tới Google. Chính sách provider/quyền dùng free tier cần chủ dự
án tự kiểm tra trước khi bật production. Không tự kết nối billing.

## Cấu hình

Mặc định tắt và model trống; không tự gọi mạng lúc startup, thiếu key không làm
các module khác crash. Chỉ bật sau khi chuẩn bị credentials/model và policy.

```dotenv
GEMINI_ENABLED=true
GEMINI_MODEL=gemini-3.5-flash-lite
GEMINI_CREDENTIALS_FILE=/var/taktak/secrets/gemini.json
GEMINI_MAX_OUTPUT_TOKENS=1024
GEMINI_REQUEST_TIMEOUT_SECONDS=30
GEMINI_MAX_RETRIES=2
GEMINI_MAX_CONCURRENT=2
GEMINI_USER_RPM=5
GEMINI_APP_RPM=20
GEMINI_APP_DAILY_REQUESTS=100
GEMINI_CONTEXT_CHARS=16000
# Tùy chọn; để trống dùng mặc định provider, chỉ đặt khi model hỗ trợ.
GEMINI_TEMPERATURE=
```

Số trên là ngân sách ứng dụng, không phải hạn mức free tier của Google.
Output32–4096token, timeout1–60s, retry0–2, concurrency1–4 được clamp an toàn;
context4000–32000ký tự, không phải token count chính xác. Context tối đa8cặp
gần nhất, thêm message mới; không cắt giữa một cặp. Input<=4000UTF-16unit và
body<=24KiB, không nhận NUL. Response upstream<=128KiB, answer<=16000ký tự.
Connect timeout5s, overall request deadline dùng chung cho các attempt.

File credential server-side, không lưu DB hoặc Android:
```json
{"keys":[
  {"key":"<set-locally>","quotaGroup":"owned-project-a","enabled":true},
  {"key":"<set-locally>","quotaGroup":"owned-project-a","enabled":false}
]}
```

Tối đa16credential. Key ID nội bộ sinh tự động, lưu enabled/invalid/cooldown/
lastError/lastSuccess/failures; không đưa metadata này ra API. Key bị invalid
không được chọn cho tới reload cấu hình bằng restart được duyệt. Không hot reload.
Các key cùng project phải dùng cùng quotaGroup. Không biết project thì dùng
`unclassified` cho tất cả, không tự suy ra project từ giá trị key. Không tự tạo
key/project hoặc mở billing. Chỉ dùng project dự phòng hợp lệ đã được chủ sở
hữu cấu hình; bản này không tự failover project trên429 hoặc permission denial.

Local đã nhận7key từ file người dùng cung cấp, chuyển vào `.local/secrets/gemini.json`
được Git ignore, tất cả chung `unclassified`. Không log/commit giá trị key.
Không copy file local này vào Docker image hoặc repository. Local PowerShell:

```powershell
$env:GEMINI_ENABLED='true'
$env:GEMINI_MODEL='gemini-3.5-flash-lite'
$env:GEMINI_CREDENTIALS_FILE=(Resolve-Path '.local/secrets/gemini.json').Path
. ./scripts/Use-TaktakJava.ps1 -Target Server
# Dùng DB_URL/DB_USERNAME/DB_PASSWORD/JWT_ACCESS_SECRET local đã chuẩn bị.
# Chạy JAR hoặc Maven theo hướng dẫn backend hiện có.
```

VPS cần duyệt deploy JAR/V7 riêng, backup DB và giữ binary hỗ trợ security_version.
Đặt secret file600 do service user đọc, thêm biến vào EnvironmentFile hiện hành.
Docker cần mount credential read-only đúng path trong container, không COPY
trong Dockerfile. Không mở Actuator metrics hoặc quản lý key công khai.

## Retry/quota/errors/observability

| Tình huống | Xử lý |
|---|---|
|429|RATE/TOKEN_RATE/DAILY/BILLING; cooldown toàn group, không retry/switch key cùng group|
|quota không rõ reset|cooldown tối thiểu60s; daily24h bảo thủ, billing1h; tôn trọng Retry-After trong phạm vi24h|
|503/500/502/504|bounded retry với backoff+jitter, theo deadline chung; key không bị vô hiệu vĩnh viễn|
|401/403 key xác minh invalid/leaked|đánh dấu invalid, failover credential tối đa retry cap|
|403 permission/billing|cooldown project, không đổi project để né hạn chế|
|400|không retry payload sai|
|404 model|AI_CONFIGURATION_ERROR; không retry và không đổi key để tìm model|
|timeout/network|AI_TEMPORARILY_UNAVAILABLE, không tự replay kết quả mơ hồ|
|safety/empty content|AI_CONTENT_UNAVAILABLE, không lưu câu trả lời giả|
|all keys cooling|AI_TEMPORARILY_UNAVAILABLE; không vòng lặp vô hạn|

Status: DISABLED / CONFIGURATION_ERROR / CONFIGURED / TEMPORARILY_UNAVAILABLE.
CONFIGURED không bảo đảm còn quota, không gọi Gemini để ping mỗi GET status.
Error envelope hiện hành, thêm Retry-After khi biết; quota/rate/busy429,
same-user lock conflict409, ownership404, invalid payload400, upstream503.
AI_DISABLED503; feature flag không chặn đọc/xóa lịch sử của chủ đã có.

Rate/budget theo user và app, fixed-minute/day UTC, bounded10000user keys trong
RAM. Các count tính cả failed attempt admission, không refund; restart làm mất
counter/cooldown, không phải financial spend cap bền vững. Không dùng nhiều
instance trước khi có cơ chế quota chung. Retry có thể tăng số upstream calls
tới3lần/request; max token và retry cap phải được tính khi đặt ngân sách.

Metrics taktak.ai.success/errors/latency/tokens chỉ tag hữu hạn; không user ID,
prompt, model tùy client, key hoặc raw upstream message. Log chỉ HTTPstatus và
nhóm lỗi cố định. Không log request/response bodies hoặc bật Java HTTP wirelog.

## Xác minh

Các test mock không cần key. PostgreSQL nhúng thật theo repo, không Testcontainers.
```powershell
. ./scripts/Use-TaktakJava.ps1 -Target Server
./.tools/apache-maven-3.9.16/bin/mvn.cmd -o -f server/pom.xml `
  '-Dmaven.repo.local=C:/Users/NamTien/Documents/taktak/.tools/maven-repository' verify
```

Smoke thật opt-in `-Dtest=GeminiLiveIT test`, cần GEMINI_MODEL và credentialfile;
chỉ một prompt công khai nhỏ, không nằm trong suite mặc định. Lần này models
list thành công;2.5FlashLite trả404 ở generate (2request gồm diagnostic), model
3.5FlashLite generate **PASS1/1**, không đổi key. Không coi list model là đủ bằng
chứng generate. Tổng3generation attempts, chỉ1thành công, không dữ liệu riêng tư.
Kết quả full regression cập nhật trong PROJECT_STATUS/SESSION_HANDOFF.

Full regression đã chạy217/217 PASS,0fail/error/skipped, BUILD SUCCESS02:54.
Sau review backoff và contention khi tạo/xóa,21ca AI được chạy lại cùng đóng
gói JAR; kết quả cuối ghi ở PROJECT_STATUS. Lượt đầu test đã tìm và sửa lỗi
DI constructor, Mockito nested stubbing, binding Instant PostgreSQL, precision
timestamp response replay và ánh xạ NOWAIT55P03. Không bỏ hoặc nới test lỗi.

Lượt review cuối **21/21 PASS**,0fail/error/skipped, JAR BUILD SUCCESS37,866s.
JAR: `server/target/taktak-server-0.0.1-SNAPSHOT.jar`, SHA256
`021AECCD73737A4D9D4A4DF4B40BF51BE1C698918569412700DD3656D2F4EF45`.
Quét source/docs/log AI theo7keythật0finding, gitignore secretfile đã xác minh,
diffcheck không lỗi. PASS backend local + provider live; chưa VPS/AndroidUI.

## File thay đổi

- `server/src/main/java/com/taktak/server/ai/`: AiSettings, AiFailure, AiDtos,
  AiProvider, GeminiKeyManager, GeminiErrorClassifier, GeminiProvider,
  AiRepository, AiChatService, AiChatController, AiBodyLimitFilter.
- `server/src/main/resources/db/migration/V7__ai_chat.sql`.
- `server/src/main/java/com/taktak/server/common/ApiExceptionHandler.java`.
- Test mới: AiChatIntegrationTest, ai/GeminiProviderTest, ai/GeminiLiveIT.
- SchemaUpgradeCompatibilityTest và ServerBootstrapTest cập nhật kỳ vọng V7,
  vẫn giữ các assertion revocation/old data và không reapply migration.
- AGENTS.md, docs/TAKTAK_PROJECT.md, PROJECT_DECISIONS.md, PROJECT_STATUS.md,
  TASKS.md, SESSION_HANDOFF.md, tài liệu này và deploy/.env.example.
- Các helper/file key trong `.local/` chỉ dành local, được ignore.

Không sửa pom.xml, SDK version, Android, Firebase hoặc migration cũ. Các diff
Camera XML/Android URL và báo cáo ngoài nhiệm vụ có sẵn được giữ nguyên.

Nguồn chính thức: [REST generateContent](https://ai.google.dev/api/generate-content),
[models](https://ai.google.dev/gemini-api/docs/models),
[quota theo project](https://ai.google.dev/gemini-api/docs/rate-limits).
