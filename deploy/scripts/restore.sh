#!/usr/bin/env bash
# Restore có xác nhận, kiểm tra trước, transaction PostgreSQL và rollback khi lỗi.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
[[ $# -ge 1 ]] || die 'Usage: restore.sh <backup-file.tar.enc> [--yes]'
load_database; require_key; lock_operations
[[ ! -e "$APP_ROOT/.restore-quarantine" && ! -L "$APP_ROOT/.restore-quarantine" ]] || die 'Restore quarantine requires operator review first.'
[[ -f "$SCRIPT_DIR/reconcile-restore.sql" ]] || die 'Missing restore security reconciliation.'
ARCHIVE="$(realpath -e -- "$1")"
[[ -f "$ARCHIVE" && ! -L "$1" && -f "$ARCHIVE.sha256" ]] || die 'Missing backup or checksum.'
EXPECTED="$(awk 'NR==1 {print $1}' "$ARCHIVE.sha256")"
[[ "$EXPECTED" =~ ^[a-f0-9]{64}$ ]] || die 'Invalid checksum.'
ACTUAL="$(sha256sum "$ARCHIVE")"; [[ "${ACTUAL%% *}" = "$EXPECTED" ]] || die 'Backup checksum mismatch.'
make_stage
DB_APPLIED=0; OLD_IMAGES=0; NEW_IMAGES=0; COMPLETE=0
BEFORE_IMAGES="$APP_ROOT/private-images.before-$(date -u +%Y%m%dT%H%M%S)-$$"
finish() {
    local result=$? recovered=1
    trap - EXIT
    if [[ "$COMPLETE" != 1 && "$DB_APPLIED" = 1 ]]; then
        systemctl stop "$SERVICE" || true
        database_restore "$STAGE/before.dump" || recovered=0
        if [[ "$NEW_IMAGES" = 1 ]]; then
            mv -- "$IMAGE_DIR" "$APP_ROOT/private-images.failed-$$" || recovered=0
        fi
        if [[ "$OLD_IMAGES" = 1 ]]; then mv -- "$BEFORE_IMAGES" "$IMAGE_DIR" || recovered=0; fi
    fi
    # Khong tu mo traffic, ke ca khi rollback thanh cong. Giu marker systemd.
    if [[ "$recovered" = 1 ]]; then remove_stage
    else echo "RECOVERY_REQUIRED: service/data need inspection; temporary recovery files retained at $STAGE" >&2; fi
    exit "$result"
}
trap finish EXIT
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass stdin -in "$ARCHIVE" -out "$STAGE/bundle.tar" < "$KEY_FILE"
# Không giải nén archive bất kỳ trước khi kiểm tra tên và kiểu entry.
[[ "$(tar -tf "$STAGE/bundle.tar" | sort)" = "$(printf '%s\n' CONTENTS.sha256 app.env db.dump private-images.tar.gz | sort)" ]] || die 'Unexpected archive entries.'
[[ -z "$(tar -tvf "$STAGE/bundle.tar" | awk 'substr($0,1,1)!="-" {print}')" ]] || die 'Links/non-regular archive entries rejected.'
tar -xf "$STAGE/bundle.tar" --no-same-owner --no-same-permissions -C "$STAGE" db.dump private-images.tar.gz app.env CONTENTS.sha256
for name in db.dump private-images.tar.gz app.env; do
    EXPECTED="$(awk -v name="$name" '{entry=$2; sub(/^\*/, "", entry); if(entry==name) print $1}' "$STAGE/CONTENTS.sha256")"
    [[ "$EXPECTED" =~ ^[a-f0-9]{64}$ ]] || die 'Invalid contents manifest.'
    ACTUAL="$(sha256sum "$STAGE/$name")"; [[ "${ACTUAL%% *}" = "$EXPECTED" ]] || die 'Content checksum mismatch.'
done
pg_restore --list "$STAGE/db.dump" >/dev/null
while IFS= read -r name; do
    [[ "$name" = private-images/ || "$name" = private-images/avatars/ || "$name" =~ ^private-images/(avatars/)?[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\.jpg$ ]] || die 'Unsafe image entry.'
done < <(tar -tzf "$STAGE/private-images.tar.gz")
[[ -z "$(tar -tvzf "$STAGE/private-images.tar.gz" | awk 'substr($0,1,1)!="-" && substr($0,1,1)!="d" {print}')" ]] || die 'Image links rejected.'
tar -xzf "$STAGE/private-images.tar.gz" --no-same-owner --no-same-permissions -C "$STAGE"
[[ -d "$STAGE/private-images" && -d "$IMAGE_DIR" && ! -L "$IMAGE_DIR" ]] || die 'Image directory missing.'
if [[ "${2:-}" != --yes ]]; then
    read -r -p "Restore database $DB_NAME and private images? Type RESTORE: " answer
    [[ "$answer" = RESTORE ]] || die 'Restore cancelled.'
fi
systemctl show "$SERVICE" --property=ExecStartPre --value | grep -Fq \
    "path=/usr/bin/test ; argv[]=/usr/bin/test ! -e $APP_ROOT/.restore-quarantine ; ignore_errors=no" \
    || die 'Loaded systemd restore quarantine guard is required.'
( set -o noclobber; printf 'RESTORE_REVIEW_REQUIRED\n' > "$APP_ROOT/.restore-quarantine" )
systemctl stop "$SERVICE"
if systemctl is-active --quiet "$SERVICE"; then die 'Service still active; restore refused.'; fi
# Giữ bản trước restore đã mã hóa; không tự thay secrets bằng app.env trong backup.
bash "$SCRIPT_DIR/backup.sh"
database_dump "$STAGE/before.dump"
PGPASSWORD="$DB_PASSWORD" psql -X -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q \
    -c 'COPY (SELECT id,security_version,password_hash,email,username,email_verified_at FROM users) TO STDOUT WITH CSV' > "$STAGE/security-before.csv"
database_restore "$STAGE/db.dump"
DB_APPLIED=1
# Credential khong xuat hien trong argv/log; import va reconciliation atomic.
(
    printf 'CREATE TEMP TABLE restore_security_before (id uuid PRIMARY KEY, security_version bigint NOT NULL CHECK (security_version>=0), password_hash varchar(255) NOT NULL, email varchar(254) NOT NULL, username varchar(255) NOT NULL, email_verified_at timestamptz);\n'
    printf '\\copy restore_security_before FROM STDIN WITH CSV\n'
    cat "$STAGE/security-before.csv"
    printf '\\.\n'
    cat "$SCRIPT_DIR/reconcile-restore.sql"
) | PGPASSWORD="$DB_PASSWORD" psql -X -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -q --single-transaction -v ON_ERROR_STOP=1 > /dev/null 2> "$STAGE/reconcile-error.log" \
    || die 'Security reconciliation failed; rolling back while quarantined.'
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -At \
    -c "SELECT file_path FROM moments WHERE deleted_at IS NULL UNION ALL SELECT 'avatars/' || avatar_path FROM users WHERE avatar_path IS NOT NULL" > "$STAGE/active-images.txt"
while IFS= read -r name; do
    [[ "$name" =~ ^(avatars/)?[a-f0-9-]{36}\.jpg$ && -f "$STAGE/private-images/$name" ]] || die 'Database references a missing/unsafe image.'
done < "$STAGE/active-images.txt"
mv -- "$IMAGE_DIR" "$BEFORE_IMAGES"; OLD_IMAGES=1
mv -- "$STAGE/private-images" "$IMAGE_DIR"; NEW_IMAGES=1
chmod 700 "$IMAGE_DIR"; chown -R taktak:taktak "$IMAGE_DIR"
COMPLETE=1
echo "RESTORE_QUARANTINED: data restored and sessions revoked; service remains stopped. Review before removing $APP_ROOT/.restore-quarantine. Previous images: $BEFORE_IMAGES"
