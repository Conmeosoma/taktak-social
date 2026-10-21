# Tương thích client/server và cập nhật an toàn

Ngày đối chiếu: 21/10/2026. Đây là quy trình chuẩn bị/staging; không phải bằng
chứng đã deploy, migrate hay restore production. Không đổi Java/XML, package,
stack hoặc sửa checksum migration đã áp dụng.

## Hợp đồng và thứ tự phát hành

1. Chạy backend integration test, Android JVM/build và contract smoke trong DB
   test độc lập. Backup DB + private images nhất quán; **thử restore** trước khi
   gọi backup là dùng được. Ghi checksum artifact, Flyway version và cấu hình
   không bí mật; secrets luôn nằm ngoài artifact/log.
2. Thử migration mới trên bản sao staging. Chỉ dùng migration số mới, không
   sửa V1–V5 đã có. Nếu outbox V6 được triển khai, kiểm tra cả schema/scheduler
   và nội dung pending, không coi FCM gửi được là điều kiện commit ảnh.
3. Phát server tương thích trước, chạy health/auth/upload/download/delete/chat
   smoke, rồi mới phát APK cần tính năng mới. Không cập nhật APK để buộc server
   cũ hiểu payload mới. Dừng rollout nếu có lỗi quyền, reset/revocation hoặc
   migration; không chỉ nhìn health 200.
4. Giữ success DTO cũ và trường cũ; thêm field tùy chọn có mặc định rõ ràng.
   Không đổi kiểu/ý nghĩa field trong cùng `/api` nếu chưa có migration contract.
   Hạn mức, pagination, idempotency và status lỗi là một phần hợp đồng.

## Ma trận các điểm dễ mất tương thích

| Thay đổi | Rủi ro mixed version | Chính sách |
|---|---|---|
| JWT `sv` / users.security_version (V3) | Server cũ chỉ kiểm chữ ký/exp có thể chấp nhận JWT đã reset thu hồi | Cấm rollback về binary không enforce security_version |
| Chat V4 và MOMENT_REPLY V5 | APK trước reply có thể không render hoặc diễn giải đúng messageType mới | Test APK cũ thật trước mở rollout; chưa có capability negotiation/min-client gate |
| Trả lời ảnh từ APK mới tới server trước V5 | Server/constraint không hiểu MOMENT_REPLY | Server trước APK; giữ lỗi an toàn, không tự đổi reply thành TEXT rồi tự gửi lại |
| Outbox V6 | Binary cũ không tiêu thụ pending; push có thể bị bỏ hoặc trùng nếu chạy hai cơ chế | Không chạy mixed dispatcher không được kiểm chứng; giữ pending DB để forward fix |
| API ảnh có auth | Cache/client cũ có thể hiển thị nội dung đã tải | Server luôn chặn request mới; không cam kết remote wipe cache APK cũ |

Gson bỏ qua field JSON mới không chứng minh tương thích messageType mới. Hiện
không có `/api/v2`, version handshake hoặc bắt buộc nâng cấp toàn ứng dụng.
Không ghi “backward compatible mọi APK” khi chưa chạy ma trận APK/server cụ thể.

## Reset mật khẩu và rollback an toàn

Bản sửa RESTORE-SECURITY-01 có SQL reconciliation và systemd quarantine,
không tự start service. Giữ current credentials, tăng version và revoke mọi
refresh; thiếu current security state bị từ chối. Quy trình mở lại vẫn cần
operator duyệt, không phải quyền deploy/restore production.
[Bằng chứng và giới hạn](QA_RESTORE_SECURITY.md), [runbook](../deploy/MAINTENANCE.md).

Hiện `TokenService` ký claim `sv`; `TokenConfiguration` so sánh với
`users.security_version` **mỗi lần xác thực**. JWT không có `sv` chỉ được xem là
version 0, không được bỏ qua so sánh. `RecoveryService` reset password và revoke
refresh; `User.resetPassword` tăng version. Đây là invariant bảo mật phải giữ.

- Rollback code chỉ được tới artifact vẫn enforce invariant này và hiểu schema
  hiện tại. Không rollback về server chỉ kiểm chữ ký/expiry dù token còn hạn.
- Không giảm security_version, không restore refresh bị revoked thành active,
  không xóa cột hoặc chạy down migration để “khớp binary cũ”.
- Restore DB cũ có thể hồi sinh password hash/refresh/security_version cũ.
  Vì vậy **không restore DB cũ trực tiếp thành public service**. Cách ly traffic;
  chạy reconciliation thu hồi toàn bộ phiên và xử lý credential/recovery bị
  hoàn nguyên trước khi mở lại. Nếu phải thay signing secret để chặn JWT cũ,
  phải đồng thời revoke refresh và xử lý password/reset state, không chỉ đổi key.
- Quy trình reconciliation/rotation là thao tác vận hành cần kế hoạch và phê
  duyệt riêng; tài liệu này không tự thực hiện SQL/rotation hay hứa restore
  giữ nguyên mọi phiên. Mặc định ưu tiên forward fix, giữ schema/data hiện tại.

## Migration và rollback không mất dữ liệu

Flyway `validate` phải đạt; không dùng repair để che checksum mismatch, không
`ddl-auto=update`. Migration mở rộng schema trước; chỉ bỏ cột/constraint sau khi
không còn binary đang dùng và có thay đổi được duyệt riêng. Outbox pending không
được truncate để giảm backlog. Không xóa message MOMENT_REPLY để phục vụ APK cũ.

Một rollback ứng dụng thành công không đồng nghĩa có thể rollback dữ liệu.
Trước mọi thay đổi cần ghi: artifact quay lại, schema tối thiểu/tối đa đã test,
phương án dừng scheduler, idempotency và ảnh hưởng request đang chạy. Nếu chưa
có bằng chứng tương thích thì dừng traffic/forward fix, không đoán.

## Gate nghiệm thu staging

- Reset thành công: access JWT trước reset và refresh cũ đều bị từ chối trên
  **mọi instance**; binary rollback ứng viên cũng phải từ chối.
- Retry upload/chat không tạo bản ghi mới sai ý; authorization sender/recipient,
  block/delete vẫn đúng sau migration và sau restart.
- Client đang phát hành hiển thị TEXT/MOMENT/MOMENT_REPLY đúng; client cũ được
  thử riêng hoặc ghi rõ không hỗ trợ, không giả định fallback đã hoạt động.
- Outbox không mất event khi process dừng sau commit; xử lý duplicate an toàn;
  FCM chỉ là invalidation, không đưa nội dung hay thông tin xác thực vào payload.
- Restore thử bằng dữ liệu QA, không reset/xóa DB hoặc file người dùng.

Kết quả gate phải ghi artifact, môi trường, số ca, thời gian và log đã scrub.
Chưa chạy production hoặc mixed APK thật thì ghi NOT RUN, không PASS.
