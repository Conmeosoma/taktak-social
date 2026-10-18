#!/usr/bin/env bash
# Backup systemd/native PostgreSQL: DB + ảnh + env, mã hóa khi lưu.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
load_database; require_key; lock_operations
[[ -d "$IMAGE_DIR" && ! -L "$IMAGE_DIR" ]] || die 'Invalid private image directory.'
command -v pg_dump >/dev/null; command -v pg_restore >/dev/null
make_stage
WAS_ACTIVE=0
finish() {
    local result=$?
    trap - EXIT
    if [[ "$WAS_ACTIVE" = 1 ]]; then
        systemctl start "$SERVICE" || result=1
    fi
    remove_stage
    exit "$result"
}
trap finish EXIT
if systemctl is-active --quiet "$SERVICE"; then WAS_ACTIVE=1; systemctl stop "$SERVICE"; fi
database_dump "$STAGE/db.dump"
pg_restore --list "$STAGE/db.dump" >/dev/null
tar -czf "$STAGE/private-images.tar.gz" -C "$APP_ROOT" private-images
# Không công bố snapshot mà restore sẽ từ chối (vd. symlink hoặc file tạm sau crash).
while IFS= read -r name; do
    [[ "$name" = private-images/ || "$name" = private-images/avatars/ || "$name" =~ ^private-images/(avatars/)?[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\.jpg$ ]] || die 'Unexpected private storage entry; inspect before backup.'
done < <(tar -tzf "$STAGE/private-images.tar.gz")
[[ -z "$(tar -tvzf "$STAGE/private-images.tar.gz" | awk 'substr($0,1,1)!="-" && substr($0,1,1)!="d" {print}')" ]] || die 'Private image links rejected.'
cp -- "$ENV_FILE" "$STAGE/app.env"
(cd "$STAGE" && sha256sum db.dump private-images.tar.gz app.env > CONTENTS.sha256)
STAMP="$(date -u +%Y%m%dT%H%M%S)-$$"
ARCHIVE="$BACKUP_ROOT/backup-$STAMP.tar.enc"
tar -cf "$STAGE/bundle.tar" -C "$STAGE" db.dump private-images.tar.gz app.env CONTENTS.sha256
openssl enc -aes-256-cbc -pbkdf2 -iter 200000 -salt -pass stdin \
    -in "$STAGE/bundle.tar" -out "$STAGE/bundle.enc" < "$KEY_FILE"
mv -- "$STAGE/bundle.enc" "$ARCHIVE"
(cd "$BACKUP_ROOT" && sha256sum "$(basename "$ARCHIVE")" > "$(basename "$ARCHIVE").sha256")
chmod 600 "$ARCHIVE" "$ARCHIVE.sha256"
# Chỉ dọn đúng các file do script tạo, không duyệt/xóa thư mục khác.
find "$BACKUP_ROOT" -maxdepth 1 -type f -name 'backup-*.tar.enc*' -mtime +14 -delete
echo "BACKUP_OK=$ARCHIVE"
