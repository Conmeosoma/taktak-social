# API ảnh riêng tư — lát cắt 3D

Ngày 16/10/2026. Chỉ triển khai ảnh **chỉ mình tôi**; không phải API gửi bạn bè hoàn chỉnh. Giữ Java17/Spring Boot3/PostgreSQL, schema V1 hiện có; không sửa migration đã áp dụng.

## Upload

`POST /api/moments`, `Authorization: Bearer <access-token>`, `multipart/form-data`:

| Field | Quy tắc |
|---|---|
| image | Bắt buộc; JPEG thực tế, tối đa 5 MiB; không tin filename/extension/MIME khai báo |
| caption | Không bắt buộc, mặc định rỗng; tối đa 50 Unicode code point, không NUL |
| recipientIds | Bỏ field (danh sách rỗng) hoặc đúng một field literal `[]`; mọi danh sách không rỗng/sai định dạng bị từ chối |

Thành công: **201**, `Cache-Control: no-store`, JSON chỉ có `{"id":"<uuid>"}`. Sender lấy từ JWT đã xác minh và phải tồn tại trong DB; không nhận senderId từ client. Không tạo moment_recipients, không gửi FCM.

Server kiểm tra JPEG signature và decode thực tế; từ chối chiều lớn hơn4096 hoặc hơn16 triệu pixel trước khi đọc bitmap. Re-encode JPEG không chuyển metadata EXIF/GPS hoặc dữ liệu nối thêm sang ảnh lưu. Tên file UUID do server sinh, không dùng tên/path client. PNG và định dạng khác chưa được hỗ trợ trong 3D.

File được ghi đầy đủ trước saveAndFlush trong transaction; callback rollback xóa file nếu DB không commit. IO thất bại trước lưu metadata không tạo bản ghi moment. File pending được dọn trong finally. Cleanup file thất bại được log thông báo kỹ thuật không chứa đường dẫn/credential; process/server bị tắt đột ngột không được coi là đã có crash-recovery/reconciliation hoàn chỉnh.

## Tải ảnh

`GET /api/moments/{id}/image`, bắt buộc JWT. Mỗi request kiểm tra user, sender và deleted_at trong DB rồi mới đọc file private. Thành công **200 image/jpeg**, no-store, nosniff và filename cố định không chứa path. Người khác, ID không tồn tại, ảnh đã đánh dấu xóa hoặc file thiếu đều **404**; không mở URL file public/static. Chưa thêm endpoint DELETE hoặc lịch sử trong 3D.

Lỗi dùng envelope timestamp/status/code/message/path hiện có:

| HTTP | Code chính |
|---|---|
| 400 | VALIDATION_ERROR, IMAGE_REQUIRED, INVALID_CAPTION, RECIPIENTS_NOT_SUPPORTED |
| 401 | UNAUTHORIZED — JWT thiếu/sai/hết hạn hoặc user không tồn tại |
| 404 | MOMENT_NOT_FOUND |
| 413 | IMAGE_TOO_LARGE — file hơn5 MiB hoặc multipart hơn6 MiB |
| 415 | INVALID_IMAGE / UNSUPPORTED_MEDIA_TYPE |
| 500 | INTERNAL_ERROR — IO/DB lỗi, không phản chiếu nội dung nội bộ |

## Android và vận hành local

CameraFragment dùng CameraX camera sau, PreviewView và ImageCapture gắn view lifecycle. Quyền CAMERA xin khi người dùng bấm; từ chối có thông báo và nút chụp bị vô hiệu. Chỉ view preview giữ màn hình sáng; không đổi timeout toàn điện thoại. Tham chiếu API: [CameraX image capture](https://developer.android.com/media/camera/camerax/take-photo).

ImageProxy → bitmap → áp rotation → crop giữa1:1 → cạnh tối đa1280 → JPEG điều chỉnh quality tới tối đa800 KiB. Không cố làm ảnh đơn giản lớn lên để đạt500 KiB. 3D dùng buffer riêng trong bộ nhớ thay vì xuất file gallery/cache raw; đóng ImageProxy và recycle bitmap. Flash/camera trước/caption UI/preview chọn bạn và nén hoàn chỉnh tiếp ở giai đoạn5.

MomentRepository dùng chính ApiClient/SessionManager đang đăng nhập: multipart và GET đều có bearer/refresh đồng bộ. Repository gắn generation ngay khi bắt đầu chụp, từ chối thao tác cũ sau đổi phiên. MomentViewModel chạy xử lý/mạng trên worker, LiveData không có token. Chỉ byte nhận từ GET được đưa vào Glide; tắt disk/memory cache của Glide cho ảnh này. Không dùng ảnh chụp local để giả kết quả tải server.

Nếu đã biết id mà GET lỗi, retry chỉ GET lại. Nếu POST mất phản hồi trước khi biết id, gửi lại có thể tạo thêm một moment hoàn chỉnh; UI nêu rõ giới hạn này. **Chưa có exactly-once/idempotency key**; phần này thuộc5D, không tuyên bố chống gửi trùng đã hoàn thành. Request không tự retry POST ở tầng transport.

Storage root: `IMAGE_STORAGE_ROOT` (mặc định server/.local/private-images khi chạy từ server). VPS phải đặt ngoài web root, chỉ user dịch vụ có quyền truy cập. Linux/POSIX: directory0700/file0600; Windows dùng ACL kế thừa của thư mục local, cần cấu hình ACL phù hợp khi vận hành. Không map storage bằng Nginx/static handler. Chưa deploy hoặc chuẩn bị DB dev bền vững.

Chạy môi trường thử Windows từ Git root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Start-LocalSmokeServer.ps1 -Port 8080
```

Runner tạo DB và storage test riêng dưới server/target. Nhập `offline`/`online` để dừng/mở riêng HTTP connector nhưng giữ DB; Enter đóng runner/DB test. Cuối runner in số moment/file/recipient để kiểm tra nhất quán. File ảnh thử được giữ trong target ignored để kiểm tra, không đưa vào Git hoặc xem là tài khoản dev lâu dài. Android debug USB cần build property URL127.0.0.1:8080 và reverse tcp:8080; release vẫn chỉ HTTPS. Xem [nghiệm thu3D](QA_SELF_MOMENT.md).
