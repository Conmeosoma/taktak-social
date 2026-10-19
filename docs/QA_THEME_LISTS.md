# UI-THEME-07 — History (Received/Sent), Friends & List Items bốn theme

Ngày 19/10/2026. Nghiệm thu bước UI-THEME-07 theo roadmap nâng cấp giao diện đa chủ đề Taktak. Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Lịch sử (Đã nhận / Đã gửi), tab Bạn bè, các phần tử danh sách (Moments, Invitations, Friends, Search results, Section headers, Footers) và hộp thoại xác nhận.

## 1. Phạm vi và Triển khai

Đã đối chiếu `AGENTS.md`, `TAKTAK_PROJECT.md`, `PROJECT_DECISIONS.md`, các bài học từ UI-THEME-01 đến 06 và mã nguồn hiện hữu trước khi thực hiện.

- **Theme & Token Architecture cho Lists & Tabs**:
  - Gỡ bỏ hardcoded `@style/Theme.TakTak.Premium` tại `fragment_history.xml` và `fragment_friends.xml`, cho phép toàn bộ Fragment, RecyclerView, ViewHolder và PopupMenu tự động thừa hưởng Context Theme của Activity theo phong cách người dùng đã chọn.
  - Bổ sung 7 theme attributes và styles cho History và Friends trong `res/values/media_themes.xml`:
    - `ttMediaPrimaryStyle`: Trỏ về `@style/Widget.TakTakPremium.Primary` trên Original; trỏ về `@style/Widget.TakTakAuth.Primary` trên Gradient/Glass/Soft.
    - `ttMediaTonalStyle`: Trỏ về `@style/Widget.TakTakPremium.Tonal` trên Original; trỏ về `@style/Widget.TakTakAuth.Tonal` trên Gradient/Glass/Soft.
    - `ttMediaCardBackground`: Trỏ về `@drawable/premium_card_background` trên Original; trỏ về `?attr/colorSurfaceContainerLow` trên Gradient/Glass/Soft.
    - `ttMediaTabIndicator`: Trỏ về `@drawable/premium_tab_indicator` trên Original; trỏ về `@drawable/media_tab_indicator` trên Gradient/Glass/Soft.
    - `ttMediaTabSelectedColor`, `ttMediaTabUnselectedColor`, `ttMediaDividerColor`, `ttMediaNoticeBackground`: Phục vụ TabLayout, divider và banner thông báo.
  - Khai báo fallback mặc định đầy đủ trong `Base.Theme.TakTak` (`res/values/themes.xml`), bảo đảm an toàn tuyệt đối cho mọi test context kế thừa.
- **Tối ưu hóa Giao diện và Khả năng tiếp cận**:
  - `item_history_moment.xml`: Tối ưu trường `momentSenderUsername` với `android:layout_width="0dp"`, `android:layout_weight="1"`, `android:singleLine="true"`, `android:ellipsize="end"`. Triệt tiêu hoàn toàn lỗi tràn biên ngang trên màn hình cực hẹp (280dp) với font trợ năng 2.0x (200%).
  - `item_friend.xml`: Chuyển `friendPrimary` sang `?attr/ttMediaPrimaryStyle`, `friendSecondary` sang `?attr/ttMediaTonalStyle`.
  - `item_friends_section.xml`: Chuyển nền sang `?attr/ttMediaCardBackground`, nút More sang `?attr/ttMediaTonalStyle`.
  - `item_history_footer.xml`: Nút More chuyển sang `?attr/ttMediaTonalStyle`.
- **Bảo toàn nghiệp vụ và Bảo mật**:
  - Giữ nguyên toàn bộ logic chuyển tab Đã nhận / Đã gửi, phân trang, caching bitmap `HistoryImageCache`, debounce tìm kiếm bạn bè.
  - Giữ nguyên các hộp thoại xác nhận hủy kết bạn / chặn người dùng `MaterialAlertDialogBuilder` và popup menu tùy chọn bạn bè.
  - Không thay đổi nghiệp vụ server, cơ sở dữ liệu, FCM hay widget.

## 2. Các file triển khai của UI-THEME-07

### Mã nguồn Android Client (`android-client/app/src/main/`):
- **XML Layouts & Drawables**:
  - `res/drawable/media_tab_indicator.xml`: Tab indicator màu `?attr/colorPrimary` cho Gradient/Glass/Soft.
  - `res/values/media_themes.xml`: Bổ sung 7 attributes và các giá trị token cho History & Friends trên 4 theme.
  - `res/values/themes.xml`: Khai báo fallback tokens an toàn trong `Base.Theme.TakTak`.
  - `res/layout/fragment_history.xml`: Gỡ hardcoded theme, dùng `?attr/ttMedia*` cho title, refresh button, tabs, divider, notice banner.
  - `res/layout/item_history_moment.xml`: Fix layout weight & singleLine cho sender username, bảo đảm timestamp không bị đẩy quá biên.
  - `res/layout/item_history_footer.xml`: Nút tải thêm dùng `?attr/ttMediaTonalStyle`.
  - `res/layout/fragment_friends.xml`: Dùng `?attr/ttMedia*` cho title, search/refresh buttons, text input.
  - `res/layout/item_friend.xml`: Dùng `?attr/ttMediaPrimaryStyle` cho primary button, `?attr/ttMediaTonalStyle` cho reject button.
  - `res/layout/item_friends_section.xml`: Dùng `?attr/ttMediaCardBackground` và `?attr/ttMediaTonalStyle`.

### Kiểm thử UI Instrumentation (`android-client/app/src/androidTest/`):
- `ui/theme/ListThemeUiTest.java`: Bộ test suite tự động kiểm thử toàn diện 4 kịch bản giao diện Lists:
  1. `allStylesModesLayoutsForHistoryAndFriends`: Kiểm tra 144 tổ hợp (4 styles × 2 modes × 3 fontScales × 3 viewports `{280, 560}`, `{360, 720}`, `{840, 420}`) cho cả 6 layout (`fragment_history`, `item_history_moment`, `item_history_footer`, `fragment_friends`, `item_friend`, `item_friends_section`), kiểm tra bounds và độ tương phản WCAG >= 4.5:1.
  2. `historyTabSwitchingAndEmptyStateSurvivesThemeChange`: Kiểm tra chuyển tab Đã nhận / Đã gửi, empty states label, lưu giữ cài đặt và khôi phục khi đổi theme / recreate activity.
  3. `friendsListSectionsAndConfirmDialogsThemedCorrectly`: Kiểm tra danh sách kết bạn, lời mời kết bạn, tìm kiếm người dùng, popup menu và hộp thoại `MaterialAlertDialogBuilder` kế thừa theme.
  4. `historyMomentItemInteractionAndNavigationToViewer`: Kiểm tra hiển thị khoảnh khắc trong danh sách, click mở `MomentViewerActivity`, kiểm tra dữ liệu và quay về an toàn.
- `ui/theme/MediaThemeFixture.java`: Cập nhật mock model `MomentModels.PublicUser`, bổ sung đầy đủ trường hợp lệ cho `FriendModels.Invitation` (`receiverId`, `status="pending"`, `createdAt`) và mock responses cho các mutation quan hệ bạn bè.
- `ui/theme/MediaThemeUiTest.java`: Cập nhật assertion khớp context theme động của các tab đã port.

## 3. Kết quả Kiểm thử Thực tế

### 1. UI Instrumentation trên Android Emulator `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM):
- **ListThemeUiTest**: **4/4 PASS (31.607s)**
  - `historyTabSwitchingAndEmptyStateSurvivesThemeChange`: PASS
  - `allStylesModesLayoutsForHistoryAndFriends`: PASS
  - `historyMomentItemInteractionAndNavigationToViewer`: PASS
  - `friendsListSectionsAndConfirmDialogsThemedCorrectly`: PASS
- **MediaThemeUiTest** (Kiểm tra hồi quy Media): **5/5 PASS (116.985s)**
  - `allStylesModesLayoutsAndOriginalTabContexts`: PASS
  - `previewDraftCaptionSelectionFrozenRetryKeyAndImageSurviveRecreation`: PASS
  - `cameraNavigationAndPendingThemeKeepActivityTabAndSettings`: PASS
  - `oneApprovedRealCaptureToCacheAndRetakeWithoutUpload`: PASS
  - `viewerAllStylesModesDialogAndRevocationKeepPrivatePhotoHidden`: PASS

### 2. Unit Tests Android JVM:
- `gradlew.bat test`: **105/105 tests PASS** (12s).

### 3. Integration Tests Spring Boot Server:
- `mvn test`: **152/152 tests PASS** (2m 01s).

## 4. Hạn chế và Bước tiếp theo

- Đã tắt emulator an toàn sau kiểm thử qua `adb emu kill`.
- Tab Hồ sơ cá nhân (`fragment_profile.xml`) hiện vẫn giữ ngữ cảnh Premium/Original và là phạm vi của bước tiếp theo: **UI-THEME-08 — Profile screen, Avatar upload & Logout bốn theme**.
- Toàn bộ thay đổi mã nguồn được giữ nguyên trên worktree, không commit, push Git remote hoặc deploy theo đúng `AGENTS.md`.
