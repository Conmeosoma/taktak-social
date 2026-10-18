#!/usr/bin/env bash
# ==============================================================================
# Taktak System Backup Script (Giai đoạn 8B)
# Tự động sao lưu: PostgreSQL Database + Private Images + File cấu hình .env
# Chạy với quyền root hoặc user có quyền đọc DB và thư mục private
# ==============================================================================
set -euo pipefail

BACKUP_ROOT="/var/taktak/backups"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
TARGET_DIR="$BACKUP_ROOT/$TIMESTAMP"
ENV_FILE="/var/taktak/.env"
IMAGE_DIR="/var/taktak/private-images"
RETENTION_DAYS=14

echo "=============================================================================="
echo " [Taktak Backup] Bắt đầu quá trình sao lưu: $TIMESTAMP"
echo "=============================================================================="

# Tạo thư mục lưu bản backup với phân quyền bảo mật nghiêm ngặt (chỉ root/taktak)
mkdir -p "$TARGET_DIR"
chmod 700 "$TARGET_DIR"

# Nạp thông tin DB từ file .env nếu có
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1090
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

DB_NAME="${DB_NAME:-taktak}"
DB_USER="${DB_USERNAME:-taktak}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"

# --- 1. Sao lưu Cơ sở dữ liệu PostgreSQL ---
echo "===> [1/3] Sao lưu cơ sở dữ liệu PostgreSQL ($DB_NAME)..."
DB_BACKUP_FILE="$TARGET_DIR/db_${DB_NAME}_${TIMESTAMP}.sql.gz"

if command -v pg_dump >/dev/null 2>&1; then
    PGPASSWORD="${DB_PASSWORD:-}" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" | gzip > "$DB_BACKUP_FILE"
    chmod 600 "$DB_BACKUP_FILE"
    echo "Đã sao lưu DB thành công: $DB_BACKUP_FILE ($(du -h "$DB_BACKUP_FILE" | cut -f1))"
else
    echo "Cảnh báo: Không tìm thấy lệnh pg_dump trên host! Bỏ qua sao lưu DB native."
fi

# --- 2. Sao lưu Thư mục Ảnh riêng tư (Private Images) ---
echo "===> [2/3] Sao lưu thư mục ảnh riêng tư ($IMAGE_DIR)..."
IMAGES_BACKUP_FILE="$TARGET_DIR/private_images_${TIMESTAMP}.tar.gz"

if [ -d "$IMAGE_DIR" ]; then
    tar -czf "$IMAGES_BACKUP_FILE" -C "/var/taktak" "private-images"
    chmod 600 "$IMAGES_BACKUP_FILE"
    echo "Đã sao lưu ảnh riêng tư thành công: $IMAGES_BACKUP_FILE ($(du -h "$IMAGES_BACKUP_FILE" | cut -f1))"
else
    echo "Cảnh báo: Không tìm thấy thư mục $IMAGE_DIR."
fi

# --- 3. Sao lưu File Cấu hình Môi trường (.env) ---
echo "===> [3/3] Sao lưu file cấu hình .env..."
ENV_BACKUP_FILE="$TARGET_DIR/env_${TIMESTAMP}.bak"

if [ -f "$ENV_FILE" ]; then
    cp "$ENV_FILE" "$ENV_BACKUP_FILE"
    chmod 600 "$ENV_BACKUP_FILE"
    echo "Đã sao lưu file cấu hình thành công: $ENV_BACKUP_FILE"
fi

# Ghi metadata thông tin bản backup
cat <<EOF > "$TARGET_DIR/metadata.json"
{
  "timestamp": "$TIMESTAMP",
  "db_name": "$DB_NAME",
  "db_user": "$DB_USER",
  "retention_days": $RETENTION_DAYS,
  "status": "COMPLETED"
}
EOF
chmod 600 "$TARGET_DIR/metadata.json"

# Phân quyền an toàn toàn bộ thư mục sao lưu
chown -R taktak:taktak "$BACKUP_ROOT" || true

# --- 4. Tự động xoay vòng & dọn dẹp bản sao lưu cũ ---
echo "===> [Dọn dẹp] Xóa các bản sao lưu cũ hơn $RETENTION_DAYS ngày..."
find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +"$RETENTION_DAYS" -exec rm -rf {} +

echo "=============================================================================="
echo " Sao lưu hoàn tất thành công tại: $TARGET_DIR"
echo "=============================================================================="
