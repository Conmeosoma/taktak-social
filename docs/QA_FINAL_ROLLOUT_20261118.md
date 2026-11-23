# Triển khai bản sửa và bàn giao APK — 18/11/2026

Bản sửa Camera/Live, Preview, Outbox và Poll đã qua hồi quy ở lượt trước. Lượt
này triển khai JAR sửa Poll đã kiểm thử và build APK từ workspace hiện tại.
Xem [kết quả hồi quy](QA_AVD_FIX_ALL_20261116.md).

- VPS chạy JAR SHA256 `53f51933ec6aece9b839ce137a3fffcd43cd4aba25032b2a3c1d13c2dc5404fe`.
  Đã giữ JAR trước cập nhật tại `/var/taktak/rollout-20261118-IWR9r2ip/previous.jar`.
  Service active, health nội bộ và HTTPS đều UP, Poll thiếu JWT trả 401.
  Bản cập nhật không thêm migration.
- Smoke VPS bằng hai tài khoản QA: tạo Poll, vote, đóng, xác nhận vote sau khi
  đóng trả `400 POLL_CLOSED` và số phiếu vẫn đúng. Tranh chấp giữa hai giao
  dịch đã có ca hồi quy local trong bộ 231 test backend. Đã dọn ảnh, quan hệ
  và phiên QA. Hai tài khoản QA vẫn tồn tại.
- Build debug và release thành công. Chạy lại Android unit test 144/144.
  Lint release có 0 lỗi và 124 cảnh báo. Release chặn cleartext. Cả hai APK
  chứa đúng URL HTTPS tunnel tạm.

Hai file đã chép vào Downloads và đối chiếu SHA256:

| File | Trạng thái | SHA256 |
|---|---|---|
| Taktak-20261118-final-debug.apk | Cài thử được, đã xác minh chữ ký debug | `9f2d61b3ef156f41a7e927893d8df0c940f635affefc610e10896112f4003a3b` |
| Taktak-20261118-release-unsigned.apk | Đã build, chờ khóa ký phát hành | `277479ac8bb225b02e6102cd7f059c9bc81ed7b9af85476b2539c27a4f83e864` |

Kết quả đạt trong phạm vi deploy, smoke VPS và build. Chưa chạy lại QA máy
thật trong lượt này. URL backend vẫn là Quick Tunnel tạm. Chưa commit/push.
