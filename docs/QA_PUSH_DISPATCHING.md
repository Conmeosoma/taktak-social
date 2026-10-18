# Taktak — Nghiệm thu Giai đoạn 6B: Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (Push Notification Dispatching)

Cập nhật: 18/10/2026. Mốc kiểm thử: **Giai đoạn 6B — Bắn thông báo đẩy khi tạo/xóa khoảnh khắc: PASS local/AVD**.

Đối chiếu quy tắc: tuân thủ `AGENTS.md`, `docs/TAKTAK_PROJECT.md`, `docs/PROJECT_DECISIONS.md`. Không tự động commit, push, merge hoặc deploy. Dừng trước Giai đoạn 6C (AppWidgetProvider & WorkManager Widget).

---

## 1. Thiết kế kiến trúc và giải pháp kỹ thuật

Giai đoạn 6B triển khai toàn bộ luồng phát và xử lý tín hiệu thông báo đẩy thời gian thực (real-time push notification) khi người dùng tạo hoặc xóa khoảnh khắc, bảo đảm tuyệt đối tính an toàn, bảo mật và hiệu năng.

### 1.1. Backend Spring Boot: Phát thông báo đẩy thời gian thực
- **Cơ chế sau Commit (`TransactionSynchronization.afterCommit`)**:
  - Khi người dùng đăng ảnh (`upload`) hoặc xóa ảnh (`delete`), thông báo push chỉ được kích hoạt **sau khi giao dịch cơ sở dữ liệu đã commit thành công**.
  - Nếu việc gửi push qua FCM gặp lỗi mạng hoặc thất bại, giao dịch lưu/xóa ảnh **không bao giờ bị rollback** (tuân thủ nguyên tắc nhất quán dữ liệu của Taktak).
- **Phát sự kiện `moment_created`**:
  - Khi ảnh được gửi tới bạn bè (`targets` không rỗng):
    - Server truy vấn toàn bộ token FCM đang hoạt động của danh sách người nhận qua `deviceTokenRepository.findAllByUserIdIn(targets)`.
    - Sau commit, server bắn FCM data message gọn nhẹ tới từng token với payload:
      ```json
      {
        "type": "moment_created",
        "momentId": "<moment_id>"
      }
      ```
  - **Ảnh chỉ mình tôi (`recipientIds=[]`)**: Không gửi bất kỳ thông báo push nào.
  - **Idempotency Replay**: Nếu request upload là gửi lại (replay) của cùng một `Idempotency-Key` và payload, server trả về 201 với ID cũ và **không phát thông báo push trùng lặp**.
- **Endpoint và Luồng xóa ảnh `DELETE /api/moments/{id}`**:
  - Chỉ người sở hữu (sender) mới có quyền xóa (`403 FORBIDDEN` nếu không phải chủ sở hữu).
  - Đánh dấu `deleted_at = now()` trên bản ghi `moments` (soft delete).
  - Xóa file ảnh an toàn khỏi private storage VPS (`storage.discard(moment.getFilePath())`).
  - Truy vấn danh sách người nhận (`findRecipientIds(momentId)`) và bắn FCM data message:
    ```json
    {
      "type": "moment_deleted",
      "momentId": "<moment_id>"
    }
    ```
  - Sau khi xóa, tất cả các yêu cầu tải ảnh `GET /api/moments/{id}/image` đều bị từ chối với mã lỗi `404 NOT_FOUND` cho cả sender lẫn recipient. Ảnh bị xóa cũng biến mất ngay khỏi kết quả `GET /api/moments?type=received`.
- **Bảo mật Push Payload**:
  - Tuyệt đối **không** gửi URL ảnh, caption, hay Bearer token qua FCM.
  - Push message chỉ đóng vai trò là "tín hiệu có thay đổi" (change signal) đánh thức client.
- **Tự động dọn dẹp Stale Token**:
  - Khi Firebase Admin SDK phản hồi lỗi `MessagingErrorCode.UNREGISTERED` hoặc `INVALID_ARGUMENT`, server tự động xóa token hết hạn khỏi bảng `device_tokens` trong DB thông qua truy vấn `@Transactional deleteByFcmToken`.

### 1.2. Android Client: Đón nhận sự kiện & Đồng bộ UI tức thì
- **Bus sự kiện tập trung (`PushEventBus.java`)**:
  - Triển khai singleton `PushEventBus` dạng thread-safe sử dụng `CopyOnWriteArrayList<Listener>`.
  - Hỗ trợ các component trong app đăng ký/hủy đăng ký nhận sự kiện `onMomentCreated(String momentId)` và `onMomentDeleted(String momentId)`.
  - Xử lý cô lập ngoại lệ: Nếu một listener gặp lỗi, các listener khác vẫn được gọi đầy đủ.
- **Xử lý trong `TaktakMessagingService.java`**:
  - Hàm `onMessageReceived(RemoteMessage remoteMessage)` trích xuất trường `type` và `momentId` từ `remoteMessage.getData()`.
  - Kích hoạt `PushEventBus.emitMomentCreated(momentId)` hoặc `PushEventBus.emitMomentDeleted(momentId)`.
- **Tự động phản ứng trong `HistoryViewModel.java`**:
  - Đăng ký listener với `PushEventBus` trong constructor và hủy đăng ký trong `onCleared()`.
  - Khi nhận `onMomentCreated`: Nếu danh sách đã tải (`loaded`) và không bận (`!busy`), ViewModel tự động gọi `refresh()` để nạp khoảnh khắc mới nhất về hiển thị cho người dùng.
  - Khi nhận `onMomentDeleted`: ViewModel tự động tìm và xóa khoảnh khắc khỏi danh sách `moments`, xóa ảnh trong RAM `imageCache`, và phát LiveData cập nhật giao diện ngay lập tức mà không cần gọi lại mạng.
- **Mở rộng API & Repository Client**:
  - `MomentApi`: Thêm `@DELETE("api/moments/{id}") Call<Void> deleteMoment(@Path("id") String id);`.
  - `MomentRepository`: Thêm phương thức `delete(String id)` kiểm tra format UUID, xác thực session hiện tại và gọi API xóa.

---

## 2. Kết quả kiểm tra thực tế

### 2.1. Backend Integration Tests (Spring Boot)
- Thực hiện: `.\mvnw.cmd clean verify` trên môi trường JDK 17.
- Kết quả: **118/118 tests PASS**, 0 failures, 0 errors, 0 skipped.
- Chi tiết 7 integration tests mới trong `MomentPushIntegrationTest.java`:
  1. `momentCreationWithRecipientsDispatchesFcmDataMessageToAllRecipientDevices`: Người gửi A đăng ảnh cho B, B có 2 thiết bị (phone và tablet) → cả 2 token đều nhận đúng data message `{type: "moment_created", momentId: "..."}`, không chứa URL/caption/token.
  2. `ownerOnlyMomentDoesNotDispatchPushMessage`: Đăng ảnh chế độ "chỉ mình tôi" (`recipientIds=[]`) → không phát push message nào.
  3. `idempotentUploadReplayDoesNotDispatchDuplicatePushMessage`: Replay request upload với cùng `Idempotency-Key` và payload → không phát thêm push message trùng.
  4. `momentDeletionBySenderDispatchesDeletedPushAndRemovesFileAndRevokesAccess`: Người gửi xóa ảnh → nhận `204 No Content`, file ảnh trên đĩa bị xóa, token của recipient nhận `{type: "moment_deleted"}`, các request GET ảnh hoặc danh sách đều bị chặn (404 / loại khỏi danh sách).
  5. `momentDeletionByNonSenderIsForbiddenAndDoesNotSendPushOrDeleteFile`: Người nhận cố tình gọi DELETE ảnh của người gửi → bị từ chối `403 FORBIDDEN`, file vẫn an toàn, không phát push.
  6. `deletingAlreadyDeletedMomentReturnsNotFound`: Xóa lại ảnh đã bị xóa trước đó → trả về `404 NOT_FOUND`.
  7. `deleteByFcmTokenRemovesStaleTokenFromDatabase`: Kiểm tra xóa token FCM không hợp lệ/hết hạn khỏi bảng `device_tokens`.
- Đóng gói: Executable JAR `taktak-server-0.0.1-SNAPSHOT.jar` repackage thành công.

### 2.2. Android JVM Unit Tests
- Thực hiện: `./gradlew.bat testDebugUnitTest` với JDK 17 (Android Studio JBR).
- Kết quả: **56/56 tests PASS** (tăng thêm 7 tests mới), 0 failures, 0 errors.
  - 4 tests mới trong `PushEventBusTest`: Đăng ký/nhận sự kiện, hủy đăng ký, lọc ID rỗng/null, cô lập ngoại lệ giữa các listener.
  - 3 tests mới trong `MomentCaptionTest`: Gọi DELETE endpoint đúng path/headers, validate UUID, ánh xạ mã lỗi 403/404.

### 2.3. Android Build & Lint
- Thực hiện: `./gradlew.bat assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.taktak.test/`
- Kết quả: **BUILD SUCCESSFUL** trong 17 giây, **0 errors**.

### 2.4. Connected Acceptance Tests trên Android Emulator (AVD)
- Môi trường: Android Emulator `Medium_Phone` (Android 17 / API 37, x86_64), reverse port 8080 tới LocalSmokeServer.
- Runner: `Run-AcceptanceTests.ps1 -Class com.taktak.app.DeviceAcceptanceTest`
- Kết quả: **4/4 tests PASS (100% success rate)** trong 27.171 giây.
  1. `deviceRegistrationAndUnregistrationEndToEnd`: PASSED (2.175s)
  2. `momentUploadAndDeleteEndToEnd`: PASSED (2.193s) — Upload ảnh thật, xác thực upload trả về ID, gọi DELETE xóa ảnh thành công 204.
  3. `multiDeviceRegistrationForSameAccount`: PASSED (21.087s)
  4. `pushEventBusDispatchesEventsToListeners`: PASSED (1.716s) — Dispatch sự kiện created/deleted và nhận phản hồi tức thì qua Bus.
- Dọn dẹp: LocalSmokeServer dừng exit 0, cổng 8080 đóng, quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`.

---

## 3. Trạng thái mã nguồn và Git

- Nhánh: `chore/push-current-project`
- HEAD: `ed9fde6` (bảo toàn toàn bộ các thay đổi chưa commit, không commit/push/deploy).
- Các file mới và sửa đổi thuộc 6B:
  - Backend:
    - Entity `Moment.java`: Thêm method `markDeleted(Instant deletedAt)`.
    - Repository `MomentRepository.java`: Thêm query `findRecipientIds`.
    - Repository `DeviceTokenRepository.java`: Thêm `@Transactional` trên các query `@Modifying`.
    - Component `FirebasePushNotificationSender.java`: Thêm ghi nhận tin nhắn (`recordedMessages`) phục vụ kiểm thử và audit.
    - Service `MomentService.java`: Bổ sung kích hoạt FCM sau commit ở `upload`, thêm method `delete(senderId, momentId)`.
    - Controller `MomentController.java`: Thêm `@DeleteMapping("/{id}")`.
    - Test `MomentPushIntegrationTest.java`: 7 integration tests kiểm thử toàn diện push notification và xóa ảnh.
    - Test `LocalSmokeServer.java`: Sửa điều kiện đếm moment đang hoạt động (`deleted_at IS NULL`) đồng bộ với số file ảnh trên đĩa.
  - Android:
    - `PushEventBus.java`: Singleton event bus thread-safe.
    - `TaktakMessagingService.java`: Dispatch `moment_created` và `moment_deleted` tới bus.
    - `HistoryViewModel.java`: Tự động làm mới khi có ảnh mới và tự động xóa ảnh khi có sự kiện ảnh bị xóa.
    - `MomentApi.java` & `MomentRepository.java`: Thêm hàm `delete(String id)`.
    - Unit test `PushEventBusTest.java` (4 tests).
    - Unit test `MomentCaptionTest.java` (3 tests mới cho DELETE).
    - Acceptance test `DeviceAcceptanceTest.java` (2 tests mới).

---

## 4. Điểm dừng và đề xuất tiếp theo

- Giai đoạn 6B đã hoàn thành 100% các tiêu chí nghiệm thu về cơ chế phát và đón nhận thông báo đẩy cho vòng đời khoảnh khắc (tạo mới, xóa ảnh, dọn dẹp token, tự động làm mới giao diện).
- **Đề xuất tiểu bước tiếp theo**: **Giai đoạn 6C — AppWidgetProvider 2×2 & WorkManager Widget Sync**.
  - Xây dựng App Widget kích thước 2×2 hiển thị ảnh mới nhất và tên người gửi.
  - Tuân thủ kiến trúc: `onUpdate` của `AppWidgetProvider` **chỉ enqueue WorkManager**, tuyệt đối không gọi mạng trực tiếp.
  - `TaktakMessagingService` khi nhận push sự kiện sẽ enqueue WorkManager để tải ảnh mới và cập nhật widget.
- Dừng chờ chỉ đạo và phê duyệt của người dùng trước khi triển khai 6C.
