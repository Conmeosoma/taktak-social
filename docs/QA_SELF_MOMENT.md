# Nghiệm thu 3D — chụp → ảnh chỉ mình tôi → tải bằng JWT

Ngày16/10/2026. Người dùng duyệt riêng3D. **PASS trên backend local và điện thoại Android15/API35, 23129RAA4G, 1080×2400**. Chưa chuyển4A, chưa deploy hoặc commit/push. Nguồn API/giới hạn: [MOMENT_API.md](MOMENT_API.md).

## Kết quả thực tế cuối

| Kiểm tra | Kết quả |
|---|---|
| Maven verify/JAR | **46/46 PASS**, 0 failures/errors/skipped,38.120s;39 regression cũ +7 MomentIntegrationTest |
| Android debug/test APK/release build + lint | **BUILD SUCCESSFUL**, lượt cuối18s; lint debug/release0 errors/3 warnings cũ (PluralsCandidate,2 unused colors) |
| JVM test | **20 test**,0 failures/errors; đã chạy lại sau thay production Java, lần build cuối UP-TO-DATE |
| Chụp thật + offline/retry | **1/1 PASS,38.076s**; CameraX STREAMING, listener HTTP test đóng thật → UI lỗi và không hiển thị ảnh local; mở lại listener → bấm Thử lại → upload/GET/display |
| Quyền camera | **1/1 PASS,5.409s**; chuẩn bị quyền riêng CAMERA bị thu hồi, bấm xin quyền và Deny qua accessibility node; UI báo từ chối và nút chụp disabled, không crash |
| Media A/B/JWT và xử lý ảnh | **2/2 PASS,2.480s**; A upload/download được, B404, thiếu/sai JWT401; repository cũ bị CANCELLED sau đổi phiên; crop vuông và rotation90 đúng màu trên ảnh tổng hợp |
| Auth regression máy thật sau CameraX | **6/6 PASS,17.591s**:2 launcher/routing/logout +4 auth/keystore/refresh |
| Chụp thêm đủ sáng | Người dùng xác nhận đã hướng camera vào đồ vật đủ sáng; **1/1 PASS,8.684s**, ảnh máy tính/bàn phím nhìn được; byte hiển thị khớp GET lại từ server |
| Cleanup/cold signed-out | Launcher **COLD,1722ms** về Login, không serverImage; secure preferences không còn entry token (không in giá trị) |
| Storage smoke cuối | **4 moment /4 file /0 recipient**, runner exit0; reverse test đã gỡ, không reset DB cá nhân |

Máy thật có **4 ca media mới duy nhất PASS**, cộng1 lần lặp chụp đủ sáng và6 auth regression =11 test execution thành công trong các lượt cuối. Không cộng các lượt fail trước đó vào PASS. Suite mặc định hiện31 test (25 cũ +2 launcher +4 camera); **chưa chạy toàn bộ31**, chưa chạy lại toàn bộ UI AVD. Không coi build/lint là kiểm thử runtime API26 hoặc release HTTPS.

Review cuối: compile test APK/lint debug thêm **11s PASS** sau khi đưa cấp quyền mạng debug API37 vào Before (trước request fixture) và dùng trim thay strip trong helper test. Production không đổi sau các ca máy thật trênAPI35; nhánhAPI37 mới chỉ build/lint, chưa chạy runtime. Git diff --check và67 link local hợp lệ; quét20 file chọn lọc theo mẫu credential/conflict/whitespace đạt, không phải audit toàn lịch sử. Đặc tả/Firebase JSON không đổi, index rỗng, HEAD8766f26; README/dependency/migration không đổi. Cổng8080 đã đóng.

## Backend7 ca media

1. Multipart JPEG/caption50 emoji/recipientIds=[] →201 chỉ id, sender từ JWT,0 recipient; GET byte khớp file private, no-store, JPEG hợp lệ và loại dữ liệu nối thêm; filename traversal không ảnh hưởng tên file.
2. B/thiếuJWT/JWTgiả không tải được; POST thiếu/saiJWT401, không có route file public qua API.
3. Recipient không rỗng/sai định dạng và caption51 emoji bị400; số file/row không đổi.
4. Fake/truncated JPEG415, hơn5 MiB413, thiếu multipart part400; không file rác/bản ghi nửa chừng.
5. Trigger lỗi INSERT trong DB test riêng →500, rollback xóa file và không thêm row; retry sau gỡ trigger201.
6. Chiều JPEG khai báo65535 bị415 trước cấp phát bitmap; file/row không đổi.
7. Sau đánh dấu deleted_at trong DB fixture, GET kế tiếp404; file fixture bị thiếu cũng404. Không triển khai DELETE API hoặc sửa migration.

## Artifact và các lần chưa đạt

Local ignored: `.local/artifacts/3d/`.

- PASS logs: `camera-roundtrip-lifecycle.txt`, `camera-denial.txt`, `media-security-processing.txt`, `auth-regression.txt`, `camera-roundtrip-bright.txt`.
- Đã xem `screenshots/camera-denied.png`, `camera-preview.png`, `media-offline.png`, `server-roundtrip.png`; lượt này cảnh tối nhưng xác minh byte GET đúng. Sau xác nhận của người dùng, xem thêm `screenshots-bright/server-roundtrip.png` có đồ vật rõ. Ảnh không đưa vào Git.
- `after-logout.xml` xác minh Login/no private media. JVM/lint report ở android-client/app/build; backend JUnit XML ở server/target/surefire-reports.
- Lượt đầu bị hệ thống kill: exit-info reason13/SwipeUpClean, không đủ kết luận crash Java app. Những lượt harness sau thất bại vì assert RESUMED ngay khi dialog đang chuyển hoặc nút Deny không còn xuất hiện khi quyền đã được cấp; không bỏ qua assertion, đã tách ca quyền và ca chụp, chờ lifecycle/Preview STREAMING.
- Gỡ ADB reverse không đóng mọi socket HTTP keep-alive nên có lượt vẫn upload thành công, assertion “phải thấy lỗi mạng” fail. Đổi sang offline/online connector trong **test runner**, không đổi production transport/mạng điện thoại. Có một lượt lỗi action khi không có Activity RESUMED và cleanup logout khi backend offline; giữ log failure, lượt cuối giữ preview sáng/chờ RESUMED và điều phối connector đúng checkpoint đã PASS.
- Lint bắt InputStream.readNBytes đòi API33: đã thay bằng vòng đọc stream có giới hạn tương thích min26, không baseline/suppress/nâng minSdk. Lỗi compile fixture truy cập constructor package-private: chỉ dùng reflection trong androidTest, không nới API production.

## Lệnh chạy Windows

Backend test (DB/storage test riêng, không DB_URL cá nhân):

```powershell
Set-ExecutionPolicy -Scope Process Bypass
. ./scripts/Use-TaktakJava.ps1 -Target Server
Push-Location server
./mvnw.cmd '-Dmaven.repo.local=../.tools/maven-repository' -B -ntp verify
Pop-Location
```

Build Android:

```powershell
. ./scripts/Use-TaktakJava.ps1 -Target Android
Push-Location android-client
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
Pop-Location
```

Chỉ cài/chạy trên thiết bị được cho phép, kiểm tra `adb devices -l`, không phiên cá nhân, backend smoke health200 và reverse đúng port. Dùng install-r giữ dữ liệu. Ca quyền chạy riêng khi CAMERA chưa cấp; nếu đã cấp thì chỉ chuẩn bị lại quyền này trên thiết bị QA được phép, không thay policy bảo mật. Sau kiểm tra đã trả về trạng thái CAMERA được cấp để dùng tính năng. Không chỉnh timeout/theme, không clear/uninstall app data.

```powershell
adb -s <serial> shell am instrument -w -r -e class 'com.taktak.app.CameraMomentAcceptanceTest#deniedCameraPermissionShowsSafeMessage' com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s <serial> shell am instrument -w -r -e class 'com.taktak.app.CameraMomentAcceptanceTest#realPhotoRoundTripWithOfflineRetry' com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Offline case thêm `-e taktakAwaitMediaNetworkRestore true`: đợi marker `MEDIA_READY_FOR_DISCONNECT`, nhập `offline` ở terminal runner; đợi `MEDIA_WAITING_FOR_RESTORE`, nhập `online` trong45s. Giữ Taktak foreground, không thao tác cùng lúc. Không chạy uiautomator song song instrumentation; screenshot trong test dùng UiAutomation. Test bảo toàn phiên cá nhân, marker userId không phải credential; chỉ logout fixture của nó. Khi chỉ chạy class mặc định, phải chuẩn bị quyền cho ca Deny hoặc chọn từng method phù hợp, không bỏ qua điều kiện rồi tuyên bố full suite đạt.

## Giới hạn và điểm dừng

- Chỉ camera sau, JPEG, ảnh chỉ mình tôi. Chưa flash/front camera, caption UI/chọn bạn, lịch sử/DELETE/gallery, FCM/widget hoặc redesign.
- Không đảm bảo exactly-once khi POST đã commit nhưng response bị mất; known id thì retry GET, unknown id có cảnh báo có thể tạo thêm moment. Idempotency hoàn chỉnh ở5D. Không gọi file/row hoàn chỉnh là orphan, không tuyên bố hết mọi tình huống crash/IO cleanup thất bại.
- Chưa API26 runtime, xoay thiết bị qua mọi hướng trên nhiều OEM, chờ JWT hết hạn thật, release HTTPS/VPS thật, load/DoS hoặc camera trước. Test ảnh tổng hợp rotation không thay thế các ca đó.
- Backend smoke đã dừng, tài khoản test không phải tài khoản dev bền vững. App debug vẫn dùng URL USB127.0.0.1:8080: muốn dùng tiếp cần chạy backend/dev config và reverse phù hợp; không hard-code tài khoản hoặc giữ server chạy ngầm.
- **3D PASS; đề xuất duy nhất4A — search username và friend requests server. Chờ duyệt, không tự triển khai.**
