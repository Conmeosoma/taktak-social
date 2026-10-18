# Báo Cáo Kiểm Thử & Nghiệm Thu — Giai đoạn 7B: Xóa khoảnh khắc phía máy chủ & Thu hồi quyền tức thời

## 1. Mục Tiêu Kiểm Thử
- **Backend**:
  - `DELETE /api/moments/{id}`: Chỉ người gửi (sender) mới có quyền xóa (`403 FORBIDDEN` đối với non-sender).
  - Thu hồi quyền tức thời: Cập nhật `deleted_at = now()`, xóa an toàn file ảnh trên đĩa private VPS (`storage.discard(filePath)`). Mọi request `GET /{id}/image` mới lập tức trả về `404 NOT_FOUND` cho cả sender và recipients.
  - Sau commit transaction, phát FCM data payload `{type: "moment_deleted", momentId: id}` tới tất cả thiết bị của người nhận để kích hoạt đồng bộ nền.
  - Danh sách `GET /api/moments/latest`, `GET /api/moments?type=received`, và `GET /api/moments?type=sent` loại bỏ hoàn toàn các khoảnh khắc đã xóa mềm.
- **Android Client**:
  - `MomentViewerActivity`: Hiển thị icon xóa thùng rác (`ic_delete.xml`) ở góc phải trên cùng khi `isSender == true`.
  - Hộp thoại xác nhận MaterialAlertDialog: Người dùng xác nhận trước khi thực hiện xóa.
  - Vô hiệu hóa cache an toàn: Khi xóa thành công, loại bỏ khỏi `HistoryImageCache`, phát sự kiện `PushEventBus.emitMomentDeleted(momentId)` (cập nhật danh sách UI ngay lập tức), và kích hoạt `WidgetUpdater.enqueueOneTimeWork(context)` để widget đồng bộ ảnh kế tiếp hoặc placeholder.
  - Xử lý Offline / Lỗi mạng: Khi mất kết nối hoặc server không phản hồi, hiển thị thông báo lỗi thân thiện, **bảo toàn cache và danh sách hiển thị** (không tự xóa local), cho phép người dùng thử lại khi có mạng.

---

## 2. Kết Quả Kiểm Thử Thực Tế

### A. Backend Unit & Integration Tests (`server/`)
- Lệnh chạy: `.\mvnw.cmd clean test`
- Kết quả: **123 tests run, 0 failures, 0 errors, BUILD SUCCESS**.
- Các test case nổi bật trong `MomentPushIntegrationTest.java`:
  - `momentDeletionBySenderDispatchesDeletedPushAndRemovesFileAndRevokesAccess`: **PASS** (xóa thành công -> phát FCM -> xóa file đĩa -> GET ảnh trả về 404 cho cả sender & recipient).
  - `momentDeletionByNonSenderIsForbiddenAndDoesNotSendPushOrDeleteFile`: **PASS** (non-sender xóa trả về 403, file nguyên vẹn, không gửi FCM).
  - `deletingAlreadyDeletedMomentReturnsNotFound`: **PASS** (xóa lần 2 trả về 404).
  - `latestMomentSkipsDeletedMoments`: **PASS** (loại bỏ ảnh đã xóa mềm, fallback về ảnh trước đó hoặc 204).

### B. Android Unit Tests (`android-client/`)
- Lệnh chạy: `.\gradlew.bat testDebugUnitTest`
- Kết quả: **BUILD SUCCESSFUL**.
- Các test case kiểm thử trong `MomentCaptionTest.java`:
  - `deleteCallsDeleteEndpointWithAuthorization`: **PASS** (gọi đúng DELETE `/api/moments/{id}` kèm JWT bearer header).
  - `deleteRejectsInvalidUuid`: **PASS** (từ chối UUID không hợp lệ, ném `ApiFailure.Kind.VALIDATION`).
  - `deleteThrowsUnauthorizedWhenNoSession`: **PASS** (ném `ApiFailure.Kind.UNAUTHORIZED` khi chưa đăng nhập).
  - `deletePropagatesServerFailureWhenForbiddenOrNotFound`: **PASS** (bảo toàn status code 403/404 khi server từ chối).
- Unit tests `PushEventBusTest.java`: **PASS** (emit và dispatch `onMomentDeleted` chuẩn xác tới các listeners).

### C. Android Connected Acceptance Tests trên Thiết Bị Ảo (`Medium_Phone`)
- Lệnh chạy: `.\scripts\Run-AcceptanceTests.ps1` kết nối `LocalSmokeServer` qua `adb reverse tcp:8080 tcp:8080`.
- Kết quả: **10/10 test cases PASS (BUILD SUCCESSFUL)**:
  1. `momentDeletionBySenderRevokesRecipientAccessImmediately`: **PASS** (2.206s)
     - Sender tải ảnh -> Bob nhận ảnh -> Bob thử xóa -> nhận `403 FORBIDDEN`.
     - Sender xóa -> thành công.
     - Ngay lập tức cả Bob và Sender tải ảnh -> nhận `404 NOT_FOUND`.
     - Moment lập tức biến mất khỏi danh sách `sent` của Sender và `received` của Bob.
  2. `momentDeletionNetworkFailurePreservesLocalCache`: **PASS** (0.855s)
     - Giả lập mạng ngắt kết nối (unreachable server).
     - Gọi `delete()` ném lỗi mạng.
     - Xác nhận `HistoryImageCache` và dữ liệu local không bị xóa sai lệch, người dùng có thể thử lại khi có kết nối mạng.
  3. `sentMomentsEndToEndWithBackend`: **PASS** (1.830s)
  4. `momentViewerActivityRendersAndCachesImage`: **PASS** (0.844s)
  5. `latestMomentEndToEndWithBackend`: **PASS** (1.938s)
  6. `widgetUpdaterRendersAllStatesAndDecodesBitmap`: **PASS** (0.942s)
  7. `deviceRegistrationAndUnregistrationEndToEnd`: **PASS** (0.819s)
  8. `multiDeviceRegistrationForSameAccount`: **PASS** (3.670s)
  9. `pushEventBusDispatchesEventsToListeners`: **PASS** (0.820s)
  10. `momentUploadAndDeleteEndToEnd`: **PASS** (0.868s)

### D. Android Build & Lint Verification
- `assembleDebug`: **BUILD SUCCESSFUL** (15s).
- `assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://taktak.example.com/`: **BUILD SUCCESSFUL** (33s, không có lỗi lint).

---

## 3. Trạng Thái & Tiêu Chí Nghiệm Thu
- [x] Endpoint `DELETE /api/moments/{id}` bảo mật, chỉ sender được phép xóa.
- [x] Thu hồi quyền tức thời, xóa file vật lý private VPS, trả về 404 cho các request tiếp theo.
- [x] FCM dispatch `moment_deleted` hoạt động sau transaction commit.
- [x] Giao diện xem toàn màn hình có nút xóa khi là người gửi, có dialog xác nhận Material Design.
- [x] Xử lý offline an toàn: báo lỗi kết nối mạng, không xóa cache bừa bãi khi mạng gặp sự cố.
- [x] Trạng thái nghiệm thu: **PASS**.
