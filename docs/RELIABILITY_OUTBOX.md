# Transactional FCM outbox — 21/10/2026

## Hợp đồng và đánh đổi

Ảnh/tin nhắn và sự kiện cho từng người nhận được ghi **cùng transaction PostgreSQL**.
Request không chờ Firebase. Nếu transaction rollback thì không có sự kiện; nếu Firebase
lỗi sau commit thì ảnh/tin nhắn vẫn có thể tải bằng API có xác thực.

V6 chỉ thêm `push_outbox`, không sửa migration V1–V5. Event chỉ lưu loại sự kiện,
UUID tài nguyên/hội thoại, sender/recipient; không copy caption, tin nhắn, JWT, email,
đường dẫn ảnh hoặc FCM token. Unique(type,resource,recipient) và idempotency nghiệp vụ
ngăn request replay tạo thêm sự kiện.

Dispatcher claim bằng `FOR UPDATE SKIP LOCKED` trong transaction ngắn, lease 120 giây
và UUID fencing. Gọi Firebase ngoài transaction; chỉ kết thúc lease còn sở hữu.
Restart sẽ lấy lại lease hết hạn. Mỗi poll tối đa 25 sự kiện, không tạo executor/thread
pool không giới hạn. Retry mặc định tối đa 8 lượt; backoff 5s,10s,20s… tối đa 1h.

Đây là **at-least-once**, không phải exactly-once: crash giữa Firebase acceptance và
ghi trạng thái, timeout không rõ kết quả, lease hết hạn trong khi send chậm, hoặc lỗi một
phần ở nhiều thiết bị có thể gửi lại. Client phải coi push là tín hiệu để đồng bộ dữ liệu
có xác thực/idempotent. `ACCEPTED` chỉ là Firebase đã chấp nhận, không chứng minh điện
thoại nhận hoặc widget đã cập nhật. Không hứa thứ tự push; Worker truy vấn trạng thái
hiện tại nên event cũ không cấp lại quyền ảnh.

## Quyền riêng tư khi dispatch

- Resolve registration hiện tại, không dùng raw token đã chụp khi upload. Kiểm tra lại
  token vẫn thuộc recipient ngay trước send. Logout/reset xóa registration thì không
  gửi tới registration cũ; token đổi sẽ dùng token mới.
- Creation/chat kiểm tra lại friend/block và tài nguyên/grant. Ảnh đã xóa trước dispatch
  thì creation bị hủy. `moment_deleted` vẫn gửi invalidation sau block/unfriend vì payload
  không chứa nội dung, giúp loại cache cũ. Tất cả GET ảnh vẫn phải kiểm tra quyền.
- Có cửa sổ race rất ngắn giữa kiểm tra quyền/registration và external send; payload chỉ
  chứa định danh, tuyệt đối không dùng push làm bằng chứng cấp quyền.
- `UNREGISTERED` xóa registration và kết thúc không retry; mã lỗi không thể sửa bằng retry
  kết thúc DEAD. Firebase bị tắt là DISABLED và retry/backoff rồi DEAD, không giả ACCEPTED.
- Không có registration thì CANCELLED, không replay tới thiết bị đăng ký về sau; mở app/
  periodic Worker vẫn đồng bộ bằng API.

## Cấu hình và vận hành

| Property | Mặc định | Ý nghĩa |
|---|---:|---|
| `taktak.outbox.enabled` | true | Chỉ bật/tắt scheduler; **không tắt enqueue** |
| `taktak.outbox.poll-ms` | 5000 | Delay sau mỗi poll |
| `taktak.outbox.initial-delay-ms` | 10000 | Delay khởi động |
| `taktak.outbox.max-attempts` | 8 | Retry budget, clamp 1–30 |

Metric hữu hạn `taktak.push.outbox.rows{state=...}` và
`taktak.push.outbox.oldest.seconds`; cảnh báo `OPS_PUSH_OUTBOX_BACKLOG` khi backlog
già hơn 300 giây hoặc có DEAD. Không dùng UUID/token làm nhãn hoặc log.

Khi cảnh báo: kiểm tra FCM enabled/credential và kết nối outbound, kiểm tra trạng thái
outbox bằng aggregate SQL trên DB quản trị; không xuất bảng device_tokens hoặc nội dung
messages. DEAD cần operator review sau khi sửa nguyên nhân; không có public replay API,
không tự replay vô hạn. Chưa bật chính sách tự xóa terminal rows hoặc thao tác DB production;
cần lập lịch retention/archival được duyệt nếu tải production tạo nhiều metadata.

Rollback binary phải dùng phiên bản còn hiểu V6/outbox hoặc tạm pause dispatcher và triển
khai forward fix. Không drop bảng, không sửa checksum migration cũ, không restore DB cũ để
rollback ứng dụng (có thể hồi sinh phiên đã thu hồi). Xem tài liệu version/rollback chung.

## Bằng chứng kiểm thử

`.local/artifacts/outbox-targeted.log`: 50/50 tests PASS, 30.162s;
ChatIntegrationTest18, MomentPushIntegrationTest18, ServerBootstrapTest14.
7 ca mới: rollback event, retry và new dispatcher/re-registration, disabled budget,
UNREGISTERED cleanup, lease recovery, concurrent claims, block trước dispatch.
Test gọi drain dispatcher tường minh, không giả afterCommit synchronous và không bỏ
assertion payload/multiple device/idempotency của các ca cũ.

Không dùng Firebase thật/credential thật; chưa chứng minh FCM cloud delivery, process kill
toàn JVM hoặc nhiều VPS thực tế. Mô phỏng restart dispatcher và expired lease trên PostgreSQL
embedded thật; kiểm thử toàn bộ server cuối lượt do workflow chính ghi nhận riêng.
