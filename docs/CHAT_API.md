# Chat 1–1 — CHAT-01–05

Phạm vi được duyệt ngày 20/10/2026. Java/XML/MVVM, giữ JWT và private media, Firebase chỉ FCM. Không nhóm chat, chatbot, upload gallery mới hoặc cấp quyền xem ảnh qua chat.

## REST

Mọi endpoint dưới `/api` cần JWT. Actor lấy từ principal, không từ body. Hội thoại chỉ khả dụng cho hai thành viên đang là bạn, không bị block hai chiều. Không có quyền trả 404 `CHAT_UNAVAILABLE`; response dùng `Cache-Control: no-store`.

| Method | Endpoint | Kết quả |
|---|---|---|
| GET | `/conversations?page=0` | `{items,page,hasNext,totalUnreadCount}`, 20 hội thoại/trang |
| POST | `/conversations/with-friend/{friendId}` | Mở/tạo hội thoại cặp duy nhất; 200 |
| GET | `/conversations/{id}` | Peer công khai, lastMessage, unreadCount, updatedAt |
| GET | `/conversations/{id}/messages?page=0` | `{items,page,hasNext}`, 50 tin/trang, mới nhất trước |
| POST | `/conversations/{id}/messages` | `{clientMessageId,messageType,content,momentId}` → Message, 200 |
| PUT | `/conversations/{id}/read` | `{messageId}` → 204 |

Trang 0–10000. Sắp xếp theo thời gian và UUID; Android khử trùng ID khi phân trang. Tổng badge unread được tính trên mọi hội thoại hợp lệ, không chỉ trang đầu.

TEXT có content 1–2000 ký tự Unicode, không trắng hoàn toàn/NUL, momentId null. MOMENT có momentId UUID, request content null; response content là caption đã kiểm tra quyền. Tin có id, conversationId, senderId, recipientId, clientMessageId, messageType, content, momentId, createdAt, readAt; không có đường dẫn ảnh/private URL. Peer có id, username, displayName, avatarVersion (hiện null).

## Nhất quán và riêng tư

- V4 thêm `conversations`/`messages`, cặp UUID có thứ tự + UNIQUE, UNIQUE(sender_id,client_message_id), index phân trang/unread. Không chạy migration lên production trong lượt phát triển này.
- Gửi/mở/read dùng khóa cặp users chung với friend/block để kiểm tra lại quan hệ sau khi chờ khóa. Moment khóa khi gửi, yêu cầu cả sender và recipient đã có quyền; ảnh chỉ mình tôi không thể đính kèm cho bạn.
- Retry cùng payload/key trả bản ghi cũ, không gửi FCM lần nữa; payload khác trả 409 `CHAT_IDEMPOTENCY_CONFLICT`. Client giữ key/payload khi timeout, không auto retry POST.
- Read chỉ cập nhật tin nhận tới anchor `(created_at,id)`, giữ readAt cũ; tin tương lai vẫn unread. Android đánh dấu khi tin mới nhất đã hiển thị.
- Khi moment bị xóa/thu hồi quyền, server che caption và momentId trong message; ảnh tiếp tục qua API moment có JWT, không public URL. Client không dùng cache ảnh chat thay cho kiểm tra quyền mới.
- SavedState chỉ giữ draft/pending trong cùng process/thế hệ phiên; process mới hoặc phiên khác phải bỏ dữ liệu chưa gửi. Phiên mất thì xóa dữ liệu chat hiển thị và notification chat.

## FCM và Android

FCM after-commit: `{type:chat_message,conversationId,messageId}`; không chứa text, caption, ảnh, URL hoặc token. Push thất bại không rollback message. Android xác minh quyền qua API trước khi emit event/notification; foreground tải lại, fallback polling 5 giây chỉ khi màn/Main đang hoạt động. Quyền thông báo tùy chọn, có nút bật ở Inbox; từ chối không chặn gửi/đọc chat.

Notification nền chỉ ghi thông báo chung, không nội dung tin; chạm mở Main rồi Chat sau xác thực. FCM/Doze/OEM/force-stop có thể trì hoãn hoặc ngăn delivery; không cam kết tức thì.

Inbox có search các trang đã tải, Xem thêm để tải trang kế; New Chat dùng danh sách bạn phân trang. Đính kèm chọn moment từ trang đầu Đã gửi/Đã nhận; server là nơi quyết định cả hai có quyền. Ảnh không phù hợp bị từ chối, không tự mở rộng quyền hoặc upload lại. Bốn phong cách Original/Gradient/Glass/Soft dùng tokens media hiện có.

## Kiểm thử

Xem [QA_CHAT.md](QA_CHAT.md) để biết lệnh, kết quả thực tế và giới hạn nghiệm thu. QA UI dùng graph/session RAM; cloud dùng PostgreSQL tạm riêng và tài khoản QA riêng, không dùng tài khoản cá nhân trên điện thoại.
