# Taktak — 5C: Gửi ảnh cho bạn bè

Ngày 17/10/2026 (Asia/Saigon). **PASS local/USB** trong phạm vi 5C đã được duyệt. Dừng trước 5D/5E/FCM/widget.

## UNDERSTAND → PLAN

Đã đọc AGENTS, toàn bộ đặc tả/roadmap, quyết định, trạng thái, tasks, handoff và source/tests. Trước phiên: HEAD `ed9fde6`, nhánh `chore/push-current-project`, 28 mục dirty/untracked của 5B được bảo toàn. 5B đã có Camera/cache/Preview/caption/chọn bạn, nhưng server chỉ cho owner-only. 5C nối người nhận thật, không xây lại auth/friends/Camera.

Giữ Java/XML/ViewBinding/MVVM, Material 1.14.0, CameraX, JWT/session, private storage và schema V1. Không nâng dependency, sửa migration, thêm lịch sử hay FCM. Dùng skill android_ui_verification để kiểm tra USB, kích thước và ảnh/hành vi thực tế. Người dùng xác nhận điện thoại/camera an toàn cho QA. Không chạy uiautomator dump trong lúc instrumentation hoạt động.

## IMPLEMENT

- RecipientIds là các field UUID lặp lại, tối đa 100 người; từ chối ID sai/trùng/bản thân, JSON/CSV hoặc trộn `[]`. Bỏ field hoặc một literal `[]` giữ owner-only.
- Khóa tất cả sender/recipients theo thứ tự UUID PostgreSQL, đồng nhất với FriendService. Sau chờ khóa, kiểm tra toàn bộ friendship và block hai chiều. Một ID không đạt thì từ chối cả yêu cầu, không lọc hoặc đổi thành owner-only.
- Moment và recipients cùng transaction/timestamp; rollback dọn cả metadata và private file. GET kiểm tra user, sender/recipient của đúng moment và deleted_at mỗi lần. Một người bạn không được chọn vẫn nhận 404; thiếu/giả JWT nhận 401.
- Giới hạn 5 MiB/file, 6 MiB/request, 4096/cạnh, 16 MP. Kiểm tra MIME image/jpeg cùng signature/decode thực tế; re-encode bỏ EXIF/GPS/trailing data. Tên UUID server sinh, private storage/no-store/nosniff. Tomcat giới hạn hữu hạn 104 parts, đủ 100 ID cùng image/caption.
- Android gửi snapshot caption/selected hoặc đọc đủ các trang khi chọn Tất cả. Rỗng/quá 100/lỗi phân trang không được gửi một phần. Payload bị khóa khi kết quả POST chưa rõ; 400 xác định từ chối giữ draft và cho sửa. Known momentId chỉ GET. Lưu riêng tư vẫn là thao tác riêng có xác nhận.
- Thành công phân biệt gửi bạn bè với chỉ mình tôi, chỉ hiện ảnh tải lại từ server. Resume đang gửi không enqueue đọc lại draft đã dọn.
- Nén bounded: cạnh 1280/1024/768, quality 92→52 bước 8, dừng khi ≤800 KiB. 500–800 KiB là mục tiêu, không upscale hoặc thêm byte cho ảnh đơn giản. Giữ rotation/mirror/crop/recycle; không tự lưu gallery.
- GET ảnh thử lại tối đa một lần khi IOException truyền tải; không retry HTTP/ApiFailure hoặc POST. Không thay ApiClient/auth/refresh policy.

## VERIFY

| Nhóm | Kết quả thực tế | Log trong .local/artifacts/5c/ |
|---|---|---|
| Backend verify + JAR | 95/95 đạt; 0 fail/error/skipped; 53.718 s | server-verify-boundaries.log |
| Android debug/test/release + lint | Build đạt 29 s; lint 0 errors/3 warnings cũ | android-build-retest.log |
| Android JVM buộc chạy lại | 39/39 đạt; 0 fail/error/skipped | android-unit-final.log |
| USB Preview/5C | 11/11 đạt; 124.055 s | usb-preview-retest.log |
| USB gửi bạn bè offline/retry | 1/1 đạt; 124.003 s, có chờ quota fixture/marker | usb-sharing-offline.log |
| USB nav/lifecycle/owner-only regression | 3/3 đạt; 23.147 s | usb-regression.log |

Backend thêm 8 ca so với 87 regression: quyền hai recipients và friend không được chọn/timestamp/delete; recipient sai/trùng/self/mixed/CSV/missing/101; block hai chiều dù friendship còn; rollback recipient insert; chờ khóa và xét block vừa commit; MIME/EXIF thực; đúng 100 recipients; hai upload nhiều recipients chồng nhau. PostgreSQL test thật, không DB dev.

JVM thêm 4 ca so với 5B: repeated multipart đúng contract; recipient sai không tới network; GET transport retry/HTTP denial không retry; GET tối đa hai attempts và POST chỉ một attempt. Hai ca caption/đổi phiên 5B cùng 33 regression tiếp tục đạt.

USB xác minh gửi đủ 21 bạn qua hai trang và từng recipient tải đúng byte. Chụp thật gửi hai bạn được chọn; friend không được chọn nhận 404. Stale/block không fallback private. Unicode/recreate, đổi phiên/logout, light/dark, font lớn, màn hẹp, IME và compression đều đạt. Offline giữ draft/đóng băng caption và recipients/vô hiệu nút private; sau khôi phục, retry đúng người nhận tải được ảnh.

### Lỗi phát hiện và sửa

- Backend đầu 92 tests có 2 failure: Tomcat mặc định 50 parts chặn trước service; ca chờ khóa chưa clear snapshot pg_stat_activity. Sửa giới hạn hữu hạn 104 và refresh snapshot trong test; lượt sau 93/93 đạt, thêm hai ca biên rồi 95/95 đạt.
- PowerShell đầu dừng ở javac Note do ErrorActionPreference=Stop; không coi đó là build PASS. Chạy lại với Continue, thu exit code Gradle; sau đó buộc chạy lại JVM bằng task option `--rerun`.
- USB đầu 8 ca: 7 đạt, một GET của recipient gặp EOF sau POST thành công ở ca 21 bạn. Bổ sung GET retry hữu hạn và hai JVM test kiểm tra cả retry/không tự POST, rồi chạy lại toàn nhóm liên quan. Không nới assertion, bỏ người nhận hoặc gọi suite từng fail là PASS.

## Lệnh tái lập

Server: từ root chọn JDK bằng `. ./scripts/Use-TaktakJava.ps1 -Target Server`; vào `server`, chạy `./mvnw.cmd "-Dmaven.repo.local=C:/Users/NamTien/Documents/taktak/.tools/maven-repository" -B -ntp verify`.

Android: chọn `-Target Android`, đặt `$ErrorActionPreference='Continue'` để javac Note không dừng trước exit code; vào `android-client`:

```powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

URL release là placeholder HTTPS cho build validation, không phải endpoint deploy. Smoke runner `scripts/Start-LocalSmokeServer.ps1 -Port 8080` tạo PostgreSQL/storage test riêng. ADB reverse 8080, cài cả APK app/test phải Success trước instrument. Fixtures kiểm tra không thay session cá nhân; credentials sinh runtime/RAM, không ghi Git/log/docs. Ca offline chỉ chạy riêng theo marker, không tính Assume/skipped là PASS.

## REVIEW → HANDOFF

Đã xem ảnh trước/sau chụp thật, recipient bị từ chối, offline/retry và sáng/tối/IME trong `.local/artifacts/5c/screens-final/`; không có overlay che app. Ảnh tổng hợp màu Coral trong ca offline là fixture có chủ đích, không nhận đó là ảnh chụp thật. Hợp đồng: [MOMENT_API.md](MOMENT_API.md), [API_ERRORS.md](API_ERRORS.md). Giới hạn parts đối chiếu [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/appendix/application-properties/) và upload thật đúng 100 recipients.

Cleanup: smoke dừng exit 0, 8 moments/8 private files/47 recipients. Cache draft còn 0 file, port 8080 còn 0 listener, reverse đã gỡ, CAMERA granted. Cold Main 1532 ms → Login; hierarchy có emailInput, không previewImage. Không xóa DB/ảnh dev/gallery. HEAD giữ nguyên, index rỗng; không commit/push/deploy.

Review diff --check, XML resources và local links đều đạt. Quét 37 file hiện hữu dirty/untracked không phát hiện mẫu API key/private key/GitHub token thực; google-services.json và artifacts vẫn được ignore. Đây là kiểm tra mẫu, không thay thế secret scanning toàn diện.

### File thay đổi trong 5C

- Android (9): `data/remote/MomentApi.java`, `data/repository/MomentRepository.java`, `ui/camera/PhotoProcessor.java`, `ui/camera/PreviewActivity.java`, `viewmodel/PreviewViewModel.java`, `res/layout/activity_preview.xml`, `res/values/preview_strings.xml`, `src/test/java/com/taktak/app/data/remote/MomentCaptionTest.java`, `src/androidTest/java/com/taktak/app/PreviewAcceptanceTest.java`. Các file này có thay đổi 5B; cập nhật nối tiếp nội dung đã kiểm tra.
- Server (7): `moment/MomentController.java`, `moment/MomentRepository.java`, `moment/MomentService.java`, `storage/PrivateImageStorage.java`, `user/UserRepository.java`, `src/main/resources/application.yml`, `src/test/java/com/taktak/server/MomentIntegrationTest.java`.
- Tài liệu (7): `MOMENT_API.md`, `API_ERRORS.md`, `PROJECT_DECISIONS.md`, `PROJECT_STATUS.md`, `TASKS.md`, `SESSION_HANDOFF.md`, `QA_MOMENT_SHARING.md` (mới). Các file 5B khác không sửa trong 5C.

### Giới hạn và bước tiếp theo

Chưa idempotency 5D: POST timeout trước nhận ID rồi người dùng retry có thể tạo thêm một moment hoàn chỉnh. Selection/known ID chưa bền qua full process death; không hứa exactly-once. Pagination không phải snapshot membership atomic; server validate toàn bộ set cuối cùng. Block/unfriend chặn gửi mới, không tự thay grant ảnh cũ ngoài spec.

Chưa History 5E: B được kiểm chứng bằng API/JWT, không giả là đã có màn Đã nhận. B/C là các phiên độc lập trên cùng điện thoại USB và HTTP fixture; chưa nghiệm thu 5C trên hai điện thoại vật lý. Chưa API26/API37/HTTPS runtime/production ACL/signing/FCM/widget/deploy.

Không có thao tác thủ công bắt buộc để nghiệm thu 5C. Muốn dùng app dev tiếp cần backend và kết nối debug vì smoke đã dừng. Đề xuất duy nhất: **5D — progress/retry upload và idempotency**, chờ người dùng xác nhận.
