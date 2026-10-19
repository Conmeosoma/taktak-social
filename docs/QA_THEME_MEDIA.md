# UI-THEME-06 — Camera, Preview, Navigation và Viewer bốn theme

Ngày 19/10/2026. Nghiệm thu bước UI-THEME-06 theo roadmap nâng cấp giao diện đa chủ đề Taktak. Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho các vai trò màn hình Media (`MAIN`, `PREVIEW`, `VIEWER`), bao gồm thanh điều hướng, CameraX 1:1, Preview bản nháp & người nhận, và Moment Viewer.

## 1. Phạm vi và Triển khai

Đã đối chiếu `AGENTS.md`, `TAKTAK_PROJECT.md`, `PROJECT_DECISIONS.md`, các bài học từ UI-THEME-01 đến 05 và mã nguồn hiện hữu trước khi thực hiện.

- **Theme Resolver cho Media**: Mở rộng `ThemeManager.ScreenRole` cho `MAIN`, `PREVIEW`, `VIEWER`. Cung cấp đầy đủ style tokens và color palettes trong `res/values/media_themes.xml` cho cả 4 phong cách:
  - `Original`: Giữ nhận diện thương hiệu dark cố định, điểm nhấn hồng neon (`#FF2E93`) và icon Material cổ điển.
  - `Gradient`: Nền dynamic chuyển màu hài hòa, contrast ink đạt chuẩn WCAG, nút chụp ảnh và navigation indicator nổi bật.
  - `Glass`: Phong cách kính mờ tinh tế, tấm nền plate bán trong suốt, hỗ trợ cả Light Mode và Dark Mode.
  - `Soft`: Phong cách tối giản, êm dịu, tương phản màu chữ cao, tối ưu khả năng đọc ngoài trời.
- **Nền native MediaBackdropView**: Vẽ native bằng Canvas shader, không chụp hay làm mờ bitmap nhằm tối ưu 60fps và tiết kiệm RAM trên thiết bị di động.
- **Bảo toàn nghiệp vụ Media & Camera**:
  - Tỷ lệ khung hình 1:1 vuông được bảo toàn tuyệt đối cho cả Preview CameraX, ảnh chụp nháp và ảnh hiển thị trong Viewer.
  - Caption UTF-8 tối đa 50 ký tự được giới hạn chuẩn xác; danh sách bạn bè nhận ảnh với checkbox chọn tất cả / từng người nhận hoạt động mượt mà.
  - Cơ chế Idempotency Key, upload progress, retry khi mạng lỗi, và xóa bản nháp khi Retake được bảo toàn 100%. Không tải lên khi user nhấn chụp lại.
  - Cơ chế bảo mật và thu hồi quyền (403/401/404): Moment Viewer ẩn hoàn toàn ảnh và tên người gửi khi bị thu hồi, vô hiệu hóa các nút Lưu/Xóa, không rò rỉ dữ liệu riêng tư.
- **Tối ưu Responsive & Khả năng tiếp cận**:
  - `activity_moment_viewer.xml` được bổ sung container `ScrollView` responsive, bảo đảm nội dung hiển thị chuẩn xác ở mọi kích thước màn hình từ điện thoại nhỏ (280×560dp) đến chế độ nằm ngang cực hạn (840×420dp) và tỷ lệ font trợ năng 2.0x (200%), triệt tiêu hoàn toàn lỗi tràn viền hoặc nén chiều cao văn bản.

## 2. Các file triển khai của UI-THEME-06

### Mã nguồn Android Client (`android-client/app/src/main/`):
- **Java**:
  - `ui/theme/ThemeManager.java`: Tích hợp các screen role `MAIN`, `PREVIEW`, `VIEWER`.
  - `ui/theme/MediaBackdropView.java`: Component render background native cho Media screens.
  - `ui/history/MomentViewerActivity.java`: Khởi tạo theme role `VIEWER`, tích hợp xử lý giao diện an toàn.
- **XML**:
  - `res/values/media_themes.xml`: Định nghĩa toàn bộ theme, style attributes và color tokens cho Media.
  - `res/layout/activity_main.xml`: Bổ sung `MediaBackdropView`, nâng cấp styling `bottomNavigation`.
  - `res/layout/fragment_camera_page.xml` & `fragment_camera.xml`: Giao diện chụp ảnh CameraX tỷ lệ 1:1, nút shutter và nút chuyển camera/flash responsive.
  - `res/layout/activity_preview.xml`: Giao diện xem trước ảnh nháp, chọn người nhận, nhập caption.
  - `res/layout/activity_moment_viewer.xml`: Giao diện chi tiết ảnh Moment với top bar, center square plate và caption card bên trong `ScrollView`.
  - `res/color/media_navigation.xml` & `res/drawable/media_*.xml`: Định nghĩa màu sắc và drawable navigation.

### Kiểm thử UI Instrumentation (`android-client/app/src/androidTest/`):
- `ui/theme/MediaThemeUiTest.java`: Bộ test suite tự động kiểm thử toàn diện 5 kịch bản giao diện Media:
  1. `allStylesModesLayoutsAndOriginalTabContexts`: Kiểm tra 144 tổ hợp layout (4 styles × 2 modes × 3 fontScales × 3 viewports), xác minh text bounds và độ tương phản >= 4.5:1.
  2. `previewDraftCaptionSelectionFrozenRetryKeyAndImageSurviveRecreation`: Kiểm tra trạng thái bản nháp, caption, khóa người nhận khi upload, retry key và khôi phục khi recreate.
  3. `cameraNavigationAndPendingThemeKeepActivityTabAndSettings`: Kiểm tra 4 tabs điều hướng, giữ nguyên cài đặt CameraX (lens facing, flash mode) khi đổi theme.
  4. `oneApprovedRealCaptureToCacheAndRetakeWithoutUpload`: Kiểm tra chụp ảnh thật vào cache local, kiểm tra kích thước 1:1, hủy bỏ khi retake không gọi API.
  5. `viewerAllStylesModesDialogAndRevocationKeepPrivatePhotoHidden`: Kiểm tra 4 theme viewer, dialog xác nhận xóa ảnh, và cơ chế bảo mật ẩn ảnh khi thu hồi quyền.

## 3. Kết quả Kiểm thử Thực tế

Chạy trên Android Emulator AVD `Medium_Phone` (Android 17 / API 37) ở chế độ headless:

```
> Task :app:connectedDebugAndroidTest
Running tests on devices: Medium_Phone(AVD) - 17

[TEST RESULT]
- allStylesModesLayoutsAndOriginalTabContexts: PASS (15.351s)
- previewDraftCaptionSelectionFrozenRetryKeyAndImageSurviveRecreation: PASS (29.771s)
- cameraNavigationAndPendingThemeKeepActivityTabAndSettings: PASS (47.450s)
- oneApprovedRealCaptureToCacheAndRetakeWithoutUpload: PASS (7.418s)
- viewerAllStylesModesDialogAndRevocationKeepPrivatePhotoHidden: PASS (38.257s)

BUILD SUCCESSFUL in 2m 41s
Total: 5/5 tests PASS (0 failures, 0 errors, 0 skipped) - 100% XANH
```

### Kiểm tra Hồi quy Toàn hệ thống (Regression Verification):
- **Android JVM Unit Tests**: `gradlew test` -> **105/105 tests PASS** (0 failures, 0 skipped, 1.744s).
- **Backend Spring Boot Tests**: `mvn test` -> **152/152 tests PASS** (0 failures, 0 errors, 0 skipped, 1m 55s).

## 4. Trạng thái & Bàn giao

- **Kết luận**: **PASS TOÀN DIỆN UI-THEME-06**.
- Toàn bộ thay đổi mã nguồn đã được kiểm tra, không có regression, không làm ảnh hưởng tới các chức năng đã nghiệm thu trước đó (Feature A - Quên mật khẩu Gmail, Feature B - Validation đăng ký đầy đủ, UI-THEME-01..05).
- Emulator AVD đã được đóng an toàn sau khi hoàn tất kiểm thử, giải phóng RAM hệ thống.
- Tuân thủ quy tắc `AGENTS.md`: Không tự ý commit, push hay deploy. Dừng lại báo cáo và chờ hướng dẫn tiếp theo từ người dùng.
