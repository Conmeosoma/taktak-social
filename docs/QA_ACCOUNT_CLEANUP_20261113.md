# Xóa tài khoản database — 13/11/2026

Người dùng yêu cầu rõ ràng xóa toàn bộ account trên DB. Trước thao tác:
37 tài khoản,29 tin nhắn,34 metadata moments đã xóa ảnh từ lượt trước,
9 tham chiếu avatar. Đã đọc schema/FK và thực hiện DELETE theo thứ tự phụ
thuộc trong một transaction, có lock timeout5s/statement timeout20s.
Xóa outbox, liên kết last_message, messages/conversations, recipients/moments,
friend requests/friendships/blocks, devices, refresh, recovery và users.

COMMIT thành công; kiểm tra sau: users/refresh_tokens/device_tokens/messages/
moments/push_outbox đều0. Giữ schema và6bản ghi Flyway; không DROP/TRUNCATE
hoặc sửa migration. Health nội bộ sau thao tác UP. Không restart/commit/push.

Không xóa backup hoặc file avatar đã được yêu cầu giữ trong lượt trước.
Avatar còn trên đĩa nhưng không còn liên kết hồ sơ sau khi xóa account.
Không tuyên bố đã xóa dữ liệu trong backup/thiết bị người dùng. Tất cả tài
khoản cũ phải đăng ký lại; refresh/device registrations đã bị xóa. Chưa chạy
UI trên điện thoại hoặc nghiệm thu cache/widget sau thao tác này.
