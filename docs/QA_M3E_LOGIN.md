# Taktak — M3E-01: Foundation + Login

Ngày nghiệm thu: 17/10/2026, Asia/Saigon. **PASS local/USB**, chỉ Foundation + Login theo kế hoạch người dùng đã duyệt. Không chuyển Register/5A, commit hoặc push. Nhánh `chore/push-current-project`, HEAD `5b6b520`; worktree sạch trước phiên.

## UNDERSTAND / PLAN

Đọc AGENTS, toàn bộ TAKTAK_PROJECT, PROJECT_DECISIONS, trạng thái/bàn giao, source và test liên quan. Material **1.14.0 stable đã được khóa**; kiểm tra release chính thức, AAR local và dependencyInsight trong phiên audit trước. Không nâng SDK, dependency hoặc đổi stack. UI UX Pro Max đã đọc/chạy CLI Python3.12.10 trong audit; chỉ giữ hướng Soft Social và nguyên tắc native phù hợp. android_ui_verification dùng để hiệu chuẩn USB, kiểm tra hierarchy và ảnh trước/sau; không chạy uiautomator đồng thời instrumentation.

Áp theme M3E riêng Login, dựng semantic tokens sáng/tối và component styles tái sử dụng. Giữ Register/Main/Camera/Friends dùng theme cũ đến lượt được duyệt. Không thay AuthViewModel/Repository/API/JWT/FCM, backend hoặc database của người dùng.

## IMPLEMENT

- Theme opt-in `Theme.TakTak.M3.Login` kế thừa `Theme.Material3Expressive.DayNight.NoActionBar`; khai báo riêng activity trong Manifest. Không thay application theme.
- Coral Pink/Cream/Lavender, surface/container/on-color/outline/error/inverse roles riêng cho light/night. Không dynamic wallpaper color, font ngoài hoặc custom animation.
- Spacing tokens, headline32sp/body16sp, form tối đa480dp; ConstraintLayout + NestedScrollView có viewport đầy đủ và nội dung wrap khi chữ lớn/màn thấp.
- Brand badge bo20dp, icon vector, field bo20dp với label, autofill/IME/password toggle; button/progress dùng style Expressive thực có trong1.14.0. TextInputLayout dùng style Material3 hợp lệ, không bịa style Expressive chưa có.
- CTA tối thiểu56dp, action phụ48dp; slot progress cố định tránh đẩy form khi loading; notice Lavender sát nút gửi, live-region polite. Lỗi field vẫn do validator hiện tại.
- Giữ toàn bộ ID binding và LoginActivity. `AuthFormActivity.configureInsets` chỉ bổ sung `displayCutout` vào union systemBars/IME; Register cũng nhận cải thiện inset an toàn này nhưng không đổi theme/layout/logic.

## VERIFY

USB: **23129RAA4G / Android15 API35**, serial8e90fdff,1080×2400. Baseline bằng UI dump và screenshot Login đang có; không thay phiên cá nhân. APK debug cập nhật `install -r`, không xóa app data. Debug API dùng loopback127.0.0.1:8080 qua adb reverse. Backend smoke có PostgreSQL tạm riêng/JWT ngẫu nhiên, không sửa source backend hoặc DB dev.

| Kiểm tra thực chạy | Kết quả | Bằng chứng ignored |
|---|---|---|
| Debug/test APK/release build + lintDebug/lintRelease | BUILD SUCCESSFUL15s; lint0errors/3warnings cũ | `.local/artifacts/m3e-login/gradle.txt` |
| Test APK/lint sau sửa harness | BUILD SUCCESSFUL29s | `test-build-reviewed.txt` |
| Unit chạy lại `--rerun-tasks` | **28/28**,0failures/errors/skipped;26tasks thực thi,29s | `unit-fresh.txt`, XML trong app/build/test-results |
| LoginM3Test toàn bộ, host-managed offline bật | **4/4 PASS66.627s**, không skipped | `usb-reviewed.txt` |
| Regression LauncherAuthAcceptanceTest(2),CameraLayoutTest(1),FriendsLayoutTest(1) | **4/4 PASS14.377s** | `usb-regression.txt` |

Bốn ca Login mới:

1. Layout inflate/measure native ở cả light/dark với font1.5×:280×560,360×720,840×420dp. Có lỗi field và notice dài; kiểm tra tràn ngang, clipping text, form≤480dp, touch target và6 cặp text contrast≥4.5:1 ở mỗi cấu hình. Đây là context mô phỏng kích thước, không phải xoay điện thoại/tablet thật.
2. Chuyển local night-mode thực trên Activity, validation rỗng, loading khóa input/CTA/navigation, password toggle và recreate: email còn, password rỗng. Không đổi night-mode hệ thống.
3. IME thật, toàn bộ password field nằm trên bàn phím; chuyển Register hiện có và trở lại Login. Không dùng ActivityScenario mở ngầm; sử dụng launcher exported như QA trước vì MIUI.
4. Đăng ký tài khoản QA duy nhất; host dừng riêng HTTP connector của smoke qua stdin, form báo network error và không lưu phiên. Host bật connector, bấm submit lại, kiểm tra profile đúng owner và tới Camera; logout về Login, store rỗng. Mật khẩu ngẫu nhiên chỉ trong RAM, không argv/log; marker ownership chỉ giữ public userId, ngăn thay phiên cá nhân.

Ảnh: `.local/artifacts/m3e-login/before.png` và `reviewed-screens/`:light,dark,validation,loading,keyboard,offline,retry-success,logout,register-unchanged. Đã xem trực tiếp light/keyboard/offline cuối; logo không còn bong bóng Zalo. Ảnh đầu có overlay được giữ làm lịch sử, không nhận là ảnh sạch. Không ghi password/token ra screenshot ở chế độ hiện mật khẩu.

Lệnh build (PowerShell, tại repository root):

```powershell
. ./scripts/Use-TaktakJava.ps1 -Target Android
cd android-client
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' --offline --no-daemon testDebugUnitTest --rerun-tasks --console=plain
```

Native stderr cảnh báo deprecation bị PowerShell với ErrorActionPreference=Stop xem như exception ở các lượt đầu; lượt ghi nhận dùng Continue quanh lệnh native và kiểm tra exit code/BUILD SUCCESSFUL. Sandbox đọc SDK bị chặn trong audit trước được chạy lại có quyền. Không sửa source hoặc tắt kiểm tra để che lỗi môi trường.

Các lỗi test đã sửa và chạy lại:2/3 ca đầu thất bại vì error icon thay password-toggle sau validation và so sánh chiều cao local với global rect. Test xóa lỗi validation trước khi thử toggle, scroll field rồi click icon thật; kiểm tra local visible bounds và global bottom so với IME. Lượt đủ4 ca sau đó vấp assertion đòi đúng1 Activity RESUMED trong khoảng chuyển Main→Login khi logout; hàm awaitMessage nay chờ lifecycle đến hạn, vẫn yêu cầu đúng thông báo và store rỗng. Không thay timeout/API/limiter hoặc production logic để làm test đạt. Chỉ lượt `usb-reviewed.txt` có đủ4 PASS được nghiệm thu.

## REVIEW / HANDOFF

12 file Android thay đổi:

- Sửa `src/main/AndroidManifest.xml`, `src/main/java/com/taktak/app/ui/auth/AuthFormActivity.java`, `src/main/res/layout/activity_login.xml`.
- Thêm `res/values/m3_colors.xml`, `res/values-night/m3_colors.xml`, `res/values/m3_dimens.xml`, `res/values/m3_styles.xml`.
- Thêm `res/drawable/ic_m3_mail.xml`, `ic_m3_lock.xml`, `m3_brand_badge.xml`, `m3_notice_background.xml`.
- Thêm `src/androidTest/java/com/taktak/app/LoginM3Test.java`.

Hồ sơ: tài liệu này và cập nhật PROJECT_STATUS/TASKS/SESSION_HANDOFF/PROJECT_DECISIONS. Đối chiếu Git với worktree sạch trước phiên xác nhận source server, dependency, LoginActivity, ViewModel/Repository và source các màn khác giữ nguyên. Spec không đổi; credentials/artifacts giữ local, Git index rỗng. Kiểm tra17 file thay đổi:10 XML parse hợp lệ,74 link local hợp lệ,không phát hiện mẫu credentials/conflict/trailing whitespace;git diff --check đạt. Đây là kiểm tra chọn lọc,không audit tuyệt đối mọi lịch sử Git.

Cleanup: smoke nhận Enter và dừng exit0, **0moment/0file/0recipient**,cổng8080 không còn lắng nghe; không ảnh/DB dev bị xóa. QA logout về Login và xóa marker của chính fixture trong test. Điện thoại ngắt USB sau kiểm thử; adb devices không còn thiết bị ở cả môi trường hạn chế và môi trường có quyền. Vì vậy không xác minh lại cold launch cuối phiên hoặc hoàn tất lệnh gỡ reverse8080; không ghi hai thao tác này là PASS. Không cần cắm lại để nghiệm thu các ca đã chạy; khi kiểm thử USB lần sau cấu hình reverse lại theo backend đang dùng. Không thay animation/timeout/network/night-mode hệ thống. Các cluster test/artifacts vẫn ignored trên disk.

Giới hạn: chưa runtime API26/tablet/landscape thật/TalkBack toàn luồng, deny local-network API37, HTTPS runtime hoặc release signing. Release build dùng HTTPS placeholder, không endpoint production. 3 cảnh báo lint cũ:PluralsCandidate và black/white chưa dùng. Backend chỉ smoke/test-compile để phục vụ QA, không chạy lại87 server tests vì source không đổi. Register/Camera/Friends regression chọn lọc không đồng nghĩa redesign hoặc nghiệm thu lại toàn bộ các màn.

**Dừng sau M3E-01 PASS. Đề xuất đúng màn tiếp theo: M3E-02 — Register; chờ người dùng xác nhận.**
