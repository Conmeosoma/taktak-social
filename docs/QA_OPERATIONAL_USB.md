# HARDENING-01 — Nghiệm thu vận hành qua USB

> Cập nhật 21/10/2026: các kết quả bên dưới là lượt trước. SSH đã vào được,
> FCM đã bật theo duyệt, USB FCM/Worker/AppWidgetHost thật1/1 đạt. Nghiệm thu
> tổng thể vẫn FAIL do lint và restore làm security_version9→7 trên DB QA riêng.
> Xem [báo cáo mới nhất](QA_ACCEPTANCE_ALL.md), gồm giới hạn ảnh logout/launcher.

Ngày 21/10/2026, điện thoại Xiaomi 23129RAA4G Android15,1080×2400.
Skill android_ui_verification dùng để đối chiếu UI, chạy harness và review ảnh;
không dùng emulator trong lượt này. Các thay đổi local/untracked trước đó được giữ.

## Phạm vi và quyền thao tác

Người dùng duyệt kiểm thử USB, tạo tài khoản QA và chuyển khỏi tài khoản đang
đăng nhập để thử FCM/widget. Không deploy, sửa cấu hình VPS, reset database,
khôi phục backup hoặc đụng ảnh/tin cá nhân. Đăng xuất không xóa tài khoản;
người dùng cần đăng nhập lại sau QA. Credential QA được tạo ngẫu nhiên trong
RAM, không đưa vào log, Git hoặc báo cáo.

## Kết quả đã xác minh

- Debug/test build cuối cho USB đạt13s, giữ nguyên HTTPS origin của APK đang
  cài. Hai lần build đầu lỗi vì Gradle chọn JRE VS Code thiếu jlink; dùng
  `-Porg.gradle.java.installations.paths=<Android Studio JBR>` và
  `-Porg.gradle.java.installations.auto-detect=false`, không sửa toolchain file.
- Cache/reply USB cuối:9/9 PASS,34,856s. Lượt đầu8/9, cache revoke thất bại
  trên APK cũ; đối chiếu DEX phát hiện classes7/9 khác bản build hardening.
  Cập nhật app bằng `adb install -r`, cùng applicationId/origin, rồi chạy lại.
- File phiên mã hóa trước/sau update và các ca read-only giống nhau. Chỉ
  sau đó mới đăng xuất theo quyền người dùng vừa duyệt.
- GET HTTPS thật từ điện thoại:3/3 PASS,3,522s. Health JSON UP/200; request ID
  UUID mới, không tin ID client; API ảnh không JWT trả401; metrics/env/configprops
  không public. Không dùng AuthGraph/token cá nhân trong các GET này.
- Đã pull và review ảnh fixture reply, ảnh bị thu hồi không hiển thị lại;
  không dùng ảnh fixture làm bằng chứng FCM hoặc backend thực.

## Lượt FCM/widget thực

`RealFcmWidgetAcceptanceTest` tạo cặp QA, kết bạn, lấy FCM token SDK thật,
đăng ký thiết bị, upload JPEG màu và chờ PushEventBus từ MessagingService.
Không tự emit push hoặc gọi Worker để giả delivery. Test gồm foreground,
background khi launcher ở foreground, xóa hai ảnh QA và logout.

- Lượt đầu không bind được QA WidgetHost khi dùng `--user current`;
  `--user 0` được xác nhận qua dumpsys grant. Quyền bind được thu hồi sau test.
- Lượt tiếp theo runner bị dừng vì chưa hoàn tất; không tính PASS. Bổ sung
  timeout300s và phase marker hữu hạn, không ID/token/nội dung riêng tư.
- Marker xác định bị chặn ở launch_host trên MIUI. Thay launch USB bằng
  `am start -W` như harness USB đã được kiểm chứng; giữ assertion thật.
- Harness cuối build đạt18s, chỉ sửa androidTest, không sửa code production.
- Lượt cuối `real-fcm-reviewed.log`: **FAIL1/1**,100,628s. Đã qua tạo tài khoản,
  đăng ký token SDK, bind host, mở Main foreground và upload ảnh; timeout90s
  ở assertion `Real FCM created event delivered`. Chưa chạy được các assertion
  widget/background/delete/logout do không qua gate delivery đầu tiên.
- Finally của lượt này đã chạy logout QA và thu hồi grant bind. Một ảnh màu
  vừa upload có thể còn backend, tài khoản QA không bị xóa; không dọn theo
  prefix hoặc xóa dữ liệu không rõ ID. Bổ sung cleanup theo chính danh sách
  momentId test vừa tạo cho các lượt sau; đã build, **chưa chạy lại runtime**.
  Build sau bổ sung cleanup đạt25s (`fcm-cleanup-build.log`).

Trạng thái tổng: **IN PROGRESS / BLOCKED** để điều tra server, không phải
production PASS. USB cache/reply9/9 và HTTPS3/3 đạt; FCM thật không đạt gate.

QA WidgetHost là AppWidgetHost thật nhận RemoteViews, không phải bằng chứng đã
thêm widget2×2 vào launcher MIUI của người dùng. Không chứng minh OEM/force-stop
luôn cập nhật nền hoặc exactly-once delivery.

## Giới hạn vận hành

PROJECT_STATUS/SESSION_HANDOFF hiện ghi VPS đã lên V6, khác snapshot báo cáo
hardening local trước đó. Giữ nguyên ghi nhận, không coi là bằng chứng đã kiểm
tra lại. HTTPS origin trên APK là tunnel; health/request ID không chứng minh
đích tunnel là đúng VPS hoặc checksum JAR/schema V6.
SSH theo DNS remote.io.vn bị connection refused. Thử IP14.250.250.68 bằng
HostKeyAlias của host đã tin cậy giữ strict checking: host key được chấp nhận,
nhưng `Permission denied (publickey,password)`. Chưa có phiên xác thực để đọc
outbox/FCM log; không bỏ kiểm tra host key hoặc dùng mật khẩu từng xuất hiện
trong hội thoại. Không kết luận FCM_DISABLED, token sai, OEM hay queue lỗi khi
chưa có bằng chứng. Cần đường dẫn SSH key local có quyền đọc môi trường này
hoặc phối hợp vận hành để kiểm tra; không gửi private key vào chat.

Chưa chạy restore DB/ảnh thực, crash/restart process, rollback binary,
mixed-version APK, load dài hạn hoặc routing cảnh báo. Không chạy load và
restore vào môi trường có dữ liệu cá nhân chỉ để đủ checklist.

## Bằng chứng local (Git ignore)

`.local/artifacts/hardening-usb/`: build-jdk-fixed.log, instrumentation.log
(8/9), instrumentation-final.log(9/9), connectivity-build.log,
connectivity.log(3/3), fcm-harness-build-final.log, real-fcm*.log và reply-images/.
APK cũ giữ ở installed-before.apk, không ghi vào Git.

File thêm/sửa lượt này: OperationalConnectivityTest.java,
RealFcmWidgetAcceptanceTest.java và tài liệu QA/trạng thái/bàn giao.
Không commit/push/deploy.
