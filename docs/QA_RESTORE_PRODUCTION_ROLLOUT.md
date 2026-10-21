# RESTORE-SECURITY-01 — Production rollout, 21/10/2026

## Kết quả: PASS trong phạm vi cài đặt

Người dùng yêu cầu cập nhật script/SQL và unit systemd production. Đã triển
khai đúng ba file sau, không chạy restore hoặc SQL vào database:

| File production | Quyền |
| --- | --- |
| `/var/taktak/scripts/restore.sh` | root:root750 |
| `/var/taktak/scripts/reconcile-restore.sql` | root:root640 |
| `/etc/systemd/system/taktak.service.d/30-restore-quarantine.conf` | root:root644 |

Unit gốc không bị ghi đè; dùng drop-in để giữ toàn bộ cấu hình production hiện
có. Drop-in thêm ConditionPathExists và ExecStartPre chặn `.restore-quarantine`.
Đã `systemctl daemon-reload`, không stop/start/restart Taktak. Không cập nhật
JAR, `.env`, credential Firebase, Nginx, schema/migration, database hoặc ảnh.
Không revoke phiên hoặc tạo marker cách ly trên production trong lượt này.

## Preflight và triển khai

- Đọc AGENTS/project/decisions, kiểm tra Git dirty và bảo toàn thay đổi có sẵn.
- SSH strict host-key checking, xác thực bằng thông tin người dùng đã cung cấp;
  không ghi credential vào script/log/Git.
- Service trước cập nhật active, healthUP, PID84167; chưa có drop-in hoặc marker.
- Đọc restore production trước ghi đè, pin checksum bản cũ. `common.sh` và
  `backup.sh` production khớp bản đã dùng trong QA, không thay hai file này.
- Chạy lại shell contract **12/12**, exit0. Log local Git-ignore:
  `.local/artifacts/acceptance-all/restore-production-preflight.log`.
- Installer dùng maintenance lock, kiểm tra symlink/collision, chuẩn hóa CRLF
  ở candidate rồi so SHA-256 với source đã review. `bash -n` đạt.
- `systemd-analyze verify` candidate gộp base unit + drop-in đạt trước cài.
- Sau cài, kiểm tra loaded ExecStartPre không ignore_errors, quyền file, hash
  của ba file và base unit/JAR/env không đổi. Guard predicate chạy được bằng
  user taktak khi marker không có.

Backend191/191, SQL+HTTP4/4 và native QA version9→10/systemd guard là bằng chứng
của lượt trước tại [QA_RESTORE_SECURITY.md](QA_RESTORE_SECURITY.md), **không chạy
lại Maven/Android hoặc native restore trong lượt rollout này**.

## Bằng chứng runtime mới

- Installer in `HEALTH_SAMPLE_1` đến `HEALTH_SAMPLE_4=UP_PID_UNCHANGED`, ba khoảng
  chờ10 giây giữa mẫu. Đây không phải theo dõi dài hạn15 phút/1 giờ.
- `RESTORE_SECURITY_PRODUCTION_INSTALLED`, không cần rollback trong lượt này.
- Kiểm tra độc lập sau installer: MainPID84167, ActiveEnterTimestamp vẫn
  `2026-10-21 12:47:56 UTC`; DropInPaths chứa đúng file guard, NeedDaemonReload=no.
- Health GET loopback trả `{"status":"UP"}`.
- Kiểm tra cuối sau cập nhật hồ sơ: service active/PID84167/NeedDaemonReload=no,
  healthUP, GET `/api/users/me` không JWT trả401. Backup directory700 root:root.
- ExecStartPre đã load nhưng chưa chạy với main process hiện tại: guard áp dụng
  cho lần start tiếp theo, không thay đổi hành vi của process đang chạy.
- Không đặt marker để thử chặn start trên production và không restart chỉ để
  thử guard; hành vi chặn được kiểm chứng trên unit QA riêng ở lượt trước.

SHA-256 file đã cài (UTF-8/LF):

```text
67e5fcc748990f549444e65d3883d6888e017caa3056980f84e90916390af7a0 restore.sh
cd261b345e6f8216bb7c1d634e824dcee3c0be620bf0d6cb6d213efff2de5482 reconcile-restore.sql
54161988c6a5c15d81a049db543b3194d8e69e3b85d809519d4970747a5f2a0e 30-restore-quarantine.conf
```

## Backup và rollback

Backup cấu hình tại
`/var/taktak/maintenance-backups/restore-security-20261021-aJiHvBQh`, root-only.
Giữ restore.sh cũ, unit gốc và bộ candidate. Không tạo DB backup trong lượt này
vì không đổi dữ liệu; không chạy backup.sh gây downtime ngoài phạm vi.

Installer có rollback script/drop-in khi kiểm tra thất bại, không rollback DB
hay restart process. Hook rollback chưa phải ca failure injection production.
Không tự quay lại restore cũ (có lỗi revocation) hoặc xóa bản backup. Installer
là thao tác một lần, pin đúng state trước rollout; chạy lại sẽ bị từ chối thay
vì ghi đè file đã thay đổi.

## File local và bàn giao

Thêm `deploy/systemd/taktak.service.d/30-restore-quarantine.conf`,
`scripts/qa/install-restore-security-production.sh`, báo cáo này; cập nhật
PROJECT_STATUS, TASKS, SESSION_HANDOFF, QA_RESTORE_SECURITY, MAINTENANCE.
Không commit/push. Skill deployment-procedures hướng dẫn backup/verify/rollback;
verification-before-completion yêu cầu evidence runtime thay vì suy ra từ file.

Chưa thực hiện production restore hoặc mở traffic sau một restore. Khi cần,
phải duyệt riêng dữ liệu đích, RPO, cách ly mọi writer/ingress, review quyền ảnh/
block/outbox/dữ liệu xóa và tương thích JAR rồi mới làm theo
[runbook](../deploy/MAINTENANCE.md#restore-có-xác-nhận). Không có thao tác bắt buộc
người dùng phải làm ngay sau rollout này. Các gate lint/launcher/crash outbox/
mixed APK/load/alert còn nguyên, không đánh dấu toàn dự án PASS.
