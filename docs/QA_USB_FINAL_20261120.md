# Nghiệm thu bản debug trên điện thoại — 20/11/2026

Xiaomi 23129RAA4G, Android 15 (API 35), 1080×2400. Đã cập nhật app và APK test
qua USB bằng `install -r`. App trên máy khớp SHA256 của APK debug ngày 18/11
trong [hồ sơ bàn giao](QA_FINAL_ROLLOUT_20261118.md).

| Phạm vi | Kết quả |
|---|---|
| Camera kép và Live | Đạt ca capture trên camera thật. Xác nhận ảnh vuông, hủy giữ nút chụp, giới hạn clip, thả sớm, video có/không có track âm thanh và phát lại. Chỉ dùng cache QA. |
| Advanced/UI | 12 ca Advanced/UI đạt trong `qa-usb-20261120-core.log`, bao gồm ca Camera trên. Ba ca HTTPS cùng lượt dừng ở bước bật opt-in và được chạy riêng. |
| QR | Hồi quy 2/2. Torch bật/tắt đạt. Lượt quét đầu timeout, lượt chạy lại đọc được đúng mã khi đưa ảnh QR vào khung. Bộ chọn ảnh Android đọc đúng file QA. Các log có tiền tố `qa-usb-20261120-qr-`. |
| Outbox | Hai pha write/read qua force-stop đạt. Lần đầu mô phỏng mất mạng bằng dừng connector gặp timeout. Lượt chạy lại ngắt/nối tuyến ADB reverse riêng đạt, WorkManager gửi đúng một lần, giữ idempotency key và dọn payload. Log `qa-usb-20261120-outbox-retry.log`. |
| Widget | Cả hai provider có mặt trên máy, kiểm tra inflate RemoteViews đạt trong nhóm Advanced/UI. Chưa nghiệm thu cập nhật FCM nền và widget trên launcher Xiaomi với tài khoản QA riêng. |
| HTTPS production | 3/3 ca kết nối thất bại ở bước DNS sau khi bật opt-in. Laptop cũng không phân giải URL tunnel hiện tại trong APK. SSH cổng quản trị hết thời gian chờ, nên chưa xác định trạng thái dịch vụ bên trong VPS. Log `qa-usb-20261120-https.log`. |

Đã khôi phục quyền microphone, dọn file QR tạm trong Downloads và gỡ tuyến
ADB reverse do lượt này tạo. Backend QA riêng đã dừng và xác nhận 0 media
còn hoạt động, 0 file và 0 avatar. Phiên cá nhân được giữ trong các ca fixture.

Kết quả **đạt một phần**. Cần khôi phục URL HTTPS trước khi chạy lại nghiệm
thu VPS. Ca FCM nền/launcher cần tài khoản QA riêng trên máy. Phạm vi thiết bị
chỉ gồm Android 15, còn ma trận API 26–30 và APK release ký chính thức.
Không có thay đổi mã ứng dụng, commit, push hoặc triển khai trong lượt QA này.
