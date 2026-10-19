# UI-THEME-04 — Login bốn theme

Ngày 19/10/2026. Người dùng duyệt riêng **Login 4 theme** sau gate03. Đã đọc đặc tả, AGENTS, quyết định/roadmap/trạng thái và source trước khi sửa. Worktree đang có thay đổi UI/recovery từ trước; không reset, commit, push, deploy hoặc đổi nhánh.

## Triển khai

- Một LoginActivity/layout/ViewBinding, không nhân bản form hoặc Repository/ViewModel. Manager áp style và local night mode trước super.onCreate/inflate.
- Original giữ nền tối, màu và gradient cũ kể cả khi chọn Light; đây là ngoại lệ đã ghi trong audit. Gradient dùng coral/lavender, Glass dùng cyan/lavender và panel tĩnh, Soft dùng pastel cùng họa tiết nhẹ; ba kiểu mới có tài nguyên sáng/tối. Không tuyên bố khớp từng pixel với ảnh tham khảo hoặc đã phủ các màn khác.
- Nền Canvas dùng shader được tạo theo kích thước; không blur/capture bitmap hoặc animation loop. TextInputLayout/MaterialButton vẫn dùng component native; CTA AppCompat giữ RippleDrawable. Token Login riêng, không sửa palette chooser toàn cục hoặc widget RemoteViews.
- Hoàn thiện màu chữ/icon/field/error/notice/CTA/system bars theo theme. Đầu tím của CTA Gradient sáng đã được tăng độ sáng riêng để đạt tương phản chữ 4,5:1; không hạ assertion.
- Lựa chọn từ chooser chỉ áp vào Login khi RESUMED, idle, form trống, chưa điều hướng success/reset. Có draft/loading thì hiển thị thông báo riêng và hoãn recreate; không xóa email/password, không lưu password vào Bundle/preferences, không dùng default night mode toàn ứng dụng.
- Chỉ LOGIN được khai báo hỗ trợ đủ4theme. AUTH/MAIN/PREVIEW/VIEWER vẫn fallback Original; Register/Recovery thuộc05 chưa triển khai. Disclaimer trong chooser phản ánh đúng phạm vi.
- Giữ nguyên submit/validation/API/JWT/recovery/session, backend/database/FCM và phiên bản Material1.14.0.

## File của riêng bước04

Đường dẫn dưới đây tương đối với repository; các thay đổi cũ khác trong Git không thuộc bước này.

- Java main: ui/auth/LoginActivity.java, ui/auth/LoginBackdropView.java, ui/theme/ThemeManager.java dưới android-client/app/src/main/java/com/taktak/app/.
- XML main: layout/activity_login.xml, values/login_design.xml, values/theme_chooser.xml, color/login_field_stroke.xml, drawable/login_notice.xml; mới values/login_themes.xml, values/login_theme_colors.xml, values-night/login_theme_colors.xml, drawable/login_theme_primary.xml, drawable/login_theme_secondary.xml dưới android-client/app/src/main/res/.
- Tests: mới src/androidTest/java/com/taktak/app/ui/theme/LoginThemeUiTest.java; cập nhật ThemePreferencesDeviceTest, src/test/java/com/taktak/app/ui/theme/ThemeFoundationTest.java, ThemeViewModelTest.java cho availability LOGIN, vẫn giữ assertion fallback các role khác.
- Hồ sơ: tài liệu này và PROJECT_STATUS, PROJECT_DECISIONS, TASKS, SESSION_HANDOFF.

## Kiểm thử và bằng chứng

Build offline, không chạy Gradle đồng thời AVD để giảm áp lực RAM. Dùng helper Use-TaktakJava và tham số toolchain local như các bước trước; release URL HTTPS placeholder chỉ phục vụ build, không deploy.

~~~powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon --max-workers=2 assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease
~~~

- Build đầu gặp lỗi namespace colorPrimary (AppCompat thay vì Material với non-transitive R); đã sửa, không nâng dependency.
- Build cuối .local/artifacts/theme04-build-contrast-fixed.log: **BUILD SUCCESSFUL39s**, debug/test/release và lint; **101/101 JVM**,0failures; lint0errors,20debug/29release warnings có sẵn.
- AVD Medium_Phone API37, read-only/headless,2GB RAM,720×1280,density320. Không dùng điện thoại/tài khoản cá nhân. Cài app/testAPK đều Success; bật IME QA và đo kích thước trước tương tác theo skill android_ui_verification.
- Các lượt cold instrumentation theme04-ui.log và theme04-ui-final.log bị dừng trước test: exit-info xác nhận LOW_MEMORY, không phải kết quả PASS.
- Lượt theme04-ui-retry.log: **19/20**,104.391s, lỗi duy nhất CTA Gradient sáng tương phản4.208:1; đã sửa token, giữ bài test.

**Batch cuối PASS: 20/20 ca, 124.809 giây**, không bỏ qua test, tại .local/artifacts/theme04-ui-final-retry.log:

- 5 ca LoginThemeUiTest: 4 theme × 2 mode thực tế; validation, loading, eye, không save password; 72 tổ hợp layout/font/night và tương phản; hoãn draft/loading rồi áp khi an toàn; IME thật và điều hướng Register/Forgot/Verify ở ba theme mới.
- 7 ca chooser hồi quy, gồm quay về đúng Login instance và giữ nguyên email/password cùng bytes file phiên; 72 tổ hợp layout chooser.
- 4 ca preferences/device/resource contracts, vẫn kiểm tra fallback các role chưa tích hợp.
- 4 ca Login Original hồi quy, gồm IME, validation, eye và điều hướng.

Ảnh cuối: .local/artifacts/theme04-final-screens/ (8 Login sáng/tối + 3 IME); .local/artifacts/theme04-original-regression-screens/ (5 ảnh hồi quy). Đã xem đủ8ảnh theme và ảnh Glass/IME: màu system bars đúng sáng/tối, không có overlay, CTA vẫn thấy được khi bàn phím mở. Màn nhỏ cần cuộn tới các thao tác ở dưới; test đã cuộn và mở được Register/Recovery. Ảnh review trước sửa tương phản ở theme04-review-screens không thay cho ảnh nghiệm thu cuối. Log AndroidRuntime cuối không có crash exception.

## Review và bàn giao

**PASS phạm vi UI-THEME-04.** Skill android_ui_verification được dùng để đo viewport, kiểm tra IME/tương tác và review ảnh thực, không dùng ảnh để suy ra backend đã hoạt động.

Snapshot trước sửa gồm308file main/source/script/deploy: chỉ8file cũ trong Android main đổi có chủ đích,5file main mới, không xóa file. Toàn bộ83file server/src, deploy/scripts và source API/Repository/ViewModel/session/FCM/widget có trong snapshot giữ nguyên hash. Không thay Material/Gradle/dependency hoặc Manifest trong04. Không ghi đè các thay đổi cũ ngoài phạm vi.

102resource XML parse hợp lệ, diff check không có lỗi khoảng trắng, index rỗng. Quét mẫu private-key/API-key/JWT trong source/resources/test mới hoặc thay đổi của04 không có kết quả; đây không phải audit secrets toàn repository. Artifacts/build được Git ignore, không đưa credential hoặc ảnh QA vào Git.

Đã hoàn trả IME0/density reset và đóng đúng hai process emulator/qemu của AVD được mở trong phiên, không xóa AVD hoặc dữ liệu. Không cần thao tác thủ công cho04. Dừng tại đây; chỉ đề xuất **UI-THEME-05 — Register/Recovery bốn theme**, chờ người dùng duyệt. Các hạn chế bên dưới không được đánh dấu đã kiểm thử.

## Giới hạn

Không kiểm thử đăng nhập/network error/reset/logout thực qua server hoặc gửi Gmail trong04; test này chỉ presentation/điều hướng và kiểm tra dữ liệu phiên không đổi. Trạng thái lỗi kết nối được đưa vào layout matrix để kiểm tra bố cục, không giả thành request mạng đã test. Không nghiệm thu OEM/điện thoại, TalkBack thủ công, OS system-mode transition khi đang có draft, reboot hoặc performance. Trì hoãn app-style không đồng nghĩa giữ password qua mọi configuration change do OS; password vẫn không save vào view state theo chính sách cũ. Blocker App Password/Gmail trước đây giữ nguyên, ngoài phạm vi04.
