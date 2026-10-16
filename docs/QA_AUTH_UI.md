# Taktak — Giao diện đăng nhập/đăng ký (3A)

Ngày kiểm tra: 16/10/2026. Người dùng duyệt riêng 3A và yêu cầu skill `android_ui_verification`. Kết quả: **PASS trong phạm vi giao diện local**, chưa auth đầu cuối.

## Phạm vi đã triển khai

- LoginActivity/RegisterActivity Java + XML, ViewBinding, Material TextInputLayout, hiện/ẩn password, IME Next/Done, form cuộn và light/dark theo theme hiện có.
- Launcher MainActivity vẫn giữ nguyên vai trò; thêm hai nút mở form. Auth activities không exported, không đổi launcher sang login/Camera hoặc tạo phiên.
- Validation bắt buộc/email, username theo chuẩn lowercase `[a-z0-9_]+`, tên hiển thị, password và nhập lại. Email precheck dùng Android Patterns; server vẫn quyết định hợp lệ/duplicate khi nối API ở 3B.
- Giới hạn tương ứng backend: email 254, username/displayName 255 đơn vị UTF-16; password ít nhất 8 Unicode code point, tối đa 72 byte UTF-8, không NUL, không trim/cắt ngầm. Không lưu/log password hoặc đưa password vào Intent/Bundle; password và confirmation tắt view-state saving.
- Có renderer loading/progress, khóa submit/switch/input và guard bỏ submit lặp lại. Đây là hook UI cho 3B, không phải request mạng: submit hợp lệ chỉ chuyển loading qua một UI post rồi báo rõ chưa nối server, không delay giả hay báo login/register thành công.
- Chưa AuthApi/Repository/AuthViewModel, token storage, refresh, routing phiên hoặc camera. Không tạo ViewModel/Repository rỗng để giả hoàn thành MVVM; nối lớp dữ liệu/state nghiệp vụ tại 3B.

## Cách chạy lại trên Windows

Chọn JDK Android Studio đã khóa, không dùng JRE của VS Code thiếu jlink. Daemon JVM criteria có thể ưu tiên hơn JAVA_HOME theo [Gradle daemon documentation](https://docs.gradle.org/current/userguide/gradle_daemon.html). Phiên này dùng system properties `-D` để giới hạn discovery vào JDK đầy đủ, không sửa cấu hình Java toàn máy hoặc thay phiên bản stack.

Tại Git root, terminal thứ nhất chạy backend PostgreSQL test riêng cho bốn test bootstrap/mạng cũ:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LocalSmokeServer.ps1 -Port 8080
```

Mở AVD Medium_Phone, chờ boot; terminal thứ hai:

```powershell
. .\scripts\Use-TaktakJava.ps1 -Target Android
Push-Location android-client
try {
    .\gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" `
        '-Dorg.gradle.java.installations.auto-detect=false' --no-daemon `
        assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android verification failed.' }
} finally {
    Pop-Location
}
```

Release build/lint đã kiểm tra riêng bằng cùng flags JDK, thêm `-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/ assembleRelease lintRelease`. Đây là URL HTTPS placeholder để kiểm tra cấu hình, không phải API thật hoặc bản release đã ký/phát hành.

Nhấn Enter để đóng smoke server sau test. Instrumentation có thể uninstall APK khi xong; muốn xem thủ công, cài lại `android-client/app/build/outputs/apk/debug/app-debug.apk`, mở launcher rồi chọn Đăng nhập/Tạo tài khoản. Không am-start trực tiếp auth từ shell vì activities không exported.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| Debug assemble/unit/lint, bản cuối | BUILD SUCCESSFUL |
| Unit XML | 8/8 PASS: 7 validator + 1 test cũ; 0 failures/errors/skipped |
| Instrumentation XML, bản cuối | 12/12 PASS: 8 auth UI + 4 bootstrap/mạng cũ; 0 failures/errors/skipped |
| Release assemble/lint | BUILD SUCCESSFUL với URL HTTPS placeholder, chưa signing/deploy |
| Lint debug/release | 0 errors; 2 warning UnusedResources cho black/white có sẵn; không baseline/suppress hoặc bỏ lint |
| Manifest release | Chặn cleartext, không ACCESS_LOCAL_NETWORK debug; hai auth activity không exported |
| Install/launch | Install Success; MainActivity cold launch Status ok |
| Backend regression kết nối từ app | Health 200/UP, protected me không JWT 401 qua alias 10.0.2.2, không ADB reverse |

Tám test UI phủ điều hướng/back; required/email errors; username/confirmation/submit hợp lệ không giả auth; loading/duplicate guard; password không restore ở login/register; dark form scroll; ô confirmation nằm trên IME và nút submit vẫn cuộn tới sau khi đóng IME.

## Kiểm tra trực quan theo skill

AVD Medium_Phone read-only do phiên này mở, Android 17 API 37, kích thước đã đo bằng `wm size`: 1080×2400. Trước thao tác đọc UIAutomator dump, chỉ tap tâm bounds của resource-id đã tìm thấy, chờ animation và chụp screenshot; dùng logcat AndroidRuntime/TestRunner cho native Java, không dùng ReactNativeJS.

Skill giúp phát hiện lỗi mà 11 test đầu chưa bắt được: ô Nhập lại mật khẩu bị IME che. Đã chuyển insets sang FrameLayout bọc ngoài để NestedScrollView có viewport thực sự nhỏ lại, yêu cầu cuộn focused rectangle sau insets; thêm test IME, chạy lại 12 test PASS và xem screenshot xác nhận ô focus nằm trên bàn phím. Không đánh dấu ảnh trước sửa là đạt.

Artifact ignored trong `.local/artifacts/3a/`: `login-final.png/xml`, `login-errors.png/xml`, `register.png/xml`, `register-dark.png/xml`, `register-keyboard.png` (lỗi trước sửa), `register-keyboard-fixed.png/xml` (đã xem và đạt), `instrumentation-results.xml`. Ảnh light/dark, lỗi required và IME đã được xem trực tiếp; screenshot không chứa credential thật. Khi đổi theme, một lần kiểm tra không thấy trang login đã dừng theo fail-fast; đọc lại bounds rồi chụp/xác minh đúng trang, không coi lần đó là PASS.

Các lỗi build trung gian đã sửa nguyên nhân: daemon chọn VS Code JRE thiếu jlink (đổi discovery sang JDK đầy đủ bằng -D; -P không có tác dụng cho daemon); parent style Widget.Material3.TextView không tồn tại (dùng platform TextView + Material typography); XML accessibilityHeading cần API28 (dùng ViewCompat cho min26). Không hạ SDK hoặc nâng dependency để né lỗi. Warning Gradle/protobuf deprecated từ toolchain được giữ và báo, không bỏ test.

## Giới hạn và điểm dừng

Chỉ nghiệm thu UI/validation/loading hook, không nghiệm thu đăng nhập thật, tạo tài khoản thật, duplicate username từ API, token/refresh hoặc Camera. Chưa chạy UI mới trên máy vật lý, API26, landscape/font-scale hoặc Android auth đầu cuối; không lấy bằng chứng máy thật bootstrap cũ làm test UI mới. Backend 39 test 2D là kết quả phiên trước, không chạy lại cả suite trong 3A; chỉ smoke backend/mạng đã chạy mới.

Không đổi package/stack/dependency/Gradle Groovy, migration, Firebase JSON, README hoặc source server; không commit/push/deploy. Không có blocker cho 3A. Tiếp theo đề xuất duy nhất: **3B — nối AuthApi/Repository/ViewModel, phiên mã hóa và refresh đồng bộ**, cần duyệt riêng.
