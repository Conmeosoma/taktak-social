# Taktak — Đăng nhập và phiên xác thực (2B)

Ngày nghiệm thu 2B: 16/10/2026. Người dùng duyệt riêng “triển khai 2B”. Tài liệu này ghi hợp đồng phiên của 2B; hồ sơ `/api/users/me` được triển khai tiếp ở 2C, xem [PROFILE_API.md](PROFILE_API.md). Android auth được nối ở 3B, xem [QA_ANDROID_AUTH.md](QA_ANDROID_AUTH.md); chưa deploy.

## Endpoint

Tất cả request dưới đây dùng JSON, không dùng cookie/session.

| Endpoint | Body | Thành công |
|---|---|---|
| POST `/api/auth/login` | `email`, `password` | 200: `accessToken`, `refreshToken`, `expiresIn` (giây), `tokenType=Bearer` |
| POST `/api/auth/refresh` | `refreshToken` | 200: cặp token mới; refresh cũ bị thu hồi trong cùng transaction |
| POST `/api/auth/logout` | `refreshToken` | 204: thu hồi refresh được cung cấp; lặp lại/không tìm thấy vẫn 204 |

Email đăng nhập được strip/lowercase bằng Locale.ROOT. Password giữ nguyên, cùng quy tắc 2A (ít nhất 8 Unicode code point, tối đa 72 byte UTF-8, không NUL). Sai password hoặc email chưa tồn tại đều trả 401 `UNAUTHORIZED` với cùng thông báo; trường hợp user không tồn tại vẫn so BCrypt với dummy hash ngẫu nhiên để giảm khác biệt thời gian.

Refresh token là chuỗi opaque gồm 43 ký tự Base64url, được tạo từ SecureRandom 32 byte. DB chỉ lưu SHA-256 hex trong bảng `refresh_tokens` sẵn có. Không trả entity/hash/đường dẫn private. Token request/response có toString redacted; không bật logging body hoặc bind parameters SQL. Response chứa token đặt `Cache-Control: no-store`, `Pragma: no-cache`.

## Access JWT và filter chain

- JWT HS256 được ký/xác minh bằng Spring Security OAuth2 Resource Server + Nimbus, dependency do Spring Boot 3.5.16 quản lý: Security OAuth2 6.5.11, Nimbus JOSE JWT 9.37.4 theo JAR đã build; không tự viết thuật toán JWT hoặc nâng các phiên bản khác.
- `iss=https://taktak.invalid` là định danh issuer logic, không phải URL backend và không gọi HTTP/discovery đến địa chỉ này. `aud=taktak-app`, `sub` UUID user, có `iat`, `exp`, `jti` ngẫu nhiên.
- Kiểm tra chữ ký/HS256, issuer, audience, subject, iat và exp; hết hạn ngay khi thời gian hiện tại đạt exp, không cho grace period.
- Access mặc định 15 phút, giới hạn cấu hình 1–60 phút. Refresh mặc định 30 ngày, giới hạn 1–90 ngày. Mỗi rotation cấp refresh mới với thời hạn tính từ lúc xoay.
- Health GET và bốn auth POST dùng chain riêng không chạy bearer filter. Refresh/logout xác thực bằng refresh token trong body, không phụ thuộc access JWT; bearer hết hạn/sai được gửi kèm không chặn các endpoint này.
- API ngoài auth yêu cầu JWT; các tài nguyên không thuộc API như actuator env tiếp tục bị chặn. Lỗi security trả JSON 401/403, không redirect, cookie hoặc nội dung token lỗi.

## Rotation, logout và đồng thời

Ngoại lệ được người dùng duyệt19/10/2026: reset mật khẩu thành công tăng security_version và thu hồi tất cả refresh; JWT đã cấp trước reset bị401 ở request mới, kể cả JWT legacy thiếu sv. Logout thông thường bên dưới giữ nguyên chính sách. Các API xác minh email/recovery và yêu cầu SMTP: [ACCOUNT_RECOVERY.md](ACCOUNT_RECOVERY.md).

Refresh đọc bản ghi bằng pessimistic write lock, kiểm tra revoked/expiry, revoke cũ và insert hash mới trong một transaction. Hai request đồng thời dùng một refresh: chỉ một request nhận 200, request còn lại 401, chỉ một refresh mới còn hiệu lực. Nếu cấp JWT thất bại, transaction rollback và token cũ vẫn dùng được.

Logout chỉ thu hồi refresh token được gửi, không thu hồi các lần login độc lập khác. JWT đã cấp vẫn hợp lệ đến exp (mặc định tối đa 15 phút); không có access-token denylist. Token cũ đã xoay không dùng lại được nhưng hiện không có mô hình token-family/revoke cả lineage; logout phải dùng refresh hiện tại. Android ở 3B/3C cần đồng bộ refresh/logout và xóa local tokens. Không tuyên bố logout thu hồi tức thời mọi access JWT hoặc mọi thiết bị.

## Giới hạn login

Fixed window 60 giây, tối đa 20 lần/IP và 5 lần/email chuẩn hóa; tính cả lần đúng/sai, không reset khi login thành công. IP lấy trực tiếp từ kết nối, không tin `X-Forwarded-For` của client. Vượt giới hạn trả 429 `LOGIN_RATE_LIMITED`, `Retry-After: 60`.

Bộ đếm đồng bộ thread, tối đa 10.000 key đang hoạt động trong một JVM; hết cửa sổ được loại, đầy bộ nhớ giới hạn thì từ chối tiếp thay vì bỏ quota. Counter reset khi restart và chưa chia sẻ giữa nhiều instance. Khi làm VPS/proxy ở giai đoạn 8, phải cấu hình trusted proxy/rate limit phù hợp; hiện reverse proxy có thể khiến nhiều client cùng được tính dưới một IP. Không thêm Redis hoặc tin header tùy ý để né giới hạn.

## Cấu hình và chạy Windows

Server thật cần `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và **`JWT_ACCESS_SECRET`** trong môi trường. Secret bắt buộc là Base64 của ít nhất 32 byte ngẫu nhiên; thiếu/rỗng/sai Base64/ngắn bị từ chối ở bean khởi tạo, không có khóa mặc định production. Không chia sẻ/commit secret.

Ví dụ tạo khóa chỉ trong process PowerShell, không in giá trị:

```powershell
$authKeyBytes = New-Object byte[] 32
$authRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $authRandom.GetBytes($authKeyBytes)
    $env:JWT_ACCESS_SECRET = [Convert]::ToBase64String($authKeyBytes)
} finally {
    $authRandom.Dispose()
    [Array]::Clear($authKeyBytes, 0, $authKeyBytes.Length)
    Remove-Variable authKeyBytes, authRandom
}
```

Giữ khóa ổn định trong môi trường dev bền vững; tạo khóa mới khiến access token đã cấp không còn xác minh được. Spring Boot không tự nạp `.env`; file mẫu là `server/.env.example`. Sau khi đặt DB env/JDK 17, chạy `server/mvnw.cmd spring-boot:run`. HTTP chỉ dùng trên loopback/debug tin cậy; public/release phải HTTPS.

Muốn thử mà không đụng DB dev, tại Git root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LocalSmokeServer.ps1 -Port 8080
```

Smoke runner ở test source tự tạo PostgreSQL và signing key tạm, không in key. Đây không phải fallback của production. Nhấn Enter để đóng cluster riêng. Hướng dẫn tạo tài khoản bằng mật khẩu nhập ẩn ở [REGISTRATION_API.md](REGISTRATION_API.md). Dùng token trong bộ nhớ để POST refresh/logout; không in response chứa token vào log/báo cáo.

## Kết quả đã chạy tại mốc 2B

`mvnw.cmd -B -ntp verify` trên JDK 17, Maven local cache: **BUILD SUCCESS, 29 test, 0 failures/errors/skipped**, JAR đóng gói.

| Nhóm | Kết quả |
|---|---|
| Bootstrap + đăng ký | 13/13 PASS, bao gồm schema/Flyway, BCrypt, validate, unique/race 2A |
| AuthenticationIntegrationTest | 12/12 PASS trên HTTP thật + PostgreSQL: login/hash; sai thông tin; JWT hết hạn/sai key/issuer/audience/subject/iat/thiếu exp/alg none; refresh khi access hết hạn kèm expired bearer; rotation/race/rollback; logout/idempotency/nhiều session; account/IP rate limit và invalid body |
| TokenConfigurationTest | 3/3 PASS: thiếu/sai/ngắn key; key ngẫu nhiên hợp lệ; thời hạn ngoài giới hạn bị từ chối |
| LoginRateLimiterTest | 1/1 PASS: 20 luồng từ các IP khác nhau cùng email, chỉ 5 lần được nhận |
| Smoke HTTP ngoài test profile | Register 201 → login 200 → refresh 200 → replay refresh cũ 401 → logout 204 → refresh đã revoke 401 |
| Artifact policy | Production JAR không chứa probe controller, clock/encoder test, test harness hoặc PostgreSQL nhúng |

Test thời gian dùng Clock điều khiển riêng trong test; production Clock.systemUTC. Protected probe chỉ có trong test source/profile, không triển khai `/users/me` sớm. Test fault injection cố ý làm encoder thất bại để chứng minh rollback; dòng ERROR “JWT encoding failed” trong phép thử này là kết quả mong đợi, không phải lỗi bị bỏ qua. Dữ liệu tài khoản test là giả và cluster tạm được đóng.

Lần verify đầu có 2 failure/1 error do probe test chưa đăng ký và issuer chưa dùng dạng URL mà Spring accessor yêu cầu; đã sửa nguyên nhân và chạy lại, không bỏ test hoặc nới kỳ vọng. Báo cáo mới nằm ở `server/target/surefire-reports/` (ignore). Android test không chạy lại vì source Android không đổi trong 2B.

2B PASS. Kết quả 29 test ở trên là mốc 2B; sau phê duyệt riêng, 2C thêm hồ sơ, 2D bổ sung curl thật và chốt [hợp đồng lỗi](API_ERRORS.md), suite hiện tại 39 test PASS theo [QA_AUTH.md](QA_AUTH.md). Giai đoạn 2 PASS trong phạm vi backend local; chưa Android auth/VPS. Đề xuất tiếp theo là 3A, chưa được duyệt.

Tham chiếu: [quyết định](PROJECT_DECISIONS.md), [đặc tả](TAKTAK_PROJECT.md), [trạng thái](PROJECT_STATUS.md), [Spring Security filter chains](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html), [NimbusJwtDecoder 6.5](https://www.springframework.org/spring-security/reference/6.5/api/java/org/springframework/security/oauth2/jwt/NimbusJwtDecoder.html).
