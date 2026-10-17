# Taktak — Android auth và quản lý phiên (3B)

Ngày kiểm tra: 16/10/2026. Phạm vi được duyệt: **3B**. Kết quả: **PASS local trên emulator**, chưa nghiệm thu toàn bộ giai đoạn 3 trên máy thật.

Mốc này giữ làm bằng chứng 3B. Người dùng đã duyệt và triển khai 3C tiếp theo; kết quả routing/logout mới tại [QA_SESSION_ROUTING.md](QA_SESSION_ROUTING.md). Các đoạn chưa có routing bên dưới phản ánh thời điểm 3B.

## Khôi phục và phạm vi

Khi tiếp tục, Git ở `main`, HEAD `8766f26`, working tree sạch. Commit này đã chứa code 3B và test nhưng hồ sơ còn ghi 3A; không tạo lại code hoặc tự commit. Phiên tiếp tục chạy lại kiểm thử, review và cập nhật hồ sơ. Không sửa backend, schema, migration, dependency, package hoặc Firebase JSON.

## Triển khai hiện có đã xác minh

- `data/model/AuthModels`: request/response khớp DTO auth và hồ sơ; các đối tượng chứa dữ liệu nhạy cảm có `toString()` đã che nội dung.
- `data/remote/AuthApi`, `UserApi`, `ApiClient`, `ApiFailure`: Retrofit/Gson; tách client auth không có bearer khỏi client nghiệp vụ. Chỉ gắn token vào cùng scheme/host/port và API path của backend; không gửi tới auth endpoint hoặc theo redirect. Không HTTP body logger/cache.
- `data/session`: EncryptedSharedPreferences và Android Keystore, ghi cặp token cùng expiry bằng commit; gắn với BASE_URL, đổi backend thì xóa phiên cũ. Lỗi mã hóa/lưu trữ không chuyển sang plaintext. File phiên bị loại khỏi backup/cloud/device-transfer; app vẫn tắt backup.
- `SessionManager`: khóa đồng bộ refresh/login/logout; các 401 dùng token cũ tái sử dụng cặp đã rotate, tối đa một lần replay. Refresh 400/401 hoặc replay vẫn 401 thì xóa phiên phù hợp. Lỗi mạng/5xx giữ phiên và gộp lỗi refresh trong 5 giây để tránh gọi dồn. Generation guard chặn login/response cũ phục hồi hoặc trả dữ liệu của phiên đã đổi.
- `AuthGraph` cung cấp repository singleton trong process. Các consumer production phải dùng chung graph này, không tạo nhiều SessionManager độc lập cho cùng file phiên.
- `AuthRepository` thực hiện register/login/me ngoài main thread; login lưu token rồi xác minh hồ sơ. Register thành công không giả đăng nhập.
- `AuthViewModel`: worker executor, LiveData trạng thái không chứa token/password; chống gửi lặp, cooldown theo Retry-After, lỗi UI bằng thông báo local thay vì raw server/proxy body. Activity giữ validation/IME và quan sát trạng thái qua lifecycle; mật khẩu không lưu vào view state, xóa sau thành công.
- Debug Android 17 xin quyền mạng local trước request, xử lý từ chối và yêu cầu bấm gửi lại sau khi cấp quyền; không giữ password trong callback xin quyền. Release không khai báo quyền debug này.

## Kết quả thực tế

| Kiểm tra | Kết quả |
|---|---|
| `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease` | PASS; release dùng URL HTTPS placeholder, không deploy/sign phát hành |
| `testDebugUnitTest --rerun-tasks` | PASS: 20 test, 0 failures/errors/skipped; toàn bộ task của lệnh chạy lại, không chỉ UP-TO-DATE |
| `lintDebug`, `lintRelease` | PASS: 0 errors, 3 warnings mỗi variant |
| Instrumentation trực tiếp trên `emulator-5554` | PASS: `OK (17 tests)`, 52.376 giây; 17 status hoàn tất thành công |
| Backend thật | Spring Boot + PostgreSQL 14.22 tạm riêng; health 200, Android dùng `10.0.2.2:8080`, không reverse |
| Screenshot | Đã xem login thành công, register thành công và login lỗi; thông báo đúng, password trống sau thành công/che khi lỗi |
| Release/backup policy | Kiểm tra manifest/BuildConfig/backup XML trong phiên bàn giao; cleartext bị chặn, không quyền local-network debug, auth activity không exported |

20 unit test gồm 12 AuthSessionTest + 7 validator + 1 mẫu. Các case session gồm 6 request 401 đồng thời chỉ refresh một lần, refresh bị thu hồi, replay vẫn 401, gộp lỗi 503, login trễ sau clear/đổi phiên, chặn token khác origin/auth path/redirect, Retry-After và lọc raw message, token response sai/storage lỗi, logout với refresh mới nhất dù server lỗi, response hồ sơ trễ sau clear, backend không kết nối không xóa phiên.

17 instrumentation gồm 9 AuthUiTest + 4 AuthBackendTest + 4 bootstrap. Đã gọi API register/login/me/refresh/logout thật, kiểm tra Unicode, duplicate 409, sai password 401, XML lưu trữ không chứa token raw, mở lại store/client đọc được phiên, refresh rotate và token cũ replay 401, refresh bị revoke xóa cả memory/disk, đổi origin và ciphertext hỏng fail-closed. UI có login/register thật, password không khôi phục, loading/duplicate, điều hướng form, dark theme và IME regression.

Test refresh Android thay access bằng chuỗi sai nhưng giữ refresh thật để buộc server trả 401; **không** gọi đây là test đợi JWT hết hạn thật. Test backend expiry ở 2D là bằng chứng lịch sử, không chạy lại 39 test Maven trong phiên này.

## Chạy lại trên Windows

Từ repo root, dùng PowerShell. Firebase JSON local phải có sẵn, không đưa nội dung vào log/Git. Mở AVD test riêng dạng read-only, chờ boot và cấp USB debugging trên chính AVD. Không chạy connected test mặc định khi có điện thoại cá nhân vì có thể tác động app/data trên thiết bị đó.

Terminal backend (giữ mở; Enter để dừng, runner thu hồi database test riêng):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Start-LocalSmokeServer.ps1 -Port 8080
```

Terminal build:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
. ./scripts/Use-TaktakJava.ps1 -Target Android
Push-Location android-client
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' assembleRelease lintRelease --console=plain
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' --no-daemon :app:testDebugUnitTest --rerun-tasks --console=plain
Pop-Location
```

Sau khi kiểm tra `adb devices -l` xác nhận `emulator-5554` là AVD test, không thay serial bằng điện thoại cá nhân:

```powershell
adb -s emulator-5554 shell wm size
adb -s emulator-5554 install -r android-client/app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r android-client/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Phải kiểm tra output `OK (17 tests)` và không có failure; exit code adb một mình chưa đủ. Fixture tạo tài khoản/mật khẩu ngẫu nhiên trong database test tạm, không ghi token ra output.

## Artifact, môi trường và giới hạn

- Unit XML/HTML ở `android-client/app/build/test-results/testDebugUnitTest/` và `build/reports/tests/`; lint ở `app/build/reports/`. Artifact local được ignore: `.local/artifacts/3b/instrumentation.txt`, `screenshots/{login-success,register-success,login-error}.png`, screenshot console và UI dump.
- Theo skill UI, đã hiệu chỉnh màn 1080×2400, chờ animation rồi xem ảnh. ADB unauthorized được xác định bằng ảnh popup, xác nhận một lần qua console trên AVD read-only. Chưa thể dump UI khi ADB chưa được phép; không tap mù. Sau test đã dump UI được. Thử mở LoginActivity trực tiếp qua adb bị từ chối đúng vì không exported; instrumentation dùng ActivityScenario trong app.
- Log crash buffer có lỗi dịch vụ UWB hệ thống của emulator, không phải tiến trình Taktak; không dùng nó để kết luận app crash hoặc tắt kiểm thử. Một lần gọi uiautomator trong lúc instrumentation không tạo dump; không dùng kết quả đó làm bằng chứng UI, đã thực hiện lại sau suite.
- Cảnh báo lint: 2 màu black/white chưa dùng từ trước, 1 PluralsCandidate ở thông báo giây Retry-After tiếng Việt. Java báo EncryptedSharedPreferences deprecated; vẫn giữ theo stack đã khóa, không tự thay thư viện/suppress. Gradle có deprecation warning cho Gradle 10; không nâng version trong 3B.
- Skill auth định hướng tách transport/credential, rotation đồng bộ và fail-closed; skill UI dùng cho kiểm tra ảnh/IME/runtime thực tế, không thay build/test tự động.
- Chưa chạy auth mới trên điện thoại thật, API 26, release runtime/TLS thật, UI mất mạng/429/quyền bị từ chối end-to-end, process-death routing hoặc chờ expiry thật. Không tuyên bố các ca này PASS. Đọc lại store/client không đồng nghĩa đã kiểm tra đóng/mở app route; việc đó thuộc 3C.
- Mất response sau refresh có thể khiến token cũ đã bị server thu hồi; request kế tiếp có thể yêu cầu đăng nhập lại. Logout hook xóa local dù mạng lỗi nhưng không bảo đảm server revoke khi offline; access JWT đã cấp còn hợp lệ tới exp theo hợp đồng backend. Không có logout UI/device unregister trong 3B.
- Kiểm tra cuối: 7 hồ sơ được quét theo mẫu credential/conflict, 66 link local hợp lệ; source data/ViewModel không có logging/mẫu credential đã chọn, không có Kotlin source. `git diff --check` đạt, staged diff rỗng, JSON Firebase giữ nguyên hash; không coi các phép quét chọn lọc là audit toàn lịch sử Git. Backend và AVD của phiên exit 0, các cổng test đóng; database test riêng được runner thu hồi, không xóa dữ liệu người dùng. Điện thoại vẫn kết nối và không bị thay đổi app/data.

Kết luận **3B PASS local**. Tiểu bước tiếp theo duy nhất đề xuất: **3C — routing launcher theo phiên, đích Camera và logout UI/xử lý lỗi**; chờ người dùng duyệt, chưa triển khai camera chức năng hoặc FCM.
