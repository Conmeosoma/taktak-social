# Hồ sơ cá nhân, avatar và kiểm thử Firebase/widget

Ngày 18/10/2026 (Asia/Saigon). Người dùng duyệt ba hạng mục này sau review dự án, rồi chọn **kiểm thử emulator trước vì đang sử dụng điện thoại**. Bảo toàn toàn bộ bản sửa review đang có; không commit/push/deploy hoặc thay migration.

## Chức năng được triển khai

- Tab Hồ sơ đã thay placeholder bằng Java/XML/ViewBinding: xem username/email, sửa tên hiển thị, chọn ảnh đại diện, tải lại và đăng xuất qua SessionViewModel hiện có.
- Giữ hệ màu/theme M3E hiện có, giới hạn form 480dp, cuộn được, email/username dài không làm vỡ layout, hỗ trợ sáng/tối; toolbar không lặp nút logout trên tab Hồ sơ.
- ProfileViewModel + ProfileRepository dùng cùng ApiClient/refresh JWT. Bản nháp tên giữ qua recreate; logout/đổi phiên dọn dữ liệu và ngăn callback cũ cập nhật UI.
- Kết quả chọn ảnh do Activity nhận để không bị mất khi session gate dựng lại Fragment lúc trở về foreground. Generation của phiên được lưu trước khi mở picker, ảnh trả về không được chuyển sang tài khoản khác.
- Đọc ảnh có giới hạn 16MiB, decode có sampling, xử lý EXIF qua thư viện AndroidX đã có từ CameraX, crop vuông/JPEG bằng PhotoProcessor; không thêm dependency hoặc lưu vào gallery.
- Avatar xuất hiện ở Hồ sơ, Bạn bè, Lịch sử và Viewer khi người gọi có quyền. AvatarLoader tải qua ApiClient để refresh JWT vẫn hoạt động; không cache ảnh private trên đĩa Glide, giới hạn hàng đợi và đóng executor khi view bị gỡ.

## API và lưu trữ avatar

| Endpoint | Quyền / hành vi |
|---|---|
| `GET /api/users/me` | Giữ DTO hiện có, chỉ hồ sơ của người gọi |
| `PUT /api/users/me` | Chỉ sửa displayName, không mass-assign avatar/email/username |
| `POST /api/users/me/avatar` | Multipart `image`, chủ tài khoản từ JWT; trả DTO hồ sơ an toàn |
| `GET /api/users/me/avatar` | JPEG có JWT; 204 nếu chưa có avatar |
| `GET /api/users/{id}/avatar` | Chính chủ hoặc bạn đã accepted, không block hai chiều; người lạ/block nhận 404, chưa xác thực 401 |

Không tạo URL ảnh public. GET có `Cache-Control: no-store`; không trả avatar_path/private path qua DTO. Avatar tách khỏi moment tại `IMAGE_STORAGE_ROOT/avatars/<server-UUID>.jpg`, dùng cột `users.avatar_path` đã có trong V1.

Server kiểm tra JPEG thực, giới hạn 5MiB/4096 cạnh/16MP bằng sanitizer hiện có, bỏ metadata rồi tạo JPEG vuông 512×512. File mới được dọn khi rollback, file avatar cũ chỉ dọn sau commit. Sửa tên và avatar cùng khóa row user để tránh thiết bị thứ hai ghi đè reference avatar mới bằng entity cũ.

Backup/restore native đã hỗ trợ subfolder `avatars/` và xác minh cả references từ `users` sau restore. Contract fixture có cả ảnh moment/avatar để bắt regression; các công cụ DB/systemd vẫn được mock trong suite shell này.

## Kiểm thử và bằng chứng

Artifacts local ở `.local/artifacts/`, được Git ignore; không chứa nội dung private key hay raw token do bộ test ghi ra.

| Kiểm tra | Kết quả hiện có |
|---|---|
| Backend suite trước bổ sung race | 135/135 PASS, JAR build |
| AvatarIntegrationTest cuối | 6/6 PASS: upload/download vuông, auth/isolation, friend/block, sai định dạng/quá lớn, replace/rollback, concurrent name/avatar |
| Full backend cuối | **136/136 PASS, 0 failures/errors/skips + runnable JAR** (`profile-server-final-retest.log`) |
| Android JVM | 72/72 PASS, thêm 3 ca profile update/refresh, validation và in-flight đổi phiên |
| Android build/lint | Debug/release và test APK build; 0 lint errors, 9 warnings cũ mỗi variant |
| ProfileAcceptanceTest | 5/5 PASS trên AVD: tên Unicode/validation/recreate; chọn ảnh trả về khi resume; hủy/ảnh lỗi; bản nháp/logout; avatar bạn bè + dark mode |
| RealFcmWidgetAcceptanceTest | PASS với Firebase thật; chạy riêng 1/1 và chạy chung trong batch 6/6, không giả push/local event để thay thế delivery |
| Shell contract có avatar | 11/11 PASS; OpenSSL/tar/checksum thực, DB/privileged commands/locking mock |
| Hồi quy trên APK cuối | 10/10 PASS: 2 ca avatar/picker/dark, 2 History, 6 privacy/cache/logout; `profile-regression-final.log` |

Log chính: `profile-server-final-retest.log`, `profile-avatar-retest.log`, `profile-android-final.log`, `profile-fcm-final.log`, `real-fcm-widget.log`, `profile-maintenance.log`.

Lượt đầu cài emulator bị từ chối vì chưa boot xong, không tính là test PASS. Một lần compile fixture ActivityResult dùng sai overload đã sửa sang requestCode thực. Race test ban đầu chưa quan sát được wait event; bật `track_activities=on` cho **cluster test** và clear statistics snapshot khi polling, giữ nguyên assertion/ca test, chạy lại 6/6 đạt rồi chạy lại toàn suite. Không nới điều kiện production để chữa test.

## Firebase thật đã được kiểm chứng đến đâu

Admin service-account JSON được người dùng lưu local; đã kiểm tra `type=service_account`, project khớp JSON Android và có private key mà không in giá trị. File nằm trong `.local/secrets`, không tracked. Nguồn quy trình chuẩn bị: [Firebase Admin SDK setup](https://firebase.google.com/docs/admin/setup). Không tạo Firebase Auth/Firestore/Storage/Functions.

Backend smoke chạy với PostgreSQL và media fixture riêng, bật FirebaseAdmin qua `Start-LocalSmokeServer.ps1 -EnableFcm -FirebaseCredentials <đường-dẫn-local>`. Tham số là đường dẫn, không đưa nội dung key vào command/log. Android debug dùng API loopback qua ADB reverse trong môi trường test tin cậy.

Luồng đã chạy thực tế:

1. Hai tài khoản QA kết bạn, thiết bị nhận lấy FCM token thật và đăng ký vào backend fixture.
2. Test bind provider Taktak bằng **AppWidgetHost QA** qua AppWidgetManager của Android, không giả RemoteViews hay tự gọi render để tạo kết quả.
3. Tài khoản gửi upload JPEG fixture, server gọi Firebase; FirebaseMessagingService nhận `moment_created`, WorkManager tải bằng JWT và widget có ảnh/tên/caption đúng.
4. Chuyển app về nền bằng HOME; upload ảnh thứ hai, nhận FCM thật và caption RemoteViews đổi thành `Background QA`.
5. Xóa hai ảnh ở server, nhận hai `moment_deleted`; Worker đưa widget về empty. Logout đưa widget về logged-out.
6. Test chỉ xóa widget ID do chính fixture cấp, unregister token/logout tài khoản QA; không thay đổi tài khoản hoặc widget của người dùng.

Ảnh bằng chứng đã đọc:

- `profile-screenshots/qa-profile/`: light, tên đã lưu, avatar đã upload, avatar bạn bè, dark mode. Hai ảnh avatar/dark cuối đã kéo lại vào `profile-final-screenshots/` và xem xác nhận placeholder hiển thị đúng.
- `fcm-widget-screenshots/`: foreground, background, deleted-empty, logged-out. Đây là ảnh vẽ từ RemoteViews đang chạy trong QA host, không phải screenshot chứng minh launcher OEM đã cập nhật.

## Giới hạn và công việc tiếp theo

- **Chưa nghiệm thu điện thoại thật/launcher OEM** theo lựa chọn hiện tại của người dùng. Firebase cloud delivery đã thật, nhưng thiết bị là AVD API37 và instrumentation giữ process test; chưa chứng minh Doze/tiết kiệm pin/force-stop trên MIUI hoặc Android khác.
- Chưa test endpoint production qua VPS/HTTPS; không tự deploy, mở firewall hoặc đổi credentials server thật. Bản release vẫn cần domain thật và ký phát hành.
- Chưa gọi profile/FCM/cloud tests này là 100% nghiệm thu MVP. Không suy ra test AVD là test USB. Không đổi chính sách JWT, grant của moments hoặc schema.
- Avatar chỉ cho chính chủ/bạn hiện tại xem; sau block/unfriend UI dùng ảnh mặc định. Không thêm xóa avatar, đổi email/username/password hoặc chức năng ngoài hạng mục đã duyệt.
- File cleanup sau commit vẫn best-effort như storage hiện có; chưa có durable janitor khi lỗi filesystem hoặc process bị kill.

Smoke server đã dừng sạch (exit 0), đối chiếu 4 moment rows/4 files và 4 avatar rows/4 files. Đây là fixture riêng, không phải dữ liệu production; không xóa dữ liệu người dùng. Đã gỡ ADB reverse và quyền bind widget QA trên emulator.

Skill `android_ui_verification` được dùng để kiểm tra ảnh màn hình, light/dark và luồng UI thực tế thay vì chỉ xác nhận XML build được.

Kiểm tra bàn giao: `git diff --check` đạt; 139 liên kết Markdown local tồn tại; quét mẫu private key/API key/GitHub token/JWT trong các file thay đổi không có phát hiện. Đây là quét mẫu, không thay thế secret scanner toàn lịch sử Git. Admin JSON được `git check-ignore` xác nhận bỏ qua.

## Nhóm file của hạng mục này

- Android: `ProfileFragment`, `ProfileViewModel`, `ProfileRepository`, `AvatarPhoto`, `AvatarLoader`, layout/strings/ids hồ sơ; nối `UserApi`, `AuthModels`, `AuthRepository`, `MainActivity`, `MainPagerAdapter`; avatar ở adapter/fragment Friends và History, `MomentViewerActivity`.
- Server: `AvatarController`, `AvatarService`, `User`, `UserService`; kiểm thử `AvatarIntegrationTest`.
- QA: `ProfileRepositoryTest`, `ProfileAcceptanceTest`, `RealFcmWidgetAcceptanceTest`, `LocalSmokeServer`, `Start-LocalSmokeServer.ps1`; backup/restore và fixtures contract trong `deploy/`.
- Tài liệu: báo cáo này, `PROJECT_STATUS`, `TASKS`, `SESSION_HANDOFF`, `PROJECT_DECISIONS`, `FIREBASE_SETUP`, `QA_REVIEW_FIXES`, `deploy/MAINTENANCE.md`. Các bản sửa review có sẵn ngoài nhóm này được bảo toàn, chưa commit.

Trạng thái: Hồ sơ/avatar và Firebase/widget **PASS trong phạm vi local/AVD đã nêu**; hồ sơ bàn giao đã cập nhật. Bước tiếp theo duy nhất: nghiệm thu cùng luồng trên điện thoại thật khi người dùng sẵn sàng cho QA.
