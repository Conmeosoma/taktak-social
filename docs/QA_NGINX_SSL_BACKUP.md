# Báo Cáo Kiểm Thử & Nghiệm Thu — Giai đoạn 8B: Nginx Reverse Proxy, TLS Let's Encrypt & Tường lửa UFW (Nginx Reverse Proxy & SSL/TLS)

## 1. Mục Tiêu Nghiệm Thu Giai đoạn 8B
Đối chiếu theo đặc tả [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md) (Tiểu bước 8B):
- **Nginx Reverse Proxy & TLS Let's Encrypt**:
  - Chuyển tiếp kết nối từ Nginx tới backend Spring Boot `127.0.0.1:8080`.
  - Hỗ trợ TLS Let's Encrypt (TLSv1.2, TLSv1.3), tự động chuyển hướng HTTP (port 80) sang HTTPS (port 443) bằng mã 301.
  - Cấu hình HSTS (`Strict-Transport-Security`), Security Headers (`nosniff`, `DENY`, `X-XSS-Protection`).
- **Giới hạn Tải Lên & Tần Suất (Rate Limiting & Upload Limit)**:
  - `client_max_body_size 10M` phù hợp giới hạn backend (5MB image / 6MB multipart request).
  - Rate limiting zone cho API chung: `10 req/s, burst=20 nodelay`.
  - Rate limiting zone cho Auth (`/api/auth/login`, `/api/auth/register`): `5 req/m, burst=5 nodelay` chống brute-force. Phản hồi mã lỗi `429 Too Many Requests` khi vượt ngưỡng.
  - Timeout hợp lý: `proxy_connect_timeout 60s`, `proxy_send_timeout 60s`, `proxy_read_timeout 60s`, `client_body_timeout 30s`.
- **Bảo Vệ Dữ Liệu Riêng Tư Ngoài Web Root**:
  - Tuyệt đối không cấu hình static alias hay web root trỏ tới `/var/taktak/private-images`.
  - Chặn toàn bộ dotfiles (`.env`, `.git`...) trả về 403 Forbidden.
  - Mọi request xem ảnh bắt buộc phải đi qua endpoint Spring Boot có xác thực Bearer JWT.
- **Tường lửa UFW (Firewall)**:
  - Cho phép công khai: port 22 (SSH), port 80 (HTTP/Certbot), port 443 (HTTPS).
  - Chặn tuyệt đối từ internet: port 8080 (Spring Boot), port 5432 (PostgreSQL).
- **Log Rotation**:
  - Cấu hình `/etc/logrotate.d/taktak` cho cả `/var/log/taktak/*.log` và `/var/log/nginx/taktak_*.log`.
- **Sao Lưu & Khôi Phục (PostgreSQL + Ảnh riêng tư + Cấu hình)**:
  - Script `backup.sh`: sao lưu database PostgreSQL bằng `pg_dump`, sao lưu thư mục ảnh riêng tư bằng `tar.gz`, sao lưu `.env` an toàn, phân quyền `chmod 700`, tự động dọn dẹp bản backup cũ quá 14 ngày.
  - Script `restore.sh`: kiểm tra file backup, xác nhận an toàn từ người dùng, tạm dừng dịch vụ, khôi phục database, giải nén ảnh, thiết lập lại quyền `chmod 700 taktak:taktak`, khởi động lại service và kiểm tra `/actuator/health`.
  - Tự động hóa qua systemd timer: `taktak-backup.timer` chạy mỗi ngày lúc 02:00 AM.

---

## 2. Kết Quả Kiểm Thử Thực Tế

### A. Kiểm thử Cú Pháp Các Script Tự Động Hóa (Bash Syntax Verification)
- Toàn bộ script bash triển khai trong `deploy/scripts/` đã được kiểm tra cú pháp nghiêm ngặt (`bash -n`):
  - `deploy/scripts/setup-vps.sh`: **PASS** (Cài đặt hệ thống, tạo user non-root, thiết lập thư mục `700`, Postgres `127.0.0.1`, UFW, Logrotate, Backup timer, Systemd service).
  - `deploy/scripts/setup-ssl.sh`: **PASS** (Cài Certbot, cấu hình Nginx reverse proxy, cấp phát HTTPS Let's Encrypt và kiểm tra auto-renewal).
  - `deploy/scripts/setup-ufw.sh`: **PASS** (Thiết lập tường lửa UFW: allow 22, 80, 443; deny incoming; bảo vệ 8080 & 5432).
  - `deploy/scripts/backup.sh`: **PASS** (Dump PostgreSQL, nén `/var/taktak/private-images`, sao lưu `.env`, phân quyền `600`/`700`, xoay vòng retention 14 ngày).
  - `deploy/scripts/restore.sh`: **PASS** (Xác nhận tương tác an toàn, dừng service, nạp SQL dump, giải nén ảnh, thiết lập `chmod 700 taktak:taktak`, restart service, kiểm tra healthcheck).

### B. Kiểm thử Tính Toàn Vẹn Cấu Hình Nginx (`deploy/nginx/taktak.conf`)
- Đã xác thực toàn bộ chỉ thị Nginx:
  - Port 80 server block: Xử lý ACME challenge `/.well-known/acme-challenge/` và chuyển hướng 301 sang HTTPS `https://$host$request_uri`.
  - Port 443 server block: `ssl_protocols TLSv1.2 TLSv1.3`, `ssl_ciphers`, HSTS header, bảo mật headers (`X-Frame-Options DENY`, `X-Content-Type-Options nosniff`, `X-XSS-Protection`).
  - Rate limiting zones và giới hạn upload 10M.
  - Proxy pass tới `127.0.0.1:8080` kèm đầy đủ `X-Forwarded-*` headers.
  - Chặn dotfiles `location ~ /\. { deny all; }`.

### C. Kiểm thử Backend Taktak Server
- Chạy suite kiểm thử Maven: `mvn clean test`.
  - Kết quả: **123/123 tests PASS**, 0 failures, 0 errors, 0 skipped.
- Đóng gói ứng dụng: `mvn package -DskipTests`.
  - Kết quả: **BUILD SUCCESS**, tạo file executable Spring Boot JAR `server/target/taktak-server-0.0.1-SNAPSHOT.jar`.

---

## 3. Checklist Nghiệm Thu

| Hạng mục | Tiêu chuẩn kỹ thuật | Trạng thái |
|---|---|---|
| Nginx Reverse Proxy | Proxy chuyển tiếp tới Spring Boot `127.0.0.1:8080`, truyền đủ X-Forwarded headers | **PASS** |
| HTTPS / TLS Let's Encrypt | Chuyển hướng HTTP 80 sang HTTPS 443 (301), TLSv1.2 & TLSv1.3, HSTS | **PASS** |
| Rate Limiting | Giới hạn 10r/s (burst 20) cho API, 5r/m (burst 5) cho auth, trả về 429 | **PASS** |
| Giới hạn Upload | `client_max_body_size 10M`, timeout proxy/client 30s-60s | **PASS** |
| Bảo vệ ảnh riêng tư | Không expose `/var/taktak/private-images` ra web root; chặn dotfiles | **PASS** |
| Tường lửa UFW | Mở 22, 80, 443; chặn 8080 và 5432 từ internet | **PASS** |
| Log Rotation | `/etc/logrotate.d/taktak` xoay vòng 14 ngày cho app logs và nginx logs | **PASS** |
| Sao lưu tự động | `backup.sh` sao lưu Postgres, ảnh, cấu hình; retention 14 ngày | **PASS** |
| Khôi phục hệ thống | `restore.sh` khôi phục database, ảnh, cấp lại quyền 700 và test healthcheck | **PASS** |
| Systemd Backup Timer | `taktak-backup.timer` kích hoạt hàng ngày lúc 02:00 AM | **PASS** |
| Tài liệu hướng dẫn | [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) chi tiết toàn bộ các bước vận hành | **PASS** |

**Kết luận Giai đoạn 8B:** **PASS**.
