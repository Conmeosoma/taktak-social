# Taktak — Trạng thái dự án

Cập nhật: 17/10/2026 (Asia/Saigon). Nguồn: workspace/Git, bằng chứng kiểm thử, ảnh Firebase Console và xác nhận thủ công của người dùng.

## Trạng thái và quyền thực hiện

- Phạm vi mới nhất: người dùng duyệt riêng **4C — FriendsFragment nối API qua Repository/ViewModel**. **PASS local/USB/AVD**: search/incoming/friends, gửi/accept/reject/hủy bạn/chặn, trạng thái và phân trang, gắn generation phiên;28 unit,6 ca USB cuối,7 regression USB,offline/retry và A/USB↔B/AVD đạt. Build/lint debug/release đạt; backend verify87/87 +JAR chạy lại. Xem [QA_FRIENDS.md](QA_FRIENDS.md). Không đổi server/schema/dependency/Firebase/stack; chưa duyệt5A hoặc commit/push/merge/deploy.
- **4B PASS backend local**: receiver-only, canonical friendship/transaction, list/unfriend/block/unblock;22 test mới +65 regression tiếp tục đạt trong suite4C. Hợp đồng [FRIEND_RELATIONS_API.md](FRIEND_RELATIONS_API.md) giữ nguyên.
- **4A PASS backend local**: search/gửi/list pending, quota và block hai chiều tiếp tục đạt trong suite4B; hợp đồng [FRIEND_API.md](FRIEND_API.md) giữ nguyên.
- **UI-CAMERA PASS local/USB (lịch sử)**: build/lint debug/release,20 unit;13 ca máy thật/14 lượt thành công, Python3.12.10/CLI skill; xem [QA_CAMERA_REDESIGN.md](QA_CAMERA_REDESIGN.md). Không nhận các kiểm thử Android đó là vừa chạy lại trong4A.
- **0A: PASS trong phạm vi tài liệu/quy ước**, đã kiểm tra lại trong phiên khôi phục. Không đồng nghĩa toàn bộ Bước 0 hoặc ứng dụng đã PASS.
- Công việc khôi phục hồ sơ trước đó: **PASS**, có kết quả trong bàn giao lịch sử; không tính các phép kiểm tra cũ là vừa chạy lại.
- **0B: PASS — đăng ký/cấu hình**. Ảnh Console khớp Project ID/project number/App ID/package với JSON local, API FCM V1 Enabled. Người dùng xác nhận chưa thiết lập Auth/Firestore/Storage/Functions; đây là bằng chứng thủ công, không phải truy vấn tự động hay kiểm thử FCM runtime.
- **1A PASS local**: server Java 17/Spring Boot/Maven, Flyway 8 bảng, 6 test PostgreSQL thật PASS và JAR build được. Đây là phần độc lập Firebase.
- **1B PASS bootstrap**, có bằng chứng build/lint/UI và cấu hình Firebase khớp đăng ký. **0C/1C PASS local**: alias emulator 4/4 test PASS và máy thật Android 15 qua USB reverse 4/4 test PASS; đã cài/mở và xem UI. **Bước 0 và giai đoạn 1 hoàn tất trong phạm vi bootstrap local**. LAN/tunnel, deploy, signing và FCM runtime chưa kiểm thử.
- **2A PASS**: JPA User/Repository, POST `/api/auth/register`, validation/normalization, BCrypt cost 12, transaction và unique DB; 13 test regression tiếp tục PASS trong suite 2B.
- **2B PASS**: login, JWT HS256 mặc định 15 phút, refresh opaque/hash/rotation, logout và login rate limit IP/account. 29 test cũ tiếp tục PASS trong suite 2C; HTTP smoke ngoài test profile có bằng chứng.
- **2C PASS**: GET/PUT `/api/users/me`, danh tính từ JWT, DTO an toàn, chỉ sửa displayName trong transaction. Mốc 2C có 36/36 test và HTTP smoke ngoài test profile; các test tiếp tục nằm trong suite 2D.
- **2D PASS local**: integration/regression và curl thật cho auth/profile, expiry/forgery/duplicate/replay/logout; JSON lỗi đã chốt. Mốc 2D: 39/39 test/JAR và CurlOnly 1/1 PASS, không tính là vừa chạy lại trong 3A. Giai đoạn 2 hoàn tất backend local.
- **3A PASS UI local**: bằng chứng UI lịch sử tại QA_AUTH_UI; form nối API trong 3B, login UI máy thật kiểm tra tiếp ở 3C (không phải chạy lại toàn bộ suite 3A).
- **3B PASS local**: API/Repository/ViewModel, phiên mã hóa và refresh đồng bộ; bằng chứng lịch sử 20 unit/17 emulator test tại [QA_ANDROID_AUTH.md](QA_ANDROID_AUTH.md).
- **3C PASS**: launcher xác minh phiên, route Login/Camera, logout/retry và dọn back stack. Bằng chứng local trước đó và USB tại [QA_SESSION_ROUTING.md](QA_SESSION_ROUTING.md);6 auth regression máy thật tiếp tục PASS sau3D.
- **3D PASS**: CameraX camera sau, quyền camera, rotation/crop1:1, multipart owner-only, storage private và GET ảnh có JWT hiển thị bằng Glide. Maven46/46; Android20 unit, build/lint debug/release0 errors/3 warnings;4 ca media máy thật,1 lần lặp chụp đủ sáng và6 auth regression PASS. Xem [QA_SELF_MOMENT.md](QA_SELF_MOMENT.md).

Nghiệm thu USB mới3D: Android15/API35, không thay phiên cá nhân; chụp thật, dừng/mở HTTP connector test để thử offline/retry, byte hiển thị khớp GET server. Từ chối quyền camera có thông báo, A/B/JWT kiểm tra đúng; screenshot đủ sáng sau xác nhận người dùng đã xem. Cold signed-out1722ms về Login/no private media. Backend smoke cuối4 moment/4 file/0 recipient, exit0; reverse đã gỡ. Suite mặc định hiện31 test, chưa chạy toàn bộ31 hoặc toàn bộ UI AVD; không lấy các lượt harness fail làm PASS. Quyền CAMERA riêng của Taktak được kiểm tra denied/granted, cuối trả về granted; không đổi bảo mật USB/timeout hệ thống.

## Bằng chứng hiện tại

| Hạng mục | Quan sát thực tế | Kết luận được phép |
|---|---|---|
| Git | Thực tế4C:nhánh `chore/push-current-project`, HEAD `8766f26`, index rỗng/worktree dirty. Hồ sơ cũ ghi main không phản ánh nhánh hiện tại; reflog có checkout từ main trước commit này | Không tự đổi nhánh/commit/push; bảo toàn source và thay đổi có sẵn |
| Thay đổi3D | Bảo toàn dirty/untracked từ3B/3C; thêm camera/media/server storage và test | Không đổi README/migration/Firebase/dependency hoặc tự commit |
| Tài liệu 0A | Có AGENTS, README, `.gitignore`, PROJECT_DECISIONS; đã đọc toàn bộ đặc tả | Các quy ước đã hiện diện và kiểm tra lại được |
| Android | Java/XML/ViewBinding; com.taktak.app,min26; CameraX/JWT giữ nguyên, thêm FriendsFragment/Repository/ViewModel/Glide avatar local | 4C PASS UI/API;3D giữ bằng chứng lịch sử, không chạy lại chụp ảnh trong4C |
| Firebase | JSON local hợp lệ/được ignore; định danh khớp ảnh General trên Console; ảnh Cloud Messaging có V1 Enabled; xác nhận thủ công chưa thiết lập dịch vụ khác | 0B PASS đăng ký/cấu hình; chưa kiểm thử FCM runtime |
| Backend và deploy | Maven/Java17,V1 giữ8 bảng; auth/profile/media owner-only và friends/block;87/87 test/JAR chạy lại PASS trong4C, source server giữ nguyên | Friends UI4C đã có; chưa gửi ảnh bạn bè/avatar API/deploy |
| Kiểm thử ứng dụng | Bằng chứng cũ: server 6 test, Android unit mẫu 1 test, emulator alias/reverse 4 test và build/lint PASS. Phiên máy thật: 4/4 test PASS trên Android 15, host health 200, install/launch/UI PASS | Máy thật dùng USB reverse; không tính test cũ là vừa chạy lại hoặc kết luận LAN/tunnel PASS |

## Việc chưa nghiệm thu và phụ thuộc

- 0B đã nghiệm thu bằng ảnh/xác nhận người dùng theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md). API key restrictions và gửi/nhận push chưa kiểm thử; không trì hoãn bootstrap để triển khai FCM runtime ngoài phạm vi.
- 0C/1C: xem [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md). Nguyên nhân alias là target SDK 37 thiếu quyền runtime `ACCESS_LOCAL_NETWORK`. Khai báo riêng debug và cấp quyền bằng UiAutomation trong test harness đã giúp cả 4 test PASS. UI bootstrap chưa gọi mạng; khi thêm request local từ UI cần xin quyền runtime trước request. Release không chứa quyền này; không thay firewall/TLS/target SDK.
- Điện thoại 23129RAA4G / Android 15 API 35 đã được kiểm tra qua USB reverse. UI test đầu tiên chậm khoảng 104 giây nhưng PASS; lần cold launch riêng khoảng 3 giây. Chưa xác định nguyên nhân chậm ban đầu; không thay cài đặt hệ thống để che kết quả. LAN/tunnel chưa chạy, không phải blocker cho kịch bản USB đã chọn.
- Android đã nghiệm thu auth/routing/logout trên AVD và máy thật Android15. Phiên UI-CAMERA bổ sung kiểm tra dialog Camera Deny→Allow/API35, sáng/tối, busy và offline/retry. Deny quyền mạng local trong auth/API37, UI429 end-to-end, API26 và runtime HTTPS thật chưa chạy; không suy rộng kết quả ra các phạm vi đó.
- Đã thay activity-ktx bằng Activity; không thêm Kotlin source. Tập phiên bản build thực tế được ghi trong PROJECT_DECISIONS. FCM auto-init tắt tới bước 6; JSON giữ nguyên.
- Backend test dùng PostgreSQL 14.22 tạm; chưa tạo database dev bền vững. Không reset hoặc sửa migration đã áp dụng ở database của người dùng.

## Kiểm tra và điểm dừng

Kết quả kiểm tra thực tế và giới hạn nằm trong [SESSION_HANDOFF.md](SESSION_HANDOFF.md). Danh sách công việc nằm trong [TASKS.md](TASKS.md). Quyết định kiến trúc nằm trong [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md); đặc tả tại [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md).

Chi tiết mới tại [QA_FRIENDS.md](QA_FRIENDS.md); hợp đồng friends/media giữ trong hồ sơ API tương ứng. **4C PASS**, không còn blocker nghiệm thu tiểu bước này. A/USB↔B/AVD gửi/chấp nhận/chặn và C isolation đã đạt; không suy rộng sang recipient-aware media5C: hiện server vẫn chặn mọi recipient không rỗng. Hành động tiếp theo duy nhất: **duyệt5A — hoàn thiện điều hướng bốn tab và CameraX**; chưa triển khai. Smoke đã dừng exit0, reverse8080 đã gỡ, AVD QA đã tắt, điện thoại về Login. Muốn dùng app tiếp cần backend/dev DB/JWT và reverse phù hợp; tài khoản QA thuộc DB tạm. Avatar là drawable local qua Glide; chưa API avatar/UI unblock, runtime API26/release HTTPS/load test hoặc UI pagination hơn20. Không commit/push/deploy.
