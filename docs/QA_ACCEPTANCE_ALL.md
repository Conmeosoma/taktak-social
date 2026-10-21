# Nghiệm thu tổng hợp — 21/10/2026

> Bổ sung sau khi người dùng duyệt sửa: RESTORE-SECURITY-01 đã có bản sửa và
> kiểm thử SQL/HTTP4, shell12 và native QA9→10/systemd guard đạt. Chưa rollout
> production; các kết quả9→7 dưới đây giữ làm baseline lịch sử. Chi tiết tại
> [QA_RESTORE_SECURITY.md](QA_RESTORE_SECURITY.md). Gate khác không tự đổi PASS.

## Kết luận

**FAIL cổng tổng thể**, không phải toàn bộ ứng dụng không chạy. Kết quả mới
dưới đây thay thế trạng thái chưa xác minh trước đó, không xóa lịch sử.
Người dùng duyệt chuyển QA USB, bật FCM/restart có rollback cấu hình và tạo
database/thư mục QA riêng. Không restore/reset DB chính, đổi JAR/migration,
commit hoặc push trong lượt này.

## Kết quả thực tế

| Hạng mục | Kết quả và phạm vi |
| --- | --- |
| Backend `mvn -o verify` | PASS 187 test, 0 failure/error/skipped; BUILD SUCCESS, 3 phút 13 giây |
| Android JVM | PASS 127 test; không suy ra instrumentation toàn app đạt |
| APK | Task debug, androidTest, unsigned release đã build; release origin QA, không phải artifact phát hành |
| Lint | FAIL debug 179 lỗi/90 cảnh báo, release 179 lỗi/98 cảnh báo; toàn bộ lỗi MissingTranslation, không cộng trùng hai variant |
| USB History/Friends + Chat theme matrix | PASS 2/2, 25,184 giây; chỉ matrix đã chọn |
| USB Chat fixture regression | PASS 6/6, 42,111 giây; không phải tin nhắn thật hai máy |
| Gmail TLS/SMTP connection | PASS 1/1, không gửi thư ở ca kết nối |
| Recovery Gmail thật | PASS 1/1: SMTP nhận 3 thư QA, API reset/revocation đạt; người dùng xác nhận nhận đủ mã xác minh, mật khẩu tạm, thông báo đổi mật khẩu |
| FCM/Worker/AppWidgetHost thật | PASS 1/1, 70,674 giây; foreground/background/delete/logout assertion; không giả event/Worker |
| VPS cuối lượt | service active, health UP, FCM_ENABLED=true; credential 600 taktak:taktak; `nginx -t` đạt |
| Load fixture local | PASS 1/1; 240 request đo, concurrency 4, không lỗi; inbox 160 statement/40 request = 4/request |
| Backup hỏng | PASS checksum bị từ chối, DB QA vẫn version 9 |
| Native encrypted DB/image restore | PASS ảnh đúng SHA-256, caption phục hồi, refresh revoked trước backup vẫn revoked |
| Revocation phát sinh sau backup | **FAIL: security_version trước restore 9, sau restore 7** |

## FCM và ảnh QA

SSH strict host-key checking đã xác thực thành công. Thực tế FCM_ENABLED=false
và đường dẫn credential được cấu hình nhưng file không tồn tại. Đã đối chiếu
project Android/Admin JSON, chuyển qua staging root-only, cài credential600 cho
service user rồi bật FCM theo duyệt. Không ghi secret vào Git/báo cáo.

Lượt đầu health timeout30s tự rollback; journal cho thấy startup56–58s. Tăng
wait120s rồi bật lại đạt. Bản `.env` cũ giữ root-only. Chỉ bản credential staging
tạm đã xóa; bản gốc local và bản cài service còn giữ. Không thay JAR/schema.

Lượt USB sau bật FCM đã nhận push nhưng harness inflate RemoteViews qua
AppCompat Activity gây lỗi AppCompatImageView/setImageBitmap. Đổi androidTest
sang application Context/framework inflater như launcher; build đạt15s,
chạy lại1/1 đạt. Không sửa widget production để né lỗi harness. Outbox cuối:
ACCEPTED5/CANCELLED2; ACCEPTED chỉ là Firebase chấp nhận, không bảo đảm delivery
mọi event; CANCELLED có thể thuộc lượt QA cũ đã hủy registration.

Đã review foreground/background/deleted-empty. Ảnh logout chỉ có nền trống dù
assertion container logout VISIBLE đạt: **chưa nghiệm thu hình ảnh logout**,
cần kiểm tra frame/layout. AppWidgetHost QA thật chưa thay thế widget2×2 ghim
launcher MIUI, reboot hoặc force-stop. Bind grant tạm đã thu hồi, QA logout;
người dùng tự đăng nhập lại tài khoản cá nhân. Hai ảnh lượt thành công đã xóa
đúng ID; QA accounts và tài nguyên từ các lượt cũ còn giữ, không purge theo prefix.

## Restore: cổng bảo mật chưa đạt

Chạy backup.sh/restore.sh thực trên DB mới từ schema/Flyway history, chỉ seed
người dùng/ảnh synthetic. Backup mã hóa, chỉnh version7→9 sau backup rồi restore
cho kết quả7. Auth so sánh claim `sv` với DB; mất high-water mark có nguy cơ
chấp nhận lại JWT cũ. **Chưa chạy HTTP để khẳng định token cụ thể được chấp nhận**.

Đây là lỗi triển khai so với invariant đã khóa trong
[kế hoạch tương thích](CLIENT_SERVER_COMPATIBILITY.md). restore.sh hiện có thể
khởi động service trước reconciliation. Không dùng để đưa DB snapshot cũ ra
public khi chưa có bản sửa/test an toàn. Không tự rotate key, reconcile password
hoặc revoke phiên cá nhân trong lượt nghiệm thu này.

QA hoàn tất tại `/var/taktak-qa-20261021-wSlcjMgm`, DB `taktak_qa_e3411b9c93b2`.
Các lần chuẩn bị thất bại còn tài nguyên QA riêng, không tự drop/purge.
Không mở QA HTTP process vào snapshot vừa restore. Service/DB chính vẫn hoạt
động. Harness chuẩn hóa CRLF ở bản sao QA của scripts Windows để chạy Linux,
không sửa script live. Raw remote output được quan sát trong phiên SSH;
không tải `.env`/backup chứa credentials về artifact.

## Còn thiếu nghiệm thu

- Sửa restore fail-closed/reconciliation; chứng minh access/refresh bị thu hồi
  vẫn bị từ chối sau restore và binary rollback ứng viên.
- Outbox process crash thật tại commit/lease/send và recovery sau restart.
- Ma trận APK cũ/mới và binary rollback thật, không chỉ Gson bỏ qua field.
- Chat/reply hai tài khoản qua VPS và UI thật; fixture USB chưa đủ.
- Widget ghim launcher, hình ảnh logout/OEM/reboot; TalkBack/RTL/accessibility
  toàn app, các vấn đề contrast từng ghi nhận; lint MissingTranslation.
- Load staging dài với ảnh500–800KB, profiling SQL/storage/pool; fixture local
  ảnh nhỏ không chứng minh capacity/SLO VPS production.
- Alert routing/dashboard thực; retention/xóa tự động, replay DEAD và ngưỡng
  SLO cần quyết định vận hành riêng. Không tự đặt chính sách dữ liệu.
- Gmail local đã nhận thật; không suy ra service VPS đã cấu hình SMTP đúng.

Ưu tiên tiếp theo: sửa an toàn restore và regression token runtime trước khi
mở traffic. Chưa đủ điều kiện đánh dấu toàn bộ DONE.

## Bằng chứng và bàn giao

Git-ignore `.local/artifacts/acceptance-all/`: backend-verify.log,
android-build-lint.log, layout-matrix.log, chat-regression.log, load.log,
widget-context-build.log, real-fcm-enabled.log (harness lỗi),
real-fcm-widget-framework.log (1/1), fcm-widget-images/.
Gmail: `.local/artifacts/recovery-gmail-connection.log` và
`.local/artifacts/recovery-gmail-acceptance.log`; inbox do người dùng xác nhận.
Load JSON: `server/target/system-load-qa/acceptance-all.json`.

File lượt này: RealFcmWidgetAcceptanceTest.java (framework Context),
scripts/qa/enable-fcm.sh, scripts/qa/backup-restore-drill.sh và tài liệu QA,
PROJECT_STATUS/TASKS/SESSION_HANDOFF. Bảo toàn các thay đổi có sẵn và báo cáo
của người dùng. Skills acceptance-orchestrator và android_ui_verification
hướng dẫn tách gate/bằng chứng và review ảnh; không dùng fixture thay E2E thật.
Mật khẩu SSH từng xuất hiện trong chat nên người dùng cần đổi; không tự đổi.
