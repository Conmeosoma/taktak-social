# Taktak — Hồ sơ cá nhân (2C)

Ngày kiểm tra: 16/10/2026. Phạm vi được duyệt: **2C — GET/PUT**. Backend Java 17/Spring Boot; không thay schema V1, dependency, Android hoặc Firebase. Chưa triển khai avatar, tìm người dùng hay UI hồ sơ.

## Hợp đồng

| Request | Thành công | Điều kiện |
|---|---|---|
| GET `/api/users/me` | 200, hồ sơ người gọi | Bearer access JWT hợp lệ và user còn tồn tại |
| PUT `/api/users/me` | 200, hồ sơ sau cập nhật | Cùng xác thực; body JSON có `displayName` hợp lệ |

PUT body:

```json
{
  "displayName": "Tên hiển thị mới"
}
```

DTO trả về đúng năm trường: `id` (UUID), `email`, `username`, `displayName`, `createdAt` (ISO-8601 UTC). Không trả JPA entity, password/hash, refresh token/hash, avatarPath hoặc đường dẫn private. Response thành công đặt `Cache-Control: no-store`; không tạo cookie/session.

Danh tính chỉ lấy từ `sub` của JWT đã được Spring Security xác minh (chữ ký, issuer, audience, thời hạn và UUID). Không nhận ID người cần sửa từ client. Query `id`/`userId` không chọn được tài khoản khác. Body chỉ bind `displayName`; các trường khác được bỏ qua theo cấu hình Jackson hiện có, không cập nhật `email`, `username`, `passwordHash`, `avatarPath`, `createdAt` hay `id`. Không có endpoint GET/PUT hồ sơ người khác theo UUID trong 2C.

`displayName` dùng cùng quy tắc với đăng ký 2A: `String.strip()`, bắt buộc nonblank, tối đa 255 đơn vị UTF-16 sau strip (`@Size`, không phải giới hạn Unicode code point như caption). Giữ chữ Unicode, emoji và khoảng trắng bên trong; không đổi hoa/thường. PUT chỉ thay trường này trong transaction; gửi lại cùng tên không tạo dữ liệu phụ hoặc đổi `createdAt`.

## Lỗi

Giữ format chung `{timestamp,status,code,message,path}`, không phản chiếu body, token hoặc SQL.

| Trường hợp | HTTP / code |
|---|---|
| Thiếu access JWT, JWT sai/giả/hết hạn hoặc dùng refresh thay access | 401 `UNAUTHORIZED` |
| JWT có chữ ký hợp lệ nhưng UUID không còn có user | 401 `UNAUTHORIZED`, cùng thông báo xác thực chung |
| Thiếu/null/blank/tên quá dài, JSON không đọc được | 400 `VALIDATION_ERROR` |
| Content-Type không được hỗ trợ, gồm PUT không có Content-Type | 415 `UNSUPPORTED_MEDIA_TYPE` |

Hết hạn access phải refresh rồi gọi lại theo [AUTH_API.md](AUTH_API.md). Logout thu hồi refresh, không làm access JWT còn hạn mất hiệu lực ngay; giới hạn này của 2B giữ nguyên.

## Chạy và kiểm tra trên Windows

Tại Git root, chọn JDK 17 theo helper rồi chạy toàn bộ backend:

```powershell
. .\scripts\Use-TaktakJava.ps1 -Target Server
Push-Location server
try {
    .\mvnw.cmd '-Dmaven.repo.local=../.tools/maven-repository' -B -ntp verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
} finally {
    Pop-Location
}
```

Để thử thủ công mà không dùng database dev, chạy terminal thứ nhất:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LocalSmokeServer.ps1 -Port 8080
```

Đăng ký tài khoản test theo [REGISTRATION_API.md](REGISTRATION_API.md). Terminal thứ hai đăng nhập bằng mật khẩu nhập ẩn, giữ token trong bộ nhớ và chỉ in hồ sơ:

```powershell
$profileBase = 'http://127.0.0.1:8080'
$profileEmail = Read-Host 'Email tai khoan test da dang ky'
$profileSecret = Read-Host 'Mat khau' -AsSecureString
$profileCredential = [PSCredential]::new('local-test', $profileSecret)
try {
    $profileLoginBody = @{
        email = $profileEmail
        password = $profileCredential.GetNetworkCredential().Password
    } | ConvertTo-Json
    $profileSession = Invoke-RestMethod -Method Post -Uri "$profileBase/api/auth/login" `
        -ContentType 'application/json; charset=utf-8' `
        -Body ([Text.Encoding]::UTF8.GetBytes($profileLoginBody))
    $profileHeaders = @{ Authorization = 'Bearer ' + $profileSession.accessToken }
    Invoke-RestMethod -Method Get -Uri "$profileBase/api/users/me" -Headers $profileHeaders
    $profileBody = @{ displayName = 'Taktak member' } | ConvertTo-Json
    Invoke-RestMethod -Method Put -Uri "$profileBase/api/users/me" -Headers $profileHeaders `
        -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($profileBody))
    Invoke-RestMethod -Method Get -Uri "$profileBase/api/users/me" -Headers $profileHeaders
} finally {
    Remove-Variable profileSession, profileHeaders, profileLoginBody, profileCredential, profileSecret, profileBody `
        -ErrorAction SilentlyContinue
}
```

Không in response login, token hoặc Authorization vào log/chat. HTTP chỉ dành cho loopback/debug tin cậy; public/release bắt buộc HTTPS. Nhấn Enter ở terminal smoke để đóng database test riêng; tài khoản test không được giữ lại. Server dev bền vững cần DB env và JWT secret riêng theo AUTH_API, không tự nạp `.env`.

## Bằng chứng nghiệm thu

Maven `verify` chạy trên JDK 17 và PostgreSQL test thật: **36 test, 0 failures/errors/skipped**, BUILD SUCCESS, executable JAR đóng gói. Trong đó 29 test 2A/2B/bootstrap giữ lại và 7 test hồ sơ mới trong `AuthenticationIntegrationTest`:

- GET đúng user, DTO đúng năm trường, no-store và không cookie.
- PUT tên Unicode/emoji/dấu nháy, strip, persistence qua GET/SQL và idempotency; các cột khác giữ nguyên.
- Hai tài khoản A/B: giả ID/query/body và mass assignment không sửa/xem B; route theo ID chưa hỗ trợ trả 4xx.
- Body thiếu/null/blank/quá dài/sai cấu trúc và Content-Type sai bị từ chối; dữ liệu không đổi.
- Biên 255 đơn vị UTF-16 nhận, 256 từ chối.
- GET/PUT thiếu JWT, malformed, refresh dùng nhầm, access hết hạn và JWT giả chữ ký đều 401; refresh rồi GET thành công.
- JWT ký đúng nhưng không có user trả 401 cho cả GET/PUT, không tạo user.

Smoke HTTP ngoài test profile đã chạy: register **201** → login **200** → GET me **200** → PUT **200** → GET lại **200** → refresh **200** → GET bằng access mới **200** → logout **204**. Đã kiểm tra tên được lưu, trường email gửi thêm không đổi, DTO không chứa trường private; không in credentials.

Đã kiểm tra XML Surefire, production JAR có `UserController` và không chứa probe/test harness/PostgreSQL nhúng. Báo cáo local tại `server/target/surefire-reports/` (ignore). Các kết quả 36 test/HTTP smoke trên thuộc mốc 2C; sau phê duyệt riêng, 2D bổ sung curl thật và chốt [hợp đồng lỗi](API_ERRORS.md), nâng suite lên 39 test PASS theo [QA_AUTH.md](QA_AUTH.md). Android/VPS/HTTPS/UI hồ sơ chưa kiểm thử trong 2C/2D. Bước tiếp theo là 3A, cần được duyệt riêng.
