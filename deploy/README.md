# Taktak Deployment & Operations Guide (Giai đoạn 8)

Tài liệu này tổng hợp toàn bộ kiến trúc hạ tầng, quy trình triển khai máy chủ VPS Ubuntu, bảo mật mạng, quy trình cập nhật an toàn và sổ tay xử lý sự cố (Troubleshooting Runbook) của dự án Taktak.

---

## 1. Sơ Đồ Kiến Trúc Hệ Thống (System Architecture)

```mermaid
graph TD
    Client["Android Client (com.taktak.app)<br/>Java 17, Material 3, CameraX, WorkManager"]
    UFW["Tường lửa UFW<br/>Mở: 22, 80, 443<br/>Chặn: 8080, 5432"]
    Nginx["Nginx Reverse Proxy<br/>Port 80 -> 443 301 Redirect<br/>Port 443 TLS Let's Encrypt<br/>Rate Limiting: API 10r/s, Auth 5r/m"]
    SpringBoot["Taktak Spring Boot Server<br/>Port 127.0.0.1:8080<br/>User: taktak (Non-root)<br/>systemd: taktak.service"]
    Postgres["PostgreSQL Database<br/>Port 127.0.0.1:5432<br/>Flyway Migration V1, V2"]
    PrivateStorage["Private Image Storage<br/>/var/taktak/private-images<br/>Quyền: chmod 700 taktak:taktak<br/>(Ngoài Web Root)"]
    FCM["Firebase Cloud Messaging (FCM)<br/>Push Notification (V1 HTTP API)"]
    BackupService["Automated Daily Backup<br/>taktak-backup.timer (02:00 AM)<br/>Database + Ảnh + .env"]

    Client -->|HTTPS / Port 443| UFW
    UFW -->|Port 443| Nginx
    Nginx -->|Reverse Proxy / Loopback| SpringBoot
    SpringBoot -->|JDBC / Localhost| Postgres
    SpringBoot -->|File I/O Xác thực| PrivateStorage
    SpringBoot -->|FCM Data Message| FCM
    FCM -->|Push Event (Sync Widget/History)| Client
    BackupService -.->|Sao lưu snapshot| Postgres
    BackupService -.->|Nén lưu trữ| PrivateStorage
```

---

## 2. Bảng Phân Quyền & Cổng Mạng (Network & Storage Matrix)

| Thành phần | Cổng / Đường dẫn | Quyền / Phạm vi mạng | Mục đích |
|---|---|---|---|
| **SSH** | `22/tcp` | Public (Mở qua UFW) | Quản trị từ xa |
| **HTTP (Nginx)** | `80/tcp` | Public (Mở qua UFW) | Certbot ACME & Redirect 301 sang HTTPS |
| **HTTPS (Nginx)** | `443/tcp` | Public (Mở qua UFW) | Kênh giao tiếp bảo mật chính của ứng dụng |
| **Spring Boot** | `8080/tcp` | **Nội bộ `127.0.0.1`** (Chặn từ ngoài) | Nhận request từ Nginx reverse proxy |
| **PostgreSQL** | `5432/tcp` | **Nội bộ `127.0.0.1`** (Chặn từ ngoài) | Lưu trữ người dùng, khoảnh khắc, quan hệ bạn bè |
| **Private Images** | `/var/taktak/private-images` | `chmod 700 taktak:taktak` | Lưu ảnh khoảnh khắc (ngoài web root, không URL public) |
| **File Secrets** | `/var/taktak/.env` | `chmod 600 taktak:taktak` | Chứa mật khẩu DB, JWT secret, FCM path |
| **Thư mục Backups** | `/var/taktak/backups` | `chmod 700 taktak:taktak` | Lưu các bản sao lưu xoay vòng 14 ngày |

---

## 3. Cấu Trúc Thư Mục `deploy/`

```text
deploy/
├── .env.example                 # Mẫu biến môi trường an toàn với placeholder
├── docker/
│   ├── Dockerfile               # Multi-stage Eclipse Temurin 17 JRE Alpine non-root
│   └── docker-compose.yml       # Cụm container app + db có private network & healthcheck
├── logrotate/
│   └── taktak                   # Cấu hình xoay vòng nhật ký cho app và Nginx
├── nginx/
│   └── taktak.conf              # Cấu hình reverse proxy, HTTPS, rate limit, upload 10M
├── scripts/
│   ├── backup.sh                # Sao lưu PostgreSQL + Private Images + .env
│   ├── restore.sh               # Khôi phục hệ thống an toàn và kiểm tra healthcheck
│   ├── setup-ssl.sh             # Cấp phát chứng chỉ HTTPS Let's Encrypt Certbot
│   ├── setup-ufw.sh             # Thiết lập tường lửa UFW siết chặt cổng
│   ├── setup-vps.sh             # Script cài đặt tổng thể VPS Ubuntu A-Z
│   ├── update.sh                # Cập nhật phiên bản JAR an toàn & auto-rollback
│   └── rollback.sh              # Quay lui phiên bản thủ công khi có sự cố
└── systemd/
    ├── taktak.service           # Unit file dịch vụ backend có sandbox hardening
    ├── taktak-backup.service    # Unit file chạy backup.sh
    └── taktak-backup.timer      # Timer kích hoạt backup định kỳ 02:00 AM mỗi ngày
```

---

## 4. Quy Trình Triển Khai Lần Đầu (First-Time Setup)

1. **Chuẩn bị máy chủ**: Tạo VPS Ubuntu 22.04 hoặc 24.04 LTS.
2. **Sao chép thư mục triển khai lên VPS**:
   ```bash
   scp -r deploy/ user@YOUR_VPS_IP:/home/user/
   ```
3. **Chạy script cài đặt tự động**:
   ```bash
   sudo bash /home/user/deploy/scripts/setup-vps.sh
   ```
4. **Cấu hình biến môi trường**:
   ```bash
   sudo cp /home/user/deploy/.env.example /var/taktak/.env
   sudo chmod 600 /var/taktak/.env
   sudo chown taktak:taktak /var/taktak/.env
   sudo nano /var/taktak/.env
   ```
5. **Chuyển giao file JAR sản phẩm**:
   ```bash
   sudo cp taktak-server.jar /var/taktak/taktak-server.jar
   sudo chown taktak:taktak /var/taktak/taktak-server.jar
   sudo chmod 750 /var/taktak/taktak-server.jar
   sudo systemctl enable --now taktak
   ```
6. **Kích hoạt HTTPS Let's Encrypt**:
   ```bash
   sudo bash /home/user/deploy/scripts/setup-ssl.sh api.taktak.example.com admin@example.com
   ```
7. **Xác nhận trạng thái hệ thống**:
   ```bash
   curl -I https://api.taktak.example.com/actuator/health
   # Kết quả mong đợi: HTTP/2 200 kèm {"status":"UP"}
   ```

---

## 5. Quy Trình Cập Nhật & Quay Lui (Update & Rollback)

### Cập nhật phiên bản mới (Zero-Risk Update):
```bash
sudo bash /var/taktak/scripts/update.sh /tmp/taktak-server-new.jar
```
- Tự động tạo bản sao lưu snapshot trước khi update.
- Sao lưu file JAR hiện tại thành `taktak-server.jar.bak`.
- Khởi động lại dịch vụ và thăm dò `http://127.0.0.1:8080/actuator/health`.
- Nếu thất bại sau 30 giây: **Tự động quay lui (Rollback) ngay lập tức** về bản cũ và giữ nguyên trạng thái dịch vụ.

### Quay lui thủ công khẩn cấp (Emergency Rollback):
```bash
# Chỉ quay lui file JAR:
sudo bash /var/taktak/scripts/rollback.sh

# Quay lui cả file JAR và khôi phục Database từ snapshot:
sudo bash /var/taktak/scripts/rollback.sh --restore-db 20261018_093000
```

---

## 6. Sổ Tay Xử Lý Sự Cố (Troubleshooting Runbook)

### Sự cố 1: Nginx báo lỗi `502 Bad Gateway`
- **Nguyên nhân**: Dịch vụ Spring Boot chưa khởi động xong, đã bị crash hoặc không lắng nghe port 8080.
- **Cách xử lý**:
  1. Kiểm tra trạng thái dịch vụ: `sudo systemctl status taktak`
  2. Xem 50 dòng log gần nhất: `sudo journalctl -u taktak -n 50 --no-pager`
  3. Thử restart dịch vụ: `sudo systemctl restart taktak`

### Sự cố 2: Lỗi kết nối Cơ sở dữ liệu (`Connection to localhost:5432 refused` hoặc `FATAL: password authentication failed`)
- **Nguyên nhân**: PostgreSQL service bị dừng hoặc mật khẩu trong `/var/taktak/.env` không khớp.
- **Cách xử lý**:
  1. Kiểm tra PostgreSQL: `sudo systemctl status postgresql`
  2. Kiểm tra kết nối psql nội bộ: `sudo -u postgres psql -U taktak -d taktak -h 127.0.0.1`
  3. Xác thực biến `DB_URL` và `DB_PASSWORD` trong `/var/taktak/.env`.

### Sự cố 3: Chứng chỉ SSL Let's Encrypt hết hạn hoặc lỗi gia hạn
- **Nguyên nhân**: Cổng 80 bị chặn khiến Certbot không thể hoàn tất ACME challenge.
- **Cách xử lý**:
  1. Kiểm tra ngày hết hạn: `sudo certbot certificates`
  2. Kiểm tra tường lửa: `sudo ufw status | grep 80` (đảm bảo port 80 ALLOW)
  3. Chạy thử nghiệm gia hạn: `sudo certbot renew --dry-run`
  4. Gia hạn ngay: `sudo certbot renew --force-renewal && sudo systemctl reload nginx`

### Sự cố 4: Đĩa cứng VPS bị đầy (`No space left on device`)
- **Nguyên nhân**: Quá nhiều log hệ thống hoặc các bản sao lưu cũ chưa được dọn dẹp.
- **Cách xử lý**:
  1. Kiểm tra phân vùng đĩa: `df -h`
  2. Kiểm tra dung lượng thư mục Taktak: `du -sh /var/taktak/*`
  3. Dọn dẹp log hệ thống cũ: `sudo journalctl --vacuum-time=7d`
  4. Kiểm tra thư mục sao lưu: `ls -lh /var/taktak/backups` (chính sách lưu 14 ngày).

### Sự cố 5: Client nhận mã lỗi `429 Too Many Requests`
- **Nguyên nhân**: Người dùng hoặc thiết bị gửi quá số lượng request cho phép trong thời gian ngắn (vượt 10r/s hoặc 5r/m ở auth).
- **Cách xử lý**:
  1. Đây là hành vi bảo vệ bình thường của Nginx rate limiting.
  2. Kiểm tra log Nginx: `sudo tail -f /var/log/nginx/taktak_error.log | grep limiting`
  3. Nếu cần tăng ngưỡng cho môi trường tải cao, điều chỉnh `rate=20r/s` trong `deploy/nginx/taktak.conf` và chạy `sudo nginx -t && sudo systemctl reload nginx`.
