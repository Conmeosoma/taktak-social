# Quy tắc làm việc trong Taktak

## Tài liệu và phạm vi

- Đọc toàn bộ `docs/TAKTAK_PROJECT.md` trước khi thay đổi file. Nếu thiếu tài liệu, dừng và yêu cầu người dùng cung cấp; không suy đoán phần thiếu.
- Đối chiếu các quyết định đã khóa trong `docs/PROJECT_DECISIONS.md`. Yêu cầu hiện tại của người dùng quyết định phạm vi; nếu có mâu thuẫn kỹ thuật chưa được giải quyết, nêu rõ trước khi triển khai.
- Mỗi lần chỉ thực hiện một tiểu bước roadmap được yêu cầu. Không tự chuyển bước; hoàn tất báo cáo rồi dừng chờ xác nhận.
- Ngoại lệ đã được người dùng duyệt trong AUTO CONTINUE ngày 16/10/2026: được tự tiếp tục giữa các tiểu bước trong Bước 0 và giai đoạn 1, kiểm tra đạt rồi mới nghiệm thu. Khi một phần bị chặn, được làm phần độc lập không phụ thuộc blocker; không đánh dấu vượt qua điều kiện chưa đạt. Dừng trước giai đoạn 2; quyền này không cho phép commit/push/merge/deploy hoặc thao tác dữ liệu phá hủy.
- Đọc code và `git status` trước khi sửa. Bảo toàn các thay đổi có sẵn, kể cả file untracked; không xóa, di chuyển hoặc ghi đè nội dung chưa kiểm tra.
- Trực tiếp thực hiện và xác minh công việc trong repository khi được yêu cầu triển khai. Giữ code đơn giản, dễ bảo trì; không thêm chức năng ngoài MVP.
- Không tự động commit, push hoặc deploy. Các mốc commit trong tài liệu là hướng dẫn cho khi người dùng yêu cầu, không phải quyền tự thực hiện.

## Định danh, cấu trúc và đặt tên

- Một repository `taktak` chứa hai project build độc lập: `android-client/` và `server/`; `docs/` chứa tài liệu, `deploy/` chứa cấu hình triển khai khi đến bước tương ứng.
- Android: package gốc, namespace và applicationId đều là `com.taktak.app`. Backend: package gốc `com.taktak.server`. Package con dùng chữ thường theo chức năng.
- Tên class, method, biến và định danh code bằng tiếng Anh. Giải thích, báo cáo và comment ngắn gọn bằng tiếng Việt.
- Nhánh chính hiện tại: `main`. Nhánh công việc dùng `feat/<short-kebab-case>`, `fix/<short-kebab-case>`, `docs/<short-kebab-case>` hoặc `chore/<short-kebab-case>`. Không tự tạo/chuyển nhánh ngoài phạm vi công việc.
- Không tùy tiện đổi package/applicationId; sau khi đăng ký Firebase, thay đổi định danh phải được người dùng chấp thuận và đồng bộ cấu hình liên quan.

## Công nghệ cố định

- Android chỉ viết Java; không Kotlin, Compose hoặc Gradle Kotlin DSL. Dùng XML, ViewBinding, Material Components, minSdk 26; MVVM + Repository, ViewModel và LiveData.
- Android dùng Gradle Groovy; Retrofit, OkHttp, Gson, Glide; CameraX; WorkManager; AppWidgetProvider/RemoteViews; EncryptedSharedPreferences để lưu phiên.
- Backend dùng Java 17, Spring Boot 3, Maven, Spring Security, JPA, Validation, PostgreSQL và Flyway. Schema thay đổi qua migration; không dùng `ddl-auto=update` trên production.
- Firebase chỉ dùng Cloud Messaging: Messaging SDK ở Android và Admin SDK ở server để gửi FCM. Không Firebase Auth, Firestore, Firebase Storage, Cloud Functions; không chatbot AI/Gemini.
- Khóa phiên bản thư viện và bộ công cụ sau khi kiểm tra tương thích ở giai đoạn 1; không tự nâng cấp hoặc thay stack trong tiểu bước khác.

## Bảo mật và nghiệp vụ

- Không hard-code hoặc ghi vào Git/log/báo cáo mật khẩu, JWT secret, token, API key hay private key. Cấu hình bí mật qua môi trường/file local; file mẫu chỉ chứa placeholder.
- Không commit `.env`, service-account JSON, `google-services.json`, keystore hoặc signing credentials. JSON Android là cấu hình FCM, khác với Admin private credentials; cả hai được giữ local theo chính sách repo.
- Auth dùng BCrypt, access JWT ngắn hạn và refresh token xoay vòng; chỉ lưu hash refresh token ở server. Refresh phải hoạt động khi access JWT hết hạn.
- Ảnh lưu private trên VPS, ngoài web root; mỗi lần tải metadata/ảnh phải kiểm tra quyền sender/recipient ở server. Không URL public; chỉ sender được xóa và yêu cầu ảnh mới phải bị từ chối sau khi xóa.
- Server kiểm tra toàn bộ người nhận, quan hệ bạn bè và block hai chiều. `recipientIds=[]` là ảnh chỉ mình tôi, chỉ sender xem và không gửi FCM.
- FCM chỉ báo thay đổi, không chứa ảnh, caption, image URL, token xác thực; Worker vẫn phải xác thực khi tải ảnh. `AppWidgetProvider` enqueue WorkManager, không gọi mạng trực tiếp.
- HTTP chỉ được phép cho debug local có kiểm soát trên mạng tin cậy, bằng cấu hình riêng debug; ưu tiên tunnel HTTPS. Release bắt buộc HTTPS và chặn cleartext. Không đưa địa chỉ HTTP local vào release.
- Giữ bốn tab Camera / Lịch sử / Bạn bè / Hồ sơ; caption tối đa 50 ký tự Unicode; ảnh vuông, nén mục tiêu 500–800 KB; chỉ lưu vào gallery khi người dùng chọn.

## Kiểm tra và bàn giao

- Chạy kiểm tra phù hợp phạm vi; sửa nguyên nhân khi lỗi, không bỏ qua kiểm thử. Với thay đổi code, xác minh build và hành vi liên quan khi môi trường cho phép.
- Chỉ ghi PASS cho kiểm tra đã chạy và có kết quả; nêu rõ kiểm tra chưa chạy và lý do. Thay đổi chỉ tài liệu/ignore không đồng nghĩa ứng dụng đã build được.
- Không bỏ qua 3D (chụp → upload ảnh chỉ mình tôi → tải lại bằng JWT) sau 3C. Hoàn tất 5E (Đã nhận) trước FCM/widget.
- Báo cáo bằng tiếng Việt: file thay đổi, quyết định, kiểm tra thực tế, hạn chế, thao tác thủ công, trạng thái PASS/FAIL/BLOCKED và đề xuất tiểu bước tiếp theo. Dừng chờ xác nhận, không triển khai bước tiếp theo.
