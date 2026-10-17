# Taktak — M3E-03: Camera + trạng thái phiên

Ngày: 17/10/2026, Asia/Saigon. **PASS local/USB** trong phạm vi màn 3 được người dùng duyệt. Nhánh `chore/push-current-project`, HEAD `5b6b520`; 25 mục dirty/untracked từ Foundation/Login/Register có sẵn được bảo toàn. Không commit/push/deploy hoặc chuyển 5A.

## UNDERSTAND → PLAN

Đọc AGENTS, toàn bộ TAKTAK_PROJECT, quyết định, trạng thái, TASKS, SESSION_HANDOFF và source/test liên quan trước sửa. Giữ Material Components 1.14.0 đã khóa, Java/XML/ViewBinding, CameraX, API upload/download, JWT, ViewModel/Repository và FCM. Không đổi backend/DB/migration/SDK/dependency. Không thêm flash, đổi camera, caption, tab hay chức năng khác.

Tiếp tục UI UX Pro Max và android_ui_verification đã đọc: ưu tiên ảnh/thao tác chụp, semantic contrast, target chạm, light/dark và viewport nhỏ/chữ lớn; đối chiếu hierarchy, chụp/xem ảnh thật qua USB. Python 3.12.10 đã có và CLI skill đã chạy, không cài thêm. Không chuyển gợi ý web/CSS sang stack khác.

## IMPLEMENT

- `activity_main.xml`: header gọn Taktak/logout, Camera cạnh lối mở Bạn bè hiện có; content tối đa 480dp, cuộn khi thiếu chiều cao. Card phiên có progress/heading/message/retry; ẩn nội dung riêng tư khi chưa xác minh phiên.
- `fragment_camera.xml`: preview/ảnh server vuông 1:1 bo 28dp; nút chụp Expressive 88dp Coral ngay dưới ảnh, nhãn và status card sau nút chụp. Giữ toàn bộ binding ID. Lavender cho sẵn sàng/đang xử lý, semantic error/success cho lỗi/thành công; không dùng màu làm tín hiệu duy nhất.
- Mở rộng foundation bằng theme Camera, hai selector enabled/disabled, token shutter/success sáng/tối và ba chuỗi trạng thái phiên. Giữ progress slot cố định, live-region và content description.
- MainActivity chỉ thêm presentation card/heading và safe insets systemBars/IME/displayCutout. CameraFragment chỉ đổi resource màu của renderer; không đổi capture, retry, API, session gate hoặc logout.
- Theme Expressive **chỉ đặt trên cây View Camera/phiên**, không đổi theme Activity toàn cục: Friends vẫn dùng theme cũ cho tới khi được duyệt màn tiếp theo. Root background đổi đúng theo Camera/Friends. Test đo riêng xác nhận hai context không lẫn palette.
- Gỡ hai selector shutter cũ, một button style, hai chuỗi header trùng và năm màu Camera không còn tham chiếu sau khi tìm kiếm. Các file cũ khôi phục được từ Git; không xóa dữ liệu hoặc thay đổi người dùng chưa kiểm tra. Những màu/theme cũ còn dùng bởi Friends được giữ.
- Thêm `CameraM3Test` gồm ba ca layout và phiên; không thêm test hook vào production hoặc sửa test cũ để bỏ assertion.

## VERIFY

Thiết bị USB `8e90fdff`, Xiaomi 23129RAA4G, Android 15/API 35, 1080×2400. Người dùng xác nhận hướng camera vào đồ vật đủ sáng, không có thông tin riêng tư. APK cập nhật `install -r`, không clear data. Fixture có ownership guard, tài khoản/password ngẫu nhiên trong RAM và EncryptedSharedPreferences; không thay phiên cá nhân.

Backend QA riêng do `Start-LocalSmokeServer.ps1 -Port 8080` tạo với PostgreSQL/JWT/storage tạm; không dùng DB dev. Debug loopback qua adb reverse. Host điều khiển riêng HTTP connector offline/online qua stdin, không đổi Wi-Fi/firewall hoặc server dùng chung.

| Kiểm tra thực chạy | Kết quả | Log ignored dưới `.local/artifacts/m3e-camera/` |
|---|---|---|
| Baseline UI APK trước redesign, sáng/tối/busy | 1/1 PASS, 14.684s; chỉ dùng làm mốc so sánh | `baseline-ui.txt`, `before/redesign-*.png` |
| Build debug/test/release + unit + lint đầu | BUILD SUCCESSFUL 31s; unit thực chạy | `gradle.txt` |
| Build test APK sau sửa harness đo layout | BUILD SUCCESSFUL 15s | `test-build.txt` |
| Build debug/test/release + unit/lint cuối | BUILD SUCCESSFUL 20s; unit up-to-date từ lượt thực chạy, không tính là chạy mới | `gradle-final.txt` |
| Unit XML | 28/28, 0 failures/errors/skipped | `app/build/test-results/testDebugUnitTest` |
| Lint debug và release | 0 errors, 3 warnings cũ: PluralsCandidate, black/white unused | `app/build/reports/lint-results-*.txt` |
| CameraM3Test với offline flag bật | 3/3 PASS, 46.038s, không skipped | `session-initial.txt` |
| Camera Deny thật | 1/1 PASS, 4.699s | `camera-deny.txt` |
| Camera Allow thật | 1/1 PASS, 6.473s | `camera-allow.txt` |
| Chụp thật → offline → retry → upload/download JWT | 1/1 PASS, 45.119s | `media-offline-test.txt` |
| Camera sáng/tối/busy + private API + rotation/crop + CameraLayout | 4/4 PASS, 15.935s | `camera-regression.txt` |
| LauncherAuth 2 + LoginM3 3 + RegisterM3 3 + FriendsLayout 1 | 9/9 PASS, 48.007s | `auth-friends-regression.txt` |
| Friends navigation/recreate/back/logout thực tế | 1/1 PASS, 9.520s | `navigation-regression.txt` |

Tổng **20 ca instrumentation trên APK redesign đạt**, không cộng baseline thành ca nghiệm thu mới. Không skipped/failure trong các lượt cuối nêu trên. Các suite offline của Login/Register không chạy lại; chọn rõ ba method không phụ thuộc host, không nhận skipped là PASS. Không chạy lại 87 backend tests vì source backend giữ nguyên.

Chi tiết ba ca mới:

1. Inflate/measure light/dark, font 1.5×, viewport 280×560/360×800/840×420dp; error message dài, retry; không tràn ngang hoặc cắt text, content ≤480dp, ảnh vuông/nút chụp vuông ≥48dp/retry ≥56dp. Đo bốn cặp màu shutter/error/success/Lavender đạt contrast ≥4.5. Friends context vẫn palette cũ, Camera dùng palette M3E. Đây là context mô phỏng, không phải xoay máy/tablet thật. Lượt harness đầu phát hiện FragmentContainerView không nhận View không gắn Fragment; sửa phép đo detached dùng FrameLayout cùng context/layout params, không đổi production hoặc nới điều kiện.
2. Checking/private-content-hidden; gate quyền mạng/retry; tắt HTTP connector thật → session network error nhưng không xóa phiên/không hiện Camera → khôi phục và bấm retry mở Camera → UI logout về Login/store rỗng. Checking dùng renderer-only MutableLiveData trong test, không nhận là slow-network end-to-end. Gate `check(false)` mô phỏng quyền mạng trên API35, không nhận là dialog quyền local-network API37.
3. Revoke refresh thật bằng API204, lưu access JWT giả trong **phiên QA riêng**, nạp lại repository rồi mở launcher → refresh thất bại/clear session/Login, không lộ ảnh. Test reflection chỉ nằm ở androidTest.

Camera permission được kiểm tra qua dialog Deny/Allow thật bằng accessibility node. Trước mỗi lượt chỉ reset flags quyền CAMERA của Taktak để tránh Android double-denial chặn dialog; không đổi cài đặt bảo mật USB/hệ thống. Cuối quyền CAMERA được xác minh granted như ban đầu.

Media test chờ Preview STREAMING/capture ready, chụp JPEG thực; offline báo lỗi, nút retry tải lại thành công. Bytes hiển thị khớp GET server, decode vuông; API owner200/other404/noJWT401/invalidJWT401 và stale-generation cancellation được chạy lại. Rotation90/crop/nén được kiểm tra bằng bitmap tổng hợp cũ, không suy ra mọi tư thế máy đã được kiểm thử. Busy light/dark dùng renderer state trong test, không giả nhận request đang gửi thật; media test riêng đã xác minh I/O thật.

Ảnh baseline `before/redesign-light.png`, `before/redesign-dark.png` và busy; ảnh cuối trong `after/`: `redesign-light.png`, `redesign-dark.png`, hai busy, `camera-denied.png`, `camera-permission-granted.png`, `camera-preview.png`, `media-offline.png`, `server-roundtrip.png`. Đã xem trực tiếp light, dark-busy, offline và server-roundtrip; ảnh đủ sáng/không overlay, shutter và retry rõ ràng. `session-screens/` có checking/permission/offline/recovered/logout/expired-session; đã xem offline/recovered. Những file lịch sử khác được pull cùng thư mục (ví dụ permission-diagnostic) không tính bằng chứng mới. Ảnh không chỉnh sửa, không đưa lên Git; preview/ảnh có tư thế ngang theo cách đặt điện thoại, không tuyên bố đã kiểm thử xoay máy thật.

Lệnh build ở `android-client/`, JDK chọn bằng `Use-TaktakJava.ps1 -Target Android`:

```powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

Release dùng HTTPS placeholder, không signing/deploy hoặc runtime HTTPS. Native Gradle exit0 được đối chiếu BUILD SUCCESSFUL; không nhận stderr javac Note là lỗi build. Test phiên cần `-e taktakSessionOffline true -e taktakQaRun <unique-run>` và marker `M3_SESSION_READY_OFFLINE_<unique-run>`. Test media cần `-e taktakAwaitMediaNetworkRestore true`, marker MEDIA_READY_FOR_DISCONNECT/MEDIA_WAITING_FOR_RESTORE; lọc log theo PID hiện tại để không đọc marker lịch sử. Không chạy uiautomator dump khi instrumentation đang giữ UiAutomation.

## REVIEW → HANDOFF

Trong phạm vi màn3: **17 file Android** (11 file có sẵn sửa, 4 mới, 2 selector cũ gỡ) và **5 tài liệu** (QA này mới, 4 hồ sơ cập nhật). Android gồm MainActivity, CameraFragment, activity_main/fragment_camera, camera styles/strings/colors sáng-tối, m3 styles/colors sáng-tối; thêm CameraM3Test, m3_camera_strings và hai selector m3_shutter. SHA256 baseline195 file tracked/untracked dùng để tách thay đổi màn3 khỏi 25 mục dirty có sẵn, không so toàn bộ worktree với HEAD rồi nhận hết là thay đổi mới.

Không đổi ViewModel/Repository/ApiClient/EncryptedSessionStore/PhotoProcessor/backend/migration/Firebase/Manifest/Gradle hoặc code Login/Register so với đầu phiên. Sửa foundation chỉ mở rộng token/theme và comment theo phạm vi. Credential scan là kiểm tra mẫu có phạm vi, không audit tuyệt đối toàn bộ lịch sử Git.

Review cuối: snapshot195file xác nhận **15file có sẵn sửa,5file mới,2selector gỡ**, đúng22file của màn3; **56file server giữ SHA256**. Quét20file hiện có trong phạm vi không phát hiện mẫu API key/JWT/private key, conflict marker hoặc whitespace cuối dòng;12XML parse hợp lệ,81link local hợp lệ,git diff --check đạt,index rỗng. Artifact và google-services.json vẫn ignored. Các cảnh báo LF→CRLF của Git là cấu hình line ending, không phải diff-check failure.

Cleanup thực tế: backend dừng **exit0**, `2 media rows / 2 private files / 0 recipients` (ảnh thật và fixture API), cổng8080 không còn listener. Không xóa test data/ảnh/cluster hoặc DB dev. QA sessions logout/refresh revoke; adb reverse8080 gỡ, CAMERA granted, cold launcher **1334ms** về Login. Hierarchy xác nhận emailInput và không serverImage, ảnh `handoff.png` lưu ignored. Điện thoại để Login, không để phiên QA hoặc dev server chạy nền.

Giới hạn: chưa runtime API26/API37 local-network dialog, TalkBack toàn luồng, font2×, tablet/landscape thật, HTTPS runtime/signing hoặc FCM; không suy rộng PASS. Không có bước thủ công bắt buộc để hoàn tất màn3. Muốn dùng app dev cần backend/dev DB và kết nối phù hợp vì smoke đã dừng.

**M3E-03 PASS local/USB. Đề xuất đúng một màn tiếp theo: M3E-04 — Bạn bè/Friends. Chờ người dùng duyệt; không triển khai tiếp hoặc chuyển 5A.**
