# Taktak — Rà soát UI/UX và sửa tài nguyên an toàn (20/10/2026)

## Cập nhật nghiệm thu AVD theo yêu cầu người dùng

Người dùng đã chọn **tự chạy máy ảo** thay USB. Skill android_ui_verification
dùng Medium_Phone API 37, 1080×2400, headless 1536 MB/2 core/SwiftShader,
userdata QA có sẵn tại .local/artifacts/qr-avd-data; xác nhận RUNNING_UNLOCKED.
Không wipe/reset AVD gốc, không dùng tài khoản cá nhân, không commit/push/deploy.

**Trạng thái: IN PROGRESS / chưa đạt cổng nghiệm thu toàn bộ UI.** Blocker USB
trước đó không còn chặn QA vì đã chạy được AVD. Những mô tả “chưa chạy runtime”
ở phần bằng chứng bên dưới là snapshot trước lượt AVD, không phải kết quả mới nhất.

### Sửa thêm từ kết quả runtime và review ảnh

- activity_login.xml + taktak_dimens.xml: CTA minHeight 56dp; Language/Theme/
  Forgot link dùng wrap_content + minHeight 48dp để không cắt chữ khi font lớn.
- activity_preview.xml: thanh trên wrap_content + minHeight 56dp; nhóm năm công
  cụ dùng GridLayout hai cột để không chia chữ vụn. Chỉ đổi layout, giữ ID/handler.
- login_themes.xml: ba token InputBackground/InputText/InputHint trỏ theo nền
  field và ink của theme resolver hiện dùng, sửa field tối cố định trong light mode.
- item_history_moment.xml: chia lại chiều rộng timestamp/username, giới hạn badge
  expiry để không tràn ngang khi font lớn.
- fragment_history.xml: TabLayout wrap_content + minHeight 48dp. Thử tăng min
  bằng sp không giải quyết được lỗi ma trận nên đã bỏ; không nhận là đã sửa xong.

Sáu file resource được chỉnh thêm trong lượt AVD; không sửa Java/test Java.

### Kết quả thực tế

| Kiểm tra | Kết quả | Bằng chứng local |
|---|---|---|
| Build debug/test/release unsigned cuối | BUILD SUCCESSFUL, 34s | ui-audit-avd-final-build-accepted.log |
| JVM cuối, task thực sự thực thi | 118 tests, 0 failures/errors | cùng log build; XML test-results |
| Bộ UI 34 ca | 31 đạt, 3 lỗi; 383,796s | ui-audit-avd-final-matrix.log |
| Hồi quy điểm sửa + QR, 12 ca | 11 đạt, 1 lỗi; 98,440s | ui-audit-avd-targeted-final.log |
| Kiểm tra lại danh sách trên APK bàn giao | vẫn FAIL Text fits height: TextView [none] | ui-audit-avd-last-check.log |
| Quyền Camera chạy riêng đúng tiền điều kiện | PASS 1/1; 10,012s | ui-audit-avd-permission-final.log |
| Lint debug/release cuối | FAIL 179/90 và 179/98 (errors/warnings), 28s | ui-audit-avd-final-lint-both.log |

Các log đều ở .local/artifacts. Bộ 34 ca gồm Login, Auth, ThemeSelection,
Media, List, Profile và Chat. Auth 4/4, chooser 7/7, Media 5/5, Profile 3/3
đạt trong lượt tổng. Lỗi minHeight/text height Login được sửa và ma trận Login
đạt ở lượt targeted; ma trận Media và luồng Preview retry/recreate cũng đạt.
QR targeted có 5 regression + 2 UI đạt; không tính đây là optical scan/flash thật.

Hai lượt quyền trước không đạt được giữ log: lần đầu không tìm thấy nút deny
do trạng thái quyền cũ; lần chạy chung với List bị fixture Media cấp CAMERA
trước khi tới test quyền. Đã revoke/clear user-set,user-fixed chỉ trên AVD QA,
chạy riêng ca quyền và đạt, không sửa/bỏ assertion hoặc dùng kết quả lỗi làm PASS.
Lượt lint thiếu tham số release HTTPS cũng được giữ log; đã chạy lại cả hai
biến thể với HTTPS placeholder chỉ trên command line, không đổi cấu hình repo.

Ảnh mới ở .local/artifacts/ui-audit-avd-final-images (theme04/05/06/07/08,
chat, qr); đã review mẫu Login light/dark, Register, Preview trước/sau nhóm
công cụ. Thư mục pull chứa cả ảnh lịch sử có sẵn của AVD; không coi tổng số
file là số screenshot mới hay độ phủ hoàn tất. Log AndroidRuntime không có
FATAL EXCEPTION; assertion UI lỗi vẫn được ghi FAIL riêng, không coi không crash
là đủ nghiệm thu. AVD được tắt sau bàn giao để trả RAM.

### Vấn đề còn lại cần xử lý riêng

1. ListThemeUiTest.allStylesModesLayoutsForHistoryAndFriends vẫn cắt chiều cao
   TextView không có ID trong ma trận hẹp/font lớn. Lỗi tràn badge đã hết nhưng
   không đồng nghĩa toàn danh sách đạt. Cần bổ sung diagnostic vào test Java
   để chỉ rõ layout/theme/font/đường dẫn View trước khi sửa tiếp; chưa có quyền
   đổi Java trong yêu cầu resource-only, không sửa mù hoặc bỏ kiểm thử.
2. Chat incomingReadAndPrivateMomentAttachmentUseAuthenticatedFixture timeout
   “Private attachment choices deduplicated” trong hai lượt bộ tổng; chạy riêng
   ở targeted đạt. Chưa chốt nguyên nhân, nghi vấn timing/lifecycle/busy cần
   điều tra thêm; không coi retry đạt là đã sửa tính không ổn định. Không đổi
   ViewModel/Repository hoặc harness để làm xanh kết quả.
3. 179 lỗi bản dịch cũ, TalkBack/RTL/đủ bảy locale và widget launcher vẫn chưa
   hoàn tất; phần cứng optical Camera/flash thật không được AVD thay thế.

Hành động tiếp theo đề xuất: duyệt mở phạm vi **test Java chẩn đoán UI** để
truy vết đúng View gây cắt chữ và nguyên nhân ca Chat không ổn định; không cần
người dùng cắm điện thoại cho việc chẩn đoán AVD này.

## Trạng thái và phạm vi

**IN PROGRESS — phần sửa tài nguyên build được; chưa nghiệm thu UI toàn diện.**
Yêu cầu nguồn: prompt “Rà soát UI/UX của Taktak Android Client và Tự sửa lỗi an toàn”.
Đã kiểm kê 29 layout và đối chiếu hệ thống Taktak/Material, Activity/Fragment,
LanguageManager, BiometricHelper cùng các điểm render/insets trong Java.
Không đổi bất kỳ android:id nào. Đối chiếu SHA-256 xác nhận 109 file
production Java giữ nguyên. Không đổi ViewModel/Repository/Network/backend,
Manifest, Gradle hoặc định danh; không thêm tính năng, commit/push/deploy.

Skills dùng: accessibility-compliance-accessibility-audit định hướng vùng chạm,
tương phản, tên truy cập và việc ghi rõ độ phủ; android_ui_verification dùng
quy trình kiểm tra thiết bị/bằng chứng. Không có kết quả TalkBack/keyboard/focus
manual trong lượt này, không tuyên bố tuân thủ WCAG toàn sản phẩm.

## Bảng phát hiện đã xuất trước khi sửa

| Màn hình / file | Vấn đề | Rủi ro | Giải pháp | Tự sửa |
|---|---|---|---|---|
| Typography, taktak_styles.xml | textFontWeight/lineHeight cần API 28, minSdk 26 | Cao | textStyle, lineHeight tương thích Material | Có |
| 8 theme, taktak_themes.xml | windowLightNavigationBar cần API 27 | Cao | alias theme giữ nguyên tên công khai, Compat base và values-v27 override | Có |
| Chat/New Chat/QR/Profile | icon 36–44dp, nút 36–44dp | TB | min touch 48dp, nút có chữ wrap_content | Có |
| Màu chữ phụ/hint, taktak_colors.xml | muted không đạt 4,5:1 trên một số nền đặc | TB | điều chỉnh 8 màu muted, giữ semantic attr | Có, giới hạn palette |
| Nút primary/Unread | chữ trắng trên gradient sáng | TB | chữ/icon tối, giữ gradient thương hiệu | Có |
| My QR/Friend Found | sheet dài có nguy cơ thiếu không gian | TB | NestedScrollView, không đổi ID hoặc code binding | Có; runtime còn chờ |
| Layout, item, widget | màu/chuỗi trần, icon trang trí và reaction thiếu mô tả | TB | color/string resources, mô tả 7 locale, đánh dấu icon trang trí | Có |
| Bản dịch cũ | 179 định danh thiếu ở locale đã khai báo | TB | cần dịch/đối chiếu placeholder và kiểm tra đủ màn | Chưa hoàn tất |
| Widget 2×2 | reaction 28dp | TB | cần bố cục thích hợp kích thước widget tối thiểu | Không tăng mù quáng |
| Chat/insets, Biometric, Language, QR overlay | một số yêu cầu chạm tới Java/native system UI | Cao | giữ logic, tách đề nghị quyết định | Không đổi Java |

## Hệ thống thiết kế được giữ

- Dùng các semantic attr taktakColor* đã khai báo; không thêm attr mới.
- Font Inter, TextAppearance.Taktak.*, ShapeAppearance.Taktak.*.
- medium/small button token tăng lên 48dp; large vẫn 52dp.
- Margin/padding khớp scale 4/8/12/16/20/24/32/40dp được tham chiếu dimen,
  không làm tròn kích thước đặc thù, không thay hình vuông CameraX/Preview.
- Line height dùng sp để theo font scale; font weight fallback thành bold/normal
  tương thích API 26, không hứa giữ chính xác variable weight 600 trên API 26.
- Bổ sung Widget.Taktak.Button.Danger; giữ logout/confirm logic cũ.
- Color error có mặc định sáng và values-night; màu overlay camera, mực vẽ và
  chữ trên ảnh widget vẫn là palette cố định qua @color, không ép đổi theo theme.
- QR giữ nền trắng tinh và module đen; widget giữ toàn bộ lớp RemoteViews hiện có,
  không thêm Material/custom view hoặc ?attr vào widget.
- Chuỗi ngôn ngữ mới có đủ vi/en/ko/zh/es/hi/ja trong ui_audit_strings.xml.
  Glyph ‹, +, emoji dùng tài nguyên chung translatable=false vì không phải ngôn ngữ;
  không dùng cờ này để giấu 179 chuỗi ngôn ngữ còn thiếu.

## Tài nguyên đã sửa

28 layout:
- activity_chat.xml
- activity_login.xml
- activity_main.xml
- activity_moment_viewer.xml
- activity_new_chat.xml
- activity_preview.xml
- activity_qr_scan.xml
- activity_recovery_form.xml
- activity_register.xml
- activity_theme_settings.xml
- dialog_my_qr_code.xml
- dialog_qr_friend_found.xml
- fragment_camera.xml
- fragment_camera_page.xml
- fragment_friends.xml
- fragment_history.xml
- fragment_messages.xml
- fragment_pending_tab.xml
- fragment_profile.xml
- item_chat_message.xml
- item_conversation.xml
- item_friend.xml
- item_friends_section.xml
- item_history_moment.xml
- item_recipient.xml
- item_theme_option.xml
- widget_taktak_2x2.xml
- widget_taktak_4x2.xml

4 file values có sẵn:
- taktak_styles.xml: typography/API26, text/icon primary, Danger.
- taktak_dimens.xml: button tối thiểu, line height sp.
- taktak_colors.xml: muted/error và các hằng màu dùng trong layout.
- taktak_themes.xml: public theme aliases/Compat, onPrimary cho theme tối.

9 file mới:
- values-v27/taktak_themes.xml (8 override navigation bar, không suppress NewApi).
- values-night/taktak_colors.xml (error color).
- values/ui_audit_strings.xml và values-{en,ko,zh,es,hi,ja}/ui_audit_strings.xml.

My QR/Friend Found được bọc bằng NestedScrollView, ID bên trong không đổi.
Profile name input được thêm hint resource để có nhãn khi TextInputLayout
hintEnabled=false. Avatar centerCrop/placeholder và switch label được bổ sung.
Badge dài được ellipsize/maxLines để hạn chế ép tên bạn bè trên màn hẹp.
Không sửa một file Java nào, kể cả test Java.

## Tương phản palette đã tính

Tỷ lệ dưới đây được tính trên sRGB từ resource, lấy nền xấu nhất trong
background/surface/surface_variant của đúng palette; không phải đo mọi pixel
của shader/ảnh/camera và không thay cho kiểm tra focus/disabled/error runtime.

| Palette muted | Cũ | Mới | Tỷ lệ nhỏ nhất |
|---|---|---|---:|
| taktak_orig_dark_text_muted | #80778D | #8F879B | 4.56:1 |
| taktak_orig_light_text_muted | #948D9E | #716A7B | 4.55:1 |
| taktak_grad_dark_text_muted | #7D83A4 | #8B91AE | 4.59:1 |
| taktak_grad_light_text_muted | #8F94AF | #686C85 | 4.56:1 |
| taktak_glass_dark_text_muted | #758DAB | #8CA0B9 | 4.51:1 |
| taktak_glass_light_text_muted | #8097B0 | #5B6E84 | 4.6:1 |
| taktak_soft_dark_text_muted | #868088 | #9C979D | 4.54:1 |
| taktak_soft_light_text_muted | #9E97A1 | #716B75 | 4.52:1 |

Chữ primary #1C1626 trên hai đầu gradient #FF8A5C/#FF6584 đạt tối thiểu
6,25:1. Các background thực tế ngoài palette này vẫn cần kiểm tra bằng thiết bị.

## Checklist 12 tiêu chí

| Tiêu chí | Kết quả / giới hạn |
|---|---|
| 1. Nhất quán | Đã chuẩn hóa layout sang color/spacing resources và semantic surface/text; legacy role themes vẫn cần review runtime |
| 2. 7 ngôn ngữ | Chuỗi mới đủ locale; 179 chuỗi cũ chưa hoàn tất, lint vẫn FAIL |
| 3. Typography/touch | Sửa API26, sp và nút/icon nhỏ; reaction widget 28dp chưa đạt |
| 4. Responsive | Nút text wrap, badge ellipsize, sheet scroll; chưa chạy matrix màn 320dp/landscape/font2 |
| 5. Keyboard/insets | Giữ insets/adjustResize đang có; không bọc Chat RecyclerView trong ScrollView để tránh thay kiến trúc IME |
| 6. Dark/contrast | Palette đã đo và error night; chưa nghiệm thu cả 4 theme x day/night thực tế |
| 7. Loading/empty/error | Đã đọc các view/status và binding hiện có; giữ enable/disable theo ViewModel, không thêm logic trạng thái |
| 8. RecyclerView | Giữ MaterialCardView ripple/click handler; avatar có crop/placeholder; chưa đo hiệu năng scroll |
| 9. Accessibility/RTL | Giữ start/end, thêm label/description trang trí/reaction/switch; TalkBack và focus chưa chạy |
| 10. Điều hướng/state | Giữ 5 tab/ID/state flags, không sửa Java điều hướng; recreate chưa hồi quy sau thay đổi |
| 11. QR | Giữ trắng/đen và ID, sheet scroll; gradient corner/laser thuộc custom Java nên không sửa |
| 12. Lint | 22 NewApi và HardcodedText XML đã xử lý; 179 MissingTranslation và cảnh báo Java/legacy còn lại |

## Bằng chứng kiểm tra trước lượt AVD

- Baseline lintDebug: **201 errors / 107 warnings**,
  .local/artifacts/ui-audit-baseline-lint.log.
- Final cmd.exe /c gradlew.bat ... assembleDebug assembleDebugAndroidTest
  testDebugUnitTest assembleRelease: **BUILD SUCCESSFUL, 19s**,
  .local/artifacts/ui-audit-build-final.log. APK release unsigned; dùng tham số
  HTTPS placeholder chỉ cho lệnh build, không sửa BASE_URL repo.
- JVM XML reports: **118 tests / 0 failures / 0 errors**. Task cuối
  testDebugUnitTest **UP-TO-DATE**, xác minh kết quả/cache hợp lệ; không giả nhận
  đã chạy lại toàn bộ test hay instrumentation trong lượt này.
- Final lintDebug: **179 errors / 90 warnings**; lintRelease:
  **179 errors / 98 warnings**. Log .local/artifacts/ui-audit-lint-final.log.
  179 lỗi đều MissingTranslation ở camera/chat/friends/history/auth/recovery/
  preview/profile và tài nguyên hiện có. Không suppress/baseline/disable lint.
- Parse cả 29 XML thành công. So sánh tập/thứ tự android:id trước–sau:
  **0 thay đổi**. SHA-256 109 production Java: **0 thay đổi**.
- Widget XML vẫn chỉ có lớp Android RemoteViews sẵn có và không có theme attr.
- Khi chuẩn bị QA, adb devices -l không còn thiết bị; đã hỏi người dùng kết nối
  lại. **Chưa cài APK sau sửa UI lên điện thoại, chưa chạy UI instrumentation,
  chưa có screenshot trước/sau của lượt này.** Kết quả QR USB trước đó là lịch sử,
  không được dùng làm bằng chứng UI mới.
- Không thay cấu hình quyền/auth/biometric/locale cá nhân khi không có thiết bị.
- Các bản sửa người dùng đã có, bao gồm untracked, được giữ; không reset/clean,
  không commit/push/deploy.

## Chưa sửa và cần quyết định/kiểm thử

1. Hoàn tất 179 chuỗi cũ x locale còn thiếu. Không clone tiếng Việt/Anh vào các
   locale chỉ để làm xanh lint; cần kiểm tra nghĩa/placeholder và nội dung recovery.
2. QRScannerOverlayView hardcode Paint, laser lifecycle; DoodleOverlayView và
   một số nhãn động trong Java. Muốn sửa cần mở phạm vi Java UI riêng.
3. LanguageManager tạo danh sách native/English bằng Java; BiometricPrompt/PIN
   là UI hệ thống. Không thể coi chỉnh XML app là đã kiểm thử/đổi các dialog đó.
4. Widget reaction 28dp: tăng cả 3/4 nút lên 48dp có thể tràn widget tối thiểu;
   cần quyết định bố cục/giới hạn nhỏ nhất, không đổi RemoteViews behavior.
5. Badge BFF/streak/expiry có nội dung preview/fallback trong code hiện tại;
   chỉ tài nguyên hóa, không tự tạo dữ liệu/logic tính streak hay đổi nghiệp vụ.
6. Navigation hiện dùng unlabeled, tự động accessibility title từ menu; muốn
   luôn hiện 5 nhãn cần kiểm tra chiều rộng/font scale/7 ngôn ngữ và chốt thiết kế.
7. Legacy ttMedia/ttLogin role themes và semantic Taktak mới cùng tồn tại.
   Không đổi ThemeManager để ép theme hoặc khẳng định 4 style đã đồng bộ hoàn toàn.
8. Không chứng nhận WCAG 2.2 AA: chưa chạy TalkBack/focus/keyboard/RTL native.

## Checklist thủ công khi có thiết bị

- Cắm USB, mở khóa, chấp nhận debugging; dùng QA fixture, không logout tài khoản chính.
- Cài APK -r, giữ dữ liệu. Kiểm tra 4 style x light/dark, màn 320dp và lớn,
  portrait/landscape, font 1x/2x; không tràn tên/email/caption/5 tab.
- Login/Register/Recovery/Profile/Friends/Chat: mở IME, focus từng ô, bàn phím
  không che field/error/action; không gửi email/reset mật khẩu/tin nhắn thật.
- Bật TalkBack: tên controls, thứ tự focus, mô tả phản ứng, trạng thái tải/lỗi;
  test RTL trong môi trường QA riêng rồi khôi phục.
- Lists: empty/loading/network error/retry, tên dài, scroll và ripple.
- QR: My QR trắng/đen, đóng sheet quét tiếp, chooser share chỉ hủy,
  chọn ảnh/quét camera/flash khi đã hướng vào vật an toàn; không gửi lời mời thật.
- Widget 2×2/4×2: xem trên launcher thật, cả trạng thái ảnh/rỗng/logout,
  kiểm tra touch reaction và chữ trên ảnh. Không dùng auto-logout tài khoản chính.
- Dùng các test matrix có sẵn LoginThemeUiTest, AuthThemeUiTest,
  MediaThemeUiTest, ListThemeUiTest, ProfileThemeUiTest, ChatThemeUiTest và
  QrRegressionTest/QrUsbUiTest với đúng opt-in; chỉ chọn test fixture không network.
- Chỉ chốt PASS từng hạng mục đã chạy, lưu log/ảnh local và khôi phục appearance.
