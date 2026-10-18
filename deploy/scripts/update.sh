#!/usr/bin/env bash
# Update fail-closed; mọi lỗi sau khi thay JAR đều đi qua rollback.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
[[ $# = 1 && -f "$1" && ! -L "$1" ]] || die 'Usage: update.sh <new.jar>'
lock_operations
NEW_JAR="$(realpath -e -- "$1")"
TARGET="$APP_ROOT/taktak-server.jar"; BACKUP="$APP_ROOT/taktak-server.jar.bak"
[[ -f "$TARGET" && ! -L "$TARGET" && "$NEW_JAR" != "$TARGET" && "$NEW_JAR" != "$BACKUP" ]] || die 'Invalid update targets.'
WAS_ACTIVE=0; SWITCHED=0; COMPLETE=0
CANDIDATE="$(mktemp "$APP_ROOT/.candidate-XXXXXXXX.jar")"
finish() {
    local result=$?
    trap - EXIT
    if [[ "$COMPLETE" != 1 && "$SWITCHED" = 1 ]]; then
        systemctl stop "$SERVICE" || true
        cp -- "$BACKUP" "$CANDIDATE"
        chown root:root "$CANDIDATE"; chmod 644 "$CANDIDATE"
        mv -f -- "$CANDIDATE" "$TARGET"
        echo 'UPDATE_ROLLED_BACK' >&2
    fi
    if [[ "$WAS_ACTIVE" = 1 ]]; then systemctl start "$SERVICE" && healthy || result=1; fi
    [[ ! -e "$CANDIDATE" ]] || rm -f -- "$CANDIDATE"
    exit "$result"
}
trap finish EXIT
if systemctl is-active --quiet "$SERVICE"; then WAS_ACTIVE=1; systemctl stop "$SERVICE"; fi
# Backup phải đạt, không tiếp tục khi thiếu key/DB/images.
bash "$SCRIPT_DIR/backup.sh"
cp -- "$NEW_JAR" "$CANDIDATE"; chown root:root "$CANDIDATE"; chmod 644 "$CANDIDATE"
cp -- "$TARGET" "$BACKUP"; chmod 600 "$BACKUP"
mv -f -- "$CANDIDATE" "$TARGET"; SWITCHED=1
systemctl start "$SERVICE"
healthy
COMPLETE=1
echo 'UPDATE_OK'
