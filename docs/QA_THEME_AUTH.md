# UI-THEME-05 — Register và Recovery bốn theme

Ngày 19/10/2026. Người dùng duyệt tiếp05 và yêu cầu dùng điện thoại USB, không chạy máy ảo. Phạm vi: Register, ForgotPassword, EmailVerification, ChangePassword; không chuyển06, không nâng dependency hoặc thay nghiệp vụ.

## Thực hiện

Đã đọc AGENTS, toàn bộ đặc tả, audit/roadmap, quyết định/trạng thái/tasks/handoff và source trước khi sửa. Các thay đổi UI/recovery chưa commit có sẵn được giữ nguyên; không reset hoặc tạo/chuyển nhánh.

- AUTH resolver hỗ trợ Original/Gradient/Glass/Soft. MAIN/PREVIEW/VIEWER vẫn fallback Original, LOGIN giữ tích hợp04.
- AuthBackdropView tái sử dụng PremiumBackdropView cho Original và renderer/token Login cho ba theme mới. Không sửa renderer Premium toàn app, không blur/capture bitmap.
- Theme.TakTak.Auth giữ style Premium cũ cho Original, ba theme mới có field/CTA/tonal/headline/notice riêng. Dùng Java/XML/ViewBinding và Material1.14.0 đã khóa.
- Review ảnh USB phát hiện placeholder Register sáng quá nhạt: thêm ttAuthInputStyle (Original rỗng để giữ mặc định cũ, ba kiểu mới dùng onSurface/onSurfaceVariant). Bổ sung assertion tương phản cả chữ và hint trên nền input đã composite; không hạ tiêu chuẩn để giữ ảnh.
- Register/Recovery áp style/local night mode trước super.onCreate/inflate; system bar icons theo màu nền. Không global default night mode, không thêm onResume recreate khiến mất form hoặc reset session.
- Giữ nguyên validation, lời gọi API, Repository/ViewModel, JWT, xác minh email trước recovery, reset-only routing, expiry/retry/cooldown và cơ chế thu hồi phiên server. Password/code vẫn saveEnabled=false; không thêm lưu credential vào Bundle/preferences.
- Chooser cập nhật mô tả phạm vi đã tích hợp, không thêm tab hoặc chức năng.

## File của riêng05

Trong android-client/app/src/main/:

- Java: mới ui/auth/AuthBackdropView.java; sửa ui/auth/AuthFormActivity.java (helper system bars), RegisterActivity.java, RecoveryFormActivity.java và ui/theme/ThemeManager.java.
- XML: mới res/values/auth_themes.xml; sửa res/layout/activity_register.xml, activity_recovery_form.xml và res/values/theme_chooser.xml.

Tests: mới ui/theme/AuthThemeUiTest.java; sửa ThemeFoundationTest, ThemePreferencesDeviceTest và context inflate trong AccountRecoveryUiTest, RegisterM3Test, PremiumScreensTest cho AUTH resource mới. Không bỏ assertion hoặc tắt test cũ. Các file nằm trong src/test hoặc src/androidTest/java/com/taktak/app/.

Hồ sơ: QA_THEME_AUTH.md, PROJECT_STATUS.md, PROJECT_DECISIONS.md, TASKS.md, SESSION_HANDOFF.md. Các thay đổi Git khác có sẵn không được nhận là do05.

## Build và QA

- .local/artifacts/theme05-build.log: BUILD SUCCESSFUL55s, assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug/assembleRelease/lintRelease, offline với helper Use-TaktakJava và release URL placeholder HTTPS chỉ để build. **101/101 JVM**,0failures; lint0errors,21debug/29release warnings. Debug tăng1 cảnh báo UnusedResources cho Theme.TakTak.M3.Register sau khi test dùng đúng AUTH context; giữ resource cũ, không suppress.
- .local/artifacts/theme05-test-harness-build.log: build riêng test APK SUCCESS18s sau sửa helper foreground MIUI; không đổi APK ứng dụng.
- Điện thoại Xiaomi23129RAA4G, Android15,1080×2400, USB authorized. Install -r app/test đều Success; không uninstall/clear data, không mở emulator.
- Lượt đầu theme05-usb-ui.log bị ngắt ở output đang chạy. Log TestRunner trên điện thoại xác nhận8ca,3lỗi khởi chạy Activity từ nền trên MIUI;4preferences và1layout không lỗi. Không coi đây là PASS hoặc lỗi reset server. Appearance sau lượt này đọc được map rỗng/default.
- Đã sửa test harness đưa launcher thực lên foreground một lần trước mở các form trong mỗi ca. Không thay Activity nghiệp vụ hoặc xin quyền hệ thống để né chặn. Không bỏ kiểm tra focus, dữ liệu phiên hoặc assertion.
- QA USB opt-in bằng runner argument taktak.allowAppearanceQa=true. Backup các key appearance trong RAM, restore typed values tại After và assert map khớp; so sánh bytes encrypted session trước/sau nhưng không in nội dung. Reset fixture chỉ trong RAM process test, chỉ clear khi fixture thuộc ca QA. Mọi thao tác submit đều input không hợp lệ hoặc reset session hết hạn, không gửi email/đăng ký/reset thật.

Lượt theme05-usb-ui-retry.log sau sửa foreground: 7/8,121.69giây; phần form chạy hết nhưng After của ca đầu phát hiện bytes file phiên đổi. Đối chiếu source MainActivity → SessionViewModel.restore → repository.profile cho thấy launcher có luồng kiểm tra phiên/refresh sẵn có; đây là khả năng gây thay đổi trong giai đoạn warm-up, chưa xác định nội dung token vì không đọc/in token. Không quy lỗi này thành lỗi theme đã sửa hoặc khôi phục token cũ. Giữ nguyên assertion, không sửa session code.

Chạy lại cùng APK/test và assertion: theme05-usb-ui-final.log **8/8,125.264giây**; restore appearance/map và bytes phiên đều đạt. Không gộp các lượt fail thành PASS. Sau đó sửa placeholder từ review ảnh; bản build mới theme05-build-hint-fixed.log **SUCCESS45s**,101JVM,lint0errors/21debug+29release warnings.

**Bản cuối PASS: theme05-usb-hint-fixed-final.log,8/8 ca,110.116giây**,0ignored/failed:

- Runtime32lượt form (4theme × 2mode × 4Activity), validation,loading,expiry,secret saveEnabled=false.
- Reset fixture4theme: mismatch/same-password bị chặn trước request, toggle eye hai chiều, không reset thật.
- Register/Change dùng bàn phím thật; xác nhận và CTA vẫn cuộn tới được.
-144tổ hợp (4style × 2night × 3fontScale × 3viewport × 2layout), chữ không tràn ngang/cắt chiều cao; text và hint input mới đạt4.5:1 trên nền composite. Original giữ baseline cũ, không tuyên bố đã sửa toàn bộ accessibility của Original.
-4ca preferences/device: lưu/đọc, dữ liệu hỏng/future schema, file QA tách phiên, resource availability đúng role.
-After mỗi ca xác nhận restore appearance khớp và encrypted session bytes không đổi. Kiểm tra local sau batch thấy Gradient/System/schema1/revision1, là trạng thái restored của lượt cuối; không ép đổi về Original chỉ vì lượt trước từng có map rỗng.

38ảnh cuối nằm tại **.local/artifacts/theme05-usb-final/theme05/** sau lần pull cuối (32form +4validation reset +2IME). Ảnh ngay thư mục cha có thể từ lần pull trước khi các ca cuối hoàn tất, không dùng thay cho bản nested cuối. Đã xem Register Glass sáng/Gradient tối, Forgot Gradient/Soft sáng, Verify Soft/Glass tối, Change Original tối và lỗi reset/IME; hint sau sửa rõ hơn, system bars đúng màu, không có cửa sổ nổi. Skill android_ui_verification giúp calibration, kiểm tra bàn phím và review ảnh phát hiện lỗi hint mà matrix cũ chưa bắt.

## Handoff

**PASS phạm vi UI-THEME-05.** Không sửa backend/DB/API/Repository/ViewModel/session/FCM/widget; AuthFormActivity chỉ thêm helper system bars, submit/reset routing hiện có giữ nguyên. Không commit/push/deploy, không mở emulator, không đổi cài đặt màn hình/IME hệ thống của điện thoại. APK cuối đã cài bằng install-r, không uninstall/clear data.

103resource XML parse0invalid, diffcheck0; quét mẫu private-key/API-key/JWT trong9file main/test chính liên quan0findings, không phải audit secrets toàn repo. Index rỗng; artifacts/build đã kiểm tra Git ignore. Danh sách file ở trên là các edit của05; không nhận toàn bộ dirty worktree là của bước này. Không có baseline hash bền vững để khẳng định đối chiếu toàn source sau lượt bị ngắt.

Không cần thao tác thủ công cho UI05; người dùng có thể mở Taktak xem bản mới. Chỉ đề xuất **UI-THEME-06 — Camera/Preview/recipients/navigation/viewer** và dừng chờ duyệt. Các giới hạn dưới đây vẫn chưa nghiệm thu.

## Giới hạn

Không chạy lại Gmail thật, reset thành công/thu hồi phiên qua server, request retry mạng thật, FCM/widget hoặc API của tài khoản cá nhân trong05. Lỗi kết nối dùng làm nội dung notice trong layout matrix, không giả là đã test mạng end-to-end. Không kiểm thử TalkBack thủ công, xoay OS/system night transition đang nhập mật khẩu, reboot/performance hoặc OEM khác. Cơ chế reset/phiên server giữ nguyên; blocker Gmail/App Password từ trước vẫn chưa được giải quyết. B validation đăng ký và UI-THEME-06 chưa được duyệt.
