# Taktak — Quy trình kỹ thuật và chuyển giao

Phạm vi hardening được người dùng duyệt ngày 21/10/2026. Đây là quy trình đề
xuất cho nhóm, không phải bằng chứng đã có on-call, branch protection hoặc sự cố
production. Không tự gán chức vụ/đóng góp cho thành viên trong README.

## Chia việc và ownership

Mỗi ticket có một người chịu trách nhiệm kết quả, một người review độc lập và
tiêu chí nghiệm thu; nhóm tự thống nhất người nhận, không gán tên thay nhóm.

| Luồng | Phạm vi bàn giao | Review bắt buộc |
|---|---|---|
| Backend/data | Transaction, outbox, migration, authorization | Rollback, race, retry, không lộ dữ liệu |
| Android | Phiên, cache, UI lifecycle, quyền thiết bị | Switch-account/revoke/offline, Java/XML |
| QA | Kịch bản, fixture tách biệt, đo tải | Log thực, số liệu tái lập, không production |
| Vận hành | Cấu hình, metrics, backup/restore | Bí mật, access control, tương thích JAR/schema |
| Tài liệu | ADR, hợp đồng API, runbook và giới hạn | Phân biệt đề xuất, đã test và đã triển khai |

Không chia bằng số dòng code. Mỗi ticket ghi dependency, file sở hữu, cổng kiểm
thử, ảnh hưởng người dùng, rollout và điều kiện rollback. Nếu hai người sửa cùng
file, thống nhất phần sở hữu trước; người tích hợp chạy lại regression toàn bộ.

## Review trước khi tích hợp

- Đọc diff và source liên quan, không chỉ báo cáo của tác giả.
- Test phải thất bại khi bỏ guard/fix; không thay assertion để che regression.
- Đầu vào không được cấp quyền thay server; cache/FCM không phải grant.
- Transaction outbox cùng transaction nghiệp vụ, không gọi mạng trong transaction
  giữ row lock; lease/fencing không cam kết exactly-once.
- Idempotency, concurrent claim, restart, token hết hạn/unregistered, giới hạn
  retry và dữ liệu tồn đọng phải có kiểm thử.
- Migration mới, không sửa checksum migration đã áp; kiểm tra JAR cũ có chấp
  nhận JWT đã revoke không trước khi cân nhắc rollback.
- Không log body/header/query/credential/email/private image path. Metric label
  không chứa userId/momentId/conversationId/requestId.
- So sánh p95/p99, query count, dataset, concurrency, JVM/cache trạng thái và
  chi phí thay đổi; không thêm index/cache chỉ vì một lần test chậm.
- Không tự commit/push/deploy; thao tác thực tế cần quyền người dùng phù hợp.

## Hướng dẫn thành viên mới

1. Đọc AGENTS, đặc tả, quyết định đã khóa, hợp đồng API và handoff.
2. Build Android/backend theo README, dùng DB/ảnh test riêng và fake push/mail.
3. Theo dõi một request: Controller → Service → DB/storage → outbox → dispatcher;
   Android ViewModel → Repository → JWT → response → view/cache.
4. Thực hiện một sửa đổi nhỏ kèm test âm tính (ví dụ quyền xem sau revoke), giải
   thích nguyên nhân và đánh đổi trong review.
5. Chạy lại phép thử dưới sự quan sát của reviewer; bàn giao log an toàn, không
   dán credential vào chat hoặc ticket.

## Mẫu ghi nhận sự cố

Không gọi các lỗi QA là sự cố production. Dùng cùng mẫu cho diễn tập, nhưng ghi
rõ môi trường, dữ liệu giả và phần chưa kiểm chứng.

```text
Tiêu đề / môi trường / mức độ:
Khoảng thời gian UTC và ảnh hưởng quan sát được:
Bằng chứng đã khử dữ liệu riêng tư (request ID, loại lỗi, metric tổng hợp):
Cách phát hiện và timeline:
Nguyên nhân gốc và điều kiện góp phần:
Biện pháp khống chế, nguy cơ còn lại:
Quyết định rollback/forward-fix, tương thích schema/JWT:
Test tái hiện trước sửa và regression sau sửa:
Hành động phòng ngừa / người nhận / điều kiện đóng:
Các giả thuyết chưa được chứng minh:
```

Không truy tìm người để đổ lỗi. Ví dụ rủi ro được phát hiện trong QA lần này:
bitmap cache từng được dùng như quyền xem offline; việc sửa cần tách cache
decoded data khỏi authorization, ghi chi phí network-per-bind, và giữ test
server từ chối ảnh sau thu hồi. Không suy ra đã có rò rỉ production.
