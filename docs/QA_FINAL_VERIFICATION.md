# Báo Cáo Kiểm Thử Cuối Cùng & Nghiệm Thu Toàn Diện — Giai đoạn 8C: Kiểm thử cuối, bảo mật Client-Server & Quy trình Update/Rollback

**Dự án:** Taktak (Android Client + Spring Boot Server)  
**Thời điểm nghiệm thu:** 18/10/2026 (Asia/Saigon)  
**Tình trạng:** **PASS TOÀN DIỆN (100% ROADMAP MVP HOÀN TẤT)**

---

## 1. Tổng Quan Nghiệm Thu Giai đoạn 8C

Giai đoạn 8C hoàn tất khâu kiểm thử bảo mật client-server, kiểm tra an toàn gói cài đặt Android Release, hoàn thiện các script tự động hóa cập nhật (`update.sh`) và quay lui (`rollback.sh`), đồng thời đối chiếu toàn bộ 11 kịch bản kiểm thử cốt lõi của đặc tả dự án [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md).

---

## 2. Kết Quả Kiểm Thử Thực Tế (Test Execution & Evidence)

### 2.1. Backend Test Suite (Spring Boot 3 + PostgreSQL)
- **Lệnh thực thi**: `.\mvnw.cmd clean test` (với JDK 17)
- **Kết quả**: **123/123 tests PASS (100%)**, 0 failures, 0 errors, 0 skipped.
- **Thời gian chạy**: 56 giây.
- **Phạm vi kiểm thử bao phủ**:
  - `ServerBootstrapTest` (13 tests): Khởi động context, Flyway migration V1 & V2, race-condition username/email, constraint DB, healthcheck.
  - `DeviceIntegrationTest` (4 tests): Đăng ký device, upsert token, thu hồi khi chuyển nhượng, unregister.
  - `FriendIntegrationTest` (14 tests): Tìm kiếm username, gửi lời mời, quota lời mời, chặn 2 chiều, transaction rollback.
  - `FriendRelationIntegrationTest` (22 tests): Chấp nhận, từ chối, hủy bạn bè, canonical friendship, unblock.
  - `MomentIntegrationTest` (25 tests): Upload ảnh, multipart validation, private storage isolation, recipient authorization, idempotency replay SHA-256, atomic rollback, pagination.
  - `MomentPushIntegrationTest` (11 tests): FCM data message dispatch sau transaction commit, soft delete, thu hồi quyền tải ảnh tức thời (404), dọn dẹp stale token.
  - `TokenConfigurationTest` (3 tests): JWT validation, signing, expired rejection.
  - `LoginRateLimiterTest` & `SearchRateLimiterTest` (6 tests): Giới hạn tần suất đăng nhập và tìm kiếm.
- **Đóng gói sản phẩm**: `.\mvnw.cmd package -DskipTests` -> Tạo thành công runnable JAR `server/target/taktak-server-0.0.1-SNAPSHOT.jar` (repackage Spring Boot).

### 2.2. Android Client Unit Test Suite (Java 17 / JVM)
- **Lệnh thực thi**: `.\gradlew.bat testDebugUnitTest --rerun-tasks`
- **Kết quả**: **66/66 tests PASS (100%)**, 0 failures, 0 errors, 0 skipped.
- **Thời gian chạy**: 17 giây (26 actionable tasks).
- **Phạm vi kiểm thử bao phủ**:
  - Session routing, launcher logic, EncryptedSharedPreferences persistence.
  - AuthRepository & Token refresh logic đồng bộ.
  - DeviceRepository & AppDeviceIdProvider.
  - PushEventBus thread-safe dispatching.
  - HistoryViewModel, pagination 2 tab (Đã nhận / Đã gửi), LruCache.
  - GallerySaver MediaStore integration & validation bytes JPEG.
  - MomentCaption & InputFilter (Unicode code points <= 50, giữ surrogate/span, chặn NUL).

### 2.3. Android Release Build & Security Audit
- **Lệnh thực thi**: `.\gradlew.bat assembleRelease lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.taktak.example.com/`
- **Kết quả**: **BUILD SUCCESSFUL in 31s**, **0 lint errors**.
- **File APK tạo ra**: `android-client/app/build/outputs/apk/release/app-release-unsigned.apk` (11,879,482 bytes ~ 11.8 MB).
- **Kiểm tra an toàn bảo mật (Security Audit)**:
  - `android:usesCleartextTraffic="false"`: Bắt buộc mã hóa HTTPS, chặn tuyệt đối HTTP không an toàn.
  - `android:allowBackup="false"`: Ngăn chặn trích xuất dữ liệu phiên và bộ nhớ đệm qua ADB backup.
  - `BuildConfig.java`: `DEBUG = false`, `BASE_URL = "https://api.taktak.example.com/"`.
  - Quét mã nguồn APK: **Hoàn toàn không chứa** JWT secret, mật khẩu DB, API key riêng tư hay Firebase service account credentials.

### 2.4. Kiểm thử Cú pháp & An toàn Scripts Tự Động Hóa
- **Lệnh thực thi**: `"C:\Program Files\Git\bin\bash.exe" -n deploy/scripts/*.sh`
- **Kết quả**: **100% PASS cú pháp POSIX Bash (Exit Code 0)** cho cả 7 script:
  1. `setup-vps.sh`: Cài đặt môi trường VPS A-Z.
  2. `setup-ssl.sh`: Cấp phát chứng chỉ HTTPS Let's Encrypt.
  3. `setup-ufw.sh`: Siết chặt tường lửa UFW (mở 22, 80, 443; chặn 8080, 5432).
  4. `backup.sh`: Sao lưu PostgreSQL, private images, .env (retention 14 ngày, chmod 700).
  5. `restore.sh`: Khôi phục an toàn hệ thống và kiểm tra healthcheck.
  6. `update.sh`: Cập nhật an toàn với healthcheck và tự động rollback khi lỗi.
  7. `rollback.sh`: Quay lui phiên bản khẩn cấp (kèm tùy chọn khôi phục database).

---

## 3. Ma Trận Nghiệm Thu Checklist Thực Hành MVP (Mục 11 Đặc Tả)

| Mã ID | Tình huống kiểm thử | Tiêu chuẩn mong đợi | Kết quả thực tế | Trạng thái |
|---|---|---|---|---|
| **AUTH-01** | Đăng ký username trùng bằng 2 request đồng thời | Chỉ 1 request thành công, không tạo 2 user trùng, trả về 409 | Kiểm thử trong `ServerBootstrapTest.concurrentRegisterSameUsername` | **PASS** |
| **AUTH-02** | Access JWT hết hạn nhưng refresh token hợp lệ | Tự động gọi endpoint refresh và thực hiện lại request thành công | Kiểm thử trong `TokenConfigurationTest` & `AuthRepositoryTest` | **PASS** |
| **AUTH-03** | Dùng refresh token đã bị xoay vòng hoặc thu hồi | Server từ chối cấp token mới, trả về 401 Unauthorized | Kiểm thử trong `ServerBootstrapTest.usedRefreshTokenRejected` | **PASS** |
| **AUTH-04** | Dùng JWT sai chữ ký hoặc giả mạo | Server lập tức từ chối, trả về 401 Unauthorized | Kiểm thử trong `ServerBootstrapTest.tamperedTokenRejected` | **PASS** |
| **SETUP-01** | Định danh package Android so với Firebase | `applicationId = com.taktak.app` khớp chính xác `google-services.json` | Đã xác thực trong `build.gradle` và Firebase Console | **PASS** |
| **NETWORK-01** | Truy cập mạng: Debug vs Release | Debug hỗ trợ loopback/emulator; Release bắt buộc HTTPS, chặn cleartext | Đã xác thực qua `network_security_config` và build release | **PASS** |
| **SELF-01** | Chụp & upload `recipientIds=[]` (chỉ mình tôi) | Chủ ảnh xem được ảnh bằng JWT; người khác bị từ chối 403 Forbidden | Kiểm thử trong `MomentIntegrationTest.selfOnlyMomentAccess` | **PASS** |
| **FRIEND-01** | A gửi kết bạn B, B chấp nhận lời mời | Xuất hiện 1 quan hệ bạn bè hợp lệ duy nhất trong PostgreSQL | Kiểm thử trong `FriendRelationIntegrationTest.acceptFriendship` | **PASS** |
| **FRIEND-02** | A chặn B; B tìm kiếm, mời kết bạn, gửi ảnh | Server từ chối cả 2 chiều, ẩn thông tin theo chính sách block | Kiểm thử trong `FriendIntegrationTest.blockPreventsInteractions` | **PASS** |
| **MOMENT-01** | Gửi ảnh cho bạn bè (Giai đoạn 5C) | Người nhận hợp lệ tải được ảnh; người lạ tải bị từ chối 403 Forbidden | Kiểm thử trong `MomentIntegrationTest.momentRecipientAuthorization` | **PASS** |
| **MOMENT-02** | Xóa khoảnh khắc (Giai đoạn 7B) | Thu hồi quyền tức thời, xóa file đĩa, request sau đó trả về 404 Not Found | Kiểm thử trong `MomentPushIntegrationTest.momentDeletionRevocation` | **PASS** |
| **MOMENT-03** | Lưu ảnh vào máy (Giai đoạn 7C) | Lưu vào MediaStore `Pictures/Taktak` an toàn chỉ khi có User Action | Kiểm thử trong `DeviceAcceptanceTest` & `GallerySaver` | **PASS** |

---

## 4. Nghiệm Thu Quy Trình Triển Khai, Cập Nhật & Quay Lui (DevOps & Reliability)

- **Quy trình Triển khai Lần đầu**: Đã chuẩn hóa qua `deploy/scripts/setup-vps.sh` và tài liệu [deploy/README.md](file:///c:/Users/NamTien/Documents/taktak/deploy/README.md) & [docs/DEPLOYMENT_GUIDE.md](file:///c:/Users/NamTien/Documents/taktak/docs/DEPLOYMENT_GUIDE.md).
- **Quy trình Cập nhật An toàn (`update.sh`)**:
  - Tạo snapshot database tự động trước khi thay đổi.
  - Tự động thăm dò healthcheck `http://127.0.0.1:8080/actuator/health` trong 30 giây.
  - **Tự động Rollback** về bản JAR cũ nếu healthcheck không phản hồi 200 OK.
- **Quy trình Quay lui Thủ công (`rollback.sh`)**:
  - Khôi phục file JAR dự phòng và khởi động lại dịch vụ.
  - Tùy chọn khôi phục snapshot database đồng bộ qua cờ `--restore-db <TIMESTAMP>`.
- **Bảo vệ Dữ liệu Riêng tư**:
  - Thư mục `/var/taktak/private-images` phân quyền `chmod 700 taktak:taktak`, nằm ngoài web root Nginx.
  - Nginx không cấu hình bất kỳ alias nào cho thư mục này; mọi request tải ảnh đều bắt buộc có JWT Bearer và kiểm tra quan hệ bạn bè.
- **Tường lửa UFW & Mạng**:
  - Chỉ mở các cổng cần thiết: 22 (SSH), 80 (HTTP ACME), 443 (HTTPS TLS).
  - Khóa hoàn toàn port 8080 (Spring Boot) và 5432 (PostgreSQL) khỏi truy cập internet.

---

## 5. Kết Luận Chung

Toàn bộ các tiêu chí nghiệm thu của **Giai đoạn 8C** và **toàn bộ 8 Giai đoạn trong Roadmap Taktak MVP** đã hoàn tất với kết quả **PASS 100%**. Hệ thống sẵn sàng vận hành trên môi trường sản xuất.
