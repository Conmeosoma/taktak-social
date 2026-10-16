# Taktak — Trạng thái dự án

Cập nhật: 16/10/2026 (Asia/Saigon). Phạm vi khôi phục: workspace local, lịch sử Git local và yêu cầu trong cuộc trò chuyện; chưa xác minh trạng thái GitHub/Firebase Console.

## Trạng thái và quyền thực hiện

- Phạm vi được duyệt mới nhất: **AUTO CONTINUE toàn Bước 0 và giai đoạn 1**, không cần xác nhận từng tiểu bước; được làm phần độc lập khi blocker khác chưa giải quyết. Quyền này thay yêu cầu chờ cũ trong phạm vi 0–1; không mở quyền giai đoạn 2 hoặc commit/push/merge/deploy.
- **0A: PASS trong phạm vi tài liệu/quy ước**, đã kiểm tra lại trong phiên khôi phục. Không đồng nghĩa toàn bộ Bước 0 hoặc ứng dụng đã PASS.
- Công việc khôi phục hồ sơ trước đó: **PASS**, có kết quả trong bàn giao lịch sử; không tính các phép kiểm tra cũ là vừa chạy lại.
- **0B: BLOCKED ở phần nghiệm thu Console**; 10 kiểm tra local đã PASS, chưa có quyền truy cập tài khoản để đối chiếu đăng ký app/dịch vụ. Firebase/GCloud CLI không có trong PATH; plugin Firebase được tìm thấy nhưng chưa cài/kết nối. Đã đề xuất tích hợp; chưa giả định kết nối thành công.
- **1A PASS local**: server Java 17/Spring Boot/Maven, Flyway 8 bảng, 6 test PostgreSQL thật PASS và JAR build được. Đây là phần độc lập Firebase.
- **1B đã triển khai và kiểm thử local**, debug/release build, lint và UI emulator đạt; nghiệm thu liên kết Firebase vẫn chờ 0B. **0C/1C IN PROGRESS**: reverse chạy được, alias 10.0.2.2 timeout và chưa có điện thoại thật. Không đánh dấu toàn Bước 0/giai đoạn 1 DONE.

## Bằng chứng hiện tại

| Hạng mục | Quan sát thực tế | Kết luận được phép |
|---|---|---|
| Git | Nhánh `main`, HEAD `99b6db4` (`first commit`); lịch sử local chỉ có một commit | Không suy ra đã bootstrap hoặc nghiệm thu từ commit |
| Thay đổi chưa commit | README đã sửa; tài liệu 0A và Android source còn untracked; không có thay đổi staged khi kiểm tra | Giữ nguyên; không tự add/commit/push |
| Tài liệu 0A | Có AGENTS, README, `.gitignore`, PROJECT_DECISIONS; đã đọc toàn bộ đặc tả | Các quy ước đã hiện diện và kiểm tra lại được |
| Android | Java/XML + ViewBinding; package/namespace/applicationId `com.taktak.app`, minSdk 26; placeholder Taktak mở trên emulator | Build debug/release, lint và UI PASS local; chưa có nghiệp vụ |
| Firebase local | JSON parse được, version 1, metadata có đủ; đúng một client `com.taktak.app`; Firebase App ID khớp project number; có client API key; không có trường Admin private key; file bị ignore và chưa tracked | 10 kiểm tra local PASS, bao gồm Gradle/dependency/Git; chưa chứng minh app còn tồn tại trên Console hoặc FCM hoạt động |
| Backend và deploy | Có Maven Wrapper, Spring Boot 3.5.16, Java 17, migration V1 8 bảng và health; deploy vẫn rỗng | 1A PASS với PostgreSQL test riêng; không có auth hoặc server deploy |
| Kiểm thử ứng dụng | Server 6 test PASS; Android unit mẫu 1 PASS; 4 instrumentation test PASS qua ADB reverse; UI screenshot đã xem | Đường trực tiếp 10.0.2.2 FAIL timeout, máy thật chưa kiểm tra; xem QA |

## Việc chưa nghiệm thu và phụ thuộc

- 0B đã được duyệt. Phần còn thiếu là **kết nối tài khoản Firebase hoặc người dùng đối chiếu Console theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md)**, xác nhận project/app khớp JSON và không sử dụng Auth/Firestore/Storage/Functions. Chưa xác minh API key có còn hiệu lực hoặc restrictions đúng; JSON local không cung cấp bằng chứng này.
- 0C/1C: xem [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md). Reverse đã chạy trên emulator; alias 10.0.2.2 vẫn timeout dù đổi NIC. Chưa xác định root cause host/AVD; không thay firewall hay làm yếu TLS để chữa lỗi.
- Chưa có điện thoại Android thật hoặc URL tunnel HTTPS để nghiệm thu máy thật. Cần kết nối/cấp quyền thiết bị; không coi emulator thay cho máy thật.
- Android đã có ViewBinding, URL theo build type, debug allowlist và release chặn cleartext. Chưa có MVVM/Repository vì chưa triển khai nghiệp vụ, không tạo class rỗng để giả hoàn thành.
- Đã thay activity-ktx bằng Activity; không thêm Kotlin source. Tập phiên bản build thực tế được ghi trong PROJECT_DECISIONS. FCM auto-init tắt tới bước 6; JSON giữ nguyên.
- Backend test dùng PostgreSQL 14.22 tạm; chưa tạo database dev bền vững. Không reset hoặc sửa migration đã áp dụng ở database của người dùng.

## Kiểm tra và điểm dừng

Kết quả kiểm tra thực tế và giới hạn nằm trong [SESSION_HANDOFF.md](SESSION_HANDOFF.md). Danh sách công việc nằm trong [TASKS.md](TASKS.md). Quyết định kiến trúc nằm trong [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md); đặc tả tại [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md).

Chi tiết tại [QA_BOOTSTRAP.md](QA_BOOTSTRAP.md). Dừng khi phần còn lại cần tài khoản Firebase/thiết bị/mạng ngoài quyền truy cập hiện tại. Khi có bằng chứng và môi trường cần thiết, tiếp tục trong phạm vi 0–1 đã duyệt mà không xin phép lại; không tự chuyển sang giai đoạn 2 hoặc commit/push/deploy.
