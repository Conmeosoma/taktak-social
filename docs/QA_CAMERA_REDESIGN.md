# Taktak — Nghiệm thu redesign Camera

Phiên 16–17/10/2026, Asia/Saigon. Tiểu việc riêng sau 3D, không mở 4A hoặc redesign Auth.

## Phạm vi đã triển khai

- Java/XML, ViewBinding, Material Components hiện có; không đổi dependency/font/SDK hoặc thêm Kotlin/Compose.
- `Theme.TakTak.Camera` chỉ áp dụng MainActivity trước inflate. Login/Register giữ nguyên theme/source. API, Repository, ViewModel, JPEG, JWT, phiên và server giữ nguyên trong phiên này.
- Nền ấm/coral, preview vuông bo24dp, thẻ trạng thái bo16dp, shutter88dp có icon vector/contentDescription, logout48dp. Không flash, đổi camera, tab hoặc chức năng mới.
- Semantic colors sáng/tối; shutter disabled có tint riêng. Thông báo sẵn sàng/đang xử lý/thành công/lỗi không chỉ dựa vào màu. Thử lại nằm sát lỗi; giữ cảnh báo gửi lại có thể tạo thêm ảnh nếu POST mất phản hồi.
- Xin quyền có placeholder, chưa cấp quyền không chụp được. Từ chối rồi cấp lại thành công trả thông báo về sẵn sàng. ScrollView/system-bar insets cho phép cuộn nội dung dài.

## Skill và Python

Đọc đầy đủ ui-ux-pro-max và android_ui_verification. Python ban đầu chỉ có Windows Store alias. Theo yêu cầu, cài **Python3.12.10 per-user** bằng winget; installer được winget xác minh hash, executable thật trả về `Python 3.12.10`. Không cài pip package. PATH của terminal cũ chưa được giả định cập nhật; dùng executable trong `%LOCALAPPDATA%/Programs/Python/Python312/python.exe`.

CLI skill đã chạy thật bằng `-B -X utf8`, script `ui-ux-pro-max/scripts/search.py`:

```text
search.py "private photo sharing camera mobile modern soft minimal playful social premium accessible" --design-system -p Taktak -f markdown
search.py "minimalism soft accessible mobile photography social" --domain style -n 3
search.py "social media app" --domain color -n 2
search.py "touch target loading error feedback dark mode accessibility" --domain ux -n 5
```

Generator trả Claymorphism/đen-vàng, **không áp dụng** vì trái yêu cầu tối giản, không shadow lớn/đồ chơi. Tổng hợp Soft UI Evolution/Inclusive Design, ưu tiên palette coral người dùng. Skill không có Android Java/XML: không dùng default Tailwind hoặc chuyển stack. Output local: `.local/artifacts/ui-camera/skill-design-system.md`.

Skill Android giúp hiệu chuẩn thiết bị, chờ CameraX STREAMING/lifecycle, tìm nút quyền bằng accessibility node, chụp trước/sau; không dùng uiautomator tranh UiAutomation khi instrumentation chạy.

## Build và accessibility

```powershell
. ./scripts/Use-TaktakJava.ps1 -Target Android
cd android-client
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

Build production cuối: **BUILD SUCCESSFUL20giây**. 20 unit test, 0 failures/errors. Lint debug/release: 0 errors, 3 warnings cũ (PluralsCandidate, unused black/white); đã sửa Overdraw phát sinh ở bản đầu. Release chỉ build, chưa ký/deploy/chạy API HTTPS production.

Relative luminance sRGB tính từ XML palette thực tế, 12 cặp chính đều >=4.5:1:

| Chữ/icon và nền | Light | Dark |
|---|---|---|
| Shutter / coral | 5.38 | 7.17 |
| Chữ chính / background | 14.51 | 17.05 |
| Chữ phụ / background | 6.05 | 10.23 |
| Chữ chính / soft | 13.91 | 13.76 |
| Success / success container | 7.83 | 7.74 |
| Error / error container | 6.55 | 7.95 |

Không nhận đây là audit TalkBack toàn diện. Đối chiếu [Android Views accessibility](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views) và [system-bar insets](https://developer.android.com/develop/ui/views/layout/edge-to-edge).

## Máy thật và bằng chứng

USB23129RAA4G, Android15/API35, 1080×2400, density440, font_scale1.0. Không thay font scale/night mode hệ thống: light/dark dùng AppCompat trong tiến trình test rồi khôi phục; layout chữ150% dùng ConfigurationContext riêng.

- Baseline trước sửa: 2 lượt chụp/upload/GET thật, mỗi lượt1/1 PASS; lượt hai9.866giây. Baseline vẫn có PiP ở header, không nhận là ảnh hoàn toàn sạch. Ảnh sau không PiP, đã xem preview/shutter/status trực tiếp.
- CameraLayoutTest: inflate/measure fragment với nội dung280dp (320dp trừ padding40dp), chữ150%, cả hai theme; không tràn ngang/ellipsis/cắt chiều cao chữ, preview/shutter vuông. Đây là layout test, không phải runtime toàn app trên máy320dp hoặc tablet.
- CameraRedesign test: preview thật sáng/tối, shutter>=48dp, nhãn, live region. Busy/progress/disabled dùng fixture renderer-only, không tính thành upload thật.
- Round-trip thật: upload chỉ mình tôi, GET bằng JWT, byte hiển thị khớp GET lại, ảnh vuông. API thiếu/sai JWT401, người khác404, operation cũ không vượt phiên.
- Regression launcher/AuthBackend: login, phiên mã hóa/restore, retry/logout UI, refresh bị thu hồi, origin isolation và ciphertext lỗi.
- Artifact/log: `.local/artifacts/ui-camera/`, ignored; ảnh trước trong before/before-clear, **bộ ảnh cuối trong after-verified**. Các lần adb pull vào thư mục đã tồn tại có thể tạo thêm thư mục 3d bên trong after-final; không dùng ảnh cũ ở cấp ngoài để nhận bản cuối. Không đưa ảnh chụp PC hoặc dữ liệu riêng tư vào Git.

## Lỗi đã điều tra, không bỏ qua

1. Hai lượt quyền đầu không thấy Deny/không tới Camera; ảnh chẩn đoán thấy trình duyệt. Người dùng xác nhận để máy cho QA; chuẩn bị quyền CAMERA riêng Taktak, không reset dữ liệu.
2. Helper Allow mới click một lần nhưng await gọi predicate lần nữa sau khi dialog đóng, báo FAIL dù đã cấp quyền. Sửa androidTest bằng AtomicBoolean ghi nhận action; không nới assertion hoặc sửa nghiệp vụ.
3. Offline đầu quá hạn host restore45giây vì adb logcat/get-state phản hồi chậm khi connector ngắt. Chạy lại với lịch offline26giây rồi online tự động, giữ timeout/điều kiện nghiệm thu. Chỉ ngắt HTTP connector smoke server, không ngắt mạng điện thoại/reset DB.
4. Qua screenshot, đưa Thử lại sát lỗi và thêm tint disabled; sửa thông báo khi từ chối rồi cấp lại. Chỉ thay presentation.

## Kết quả cuối và bàn giao

**PASS trong phạm vi Camera redesign local/USB.** Kết quả trên APK cuối:

| Lượt chạy | Kết quả | Thời gian |
|---|---|---|
| Từ chối Camera | 1/1 PASS | 5.913 giây |
| Từ chối rồi cấp lại qua dialog, thông báo sẵn sàng | 1/1 PASS | 10.249 giây |
| Layout/chữ lớn, sáng/tối/busy, ảnh thật, API/JPEG, auth/logout | 11/11 PASS | 45.375 giây |
| Ngắt connector, lỗi mạng, khôi phục và retry ảnh thật | 1/1 PASS | 48.397 giây |

Tổng **14 successful executions / 13 ca duy nhất**, do round-trip chạy cả online và offline. Log: permission-denied-final.txt, permission-granted-final.txt, final-suite.txt, offline-retry-final.txt. Không nhận toàn bộ suite instrumentation mặc định hoặc 46 test backend lịch sử là vừa chạy lại.

Đã xem bộ ảnh cuối: light/dark, disabled busy, lỗi và nút Thử lại, ảnh tải từ server; cold start sau logout **COLD1881ms** về Login không mang theme Camera. Quyền CAMERA cuối granted như trước; không đổi timeout/theme/font scale hệ thống. Fixture tự logout và bỏ marker trong @After; không xóa dữ liệu cá nhân hoặc gallery.

Backend QA dừng **exit0**, cuối **10 moment / 10 file / 0 recipient**, cổng8080 đóng. Gỡ đúng reverse tcp8080 do phiên tạo. Ảnh fixture/backend target vẫn được giữ ignored, không xóa hoặc reset DB cá nhân. APK debug cập nhật vẫn trên máy; muốn dùng API debug lại cần backend/dev config và USB reverse, không dùng tài khoản QA tạm làm tài khoản bàn giao.

### File thay đổi trong phiên này

- Sửa: MainActivity.java (chỉ setTheme), ui/camera/CameraFragment.java (presentation), res/layout/activity_main.xml, res/layout/fragment_camera.xml; androidTest/CameraMomentAcceptanceTest.java (thêm ca UI/quyền).
- Tạo: androidTest/CameraLayoutTest.java; res/values/camera_colors.xml, camera_styles.xml, camera_strings.xml; res/values-night/camera_colors.xml; res/color/camera_shutter_tint.xml, camera_shutter_icon.xml; res/drawable/ic_camera_shutter.xml.
- Hồ sơ: tạo QA_CAMERA_REDESIGN.md; cập nhật PROJECT_STATUS.md, TASKS.md, SESSION_HANDOFF.md, UI_REDESIGN_REVIEW.md. Các đường dẫn Java/resource đều dưới android-client/app/src với source set tương ứng.
- Python cài per-user ngoài repository theo quyền người dùng. Không sửa AGENTS, đặc tả, quyết định kỹ thuật, README, dependency, migration, Firebase config hoặc server trong phiên này. Các file dirty/untracked đã có từ 3B/3C/3D được bảo toàn, không nhận toàn bộ git status là thay đổi của phiên UI.

Review cuối: 102 file Android/server có snapshot trước sửa, 97 giữ nguyên; chỉ 5 file có sẵn được sửa trong phiên UI và thêm 8 file test/resource. Cả40 file server trong snapshot, API/Repository/ViewModel/session/Auth/PhotoProcessor giữ nguyên. Parse22 resource XML đạt; kiểm tra18 file phiên này: 0 mẫu private key/API key/JWT thực, 0 conflict/trailing whitespace,56 link local hợp lệ. Đây là kiểm tra chọn lọc, không phải audit toàn lịch sử. git diff --check đạt; index rỗng, HEAD vẫn8766f26; đặc tả/Firebase JSON khớp SHA-256 đầu phiên, config/ảnh được ignore.

Chưa chạy TalkBack thủ công toàn luồng, API26 runtime, mọi hướng xoay, tablet hoặc release HTTPS runtime. Font150%/320dp chỉ kiểm tra layout fragment, không suy rộng toàn app. Không có blocker cho phạm vi đã duyệt. **Dừng; 4A chưa được duyệt, không commit/push/deploy.**
