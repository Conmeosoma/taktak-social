# Sổ Tay Hướng Dẫn Sử Dụng VPS Ubuntu Cho Dự Án Taktak (Từ A đến Z)

Tài liệu này được viết chi tiết, dễ hiểu dành cho quản trị viên và lập trình viên để làm quen, kết nối, triển khai, vận hành và xử lý sự cố máy chủ Taktak trên VPS Ubuntu 22.04 / 24.04 LTS.

---

## 1. Chuẩn Bị Thông Tin Trước Khi Bắt Đầu

Khi mua hoặc tạo một máy chủ VPS (từ các nhà cung cấp như DigitalOcean, Linode/Akamai, Vultr, Hetzner, AWS, hoặc VPS Việt Nam như Viettel IDC, BKNS, Vietnix...), bạn sẽ nhận được thông tin:
- **Địa chỉ IP của VPS** (Ví dụ: `103.123.45.67`)
- **Tài khoản mặc định**: Thường là `root` (hoặc `ubuntu`)
- **Mật khẩu quản trị** hoặc **SSH Private Key** (`.pem` / `.ppk`)
- **Cổng SSH**: Mặc định là cổng `22`

---

## 2. Cách Kết Nối Vào VPS Ubuntu Từ Máy Tính Windows

### Cách 1: Dùng PowerShell / Windows Terminal (Đơn giản nhất, không cần cài phần mềm)
1. Mở **PowerShell** hoặc **Windows Terminal** trên máy tính của bạn.
2. Gõ lệnh kết nối SSH:
   ```powershell
   ssh root@103.123.45.67
   ```
   *(Thay `103.123.45.67` bằng IP thật của VPS của bạn)*.
3. Nếu là lần đầu kết nối, hệ thống sẽ hỏi: `Are you sure you want to continue connecting (yes/no/[fingerprint])?`  
   -> Gõ `yes` và nhấn **Enter**.
4. Nhập mật khẩu VPS khi được hỏi (lưu ý: khi gõ mật khẩu trên Linux, màn hình sẽ **không hiện dấu sao `*` hay ký tự**, bạn cứ gõ đúng mật khẩu rồi nhấn **Enter**).
5. Khi màn hình hiện `root@ubuntu:~#` là bạn đã đăng nhập thành công vào VPS!

*(Nếu dùng SSH Key: `ssh -i C:\duong-dan\id_rsa root@103.123.45.67`)*

---

### Cách 2: Dùng phần mềm giao diện trực quan (Termius / PuTTY / MobaXterm)
- **Termius** (Khuyên dùng, giao diện đẹp, hiện đại, lưu thông tin server dễ dàng): Tải tại [termius.com](https://termius.com/). Tạo New Host -> Điền IP, Username (`root`), Password và bấm Connect.
- **MobaXterm** (Tích hợp sẵn cả giao diện dòng lệnh SSH và khung kéo thả file SFTP ở cột bên trái).

---

## 3. Cách Truyền File Từ Máy Tính Lên VPS (File Transfer)

### Cách 1: Dùng phần mềm đồ họa WinSCP hoặc FileZilla (Cực kỳ dễ, kéo thả như Windows Explorer)
1. Tải và cài đặt **WinSCP** (tại [winscp.net](https://winscp.net/)) hoặc **FileZilla**.
2. Mở WinSCP, điền thông tin kết nối:
   - **File protocol**: SFTP
   - **Host name**: Địa chỉ IP của VPS
   - **Port number**: 22
   - **User name**: `root`
   - **Password**: Mật khẩu VPS
3. Bấm **Login**. 
4. Cửa sổ bên trái là thư mục trên máy tính Windows của bạn, cửa sổ bên phải là thư mục trên VPS Ubuntu. Bạn chỉ cần kéo thả file/thư mục qua lại như dùng USB.

---

### Cách 2: Dùng lệnh `scp` trên PowerShell của Windows
Mở PowerShell tại thư mục dự án `taktak`:
```powershell
# 1. Tải cả thư mục deploy/ lên thư mục /home/ của VPS:
scp -r deploy root@103.123.45.67:/root/

# 2. Tải file JAR backend lên VPS:
scp server/target/taktak-server-0.0.1-SNAPSHOT.jar root@103.123.45.67:/root/taktak-server.jar
```

---

## 4. Các Lệnh Linux Cơ Bản Thường Dùng Trên VPS Ubuntu

| Lệnh | Ý nghĩa & Mục đích sử dụng |
|---|---|
| `ls -la` | Liệt kê tất cả file và thư mục (kể cả file ẩn) kèm phân quyền |
| `cd /duong/dan` | Di chuyển vào thư mục chỉ định (Ví dụ: `cd /var/taktak`) |
| `pwd` | Xem đường dẫn thư mục hiện tại bạn đang đứng |
| `cat tenfile` | Xem nhanh nội dung của một file |
| `nano tenfile` | Mở trình soạn thảo văn bản đơn giản để sửa file (Bấm `Ctrl+O` để Lưu, `Enter`, `Ctrl+X` để Thoát) |
| `df -h` | Kiểm tra dung lượng ổ cứng còn trống hay đã đầy |
| `free -h` | Kiểm tra dung lượng RAM đang dùng và còn trống |
| `top` hoặc `htop` | Xem biểu đồ CPU, RAM và các tiến trình đang chạy (Bấm `q` để thoát) |
| `ss -tulpn` | Xem danh sách các cổng (Port) mạng đang mở trên VPS |
| `clear` | Xóa sạch màn hình dòng lệnh cho đỡ rối mắt |

---

## 5. Quy Trình Cài Đặt Máy Chủ Taktak Lần Đầu Trên VPS

Sau khi kết nối SSH vào VPS và upload thư mục `deploy/` cùng file `taktak-server.jar`:

### Bước 1: Chạy script cài đặt hệ thống tự động
```bash
cd /root/deploy/scripts
sudo bash setup-vps.sh
```
*Script này sẽ tự động cài Java 17, PostgreSQL, Nginx, Certbot, UFW, tạo user `taktak`, tạo thư mục ảnh riêng tư `chmod 700`, chặn port 8080 & 5432 và đăng ký systemd service.*

### Bước 2: Tạo Cơ sở dữ liệu PostgreSQL
```bash
sudo -u postgres psql << EOF
CREATE USER taktak WITH PASSWORD 'MatKhauDatabaseCucKyManh123!';
CREATE DATABASE taktak OWNER taktak;
GRANT ALL PRIVILEGES ON DATABASE taktak TO taktak;
EOF
```

### Bước 3: Điền file cấu hình mật (.env)
```bash
sudo cp /root/deploy/.env.example /var/taktak/.env
sudo chmod 600 /var/taktak/.env
sudo chown taktak:taktak /var/taktak/.env
sudo nano /var/taktak/.env
```
Trong file `.env`, chỉnh sửa:
- `DB_PASSWORD`: Điền mật khẩu DB vừa tạo ở Bước 2 (`MatKhauDatabaseCucKyManh123!`).
- `JWT_ACCESS_SECRET`: Tạo một chuỗi ngẫu nhiên bằng lệnh `openssl rand -base64 32` rồi dán vào đây.
- Bấm `Ctrl+O` -> `Enter` để lưu, `Ctrl+X` để thoát.

### Bước 4: Đặt file JAR và khởi động dịch vụ
```bash
sudo cp /root/taktak-server.jar /var/taktak/taktak-server.jar
sudo chown taktak:taktak /var/taktak/taktak-server.jar
sudo chmod 750 /var/taktak/taktak-server.jar

# Khởi động dịch vụ Taktak:
sudo systemctl enable --now taktak

# Kiểm tra dịch vụ có chạy không:
curl -i http://127.0.0.1:8080/actuator/health
```
*Kết quả thấy `{"status":"UP"}` là backend đã chạy ngon lành!*

---

## 6. Cấu Hình Tên Miền (Domain) & Chứng Chỉ HTTPS Let's Encrypt Miễn Phí

Để ứng dụng Android kết nối được vào VPS qua HTTPS an toàn:

### Bước 1: Trỏ DNS của Tên miền về IP của VPS
Vào trang quản trị tên miền của bạn (Cloudflare, Namecheap, GoDaddy, MatBao, PA Vietnam...):
- Thêm bản ghi: **Type A**
- Name (Host): `api` (hoặc `@` nếu dùng tên miền gốc)
- IPv4 address (Value): `103.123.45.67` (Địa chỉ IP của VPS)
- TTL: `Auto` hoặc `300`

### Bước 2: Chạy script tự động cấp phát SSL
```bash
sudo bash /root/deploy/scripts/setup-ssl.sh api.taktak.example.com email-cua-ban@gmail.com
```
*Script sẽ tự động cấu hình Nginx, xin chứng chỉ HTTPS từ Let's Encrypt và bật tự động gia hạn.*

### Bước 3: Kiểm tra đường dẫn HTTPS từ máy tính hoặc điện thoại
Mở trình duyệt trên máy tính và gõ:
```text
https://api.taktak.example.com/actuator/health
```
Thấy hiện ổ khóa bảo mật màu xanh và phản hồi `{"status":"UP"}` là hoàn tất!

---

## 7. Các Lệnh Quản Lý Vận Hành Hàng Ngày

### Quản lý Dịch vụ Backend (Systemd)
```bash
# Xem trạng thái hoạt động:
sudo systemctl status taktak

# Khởi động dịch vụ:
sudo systemctl start taktak

# Dừng dịch vụ:
sudo systemctl stop taktak

# Khởi động lại dịch vụ:
sudo systemctl restart taktak

# Xem log chạy trực tiếp thời gian thực:
sudo journalctl -u taktak -f
```

### Quản lý Web Server Nginx
```bash
# Xem trạng thái Nginx:
sudo systemctl status nginx

# Kiểm tra cú pháp cấu hình Nginx có bị lỗi không:
sudo nginx -t

# Tải lại cấu hình Nginx mà không làm gián đoạn kết nối:
sudo systemctl reload nginx

# Xem log truy cập thời gian thực:
sudo tail -f /var/log/nginx/taktak_access.log

# Xem log lỗi Nginx:
sudo tail -f /var/log/nginx/taktak_error.log
```

### Quản lý Cơ sở dữ liệu PostgreSQL
```bash
# Vào màn hình dòng lệnh psql:
sudo -u postgres psql -d taktak

# Trong psql:
# Xem danh sách các bảng: \dt
# Thoát khỏi psql: \q
```

---

## 8. Cách Cập Nhật Ứng Dụng Khi Có Phiên Bản Mới (Update)

Mỗi khi bạn sửa code Java ở máy tính local và muốn cập nhật lên VPS:

1. **Build file JAR mới trên máy tính Windows**:
   ```powershell
   cd c:\Users\NamTien\Documents\taktak\server
   .\mvnw.cmd package -DskipTests
   ```
2. **Upload file JAR mới lên VPS**:
   ```powershell
   scp target/taktak-server-0.0.1-SNAPSHOT.jar root@103.123.45.67:/root/taktak-server-new.jar
   ```
3. **Chạy lệnh cập nhật an toàn trên VPS**:
   ```bash
   sudo bash /var/taktak/scripts/update.sh /root/taktak-server-new.jar
   ```
   *Cơ chế tự động: Script sẽ tự động snapshot sao lưu database -> backup JAR cũ -> nạp JAR mới -> restart dịch vụ -> kiểm tra healthcheck trong 30s. Nếu bản mới bị crash hoặc lỗi khởi động, script sẽ **tự động Rollback** ngay về bản cũ để bảo đảm hệ thống không bị chết.*

---

## 9. Sao Lưu (Backup) & Khôi Phục (Restore) Dữ Liệu

### 1. Sao lưu tự động
Hệ thống đã được cài đặt sẵn lịch tự động sao lưu toàn bộ (Database + Ảnh riêng tư + File `.env`) vào lúc **02:00 AM mỗi đêm**.
- Các file backup được lưu tại `/var/taktak/backups/`.
- Tự động xoay vòng xóa các bản sao lưu cũ hơn **14 ngày** để không làm đầy ổ cứng.

### 2. Tạo bản sao lưu ngay lập tức bằng tay
Bất cứ khi nào bạn muốn sao lưu ngay (ví dụ trước khi bảo trì lớn):
```bash
sudo bash /var/taktak/scripts/backup.sh
```

### 3. Khôi phục lại dữ liệu khi máy chủ gặp sự cố
1. Xem danh sách các bản backup đang có:
   ```bash
   ls -la /var/taktak/backups/
   ```
2. Chạy lệnh khôi phục (điền tên thư mục backup mong muốn):
   ```bash
   sudo bash /var/taktak/scripts/restore.sh 20261018_093000
   ```
   Hệ thống sẽ hỏi xác nhận `yes`, sau đó tự động nạp lại DB, phục hồi thư mục ảnh, cấp lại quyền `chmod 700` và khởi động lại dịch vụ an toàn.

---

## 10. Các Quy Tắc Bảo Mật Cần Ghi Nhớ Khi Dùng VPS

1. **Tuyệt đối không chạy ứng dụng bằng user `root`**: Taktak Server luôn được chạy dưới user dịch vụ `taktak:taktak` bị giới hạn quyền.
2. **Không mở cổng 8080 và 5432 ra internet**: Tường lửa UFW đã chặn 2 cổng này. Mọi truy cập từ internet phải đi qua Nginx cổng 443 HTTPS.
3. **Bảo mật file `/var/taktak/.env`**: File này chứa mật khẩu DB và JWT Secret, chỉ root và user taktak được đọc (`chmod 600`), không bao giờ đưa nội dung file này lên mạng hoặc gửi cho người lạ.
4. **Cập nhật bảo mật Ubuntu định kỳ**: Mỗi tháng 1 lần, bạn nên chạy lệnh sau trên VPS để cập nhật các bản vá lỗi bảo mật của Linux:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```
