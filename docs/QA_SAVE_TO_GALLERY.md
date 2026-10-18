# Báo Cáo Kiểm Thử & Nghiệm Thu — Giai đoạn 7C: Tải ảnh vào bộ sưu tập thiết bị (Save to Device Gallery)

## 1. Mục Tiêu Kiểm Thử
- **Yêu cầu hành động người dùng (User Action Required)**:
  - Ảnh riêng tư và khoảnh khắc nhận được **không bao giờ** tự động xuất ra thư viện ảnh (Gallery) của thiết bị khi tải về hay hiển thị.
  - Chỉ lưu vào bộ sưu tập khi người dùng chủ động nhấn nút tải ảnh (`viewerSaveButton`).
- **Tương thích MediaStore & Phân quyền theo phiên bản Android**:
  - Khai báo `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` trong `AndroidManifest.xml`.
  - Android 10+ (API 29+): Sử dụng Scoped Storage qua `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`, `RELATIVE_PATH = "Pictures/Taktak"`, cờ `IS_PENDING = 1` trong lúc ghi và `IS_PENDING = 0` khi hoàn tất, không cần xin quyền runtime.
  - Android 8–9 (API 26–28): Kiểm tra và yêu cầu quyền runtime `WRITE_EXTERNAL_STORAGE` trước khi lưu vào thư mục `Pictures/Taktak`.
- **Giao diện & Trải nghiệm (UI/UX)**:
  - Nút lưu ảnh (`ic_download.xml`, `viewerSaveButton`) hiển thị trên thanh tiêu đề `viewerTopBar` trong `MomentViewerActivity` cho cả người gửi lẫn người nhận.
  - Luồng lưu nền không gây giật lag UI (chạy trên worker thread), tự động tải bytes JPEG gốc từ server hoặc nén từ RAM cache nếu offline.
  - Hiển thị Toast thông báo trạng thái trực quan: "Đang lưu ảnh…", "Đã lưu ảnh vào Bộ sưu tập.", "Không thể lưu ảnh vào máy. Vui lòng thử lại."

---

## 2. Kết Quả Kiểm Thử Thực Tế

### A. Android Unit & Build Tests (`android-client/`)
- Lệnh chạy: `.\gradlew.bat testDebugUnitTest`
- Kết quả: **66/66 unit tests PASS**, BUILD SUCCESSFUL trong 13s.
- `assembleDebug`: **BUILD SUCCESSFUL** (15s).
- `assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://taktak.example.com/`: **BUILD SUCCESSFUL** trong 27s, **0 errors, 0 lint warnings**.

### B. Android Connected Acceptance Tests trên Thiết Bị Ảo (`Medium_Phone` - Android 17 / API 37)
- Lệnh chạy: `.\scripts\Run-AcceptanceTests.ps1` kết nối `LocalSmokeServer` qua `adb reverse tcp:8080 tcp:8080`.
- Kết quả: **12/12 test cases PASS (100% success rate)** trong 2m 5s:
  1. `saveMomentToGalleryOnUserActionSavesValidJpegToMediaStore`: **PASS** (3.493s)
     - Giả lập hành động người dùng bấm lưu ảnh.
     - Lưu thành công Bitmap/JPEG vào `MediaStore` với thư mục `Pictures/Taktak`.
     - Đọc lại dữ liệu từ `MediaStore` ContentResolver, giải mã Bitmap thành công với đúng kích thước gốc 80×80.
     - Dọn dẹp Uri an toàn sau kiểm thử.
  2. `emptyJpegBytesThrowsIOExceptionOnGallerySave`: **PASS** (1.561s)
     - Kiểm tra trường hợp byte rỗng, ném `IOException` chuẩn xác.
  3. `momentDeletionBySenderRevokesRecipientAccessImmediately`: **PASS** (4.594s)
  4. `momentDeletionNetworkFailurePreservesLocalCache`: **PASS** (2.308s)
  5. `sentMomentsEndToEndWithBackend`: **PASS** (4.133s)
  6. `momentViewerActivityRendersAndCachesImage`: **PASS** (2.707s)
  7. `widgetUpdaterRendersAllStatesAndDecodesBitmap`: **PASS** (1.929s)
  8. `latestMomentEndToEndWithBackend`: **PASS** (5.989s)
  9. `multiDeviceRegistrationForSameAccount`: **PASS** (23.240s)
  10. `deviceRegistrationAndUnregistrationEndToEnd`: **PASS** (2.215s)
  11. `pushEventBusDispatchesEventsToListeners`: **PASS** (1.615s)
  12. `momentUploadAndDeleteEndToEnd`: **PASS** (2.405s)

### C. Dọn dẹp môi trường
- Smoke server dừng an toàn, đóng cổng 8080.
- Quy tắc ADB reverse đã gỡ bỏ (`adb reverse --remove-all`).
- Android Emulator đã dừng an toàn qua `adb emu kill` (exit 0).

---

## 3. Trạng Thái & Tiêu Chí Nghiệm Thu
- [x] Lưu ảnh về máy bằng Android MediaStore chuẩn Scoped Storage.
- [x] Quản lý quyền theo phiên bản Android (maxSdkVersion 28 cho quyền cũ, Scoped Storage trên Android 10+).
- [x] Ảnh riêng tư không bao giờ tự động lưu ra Gallery khi chưa có thao tác người dùng.
- [x] Nút lưu ảnh hoạt động mượt mà trên giao diện xem toàn màn hình.
- [x] Trạng thái nghiệm thu: **PASS**.
