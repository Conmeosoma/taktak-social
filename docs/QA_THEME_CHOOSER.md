# UI-THEME-03 — Chọn giao diện, preview, Áp dụng/Hủy

Ngày19/10/2026. Trạng thái: **PASS phạm vi UI-THEME-03**.

## Phạm vi được duyệt

Người dùng duyệt riêng03 sau foundation02. Đã đọc AGENTS, đặc tả, quyết định, hồ sơ02 và source liên quan; giữ toàn bộ worktree dirty cũ. Không chuyển04 hoặc sửa backend/database/dependency.

Đã triển khai Java/XML/ViewBinding/Material1.14.0:

- `ThemeSettingsActivity`: Activity không exported,4card native có checkmark/state-description, tên/mô tả/thumbnail; preview lớn; mode Hệ thống/Sáng/Tối riêng; nút Áp dụng/Hủy/Quay lại.
- `ThemeViewModel`: LiveData với saved selection và draft tách biệt; chọn thẻ/mode không ghi; save trên executor, chỉ success sau repository commit đạt. Khóa chọn/Apply/Cancel/Back khi đang ghi để không giả đã hủy commit; lỗi giữ draft và cho retry. Không giữ Activity/phiên.
- `ThemePreviewView`: minh họa tĩnh từ cùng tokens chooser, Canvas gradient/lớp/viền; không bitmap blur, shader không cấp phát theo mỗi frame; không giả thumbnail là screenshot thực.
- `ThemeManager.chooserThemeResource`:4style riêng **chỉ chooser**. Không sửa resolver5role nghiệp vụ và availability Original fallback của02.
- Lối mở tại Login và Hồ sơ, disable khi auth/profile đang xử lý. Không thêm tab, không auto recreate Main/Login/Profile; không thay callback auth/profile hiện có.

Chọn chỉ đổi preview; Áp dụng mới persist style/mode/revision. Save thành công recreate **riêng chooser** để nhận palette/local night mode trước inflate, rồi hiện thông báo đã lưu. Hủy/Back bỏ draft; sau khi đã lưu, nút Đóng chỉ đóng màn, không rollback lựa chọn đã commit. Draft không nhạy cảm giữ trong ViewModel qua configuration change, các ID draft được lưu Bundle để phục hồi process; không có password/token/profile/photo trong Bundle này.

## Phân biệt chooser và các màn chưa tích hợp

Original/Gradient/Glass/Soft đều có palette DayNight trong chooser và preview. Đây chỉ là minh họa ngôn ngữ màu/phân lớp, chưa phải dựng lại3ảnh tham chiếu pixel-perfect hoặc theming toàn app. **Login/Register/Camera/History/Friends/Profile/Preview/Viewer chưa chuyển3style mới ở03**; vẫn giữ giao diện hiện tại. Thông báo giới hạn có trên màn hình và sau lưu. Login Original vẫn fixed-dark như trước.

Không đặt AppCompat default night mode toàn app, không thay task/session để áp theme. Parent Login giữ nguyên instance/form khi trở về; Main/Profile không có observer theme hay recreate thêm nên không ép mất draft. Tích hợp màn Login thuộc04 sau xác nhận; các màn khác tiếp tục05–08 theo audit. Widget RemoteViews không thay đổi.

Chooser dùng Material controls/semantic text, hit target≥48dp, scroll/maxwidth600dp/edge-to-edge insets systemBars+cutout, content descriptions và trạng thái checked. Preview trang trí không vào cây accessibility. Không tự nhận đã nghiệm thu TalkBack thủ công hoặc hiệu năng điện thoại.

## File riêng bước03

Tạo mới:

- `android-client/app/src/main/java/com/taktak/app/ui/theme/{ThemeSettingsActivity,ThemeViewModel,ThemePreviewView}.java`
- `android-client/app/src/main/res/layout/activity_theme_settings.xml`, `item_theme_option.xml`
- `android-client/app/src/main/res/values/theme_chooser.xml`, `theme_chooser_colors.xml`
- `android-client/app/src/main/res/values-night/theme_chooser_colors.xml`
- `android-client/app/src/test/java/com/taktak/app/ui/theme/ThemeViewModelTest.java`
- `android-client/app/src/androidTest/java/com/taktak/app/ui/theme/ThemeSelectionUiTest.java`
- Báo cáo này; cập nhật4docs quản lý trạng thái.

Sửa có kiểm soát6file main cũ: ThemeManager.java, AndroidManifest.xml, LoginActivity.java, ProfileFragment.java, activity_login.xml, fragment_profile.xml. Header Login nhận thêm lối mở và bố trí language label xuống dưới hàng back/theme để không chồng khi font lớn; không làm lại Login03. Hash300file main/server/deploy/scripts trước/sau cho thấy chỉ6file này đổi,0file cũ bị mất;8file main mới nêu trên. Sửa thêm **helper trong LoginReferenceTest.java** sau khi đọc code: AVD mở Login trực tiếp không gọi launcher/session routing trước mỗi ca; máy thật vẫn giữ bootstrap workaround MIUI. Dùng startActivitySync trả đúng instance mới và chờ window focus, thay việc bắt Login đầu tiên RESUMED có thể là instance launcher route. Giữ nguyên toàn bộ assertion/IME thật; không giảm thời gian chờ hoặc skip ca lỗi. Không dùng git diff HEAD để nhận các sửa recovery/UI trước đây là của03.

## VERIFY — bằng chứng hiện có

Build đầu thất bại do `colorPrimary` lấy từ namespace Material thay vì AppCompat với non-transitive R; đã sửa đúng `androidx.appcompat.R.attr.colorPrimary`, không thêm dependency. Build app review `.local/artifacts/theme03-reviewed-build.log` **SUCCESS58s** sau sửa system bars; build đủ task có helper `.local/artifacts/theme03-harness-build.log` **SUCCESS39s**, assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug/assembleRelease/lintRelease. Test APK cuối build riêng **SUCCESS23s** tại `theme03-harness-isolation-build.log` sau điều chỉnh chỉ helper, mã ứng dụng không đổi. **101/101 JVM**,0failure/error/skipped, gồm8test ThemeViewModel mới. Lint0errors,20debug/29release warnings cũ; đã gỡ root background chooser vẽ trùng window, không suppress warning. Decor background và icon system bars đồng bộ palette khi chuyển tối; đã xem ảnh bản review, không còn dải status bar sáng sai màu. Release build dùng HTTPS placeholder api.example.invalid, không deploy.

Unit mới kiểm tra preview không ghi, success/revision, double-click/busy guard, no-op, failure+retry, reselect saved, late completion sau clear và4resource chooser riêng không đổi fallback nghiệp vụ.

Instrumentation mới gồm7ca: Hủy/Back; apply/recreate/reopen; draft qua recreate; Login mở/áp dụng/quay lại giữ instance/form/session bytes; save error/retry;4style×2mode +System; layout/contrast matrix. Matrix inflate **72 tổ hợp**:4style×2night×3font(1/1.5/2)×3viewport(280×560,360×720,840×420), có4card/mode controls/error footer, kiểm tra không overflow/chữ cắt/body contrast≥4.5. Đây không phải benchmark/tự động chứng nhận toàn bộ accessibility.

Test chooser mutation chỉ cho emulator ranchu/goldfish, dùng appearance của AVD **read-only QA**, không điện thoại/tài khoản thật. Save error UI dùng schema tương lai99 làm blocker có kiểm soát rồi sửa lại fixture để retry; lỗi IO commit/rollback được fault-inject JVM ở02, không làm đầy disk. Test Login so sánh bytes encrypted session trước/sau, không xuất contents. Không gọi API logout/login, gửi thư, sửa hồ sơ hoặc upload ảnh.

Lượt AVD đầu1536MB/API37/1080×2400 gặp LOW_MEMORY kill trước instrumentation; riêng Hủy retry1/1 đạt. Batch đầu15ca có13đạt/2lỗi RootViewWithoutFocus ở Login flow và keyboard; ảnh QA phát hiện **System UI isn't responding** che app. Không tính lượt này PASS, không dùng10ảnh bị overlay làm nghiệm thu visual. Log `theme03-ui-first.log`, `theme03-cancel-retry.log`, `theme03-ui-final.log`; ảnh cũ `theme03-screens/` chỉ là bằng chứng lỗi môi trường.

Đã đóng đúng AVD của phiên, không xóa AVD/dữ liệu. Khởi động cold/read-only/headless2GB/2core/720×1280,density320 tạm thời; build đã kết thúc, không chạy song song để giảm áp lực RAM laptop. Lượt cold đầu vẫn LOW_MEMORY, sau đó dừng tạm7app phụ **chỉ trên AVD QA**: Play Store,Messages,Photos,Google Search,Wellbeing,Android System Intelligence và bản OSS. Không dừng SystemUI/GMS, không clear dữ liệu, không đổi máy thật. Các trạng thái dừng chỉ thuộc bản sao read-only, bỏ khi đóng AVD.

Batch `theme03-ui-retest-light.log`14/15 PASS,ca IME fail rồi chạy riêng sau khởi động lại Gboard **1/1PASS16.575s**. Review ảnh tìm ra dải status bar chưa theo dark palette, đã sửa decor background/icons của chooser rồi build lại58s. Batch bản đó `theme03-ui-reviewed.log`14/15,ca IME chạy lại riêng **1/1PASS20.45s** (`theme03-reviewed-ime.log`). Những lượt14/15 không được gọi là full batch PASS.

Sau sửa helper trả đúng instance nhưng còn bootstrap launcher trong mỗi ca, batch `theme03-ui-harness-fixed.log`14/15: IME đạt, ca eye không lấy được window focus. Vì đây là test Login presentation, đã bỏ launcher bootstrap **chỉ trên AVD** để session routing bất đồng bộ không cướp focus; nhánh máy thật giữ workaround cũ. Build riêng test APK23s, không đổi app hoặc assertion. Cold-run đầu `theme03-ui-isolated.log` bị LOW_MEMORY trước test; warm app một lần ngoài batch rồi chạy lại, helper trong từng ca không khởi động Main.

**Bản cuối:15/15 PASS110.614s**,0ignored/failed, tại `.local/artifacts/theme03-ui-isolated-retry.log`:7chooser +4preferences +4Login, gồm physical IME/validation/register navigation/eye. Không gộp các lượt lỗi thành một batch PASS. Ảnh cuối `.local/artifacts/theme03-final-screens/`:10ảnh chooser và5ảnh Login/IME. Đã xem Glass light vàSoft dark của app review để xác minh system bars, không cóoverlay. Ảnh từ AVD đầu cóoverlay không dùng làm nghiệm thu.

Skill `android_ui_verification` hướng dẫn calibration/layout/ảnh và kiểm chứng bằng emulator; ảnh đã giúp tìm/sửa system bars.97resource XML parse0invalid;diff check0,quét mẫu private-key/API-key/JWT trong3class mới0findings (không phải audit secrets toàn repo),artifacts ignored,index rỗng. IME setting hoàn trả0,density override reset; AVD của phiên được đóng sau QA để giải phóng RAM,không xóa AVD/dữ liệu.

## Handoff

Đã đóng gate03 bằng build/JVM/UI/ảnh; không còn lỗi nghiêm trọng phát hiện trong phạm vi này. Không commit/push/deploy. Chưa nghiệm thu điện thoại/OEM,TalkBack thủ công,reboot,OS system-mode transition,performance hoặc luồng API tài khoản thật. Lối mở Hồ sơ được kiểm tra source/ViewBinding/layout,không thao tác hồ sơ/tên/avatar của tài khoản cá nhân; runtime quay về giữ form/phiên đã xác minh tại Login. Main/Profile không bị thêm recreate; ProfileViewModel existing là Activity-scoped,SavedStateHandle draft được load giữ lại khi resetName=false,không sửa logic này. Không suy rộng thành mọi luồng draft nghiệp vụ đã test. Gmail App Password/inbox blocker cũ không nằm trong03 và không bị coi đã đạt. Không cần thao tác thủ công cho03. Tiếp theo chỉ đề xuất **UI-THEME-04 — Login4style**, chưa thực hiện khi chưa được duyệt.
