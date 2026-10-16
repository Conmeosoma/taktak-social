# Taktak — 0B: Firebase Android chỉ dùng FCM

Cập nhật: 16/10/2026. **0B PASS — đăng ký/cấu hình Firebase**, dựa trên kiểm tra local, ảnh Console do người dùng cung cấp và xác nhận thủ công về phạm vi dịch vụ. Chưa kiểm thử gửi/nhận FCM.

Quyền mới nhất AUTO CONTINUE đã mở toàn Bước 0 và giai đoạn 1, bao gồm phần độc lập khi 0B bị chặn. Các kết quả local 0B dưới đây là của phiên 0B trước; phần build/runtime giai đoạn 1 được ghi riêng trong QA_BOOTSTRAP. JSON local được giữ nguyên.

## Phạm vi

Khóa đăng ký Firebase Android đúng package `com.taktak.app`, xác minh cấu hình và cách giữ file local. Không tích hợp SDK mới, sửa Gradle/Manifest, tạo Admin credentials, lấy token hoặc gửi push trong 0B. Các phần code/build thuộc bước Android và FCM tương ứng trong [đặc tả](TAKTAK_PROJECT.md).

## Cấu hình có sẵn và kiểm tra thực tế

File local: `android-client/app/google-services.json`. File đã tồn tại trước khi duyệt 0B và được giữ nguyên; không sao chép API key, project identifier hoặc JSON vào tài liệu.

| Kiểm tra đã chạy | Kết quả |
|---|---|
| Parse bằng PowerShell `ConvertFrom-Json` | PASS |
| Không có trường Admin `private_key`/`private_key_id` hoặc `type=service_account` | PASS |
| Configuration version 1; có project ID và project number dạng số | PASS |
| Đúng một client khớp package `com.taktak.app`, phân biệt hoa/thường | PASS |
| Firebase App ID có định dạng Android, khớp project number | PASS |
| Client có API key không rỗng, không in giá trị | PASS local; chưa kiểm tra hiệu lực/restrictions |
| Package JSON khớp namespace/applicationId trong `app/build.gradle` | PASS |
| `git check-ignore --no-index -q -- android-client/app/google-services.json` | PASS: file bị ignore |
| `git ls-files` không chứa JSON Android/Admin credential filenames hoặc `.env` tương ứng | PASS: chưa tracked; không phải quét toàn bộ lịch sử |
| Quét Firebase artifact khai báo trực tiếp trong Gradle/TOML/POM | PASS: chỉ BoM và Messaging; không kiểm tra dependency transitive |

Không suy ra project/app còn tồn tại trên Firebase Console từ JSON local. Trường `storage_bucket`, OAuth client hoặc API allowlist trong cấu hình cũng không tự chứng minh Storage/Auth đã được sử dụng. Không xóa các trường hoặc đổi API key chỉ vì chúng hiện diện.

## Đối chiếu trên Firebase Console

Có thể thực hiện qua plugin Firebase chính thức sau khi người dùng cài/kết nối, hoặc đối chiếu thủ công dưới đây. Môi trường hiện không có Firebase/GCloud CLI trong PATH và chưa có kết nối Firebase được xác nhận; chưa có kiểm tra tài khoản từ xa.

1. Mở [Firebase Console](https://console.firebase.google.com/), đăng nhập tài khoản sở hữu project. Tìm project có ID trùng `project_info.project_id` trong JSON local; đối chiếu cả project number. Không tạo project mới khi chưa kiểm tra project hiện có.
2. Mở Project settings → General → Your apps. Chọn Android app có package chính xác `com.taktak.app`; đối chiếu Firebase App ID với `client_info.mobilesdk_app_id` trong client tương ứng của JSON local. Chỉ tạo app mới nếu xác nhận chưa có app đúng package và tài nguyên đích đã rõ.
3. Xác minh JSON được tải từ app/project vừa đối chiếu. Nếu cần tải lại, lưu bản tải vào vị trí local khác trước, đối chiếu định danh và kiểm tra khác biệt; không ghi đè bản đang có mà chưa kiểm tra. Không gửi nguyên JSON hoặc giá trị key vào chat.
4. Xác nhận Taktak chỉ dùng Cloud Messaging: không thiết lập Firebase Authentication/providers, Firestore database, Cloud Storage bucket hay triển khai Cloud Functions cho app. Không bật Analytics hoặc AI/Gemini cho phạm vi này. Không tự xóa tài nguyên đã tồn tại nếu phát hiện khác biệt; báo cáo để quyết định xử lý.
5. Có thể xem Cloud Messaging trong Project settings để ghi nhận trạng thái cấu hình hiện tại. Chưa tạo private key/service account hoặc gửi push thử; phần gửi FCM từ server thuộc giai đoạn 6. Đừng dùng API key client làm credential gửi FCM.
6. Theo [hướng dẫn API key Firebase](https://firebase.google.com/docs/projects/api-keys), xem API restrictions nếu có quyền; không mở quyền rộng để chữa lỗi, không thêm API ngoài Firebase như Gemini. Không tự thay restrictions hiện có khi chưa xác định API cần thiết; cấu hình sai có thể làm client không hoạt động.

Package Firebase phân biệt hoa/thường và không đổi được cho app đã đăng ký. File cấu hình nằm tại app-module root khi tích hợp Android. Google Analytics là tùy chọn trong quy trình tạo project, không phải điều kiện để đăng ký app ở 0B. Các điểm này dựa trên [hướng dẫn Android chính thức](https://firebase.google.com/docs/android/setup); không làm theo ví dụ thêm Auth/Firestore trong trang đó vì ngoài stack Taktak.

## Bằng chứng nghiệm thu

- [x] Ảnh Project settings → General cho thấy project/app Taktak. Đã đối chiếu Project ID/project number với JSON local bằng PowerShell: đều khớp.
- [x] Package `com.taktak.app` và Firebase App ID trong ảnh khớp đúng một Android client trong JSON local. Ảnh workspace xác nhận file ở `android-client/app/google-services.json`; đây là đối chiếu định danh, không phải audit lịch sử tải file.
- [x] Ảnh Cloud Messaging cho thấy API V1 Enabled, Legacy Disabled. Người dùng xác nhận “tôi chưa thiết lập” sau hướng dẫn kiểm tra Auth/Firestore/Storage/Functions; ghi nhận là xác nhận thủ công rằng các dịch vụ này chưa được thiết lập, không phải truy vấn tài khoản tự động.

Nguồn bằng chứng là ảnh và xác nhận của người dùng trong cuộc trò chuyện ngày 16/10/2026 cùng phép đối chiếu JSON local đã chạy. Agent chưa có phiên đăng nhập Firebase, chưa tự truy vấn API quản trị hoặc kiểm tra hiệu lực/restrictions của API key. Những giới hạn này không được coi là kiểm thử FCM runtime; phần gửi/nhận thuộc giai đoạn 6.

## Chính sách file và giới hạn

`google-services.json` chứa cấu hình định danh client, API key Firebase không phải Admin private key. Firebase không coi các định danh này là secret; Taktak vẫn giữ file local và Git ignore theo [quyết định repository](PROJECT_DECISIONS.md). Firebase Admin service-account private key phải giữ bí mật phía server, chỉ chuẩn bị ở bước 6.

FCM runtime, FCM token, worker/widget và trạng thái API key từ xa chưa được kiểm thử trong 0B. Build Android được kiểm tra riêng ở giai đoạn 1, không thay bằng chứng Console. Không coi phần local PASS là toàn bộ 0B PASS. Khi có bằng chứng, tiếp tục phần còn lại của 0–1 theo quyền AUTO CONTINUE, không xin lại xác nhận từng tiểu bước.

Tham chiếu chính thức: [Android config/plugin](https://firebase.google.com/docs/android/google-services-plugin-and-file), [FCM Android](https://firebase.google.com/docs/cloud-messaging/android/get-started), [API keys](https://firebase.google.com/docs/projects/api-keys).
