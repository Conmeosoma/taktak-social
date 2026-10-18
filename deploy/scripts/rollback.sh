#!/usr/bin/env bash
# Rollback JAR không tự reset DB. Restore DB là thao tác riêng có xác nhận.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"
[[ $# = 0 ]] || die 'Usage: rollback.sh. Restore data separately using restore.sh <archive>.'
lock_operations
TARGET="$APP_ROOT/taktak-server.jar"; BACKUP="$APP_ROOT/taktak-server.jar.bak"
[[ -f "$TARGET" && ! -L "$TARGET" && -f "$BACKUP" && ! -L "$BACKUP" ]] || die 'Missing/unsafe JAR targets.'
CANDIDATE="$(mktemp "$APP_ROOT/.candidate-XXXXXXXX.jar")"
trap '[[ ! -e "$CANDIDATE" ]] || rm -f -- "$CANDIDATE"' EXIT
cp -- "$BACKUP" "$CANDIDATE"; chmod 644 "$CANDIDATE"; chown root:root "$CANDIDATE"
systemctl stop "$SERVICE"
# Lưu bản vừa thay để vẫn có thể quay lại; không ghi đè backup cũ.
cp -- "$TARGET" "$APP_ROOT/taktak-server.failed-$(date -u +%Y%m%dT%H%M%S)-$$.jar"
mv -f -- "$CANDIDATE" "$TARGET"
systemctl start "$SERVICE"
healthy || die 'Rollback healthcheck failed; service requires inspection.'
echo 'ROLLBACK_OK'
