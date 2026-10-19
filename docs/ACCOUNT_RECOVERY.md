# Xác minh email và khôi phục tài khoản Taktak

Phạm vi A được duyệt ngày 19/10/2026. Không triển khai phần B — validation đăng ký. Chỉ Java/XML/ViewBinding; Spring Boot 3.5.16 và Java 17. Dùng Spring Boot Mail theo BOM hiện có, không Firebase Auth.

## Chính sách đã được duyệt

- Tài khoản mới/cũ mặc định **chưa xác minh**; không backfill tự động. Người còn biết mật khẩu vào Đăng nhập → Xác minh email, nhập email và mật khẩu hiện tại, yêu cầu mã rồi nhập mã trong thư. Chỉ có cả mật khẩu hiện tại và quyền truy cập hộp thư mới xác minh được.
- Tài khoản chưa xác minh không nhận recovery. Nếu đã quên mật khẩu trước khi xác minh, luồng tự phục vụ này không thể chứng minh chủ tài khoản; cần hỗ trợ xác minh độc lập, không tự mở ngoại lệ hoặc sửa DB.
- Forgot luôn trả cùng thông báo với email tồn tại/không tồn tại/chưa xác minh. Rate limit vẫn có thể trả429; validation trả400. HTTP200 chỉ xác nhận yêu cầu, **không xác nhận thư đã được giao**.
- Mật khẩu tạm: SecureRandom32byte/Base64URL43ký tự, BCrypt hash,15phút. Mật khẩu thật và phiên cũ vẫn dùng được đến lúc reset hoàn tất. Yêu cầu mới thay thế credential recovery trước đó.
- Đăng nhập bằng mật khẩu tạm chỉ cấp opaque reset token một lần, không JWT/refresh. Server không chấp nhận token này ở các API nghiệp vụ. Reset session hết hạn cùng mật khẩu tạm; nếu app mất process sau khi tiêu thụ mật khẩu tạm, yêu cầu mật khẩu tạm mới.
- Reset giữ khóa user, xác minh token + mật khẩu tạm, đổi BCrypt, tăng `security_version`, vô hiệu toàn bộ credential recovery và revoke mọi refresh trong **một giao dịch**. Login/refresh cùng khóa user trước khi cấp token, tránh race với reset.
- JWT được đối chiếu `sv` với DB ở mỗi request, không cache. JWT cũ thiếu `sv` tương đương0, chỉ được dùng khi user vẫn version0. Sau reset, mọi JWT đã cấp trước đó bị401 ngay ở request mới; request đã được xác thực/đang chạy không bị hủy ngược thời gian. Logout thông thường vẫn giữ chính sách cũ: revoke refresh hiện tại, không tăng version.
- Android giữ reset token trong bộ nhớ `ResetSessionVault`, không Intent/Bundle/preferences/full session. Process death làm mất reset session. Gửi email thông báo sau commit, không chứa password; lỗi thông báo không rollback mật khẩu đã đổi và được log an toàn.

## API

Tất cả POST JSON, response no-store, cùng error envelope hiện có. Không đưa credential vào URL/query/log.

| Endpoint | Body | Kết quả |
|---|---|---|
| `/api/auth/verification/request` | email,password hiện tại |200 notice;401 sai credential;503 SMTP lỗi |
| `/api/auth/verification/confirm` | email,password hiện tại,code |200;401 sai/hết hạn/đã dùng |
| `/api/auth/forgot-password` | email |200 thông báo chung; email xử lý qua hàng đợi bounded |
| `/api/auth/login` | email,password | Giữ nguyên cặp token với mật khẩu thật; mật khẩu tạm trả `resetRequired=true,resetToken,expiresIn` và không có access/refresh |
| `/api/auth/reset-password` | resetToken,temporaryPassword,newPassword,confirmation |200 rồi đăng nhập lại;401 credential sai/hết hạn/replay;400 policy/mismatch/trùng mật khẩu |

Recovery endpoint dùng limiter riêng:5request/email/60giây,20request/IP/60giây, tối đa10.000key/JVM; verify request/confirm/forgot dùng chung quota account. Reset giới hạn IP và hash reset token. Login tiếp tục limiter hiện có. Không tin XFF tùy ý; cấu hình trusted proxy hiện hành vẫn áp dụng. Multi-instance cần quota chia sẻ hoặc giới hạn ở proxy; chưa thêm Redis.

Hàng đợi recovery trong bộ nhớ tối đa100,1–2worker. Restart có thể mất yêu cầu chưa xử lý; yêu cầu lại sau cooldown. Hàng đợi không lưu plaintext password; SMTP lỗi rollback hash mới. Không có outbox retry lưu plaintext secret. Thông báo đổi mật khẩu chưa có durable retry nếu SMTP lỗi.

## Migration và tương thích

`V3__account_recovery.sql`: thêm `users.email_verified_at`, `users.security_version` mặc định0 và bảng `account_recovery` tối đa một dòng/user. V1/V2 không sửa. Chỉ chạy trên PostgreSQL test trong phiên phát triển; không truy cập DB production/deploy. Khi vận hành cần backup và đưa code server mới qua quy trình deploy được duyệt. Không rollback về JAR bỏ qua version-check sau khi đã reset, vì JAR cũ có thể chấp nhận JWT đã bị thu hồi.

## Gmail SMTP — cấu hình thủ công

1. Bật xác minh2bước cho tài khoản gửi thư; tạo Gmail App Password nếu tài khoản/chính sách cho phép. Không dùng mật khẩu đăng nhập Gmail thường.
2. Đặt biến môi trường ở tiến trình backend hoặc file local được ignore: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME=<sender>`, `MAIL_PASSWORD=<app-password>`, `MAIL_FROM=<sender>`.
3. Không gửi giá trị bí mật vào chat, log hay Git. `.env` không được Spring Boot tự đọc mặc định; phải nạp vào environment trước chạy. Với systemd dùng EnvironmentFile hiện có; Docker Compose cần truyền các biến vào container.
4. Adapter bắt buộc STARTTLS, kiểm tra hostname chứng chỉ, auth, timeout connect/read/write5giây, mail debug tắt. Thiếu cấu hình: không gửi giả, verification trả503, recovery job log thất bại an toàn và client vẫn nhận thông báo chung.
5. Nếu App Password bị tổ chức chặn, dùng SMTP provider khác có STARTTLS587 và quyền gửi phù hợp; cần chủ tài khoản tự cấu hình, không tự tạo tài khoản/đổi credentials.
6. Nghiệm thu thật bằng tài khoản QA riêng và inbox thuộc người kiểm thử: xác minh → nhận mật khẩu tạm → reset → nhận thông báo. SMTP accept không chứng minh vào inbox; phải kiểm tra thư thực tế, kể cả Spam. Không dùng tài khoản cá nhân để tự reset.

Mẫu tên biến tại [deploy/.env.example](../deploy/.env.example). Hướng dẫn thư viện: [Spring Boot Mail3.5](https://docs.spring.io/spring-boot/3.5/reference/io/email.html); các nguyên tắc token một lần, chống dò account và vô hiệu phiên theo [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).

## Kiểm thử và giới hạn hiện tại

### Nghiệm thu USB và Gmail tiếp nối19/10/2026

Người dùng cung cấp đường dẫn SMTP local ignored, cắm/mở khóa điện thoại; đã cài APK/test APK giữ nguyên dữ liệu/API origin. Batch đầu9test có1FAIL thật: initial LiveData idle ghi đè notice hết hạn, dù submit vẫn disable. Sửa renderer `RecoveryFormActivity` ưu tiên expired state khi không có reset session, không sửa test để bỏ lỗi. Thêm IME USB và unit network-disconnect→retry.

Bản cuối `recovery-usb-fix-build.log`: **79/79 JVM,0fail/error; assemble debug/test/release + lint debug/release PASS**. Batch cuối `.local/artifacts/recovery-usb-retest.log`: **10/10 USB PASS32.942giây**, gồm6Recovery và4Login; validation/password-eye/loading/expired/confirmation/IME, layout hẹp/ngang/font1.5/light-dark. Năm screenshot đã pull/xem tại `.local/artifacts/recovery-usb-screenshots/`; không chụp credential thật. Không đăng xuất hoặc đổi mật khẩu tài khoản cá nhân; không dùng emulator.

Gmail thật **chưa PASS**. Lệnh gửi ban đầu bị auto-review chặn; chỉ chạy sau người dùng xác nhận đích nhận ba thư QA. Test `GmailRecoveryAcceptanceIT` dùng DB nhúng riêng và SMTP adapter thật: verification/request trả503 ở thư đầu,0thư được xác nhận SMTP accept. Probe không gửi mail `smtpConnectionOnly` FAIL với tên loại lỗi an toàn `AuthenticationFailedException`. Host/cổng phù hợp, không log SMTP response/password. Người dùng xác nhận chưa có App Password và cần hướng dẫn. **BLOCKED chỉ phần Gmail auth/inbox**, không còn blocker USB. Không tự sửa credentials hoặc lặp gửi thư khi chưa cập nhật file.

Helper [Test-AccountRecoveryGmail.ps1](../scripts/Test-AccountRecoveryGmail.ps1) đọc năm khóa từ file được ignore, không dot-source/evaluate nội dung, phục hồi env sau chạy; `Connection` không gửi thư, `Acceptance` bắt buộc recipient được duyệt và gửi3thư. `GmailRecoveryAcceptanceIT` không chạy trong suite mặc định (*IT), chỉ opt-in; credential QA lấy trong memory của outgoing mail sau SMTP thành công để gọi API, không dùng IMAP/đọc inbox. Cần người dùng xác nhận nhận đủ3thư thật trước khi nghiệm thu delivery. Hai phép thử thật đã chạy hiện đều FAIL đúng nguyên nhân SMTP; không tính thay cho151test regression đã PASS ở lượt trước.

Hướng dẫn chính thức: bật [Xác minh2bước](https://support.google.com/accounts/answer/185839?hl=vi), tạo [App Password](https://myaccount.google.com/apppasswords) cho đúng tài khoản gửi, đặt riêng trong MAIL_PASSWORD local. Không dùng mật khẩu Gmail thường. Nếu không có lựa chọn, kiểm tra tài khoản tổ chức/security-key-only/Advanced Protection theo [Google Help](https://support.google.com/accounts/answer/185833?hl=vi); không tắt2FA để chữa SMTP.

Các bullet dưới ghi bằng chứng lượt trước; kết luận về USB/JVM đã được mục này thay thế.

- Backend lượt đầu45/45; toàn bộ suite145/145, bản cuối **151/151,0failures/errors/skipped + JAR**. Recovery13test gồm race login/refresh/reset, policy và legacy JWT; SMTP2test. Log `.local/artifacts/recovery-backend-final.log`.
- Android bản hardening cuối **78/78JVM,0failures/errors/skipped; assemble debug/test/release và lint PASS**, log `.local/artifacts/recovery-android-reviewed-build.log` (52giây). Lint0errors,20debug/29release warnings (gồm unused và cảnh báo cũ, không suppress/baseline). UI USB chưa nghiệm thu: điện thoại khóa/Dozing rồi mất kết nối, cài APK trảdevice-not-found. Có5test UI-only mới và4Login regression, **chưa chạy** trên bản mới; không gửi email/reset/logout tài khoản cá nhân, không có ảnh QA mới.
- `RecoveryIntegrationTest` dùng HTTP thật + PostgreSQL thật nhúng, **AccountMail fixture trong bộ nhớ**, không Gmail thật. `SmtpAccountMailTest` kiểm tra thiếu config và SMTP loopback không hỗ trợ STARTTLS phải bị từ chối trước AUTH/DATA. Không coi fixture là giao thư thật.
- Chưa nghiệm thu Gmail thật vì chưa có cấu hình SMTP trong environment. Chưa deploy VPS, chưa chạy migration production; không tuyên bố DONE/PASS toàn bộ A cho đến khi đóng các cổng còn thiếu.

Artifacts local ignored: `.local/artifacts/recovery-*.log`; Maven XML tại `server/target/surefire-reports`, Android XML tại `android-client/app/build/test-results/testDebugUnitTest`. Các lỗi tooling đầu: Maven Wrapper trong sandbox không khởi động được; Maven portable cùng phiên bản được dùng. Compile đầu phát hiện getter JWT không tồn tại, đã sửa bằng đọc claim Number có kiểm tra kiểu, không suppress test.

## File thay đổi thuộc tính năng A

Backend: `pom.xml`; auth `AccountMail`, `SmtpAccountMail`, `RecoveryRequests`, `RecoveryCredentials`, `RecoveryCredentialsRepository`, `RecoveryService`, `RecoveryDispatcher`, `RecoveryController` (mới); `AuthController`, `SessionService`, `TokenService`, `RefreshTokenRepository`; config `TokenConfiguration`, `SecurityConfig`; `user/User`, `UserRepository`; `common/ApiExceptionHandler`; migrationV3. Test mới `RecoveryIntegrationTest`, `SmtpAccountMailTest`; cập nhật `ServerBootstrapTest`, `TokenConfigurationTest` cho schema/constructor mới.

Android: model `RecoveryModels` mới và `AuthModels`; `AuthApi`; repository `AccountRecoveryRepository`, `PasswordResetRequired` mới và `AuthRepository`; session `ResetSessionVault` mới và `SessionManager`; `RecoveryViewModel` mới và `AuthViewModel`; UI `RecoveryFormActivity`, `ForgotPasswordActivity`, `EmailVerificationActivity`, `ChangePasswordActivity` mới và `LoginActivity`; manifest; `activity_recovery_form.xml`, `recovery_strings.xml` mới và `activity_login.xml`. Test mới `AccountRecoveryRepositoryTest`, `AccountRecoveryUiTest`; cập nhật `LoginReferenceTest` vì link recovery nay đã hoạt động.

Docs/config: tài liệu này, `AUTH_API.md`, `PROJECT_DECISIONS.md`, `PROJECT_STATUS.md`, `TASKS.md`, `SESSION_HANDOFF.md`, `deploy/.env.example`. Không nhận các thay đổi UI cũ có sẵn là thay đổi mới của recovery; không sửa Camera/Friends/History/Profile/backend media/FCM/widget hoặc validation đăng ký.
