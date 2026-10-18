#!/usr/bin/env bash
# ==============================================================================
# Taktak Production Manual Rollback Script (Giai đoạn 8C)
# Khôi phục phiên bản JAR trước đó và tùy chọn khôi phục snapshot database
# Chạy với quyền sudo trên Ubuntu VPS
# Cách dùng: sudo ./rollback.sh [--restore-db <BACKUP_TIMESTAMP>]
# ==============================================================================
set -euo pipefail

APP_DIR="/var/taktak"
TARGET_JAR="$APP_DIR/taktak-server.jar"
BACKUP_JAR="$APP_DIR/taktak-server.jar.bak"
HEALTHCHECK_URL="http://127.0.0.1:8080/actuator/health"

echo "=============================================================================="
echo " [ROLLBACK] Bắt đầu quy trình quay lui phiên bản Taktak Server"
echo "=============================================================================="

if [ ! -f "$BACKUP_JAR" ]; then
    echo "Lỗi: Không tìm thấy file sao lưu JAR tại: $BACKUP_JAR"
    echo "Không thể thực hiện rollback JAR!"
    exit 1
fi

echo "===> [1/3] Khôi phục file JAR từ $BACKUP_JAR sang $TARGET_JAR..."
systemctl stop taktak || true
cp -f "$BACKUP_JAR" "$TARGET_JAR"
chown taktak:taktak "$TARGET_JAR"
chmod 750 "$TARGET_JAR"

# Kiểm tra nếu người dùng yêu cầu khôi phục Database từ snapshot
RESTORE_DB_FLAG="${1:-}"
BACKUP_TIMESTAMP="${2:-}"

if [ "$RESTORE_DB_FLAG" = "--restore-db" ] && [ -n "$BACKUP_TIMESTAMP" ]; then
    echo "===> [Tùy chọn] Khôi phục database từ snapshot: $BACKUP_TIMESTAMP..."
    if [ -f "$APP_DIR/scripts/restore.sh" ]; then
        bash "$APP_DIR/scripts/restore.sh" "$BACKUP_TIMESTAMP" -y
    fi
fi

echo "===> [2/3] Khởi động lại dịch vụ taktak.service..."
systemctl start taktak

echo "===> [3/3] Kiểm tra Healthcheck dịch vụ sau Rollback..."
sleep 5

if curl -s -f "$HEALTHCHECK_URL" >/dev/null 2>&1; then
    echo ""
    echo "=============================================================================="
    echo " QUAY LUI THÀNH CÔNG! Dịch vụ Taktak đã hoạt động bình thường (200 OK)."
    echo "=============================================================================="
else
    echo ""
    echo "=============================================================================="
    echo " CẢNH BÁO: Dịch vụ chưa phản hồi 200 OK sau khi rollback."
    echo " Kiểm tra nhật ký: sudo journalctl -u taktak -n 50"
    echo "=============================================================================="
    exit 1
fi
