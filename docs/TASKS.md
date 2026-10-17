# Taktak — Công việc và cổng xác nhận

Cập nhật: 18/10/2026. Nguồn đối chiếu: [đặc tả](TAKTAK_PROJECT.md), [trạng thái](PROJECT_STATUS.md), [bàn giao](SESSION_HANDOFF.md) và lịch sử yêu cầu trong cuộc trò chuyện.

Quyền mới nhất: **5E — Lịch sử ảnh đã nhận (Received Moments History): PASS local/USB**, được duyệt riêng. Backend GET /api/moments?type=received native query index-friendly, stranger/self-moment isolation, pagination LIMIT 21 OFFSET. Android HistoryFragment M3E, HistoryViewModel LRU Bitmap cache 20MB, định dạng thời gian tiếng Việt, tải ảnh bảo mật qua JWT. Backend 104/104 verify, Android 45/45 unit tests, assemble/lint debug/release đạt 0 errors, 3 acceptance tests USB máy Xiaomi đạt. Xem [QA_MOMENT_HISTORY.md](QA_MOMENT_HISTORY.md). Dừng chờ duyệt riêng **Giai đoạn 6**. HEAD vẫn `ed9fde6`; bảo toàn toàn bộ thay đổi chưa commit.

`PASS` chỉ áp dụng cho phạm vi đã có bằng chứng. `CHƯA DUYỆT` nghĩa là không được tự triển khai; `CHƯA XÁC MINH` không đồng nghĩa thất bại.

| Mã | Công việc | Trạng thái | Bằng chứng / điều kiện |
|---|---|---|---|
| M3E-01 | Foundation + Login Material3Expressive1.14.0, Java/XML | PASS local/USB | Theme opt-in;light/dark,font1.5,màn hẹp/IME,offline/retry/auth/logout;28unit,4+4USB,build/lint;QA_M3E_LOGIN |
| M3E-02 | Redesign Register | PASS local/USB | Light/dark,long form,font1.5,IME,validation/loading/recreate,offline/retry/API/conflict;28unit,4+7USB,build/lint;QA_M3E_REGISTER |
| M3E-03 | Camera + trạng thái phiên theo M3E | PASS local/USB | Camera-first, scoped theme giữ Friends cũ; 28unit/20USB/build/lint, QA_M3E_CAMERA |
| M3E-04 | Redesign Bạn bè/Friends | PASS local/USB | M3E cards/header cuộn/light-dark/IME/dialog;28unit,20USB,build/lint;QA_M3E_FRIENDS |
| 0A | Khóa định danh và quy ước repository | PASS — tài liệu/quy ước | Có đủ 4 file đầu ra; kiểm tra lại ignore, link, nội dung và định danh; xem bàn giao |
| RESUME-DOCS | Khôi phục ngữ cảnh và tạo hồ sơ trạng thái còn thiếu | PASS — hồ sơ | Kiểm tra 7 file, 19 link local, 3 file mới không bị ignore; SHA-256 xác nhận 62 file có sẵn giữ nguyên |
| 0B | Firebase Android `com.taktak.app`, chỉ FCM | PASS — đăng ký/cấu hình | Kiểm tra local, ảnh Console khớp định danh, FCM V1 Enabled; người dùng xác nhận chưa thiết lập dịch vụ bị loại trừ |
| 0C | Kịch bản kết nối emulator và máy thật | PASS local | Alias emulator và máy thật qua USB reverse đã kiểm tra; LAN/tunnel chưa chạy |
| 1A | Server Java 17 / Spring Boot 3 / Maven tối thiểu | PASS local | JAR build; 6 test PostgreSQL/Flyway/HTTP/constraints, không skipped; độc lập Firebase |
| 1B | Android Java/XML tối thiểu | PASS bootstrap | ViewBinding/dependency/config mạng; APK debug/release và UI emulator đạt; Firebase khớp ảnh Console |
| 1C | Build, database/migration, health và kết nối thiết bị | PASS local | Backend/migration/build có bằng chứng PASS; emulator alias 4/4 và máy thật USB reverse 4/4 PASS; cài/mở/UI đạt |
| 2A | Đăng ký backend, User/Repository, BCrypt, validation, unique/transaction | PASS | Maven verify 13/13 integration test, gồm race username/email và regression; xem REGISTRATION_API |
| 2B | Login/JWT, refresh rotation/logout, giới hạn thử login | PASS | 29/29 test; expired/forged JWT, rotation/replay/race/rollback, logout và IP/account limits; HTTP smoke ngoài test profile PASS |
| 2C | GET/PUT hồ sơ cá nhân, chỉ sửa displayName | PASS | 36/36 test, 7 case hồ sơ mới; isolation A/B, mass assignment, validation, JWT, DTO; HTTP smoke ngoài test profile và JAR PASS |
| 2D | Integration test/curl cho auth/hồ sơ, chốt lỗi | PASS local | 39/39 test và JAR, curl thật cho expiry/forgery/duplicate/rotation/logout; hợp đồng lỗi chung đã kiểm tra |
| 3A | Login/Register Java XML, ViewBinding/Material, validation/loading | PASS UI local | Build/lint debug/release; 8 unit/12 emulator test; screenshot light/dark/required/IME, sửa keyboard che field; chưa nối API |
| 3B | Auth API/Repository/ViewModel, phiên mã hóa và refresh đồng bộ | PASS local | Build/lint debug/release; 20 unit + 17 emulator test với backend thật; xem QA_ANDROID_AUTH |
| 3C | Điều hướng phiên, Camera destination và logout UI | PASS | Local lịch sử 20 unit/25 emulator; USB mới 6/6 test chọn lọc, cold-process restore/logout, 5 screenshot; xem QA_SESSION_ROUTING |
| 3D | CameraX → upload chỉ mình tôi → GET bằng JWT | PASS | Maven46/46; Android build/lint +20 unit;4 ca media +1 lặp đủ sáng +6 auth regression máy thật PASS |
| UI-CAMERA | Redesign Camera sau3D, không thêm chức năng | PASS local/USB | Python3.12.10/CLI skill; Java/XML theme Camera riêng, build/lint +20 unit;13 ca USB/14 lượt đạt gồm quyền, sáng/tối/busy, ảnh thật/offline/retry và logout; xem QA_CAMERA_REDESIGN |
| 4A | Search username, gửi/list pending incoming server | PASS backend local | Maven65/65 +JAR;19 ca mới gồm quota/block/DTO/isolation/concurrency/rollback,46 regression; schema/Android không đổi |
| 4B | Accept/reject, canonical friendship/list/unfriend/block/unblock | PASS backend local | Maven87/87 +JAR;22 HTTP/PostgreSQL tests mới/65 regression, race/rollback/receiver-only/idempotency; schema/Android giữ nguyên |
| 4C | FriendsFragment nối API qua Repository/ViewModel | PASS local/USB/AVD | Search/incoming/friends và actions;28 unit,6 USB cuối,7 regression,offline/retry,A/USB↔B/AVD+C;build/lint;avatar local, chưa recipient-aware media5C |
| 5A | ViewPager2/bốn tab, CameraX quyền/đổi camera/flash/lifecycle/rotation | PASS local/USB | 33JVM,33ca USB duy nhất,build/lint debug/release;QA_MAIN_CAMERA;History/Profile chưa chức năng |
| 5B | Cache JPEG/crop/mirror; Preview/chụp lại/caption50/chọn bạn | PASS local/USB | 35 JVM,32 USB sau retest;21 bạn phân trang,đổi phiên,IME/offline/JWT;QA_PREVIEW |
| 5C | Gửi ảnh cho bạn bè; nén bounded/private storage/recipient authorization | PASS local/USB | 95backend,39JVM,15USB; chọn2/đủ21bạn,100recipient/race/rollback/EXIF/JWT/offline;QA_MOMENT_SHARING |
| 5D | Tiến trình upload, retry & Idempotency Key | PASS local/USB | 99backend, 42JVM, 3USB; DB V2 index, SHA-256 fingerprint, CountingRequestBody 0–100%, SavedStateHandle retry; QA_MOMENT_IDEMPOTENCY |
| 5E | Lịch sử ảnh đã nhận (Received Moments History) | PASS local/USB | 104backend, 45JVM, 3USB; HistoryFragment M3E, LRU Bitmap cache 20MB, JWT image load, relative time, native index query; QA_MOMENT_HISTORY |
| 6–8 | Các giai đoạn tiếp theo (FCM, Widget, Polish) | CHƯA DUYỆT / CHƯA NGHIỆM THU | Đề xuất duy nhất Giai đoạn 6 (Firebase Cloud Messaging & Real-time Sync); chờ duyệt |

## Nghiệm thu phiên khôi phục trước khi duyệt 0B (lịch sử)

- [x] Đọc toàn bộ đặc tả, quy tắc agent, README và quyết định kỹ thuật.
- [x] Kiểm tra Git status, lịch sử commit local, thay đổi staged/unstaged/untracked.
- [x] Đọc source, Gradle, Manifest, layout và test mẫu liên quan.
- [x] Xác định 0A là tiểu bước gần nhất được duyệt; 0B chưa được cho phép.
- [x] Kiểm tra lại các đầu ra 0A; chỉ ghi kết quả kiểm tra đã thực hiện.
- [x] Tạo PROJECT_STATUS, TASKS và SESSION_HANDOFF sau khi xác nhận chúng chưa tồn tại.
- [x] Xác minh file mới, liên kết, dấu conflict, mẫu credentials và tính toàn vẹn các file có sẵn.
- [x] Hoàn tất hồ sơ bàn giao; giữ điểm dừng tại cổng xác nhận 0B.

## 0B — việc còn thiếu trong phạm vi được duyệt

Người dùng đã xác nhận “continue” cho 0B. Không xin phê duyệt lại; chỉ cần bổ sung kết nối/bằng chứng còn thiếu.

- [x] Đọc đặc tả, code/config và trạng thái Git trước khi sửa.
- [x] Kiểm tra JSON, loại trừ Admin private-key fields, đối chiếu App ID/project number và package/applicationId.
- [x] Kiểm tra file cấu hình bị ignore, không có cấu hình/credential filenames tương ứng trong Git index.
- [x] Kiểm tra Firebase dependency khai báo trực tiếp chỉ gồm BoM và Messaging; giữ nguyên source/SDK.
- [x] Tìm khả năng truy cập Firebase: không có CLI trong PATH, tìm thấy plugin chính thức nhưng chưa cài/kết nối; đã đề xuất tích hợp.
- [x] Ghi 10 kết quả local và hướng dẫn đối chiếu vào [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
- [x] Đối chiếu project ID/project number và Android App ID/package trong ảnh Console với JSON local: khớp.
- [x] JSON khớp app/project trong ảnh; người dùng xác nhận chưa thiết lập Auth/Firestore/Storage/Functions; không tạo Admin credentials.
- [x] Ghi nguồn ảnh/xác nhận thủ công và kết luận 0B PASS trong FIREBASE_SETUP; chưa kiểm thử FCM runtime.

Không tạo lại project/app khi chưa đối chiếu tài nguyên đã có; không gửi nguyên JSON hoặc key vào báo cáo. Khi 0B đạt, tiếp tục các việc còn lại của 0–1 theo quyền AUTO CONTINUE; không xin lại phê duyệt từng tiểu bước.

## Bàn giao AUTO CONTINUE

- [x] Chuẩn bị JDK 17/Maven portable có checksum; helper chỉ chọn Java trong process.
- [x] Tạo server, migration V1, security mặc định đóng, env mẫu, Wrapper và 6 integration test với PostgreSQL thật.
- [x] Build executable JAR; test startup/schema/constraints/HTTP.
- [x] Tận dụng Android skeleton; ViewBinding, Java dependencies, URL/network policy theo variant, FCM auto-init tắt.
- [x] Build/lint debug và release; xác nhận ba cấu hình mạng không hợp lệ bị từ chối.
- [x] Chạy 4 instrumentation test qua reverse và xem screenshot/UI dump theo skill kiểm tra Android.
- [x] Thử lại đường alias; lưu FAIL timeout riêng, không che lỗi hoặc sửa package.
- [x] Có bằng chứng Firebase Console để nghiệm thu 0B và liên kết Firebase của 1B.
- [x] Sửa lỗi alias Android 17: quyền local network riêng debug, cấp quyền trong instrumentation; 4/4 test PASS trực tiếp `10.0.2.2:8080`, không reverse.
- [x] Kiểm tra health/app trên máy thật: Android 15 API 35 / 23129RAA4G, USB reverse, 4/4 test PASS và screenshot/UI dump.
- [x] Đạt điều kiện bootstrap local Bước 0/giai đoạn 1; dừng trước giai đoạn 2. LAN/tunnel chưa chạy, chưa FCM runtime/deploy.

## 2A — hoàn tất sau khi người dùng duyệt “continue”

- [x] Đọc đặc tả/source/Git; bảo toàn thay đổi các phiên trước.
- [x] Tạo User/JpaRepository và service transaction, không sửa migration V1.
- [x] Mở riêng POST register, trả 201 với DTO an toàn; các route nghiệp vụ khác vẫn 401.
- [x] Chuẩn hóa username/email, validation, password UTF-8/Unicode và BCrypt cost 12.
- [x] Xử lý 400/409/415 theo JSON lỗi; không phản chiếu password/SQL.
- [x] Chạy 13 test thật trên PostgreSQL test riêng, gồm hai case HTTP đồng thời, 0 failures/errors/skipped; build JAR PASS.
- [x] Ghi hợp đồng/cách chạy/kết quả và bàn giao; không commit/push/deploy.

## 2B — đã được duyệt và hoàn tất

- [x] Login bằng email/password; BCrypt/dummy hash, không tạo phiên khi credentials sai.
- [x] JWT HS256, secret môi trường fail-fast; kiểm tra exp/iat/iss/aud/sub/chữ ký, 401 JSON an toàn.
- [x] Refresh token ngẫu nhiên/hash-only trong bảng V1 có sẵn, lock/transaction rotation; replay 401, race chỉ một thành công, failure rollback.
- [x] Refresh không phụ thuộc access JWT, kể cả expired bearer gửi kèm; logout idempotent thu hồi refresh được gửi.
- [x] Login quota 20/IP và 5/account trong 60 giây; Retry-After, bounded memory, đồng thời không bypass; không tin X-Forwarded-For.
- [x] Maven verify 29/29 PASS, JAR; HTTP register→login→refresh→replay→logout→revoked refresh đúng 201/200/200/401/204/401.
- [x] AUTH_API/README/env mẫu/quyết định/trạng thái/bàn giao cập nhật; không đổi V1 hoặc source Android.

## 2C — đã được duyệt và hoàn tất

- [x] Đọc đặc tả/quyết định/trạng thái/Git/source; bảo toàn thay đổi cũ của 2A/2B.
- [x] GET/PUT `/api/users/me` xác thực qua filter chain hiện có, user ID chỉ lấy từ JWT subject.
- [x] DTO chỉ trả id/email/username/displayName/createdAt; no-store, không entity/hash/private path.
- [x] Chỉ bind và cập nhật displayName; strip/nonblank/max 255 như đăng ký, transaction JPA.
- [x] User không tồn tại trả 401 an toàn; input lỗi giữ format 400/415 có sẵn.
- [x] Thêm 7 test HTTP/PostgreSQL: GET, PUT/persistence/idempotency, A/B và mass assignment, invalid body, biên độ dài, JWT và orphan subject.
- [x] Maven verify 36/36 PASS, không failures/errors/skipped; JAR có controller và không chứa test artifacts.
- [x] Smoke ngoài test profile: register→login→GET→PUT→GET→refresh→GET→logout đúng 201/200/200/200/200/200/200/204.
- [x] PROFILE_API/quyết định/trạng thái/bàn giao cập nhật; không sửa migration, dependency, Android, Firebase hoặc README.

## 2D — đã được duyệt và hoàn tất

- [x] Đối chiếu Git/source/bằng chứng 36 test cũ, không coi output cũ là mới chạy.
- [x] Curl thật trên loopback: register/login/me/PUT, username trùng, JWT giả/hết hạn, refresh/replay/logout/revoked refresh.
- [x] Không đưa credentials vào argv/file/log; curl config qua stdin, bỏ curlrc/proxy và có timeout.
- [x] Test ban đầu bắt được 404 bị đổi thành 403; sửa MVC handler 404/405, giữ filter chain và `/error` đóng.
- [x] Chốt JSON lỗi/headers/path/time; xử lý 406/500 an toàn; fault injection chỉ test scope.
- [x] Script Windows nghiệm thu đầy đủ và chế độ CurlOnly đã chạy; bản cuối 39/39 test PASS, JAR build.
- [x] API_ERRORS/QA_AUTH và hồ sơ bàn giao cập nhật, không migration/dependency/Android/Firebase/README.

## 3A — đã được duyệt và hoàn tất trong phạm vi UI

- [x] Đọc skill được chỉ định, đặc tả/quyết định/trạng thái/Git và source Android trước sửa; bảo toàn thay đổi cũ.
- [x] Login/Register Java/XML/ViewBinding/Material, lỗi từng field, password toggle/confirmation, scroll/IME/insets và light/dark.
- [x] Validation theo giới hạn backend; password không trim/cắt/lưu view state hoặc log.
- [x] Renderer loading khóa nút/inputs và guard duplicate; submit hợp lệ chỉ báo chưa nối server, không giả auth.
- [x] Launcher có lối mở form; auth activity không exported; chưa routing Camera/phiên.
- [x] Sửa root cause môi trường/style/min26; không tắt test/lint hoặc thay stack.
- [x] Skill UI dump/screenshot phát hiện keyboard che confirmation; sửa viewport, thêm test IME, xem ảnh sau sửa.
- [x] Bản cuối 8 unit + 12 instrumentation PASS, 0 failures/errors/skipped; build/lint debug và release PASS.
- [x] Manifest release/ignore/credentials và hồ sơ QA/trạng thái/bàn giao được kiểm tra; không commit/push/deploy.

## 3B — đã được duyệt và nghiệm thu local

- [x] Khôi phục Git/code: HEAD 8766f26 đã chứa triển khai; giữ nguyên commit/source và kiểm tra lại thay vì tạo lại.
- [x] AuthApi/UserApi, DTO Gson, ApiClient tách auth/protected và Repository gọi server thật.
- [x] ViewModel/LiveData + worker, loading/duplicate guard, lỗi local an toàn, cooldown Retry-After.
- [x] EncryptedSharedPreferences/Keystore, ghi cặp token, origin binding, backup exclusion, fail-closed.
- [x] Bearer đúng đích; refresh đồng bộ, tối đa một replay; clear phiên khi revoke/401, guard kết quả trễ sau đổi phiên.
- [x] Chạy lại 20 unit test, 0 failures/errors/skipped; 17 instrumentation PASS trên AVD read-only/backend/PostgreSQL tạm thật.
- [x] Build/lint debug/release PASS; 0 lint errors, 3 warnings được ghi rõ, không suppress.
- [x] Xem screenshot UI thành công/lỗi, rà policy/credentials và cập nhật hồ sơ; không commit/push/deploy.

Giới hạn chưa chạy và lệnh kiểm thử tại [QA_ANDROID_AUTH.md](QA_ANDROID_AUTH.md). Nghiệm thu auth máy thật/đóng mở app/routing còn ở 3C; không đánh dấu toàn giai đoạn 3 hoàn tất.

## 3C — PASS local và nghiệm thu máy thật Android 15

- [x] Đọc tài liệu/Git/source/skill; giữ 7 hồ sơ dirty từ 3B.
- [x] Launcher kiểm tra phiên trên worker, verify me/refresh trước Camera; phiên vắng/revoke về Login.
- [x] Login/logout dọn task/back stack, Camera destination placeholder chưa chụp ảnh.
- [x] Lỗi kết nối/read timeout/permission/storage có UI; retry hồi phục và logout local khi mạng lỗi.
- [x] Không báo xóa token thành công khi disk clear lỗi, giữ gate qua recreate và thử logout lại.
- [x] Sửa thông báo bị initial LiveData che, đồng bộ test IME theo visibility thực thay vì delay cố định.
- [x] Build/lint debug/release PASS (0 errors, 3 warnings); 20 unit + 25 emulator test PASS, fixture cold start 1/1 và ADB force-stop/relaunch/logout PASS.
- [x] Xem screenshot, review bảo mật và cập nhật hồ sơ; không đổi backend/migration/Firebase/dependency.
- [x] Điện thoại USB kết nối lại, không có phiên auth trước test; build/cập nhật debug giữ dữ liệu và cold launcher → Login trên máy thật PASS.
- [x] Máy thật: login đúng/sai, encrypted persistence, cold-process restore, mất kết nối/retry, logout/root task và revoke refresh PASS. Hai test launcher + bốn AuthBackendTest = 6/6 PASS; xác minh lại cold launcher về Login sau logout. Không tuyên bố suite cũ 25 test máy thật đạt; suite mặc định mới 27 test chưa chạy toàn bộ.

Mốc3C đã được duyệt tiếp3D sau đó; xem nghiệm thu3D bên dưới.

## 3D — PASS backend local và Android15 qua USB

- [x] Đọc đặc tả/quyết định/trạng thái/Git/source, bảo toàn thay đổi3B/3C/UI review.
- [x] CameraX sau, xin/từ chối quyền an toàn, lifecycle, rotation/crop1:1/JPEG có giới hạn; không gallery.
- [x] POST multipart owner-only, sender từ JWT, không recipient/FCM; JPEG thực tế/caption/dung lượng/pixel được kiểm tra.
- [x] Private storage tên server sinh, rollback dọn file, V1 không đổi; GET kiểm tra owner/deleted từng lần, B404/thiếu hoặc saiJWT401.
- [x] Repository/ViewModel dùng transport/refresh hiện có, chốt generation chống media qua phiên, không fake ảnh local; retry known id chỉ GET.
- [x] Maven46/46 +JAR;20 unit, build/lint debug/release0 errors/3 warnings;4 ca media mới máy thật và6 auth regression PASS, lặp chụp đủ sáng1/1 PASS.
- [x] Xem screenshot, kiểm tra cold logout không còn ảnh/token; smoke4row/4file/0recipient, dừng runner/gỡ reverse; giữ dữ liệu cá nhân.
- [x] Ghi giới hạn unknown POST response có thể tạo thêm moment; idempotency hoàn chỉnh ở5D, chưa coi31 test mặc định đã chạy.

Mốc3D đã được duyệt tiếp4A sau UI-CAMERA. API/hướng dẫn/giới hạn3D tại [MOMENT_API.md](MOMENT_API.md), [QA_SELF_MOMENT.md](QA_SELF_MOMENT.md).

## 4A — PASS backend local

- [x] Đọc toàn bộ đặc tả/quyết định, trạng thái/bàn giao/Git/source; bảo toàn dirty/untracked trước phiên.
- [x] GET users/search prefix chuẩn hóa, underscore literal, max20/public DTO, loại self/block hai chiều; quota user/IP bounded và Retry-After.
- [x] POST friends/requests receiverId, sender JWT; self/absent/block/friendship/duplicate checks; khóa cặp PostgreSQL và unique V1 chống race.
- [x] GET incoming chỉ người nhận/pending, lọc block hai chiều, page20/sort ổn định/hasNext; không email/hash/đường dẫn private.
- [x]14 integration +5 limiter tests mới,46 regression; Maven verify65/65,0 failures/errors/skipped +JAR,44.283giây.
- [x] Ghi lỗi fixture orphan và sửa test đúng nguyên nhân; không bỏ FK/JWT assertion, không dùng DB cá nhân.
- [x] Cập nhật hợp đồng/API_ERRORS/quyết định/trạng thái/handoff; không Android/migration/dependency hoặc commit/push/deploy.

Mốc4A đã được duyệt tiếp4B riêng.

## 4B — PASS backend local

- [x] Đọc toàn bộ đặc tả/Git, quyết định và source4A; bảo toàn dirty/untracked, không sửa Android hoặc migration.
- [x] PUT accept/reject chỉ receiver, hidden404; status/responded_at/friendship transaction, canonical UUID PostgreSQL, retry không resurrect.
- [x] GET friends page20/hasNext/public DTO, block filter hai chiều; DELETE friend chỉ cặp actor–target/idempotent.
- [x] POST block atomically ghi block/xóa friendship/reject pending hai chiều; DELETE unblock chỉ hướng của actor, không restore.
- [x] Cùng lockPair users cho send/respond/unfriend/block/unblock; projection tránh status cache cũ trước khóa; kiểm thử race và chờ lock thật.
- [x]22 test mới gồm receiver-only/JWT/DTO/input/pagination/rollback/concurrency,65 regression; Maven verify87/87,0 failures/errors/skipped +JAR,46.821giây.
- [x] Ghi3 lỗi harness ban đầu, sửa assertion tập kết quả và snapshot thống kê đúng nguyên nhân; không bỏ kiểm thử hoặc FK/transaction.
- [x] Cập nhật hợp đồng/API_ERRORS/quyết định/trạng thái/handoff; không commit/push/deploy hoặc tự chuyển4C.

**Đề xuất duy nhất:4C — FriendsFragment nối API qua Repository/ViewModel. Chờ duyệt, không triển khai.**

Lệnh, báo cáo, cảnh báo và phạm vi kiểm thử: [QA_BOOTSTRAP.md](QA_BOOTSTRAP.md), [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md).
