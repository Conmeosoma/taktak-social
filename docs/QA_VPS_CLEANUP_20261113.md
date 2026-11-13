# Dọn VPS — 13/11/2026

Người dùng xác nhận xóa toàn bộ ảnh khoảnh khắc mọi tài khoản, giữ avatar.
Đã kiểm kê34bản ghi moments;31file còn tồn tại, tổng2.626.748byte. Kiểm tra
đường dẫn trực tiếp trong private-images, không symlink/đệ quy, không trùng
avatar trước khi thực hiện. Trong transaction, đánh dấu deleted_at cho các
moment còn active và enqueue moment_deleted cho recipients theo outbox hiện
có; sau commit mới xóa31file đã kiểm kê. ACTIVE_MOMENTS=0.

Giữ9tham chiếu avatar, tài khoản, quan hệ bạn bè và tin nhắn. Không xóa bản
sao lưu; dữ liệu ảnh cũ có thể vẫn nằm trong backup hoặc gallery/cache máy
nhận, không tuyên bố xóa từ xa các bản sao đó. Metadata tombstone giữ để
backend thu hồi quyền và xử lý tham chiếu chat, không truncate database.

Dọn cache gói tải về bằng apt-get clean; không gỡ package hoặc xóa log.
Cache APT còn28KB, private-images còn380KB. Ổ đĩa từ69% xuống67%, còn3,2GB.
Health nội bộ sau thao tác200UP. Không thay heap/pool hoặc restart khi số
liệu không cho thấy bottleneck; không commit/push. Chưa nghiệm thu widget
trên thiết bị; outbox enqueue không đồng nghĩa mọi thiết bị đã nhận FCM.
