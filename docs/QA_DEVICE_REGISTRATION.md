# Taktak — Nghiệm thu Giai đoạn 6A: Đăng ký thiết bị & FCM Token (Device & FCM Registration)

Cập nhật: 18/10/2026. Mốc kiểm thử: **Giai đoạn 6A — Đăng ký thiết bị & FCM Token: PASS local/AVD**.

Đối chiếu quy tắc: tuân thủ `AGENTS.md`, `docs/TAKTAK_PROJECT.md`, `docs/PROJECT_DECISIONS.md`. Không tự động commit, push, merge hoặc deploy. Dừng trước Giai đoạn 6B (Dispatch Push Notifications).

---

## 1. Thiết kế kiến trúc và giải pháp kỹ thuật

Giai đoạn 6A đặt nền tảng cho hệ thống đồng bộ thời gian thực bằng Firebase Cloud Messaging (FCM). Hệ thống quản lý vòng đời thiết bị (device lifecycle), ánh xạ định danh thiết bị (`deviceId`) với token FCM (`fcmToken`) của từng tài khoản người dùng, đồng thời xử lý các bài toán biên như luân chuyển token giữa nhiều người dùng trên cùng thiết bị vật lý và hỗ trợ đa thiết bị (multi-device).

### 1.1. Backend API & Quản lý Device Token
- **Bảng dữ liệu `device_tokens`** (tận dụng schema Flyway `V1__init_schema.sql` có sẵn):
  - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
  - `user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - `device_id VARCHAR(100) NOT NULL`
  - `fcm_token TEXT NOT NULL`
  - `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`
  - `updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`
  - Ràng buộc duy nhất:
    - `uq_device_tokens_user_device UNIQUE (user_id, device_id)`: Mỗi cặp (người dùng, thiết bị) có duy nhất một bản ghi.
    - `uq_device_tokens_fcm_token UNIQUE (fcm_token)`: Một token FCM tại một thời điểm chỉ thuộc về duy nhất một người dùng và thiết bị, tránh gửi nhầm thông báo khi đổi tài khoản trên cùng máy.

- **Các Endpoint quản lý thiết bị**:
  - `POST /api/devices`: Đăng ký hoặc cập nhật `deviceId` và `fcmToken` cho người dùng hiện tại (yêu cầu Bearer JWT).
    - Request body: `{"deviceId": "...", "fcmToken": "..."}`
    - Logic Upsert & Reassignment:
      1. Tìm và xóa bất kỳ bản ghi nào có cùng `fcmToken` nhưng thuộc người dùng khác hoặc `deviceId` khác (giải quyết trường hợp người dùng A logout, người dùng B login trên cùng máy hoặc máy đổi ID).
      2. Nếu đã tồn tại bản ghi cho `(userId, deviceId)`, cập nhật `fcmToken` và `updatedAt`.
      3. Nếu chưa tồn tại, tạo bản ghi mới.
    - Response: `200 OK` với DTO `DeviceItem`.
  - `DELETE /api/devices/{deviceId}`: Hủy đăng ký thiết bị khi người dùng đăng xuất (logout).
    - Response: `204 No Content`. Idempotent nếu thiết bị không tồn tại hoặc đã bị xóa trước đó.
  - `GET /api/devices`: Liệt kê danh sách các thiết bị đang hoạt động của người dùng hiện tại.
    - Response: `200 OK` với mảng `DeviceItem`.

- **Cấu hình Firebase Admin SDK (`FirebaseConfig.java`)**:
  - Tự động nạp service account private credentials nếu có biến môi trường `FIREBASE_CREDENTIALS_PATH` hoặc `FIREBASE_CONFIG_JSON`.
  - Cơ chế Graceful Fallback: Nếu không có file credential (như môi trường test local / CI), server khởi tạo với chế độ logging cảnh báo mà không làm gián đoạn luồng nghiệp vụ API hay làm crash server.
  - Interface trừu tượng `PushNotificationSender` và triển khai `FirebasePushNotificationSender` sẵn sàng cho Giai đoạn 6B.

### 1.2. Android Client: Đăng ký & Đồng bộ Token tự động
- **Định danh thiết bị duy nhất (`DeviceIdProvider` & `AppDeviceIdProvider`)**:
  - Giao diện `DeviceIdProvider` cung cấp phương thức `getDeviceId()`.
  - Triển khai `AppDeviceIdProvider` tạo và lưu trữ ngẫu nhiên một UUID trong `SharedPreferences` bảo vệ bởi `Context.MODE_PRIVATE`. ID này bền vững qua các lần khởi động lại app.
- **Tầng Remote API & Repository**:
  - `DeviceApi`: Retrofit interface cho `POST /api/devices`, `DELETE /api/devices/{deviceId}`, `GET /api/devices`.
  - `ApiClient`: Tích hợp sẵn `public final DeviceApi devices;`.
  - `DeviceRepository`: Cung cấp `register(String fcmToken)` và `unregister()`.
  - `AuthRepository`: Mở rộng helper `devices(Context)` và tích hợp `logout(Context)` tự động hủy đăng ký thiết bị trước khi xóa phiên local.
- **Firebase Messaging Service (`TaktakMessagingService.java`)**:
  - Kế thừa `FirebaseMessagingService`.
  - `onNewToken(String token)`: Khi FCM cấp hoặc làm mới token, nếu người dùng đang có phiên đăng nhập hợp lệ (`AuthGraph.repository(context).isLoggedIn()`), service tự động gọi `DeviceRepository.register(token)` trong luồng nền để đồng bộ ngay lập tức với backend.
  - `onMessageReceived(RemoteMessage message)`: Xử lý push data payload (chuẩn bị cho 6B đồng bộ dữ liệu thời gian thực).
- **Tích hợp vòng đời phiên (`SessionViewModel.java`)**:
  - Khi người dùng đăng nhập thành công hoặc khôi phục phiên từ EncryptedSharedPreferences, `syncFcmToken(Context)` được kích hoạt: lấy FCM token hiện tại qua `FirebaseMessaging.getInstance().getToken()` và gửi lên backend qua `DeviceRepository.register`.
  - Khi người dùng đăng xuất (`logout(Context)`), app chủ động gọi `DeviceRepository.unregister()` để xóa binding trên server trước khi xóa token JWT local.
- **Manifest & Cấu hình quyền**:
  - Khai báo quyền `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
  - Khai báo service `TaktakMessagingService` với intent-filter `com.google.firebase.MESSAGING_EVENT` và `exported="false"`.
  - Bật cờ `firebase_messaging_auto_init_enabled = true`.

---

## 2. Kết quả kiểm thử thực tế

### 2.1. Backend Integration Tests (Spring Boot)
- Thực hiện: `mvn verify` trên môi trường JDK 17.
- Kết quả: **111/111 tests PASS**, 0 failures, 0 errors, 0 skipped.
- Chi tiết 7 integration tests mới trong `DeviceIntegrationTest.java`:
  1. `registerDeviceSucceedsAndReturnsDeviceItem`: Đăng ký thành công thiết bị mới, trả về `200 OK` với thông tin `id`, `deviceId`, `fcmToken`.
  2. `reregisterDeviceUpdatesTokenSuccessfully`: Gửi token mới trên cùng `deviceId` cập nhật thành công token trong DB mà không sinh bản ghi thừa.
  3. `multiDeviceRegistrationForSameUser`: Cùng một người dùng đăng ký nhiều thiết bị (`phone-1`, `tablet-2`) đều thành công và được lưu trữ độc lập.
  4. `tokenTransferBetweenUsersReassignsOwnership`: Khi token T1 đã gắn với User A được User B gửi lên đăng ký (kịch bản chuyển nhượng máy), server tự động thu hồi binding của User A và gán cho User B, bảo toàn ràng buộc `uq_device_tokens_fcm_token`.
  5. `unregisterDeviceDeletesTokenSuccessfully`: Gọi `DELETE /api/devices/{deviceId}` xóa chính xác bản ghi của thiết bị; gọi lại trả về `204` idempotent.
  6. `registerDeviceWithoutAuthReturns401`: Gọi endpoint thiết bị khi chưa đăng nhập bị từ chối với mã lỗi `401 Unauthorized`.
  7. `registerDeviceWithBlankFieldsReturns400`: Gửi payload rỗng hoặc thiếu trường bắt buộc trả về mã lỗi `400 Bad Request`.
- Đóng gói: Executable JAR `taktak-server-0.0.1-SNAPSHOT.jar` repackage thành công.

### 2.2. Android JVM Unit Tests
- Thực hiện: `./gradlew.bat testDebugUnitTest` với JDK 17 (Android Studio JBR).
- Kết quả: **49/49 tests PASS**, 0 failures, 0 errors.
- Chi tiết 4 unit tests mới trong `DeviceRepositoryTest.java`:
  1. `registerSendsValidRequestToBackend`: Gửi request đăng ký với `deviceId` và `fcmToken` hợp lệ, xác thực mapping model.
  2. `unregisterSendsCorrectDeviceId`: Gửi request hủy đăng ký đúng `deviceId` của thiết bị hiện tại.
  3. `registerWithBlankTokenThrowsIllegalArgumentException`: Chặn từ sớm ở client nếu token rỗng.
  4. `httpErrorThrowsApiExceptionWithCorrectCode`: Ánh xạ mã lỗi HTTP (400, 401, 500) thành `ApiException` với thông điệp rõ ràng.

### 2.3. Android Build & Static Analysis
- Thực hiện: `./gradlew.bat assembleDebug assembleRelease lintDebug lintRelease`
- Kết quả: **BUILD SUCCESSFUL** trong 29 giây, **0 errors**.

### 2.4. Connected Acceptance Tests trên Android Emulator (AVD)
- Môi trường kiểm thử:
  - Thiết bị: Android Emulator `Medium_Phone` (Android 17 / API 37, x86_64, Google Play Services enabled).
  - Kết nối: ADB reverse `tcp:8080 tcp:8080` tới Spring Boot LocalSmokeServer.
  - Runner: `androidx.test.runner.AndroidJUnitRunner`.
- Thực hiện: `Run-AcceptanceTests.ps1 -Class com.taktak.app.DeviceAcceptanceTest`
- Kết quả: **2/2 tests PASS (100% success rate)** trong 24 giây.
  1. `deviceRegistrationAndUnregistrationEndToEnd`: **PASSED (0.745s)**
     - Tạo tài khoản QA, đăng nhập lấy JWT.
     - Kiểm tra `deviceId` tự sinh hợp lệ.
     - Đăng ký FCM token ban đầu → thành công.
     - Làm mới với token mới → cập nhật thành công.
     - Hủy đăng ký thiết bị → thành công `204`.
  2. `multiDeviceRegistrationForSameAccount`: **PASSED (2.988s)**
     - Đăng ký thiết bị chính (phone).
     - Mô phỏng thiết bị thứ hai (tablet) đăng nhập cùng tài khoản với `SessionStore` độc lập.
     - Đăng ký token của tablet thành công song song với phone.
     - Hủy đăng ký tablet độc lập, không ảnh hưởng phone.
- Dọn dẹp: LocalSmokeServer dừng exit 0 (`SMOKE_MEDIA_ROWS=0 FILES=0 RECIPIENTS=0`), cổng 8080 đóng, quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`.

---

## 3. Trạng thái mã nguồn và Git

- Nhánh hiện tại: `chore/push-current-project`
- HEAD: `ed9fde6` (bảo toàn toàn bộ các thay đổi chưa commit, không commit/push/deploy).
- Các file mới và sửa đổi thuộc phạm vi 6A:
  - Backend:
    - Entity `DeviceToken.java`
    - Repository `DeviceTokenRepository.java`
    - DTOs `DeviceDtos.java`
    - Service `DeviceService.java`
    - Controller `DeviceController.java`
    - Config `FirebaseConfig.java`
    - Interfaces `PushNotificationSender.java`, `FirebasePushNotificationSender.java`
    - Exception `DeviceFailure.java` và mapping trong `ApiExceptionHandler.java`
    - Test `DeviceIntegrationTest.java`
  - Android:
    - `DeviceIdProvider.java`, `AppDeviceIdProvider.java`
    - `DeviceModels.java`
    - `DeviceApi.java`
    - `DeviceRepository.java`
    - `TaktakMessagingService.java`
    - Mở rộng `ApiClient.java`, `AuthRepository.java`, `SessionViewModel.java`
    - Khai báo service/permission trong `AndroidManifest.xml`
    - Test `DeviceRepositoryTest.java` (JVM)
    - Test `DeviceAcceptanceTest.java` (AVD Acceptance)
    - Helper script `scripts/Run-AcceptanceTests.ps1`

---

## 4. Điểm dừng và đề xuất tiếp theo

- Giai đoạn 6A đã hoàn thành 100% các tiêu chí nghiệm thu về thiết kế kiến trúc, API backend, xử lý client, và kiểm thử chấp nhận tự động trên môi trường thật/AVD.
- **Đề xuất tiểu bước tiếp theo**: **Giai đoạn 6B — Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (Push Notification Dispatching)**.
  - Khi khoảnh khắc được tạo: Bắn push data notification (`type: moment_created`, `momentId`, `senderDisplayName`) tới các thiết bị của toàn bộ người nhận trong `recipientIds`.
  - Tuyệt đối tuân thủ quy tắc bảo mật: Push chỉ là data payload gọn nhẹ, **không chứa ảnh JPEG, caption, URL hay Bearer token**.
  - Client nhận push sẽ kích hoạt worker đồng bộ hoặc làm mới danh sách ảnh ở màn hình Lịch sử.
- Dừng chờ chỉ đạo và phê duyệt của người dùng trước khi triển khai 6B.
