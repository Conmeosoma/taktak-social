# HARDENING-01 — Bằng chứng và bàn giao

Ngày: 21/10/2026. Phạm vi người dùng duyệt: reliability, compatibility,
observability, đo tải/tối ưu, privacy và thực hành dẫn dắt kỹ thuật. Bắt đầu
ở main/ebbe12b, worktree sạch. Không đổi stack/định danh, không deploy hoặc
migrate database có sẵn, không gửi SMTP/FCM thật, không commit/push.

## Thay đổi

- Server `outbox/`, V6 và MomentService/ChatService: event cùng transaction
  nghiệp vụ; dispatcher claim/lease/fencing, retry/backoff, DEAD/CANCELLED,
  tra registration và quyền hiện tại. Firebase sender trả kết quả rõ ràng.
- Server `observability/`, application.yml: request ID do server sinh,
  metric hữu hạn, cảnh báo log 5xx/pool/backlog, không lộ raw path/query/body.
  Actuator vẫn chỉ health, chưa có exporter/alert routing production.
- ChatRepository/ChatService: batch inbox peer/lastMessage/unread thay N+1,
  giữ pagination, block và redaction ảnh.
- Android HistoryImageCache/SessionImageCache, HistoryViewModel,
  MomentViewerActivity và MomentRepository: TTL5phút/LRU giới hạn RAM,
  generation phiên; quyền server được kiểm tra trước mỗi lookup cache.
  Không cấp quyền offline từ bitmap cũ; guard publication/logout được giữ.
- Test: SystemLoadAcceptanceIT, SchemaUpgradeCompatibilityTest,
  observability tests, cache JVM/UI tests; mở rộng ChatIntegrationTest,
  MomentPushIntegrationTest và cập nhật các harness gọi cache cũ.
- Hai template deploy/nginx: access log whitelist; error log tắt vì có thể
  kèm URI/query. Đánh đổi mất chẩn đoán TLS/proxy, cần gate staging.
- Tài liệu mới: RELIABILITY_OUTBOX, CLIENT_SERVER_COMPATIBILITY,
  OBSERVABILITY, PERFORMANCE, PRIVACY_AND_RETENTION, ENGINEERING_PLAYBOOK;
  cập nhật PROJECT_DECISIONS, PROJECT_STATUS, TASKS, SESSION_HANDOFF và README.

## Kiểm tra thực tế

| Kiểm tra | Kết quả | Bằng chứng local ignored |
|---|---|---|
| Backend `mvn -o verify` cuối sau review | PASS187/187; build1:59 | hardening-server-verify-reviewed.log |
| Backend verify các snapshot trước | PASS185/185 và186/186 | hardening-server-verify.log; hardening-server-verify-final.log |
| Upgrade V5→V6 + load/query-budget | PASS2/2; build27,679s | hardening-compatibility-budget.log |
| Android debug + test APK + JVM | PASS127/127; build27s | hardening-android-build-final.log |
| Android release unsigned, HTTPS QA placeholder | PASS; build23s | hardening-android-release-final.log |
| AVD API37,17 selected privacy/reply/chat/history/viewer tests | PASS17/17;145,302s | hardening-avd-ui.log |
| Deployment maintenance contract, privileged/DB commands mocked | PASS11/11 | terminal output; deploy/tests/maintenance-test.sh |
| Static whitelist Nginx log format | PASS; không phải `nginx -t` | PowerShell source assertion |

Log nằm ở `.local/artifacts/`. Backend dùng Embedded PostgreSQL/storage riêng
trong target, HTTP127.0.0.1 random port. APK release dùng `https://qa.invalid/`
chỉ để kiểm tra build, **không được phát hành**. Không vô hiệu release URL guard.

Upgrade test tạo trạng thái V5 có security_version7, refresh đã revoked, ảnh,
recipient và MOMENT_REPLY; migrate V6/validate/migrate lại, kiểm tra dữ liệu
và revocation còn nguyên. Đây không thay thế thử restore backup hoặc rollback
binary thật. Suite recovery/token hiện có tiếp tục nằm trong backend verify.

AVD dùng userdata QA riêng; fixture ảnh màu tạo trong RAM, không chụp camera,
không gửi tin/ảnh thật hoặc sửa tài khoản cá nhân. Đã kéo ảnh fixture vào
`.local/artifacts/hardening-images/`, xem Chat glass và Viewer soft/dark.
Ảnh màu đặc là chủ đích test. Contrast timestamp/input và icon back Chat cũ
vẫn là hạn chế UI, không đánh dấu toàn app UI PASS. Emulator đã được tắt.

Các lần thất bại đã xử lý: build test Android đầu do caller cache cũ được cập
nhật đúng API; một lượt Maven do helper bị ExecutionPolicy chặn/JRE mặc định,
chạy lại bằng JDK17 và policy chỉ process; release thiếu URL bị guard từ chối,
chạy lại URL HTTPS mẫu. Không bỏ assertion, không suppress để báo PASS.

Review tích hợp phát hiện Firebase sender còn xóa registration toàn cục khi
UNREGISTERED. Đã bỏ dependency repository khỏi sender; chỉ dispatcher xóa với
điều kiện recipient+token. Test Firebase mocked mới xác minh trả UNREGISTERED,
không tự truy cập DB; full verify187/187 chạy sau sửa. Không dùng cloud thật.

## Đo tải và tối ưu

Mỗi baseline/after:240request đo +25warmup, concurrency4,25user,
24conversation/1440tin,24JPEG480×480 synthetic,0response lỗi;20send+
20retry tạo đúng20message. Inbox text64→4 prepared statements/request
(-93,75%); p95 khoảng44,03→18,65ms. Lượt budget cuối cũng đạt ≤8/request.
Chi tiết phương pháp/report/giới hạn tại [PERFORMANCE.md](PERFORMANCE.md).
Một cặp lượt laptop không chứng minh capacity/SLO production. Chưa có số
liệu để tăng pool hoặc kết luận storage/thread pool là bottleneck.

## Review và giới hạn còn lại

- V1–V5 không sửa; V6 additive. Không rollback binary bỏ security_version,
  không restore DB cũ rồi mở traffic nếu chưa reconciliation revocation.
- Outbox at-least-once: có thể trùng khi crash/partial device failure;
  ACCEPTED không chứng minh thiết bị nhận. Disabled không giả thành công.
  Chưa có auto-retention terminal/replay DEAD hoặc process-kill/cloud test.
- Cache không đảm bảo remote wipe file gallery/widget/backup đã tải trước;
  không thêm tác vụ tự xóa dữ liệu cá nhân hoặc định nghĩa hạn pháp lý thay user.
- Chưa chạy Nginx thật/config test TLS, exporter/alert routing, load dài hạn,
  mixed APK thật, FCM delivery và restore staging. Không claim các mục đó PASS.
- Lint179errors có sẵn được ghi ở hồ sơ UI trước; không chạy lại lint trong
  lượt này, không sửa/suppress và không tuyên bố toàn ứng dụng sạch lỗi.
- Review thay đổi và `git diff --check`; kiểm tra các file mới không có
  private key/credential thật. Secrets local không được stage hoặc xuất log.

## Hành động tiếp theo (cần phê duyệt riêng)

Nghiệm thu staging HARDENING-01: chuẩn bị bản sao dữ liệu QA và artifact,
backup/restore nhất quán + bảo toàn revocation, mixed APK và restart dispatcher,
FCM thật và routing cảnh báo qua kênh vận hành; review Nginx trước reload.
Không tự deploy hoặc thao tác database từ tài liệu này.

Trạng thái: **PASS phần triển khai/kiểm thử local đã liệt kê; chưa nghiệm thu
production**. Dừng sau bàn giao, không tự mở tính năng/tiểu bước khác.
