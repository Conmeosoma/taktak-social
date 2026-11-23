# Chuyển Taktak sang VPS Ubuntu mới — 22/11/2026

Người dùng chọn khởi tạo PostgreSQL và kho media mới. Phiên bản backend sẵn
sàng kiểm tra có SHA256
`53f51933ec6aece9b839ce137a3fffcd43cd4aba25032b2a3c1d13c2dc5404fe`.

| Bước | Trạng thái và bằng chứng |
|---|---|
| 1. Chuẩn bị VPS | Đã chuẩn bị ở repo. `ssh-keyscan -T 6 -t ed25519 -p 13464 remote.io.vn` không lấy được khóa. `ssh` với kiểm tra host key nghiêm ngặt báo timeout qua tất cả địa chỉ DNS trả về. Panel VPS báo chưa có Internet. Chưa đăng nhập hoặc thay đổi VPS. |
| 2. HTTPS ổn định | Đã nhận hostname `conmeosoma.duckdns.org`. Bản ghi A hiện trả `104.28.254.73`, chưa xác nhận đây là IP VPS. Kiểm tra HTTPS timeout. Chờ IP public được nhà cung cấp xác nhận để chỉnh bản ghi A, rồi cấu hình Nginx và Let's Encrypt. |
| 3. Android | Đã rà cấu hình. Retrofit lấy URL từ `BuildConfig.BASE_URL` và phiên mã hóa so khớp origin. URL Quick Tunnel cũ còn trong cấu hình Gradle debug local. Chưa thay URL vì chưa có HTTPS đích đã xác minh. |
| 4. APK | Chờ kiểm tra HTTPS bên ngoài và reboot VPS. Sau đó tăng phiên bản, build và kiểm tra URL trong APK. |

Script setup đã bổ sung timezone Asia/Ho_Chi_Minh, FFmpeg và fail2ban đọc
journal SSH. Backup/restore chấp nhận đúng tên MP4 do ứng dụng tạo và kiểm
tra video còn được tham chiếu trong dữ liệu restore. Cơ chế backup hiện có
chạy `pg_dump` theo systemd timer lúc 02:00 và mã hóa snapshot cùng kho media.

Trước khi áp dụng firewall hoặc tắt đăng nhập root qua SSH, cần kiểm tra
kết nối bằng key và quyền sudo của user mới trong một phiên thứ hai. Sau khi
SSH hoạt động, kiểm tra RAM, swap và dung lượng đĩa rồi mới chọn mức giới hạn
JVM. Dùng systemd cho backend và PostgreSQL native trên VPS cấu hình thấp.
Chỉ ứng dụng localhost và reverse proxy truy cập PostgreSQL và backend.

Để mở chặn, trên Console nhà cung cấp chạy `changeip` theo hướng dẫn panel,
sau đó chạy `ip -br -4 addr` và
`ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`. Cần IP Internet hoặc cổng
SSH hoạt động và fingerprint SHA256 để xác minh đúng máy.
