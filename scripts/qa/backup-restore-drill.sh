#!/usr/bin/env bash
# Diễn tập thật trên database/thư mục mới; chỉ đọc schema/history từ DB đang chạy.
set -euo pipefail
umask 077
[[ $(id -u) == 0 ]] || exit 1
source_dir=$(cd "$(dirname "$0")" && pwd)
suffix=$(openssl rand -hex 6)
qa=$(mktemp -d /var/taktak-qa-20261021-XXXXXXXX)
db=taktak_qa_$suffix
[[ $qa == /var/taktak-qa-20261021-* && $db == taktak_qa_* ]] || exit 1
export TAKTAK_ROOT=$qa TAKTAK_SERVICE=taktak-qa-$suffix TAKTAK_BACKUP_KEY_FILE=$qa/backup.key
[[ -z $(sudo -u postgres psql -Atc "select datname from pg_database where datname='$db'") ]] || exit 1
db_user=taktak; db_password=
while IFS='=' read -r name value; do
    case "$name" in DB_USERNAME|DB_PASSWORD) ;; *) continue;; esac
    value=${value%$'\r'}
    if [[ $value == \"*\" || $value == \'*\' ]]; then value=${value:1:${#value}-2}; fi
    case "$name" in DB_USERNAME) db_user=$value;; DB_PASSWORD) db_password=$value;; esac
done < /var/taktak/.env
[[ $db_user =~ ^[a-z_][a-z0-9_]*$ && -n $db_password ]] || exit 1
sudo -u postgres createdb --owner="$db_user" "$db"
printf 'DB_URL=jdbc:postgresql://127.0.0.1:5432/%s\nDB_USERNAME=%s\nDB_PASSWORD=%s\nIMAGE_STORAGE_ROOT=%s/private-images\n' "$db" "$db_user" "$db_password" "$qa" > "$qa/.env"
db_password=
mkdir -p "$qa/scripts" "$qa/private-images"
cp -- "$source_dir/common.sh" "$source_dir/backup.sh" "$source_dir/restore.sh" "$source_dir/reconcile-restore.sql" "$qa/scripts/"
# Ban sao QA tu checkout Windows: chuan hoa CRLF, khong sua script live.
sed -i 's/\r$//' "$qa/scripts/common.sh" "$qa/scripts/backup.sh" "$qa/scripts/restore.sh" "$qa/scripts/reconcile-restore.sql"
# Unit QA rieng khong HTTP/FCM; kiem tra stop va guard systemd that.
unit=/run/systemd/system/$TAKTAK_SERVICE.service
[[ ! -e $unit ]] || exit 1
printf '[Unit]\nConditionPathExists=!%s/.restore-quarantine\n[Service]\nExecStartPre=/usr/bin/test ! -e %s/.restore-quarantine\nExecStart=/usr/bin/sleep infinity\n' "$qa" "$qa" > "$unit"
chmod 644 "$unit"
systemctl daemon-reload
systemctl start "$TAKTAK_SERVICE"
trap 'systemctl stop "$TAKTAK_SERVICE"' EXIT
openssl rand -base64 32 > "$qa/backup.key"
sudo -u postgres pg_dump -d taktak --schema-only --no-owner --no-acl > "$qa/schema.sql"
sudo -u postgres pg_dump -d taktak --data-only --no-owner --no-acl -t public.flyway_schema_history > "$qa/history.sql"
sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q -c 'drop schema public;' > /dev/null
if ! grep -q '^CREATE SCHEMA public;' "$qa/schema.sql"; then
    sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q -c "create schema public authorization $db_user;" > /dev/null
fi
{ printf 'SET ROLE "%s";\n' "$db_user"; cat "$qa/schema.sql"; } | sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q > /dev/null
sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q < "$qa/history.sql" > /dev/null
# Chỉ cấp quyền trong DB QA vừa tạo, không REASSIGN OWNED trên cluster đang dùng.
sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q -c "grant all on all tables in schema public to $db_user; grant all on all sequences in schema public to $db_user;" > /dev/null
image=00000000-0000-4000-8000-000000000033.jpg
cp -- "$source_dir/qa-fixture.jpg" "$qa/private-images/$image"
size=$(stat -c %s "$qa/private-images/$image")
expected=$(sha256sum "$qa/private-images/$image" | cut -d' ' -f1)
sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q <<SQL
insert into users(id,email,username,password_hash,display_name,security_version) values
('00000000-0000-4000-8000-000000000011','sender@example.test','qa_sender','QA noncredential hash','QA Sender',7),
('00000000-0000-4000-8000-000000000022','receiver@example.test','qa_receiver','QA noncredential hash','QA Receiver',7);
insert into refresh_tokens(id,user_id,token_hash,expires_at,revoked_at,created_at) values
('00000000-0000-4000-8000-000000000044','00000000-0000-4000-8000-000000000011','QA hash',now()+interval '1 day',now(),now());
insert into refresh_tokens(id,user_id,token_hash,expires_at,revoked_at,created_at) values
('00000000-0000-4000-8000-000000000055','00000000-0000-4000-8000-000000000011','QA active hash',now()+interval '1 day',null,now());
insert into moments(id,sender_id,caption,file_path,mime_type,byte_size,created_at) values
('00000000-0000-4000-8000-000000000033','00000000-0000-4000-8000-000000000011','QA before backup','$image','image/jpeg',$size,now());
insert into moment_recipients(moment_id,user_id,created_at) values
('00000000-0000-4000-8000-000000000033','00000000-0000-4000-8000-000000000022',now());
SQL
bash "$qa/scripts/backup.sh"
archive=$(find "$qa/backups" -maxdepth 1 -name 'backup-*.tar.enc' -type f | head -1)
[[ -n $archive ]] || exit 1
sudo -u postgres psql -d "$db" -v ON_ERROR_STOP=1 -q -c "update users set security_version=9,password_hash='QA latest noncredential hash'; update moments set caption='QA after backup';" > /dev/null
printf 'QA changed bytes' > "$qa/private-images/$image"
cp -- "$archive" "$qa/backups/corrupt.tar.enc"
cp -- "$archive.sha256" "$qa/backups/corrupt.tar.enc.sha256"
truncate -s -1 "$qa/backups/corrupt.tar.enc"
if bash "$qa/scripts/restore.sh" "$qa/backups/corrupt.tar.enc" --yes; then
    echo 'FAIL_CORRUPT_BACKUP_ACCEPTED'; exit 1
fi
[[ $(sudo -u postgres psql -d "$db" -Atc 'select min(security_version) from users') == 9 ]] || exit 1
echo 'PASS_CORRUPT_BACKUP_REJECTED_WITHOUT_DB_CHANGE'
systemctl is-active --quiet "$TAKTAK_SERVICE"
bash "$qa/scripts/restore.sh" "$archive" --yes
[[ -f $qa/.restore-quarantine ]] || exit 1
if systemctl is-active --quiet "$TAKTAK_SERVICE"; then echo FAIL_SERVICE_RESTARTED; exit 1; fi
systemctl start "$TAKTAK_SERVICE"
if systemctl is-active --quiet "$TAKTAK_SERVICE"; then echo FAIL_QUARANTINE_BYPASSED; exit 1; fi
echo 'PASS_SYSTEMD_QUARANTINE_BLOCKS_MANUAL_START'
[[ $(sha256sum "$qa/private-images/$image" | cut -d' ' -f1) == "$expected" ]] || exit 1
[[ $(sudo -u postgres psql -d "$db" -Atc "select count(*) from moments where caption='QA before backup'") == 1 ]] || exit 1
[[ $(sudo -u postgres psql -d "$db" -Atc 'select count(*) from refresh_tokens where revoked_at is not null') == 2 ]] || exit 1
[[ $(sudo -u postgres psql -d "$db" -Atc "select count(*) from users where password_hash='QA latest noncredential hash'") == 2 ]] || exit 1
echo 'PASS_NATIVE_ENCRYPTED_DB_IMAGE_RESTORE_AND_EXISTING_REVOCATION'
version=$(sudo -u postgres psql -d "$db" -Atc 'select min(security_version) from users')
echo "QA_ROOT=$qa QA_DATABASE=$db"
echo "REVOCATION_VERSION_BEFORE_RESTORE=9 AFTER_RESTORE=$version"
if (( version != 10 )); then
    echo 'FAIL_RESTORE_LOWERS_REVOCATION_HIGH_WATER_MARK; DO_NOT_OPEN_TRAFFIC'
    exit 3
fi
echo 'PASS_REVOCATION_HIGH_WATER_MARK'
