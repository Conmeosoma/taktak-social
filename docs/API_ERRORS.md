# Taktak — Hợp đồng lỗi API (2D)

Phạm vi: backend auth và hồ sơ đã triển khai; chốt để client giai đoạn 3 sử dụng. Không đổi stack, định danh, JWT/refresh policy hoặc success DTO của 2A–2C.

## JSON chung

```json
{
  "timestamp": "2026-10-16T07:00:00Z",
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Thông tin xác thực không hợp lệ hoặc đã hết hạn.",
  "path": "/api/users/me"
}
```

| Trường | Kiểu / ý nghĩa |
|---|---|
| timestamp | Chuỗi ISO-8601 UTC do server tạo; không phải thời gian của client |
| status | Số nguyên, khớp HTTP status |
| code | Mã tiếng Anh ổn định, không dịch theo ngôn ngữ |
| message | Thông báo tiếng Việt an toàn; client không so sánh nguyên văn để điều khiển logic |
| path | Request URI path, không kèm query string, không chứa Authorization/body |

Response lỗi của MVC và Spring Security dùng `application/json`, không redirect/cookie; `Cache-Control` có `no-store`. Body không có stack trace, tên exception, SQL, rejected values, password, token hoặc hash. Không có envelope `success/data/error` khác. Timestamp MVC và security cùng dùng Clock UTC của server (Clock điều khiển chỉ thuộc test).

## Mã đã chốt trong phạm vi hiện tại

| HTTP | code | Tình huống |
|---|---|---|
| 400 | VALIDATION_ERROR | Body thiếu/sai cấu trúc/JSON hỏng hoặc validation không đạt |
| 401 | UNAUTHORIZED | Thiếu/sai/giả/hết hạn access; login sai; refresh không hợp lệ; JWT subject không có user |
| 403 | FORBIDDEN | Đã xác thực nhưng bị chính sách security từ chối, ví dụ actuator env hoặc `/error` |
| 404 | NOT_FOUND | Route/tài nguyên không tồn tại sau khi qua xác thực |
| 405 | METHOD_NOT_ALLOWED | Route có nhưng không hỗ trợ method đã gọi; header `Allow` nêu method hỗ trợ |
| 406 | NOT_ACCEPTABLE | `Accept` không hỗ trợ response JSON; lỗi vẫn trả JSON để client đọc được |
| 409 | ACCOUNT_CONFLICT | DB unique email hoặc username; không tiết lộ constraint hay trường nào đã trùng |
| 415 | UNSUPPORTED_MEDIA_TYPE | Endpoint yêu cầu JSON nhưng Content-Type không được hỗ trợ |
| 429 | LOGIN_RATE_LIMITED | Quota login IP/account; header `Retry-After: 60` |
| 500 | INTERNAL_ERROR | Lỗi xử lý nội bộ, kể cả cấp JWT thất bại; message chung |

Thứ tự xử lý quan trọng: request tới API protected chưa có JWT hợp lệ trả 401 trước khi xét route/body, không bảo đảm 404/405/400 cho request chưa xác thực. Auth chỉ mở đúng bốn POST register/login/refresh/logout; GET các route auth vẫn cần JWT rồi mới trả 405. Không mở `/error` hoặc endpoint thay đổi thời gian để làm test. Handler MVC xử lý trực tiếp 404/405, không để error dispatch đổi chúng thành 403.

Các lỗi transport, TLS, proxy hoặc container trước khi vào Spring MVC có thể không có JSON trên; client phải xử lý response trống/không đọc được. Không gán mã media/friends tương lai thành đã triển khai. Bổ sung code mới phải cập nhật hợp đồng và test, giữ khả năng xử lý code lạ ở client.

## Quy tắc cho client sắp triển khai

- Dùng HTTP status và code; message chỉ để hiển thị phù hợp, không dùng regex để suy ra trường validation.
- 401 từ API protected: thử refresh một lần có đồng bộ nếu có refresh token; refresh 401 thì xóa phiên và yêu cầu đăng nhập. Không refresh lặp lại trên login/refresh/logout.
- 400/409: sửa input; 403/404/405/406/415: không tự retry vòng lặp; 429: tôn trọng Retry-After.
- 500/lỗi mạng: cho phép thử lại bằng hành động người dùng khi phù hợp. Không tự gửi lại register hoặc refresh vô hạn, vì request trước có thể đã commit/rotate.
- Không log body auth hoặc Authorization; không suy ra logout đã vô hiệu access JWT trước exp.

Hợp đồng nghiệp vụ: [REGISTRATION_API.md](REGISTRATION_API.md), [AUTH_API.md](AUTH_API.md), [PROFILE_API.md](PROFILE_API.md). Bằng chứng nghiệm thu và lệnh chạy: [QA_AUTH.md](QA_AUTH.md).
