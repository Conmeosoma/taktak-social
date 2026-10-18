# Báo cáo QA — Giai đoạn 7A: Màn lịch sử Đã gửi & Xem toàn màn hình (Sent Moments History & Full-screen Viewer)

---

## 1. Thông tin chung

- **Tiểu bước roadmap**: 7A — Mở rộng `HistoryFragment` đã có tab Đã nhận ở 5E: thêm **Đã gửi**, phân trang và sort mới nhất; ảnh tải qua endpoint có xác thực; full-screen xem caption và sender.
- **Ngày thực hiện**: 18/10/2026.
- **Môi trường thử nghiệm**:
  - Máy chủ: Spring Boot 3.5.16, Temurin JDK 17, Embedded PostgreSQL 14.22.
  - Thiết bị kiểm thử: Android Emulator `Medium_Phone` (Android 17 / API 37, x86_64).
  - Kết nối: ADB reverse `tcp:8080 tcp:8080` tới Spring Boot `LocalSmokeServer`.
- **Trạng thái**: **PASS (100%)**.

---

## 2. Kiến trúc & Quyết định kỹ thuật

### 2.1. Backend (Spring Boot 3 / PostgreSQL)
1. **Endpoint `GET /api/moments?type=sent&page=0`**:
   - Truy vấn JPQL `sentMoments`: Lấy danh sách khoảnh khắc do chính user gửi (`m.sender_id = :senderId AND m.deleted_at IS NULL`).
   - Sắp xếp mới nhất lên đầu: `ORDER BY m.created_at DESC, m.id DESC`.
   - Phân trang: `LIMIT 21 OFFSET :offset` (kích thước trang 20; `hasNext = found.size() > 20`).
   - Header: `Cache-Control: no-store`.
2. **Tải ảnh có xác thực (`GET /api/moments/{id}/image`)**:
   - Sender được phân quyền tải trực tiếp ảnh do chính mình gửi qua token JWT Bearer hợp lệ.
   - Khoảnh khắc đã bị xóa mềm (`deleted_at IS NOT NULL`) lập tức bị từ chối với HTTP 404.

### 2.2. Android Client (Material 3 / Java)
1. **Quản lý đa tab (`TabLayout`)**:
   - `HistoryFragment` tích hợp `TabLayout` gồm 2 tab:
     - Tab 0: **Đã nhận** (`R.string.history_tab_received`) — Hiển thị ảnh bạn bè gửi đến.
     - Tab 1: **Đã gửi** (`R.string.history_tab_sent`) — Hiển thị ảnh người dùng đã gửi (bao gồm ảnh gửi bạn bè và ảnh lưu riêng tư).
   - Tiêu đề màn hình và trạng thái rỗng tự động điều chỉnh theo tab đang chọn, bảo toàn tương thích tuyệt đối với các bài kiểm thử tự động của 5E và 5A.
2. **Bộ nhớ đệm ảnh dùng chung (`HistoryImageCache`)**:
   - Xây dựng lớp bộ nhớ đệm an toàn đa luồng `HistoryImageCache` sử dụng `androidx.collection.LruCache` (giới hạn tối đa 20 MB hoặc 1/8 dung lượng heap).
   - Khi ảnh đã được tải về danh sách ở `HistoryFragment`, việc mở màn hình xem toàn màn hình sẽ lấy trực tiếp `Bitmap` từ cache, hiển thị tức thì 0 ms mà không cần gọi lại mạng hay nháy màn hình.
3. **Màn hình xem toàn màn hình (`MomentViewerActivity`)**:
   - Thiết kế chuẩn Material 3 Expressive trên nền tối cao cấp (`#0D0E11`), hỗ trợ `EdgeToEdge` an toàn với tai thỏ (cutout) và thanh điều hướng.
   - Hiển thị đầy đủ:
     - Nút đóng (`ic_close`) và cử chỉ vuốt/back hệ thống.
     - Avatar người gửi bo tròn (`friend_avatar`).
     - Tên hiển thị (`displayName`) và `@username`.
     - Thời gian tạo chuyển đổi theo múi giờ thiết bị (`HistoryTimeFormatter`).
     - Khung ảnh tỷ lệ 1:1 fit-center với góc bo mềm mại 24dp.
     - Khung chú thích (`viewerCaptionCard`) hiển thị caption đầy đủ (tối đa 50 ký tự Unicode).

---

## 3. Kết quả kiểm thử thực tế

### 3.1. Backend Suite (`mvn clean verify`)
- **Tổng số tests**: 123 tests.
- **Kết quả**: **123 / 123 tests PASS (100%)**.
- **Kịch bản mới bổ sung**:
  - `MomentIntegrationTest.listMomentsSentTabReturnsSentMomentsWithPaginationAndSorting`:
    - Upload 1 ảnh riêng tư + 20 ảnh gửi cho bạn bè.
    - Gọi `type=sent&page=0` -> trả về 20 items, `hasNext=true`, sắp xếp DESC.
    - Gọi `type=sent&page=1` -> trả về 1 item (ảnh riêng tư), `hasNext=false`.
    - Xác minh người gửi tải ảnh thành công với HTTP 200.
    - Xác minh người nhận chỉ thấy 20 ảnh trong tab `type=received` (ảnh riêng tư bị loại trừ).

### 3.2. Android Client Unit Tests (`testDebugUnitTest`)
- **Tổng số tests**: 62 tests.
- **Kết quả**: **62 / 62 tests PASS (100%)**.
- **Kịch bản mới bổ sung**:
  - `MomentCaptionTest.sentCallsListWithSentType`: Kiểm tra URL `/api/moments?type=sent&page=0`, header Authorization Bearer và phân giải DTO.
  - `MomentCaptionTest.sentRejectsNegativePageAndInvalidType`: Kiểm tra validation client-side từ chối page âm hoặc type không hợp lệ.

### 3.3. Android Build & Lint Packaging
- **Lệnh**: `assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://taktak.example.com/`
- **Kết quả**: **BUILD SUCCESSFUL, 0 Errors, 0 Warnings**.

### 3.4. Connected Device Acceptance Tests (`DeviceAcceptanceTest`)
Chạy trên Android Emulator `Medium_Phone` (Android 17 / API 37) kết nối `LocalSmokeServer` qua ADB reverse 8080:
| Test Method | Thời gian | Kết quả |
| :--- | :--- | :--- |
| `multiDeviceRegistrationForSameAccount` | 12.364s | **PASS** |
| `deviceRegistrationAndUnregistrationEndToEnd` | 1.529s | **PASS** |
| `latestMomentEndToEndWithBackend` | 2.474s | **PASS** |
| `pushEventBusDispatchesEventsToListeners` | 1.108s | **PASS** |
| `widgetUpdaterRendersAllStatesAndDecodesBitmap` | 1.657s | **PASS** |
| `momentUploadAndDeleteEndToEnd` | 2.189s | **PASS** |
| `sentMomentsEndToEndWithBackend` | 2.936s | **PASS** |
| `momentViewerActivityRendersAndCachesImage` | 1.049s | **PASS** |
- **Tổng cộng**: **8 / 8 tests PASS (100%)**.

---

## 4. Dọn dẹp môi trường

- `LocalSmokeServer`: Đã kết thúc an toàn, đóng cổng 8080.
- ADB Reverse: Đã gỡ bỏ toàn bộ quy tắc reverse (`adb reverse --remove-all`).
- Máy ảo `Medium_Phone`: Đã tắt an toàn qua `adb emu kill`, lưu trạng thái snapshot nguyên vẹn.
- Tất cả thay đổi được giữ nguyên vẹn trên working tree (uncommitted theo đúng quy định `AGENTS.md`).
