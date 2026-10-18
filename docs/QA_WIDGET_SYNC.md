# Taktak — Nghiệm thu Giai đoạn 6C: AppWidgetProvider 2×2 & WorkManager Widget Sync

Cập nhật: 18/10/2026. Mốc kiểm thử: **Giai đoạn 6C — AppWidgetProvider 2×2 & WorkManager Widget Sync: PASS local/AVD**.

Đối chiếu quy tắc: tuân thủ `AGENTS.md`, `docs/TAKTAK_PROJECT.md`, `docs/PROJECT_DECISIONS.md`. Không tự động commit, push, merge hoặc deploy. Dừng trước Giai đoạn 7A (Lịch sử Đã gửi).

---

## 1. Thiết kế kiến trúc và giải pháp kỹ thuật

Giai đoạn 6C hoàn thành việc đưa khoảnh khắc mới nhất của bạn bè lên màn hình chính thông qua App Widget kích thước 2×2, tuân thủ nghiêm ngặt các nguyên tắc bất biến của Taktak: bảo mật, riêng tư, tiết kiệm pin, và an toàn IPC của Android.

### 1.1. Backend Spring Boot: Endpoint `GET /api/moments/latest`
- **Đặc tả nghiệp vụ**:
  - `GET /api/moments/latest` yêu cầu xác thực Bearer JWT (`@AuthenticationPrincipal Jwt jwt`).
  - Truy vấn khoảnh khắc nhận được mới nhất chưa bị xóa (`deleted_at IS NULL`) mà người gọi là người nhận hợp lệ (`moment_recipients.user_id = :viewerId`).
  - Trả về `200 OK` kèm `MomentItem` (id, thông tin công khai sender, caption, createdAt) nếu có khoảnh khắc.
  - Trả về `204 No Content` (body rỗng) nếu người dùng chưa nhận được khoảnh khắc nào hoặc toàn bộ khoảnh khắc đã bị xóa.
  - Phản hồi gắn header `Cache-Control: no-store` để bảo vệ quyền riêng tư.
  - Nếu khoảnh khắc mới nhất bị xóa bởi người gửi, lần gọi `GET /api/moments/latest` tiếp theo sẽ tự động rơi về khoảnh khắc hợp lệ liền kề trước đó hoặc trả về 204.

### 1.2. Android Client: App Widget 2×2 & RemoteViews Tuân thủ Chuẩn M3
- **Giao diện Widget 2×2 (`res/layout/widget_taktak_2x2.xml` & `widget_taktak_2x2_info.xml`)**:
  - Kích thước 2×2 (`minWidth="140dp"`, `minHeight="140dp"`), bo góc `24dp` theo chuẩn Android 12+ Material Design.
  - Khung ảnh hiển thị ảnh khoảnh khắc `centerCrop`, phủ dải chuyển sắc gradient chân ảnh (`widget_gradient_overlay`) để chữ màu trắng nổi bật trên nền mọi bức ảnh.
  - Hiển thị tên người gửi (`displayName` hoặc `username`) và chú thích ảnh (`caption` tối đa 50 ký tự Unicode, ẩn nếu rỗng).
  - Trạng thái rỗng (Placeholder / Empty state): Icon camera, tiêu đề *"Chưa có ảnh nào"*, phụ đề *"Chạm để mở Taktak"*.
  - Trạng thái chưa đăng nhập (Logged-out state): Icon ổ khóa, tiêu đề *"Chưa đăng nhập"*, phụ đề *"Đăng nhập để xem khoảnh khắc"*.
  - Bấm vào bất kỳ đâu trên Widget đều kích hoạt `PendingIntent` mở `MainActivity` (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`).
- **An toàn IPC Binder & RemoteViews**:
  - Tuân thủ danh sách view hợp lệ của `RemoteViews` (sử dụng `ImageView` cho dải gradient thay vì thẻ `View` thô).
  - Thuật toán `decodeSampledBitmap`: Downsample ảnh an toàn xuống độ phân giải tối đa 512×512 với cấu hình `RGB_565` (tiết kiệm 50% RAM), giới hạn dung lượng Bitmap dưới 512 KB, loại bỏ hoàn toàn nguy cơ sập ứng dụng do lỗi `TransactionTooLargeException` của Binder IPC.

### 1.3. Cơ chế Đồng bộ: `AppWidgetProvider` & `WorkManager`
- **Nguyên tắc bất biến**: `TaktakAppWidgetProvider.onUpdate` **chỉ enqueue WorkManager**, tuyệt đối không gọi mạng trực tiếp trong BroadcastReceiver.
- **Worker xử lý nền (`WidgetUpdateWorker.java`)**:
  - Kiểm tra trạng thái phiên (`AuthRepository.hasSession()`): Nếu chưa đăng nhập, render trạng thái yêu cầu đăng nhập.
  - Gọi `MomentRepository.latest()` qua mạng an toàn với JWT Bearer.
  - Nếu nhận `null` (204 No Content), render trạng thái rỗng (`renderEmpty`).
  - Nếu có khoảnh khắc mới nhất, tải file ảnh qua `moments.download(id)`, downsample bitmap và cập nhật widget (`renderMoment`).
  - Tự động bắt lỗi: Nếu lỗi `UNAUTHORIZED` thì render trạng thái đăng xuất; nếu lỗi mạng tạm thời hoặc rate-limit thì yêu cầu WorkManager `Result.retry()`.
- **Kích hoạt tức thì từ FCM Push**:
  - `TaktakMessagingService.onMessageReceived`: Khi nhận push `moment_created` hoặc `moment_deleted`, service gọi `WidgetUpdater.enqueueOneTimeWork(...)` với `ExistingWorkPolicy.REPLACE`. Khi thiết bị đang ở chế độ nền, Worker lập tức được kích hoạt để cập nhật ảnh mới nhất lên widget.
- **Dự phòng định kỳ (Periodic Work)**:
  - Khi widget được thêm vào màn hình chính (`onEnabled`), app đăng ký `PeriodicWorkRequest` 15 phút (`taktak_widget_periodic`) qua WorkManager để đồng bộ dự phòng.
  - Khi xóa toàn bộ widget khỏi màn hình chính (`onDisabled`), công việc định kỳ tự động bị hủy.
- **Đồng bộ phiên đăng xuất**:
  - Khi người dùng đăng xuất trong app (`SessionViewModel.signOut`), hàm `WidgetUpdater.renderLoggedOut(...)` được gọi ngay lập tức, chuyển giao diện widget sang trạng thái yêu cầu đăng nhập.

---

## 2. Kết quả kiểm tra thực tế

### 2.1. Backend Integration Tests (Spring Boot)
- Thực hiện: `.\mvnw.cmd clean verify` trên môi trường JDK 17 (Android Studio JBR).
- Kết quả: **122/122 tests PASS**, 0 failures, 0 errors, 0 skipped.
- 4 integration tests mới cho `GET /api/moments/latest`:
  1. `latestMomentReturnsNoContentWhenNoMomentsReceived`: Trả về 204 No Content khi người dùng chưa nhận ảnh nào.
  2. `latestMomentReturnsMostRecentReceivedMoment`: Trả về đúng khoảnh khắc mới nhất kèm sender profile và caption khi nhận liên tiếp 2 ảnh.
  3. `latestMomentSkipsDeletedMoments`: Khi người gửi xóa ảnh mới nhất, API tự động trả về ảnh hợp lệ kế trước; khi xóa hết, API trả về 204.
  4. `latestMomentRequiresAuthentication`: Request không có Bearer token bị từ chối 401 Unauthorized.
- Đóng gói: Executable JAR `taktak-server-0.0.1-SNAPSHOT.jar` repackage thành công.

### 2.2. Android JVM Unit Tests
- Thực hiện: `./gradlew.bat testDebugUnitTest` với JDK 17.
- Kết quả: **60/60 tests PASS** (tăng thêm 4 unit tests mới cho `MomentRepository.latest()`), 0 failures, 0 errors.
  1. `latestReturnsNullOn204NoContent`: PASS
  2. `latestReturnsMomentItemOn200Ok`: PASS
  3. `latestThrowsUnauthorizedWhenNoSession`: PASS
  4. `latestThrowsServerWhenInvalidJson`: PASS

### 2.3. Android Build & Lint
- Thực hiện: `./gradlew.bat assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.taktak.test/`
- Kết quả: **BUILD SUCCESSFUL** trong 6 giây, **0 errors**.

### 2.4. Connected Acceptance Tests trên Android Emulator (AVD)
- Môi trường: Android Emulator `Medium_Phone` (Android 17 / API 37, x86_64), reverse port 8080 tới Spring Boot `LocalSmokeServer`.
- Runner: `scripts/Run-AcceptanceTests.ps1 -Class com.taktak.app.DeviceAcceptanceTest`
- Kết quả: **6/6 tests PASS (100% success rate)** trong 15.322 giây:
  1. `multiDeviceRegistrationForSameAccount`: PASSED (8.141s)
  2. `deviceRegistrationAndUnregistrationEndToEnd`: PASSED (1.032s)
  3. `latestMomentEndToEndWithBackend`: PASSED (2.545s) — Alice kết bạn với User, Alice upload ảnh gửi cho User, User gọi `latest()` nhận đúng moment của Alice; Alice xóa ảnh, User gọi `latest()` tự động nhận null.
  4. `pushEventBusDispatchesEventsToListeners`: PASSED (0.902s)
  5. `widgetUpdaterRendersAllStatesAndDecodesBitmap`: PASSED (0.977s) — Render an toàn các trạng thái LoggedOut, Empty, Loaded Moment; kiểm tra thuật toán `decodeSampledBitmap` downsample ảnh 800×800 về `<= 512×512` an toàn; enqueue OneTime và Periodic Work.
  6. `momentUploadAndDeleteEndToEnd`: PASSED (1.725s)
- Dọn dẹp môi trường:
  - `LocalSmokeServer` nhận tín hiệu kết thúc an toàn (exit 0): `SMOKE_MEDIA_ROWS=0 FILES=0 RECIPIENTS=1`.
  - Cổng 8080 đã đóng, quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`.

---

## 3. Giới hạn Android bắt buộc phải trình bày

Tuân thủ mục 384 của `docs/TAKTAK_PROJECT.md`:
1. **Chính sách nền & Pin của hệ điều hành**: Không thể đảm bảo widget cập nhật lập tức 100% trong mọi tình huống khi app ở chế độ nền sâu. Android áp dụng các cơ chế tiết kiệm pin (Doze Mode, App Standby Buckets) có thể trì hoãn WorkManager và kết nối mạng cho đến khi thiết bị vào Maintenance Window hoặc người dùng bật màn hình.
2. **Tùy biến của nhà sản xuất (OEM Battery Optimization)**: Một số nhà sản xuất thiết bị (Xiaomi/MIUI/HyperOS, Samsung, Oppo, Vivo...) áp dụng chính sách tắt tiến trình nền gắt gao; người dùng cần bật quyền *"Tự khởi chạy"* (Autostart) và *"Không hạn chế pin"* cho Taktak để widget nhận tín hiệu FCM và WorkManager kịp thời.
3. **Lực dừng ứng dụng (Force Stop)**: Khi người dùng Force Stop ứng dụng trong Cài đặt hệ thống, toàn bộ BroadcastReceiver, AlarmManager và WorkManager đều bị vô hiệu hóa cho đến lần mở app thủ công kế tiếp.
4. **Giới hạn WorkManager Periodic**: Chu kỳ `PeriodicWorkRequest` tối thiểu của Android là 15 phút và không chạy chính xác theo từng phút mà phụ thuộc vào thời điểm hệ thống gom nhóm tác vụ (batching).

---

## 4. Trạng thái mã nguồn và Git

- Nhánh: `chore/push-current-project`
- HEAD: `ed9fde6` (bảo toàn toàn bộ các thay đổi chưa commit, không commit/push/deploy).
- Các file mới và sửa đổi thuộc 6C:
  - Backend:
    - `MomentRepository.java`: Bổ sung query `findLatestReceivedMoment`.
    - `MomentService.java`: Bổ sung phương thức `latestMoment(UUID viewerId)`.
    - `MomentController.java`: Bổ sung endpoint `GET /api/moments/latest`.
    - `MomentPushIntegrationTest.java`: Bổ sung 4 integration tests kiểm thử `GET /api/moments/latest`.
  - Android:
    - `MomentApi.java`: Thêm `Call<MomentItem> latest()`.
    - `MomentRepository.java`: Thêm `latest()`.
    - `strings.xml`: Thêm các chuỗi giao diện widget.
    - `res/drawable/`: Thêm `widget_background.xml`, `widget_gradient_overlay.xml`, `widget_preview.xml`, `ic_widget_camera.xml`, `ic_widget_lock.xml`.
    - `res/layout/widget_taktak_2x2.xml`: Layout widget 2×2 RemoteViews.
    - `res/xml/widget_taktak_2x2_info.xml`: Provider meta-data.
    - `com.taktak.app.widget.WidgetUpdater.java`: Helper điều khiển RemoteViews, giải mã ảnh an toàn và quản lý WorkManager.
    - `com.taktak.app.widget.TaktakAppWidgetProvider.java`: BroadcastReceiver cho widget, strictly chỉ enqueue WorkManager.
    - `com.taktak.app.widget.WidgetUpdateWorker.java`: Worker tải khoảnh khắc mới nhất và cập nhật widget.
    - `TaktakMessagingService.java`: Bổ sung kích hoạt Worker khi có push `moment_created` và `moment_deleted`.
    - `SessionViewModel.java`: Bổ sung cập nhật trạng thái widget khi đăng xuất và khôi phục phiên.
    - `AndroidManifest.xml`: Khai báo receiver `TaktakAppWidgetProvider`.
    - Unit test `MomentCaptionTest.java` (4 tests mới).
    - Acceptance test `DeviceAcceptanceTest.java` (2 tests mới).

---

## 5. Điểm dừng và đề xuất tiếp theo

- Giai đoạn 6C đã hoàn tất 100% các tiêu chí nghiệm thu. Toàn bộ lộ trình Giai đoạn 6 (Firebase Cloud Messaging & Đồng bộ thời gian thực cho Widget 2×2) đã hoàn thiện và được kiểm chứng end-to-end trên thiết bị thực tế.
- **Đề xuất tiểu bước tiếp theo**: **Giai đoạn 7A — Màn lịch sử Đã gửi & Xem toàn màn hình (Sent Moments History & Full-screen Viewer)**.
  - Mở rộng màn hình Lịch sử đã có tab Đã nhận (5E), bổ sung thêm tab **Đã gửi** (`type=sent`).
  - Hiển thị danh sách ảnh người dùng đã gửi, phân trang `page=0, 1...`, sắp xếp mới nhất trước.
  - Xem chi tiết ảnh toàn màn hình, hiển thị caption và danh sách người nhận.
- Dừng chờ chỉ đạo và phê duyệt của người dùng trước khi triển khai Giai đoạn 7A.
