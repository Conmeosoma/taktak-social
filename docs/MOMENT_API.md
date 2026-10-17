# API ảnh riêng tư — 5E: Lịch sử ảnh đã nhận (Received Moments History)

Cập nhật 18/10/2026. Bổ sung `GET /api/moments?type=received&page=0` trả về danh sách khoảnh khắc đã nhận kèm người gửi, caption và timestamp. Giữ nguyên Idempotency-Key và upload progress/retry từ 5D. Chưa có DELETE/FCM runtime/widget.

## Upload

`POST /api/moments`, `Authorization: Bearer <access-token>`, `Idempotency-Key: <UUID>` (tùy chọn nhưng client luôn gửi UUID cho mỗi lần bắt đầu draft), `multipart/form-data`:

| Field / Header | Quy tắc |
|---|---|
| Idempotency-Key | Header tùy chọn, định dạng UUID v4 (chuẩn 36 ký tự). Khi được truyền, server bảo đảm tính idempotent bền vững qua PostgreSQL |
| image | Bắt buộc; Content-Type image/jpeg đồng thời phải có signature và decode JPEG hợp lệ; tối đa 5 MiB; không tin filename/extension |
| caption | Không bắt buộc, mặc định rỗng; tối đa 50 Unicode code point, không NUL |
| recipientIds | Bỏ field hoặc đúng một literal `[]` = chỉ mình tôi; gửi bạn bè dùng 1–100 field cùng tên, mỗi field một UUID đầy đủ. Không JSON array/CSV, ID trùng (kể cả khác hoa/thường), bản thân, ID thiếu/sai hoặc trộn `[]` với ID |

### Cơ chế Idempotency Key qua PostgreSQL

- **Bền vững qua restart**: Lưu trữ cột `idempotency_key UUID` và `payload_hash VARCHAR(64)` trong bảng `moments`. Unique index có điều kiện `uq_moments_sender_idempotency` trên `(sender_id, idempotency_key) WHERE idempotency_key IS NOT NULL`.
- **Payload Hash (Fingerprint)**: Tính bằng SHA-256 trên: `sender_id` + `caption` (UTF-8) + danh sách `recipientIds` (sắp xếp tăng dần theo UUID) + raw bytes của ảnh JPEG.
- **Replay cùng key & cùng payload**: Nếu sender và idempotency_key đã tồn tại và `payload_hash` trùng khớp, server lập tức trả về **201 Created** với `{"id":"<momentId ban đầu>"}`, không lưu ảnh mới lên đĩa VPS, không tạo bản ghi trùng lặp và không gửi lại FCM.
- **Conflict cùng key & khác payload**: Nếu cùng key nhưng `payload_hash` khác (thay đổi caption, người nhận hoặc ảnh), server trả về **409 Conflict** với mã lỗi `IDEMPOTENCY_CONFLICT`.
- **Key không hợp lệ**: Nếu `Idempotency-Key` không đúng định dạng UUID hợp lệ, trả về **400 Bad Request** với mã lỗi `INVALID_IDEMPOTENCY_KEY`.
- **Đồng thời (Concurrency)**: Khóa bi quan `UserRepository.lockAll` theo thứ tự sender + recipients tuần tự hóa các request cùng sender, kết hợp unique constraint ở tầng DB để ngăn ngừa hoàn toàn duplicate moment.

Thành công: **201**, `Cache-Control: no-store`, JSON chỉ có `{"id":"<uuid>"}`. Sender lấy từ JWT đã xác minh và phải tồn tại trong DB; không nhận senderId từ client.

Multipart tối đa 5 MiB/file, 6 MiB/request; Tomcat giới hạn 104 parts để chứa 100 recipients + image/caption và trả được lỗi nghiệp vụ ở biên101. Không tắt giới hạn container. Thuộc tính được đối chiếu [Spring Boot 3.5 application properties](https://docs.spring.io/spring-boot/3.5/appendix/application-properties/).

Server kiểm tra JPEG signature và decode thực tế; từ chối chiều lớn hơn4096 hoặc hơn16 triệu pixel trước khi đọc bitmap. Re-encode JPEG không chuyển metadata EXIF/GPS hoặc dữ liệu nối thêm sang ảnh lưu. Tên file UUID do server sinh, không dùng tên/path client. PNG và định dạng khác chưa được hỗ trợ trong 3D.

File được ghi đầy đủ trước saveAndFlush trong transaction; callback rollback xóa file nếu DB không commit. IO thất bại trước lưu metadata không tạo bản ghi moment. File pending được dọn trong finally.

## Lịch sử ảnh đã nhận (Received Moments History — 5E)

`GET /api/moments?type=received&page=0`, `Authorization: Bearer <access-token>`.

### Quy tắc nghiệp vụ và bảo mật
- **Query Parameters**:
  - `type`: Bắt buộc; hiện tại hỗ trợ `type=received` (nếu khác hoặc thiếu trả về `400 INVALID_TYPE`).
  - `page`: Không bắt buộc, số nguyên không âm (mặc định `0`); nếu nhỏ hơn 0 trả về `400 INVALID_PAGE`.
- **Phân trang (Pagination)**:
  - Cố định `pageSize = 20`.
  - Phân trang tính theo `LIMIT 21 OFFSET (page * 20)` để xác định cờ `hasNext` mà không cần đếm toàn bộ bảng (`COUNT`).
- **Sắp xếp và phạm vi**:
  - Sắp xếp mới nhất trước theo thời gian tạo: `created_at DESC, id DESC`.
  - Tận dụng index tối ưu: `ix_moment_recipients_user_created` trên `(recipient_id, created_at DESC)`.
  - Chỉ trả về khoảnh khắc mà người dùng hiện tại là **recipient được lưu trong `moment_recipients`** và `deleted_at IS NULL`.
  - **Cô lập hoàn toàn**: Ảnh riêng tư của bản thân (owner-only/self-moments với `recipientIds=[]`) KHÔNG xuất hiện trong danh sách ảnh đã nhận (`type=received`).
  - Người lạ (strangers), bạn bè không được chọn, hoặc khoảnh khắc đã bị xóa mềm (`deleted_at IS NOT NULL`) tuyệt đối không xuất hiện trong danh sách.
- **DTO Response**:
  - JSON trả về gồm: `items` (danh sách khoảnh khắc), `page` (trang hiện tại), `hasNext` (boolean báo còn trang tiếp theo không).
  - Mỗi phần tử `MomentItem`:
    - `id`: UUID của moment.
    - `sender`: Thông tin công khai của người gửi (`id`, `username`, `displayName`). Tuyệt đối không để lộ email, mật khẩu hoặc số điện thoại.
    - `caption`: Chuỗi chú thích (tối đa 50 ký tự Unicode, có thể rỗng).
    - `createdAt`: Chuỗi ISO-8601 UTC timestamp (`Instant`).

Ví dụ response:
```json
{
  "items": [
    {
      "id": "c0a80165-9273-4c92-881a-7b3b44c68832",
      "sender": {
        "id": "e4b10294-82a1-4d3b-9a2c-1b3c4d5e6f7a",
        "username": "alice",
        "displayName": "Alice Nguyen"
      },
      "caption": "Hoàng hôn chiều nay đẹp quá!",
      "createdAt": "2026-10-18T00:30:00Z"
    }
  ],
  "page": 0,
  "hasNext": false
}
```

## Tải ảnh

`GET /api/moments/{id}/image`, bắt buộc JWT. Mỗi request kiểm tra user tồn tại, moment chưa xóa và người gọi là **sender hoặc recipient được lưu cho đúng moment**, rồi mới đọc file private. Friendship không tự cấp quyền xem ảnh không được chọn; owner-only vẫn chỉ sender. Thành công **200 image/jpeg**, no-store, nosniff và filename cố định không chứa path. Người khác, ID không tồn tại, ảnh đã đánh dấu xóa hoặc file thiếu đều **404**; không mở URL file public/static. Block/unfriend ngăn gửi mới; không tự sửa grant của moment cũ hoặc thêm cơ chế thu hồi ngoài đặc tả. Chưa có endpoint DELETE/lịch sử ảnh đã gửi.

Lỗi dùng envelope timestamp/status/code/message/path hiện có:

| HTTP | Code chính |
|---|---|
| 400 | VALIDATION_ERROR, IMAGE_REQUIRED, INVALID_CAPTION, INVALID_RECIPIENTS, TOO_MANY_RECIPIENTS, RECIPIENT_UNAVAILABLE, INVALID_IDEMPOTENCY_KEY |
| 401 | UNAUTHORIZED — JWT thiếu/sai/hết hạn hoặc user không tồn tại |
| 404 | MOMENT_NOT_FOUND |
| 409 | IDEMPOTENCY_CONFLICT — Cùng idempotency key nhưng payload khác với request trước |
| 413 | IMAGE_TOO_LARGE — file hơn5 MiB hoặc multipart hơn6 MiB |
| 415 | INVALID_IMAGE / UNSUPPORTED_MEDIA_TYPE |
| 500 | INTERNAL_ERROR — IO/DB lỗi, không phản chiếu nội dung nội bộ |

## Android và vận hành local

- **CountingRequestBody**: Theo dõi số byte ghi thực tế của OkHttp để tính tiến trình 0–100%.
- **Hiển thị tiến trình**: Phân biệt tiến trình truyền dữ liệu (0–99%, determinate ProgressBar hiển thị % cụ thể) với thời gian server xử lý/xác thực (100%, indeterminate ProgressBar "Máy chủ đang xử lý và xác thực ảnh...").
- **SavedStateHandle**: Lưu `idempotencyKey` trong ViewModel state để sống sót qua configuration change và process death.
- **Retry bản nháp**: Khi gặp lỗi truyền tải, giữ nguyên `idempotencyKey` và payload đóng băng để gửi lại cùng key. Khi người dùng chủ động thay đổi caption hoặc chọn lại bạn bè, xóa key cũ để sinh key mới cho lần gửi logic mới.
- **Lỗi không retryable**: 400, 401, 409 mở khóa bản nháp để người dùng điều chỉnh lại và xóa key cũ. Không tự động retry vô hạn.

Storage root: `IMAGE_STORAGE_ROOT` (mặc định server/.local/private-images khi chạy từ server). VPS phải đặt ngoài web root, chỉ user dịch vụ có quyền truy cập. Linux/POSIX: directory0700/file0600; Windows dùng ACL kế thừa của thư mục local, cần cấu hình ACL phù hợp khi vận hành. Không map storage bằng Nginx/static handler. Chưa deploy hoặc chuẩn bị DB dev bền vững.

Chạy môi trường thử Windows từ Git root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Start-LocalSmokeServer.ps1 -Port 8080
```

Runner tạo DB và storage test riêng dưới server/target. Nhập `offline`/`online` để dừng/mở riêng HTTP connector nhưng giữ DB; Enter đóng runner/DB test. Cuối runner in số moment/file/recipient để kiểm tra nhất quán. File ảnh thử được giữ trong target ignored để kiểm tra, không đưa vào Git hoặc xem là tài khoản dev lâu dài. Android debug USB cần build property URL127.0.0.1:8080 và reverse tcp:8080; release vẫn chỉ HTTPS. Xem [nghiệm thu3D](QA_SELF_MOMENT.md).
