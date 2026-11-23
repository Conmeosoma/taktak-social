# Sửa kết nối VPS và đồng bộ APK — 15/11/2026

## Nguyên nhân và thay đổi

- VPS còn IP VLAN quản trị nhưng VLAN Internet eth0.209 không có IPv4 DHCP;
  default route chỉ qua VLAN quản trị. Backend nội bộ UP, DNS/HTTPS ra ngoài lỗi.
- Khôi phục DHCP trên VLAN đã được nhà cung cấp cấu hình, route Internet qua
  gateway của lease; policy route riêng cho IP quản trị giữ SSH hoạt động.
- Thay ExecStart của changip-apply.service bằng script idempotent trong
  deploy/scripts/restore-provider-network.sh, có timeout/retry và giữ VLAN quản trị.
  Script được kiểm tra bash -n và restart service thành công; chưa thử reboot VPS.
- Cloudflare Quick Tunnel đã restart hơn 6.500 lần, sau khi có mạng trả 429.
  Dừng vòng lặp, đặt RestartSec=300; lần khởi động sau đã đăng ký tunnel thành công.
  Drop-in mẫu nằm trong deploy/systemd/. Không đổi IP để vượt rate limit.
- Cài FFmpeg/ffprobe Ubuntu 4.4.2-0ubuntu0.22.04.1 cho Live validator.
- Deploy JAR mới sau full verify. Backup JAR/env/database/private-images tại
  /var/taktak/before-advanced-20261115-1794755557, chỉ root đọc thư mục.
  pg_restore --list đọc được dump; không phải một lượt diễn tập restore mới.
- Flyway áp V8/V9 thành công, giữ V1–V7; không reset hoặc xóa tài khoản/dữ liệu cũ.

JAR SHA256: `6357d450d9cd38ec56ba3b8420feb5afe3624df876b64e7a9609a622684bea7d`.

## Kết quả xác minh

| Kiểm tra | Kết quả |
|---|---|
| Backend Maven verify | 230/230, 0 failure/error/skipped, BUILD SUCCESS |
| Android assembleDebug và unit | BUILD SUCCESS; 144/144, 0 failure/error |
| Health qua HTTPS | 200, status UP |
| API AI không JWT | 401 |
| Hai tài khoản QA đăng ký/đăng nhập | PASS |
| Upload ảnh chỉ mình tôi, tải ảnh, tạo/đọc/đóng Poll | PASS |
| Tài khoản lạ tải ảnh private | Bị từ chối |
| Live upload/download qua HTTPS | Bytes khớp; duration/audio lấy từ media thật |
| Video giả | 415 |
| Refresh phiên | PASS |
| Dọn media QA, từ chối tải sau xóa, logout QA | PASS |
| SMTP Gmail STARTTLS + authentication | PASS; không gửi email |
| taktak/cloudflared/changip-apply | active sau sửa |

Hai tài khoản qa_ops_* giữ trong database; phiên QA đã logout, media QA đã xóa
qua API. Không sửa tài khoản cá nhân hoặc gửi FCM/email thử.
Không gọi Gemini generateContent trong lượt này.

Log local ignored: vps-fix-backend-verify-final.log, vps-fix-apk-build.log,
vps-fix-https-smoke.log trong .local/artifacts/. Lượt verify đầu thiếu đường dẫn
FFmpeg không đạt, đã sửa môi trường và chạy lại đủ 230 ca; không bỏ qua test.

## APK và giới hạn

URL mới: https://notes-lincoln-nickel-spec.trycloudflare.com/.
Đã cập nhật debug Gradle property, build APK và kiểm tra URL trong DEX.
File bàn giao: Downloads/Taktak-20261115-vps-fixed.apk (debug-signed).
SHA256: `B87D897A50AC243A7BEF18D4BB02198212DD24688ABF37D169DBA79E660BB6EE`.

PASS cho phạm vi khôi phục kết nối/deploy/API smoke/build. Chưa chạy QA UI trên
điện thoại trong lượt này; các timeout Camera/Live AVD và cảnh báo lint ở hồ sơ
UI review vẫn còn mở. Không coi build/unit/API smoke là nghiệm thu toàn ứng dụng.

Quick Tunnel vẫn có thể đổi URL khi process restart; cần named tunnel/domain ổn
định để phát hành lâu dài. Chưa cấu hình vì cần tài khoản/domain của chủ dự án.
Không tự reboot VPS để nghiệm thu boot hoặc đổi kiến trúc kết nối.
Không commit/push trong lượt sửa này; bảo toàn thay đổi UI/báo cáo đang có.
