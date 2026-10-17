# Taktak — Điều hướng phiên và logout UI (3C)

Cập nhật sau mốc3C: người dùng đã duyệt và hoàn tất3D; Camera placeholder nêu trong hồ sơ3C bên dưới đã được thay bằng CameraX. Kết quả3C giữ nguyên theo lịch sử; suite hiện31 test chưa chạy toàn bộ, xem [QA_SELF_MOMENT.md](QA_SELF_MOMENT.md).

Ngày: 16/10/2026. **3C PASS trong phạm vi điều hướng phiên và logout UI**, gồm bằng chứng local trước đó và nghiệm thu Android 15 dưới đây. Camera vẫn là placeholder; chưa triển khai 3D, redesign hoặc tự commit/push/deploy.

## Nghiệm thu USB hoàn tất — 16/10/2026, khoảng 22:57–23:00

- Sau yêu cầu “thử lại lần nữa”, test APK cài `-r` **Success**. Không đổi restriction, thiết lập bảo mật hoặc clear/uninstall app data. Trước test, secure preferences chỉ có keyset AndroidX, không có phiên cá nhân; harness từ chối thay thế phiên không do fixture sở hữu.
- Thêm `LauncherAuthAcceptanceTest` trong androidTest: mở launcher exported bằng UiAutomation shell, thao tác view theo ID trên main thread, chờ trạng thái có timeout, tài khoản ngẫu nhiên trên PostgreSQL smoke riêng. Không thêm hook production hoặc truyền password/token qua shell/Intent/log. Marker QA chỉ chứa `WAITING_FOR_USB_RESTORE`.
- Build `assembleDebugAndroidTest lintDebug`: **BUILD SUCCESSFUL 32 giây**, lint **0 errors / 3 warnings có sẵn**. Không phải chạy lại release build hay unit suite.
- `realLoginFromLauncherAndEncryptedSession`: **1/1 PASS, 7.467 giây**. Register API thật, sai mật khẩu không lưu phiên, login UI đúng → Camera destination, phiên mã hóa tồn tại. Giữ riêng phiên fixture để kiểm tra cold process rồi logout ở phép thử sau.
- Host `am force-stop` → launcher: **COLD / Status ok / 1667 ms**, UI dump có cameraTitle: **PASS đọc lại phiên sau tiến trình chết**, không chỉ graph RAM.
- `restoredLauncherRetryAndLogoutRevokesRefresh`: **1/1 PASS, 15.505 giây**. Gỡ riêng reverse tcp:8080 trước test: UI báo lỗi kết nối, Camera bị chặn, phiên vẫn còn. Khi có marker QA, host khôi phục reverse; nút Thử lại xác minh phiên thành công → Camera. Logout UI → Login root task, clear secure session, refresh cũ bị server trả 401.
- `com.taktak.app.data.session.AuthBackendTest`: **4/4 PASS, 3.458 giây**. Encrypted persistence/không plaintext, invalid access → refresh rotation/replay, wrong password/duplicate/revoked refresh, đổi origin và ciphertext hỏng fail-closed. Invalid access không đồng nghĩa đã chờ JWT hết hạn thật.
- Sau logout, force-stop/launcher lần nữa: **COLD / Status ok / 1818 ms**, UI dump có emailInput, không cameraTitle. Secure preferences không còn entry phiên (không in nội dung); không xóa dữ liệu cá nhân.
- Tổng **6/6 instrumentation chọn lọc PASS trên Android 15/API35, model 23129RAA4G, 1080×2400**. Đã xem 5 screenshot: wrong-password, signed-in, usb-disconnected, restored, logged-out. Ảnh có PiP ứng dụng khác ở góc dưới; không đổi cài đặt hoặc coi PiP là UI Taktak. Artifact chỉ lưu ignored tại `.local/artifacts/3c-physical/`, không đưa ảnh cá nhân vào Git.
- Giới hạn runner: suite cũ kẹt ở `ActivityScenario.launch(LoginActivity)`; log hệ thống ghi START result code 102 và phone ở launcher. Khi mở app bình thường, intent khác intent ActivityScenario đang chờ. Đã chủ động force-stop runner; dòng `Process crashed` của lần đó là kết quả ngắt kiểm thử, không phải bằng chứng Java crash của app. **Không tuyên bố 25/25 test máy thật PASS**. Chưa xác định toàn bộ nguyên nhân chính sách OEM; không nới manifest/quyền điện thoại để làm test chạy.
- Cleanup: logout fixture, giữ app/test APK, gỡ reverse do phiên tạo; smoke server exit 0, cổng 8080 không còn listen, runner đóng cluster test riêng. Không reset DB cá nhân, sửa backend/Firebase/dependency hoặc commit/push/deploy. Đề xuất **3D**, chờ duyệt.

### Chạy lại riêng trên điện thoại được phép kiểm thử

Chuẩn bị smoke backend tạm, đúng serial, debug APK với `-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/` và reverse tcp:8080. Không chạy fixture AVD tùy tiện trên điện thoại có phiên cá nhân. Hai test launcher mặc định tự logout fixture, có thể chạy riêng class khi backend kết nối:

```powershell
adb -s <serial> shell am instrument -w -r -e class com.taktak.app.LauncherAuthAcceptanceTest com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s <serial> shell am instrument -w -r -e class com.taktak.app.data.session.AuthBackendTest com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Quy trình cold/network đã chạy tách hai method: login dùng `-e taktakKeepSession true`; host force-stop/cold launch và gỡ reverse; restore dùng `-e taktakAwaitUsbRestore true`. Host đợi marker QA rồi khôi phục reverse trong 45 giây. Nếu ngắt giữa quy trình, chỉ logout phiên fixture có marker sở hữu; không clear data. Không chạy uiautomator dump đồng thời instrumentation vì tranh UiAutomation.

## Lịch sử chưa đạt — 16/10/2026, khoảng 22:44–22:45

Cập nhật sau yêu cầu “tiếp tục”: thử lại install streaming và install chuẩn `--no-streaming -r`, cả hai vẫn INSTALL_FAILED_USER_RESTRICTED. Không chạy test hoặc khởi động lại backend vì test APK chưa cài được. `dumpsys user` có dòng no_install_unknown_sources; chưa đủ để quy kết restriction áp dụng user/profile nào hoặc là nguyên nhân duy nhất. Không sửa restriction/secure settings. Cần người dùng kiểm tra quyền cài qua USB cho `com.taktak.app.test`, có thể tham khảo [hướng dẫn Xiaomi về mục Install via USB](https://www.mi.com/global/support/faq/details/KA-497761/) (hướng dẫn cho REDMI 13C, tên/vị trí mục trên HyperOS máy hiện tại có thể khác). Nếu quyền đã bật vẫn lỗi, cần ảnh màn cài qua USB/thông báo chặn để chẩn đoán tiếp thay vì lặp install.

- ADB nhận model 23129RAA4G, Android 15/API 35, 1080×2400, màn hình Awake/unlocked. Kiểm tra chỉ đọc bằng run-as xác nhận chưa có shared_prefs/auth_session_v1.xml; không đọc token/password hoặc xóa phiên cá nhân.
- Build debug + test APK với property `-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/`: **BUILD SUCCESSFUL, 14 giây**. Chỉ thay cấu hình build qua command line, không sửa source/Gradle/release URL.
- Chạy backend/PostgreSQL smoke tạm riêng, health runner sẵn sàng port 8080; thêm reverse tcp:8080 trên đúng điện thoại (trước đó không có mapping). Không dùng/reset database cá nhân.
- `adb install -r` app debug **Success**, giữ app data. Test APK `com.taktak.app.test` bị từ chối **hai lần** với `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. Đây là thông báo hệ thống, không suy đoán người dùng chủ động bấm hủy hay nguyên nhân chính sách cụ thể. Không đổi cấu hình bảo mật điện thoại để vượt chặn. Manifest test không có testOnly; không có bằng chứng lỗi do thiếu flag test-only.
- Bộ 25 instrumentation **chưa chạy trên điện thoại**, vì command dừng ngay sau install test APK thất bại. Không tính build/install app hoặc 25 test AVD trước đó là nghiệm thu auth máy thật.
- Phần độc lập đã chạy: cold launch MainActivity **Status ok, COLD, TotalTime 1572 ms**, sau đó UI dump có emailInput của Login. Đã chụp và xem ảnh dark mode đúng Taktak; **PASS launcher signed-out → Login trên máy thật**. Chưa kiểm tra login thật, persistence qua process chết, retry mạng hoặc logout trên điện thoại.
- Artifact local ignored: `.local/artifacts/3c-physical/signed-out.xml` và `signed-out.png`. Chưa có instrumentation result thành công. Giữ app debug vừa cập nhật trên điện thoại; không uninstall/clear data, không đổi theme/cài đặt cá nhân.
- Cleanup: gỡ đúng reverse tcp:8080 do phiên tạo, smoke server exit 0 và runner thu hồi cluster test riêng. Chờ người dùng cho phép cài APK kiểm thử trên điện thoại rồi tiếp tục trong 3C đã duyệt; chưa chuyển redesign hoặc 3D.

## Phạm vi

- MainActivity vẫn là launcher. SessionViewModel/Repository đọc phiên mã hóa và xác minh GET me (tự refresh theo 3B) trên worker; không tin riêng sự tồn tại hoặc expiry do client lưu để mở Camera.
- Không có phiên → LoginActivity; xác thực thành công → MainActivity ở điểm đến Camera. Camera mới là placeholder, chưa preview/permission CameraX/chụp/upload. Bốn tab hoàn chỉnh thuộc 5A.
- Login và logout dùng NEW_TASK + CLEAR_TASK để không Back về màn của phiên trước. Main kiểm tra lại khi vào foreground, không lưu nội dung phiên vào view state.
- Mất mạng/timeout/5xx không xóa phiên hoặc cho mở Camera; có lỗi an toàn và nút Thử lại/Đăng xuất. Debug API 37 thiếu quyền mạng local có gate riêng, người dùng nhấn Thử lại để xin quyền.
- Logout gọi endpoint với refresh hiện tại và luôn thử xóa token local. Mạng/server lỗi nhưng local xóa được → Login kèm cảnh báo chỉ xác nhận logout local. Nếu clear storage lỗi → giữ màn lỗi, không giả logout thành công; thử Logout lại để xóa, trạng thái lỗi còn giữ khi recreate ViewModel owner.
- Chưa đăng ký device/FCM, nên chưa có device registration để unregister. Phần này thuộc bước 6, không gọi endpoint chưa triển khai.

## File triển khai

- MainActivity, activity_main.xml, strings.xml: launcher/session gate, placeholder Camera và nút logout/retry.
- SessionViewModel mới: state machine CHECKING/LOGIN/CAMERA/PERMISSION/ERROR, worker, busy guard và LiveData không chứa token/password/profile.
- AuthRepository: thêm hasSession/logout sử dụng SessionManager hiện có; không tạo transport hoặc store thứ hai ở production.
- LoginActivity/AuthFormActivity: điều hướng sau login, hiển thị thông báo logout/hết phiên qua whitelist resource; AuthViewModel bổ sung mapping NETWORK.
- AuthUiTest/ExampleInstrumentedTest: cập nhật kỳ vọng launcher mới, giữ kiểm thử auth/validation/IME. SessionRoutingTest mới bổ sung restore/logout/back stack, revoke, unavailable backend/retry, timeout, permission gate và clear storage lỗi.

## Kết quả và lỗi đã phát hiện

- Build debug/test APK/release + lint debug/release đã chạy thành công. Release dùng URL HTTPS placeholder, không deploy/sign phát hành.
- 20 unit test đã chạy lại do source thay đổi: 12 session + 7 validator + 1 mẫu, 0 failures/errors/skipped.
- Lần instrumentation đầu: 21 test, 4 failures. Ba failure bắt được thông báo logout/hết phiên bị LiveData initial state che; đã sửa renderer dùng initialNotice từ whitelist khi state còn idle. Failure IME do chưa hiện bàn phím tại mốc cố định 1,2 giây; test nay yêu cầu hiện IME và chờ visibility thực (có timeout), vẫn assert viewport/focus và không bỏ kiểm thử.
- Hai lần runner (PID 12849 và 19014) báo Process crashed trước khi test bắt đầu; `dumpsys activity exit-info com.taktak.app` xác nhận cả hai **reason=3 LOW_MEMORY**, không phải Java exception của Taktak. Lần thứ hai là fixture cold start riêng sau suite, không phải suite 25 test đã đạt. Cold launch thường thành công; chạy lại trong task foreground giúp suite đầy đủ chạy được. Không kết luận từ crash UWB không liên quan, không nới kiểm thử/đổi SDK/GPU/xóa data. Hạn chế RAM AVD còn cần lưu ý khi chạy lại.
- Suite bản cuối: **25/25 PASS, 122.828 giây**, 0 failed/ignored theo runner; gồm 9 AuthUiTest, 8 SessionRoutingTest, 4 AuthBackendTest và 4 bootstrap. 8 test routing kiểm tra phiên vắng/hợp lệ, đọc lại disk/recreate/logout/back stack/revoke server, refresh bị thu hồi, mất kết nối và retry hồi phục, read timeout thật, permission gate và clear storage lỗi giữ màn lỗi qua recreate rồi retry thành công.
- Fixture cold start chạy riêng **1/1 PASS**, 4.077 giây. Sau đó `am force-stop` → `am start`: LaunchState COLD, 1818 ms; UI dump có cameraTitle, đã xem screenshot. Logout bằng tap tâm bounds `[63,1339][1017,1486]` đã đọc từ UI dump, chờ animation, kiểm tra thông báo “Đã đăng xuất.”; Back → force-stop → launcher COLD lần nữa về emailInput, không cameraTitle. Không dùng sự tồn tại của graph RAM để kết luận persistence.
- Đã xem 4 ảnh: Camera từ suite, Camera sau cold process, logout và lỗi kết nối. Hiệu chỉnh màn 1080×2400; screenshot suite dùng UiAutomation, cold start dùng adb screencap và UI dump sau suite. Không chạy uiautomator đồng thời tranh UiAutomation của test runner.
- Lint bản hiện tại: 0 errors, 3 warnings có từ 3B (PluralsCandidate giây tiếng Việt, hai màu black/white chưa dùng). Cảnh báo resource bootstrap_message phát sinh khi đổi layout đã được xử lý bằng tái sử dụng resource hiện có, không suppress.

## Chạy lại trên Windows

Chuẩn bị JDK/SDK/Firebase JSON local như [QA_ANDROID_AUTH.md](QA_ANDROID_AUTH.md). Mở AVD test read-only, xác nhận đúng serial; không dùng connected test mặc định khi có điện thoại cá nhân. Suite thay đổi phiên của app trên AVD và chỉ sử dụng database smoke tạm.

Terminal backend (Enter để dừng runner và thu hồi database test riêng):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Start-LocalSmokeServer.ps1 -Port 8080
```

Build từ root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
. ./scripts/Use-TaktakJava.ps1 -Target Android
Push-Location android-client
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' assembleRelease lintRelease --console=plain
Pop-Location
adb devices -l
adb -s emulator-5554 install -r android-client/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r android-client/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Mốc suite AVD cũ đã chạy là `OK (25 tests)`. Sau khi thêm 2 test launcher, suite mặc định có 27 test; **chưa chạy lại toàn bộ 27 test**, không đổi bằng chứng lịch sử thành 27 PASS. Không chỉ dựa exit code adb. Không dùng `taktakKeepSession` cho suite đầy đủ: chỉ dùng cho fixture đơn kiểm tra cold process, sau đó phải logout; không có argument này trong production app.

Fixture cold process (chỉ AVD test; sau phép thử phải logout qua UI):

```powershell
adb -s emulator-5554 shell am start -W -n com.taktak.app/.MainActivity
adb -s emulator-5554 shell am instrument -w -r -e class 'com.taktak.app.SessionRoutingTest#validStoredSessionEntersCamera' -e taktakKeepSession true com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am force-stop com.taktak.app
adb -s emulator-5554 shell am start -W -n com.taktak.app/.MainActivity
```

## Giới hạn và bảo mật

- Timeout test dùng socket loopback thật không trả response; unavailable test dùng loopback đóng. Test injection graph bằng reflection chỉ trong androidTest, không thêm setter production hoặc đổi BASE_URL app. Permission test gọi gate với networkAllowed=false rồi retry khi quyền đã cấp; chưa đồng nghĩa đã kiểm thử bấm Deny trên dialog hệ thống.
- Auth trên máy thật Android 15 đã có 6 test chọn lọc và cold-process kiểm chứng ở đầu tài liệu; các lần thiếu ADB/cài bị chặn là lịch sử. API 26, release runtime HTTPS thật và expiry chờ đủ thời gian chưa chạy; test revoked/invalid JWT không được ghi thành test chờ expiry thật. Không chạy fixture xóa phiên trên điện thoại có dữ liệu cá nhân.
- Logout offline không thể bảo đảm server đã revoke; JWT access đã cấp còn hợp lệ tới exp theo backend. Nếu disk không xóa được, phải thử logout lại; không tuyên bố token đã bị xóa bền vững.
- Skill auth hướng dẫn kiểm tra credential lifecycle và fail-closed; skill UI dùng để hiệu chỉnh màn, kiểm tra screenshot/runtime và lỗi IME. Không thay thế kiểm thử tự động bằng việc nhìn ảnh.
- Artifact trong `.local/artifacts/3c/` đã ignore; unit/lint ở app/build. Không ghi token/password vào Intent, screenshot, log hoặc hồ sơ; backend, migration, Firebase JSON và dependency không đổi.

Điểm dừng hiện tại: **3C PASS; chờ duyệt 3D — CameraX → upload chỉ mình tôi → tải lại bằng JWT**. Chưa triển khai 3D trong phiên này. Đoạn review/cleanup AVD bên dưới là lịch sử, không phải trạng thái kết nối điện thoại hiện tại.

Review/cleanup cuối: policy release/URL/auth activity PASS; không quyền CAMERA, không thêm CameraX trước 3D. Firebase JSON hash không đổi, server/README/Gradle không diff và Git index rỗng, HEAD 8766f26. Quét chọn lọc 6 hồ sơ và source auth/data/ViewModel/Main PASS cho mẫu credential/logging/conflict; 56 link local hợp lệ, không Kotlin source; git diff --check đạt. Các phép quét không phải audit bí mật toàn lịch sử. Backend và AVD do phiên tạo exit 0, cổng 8080/5554/5555 đóng, cluster test được runner thu hồi; sau cleanup ADB trống. Không commit/push/deploy hoặc xóa dữ liệu cá nhân.
