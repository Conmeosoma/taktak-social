# UI-THEME-08 — Profile screen, Avatar upload & Logout bốn theme

Ngày 20/10/2026. Nghiệm thu bước UI-THEME-08 theo roadmap nâng cấp giao diện đa chủ đề Taktak. Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Hồ sơ cá nhân (`fragment_profile.xml`), bao gồm thông tin tài khoản, chỉnh sửa tên hiển thị (display name), tải lên ảnh đại diện (avatar upload), chuyển giao diện (Theme settings) và đăng xuất an toàn (logout).

## 1. Phạm vi và Triển khai

Đã đối chiếu `AGENTS.md`, `TAKTAK_PROJECT.md`, `PROJECT_DECISIONS.md`, các bài học từ UI-THEME-01 đến 07 và mã nguồn hiện hữu trước khi thực hiện.

- **Theme & Token Architecture cho Profile**:
  - Gỡ bỏ hardcoded `@style/Theme.TakTak.Premium` tại `fragment_profile.xml`, cho phép tab Profile tự động thừa hưởng Context Theme của Activity theo phong cách người dùng đã chọn (Original, Gradient, Glass, Soft).
  - Bổ sung attribute và style cho Outline/Secondary Button:
    - `<attr name="ttMediaOutlineStyle" format="reference" />` khai báo trong `res/values/media_themes.xml`.
    - Định nghĩa `@style/Widget.TakTakMedia.Outline` với `android:background="?attr/ttLoginSecondary"` và `android:textColor="?attr/colorOnSurface"`.
    - Cấu hình `ttMediaOutlineStyle` trỏ về `@style/Widget.TakTakPremium.Outline` trên Original; trỏ về `@style/Widget.TakTakMedia.Outline` trên Gradient, Glass, Soft.
    - Khai báo fallback mặc định `<item name="ttMediaOutlineStyle">@style/Widget.TakTakPremium.Outline</item>` trong `Base.Theme.TakTak` (`res/values/themes.xml`).
  - Áp dụng các token theme động vào `fragment_profile.xml`:
    - Tiêu đề màn hình `profileTitle`: `android:textAppearance="?attr/ttMediaHeadlineStyle"`.
    - Viền ảnh đại diện `profileAvatar`: `app:strokeColor="?attr/colorPrimary"`.
    - Nút camera nhỏ trên ảnh đại diện `profileAvatarEdit`: `app:iconTint="?attr/colorOnPrimary"`, `app:backgroundTint="?attr/colorPrimary"`.
    - Nút đổi ảnh đại diện `profileChangeAvatar`: `style="?attr/ttMediaTonalStyle"`.
    - Ô nhập tên `profileNameLayout`: `style="?attr/ttMediaFieldStyle"`, `profileNameInput` có `android:theme="?attr/ttMediaInputStyle"`.
    - Nút Lưu `profileSave`: `style="?attr/ttMediaPrimaryStyle"`.
    - Nút Đăng xuất `profileLogout`: `style="?attr/ttMediaOutlineStyle"`.
    - Nút Tải lại `profileRetry` và Đổi giao diện `profileThemeButton`: `@style/Widget.TakTakM3.TextButton` dùng `?attr/colorPrimary`.
- **Bảo toàn nghiệp vụ và Bảo mật**:
  - Giữ nguyên toàn bộ logic tải thông tin cá nhân, sửa tên hiển thị có validation, tải ảnh đại diện qua `AvatarPhoto.read` (kiểm tra kích thước, xoay theo EXIF, nén vuông chuẩn 500-800KB).
  - Giữ nguyên quy trình xóa phiên và hủy đăng ký thiết bị khi đăng xuất: `DeviceRepository.unregister()`, `SessionManager.logout()`, `WidgetUpdater.renderLoggedOut()`.
  - Không thay đổi nghiệp vụ server, cơ sở dữ liệu, JWT, FCM hay widget.

## 2. Các file triển khai của UI-THEME-08

### Mã nguồn Android Client (`android-client/app/src/main/`):
- `res/values/media_themes.xml`: Khai báo `ttMediaOutlineStyle` và định nghĩa style `Widget.TakTakMedia.Outline` trên 4 theme.
- `res/values/themes.xml`: Khai báo fallback `ttMediaOutlineStyle` an toàn trong `Base.Theme.TakTak`.
- `res/layout/fragment_profile.xml`: Gỡ hardcoded theme, áp dụng các token theme động (`ttMediaHeadlineStyle`, `ttMediaTonalStyle`, `ttMediaFieldStyle`, `ttMediaInputStyle`, `ttMediaPrimaryStyle`, `ttMediaOutlineStyle`, `colorPrimary`, `colorOnPrimary`).

### Kiểm thử UI Instrumentation (`android-client/app/src/androidTest/`):
- `ui/theme/ProfileThemeUiTest.java`: Bộ test suite tự động kiểm thử toàn diện 3 kịch bản màn hình Hồ sơ cá nhân:
  1. `test01_allStylesModesLayoutsForProfile`: Kiểm tra 72 tổ hợp (4 styles × 2 modes × 3 fontScales × 3 viewports `{280, 560}`, `{360, 720}`, `{840, 420}`) cho layout Profile, xác minh các widget hiển thị đầy đủ, bounds không bị tràn và độ tương phản WCAG >= 4.5:1.
  2. `test02_profileDetailsAndDisplayNameEditSurvivesThemeChange`: Kiểm tra tải hồ sơ người dùng (`@qa_profile`, email `qa_profile@example.com`), chỉnh sửa tên hiển thị trên cả 4 theme (Original, Gradient, Glass, Soft), click Lưu và kiểm tra thông báo thành công, kiểm tra việc bảo toàn thông tin khi đổi theme và recreate Activity.
  3. `test03_profileAvatarAndLogoutInteractionThemedCorrectly`: Kiểm tra tương tác tải ảnh đại diện hợp lệ qua `AvatarPhoto` (file JPEG chuẩn), kiểm tra mở màn hình Cài đặt giao diện và kiểm tra nút Đăng xuất xóa phiên an toàn và kết thúc MainActivity.
- `ui/theme/MediaThemeFixture.java`:
  - Bổ sung mock cho `UserApi`: `me()`, `update()`, `uploadAvatar()`, `avatar()` khi cờ `mockProfile = true`.
  - Bổ sung mock cho `DeviceApi`: `devices` trả về thành công để hỗ trợ quy trình đăng xuất không phụ thuộc mạng ngoại vi.
- `ui/theme/MediaThemeUiTest.java`: Cập nhật assertion kiểm tra `fragment_profile` cùng với `fragment_history` và `fragment_friends` đều áp dụng context theme động của Activity.

## 3. Kết quả Kiểm thử Thực tế

### 1. UI Instrumentation trên Android Emulator `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM):
- **ProfileThemeUiTest**: **3/3 PASS (24.209s)**
  - `test01_allStylesModesLayoutsForProfile`: PASS
  - `test02_profileDetailsAndDisplayNameEditSurvivesThemeChange`: PASS
  - `test03_profileAvatarAndLogoutInteractionThemedCorrectly`: PASS
- **MediaThemeUiTest** (Kiểm tra hồi quy Media): **5/5 PASS (114.147s)**
  - `allStylesModesLayoutsAndOriginalTabContexts`: PASS
  - `previewDraftCaptionSelectionFrozenRetryKeyAndImageSurviveRecreation`: PASS
  - `cameraNavigationAndPendingThemeKeepActivityTabAndSettings`: PASS
  - `oneApprovedRealCaptureToCacheAndRetakeWithoutUpload`: PASS
  - `viewerAllStylesModesDialogAndRevocationKeepPrivatePhotoHidden`: PASS
- **ListThemeUiTest** (Kiểm tra hồi quy Lists): **4/4 PASS (29.027s)**
  - `historyTabSwitchingAndEmptyStateSurvivesThemeChange`: PASS
  - `allStylesModesLayoutsForHistoryAndFriends`: PASS
  - `historyMomentItemInteractionAndNavigationToViewer`: PASS
  - `friendsListSectionsAndConfirmDialogsThemedCorrectly`: PASS

### 2. Unit Tests Android JVM:
- `gradlew.bat test`: **105/105 tests PASS** (27s).

### 3. Integration Tests Spring Boot Server:
- `mvn test`: **152/152 tests PASS** (1m 39s).

## 4. Hạn chế và Bước tiếp theo

- Đã tắt emulator an toàn sau kiểm thử qua `adb emu kill`.
- Toàn bộ 4 tab chính của `MainActivity` (Camera, Lịch sử, Bạn bè, Hồ sơ cá nhân) cùng Preview, Viewer, Auth (Login, Register, Recovery) và Theme Chooser hiện đã được chuyển đổi hoàn chỉnh sang kiến trúc 4 theme động.
- Sẵn sàng chuyển sang bước tiếp theo: **UI-THEME-09 — Dark mode contrast, Dynamic color audit & WCAG compliance across all 4 themes** (kiểm tra và tối ưu độ tương phản, dynamic color và tính nhất quán trên toàn bộ ứng dụng).
- Toàn bộ thay đổi mã nguồn được giữ nguyên trên worktree, không commit, push Git remote hoặc deploy theo đúng `AGENTS.md`.
