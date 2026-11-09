# QA backend/VPS — 08/11/2026

Hồ sơ này ghi nhận đợt khôi phục mạng/SMTP ban đầu. Chính sách recovery yêu
cầu email verified trước forgot trong các kết quả lịch sử bên dưới đã được
người dùng thay đổi và triển khai sau đó; trạng thái hiện hành tại
[QA_MAIL_RECOVERY_20261108.md](QA_MAIL_RECOVERY_20261108.md).

## Kết quả sau khôi phục theo panel người dùng cung cấp

Người dùng tiếp tục yêu cầu tự sửa và cung cấp ảnh panel: đúng endpoint SSH,
hostname và cảnh báo VPS chưa có Internet, hướng dẫn chạy `changeip`.
Đã tiếp nhận xác nhận endpoint này, pin đúng fingerprint mới khi kết nối và
đối chiếu hostname đăng nhập `web-01kl2jvk` khớp panel. Đây không phải xác minh
fingerprint độc lập qua console nhà cung cấp; các giới hạn đợt đầu bên dưới
được giữ làm lịch sử.

### Nguyên nhân và thay đổi VPS

1. Spring Boot, PostgreSQL, Nginx vẫn chạy; health loopback trả 200. Default
   route đi nhầm qua mạng quản trị VLAN 50, không có Internet. Đã sao lưu
   netplan/routes/rules rồi chạy `changeip`, chọn lại VLAN 209 đang lưu.
   Default route về gateway DHCP 10.203.0.1, giữ SSH policy routing riêng.
2. Script boot `/usr/local/bin/changip-apply` thiếu bước đặt default route
   như bản interactive. Đã sao lưu và bổ sung đọc router từ lease DHCP đúng
   interface, `ip route replace default` sau khi có IP. `bash -n` PASS;
   chạy riêng block route trên interface hiện hành PASS. Không reboot hoặc
   restart toàn bộ network để nghiệm thu boot; gate này còn mở.
3. Interface tạo lại mất DNS; resolver vẫn chọn mạng quản trị. Đã cấu hình
   DNS trên interface Internet và thêm
   `/etc/systemd/resolved.conf.d/60-taktak-internet.conf` mới với DNS
   1.1.1.1/8.8.8.8 và routing domain `~.`, restart systemd-resolved.
   Sau đó resolve smtp.gmail.com đạt, HTTPS Google trả 200; kết nối TCP
   Internet cũng đã kiểm tra đạt. Không thay `/etc/resolv.conf` bằng file tĩnh.
4. Tiến trình backend thiếu cả năm biến MAIL_*. Cấu hình local được người dùng
   cung cấp trước đây đã được kiểm tra SMTP từ VPS trước khi ghi vào
   `/var/taktak/.env` quyền root/600, giữ nguyên các biến khác và có backup.
   Restart taktak.service một lần để nạp SMTP; health nội bộ/public 200,
   NRestarts=0 sau restart. Không thay JAR, schema, JWT secret hoặc dữ liệu.
5. Tunnel tự khôi phục sau khi có Internet và cấp origin mới
   `https://vic-extent-metres-verde.trycloudflare.com/`.
   Đã cập nhật riêng `TAKTAK_DEBUG_BASE_URL` trong Gradle; release không đổi.
   Quick Tunnel có thể đổi URL sau lần restart, chưa có tên miền cố định.

Backup quyền root nằm tại `/root/taktak-network-repair-20261108/`, gồm netplan,
routes/rules, `env-before-smtp`, `changip-apply-before`. Nếu cần rollback script,
khôi phục đúng bản này; không rollback database hoặc thay security_version.
Helper chẩn đoán/cấu hình chỉ nằm trong `.local/` được ignore, không chứa
credential hard-code. Nội dung MAIL_* không đưa vào output hoặc tài liệu.

### Kiểm tra sau sửa

- SMTP probe từ VPS trước cấu hình và probe bằng môi trường tiến trình thật
  sau restart: **PASS STARTTLS + authentication**, **EMAILS_SENT=0**.
- Tunnel từ máy local: health 200; GET `/api/users/me` không JWT 401;
  POST JSON rỗng tới verification/request và forgot-password đều 400.
  Không gọi recovery bằng credential thật hoặc gửi email trong lượt này.
- Truy vấn chỉ đọc đúng tài khoản người dùng báo lỗi: tồn tại, chưa xác minh
  email. Vì vậy forgot-password không gửi thư theo policy; cần verification
  với mật khẩu hiện tại trước. Không tự đổi trạng thái verified/reset account.
- Android assembleDebug + testDebugUnitTest: **BUILD SUCCESSFUL**, exit 0,
  135 tests, 0 failures/errors. Ban đầu JRE VS Code thiếu jlink; chạy lại với
  `-Porg.gradle.java.installations.auto-detect=false`
  `-Porg.gradle.java.installations.paths=C:/Program Files/Java/jdk-25.0.2`
  và `--no-daemon`, không đổi stack hoặc bỏ qua test.
- APK bàn giao `.local/artifacts/apk/Taktak-network-fixed-20261108.apk`,
  20.068.298 byte; SHA-256
  `3784034D8A16B5EE5930314414CC75BCF7187263BBC8431B7CEA325C93262A53`.
  ADB không có thiết bị: chưa cài APK/QA UI trên máy thật.

**PASS** phần khôi phục mạng, backend và SMTP authentication đã kiểm tra.
**PARTIAL** nghiệm thu tổng thể: chưa thư thực tế/Inbox, chưa reboot test,
chưa cài APK mới lên điện thoại. Không commit/push; bảo toàn Camera XML và
các file báo cáo có sẵn.

## Lịch sử kiểm tra trước khi người dùng cung cấp panel

## Phạm vi

Đợt đầu kiểm tra lỗi kết nối backend và phản ánh không nhận được email xác minh/khôi phục.
Không sử dụng mật khẩu đăng nhập khi danh tính máy chủ chưa được xác minh.
Không sửa dữ liệu tài khoản, gửi email, restart hoặc deploy trong lượt kiểm tra này.

## Bằng chứng thực tế

- `android-client/gradle.properties` đang dùng debug origin
  `https://roommate-sentence-chemical-tanks.trycloudflare.com/`.
- `curl` tới `/actuator/health` thất bại trước HTTP: mã thoát 6,
  `Could not resolve host`, HTTP 000. Google DNS 8.8.8.8 và Cloudflare DNS
  1.1.1.1 đều trả `DNS_ERROR_RCODE_NAME_ERROR` cho hostname này.
  Đây là lỗi origin hiện cấu hình; chưa chứng minh tiến trình Spring Boot bị dừng.
- Cổng TCP 11918 tại IP đã ghi nhận trước đây vẫn mở.
- Đã đọc public SSH host key bằng Paramiko, không gửi credential. Cả tám IP DNS
  hiện tại của `remote.io.vn`, bao gồm IP triển khai lịch sử 14.250.250.68,
  đều trả fingerprint ED25519:
  `SHA256:n/rVJEoXUxJaHrT6vY7Mo+jFtrgNofEgB7pINSZ4ALo`.
- Fingerprint lịch sử trong hội thoại là
  `SHA256:kb0mNotqTiCWpME9WvLHX9QuRZyVnNAF2qKyJ7kzc7w`.
  Không có bản ghi SSH trusted tại thư mục `.ssh` đã kiểm tra để giải quyết
  khác biệt. Tám IP cùng khóa chỉ xác nhận sự nhất quán của endpoint hiện tại,
  không phải bằng chứng độc lập rằng đó là VPS Taktak.
- `ssh-keyscan` Windows gặp lỗi thuật toán KEX; đã dùng Paramiko để đọc key,
  không tắt xác minh danh tính hoặc chấp nhận key mới để đăng nhập.

## Đối chiếu recovery trong mã nguồn

- `/verification/request` cần email và mật khẩu hiện tại; tài khoản đã xác minh
  sẽ không gửi mã mới.
- `/forgot-password` trả notice chung; tài khoản không tồn tại hoặc chưa xác minh
  không được gửi mật khẩu tạm. Lỗi SMTP ở worker có thể xảy ra sau HTTP 200.
- SMTP sử dụng `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
  `MAIL_FROM`; STARTTLS bắt buộc và timeout 5 giây. Health UP không xác nhận
  gửi email thành công.
- Chưa đọc được cấu hình runtime, journal, schema hoặc trạng thái tài khoản
  trên VPS. Không kết luận SMTP sai hay tài khoản cụ thể chưa xác minh.

## Kiểm thử local

Maven offline `verify`, Java 17, PostgreSQL nhúng riêng: **PASS**.
191 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`, exit code 0,
thời gian 02:01. JAR được đóng gói thành công. Gồm recovery integration và
kiểm tra SMTP fixture; không phải nghiệm thu giao thư Gmail/VPS thật.
Log local được Git ignore: `.local/artifacts/backend-qa-20261108-verify.log`.
Lượt đầu bị PowerShell `ErrorActionPreference=Stop` coi warning stderr JVM
là lỗi lệnh; đã chạy lại toàn bộ với `Continue`, giữ nguyên mọi test và kiểm
tra exit code Maven. Không dùng kết quả lượt đầu để tuyên bố build PASS.
`git diff --check` đã chạy, không có lỗi whitespace.

## Điều kiện tiếp tục

Cần fingerprint SSH lấy qua console nhà cung cấp hoặc SSH host key đã tin cậy
để xác minh máy chủ. Sau khi xác minh: đọc status/journal và sự hiện diện của
MAIL_* trong môi trường tiến trình (không in giá trị bí mật), xác định origin
HTTPS hiện hành rồi mới sửa cấu hình đã xác nhận sai. Không thay URL bằng một
hostname tự đoán; không gửi credential vào endpoint chưa xác minh.

Trạng thái vận hành: **BLOCKED** do danh tính SSH chưa xác minh; debug origin
hiện tại **FAIL DNS**. Chưa thay đổi source, cấu hình runtime hoặc database.
