# Taktak — Công việc và cổng xác nhận

Cập nhật: 16/10/2026. Nguồn đối chiếu: [đặc tả](TAKTAK_PROJECT.md), [trạng thái](PROJECT_STATUS.md), [bàn giao](SESSION_HANDOFF.md) và lịch sử yêu cầu trong cuộc trò chuyện.

Quyền mới nhất: người dùng duyệt riêng **3A**, yêu cầu skill android_ui_verification. Không tự chuyển sang 3B; quyền AUTO CONTINUE cũ chỉ áp dụng 0–1.

`PASS` chỉ áp dụng cho phạm vi đã có bằng chứng. `CHƯA DUYỆT` nghĩa là không được tự triển khai; `CHƯA XÁC MINH` không đồng nghĩa thất bại.

| Mã | Công việc | Trạng thái | Bằng chứng / điều kiện |
|---|---|---|---|
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
| 3B–8 | Các tiểu bước MVP tiếp theo | CHƯA DUYỆT / CHƯA NGHIỆM THU | Không suy đoán hoàn thành; chỉ chọn từng tiểu bước khi người dùng yêu cầu |

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

Tiểu bước đề xuất tiếp theo duy nhất: **3B — nối AuthApi/Repository/ViewModel, phiên mã hóa và refresh đồng bộ**. Chưa được duyệt.

Lệnh, báo cáo, cảnh báo và phạm vi kiểm thử: [QA_BOOTSTRAP.md](QA_BOOTSTRAP.md), [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md).
