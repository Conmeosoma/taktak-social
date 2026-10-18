# Bảo trì Taktak sau review 18/10/2026

Quy trình này thay thế hướng dẫn backup/restore dạng thư mục timestamp và `rollback.sh --restore-db` cũ. Script dành cho **systemd + PostgreSQL native trên Ubuntu**, không dùng cho Docker volumes. Chưa chạy thử trên VPS thật; cần nghiệm thu ở staging trước production. Không có quy trình update nào bảo đảm zero-risk hay zero-downtime.

## Quyền và chuẩn bị

- `/var/taktak` và `scripts/`: root:root, 755; script 750, app user không được sửa.
- JAR root:root 644. `.env` root:root 600, systemd đọc EnvironmentFile trước khi chuyển sang user `taktak`.
- `private-images/`: taktak:taktak 700. `backups/`: root:root 700; archive/checksum 600.
- Setup tạo key ngẫu nhiên `/etc/taktak/backup.key`, root:root 600, chỉ tạo khi chưa tồn tại. Lưu bản sao key **tách khỏi VPS và archive**, không đưa vào Git/log/chat. Mất key không giải mã được.
- Cần `bash`, `flock` (util-linux), OpenSSL, tar, coreutils, curl, PostgreSQL client cùng phiên bản phù hợp server. `.env` hỗ trợ các giá trị literal, không nội suy shell hay chạy lệnh; giữ cùng giá trị credentials với systemd.
- Nếu bật FCM: `FCM_ENABLED=true`, `GOOGLE_APPLICATION_CREDENTIALS` trỏ tới Admin credentials thật, file chỉ app user đọc (600). Sai/thiếu credentials sẽ làm startup thất bại có chủ đích; không còn âm thầm bỏ qua push. Tắt rõ `FCM_ENABLED=false` nếu chưa chuẩn bị FCM. Không dùng `google-services.json` phía Android làm Admin credentials.

## Backup nhất quán

Avatar được lưu tại `private-images/avatars/<UUID>.jpg`, dùng cùng snapshot database chứa `users.avatar_path`. Script backup/restore hiện bao gồm subfolder này và kiểm tra cả reference avatar sau restore; giữ cấu trúc thư mục này khi chuyển dữ liệu. Không có migration mới cho avatar.

```bash
sudo bash /var/taktak/scripts/backup.sh
```

Tạm dừng service nếu đang chạy, tạo custom `pg_dump -Fc` và archive ảnh cùng env, mã hóa bằng AES-256-CBC/PBKDF2 rồi mới công bố `backup-<UTC>-<PID>.tar.enc` + `.sha256`. Bản plaintext tạm nằm trong thư mục root-only và được dọn khi kết thúc bình thường/lỗi có trap; operator phải kiểm tra scratch còn lại sau mất điện/SIGKILL. Có downtime. Service được khởi động lại nếu trước đó đang chạy.

Checksum ngoài và manifest trong giúp phát hiện hỏng dữ liệu; **không phải chữ ký xác thực nguồn**. Chỉ restore archive tin cậy do operator quản lý. Giữ archive lẫn checksum ở nơi an toàn ngoài VPS; retention 14 ngày chỉ áp dụng file archive do script tạo.

## Restore có xác nhận

```bash
# Thay placeholder bằng đường dẫn archive thực tế; thao tác này thay dữ liệu hiện tại.
sudo bash /var/taktak/scripts/restore.sh /var/taktak/backups/backup-<UTC>-<PID>.tar.enc
```

Kiểm tra checksum/giải mã/whitelist entry/manifest trước khi dừng service. Gõ `RESTORE` khi đã kiểm tra đúng database và snapshot; `--yes` chỉ dùng khi đã có phê duyệt rõ cho chính dữ liệu đích. Không dùng archive snapshot kiểu cũ với script mới.

Script tạo backup mã hóa trạng thái hiện tại trước khi áp dụng. `pg_restore --clean --if-exists --exit-on-error --single-transaction` chạy trên database đã tồn tại: thất bại SQL rollback transaction. Không restore role/password DB, không tự thay `.env`. Sau đó kiểm tra mọi ảnh active có trong snapshot, đổi thư mục ảnh bằng rename và giữ bản cũ `private-images.before-*`.

Lỗi sau khi áp dụng sẽ thử phục hồi DB trước restore và đổi lại ảnh. Nếu xuất hiện `RECOVERY_REQUIRED`, **không xóa scratch hoặc bản ảnh cũ**, kiểm tra trạng thái DB/service và recovery files với operator. Không tự gọi lại restore liên tục. Thư mục ảnh cũ/failed không tự bị dọn; operator chỉ dọn sau nghiệm thu và backup ngoài VPS.

## Update / rollback JAR

```bash
sudo bash /var/taktak/scripts/update.sh /tmp/taktak-server-new.jar
sudo bash /var/taktak/scripts/rollback.sh
```

Update bắt buộc backup thành công trước khi đổi JAR; copy candidate vào cùng filesystem rồi atomic rename. Mọi lỗi restart/health sau đổi JAR đều thử quay lại JAR cũ và kiểm tra service. `rollback.sh` chỉ đổi JAR, giữ bản vừa thay để điều tra. Nó không restore DB hoặc đảo migration.

Nếu bản mới áp migration không tương thích JAR cũ, **không được coi JAR rollback là khôi phục hoàn chỉnh**. Cần quyết định operator về cặp JAR/schema và snapshot dữ liệu, chấp nhận khoảng dữ liệu có thể mất, rồi mới thực hiện restore riêng. Script không tự reset database hoặc sửa migration đã áp dụng.

## TLS và IP client

`setup-ssl.sh <domain> <email>` dùng HTTP webroot trước khi có certificate, sau đó mới bật config HTTPS. Domain/DNS và port 80 phải sẵn sàng. Có deploy hook reload Nginx sau renew, vẫn phải kiểm tra `certbot renew --dry-run` và timer trên host thật.

Nginx ghi đè `X-Forwarded-For` bằng `$remote_addr`, không nối header do client gửi. Backend prod dùng Tomcat native forwarding và chỉ tin proxy loopback mặc định. Nếu thêm load balancer/CDN hoặc Docker gateway, phải xác định chuỗi proxy tin cậy cụ thể; không đặt regex tin mọi địa chỉ. Biến `TRUSTED_PROXY_REGEX` cho phép khai báo IP/regex chính xác khi cần, backend không được public bypass Nginx. Cần kiểm thử quota với hai IP ngoài và header giả qua Nginx thật ở staging.

## Docker là phương án triển khai riêng

Từ `deploy/docker/`, chuẩn bị `deploy/.env` local với credentials thật và Admin credentials riêng theo chính sách repo, rồi:

```bash
# config --quiet xác minh mà không in secrets đã nội suy ra terminal/log.
docker compose --env-file ../.env -f docker-compose.yml config --quiet
docker compose --env-file ../.env -f docker-compose.yml up -d --build
```

`env_file` của container **không** cấp giá trị cho nội suy `${...}` của Compose CLI; cần `--env-file` như trên. Thiếu DB password/JWT sẽ bị từ chối thay vì dùng mặc định. Không đưa `.env`/Admin JSON vào Git. Docker image chạy user non-root nên cần kiểm tra quyền đọc file credentials bind-mount trên host; không nới quyền public để chữa lỗi. Chưa có runbook backup/restore Docker được nghiệm thu: không dùng script native vào container volumes.

## Bằng chứng hiện có

`bash deploy/tests/maintenance-test.sh`: 11 ca hợp đồng trên fixture riêng. OpenSSL/tar/checksum thực, nhưng systemctl, PostgreSQL, nginx, certbot, chown và flock được mock. Nó **không** xác minh SQL restore thực, Linux permissions/locking, ACME, Docker hay rollback trên VPS. Xem [báo cáo sửa lỗi](../docs/QA_REVIEW_FIXES.md) và thực hiện staging drill trước production.
