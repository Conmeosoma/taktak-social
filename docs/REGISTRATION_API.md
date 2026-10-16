# Taktak — API đăng ký (2A)

Đã được duyệt bằng yêu cầu “continue” sau khi bàn giao Bước 0/giai đoạn 1, ngày 16/10/2026. Trang này mô tả riêng đăng ký 2A; luồng login/JWT/refresh/logout đã triển khai ở [AUTH_API.md](AUTH_API.md). Chưa Android auth.

## Hợp đồng

`POST /api/auth/register`, `Content-Type: application/json`, không yêu cầu token.

| Trường | Quy tắc |
|---|---|
| email | Bắt buộc; email hợp lệ theo Jakarta Validation; tối đa 254 ký tự; strip và lowercase với Locale.ROOT |
| username | Bắt buộc; strip và lowercase với Locale.ROOT; chỉ a–z, 0–9, dấu gạch dưới; tối đa 255 ký tự theo schema V1 |
| displayName | Bắt buộc; strip; không được trống; tối đa 255 ký tự |
| password | Bắt buộc; tối thiểu 8 Unicode code point, tối đa 72 byte UTF-8; không chứa NUL; không trim, lowercase hoặc cắt chuỗi |

Server sinh UUID và thời gian UTC. Thành công trả **201** với đúng `id`, `email`, `username`, `displayName`, `createdAt`; chưa cấp token/phiên, không trả hash hoặc đường dẫn avatar.

Mật khẩu được hash bằng BCrypt cost 12 với salt ngẫu nhiên. Record request có password write-only và toString redacted; lỗi không phản chiếu request/rejected value. Không log body đăng ký hoặc bật SQL parameter logging.

Service có transaction; `saveAndFlush` buộc PostgreSQL kiểm tra unique trước khi trả kết quả. Dùng nguyên `users_username_key` và `uq_users_email_lower` của V1, không sửa migration hoặc chỉ dựa vào kiểm tra exists trước insert. Trùng username/email trả **409 ACCOUNT_CONFLICT** chung. Hibernate có thể log SQLState 23505/constraint và identity trùng; không bật logging bind parameters có thể chứa password hash. Những dòng duplicate trong test là tình huống mong đợi với dữ liệu giả, không phải lỗi test bị bỏ qua.

Lỗi API đăng ký có năm trường theo đặc tả: `timestamp`, `status`, `code`, `message`, `path`. Validation/JSON lỗi trả **400 VALIDATION_ERROR**; Content-Type không hỗ trợ trả **415 UNSUPPORTED_MEDIA_TYPE**. Response không chứa stack trace/SQL/password. Health GET vẫn public; auth POST đã mở theo 2B, các API khác yêu cầu JWT.

## Chạy và thử trên Windows

Tại Git root, dùng JDK 17. Test tự tạo PostgreSQL cluster mới, không cần cấu hình DB cá nhân:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
. .\scripts\Use-TaktakJava.ps1 -Target Server
Push-Location server
.\mvnw.cmd -B -ntp verify
Pop-Location
```

Nếu đã chuẩn bị Maven cache tại `.tools/maven-repository`, có thể truyền `-Dmaven.repo.local=../.tools/maven-repository` khi ở `server/`. Test dùng port ngẫu nhiên và tự đóng cluster, không reset database có sẵn.

Muốn thử HTTP thủ công, chạy terminal thứ nhất:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LocalSmokeServer.ps1 -Port 8080
```

Terminal thứ hai dùng mật khẩu nhập ẩn, không ghi giá trị vào history hoặc file:

```powershell
$registrationSecret = Read-Host 'Mat khau tai khoan test' -AsSecureString
$registrationCredential = [PSCredential]::new('local-test', $registrationSecret)
try {
    $registrationBody = @{
        email = 'member@example.test'
        username = 'member'
        displayName = 'Taktak member'
        password = $registrationCredential.GetNetworkCredential().Password
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/auth/register' `
        -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($registrationBody))
} finally {
    Remove-Variable registrationBody, registrationCredential, registrationSecret -ErrorAction SilentlyContinue
}
```

Mật khẩu ở dạng rõ trong bộ nhớ khi gửi HTTP là cần thiết cho request; không in body. Chỉ dùng tài khoản test trên loopback. Nhấn Enter ở terminal smoke để dừng, dữ liệu test sẽ không được giữ. Với database dev riêng, đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `JWT_ACCESS_SECRET` vào môi trường theo `server/.env.example` và AUTH_API; Spring Boot không tự nạp `.env`.

Các lệnh thủ công trên là hướng dẫn chạy lại; nghiệm thu tự động dùng HTTP thật trong integration test. Chưa deploy hoặc mở endpoint đăng ký ra Internet; rate limiting login/refresh/logout hiện được mô tả tại AUTH_API.

## Kiểm thử và giới hạn

Bộ `ServerBootstrapTest` hiện gồm 13 test (6 bootstrap cũ và 7 đăng ký mới): tạo/normalize/BCrypt/DTO; duplicate tuần tự; hai request đồng thời trùng username; hai request đồng thời trùng email; validation/null/blank/độ dài/Unicode/NUL không tạo row; biên 72 byte và salt khác nhau; JSON/Content-Type lỗi và các route còn đóng.

Các phép kiểm tra HTTP sử dụng server Spring Boot thật, JPA và PostgreSQL thật qua Flyway, không mock repository. Chưa chứng minh luồng login/refresh/me hoặc Android auth; không đánh dấu toàn giai đoạn 2 DONE.

Kết quả đã chạy ngày 16/10/2026: Maven `verify` hai lần đều BUILD SUCCESS; bản cuối **13 test / 0 failures / 0 errors / 0 skipped**, JAR đã đóng gói. Báo cáo local: `server/target/surefire-reports/com.taktak.server.ServerBootstrapTest.txt` và XML tương ứng (được ignore). Hai test concurrency kiểm tra đồng thời HTTP và xác nhận DB chỉ có một user; test không chỉ dựa vào HTTP status.

Tham chiếu: [đặc tả](TAKTAK_PROJECT.md), [trạng thái](PROJECT_STATUS.md), [quyết định](PROJECT_DECISIONS.md), [Spring BCrypt](https://github.com/spring-projects/spring-security/blob/main/crypto/src/main/java/org/springframework/security/crypto/bcrypt/BCrypt.java).
