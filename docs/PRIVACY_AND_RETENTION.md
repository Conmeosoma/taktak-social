# Quyền riêng tư và vòng đời dữ liệu Taktak

Ngày đối chiếu: 21/10/2026. Tài liệu mô tả cơ chế hiện có và thay đổi hardening;
không phải cam kết rằng dữ liệu trên mọi thiết bị/backup biến mất tức thời.
Không tự bật xóa tài khoản, xóa ảnh theo tuổi hoặc xóa gallery của người dùng.

## Phạm vi truy cập

- JWT nhận diện người gọi; server quyết định quyền mỗi lần đọc metadata/ảnh.
  ID ảnh, thông báo FCM, bản ghi chat hoặc cache không tự cấp quyền xem ảnh.
- Ảnh `recipientIds=[]` chỉ chủ ảnh xem. Upload/gửi mới xét quan hệ và block;
  GET ảnh kiểm tra sender/recipient grant và chưa bị xóa. Hủy bạn/block không
  tự xóa grant của ảnh đã gửi theo code hiện tại; không được diễn giải là
  remote revoke mọi ảnh lịch sử. Chat xét quan hệ/block và không mở thêm người nhận.
- `MOMENT_REPLY` là nội dung riêng của hai người. Khi ảnh bị thu hồi, tham chiếu
  ảnh bị che; văn bản đã gửi trong hội thoại không tự bị xóa theo ảnh.
- Không có image URL public. Android không lưu bearer vào URL, Intent hay log.
  Firebase chỉ là tín hiệu thay đổi, không phải kênh truyền nội dung riêng tư.

## Vòng đời theo nơi lưu

| Dữ liệu | Nơi lưu và thời hạn thực tế | Khi logout/thu hồi |
|---|---|---|
| Access/refresh | EncryptedSharedPreferences, theo hạn server; refresh xoay vòng | SessionManager clear local; reset mật khẩu tăng security_version và revoke refresh trên server |
| Ảnh lịch sử đã decode | RAM; LRU tối đa min(20 MiB, 1/8 heap), TTL 5 phút không gia hạn khi đọc | Clear khi đổi/thu hồi phiên; remove theo sự kiện xóa hoặc GET bị từ chối |
| Ảnh chụp nháp | App-private cache `camera-drafts/<generation>/<UUID>.jpg`; không đọc khi quá 24 giờ | Generation guard từ chối phiên cũ; luồng preview discard ID thuộc chính nó; không đụng gallery |
| Ảnh viewer/chat/avatar | RAM trong vòng đời màn hình; request bảo vệ bằng generation; Glide private loads tắt disk cache | UI bỏ snapshot/private view khi nhận biết phiên/quyền không hợp lệ |
| Widget | Bitmap trong RemoteViews do launcher giữ | Logout render placeholder; worker truy vấn lại quyền trước khi cập nhật |
| Ảnh gốc server | Private storage ngoài web root; metadata PostgreSQL | Chủ ảnh DELETE làm endpoint từ chối truy cập và thực hiện xóa file theo storage service |
| Ảnh người dùng chọn Lưu | MediaStore/gallery do người dùng chủ động xuất | Không tự thu hồi/xóa bản xuất khi logout hoặc xóa ảnh trên server |

TTL RAM được kiểm tra khi cache được truy cập; đây không phải timer xóa vật lý
đúng phút khi process ngủ. OS có thể hủy RAM/cache sớm hơn. Draft quá 24 giờ bị
từ chối đọc, nhưng hiện không có job quét bảo đảm xóa file đúng thời điểm hết hạn.
Không tuyên bố xóa vật lý ngay hoặc mã hóa toàn bộ cache trên ổ đĩa bằng khóa riêng
của ứng dụng: draft đang dựa vào sandbox và bảo vệ lưu trữ của hệ điều hành.

## Auth trước khi hiển thị, không tin cache như grant

`HistoryViewModel.loadImage` luôn gọi GET ảnh có xác thực trước mỗi lần bind,
kể cả có bitmap RAM. Chỉ sau GET thành công, trong `publishIfCurrent`, cache
decode mới được đọc/ghi bằng generation hiện tại. TTL không thay thế kiểm tra quyền.
Sau 403/404/401/cancel, cache ID tương ứng bị bỏ. Một GET hợp lệ mới được phép
tải/hiển thị lại sau khi quyền được cấp mới; không dùng tombstone vĩnh viễn gây
chặn nhầm ảnh hợp lệ.

Đánh đổi: bind lại có chi phí mạng/tải JPEG, đổi lại FCM bị mất không biến RAM
thành quyền xem offline. Chưa có API grant/ETag riêng nên không giả định metadata
đã tải trước đó đủ để xác nhận quyền tại thời điểm hiện tại. Nếu tối ưu sau này,
phải đo số request và có giao thức xác thực cache rõ ràng trước khi tái dùng offline.

Generation guard kiểm tra cả sau tải và khi publish; kết quả cũ không được đưa
lên phiên mới. Clear cache đồng bộ với session invalidation để không đưa entry
cũ trở lại sau logout. Không recycle bitmap còn được ImageView sử dụng.

## Giới hạn cần nói rõ

- Không thể thu hồi những pixel người nhận đã thấy, screenshot hoặc bản gallery
  do họ chủ động lưu. Xóa trên server không phải remote wipe điện thoại khác.
- Màn hình đang mở và widget offline có thể giữ ảnh cũ cho đến lần sự kiện/đồng bộ
  hoặc revalidation tiếp theo. FCM, WorkManager và launcher/OEM không bảo đảm
  tức thì; TTL RAM không tự xóa bitmap đã giao cho ImageView/RemoteViews.
- Backup DB/file có thể chứa dữ liệu đã xóa trước thời điểm backup. Chưa có thời
  hạn backup được người vận hành chốt; phải chốt thời hạn, quyền, mã hóa và thử
  restore trước production. Không đánh dấu tuân thủ pháp lý chỉ nhờ tài liệu này.
- Chưa có luồng yêu cầu xóa tài khoản/xuất toàn bộ dữ liệu; cần thiết kế, quyền
  vận hành và nghiệm thu riêng, không tự thêm trong đợt hardening này.

## Kiểm tra hồi quy

- `SessionImageCacheTest`: generation, hết TTL đúng biên, clock regression,
  revoke/clear, LRU và bỏ entry quá lớn; fresh entry không bị chặn nhầm.
- `HistoryCachePrivacyTest`: tải thành công rồi GET từ chối không dùng cache cũ;
  quyền mới thành công hiển thị lại; repository phiên cũ không publish cache.
- Các suite Viewer/MomentReply/Media hiện có tiếp tục kiểm tra quyền, đổi phiên,
  xóa và tải ảnh. Kết quả chạy thực tế nằm trong hồ sơ QA/bàn giao, không suy
  luận PASS chỉ từ danh sách test này.

## Dữ liệu hỗ trợ điều tra

Chỉ chia sẻ correlation ID, route template, mã lỗi và khoảng thời gian. Không
đính kèm token, SMTP credentials, JPEG, caption, nội dung chat/recovery hoặc raw
request/response. Screenshot QA dùng fixture, không đưa ảnh cá nhân vào Git.
