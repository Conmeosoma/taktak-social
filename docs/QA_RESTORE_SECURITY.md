# RESTORE-SECURITY-01 — 21/10/2026

> Cập nhật sau phê duyệt mới: script/SQL và systemd drop-in production đã cài,
> daemon-reload/health/PID/hash đạt, không restart hoặc thực thi SQL/restore.
> Xem [báo cáo rollout](QA_RESTORE_PRODUCTION_ROLLOUT.md). Nội dung chưa deploy
> bên dưới là snapshot của lượt sửa + QA trước rollout, không xóa lịch sử.

## Phạm vi và nguyên nhân

Người dùng duyệt sửa restore an toàn và regression JWT/refresh. Lỗi ban đầu:
restore DB cũ đưa security_version9 về7 và có thể tự mở service trước khi
hòa giải bảo mật. Không đổi stack, API, schema/migration, Android hoặc secret.
Không restore DB chính, không triển khai script/unit/JAR vào production.

## Thay đổi

- `deploy/scripts/restore.sh`: yêu cầu ExecStartPre guard đã load trong systemd;
  tạo marker root-only, dừng service, chụp current security state, restore rồi
  reconciliation trong transaction. Luôn giữ cách ly sau restore/rollback.
- `deploy/scripts/reconcile-restore.sql`: version=max(snapshot,current)+1;
  giữ current password hash/email/username/email_verified_at; revoke mọi refresh
  trong snapshot và xóa recovery challenges. Unknown restored account hoặc
  BIGINT overflow làm transaction fail, không hồi sinh account bằng suy đoán.
- `deploy/systemd/taktak.service`: ConditionPathExists và ExecStartPre chặn
  marker `.restore-quarantine`. Không chỉ dựa vào người vận hành nhớ dừng app.
- `setup-vps.sh` cài thêm SQL root:root640; native QA harness, shell mocks và
  các ca hợp đồng được cập nhật theo hành vi cách ly mới.
- Runbook MAINTENANCE và các hướng dẫn deploy liên quan dẫn về quy trình mới.

Không tạo cờ bypass, không tự xoay signing key hoặc đổi mật khẩu cá nhân.
Chỉ operator được duyệt mới review RPO/quyền ảnh/block/dữ liệu đã xóa/outbox,
schema/JAR và auth, rồi bỏ marker đúng đích để mở lại. Mất current security
state cần kế hoạch DR riêng, không force restore DB rỗng. Dừng mọi writer/
instance và cách ly ingress trước vận hành thật; script không điều phối cluster.

## Kiểm thử mới

Trạng thái phạm vi sửa + QA: **PASS**, không đồng nghĩa rollout production đạt.
Backend full `mvn -o verify`: **191/191**, 0 failure/error/skipped,
BUILD SUCCESS2 phút28 giây, kết thúc20:27:57 Asia/Saigon. Log
`.local/artifacts/acceptance-all/restore-backend-verify-final.log`.
Lượt verify trước bị PowerShell ErrorAction=Stop ngắt khi JVM in cảnh báo stderr;
đã chạy lại và kiểm tra native exit code, không bỏ test hoặc assertion.

### PostgreSQL + HTTP thật local: PASS4/4

`mvn -o -Dtest=RestoreSecurityIntegrationTest test`, BUILD SUCCESS24,458s;
test19,81s. Embedded PostgreSQL riêng, HTTP loopback và Spring Security thật,
không mock JwtDecoder hoặc refresh API; dùng chính SQL deploy trong repository.

1. Seed snapshot version7, reset mô phỏng lên9/password mới, ghi current state.
   Hạ về snapshot7/old password/unrevoke refresh như restore cũ. JWT cũ trả200
   chứng minh baseline đỏ. Sau SQL, version10; JWT7 và JWT9 trả401; cả hai refresh
   trả401; mật khẩu cũ401, mật khẩu mới login/me200; recovery cũ bị xóa, email
   verification hiện tại giữ nguyên. Reset API tự nó vẫn do RecoveryIntegrationTest
   xác minh, không gọi SMTP hoặc reset tài khoản thật trong ca này.
2. Unknown restored account làm SQL fail atomically, không đổi version user khác.
3. BIGINT overflow fail atomically, không wrap về0 hoặc bỏ đối chiếu.
4. Snapshot version12 cao hơn current7 vẫn tăng lên13.

Lượt đầu lỗi fixture do bảng tạm còn trên physical Hikari connection; đã sửa
DROP pg_temp table và cô lập dữ liệu mỗi ca, không bỏ assertion. Log đầu giữ
`restore-security-tests.log`, log cuối `restore-security-tests-final.log`.

### Shell contract: PASS12/12

`bash deploy/tests/maintenance-test.sh`, exit0. Crypto/archive/checksum thật;
systemctl/DB/permissions/locking mock. Kiểm tra encrypted backup, restore cách
ly, corrupt archive không đổi dữ liệu/service, SQL failure, reconciliation
failure rollback vẫn cách ly, thiếu loaded guard từ chối trước thay đổi,
update/rollback regression và unsafe storage entry. Không coi mock là bằng
chứng PostgreSQL hoặc Linux systemd thực.

Log `.local/artifacts/acceptance-all/restore-maintenance-final.log`.

### VPS QA native: PASS theo assertion thực

Chỉ copy candidate vào staging QA root-only và thư mục QA mới, không thay script
hoặc unit live. DB mới từ schema/history, user/ảnh synthetic, không sao chép dữ
liệu cá nhân. Dùng pg_dump/pg_restore/OpenSSL/tar/psql và systemd thật.

- QA root `/var/taktak-qa-20261021-nn9TJQKX`, DB `taktak_qa_a76cf525301d`.
- Unit QA ở `/run/systemd/system/`, chỉ chạy `sleep infinity`, không phải JVM
  Spring Boot/HTTP/FCM. Dừng sau diễn tập; không enable unit vào boot.
- Corrupt archive bị checksum từ chối, version9/service QA không đổi.
- Native restore DB/JPEG đúng SHA-256/caption; hai refresh đều revoked, kể cả
  refresh active lúc backup; latest synthetic credential giữ nguyên.
- **Version9→10**; marker tồn tại, service inactive. Gọi systemctl start vẫn
  không chạy: PASS_SYSTEMD_QUARANTINE_BLOCKS_MANUAL_START.
- Kết thúc PASS_REVOCATION_HIGH_WATER_MARK; live taktak.service active, healthUP.

Lượt QA trước bị từ chối guard vì Ubuntu in `Conditions=[unprintable]`; không
đổi DB ở bước này. Đổi sang xác minh structured ExecStartPre đã load, không
bỏ guard. Các tài nguyên QA/backup/scratch phục vụ điều tra được giữ, không
tự drop database/purge. Native QA không có HTTP Spring Boot; HTTP token proof
là bài local riêng phía trên, không giả là đã test JWT qua restore native VPS.

## Giới hạn và bàn giao

Chưa cài bản sửa lên script/unit production, chưa restore dữ liệu cá nhân hoặc
thử binary rollback cũ. Không chấp nhận binary bỏ `sv`. Cổng lint/launcher/
crash outbox/mixed APK/load dài/alert routing còn nguyên ở QA_ACCEPTANCE_ALL;
sửa cổng restore không có nghĩa toàn dự án PASS. Không commit/push.

Skills acceptance-orchestrator/verification-before-completion yêu cầu bằng
chứng độc lập cho SQL/HTTP, shell contract và native QA; không gom thành một
nhãn PASS production. Không chạy emulator/USB vì không thay Android.
