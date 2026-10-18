# Báo Cáo Kiểm Thử & Nghiệm Thu — Giai đoạn 8A: Chuẩn bị môi trường VPS Ubuntu & Cấu hình dịch vụ (VPS Ubuntu Setup & System Services)

## 1. Mục Tiêu Kiểm Thử
- **Môi trường & Phân quyền bảo mật**:
  - Tạo cấu hình triển khai trên VPS Ubuntu 22.04 / 24.04 LTS.
  - User hệ thống không phải root (`taktak`).
  - Thư mục lưu trữ ảnh riêng tư (`IMAGE_STORAGE_ROOT=/var/taktak/private-images`) nằm ngoài web root công khai của Nginx, phân quyền `chmod 700` thuộc sở hữu `taktak:taktak`.
  - Cấu hình PostgreSQL chỉ lắng nghe local loopback `127.0.0.1` hoặc private docker network; không expose cổng 5432 ra internet.
  - Tường lửa UFW chặn toàn bộ truy cập từ internet vào port 8080 (Spring Boot) và 5432 (PostgreSQL).
- **Cấu hình Production (`application-prod.yml`)**:
  - Kích hoạt qua profile `prod` (`spring.profiles.active=prod`).
  - Chặn `ddl-auto=update`, bắt buộc `validate` kết hợp Flyway migration.
  - Toàn bộ bí mật (DB URL/Credentials, JWT Secret, Storage Root, FCM Credentials) nạp qua biến môi trường.
  - Endpoint `actuator/health` hoạt động phục vụ healthcheck của systemd và docker.
- **Dịch vụ chạy nền (System Services)**:
  - Cung cấp file dịch vụ systemd `taktak.service` có các cờ bảo mật hardening (`NoNewPrivileges=true`, `ProtectSystem=strict`, `ProtectHome=true`, `PrivateTmp=true`).
  - Cung cấp file `docker-compose.yml` và `Dockerfile` đa giai đoạn với non-root container, network nội bộ và healthcheck tự động.
  - Script tự động hóa `deploy/scripts/setup-vps.sh`.

---

## 2. Kết Quả Kiểm Thử Thực Tế

### A. Kiểm thử cấu hình & Tương thích Backend (`server/`)
- Đã thêm `server/src/main/resources/application-prod.yml`.
- Chạy toàn bộ test suite backend: `.\mvnw.cmd clean test`.
  - Kết quả: **123/123 tests PASS**, 0 failures, 0 errors, 0 skipped.
- Đóng gói ứng dụng: `.\mvnw.cmd package -DskipTests`.
  - Kết quả: **BUILD SUCCESS**, tạo thành công file executable JAR `taktak-server-0.0.1-SNAPSHOT.jar` (kèm Spring Boot repackage).

### B. Kiểm thử cấu hình Môi trường & Template Bí mật
- File `deploy/.env.example` cung cấp đầy đủ các biến môi trường cần thiết, sử dụng placeholder an toàn, không chứa bất kỳ secret hay mật khẩu thật nào.
- File `deploy/systemd/taktak.service` kiểm tra cú pháp hợp lệ theo chuẩn systemd unit spec.
- File `deploy/docker/docker-compose.yml` và `deploy/docker/Dockerfile` tuân thủ nguyên tắc Least Privilege (chạy dưới non-root user `taktak`, cổng 8080 chỉ bind vào loopback `127.0.0.1`, network internal bridge).

---

## 3. Trạng Thái & Tiêu Chí Nghiệm Thu
- [x] Tạo user dịch vụ `taktak` không phải root.
- [x] PostgreSQL chỉ lắng nghe local/private network.
- [x] Thư mục private images ngoài Nginx public root (`/var/taktak/private-images`, `chmod 700`).
- [x] `application-prod.yml` nạp secrets từ biến môi trường.
- [x] Cấu hình systemd service và Docker Compose có healthcheck.
- [x] Script tự động hóa `setup-vps.sh` và tài liệu hướng dẫn [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md).
- [x] Trạng thái nghiệm thu: **PASS**.
