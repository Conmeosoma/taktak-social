# Sửa luồng email/recovery — 08/11/2026

## Phạm vi và quyết định

Người dùng yêu cầu sửa luồng Gmail và duyệt cho tài khoản chưa xác minh được
nhận credential khôi phục; chỉ sau reset hợp lệ mới xác minh email. Đã áp dụng
skills auth-implementation-patterns và debugging-strategies. Đối chiếu
[OWASP Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html):
response chung, credential ngẫu nhiên/hash-only/một lần/có hạn, không tự login
sau reset và vô hiệu phiên cũ. Đây là thay đổi chính sách được duyệt, không
backfill trạng thái verified hay reset tài khoản cá nhân trong database VPS.

## Thay đổi

- RecoveryService: issueTemporary hỗ trợ mọi email đã đăng ký; loginTemporary
  chỉ kiểm tra credential hợp lệ, không yêu cầu verified từ trước. Reset giữ
  user lock, xác minh credential, đổi BCrypt, đánh dấu email verified nếu cần,
  tăng security_version, revoke refresh và xóa credential trong cùng transaction.
- Không cấp JWT/refresh ở bước đăng nhập tạm. TTL15phút, SecureRandom32byte,
  credential43ký tự, quota và response chống dò account giữ nguyên.
- SmtpAccountMail: loại whitespace thừa quanh host/username/from; chuẩn hóa
  nhóm App Password chỉ cho smtp.gmail.com, giữ nguyên password provider khác.
  Cổng ngoài1–65535 bị từ chối trước kết nối. Giữ STARTTLS bắt buộc, hostname
  verification và timeout5giây; thiếu config có cảnh báo không chứa giá trị.
- RecoveryDispatcher và thông báo sau reset: log nhóm lỗi hữu hạn
  authentication/smtp/processing, không recipient/body/token/exception message.
- Android: sửa hướng dẫn recovery/xác minh/Spam/gửi lại/mật khẩu tạm cho vi,
  en,es,hi,ja,ko,zh. Giữ API, Repository/ViewModel và layout hiện hành.
- Không thay schema hoặc migration, không dependency mới. Không sửa Camera
  XML hay file báo cáo có sẵn của người dùng. Không tự commit/push/deploy.

## Kiểm tra đã chạy

- Targeted: RecoveryIntegrationTest16 + SmtpAccountMailTest4 = **20/20 PASS**,
  failures/errors/skipped0, BUILD SUCCESS. Kiểm tra unverified reset, verify
  chỉ sau reset, JWT/refresh cũ401, token giới hạn, resend invalidation,
  wrong/expired/replay, rollback SMTP, race và cấu hình Gmail/port/STARTTLS.
- Gmail thật: người dùng cho phép đúng3thưQA tới hộp thư được chỉ định.
  GmailRecoveryAcceptanceIT1/1 PASS31,11s, BUILD SUCCESS. Gmail SMTP chấp nhận
  3 thư (verification,temporary,changed); tài khoản QA trong PostgreSQL nhúng
  riêng được cố ý để unverified, reset thành công và credential/phiên cũ bị từ
  chối. Không thay tài khoản production, không đọc inbox hoặc log credential.
  Người dùng đã xác nhận nhận đủ3thưQA trong hộp thư. Gate giao thư QA đã đạt.
- Android assembleDebug/testDebugUnitTest/lintDebug: BUILD SUCCESSFUL36s,
  exit0; lint0errors/84warnings. Unit task UP-TO-DATE với135 kết quả PASS đã
  xác minh trước đó, không gọi là135ca vừa chạy lại. Dùng full JDK25 local
  qua Gradle project property để tránh JRE VS Code thiếujlink.
- Full backend verify bản cuối: **196/196 PASS**, 0 failures/errors/skipped,
  BUILD SUCCESS, exit0, thời gian02:11. JAR đóng gói thành công; không chạy
  Gmail opt-in trong suite mặc định, không gửi thêm thư.
- `git diff --check` không có lỗi whitespace; migration không có diff.

Log Git-ignore: `.local/artifacts/mail-fix-targeted-20261108.log`,
`.local/artifacts/recovery-gmail-acceptance.log`,
`.local/artifacts/mail-fix-android-20261108.log`,
`.local/artifacts/mail-fix-backend-final-20261108.log`.

## Giới hạn và bàn giao

Gửi qua SMTP không bảo đảm inbox/delivery tức thời. Hàng đợi recovery bounded
trong RAM vẫn có thể mất yêu cầu khi process dừng; người dùng gửi lại sau
cooldown. Không tạo durable outbox chứa plaintext credential. Thông báo đổi
mật khẩu gửi sau commit; lỗi mail không hoàn tác reset và thu hồi phiên.

APK mới: `.local/artifacts/apk/Taktak-mail-fixed-20261108.apk`; khác APK sửa
mạng trước đó, bổ sung hướng dẫn recovery mới. Chưa cài lên máy thật.

## Triển khai VPS đã được duyệt và thực hiện

Người dùng xác nhận “Cho phép deploy bản sửa email lên VPS”. Đã pin SSH key,
đối chiếu hostname và checksum artifact trước/sau upload, sao lưu JAR cũ,
thay atomic và restart taktak.service một lần. Script có rollback JAR nếu
restart/health fail; thực tế health đạt nên không cần rollback.

- SHA-256 JAR local và VPS:
  `673fd99ca499849fc2e9f91e5217d55100ae6072d6b90880d6da06839c89fbb0`.
- Backup JAR: `/var/taktak/taktak-server.before-mail-fix-1794154802.jar`.
- Kiểm tra sau deploy: backend/PostgreSQL/Nginx/quick tunnel đều active,
  `NRestarts=0`; health loopback và HTTPS200 UP, `/api/users/me` và
  `/actuator/env` không JWT401; login/verification/request/forgot/reset với
  JSON rỗng400. Không thử gửi mail bằng tài khoản production.
- Cả5 biến MAIL_* có trong môi trường process mới; journal2giờ không có các
  marker failure recovery/authentication/send được kiểm tra. Truy vấn chỉ đọc
  xác nhận tài khoản người dùng báo vẫn unverified và chưa có recovery record;
  không tự đổi trạng thái tài khoản khi deploy.
- Không có migration mới, không reset/restore DB, không đổi JWT secret,
  không commit/push. Chưa QA UI trên điện thoại trong lượt này.

Trạng thái **PASS phần code, Gmail QA và triển khai đã kiểm tra**. Người dùng
có thể chọn Quên mật khẩu trên app đang dùng origin HTTPS mới để gửi yêu cầu
cho tài khoản thật; credential43ký tự nhận trong thư được dùng tại màn Đăng nhập
để đặt mật khẩu mới. Không tuyên bố đã gửi/nhận recovery production khi người
dùng chưa thực hiện thao tác này. Quick Tunnel vẫn có thể đổi URL sau restart.
