# Taktak — 5A: Điều hướng bốn tab và CameraX

Ngày: 17/10/2026 (Asia/Saigon). **PASS local/USB** trong phạm vi được duyệt riêng **5A**, không triển khai 5B hoặc chức năng History/Profile. Nhánh `chore/push-current-project`, HEAD `5b6b520`. Các kết quả dưới đây đã được xác minh, không tính lượt fail hoặc lặp thành ca PASS mới.

## UNDERSTAND → PLAN

Đã đọc AGENTS, toàn bộ đặc tả/roadmap, quyết định, trạng thái, bàn giao và source/test hiện có. Trước phiên: 3D/4C và bốn màn M3E đã có bằng chứng nghiệm thu; Main chỉ chuyển Camera/Friends, chưa có shell bốn tab. Snapshot SHA256 201 file có sẵn tại `.local/artifacts/5a/baseline.json` bảo vệ cả dirty/untracked của các màn M3E trước.

Tái sử dụng CameraX và API owner-only 3D; chuyển Main sang ViewPager2 + BottomNavigationView. Giữ Java/XML/ViewBinding, Material1.14.0, hệ màu/font M3E đã duyệt và toàn bộ API/JWT/Repository/backend/DB/FCM. UI UX Pro Max dùng cho tính nhất quán/touch target; android_ui_verification dùng đo thiết bị, thao tác và ảnh USB. Python3.12.10 sẵn có, CLI chạy với output UTF-8; không cài hoặc nâng dependency.

Lịch sử/Hồ sơ chỉ có trang thông báo chưa triển khai, không dữ liệu giả, API mới hoặc tính năng ngoài 5A. Chỉ bắt đầu bind Camera sau xác minh phiên và khi tab Camera thực sự RESUMED; kiểm tra release camera khi chuyển tab, nền, recreate và logout.

## IMPLEMENT

- `MainActivity`: shell bốn tab đúng thứ tự Camera/Lịch sử/Bạn bè/Hồ sơ, đồng bộ click/vuốt, Back về Camera, lưu tab qua recreate. Ẩn navigation khi IME mở; áp dụng systemBars/IME/cutout insets. Private pager chỉ được tạo sau session gate, gỡ các page khi kiểm tra lại/phiên không hợp lệ; không phục hồi private Fragment trước xác minh server. Không đổi SessionViewModel hoặc auth API.
- `MainPagerAdapter`, `CameraPageFragment`, `PendingTabFragment` cùng hai layout mới: Friends thật được tái sử dụng; Camera giữ heading/scroll/max480dp; hai trang tương lai chỉ thông báo trung thực. `activity_main.xml`, menu/icons/strings/ids dùng foundation M3E và BottomNavigationView Expressive có sẵn trong Material1.14.0.
- `CameraFragment`: xin quyền runtime như trước; đổi camera trước/sau theo phần cứng; flash Off/Auto/On của ImageCapture (không bật torch). Không có flash hoặc camera thứ hai thì vô hiệu hóa thao tác tương ứng. Chặn nút chụp/đổi/flash khi bận.
- Camera chỉ bind ở page active; mỗi request async có ticket và binding/lifecycle owner riêng để không bind muộn sau chuyển tab. Nhả đúng Preview/ImageCapture thuộc Fragment ở onPause/onDestroyView, không unbindAll ảnh hưởng consumer khác. OrientationEventListener cập nhật targetRotation; ImageProxy luôn được close; callback dùng đúng MomentViewModel của lượt chụp.
- `CameraSettingsViewModel` chỉ giữ lens/flash trong RAM qua recreate, không ảnh/token/disk; `CameraRotation` tách mapping bốn góc để kiểm thử. `MomentViewModel.captureFailed` không publish sau cleared. PhotoProcessor/API upload/download, JWT, storage và Repository không đổi.
- Thêm `MainCameraAcceptanceTest`, hai JVM test và `MainNavigationTestSupport`. Sửa đường dẫn điều hướng/tìm Fragment trong FriendsAcceptanceTest/FriendsM3Test/CameraMomentAcceptanceTest và đo shell mới trong CameraM3Test; giữ nguyên assertion nghiệp vụ/JWT và không thêm production test hook.

Lifecycle và rotation đối chiếu tài liệu chính thức: [CameraX architecture](https://developer.android.com/media/camera/camerax/architecture), [orientation/rotation](https://developer.android.com/media/camera/camerax/orientation-rotation), [FragmentStateAdapter](https://developer.android.com/reference/androidx/viewpager2/adapter/FragmentStateAdapter). Đây là chi tiết triển khai stack đã khóa, không đổi quyết định kỹ thuật.

## VERIFY

Thiết bị thật Xiaomi23129RAA4G, Android15/API35, USB; màn hình1080×2400. Người dùng xác nhận hướng cả hai camera vào khu vực an toàn và đóng bong bóng Zalo. Dùng backend smoke PostgreSQL/JWT/private media riêng trên loopback8080 qua adb reverse, không DB dev/tài khoản cá nhân. Fixture kiểm tra quyền sở hữu phiên QA và tự logout/revoke; không ghi mật khẩu/token vào báo cáo.

- Build cuối: `gradle-final.txt`, **BUILD SUCCESSFUL19s**; assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, lintDebug, assembleRelease, lintRelease đều thành công. Lượt build đầy đủ đầu33s và lượt sau38s đã thực chạy **33/33 JVM**,0 failure/error/skipped; lượt cuối tái sử dụng task không đổi. Gồm5 ca mới về mapping360góc/boundaries/unknown và lens/flash cycle. Lint debug/release **0 errors,3 warnings cũ**, không suppress/baseline mới.
- `acceptance-final.txt`: **6/6 PASS47.243s**. Bốn tab/click/vuốt/Back/recreate/logout; camera không còn bound khi rời tab/nền; camera trước/sau streaming; flash cycle và rear ImageCapture ở ON; hai ảnh chụp thật → API owner-only → GET JWT và so bytes/ảnh vuông; light/dark,font1.5/ba viewport context; Activity đổi landscape/portrait giữ lens và preview vuông; JPEG bốn góc màu kiểm chứng rotation0/90/180/270.
- `friends-regression.txt`: **7/7 PASS51.651s**, năm ca Friends core và hai layout Friends/Camera.
- `camera-deny.txt` và `camera-allow.txt`: **1/1 PASS4.923s** và **1/1 PASS6.964s**, system dialog Deny/Allow thật; cuối CAMERA được granted như trước QA.
- `final-regression-retry.txt`: **15/15 PASS88.075s**:2LauncherAuth,3LoginM3,3RegisterM3,3FriendsM3,3media UI/privateAPI/crop và1CameraM3 shell/layout. Đăng nhập/API/logout/IME/recreate cùng hoạt động dưới kiến trúc navigation mới.
- `media-offline.txt`: **1/1 PASS43.807s**, chụp thật khi connector QA dừng → lỗi/không ảnh server giả → mở lại → Retry → GET JWT/so bytes/ảnh vuông.
- `session-retry.txt`: **2/2 PASS52.625s**, refresh revoked về Login; CHECKING/network-permission renderer/offline giữ private content ẩn, giữ phiên khi mạng lỗi, khôi phục rồi Retry/logout. Network-permission renderer mô phỏng trênAPI35, không nhận là dialogAPI37.

Tổng **33 ca USB duy nhất PASS**:6+7+2+15+1+2. Baseline trước sửa1/1, routing sớm4/4,MainCamera trước thêm orientation5/5 và các lượt lặp không cộng lại. Không tuyên bố đã chạy toàn bộ suite Android hoặc các ca backend trong phiên5A.

Lệnh build (PowerShell tại android-client; đã chọn JDK bằng scripts/Use-TaktakJava.ps1):

```powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

Lệnh USB dùng Android SDK platform-tools/adb.exe, serial máy QA và runner `com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner`; mỗi lần cài phải kiểm tra Success trước chạy test. Toàn bộ log/ảnh/build snapshot dưới `.local/artifacts/5a/` ignored, không commit ảnh riêng tư.

### Các lượt lỗi và giới hạn bằng chứng

MIUI từng từ chối APK test (`INSTALL_FAILED_USER_RESTRICTED`), lệnh chạy sau đó thấy ClassNotFound vì APK test cũ; retry cài Success rồi 5/5 đạt, sau thêm ca orientation thì cài lại hai APK Success và6/6 đạt. Một lệnh hồi quy gõ sai LauncherAuthTest thay vì LauncherAuthAcceptanceTest gây initializationError; không coi lượt đó PASS. Không bỏ assertion hoặc sửa production để né lỗi harness.

Lượt `session-final.txt` fail vì host theo dõi ảnh qua ADB chậm và khôi phục connector quá muộn: assertion Host reconnects QA backend rồi cleanup logout timeout. Connector được mở lại, dừng riêng tiến trình QA (không clear data), chạy lại hai ca với marker mới và khôi phục sau30s; `session-retry.txt`2/2 đạt. Không nới timeout hoặc bỏ kiểm tra offline. Lệnh chẩn đoán pidof sau test media gặp null vì process đã kết thúc; không phải lỗi test/media (log1/1 đạt), các lệnh sau kiểm tra chuỗi rỗng.

Ảnh đầu có Zalo che UI, không dùng làm ảnh cuối. Sau xác nhận người dùng đã đóng, chụp lại `final-screens/`11ảnh và xem trực tiếp Camera/History/landscape; không còn bong bóng. Ảnh camera trước giữ local ignored, không đưa vào README hoặc Git.

Landscape/portrait là đổi requestedOrientation của riêng Activity trên máy thật và khôi phục sau test, không đổi system auto-rotation; điện thoại không được người dùng xoay vật lý trong lượt đó nên không nhận là nghiệm thu hướng cảnh thực ở landscape. Mapping sensor và JPEG rotation được kiểm thử riêng. Viewport rộng/font1.5 là context đo layout, không giả nhận tablet thật. Chưa API26/API37 local-network dialog, TalkBack toàn luồng, font2×, tablet, process-death full suite, HTTPS runtime/signing hoặc FCM. Không chạy lại87backend tests vì source backend không đổi.

## REVIEW → HANDOFF

- So snapshot: **13file có sẵn sửa,17file mới,0file xóa** trong phiên5A, gồm25Android +5docs. Các deleted resource của M3E có từ trước, không phải xóa mới. **84/84 server/data/build/manifest** giữ SHA256; schema/Firebase JSON/README/spec giữ nguyên. Các thay đổi người dùng trước phiên không bị ghi đè.
- Quét các file5A: không phát hiện credential-pattern/privatekey/token literal, conflict marker hoặc trailing whitespace;9XML parse được, link local kiểm tra được và git diff --check đạt. Đây là kiểm tra chọn lọc, không audit tuyệt đối toàn lịch sử Git. Index rỗng,HEAD không đổi;artifacts/Firebase JSON vẫn ignored.
- Xem ảnh cuối không lớp phủ: `final-screens/camera-tab.png`, `history-tab.png`, `rear-photo.png`, `landscape.png`; session offline và media offline/success ở `session-screens/`,`media-screens/`. Ảnh đầu/cuối phục vụ QA local; không đưa ảnh mặt camera trước lên Git.
- Smoke backend riêng dừng **exit0**, **7media rows/7private files/0recipients**,cổng8080 không listener. Không xóa test data/private files/devDB/app data. QA fixtures đã logout/revoke,reverse8080 gỡ,CAMERA granted như ban đầu. Cold start1981ms,`handoff.xml` xác nhận Login/emailInput,không mainPager/serverImage;ảnh handoff đã xem.
- Không có bước thủ công bắt buộc để nghiệm thu5A. Dùng app dev tiếp cần backend/devDB và kết nối dev vì smoke tạm đã dừng, không dùng tài khoản QA làm tài khoản bền vững.

**5A PASS; đề xuất duy nhất5B và chờ duyệt.** 5B (cache/Preview/caption/chọn bạn/mirror khi cần),5C recipient-aware media và5E History thật chưa triển khai. Không commit/push/deploy, xóa DB/ảnh hay tự chuyển bước.

### Danh sách file trong phạm vi5A

Sửa9Android có sẵn: MainActivity.java,ui/camera/CameraFragment.java,viewmodel/MomentViewModel.java,res/layout/activity_main.xml,res/layout/fragment_camera.xml;androidTest/CameraM3Test.java,CameraMomentAcceptanceTest.java,FriendsAcceptanceTest.java,FriendsM3Test.java.

Thêm16Android: ui/main/MainPagerAdapter.java,PendingTabFragment.java;ui/camera/CameraPageFragment.java,CameraRotation.java;viewmodel/CameraSettingsViewModel.java;res/layout/fragment_camera_page.xml,fragment_pending_tab.xml;res/menu/main_tabs.xml;res/values/main_ids.xml,main_strings.xml;res/drawable/ic_nav_history.xml,ic_nav_profile.xml;androidTest/MainNavigationTestSupport.java,MainCameraAcceptanceTest.java;test/ui/camera/CameraRotationTest.java,test/viewmodel/CameraSettingsViewModelTest.java. Java paths thuộc packagecom.taktak.app và source set tương ứng.

Tài liệu: tạo QA_MAIN_CAMERA.md,cập nhật PROJECT_STATUS.md,TASKS.md,SESSION_HANDOFF.md,PROJECT_DECISIONS.md. Không sửa AGENTS/README/đặc tả hoặc script.
