# Hướng Dẫn Triển Khai Taktak Server Trên VPS Ubuntu (Giai đoạn 8A & 8B)

Tài liệu này hướng dẫn chi tiết toàn bộ quy trình chuẩn bị môi trường, cấu hình dịch vụ bảo mật, Nginx reverse proxy, TLS Let's Encrypt, giới hạn tần suất (Rate Limiting), tường lửa UFW, xoay vòng nhật ký (Logrotate) và quy trình sao lưu / phục hồi dữ liệu tự động cho máy chủ Taktak trên Ubuntu 22.04 / 24.04 LTS.

---

## 1. Yêu Cầu Kiến Trúc & Tiêu Chuẩn Bảo Mật

1. **User hệ thống chuyên dụng**: Chạy ứng dụng dưới user không có đặc quyền `taktak` (không bao giờ chạy bằng `root`).
2. **Cơ sở dữ liệu biệt lập**: PostgreSQL chỉ lắng nghe kết nối nội bộ loopback `127.0.0.1` hoặc private docker network; cấm tuyệt đối mở port `5432` ra internet công khai.
3. **Lưu trữ ảnh riêng tư ngoài Web Root**: Thư mục lưu trữ ảnh (`IMAGE_STORAGE_ROOT=/var/taktak/private-images`) nằm hoàn toàn ngoài thư mục web root của Nginx (`/var/www/` hoặc `/usr/share/nginx/html`). Chỉ user `taktak` có quyền đọc/ghi (`chmod 700`). Mọi request truy cập ảnh bắt buộc phải đi qua API Spring Boot để kiểm tra quan hệ bạn bè và quyền xem của người dùng.
4. **Không hard-code Secrets**: Toàn bộ mật khẩu cơ sở dữ liệu, JWT secret, và đường dẫn service account Firebase được nạp qua biến môi trường local `/var/taktak/.env` (`chmod 600`).
5. **Giới hạn kết nối Spring Boot**: Ứng dụng chỉ bind vào `127.0.0.1:8080` (loopback) để Nginx reverse proxy nhận traffic HTTPS và chuyển tiếp nội bộ.
6. **HTTPS & TLS Bắt Buộc**: Bắt buộc chứng chỉ TLS Let's Encrypt, tự động chuyển hướng HTTP (port 80) sang HTTPS (port 443), kích hoạt HSTS.

---

## 2. Chuẩn Bị Môi Trường VPS (Giai đoạn 8A)

### Bước 1: Chạy script cài đặt tự động
Sao chép thư mục `deploy/` lên VPS và chạy:
```bash
sudo bash deploy/scripts/setup-vps.sh
```
Script sẽ tự động:
- Cài đặt OpenJDK 17 JRE headless, PostgreSQL, Nginx, Certbot, UFW, Logrotate.
- Tạo user hệ thống `taktak` (non-root).
- Tạo cấu trúc thư mục `/var/taktak/private-images` và `/var/taktak/backups` (`chmod 700`).
- Khóa PostgreSQL chỉ lắng nghe `127.0.0.1`.
- Cấu hình tường lửa UFW (mở 22, 80, 443; chặn 8080 và 5432).
- Cài đặt cấu hình logrotate và systemd backup timer.
- Đăng ký service `taktak.service` vào `/etc/systemd/system/`.

### Bước 2: Khởi tạo Database PostgreSQL
```bash
sudo -u postgres psql << EOF
CREATE USER taktak WITH PASSWORD 'CHANGE_ME_STRONG_DATABASE_PASSWORD';
CREATE DATABASE taktak OWNER taktak;
GRANT ALL PRIVILEGES ON DATABASE taktak TO taktak;
EOF
```

### Bước 3: Cấu hình biến môi trường
Tạo file `/var/taktak/.env` từ file mẫu:
```bash
sudo cp deploy/.env.example /var/taktak/.env
sudo chmod 600 /var/taktak/.env
sudo chown taktak:taktak /var/taktak/.env
```
Mở file và cập nhật các giá trị bí mật:
- Sinh khóa JWT ngẫu nhiên mạnh: `openssl rand -base64 32` và điền vào `JWT_ACCESS_SECRET`.
- Điền mật khẩu database vào `DB_PASSWORD`.
- Nếu dùng FCM, đặt file credentials Firebase vào `/var/taktak/firebase-admin.json` và cấp quyền `chmod 600`.

### Bước 4: Chuyển giao file ứng dụng & Khởi động Backend
Sao chép file JAR build từ máy local lên VPS:
```bash
sudo cp server/target/taktak-server-0.0.1-SNAPSHOT.jar /var/taktak/taktak-server.jar
sudo chown taktak:taktak /var/taktak/taktak-server.jar
sudo chmod 750 /var/taktak/taktak-server.jar

# Khởi động dịch vụ backend
sudo systemctl enable --now taktak

# Xem trạng thái hoạt động
sudo systemctl status taktak
```

### Bước 5: Kiểm tra Healthcheck nội bộ
```bash
curl -i http://127.0.0.1:8080/actuator/health
```
Phản hồi mong đợi: `{"status":"UP"}` kèm `HTTP/1.1 200 OK`.

---

## 3. Cấu Hình Nginx Reverse Proxy, TLS Let's Encrypt & UFW (Giai đoạn 8B)

### Bước 1: Cấu hình Nginx & Cấp phát chứng chỉ HTTPS Let's Encrypt
Chạy script tự động hóa cấp phát chứng chỉ SSL:
```bash
sudo bash deploy/scripts/setup-ssl.sh <DOMAIN_CỦA_BẠN> <EMAIL_QUẢN_TRỊ>
```
*Ví dụ:* `sudo bash deploy/scripts/setup-ssl.sh api.taktak.example.com admin@example.com`

Script thực hiện:
1. Sao chép mẫu cấu hình `deploy/nginx/taktak.conf` sang `/etc/nginx/sites-available/taktak`.
2. Thay thế `YOUR_DOMAIN_OR_IP` bằng domain thật của bạn.
3. Kích hoạt site qua `/etc/nginx/sites-enabled/taktak` và gỡ bỏ site `default`.
4. Gọi `certbot --nginx` để tự động xác thực tên miền qua ACME challenge và nạp chứng chỉ TLS vào Nginx.
5. Kiểm tra cú pháp bằng `nginx -t` và tải lại cấu hình `systemctl reload nginx`.
6. Kiểm tra tính năng tự động gia hạn chứng chỉ bằng `certbot renew --dry-run`.

### Bước 2: Cơ chế Rate Limiting (Giới hạn tần suất)
Cấu hình Nginx được tích hợp 2 lớp phòng vệ chống tấn công quá tải và brute-force:
- **API chung (`/api/`)**: `10 req/s`, burst `20 nodelay`.
- **Xác thực (`/api/auth/login`, `/api/auth/register`)**: `5 req/phút`, burst `5 nodelay`.
- Khi vượt ngưỡng, Nginx lập tức phản hồi mã lỗi `429 Too Many Requests`.

### Bước 3: Bảo mật đường dẫn riêng tư & File tĩnh
- **Tuyệt đối không map thư mục ảnh riêng tư**: Nginx không cấu hình bất kỳ `location` nào trỏ tới `/var/taktak/private-images`.
- Mọi yêu cầu lấy ảnh đều phải đi qua `GET /api/moments/{id}/image` của Spring Boot để xác thực JWT token và quan hệ bạn bè.
- Chặn toàn bộ dotfiles (`.env`, `.git`...):
  ```nginx
  location ~ /\. {
      deny all;
      access_log off;
      log_not_found off;
  }
  ```

### Bước 4: Kiểm tra Tường lửa UFW
Chạy script kiểm tra và siết chặt tường lửa:
```bash
sudo bash deploy/scripts/setup-ufw.sh
```
Trạng thái tường lửa:
- `22/tcp (SSH)`: **ALLOW**
- `80/tcp (HTTP)`: **ALLOW** (phục vụ Certbot ACME & Redirect sang HTTPS)
- `443/tcp (HTTPS)`: **ALLOW** (Giao thức mã hóa an toàn chính)
- `8080/tcp (Spring Boot)`: **BLOCKED** từ internet (chỉ nhận traffic từ loopback 127.0.0.1 do Nginx chuyển tiếp)
- `5432/tcp (PostgreSQL)`: **BLOCKED** từ internet (chỉ nhận traffic nội bộ từ 127.0.0.1)

---

## 4. Xoay Vòng Nhật Ký (Logrotate)

Cấu hình xoay vòng log đã được thiết lập tại `/etc/logrotate.d/taktak`:
- Nhật ký ứng dụng: `/var/log/taktak/*.log` xoay vòng hàng ngày, lưu trữ 14 ngày, nén tự động bằng gzip (`delaycompress`), tạo file mới với quyền `0640 taktak taktak`.
- Nhật ký Nginx: `/var/log/nginx/taktak_*.log` xoay vòng hàng ngày, lưu 14 ngày, gửi tín hiệu `USR1` reload pid Nginx sau khi xoay vòng.

Kiểm tra thử nghiệm chạy logrotate:
```bash
sudo logrotate -d /etc/logrotate.d/taktak
```

---

## 5. Quy Trình Sao Lưu & Khôi Phục Dữ Liệu (Backup & Restore)

Hệ thống cung cấp giải pháp toàn diện sao lưu đồng thời 3 thành phần sống còn: **Database PostgreSQL + Thư mục Ảnh riêng tư + File cấu hình `.env`**.

### 5.1. Sao lưu tự động qua Systemd Timer
- Dịch vụ sao lưu tự động `taktak-backup.timer` kích hoạt script `/var/taktak/scripts/backup.sh` mỗi ngày vào lúc **02:00 AM**.
- Bản sao lưu được lưu trữ tại `/var/taktak/backups/<YYYYMMDD_HHMMSS>/` với phân quyền `chmod 700` thuộc sở hữu `taktak:taktak`.
- Chính sách xoay vòng (Retention Policy): Tự động xóa các bản sao lưu cũ hơn **14 ngày**.

Kiểm tra trạng thái timer:
```bash
sudo systemctl status taktak-backup.timer
```

### 5.2. Chạy sao lưu thủ công
Quản trị viên có thể kích hoạt sao lưu bất kỳ lúc nào:
```bash
sudo bash /var/taktak/scripts/backup.sh
```
Kết quả bản sao lưu bao gồm:
- `db_taktak_<timestamp>.sql.gz`: Dump PostgreSQL nén.
- `private_images_<timestamp>.tar.gz`: Toàn bộ ảnh khoảnh khắc riêng tư nén.
- `env_<timestamp>.bak`: Bản sao file biến môi trường (`chmod 600`).
- `metadata.json`: Thông tin chi tiết bản sao lưu.

### 5.3. Quy trình Khôi phục Dữ liệu (Disaster Recovery)
Khi cần khôi phục lại hệ thống từ một bản sao lưu cụ thể:

1. **Liệt kê danh sách bản sao lưu có sẵn**:
   ```bash
   ls -la /var/taktak/backups/
   ```
2. **Thực hiện khôi phục**:
   ```bash
   sudo bash /var/taktak/scripts/restore.sh <TIMESTAMP_HOẶC_THƯ_MỤC>
   ```
   *Ví dụ:* `sudo bash /var/taktak/scripts/restore.sh 20261018_093000`

3. **Cơ chế hoạt động của script restore**:
   - Yêu cầu xác nhận tường minh (`yes`) để tránh thao tác nhầm lẫn ghi đè dữ liệu.
   - Tạm dừng dịch vụ `taktak.service` nhằm ngăn chặn tranh chấp dữ liệu.
   - Khôi phục database PostgreSQL từ file SQL dump nén.
   - Dọn sạch và giải nén lại toàn bộ thư mục `/var/taktak/private-images`.
   - Thiết lập lại phân quyền an toàn `chmod 700 /var/taktak/private-images` và `chown -R taktak:taktak`.
   - Khởi động lại dịch vụ `taktak.service`.
   - Tự động gọi endpoint `/actuator/health` để xác thực dịch vụ đã sẵn sàng và hoạt động ổn định.

---

## 6. Lệnh Vận Hành & Giám Sát Thường Dùng

| Mục đích | Lệnh thực thi |
|---|---|
| Kiểm tra trạng thái Backend | `sudo systemctl status taktak` |
| Xem nhật ký backend thời gian thực | `sudo journalctl -u taktak -f` |
| Xem nhật ký truy cập Nginx | `sudo tail -f /var/log/nginx/taktak_access.log` |
| Xem nhật ký lỗi Nginx | `sudo tail -f /var/log/nginx/taktak_error.log` |
| Khởi động lại Backend | `sudo systemctl restart taktak` |
| Tải lại cấu hình Nginx | `sudo nginx -t && sudo systemctl reload nginx` |
| Kiểm tra trạng thái tường lửa | `sudo ufw status verbose` |
| Kiểm tra thời hạn chứng chỉ SSL | `sudo certbot certificates` |
| Thử nghiệm gia hạn chứng chỉ SSL | `sudo certbot renew --dry-run` |
| Kiểm tra danh sách bản backup | `sudo ls -la /var/taktak/backups` |
