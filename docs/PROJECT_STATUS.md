# Taktak — Trạng thái dự án

Cập nhật: 16/10/2026 (Asia/Saigon). Nguồn: workspace/Git, bằng chứng kiểm thử, ảnh Firebase Console và xác nhận thủ công của người dùng.

## Trạng thái và quyền thực hiện

- Phạm vi mới nhất: người dùng duyệt riêng **3A — Java/XML**, yêu cầu skill android_ui_verification. Không tự mở quyền 3B trở đi hoặc commit/push/merge/deploy; quyền AUTO CONTINUE cũ chỉ áp dụng 0–1.
- **0A: PASS trong phạm vi tài liệu/quy ước**, đã kiểm tra lại trong phiên khôi phục. Không đồng nghĩa toàn bộ Bước 0 hoặc ứng dụng đã PASS.
- Công việc khôi phục hồ sơ trước đó: **PASS**, có kết quả trong bàn giao lịch sử; không tính các phép kiểm tra cũ là vừa chạy lại.
- **0B: PASS — đăng ký/cấu hình**. Ảnh Console khớp Project ID/project number/App ID/package với JSON local, API FCM V1 Enabled. Người dùng xác nhận chưa thiết lập Auth/Firestore/Storage/Functions; đây là bằng chứng thủ công, không phải truy vấn tự động hay kiểm thử FCM runtime.
- **1A PASS local**: server Java 17/Spring Boot/Maven, Flyway 8 bảng, 6 test PostgreSQL thật PASS và JAR build được. Đây là phần độc lập Firebase.
- **1B PASS bootstrap**, có bằng chứng build/lint/UI và cấu hình Firebase khớp đăng ký. **0C/1C PASS local**: alias emulator 4/4 test PASS và máy thật Android 15 qua USB reverse 4/4 test PASS; đã cài/mở và xem UI. **Bước 0 và giai đoạn 1 hoàn tất trong phạm vi bootstrap local**. LAN/tunnel, deploy, signing và FCM runtime chưa kiểm thử.
- **2A PASS**: JPA User/Repository, POST `/api/auth/register`, validation/normalization, BCrypt cost 12, transaction và unique DB; 13 test regression tiếp tục PASS trong suite 2B.
- **2B PASS**: login, JWT HS256 mặc định 15 phút, refresh opaque/hash/rotation, logout và login rate limit IP/account. 29 test cũ tiếp tục PASS trong suite 2C; HTTP smoke ngoài test profile có bằng chứng.
- **2C PASS**: GET/PUT `/api/users/me`, danh tính từ JWT, DTO an toàn, chỉ sửa displayName trong transaction. Mốc 2C có 36/36 test và HTTP smoke ngoài test profile; các test tiếp tục nằm trong suite 2D.
- **2D PASS local**: integration/regression và curl thật cho auth/profile, expiry/forgery/duplicate/replay/logout; JSON lỗi đã chốt. Mốc 2D: 39/39 test/JAR và CurlOnly 1/1 PASS, không tính là vừa chạy lại trong 3A. Giai đoạn 2 hoàn tất backend local.
- **3A PASS UI local**: LoginActivity/RegisterActivity Java/XML/ViewBinding/Material, validation và loading hook, điều hướng từ launcher; không giả auth. Debug/release build/lint PASS, 8 unit + 12 emulator instrumentation PASS; đã xem screenshot light/dark/validation/IME theo skill và sửa lỗi keyboard che field. Chưa nối API/token hoặc máy thật cho UI mới; dừng trước 3B.

## Bằng chứng hiện tại

| Hạng mục | Quan sát thực tế | Kết luận được phép |
|---|---|---|
| Git | Nhánh `main`, HEAD `96d2311`; có commit bootstrap `a4a2bf5` và README `96d2311` từ yêu cầu commit/push trước | Đối chiếu lịch sử; không tự commit/push trong phiên AUTO CONTINUE này |
| Thay đổi chưa commit | Đầu phiên 3A đã có thay đổi bootstrap/2A–2D và file untracked; đã đọc trước khi bổ sung UI | Không ghi đè thay đổi cũ, chưa staged; README/server không sửa trong 3A |
| Tài liệu 0A | Có AGENTS, README, `.gitignore`, PROJECT_DECISIONS; đã đọc toàn bộ đặc tả | Các quy ước đã hiện diện và kiểm tra lại được |
| Android | Java/XML/ViewBinding; định danh com.taktak.app, min26; launcher mở form login/register, validation/loading UI | Build/lint debug/release, 8 unit/12 emulator test và screenshot 3A PASS; chưa API/phiên Android |
| Firebase | JSON local hợp lệ/được ignore; định danh khớp ảnh General trên Console; ảnh Cloud Messaging có V1 Enabled; xác nhận thủ công chưa thiết lập dịch vụ khác | 0B PASS đăng ký/cấu hình; chưa kiểm thử FCM runtime |
| Backend và deploy | Maven/Java 17, Flyway V1 8 bảng, register/login/refresh/logout và GET/PUT me; 39 test/curl PASS | 2A–2D PASS local; V1 giữ nguyên; chưa avatar/deploy |
| Kiểm thử ứng dụng | Bằng chứng cũ: server 6 test, Android unit mẫu 1 test, emulator alias/reverse 4 test và build/lint PASS. Phiên máy thật: 4/4 test PASS trên Android 15, host health 200, install/launch/UI PASS | Máy thật dùng USB reverse; không tính test cũ là vừa chạy lại hoặc kết luận LAN/tunnel PASS |

## Việc chưa nghiệm thu và phụ thuộc

- 0B đã nghiệm thu bằng ảnh/xác nhận người dùng theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md). API key restrictions và gửi/nhận push chưa kiểm thử; không trì hoãn bootstrap để triển khai FCM runtime ngoài phạm vi.
- 0C/1C: xem [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md). Nguyên nhân alias là target SDK 37 thiếu quyền runtime `ACCESS_LOCAL_NETWORK`. Khai báo riêng debug và cấp quyền bằng UiAutomation trong test harness đã giúp cả 4 test PASS. UI bootstrap chưa gọi mạng; khi thêm request local từ UI cần xin quyền runtime trước request. Release không chứa quyền này; không thay firewall/TLS/target SDK.
- Điện thoại 23129RAA4G / Android 15 API 35 đã được kiểm tra qua USB reverse. UI test đầu tiên chậm khoảng 104 giây nhưng PASS; lần cold launch riêng khoảng 3 giây. Chưa xác định nguyên nhân chậm ban đầu; không thay cài đặt hệ thống để che kết quả. LAN/tunnel chưa chạy, không phải blocker cho kịch bản USB đã chọn.
- Android đã có ViewBinding, URL theo build type, debug allowlist và release chặn cleartext. Form auth 3A có validation và loading renderer; AuthApi/Repository/ViewModel/token thuộc 3B, không tạo class rỗng hoặc request giả để báo hoàn thành.
- Đã thay activity-ktx bằng Activity; không thêm Kotlin source. Tập phiên bản build thực tế được ghi trong PROJECT_DECISIONS. FCM auto-init tắt tới bước 6; JSON giữ nguyên.
- Backend test dùng PostgreSQL 14.22 tạm; chưa tạo database dev bền vững. Không reset hoặc sửa migration đã áp dụng ở database của người dùng.

## Kiểm tra và điểm dừng

Kết quả kiểm tra thực tế và giới hạn nằm trong [SESSION_HANDOFF.md](SESSION_HANDOFF.md). Danh sách công việc nằm trong [TASKS.md](TASKS.md). Quyết định kiến trúc nằm trong [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md); đặc tả tại [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md).

Chi tiết backend tại [QA_AUTH.md](QA_AUTH.md), [API_ERRORS.md](API_ERRORS.md), [AUTH_API.md](AUTH_API.md), [PROFILE_API.md](PROFILE_API.md); UI mới tại [QA_AUTH_UI.md](QA_AUTH_UI.md). 3A không có blocker, không cần thao tác tài khoản/secret để nghiệm thu UI. Server dev bền vững vẫn cần DB/JWT env; rate limit single JVM và logout chỉ thu hồi refresh hiện tại như 2B. Đề xuất duy nhất: **3B — nối AuthApi/Repository/ViewModel, lưu phiên mã hóa và refresh đồng bộ**; chờ duyệt. Không tự commit/push/deploy.
