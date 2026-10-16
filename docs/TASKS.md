# Taktak — Công việc và cổng xác nhận

Cập nhật: 16/10/2026. Nguồn đối chiếu: [đặc tả](TAKTAK_PROJECT.md), [trạng thái](PROJECT_STATUS.md), [bàn giao](SESSION_HANDOFF.md) và lịch sử yêu cầu trong cuộc trò chuyện.

Quyền hiện tại: AUTO CONTINUE cho toàn Bước 0 và giai đoạn 1, được làm phần độc lập khi blocker khác chưa giải quyết. Không cần xin lại từng tiểu bước. Chưa được phép giai đoạn 2; các cổng nghiệm thu vẫn giữ nguyên.

`PASS` chỉ áp dụng cho phạm vi đã có bằng chứng. `CHƯA DUYỆT` nghĩa là không được tự triển khai; `CHƯA XÁC MINH` không đồng nghĩa thất bại.

| Mã | Công việc | Trạng thái | Bằng chứng / điều kiện |
|---|---|---|---|
| 0A | Khóa định danh và quy ước repository | PASS — tài liệu/quy ước | Có đủ 4 file đầu ra; kiểm tra lại ignore, link, nội dung và định danh; xem bàn giao |
| RESUME-DOCS | Khôi phục ngữ cảnh và tạo hồ sơ trạng thái còn thiếu | PASS — hồ sơ | Kiểm tra 7 file, 19 link local, 3 file mới không bị ignore; SHA-256 xác nhận 62 file có sẵn giữ nguyên |
| 0B | Firebase Android `com.taktak.app`, chỉ FCM | ĐÃ DUYỆT / BLOCKED — Console | “continue” đã duyệt 0B; 10 kiểm tra local PASS; chờ kết nối tài khoản hoặc xác nhận đối chiếu Console theo FIREBASE_SETUP |
| 0C | Kịch bản kết nối emulator và máy thật | ĐÃ DUYỆT / IN PROGRESS | Reverse emulator PASS; 10.0.2.2 FAIL timeout; máy thật/tunnel chưa có |
| 1A | Server Java 17 / Spring Boot 3 / Maven tối thiểu | PASS local | JAR build; 6 test PostgreSQL/Flyway/HTTP/constraints, không skipped; độc lập Firebase |
| 1B | Android Java/XML tối thiểu | ĐÃ TRIỂN KHAI / PASS local, chờ nghiệm thu 0B | ViewBinding/dependency/config mạng; APK debug/release và UI emulator đạt; Console chưa xác minh |
| 1C | Build, database/migration, health và kết nối thiết bị | ĐÃ DUYỆT / IN PROGRESS | Backend/migration test PASS, 4 instrumentation test qua reverse PASS; chưa máy thật, alias còn timeout |
| 2–8 | Các tiểu bước MVP tiếp theo | CHƯA DUYỆT / CHƯA NGHIỆM THU | Không suy đoán hoàn thành; chỉ chọn từng tiểu bước khi người dùng yêu cầu |

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
- [ ] Đối chiếu project ID/project number và Android App ID trên Console với JSON local; package chính xác `com.taktak.app`.
- [ ] Xác minh JSON có nguồn từ app/project đó; các sản phẩm Auth/Firestore/Storage/Functions không được triển khai cho Taktak; không tạo Admin credentials.
- [ ] Có bằng chứng Console qua kết nối tài khoản hoặc xác nhận đối chiếu của người dùng, ghi nguồn bằng chứng rồi đánh giá 0B.

Không tạo lại project/app khi chưa đối chiếu tài nguyên đã có; không gửi nguyên JSON hoặc key vào báo cáo. Khi 0B đạt, tiếp tục các việc còn lại của 0–1 theo quyền AUTO CONTINUE; không xin lại phê duyệt từng tiểu bước.

## Bàn giao AUTO CONTINUE

- [x] Chuẩn bị JDK 17/Maven portable có checksum; helper chỉ chọn Java trong process.
- [x] Tạo server, migration V1, security mặc định đóng, env mẫu, Wrapper và 6 integration test với PostgreSQL thật.
- [x] Build executable JAR; test startup/schema/constraints/HTTP.
- [x] Tận dụng Android skeleton; ViewBinding, Java dependencies, URL/network policy theo variant, FCM auto-init tắt.
- [x] Build/lint debug và release; xác nhận ba cấu hình mạng không hợp lệ bị từ chối.
- [x] Chạy 4 instrumentation test qua reverse và xem screenshot/UI dump theo skill kiểm tra Android.
- [x] Thử lại đường alias; lưu FAIL timeout riêng, không che lỗi hoặc sửa package.
- [ ] Có bằng chứng Firebase Console để nghiệm thu 0B và liên kết Firebase của 1B.
- [ ] Kiểm tra máy thật; giải quyết/xác định rõ vấn đề alias hoặc xác nhận kịch bản mạng được chấp nhận cho môi trường thật.
- [ ] Đạt đủ điều kiện Bước 0/giai đoạn 1. Sau đó dừng trước giai đoạn 2.

Lệnh, báo cáo, cảnh báo và phạm vi kiểm thử: [QA_BOOTSTRAP.md](QA_BOOTSTRAP.md), [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md).
