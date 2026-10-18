#!/usr/bin/env bash
# ==============================================================================
# Taktak System Restore Script (Giai đoạn 8B)
# Khôi phục hệ thống từ bản sao lưu: Database + Private Images
# Chạy với quyền sudo trên Ubuntu VPS
# Cách dùng: sudo ./restore.sh <TIMESTAMP_OR_PATH> [-y]
# Ví dụ: sudo ./restore.sh 20261018_093000
# ==============================================================================
set -euo pipefail

BACKUP_ROOT="/var/taktak/backups"

if [ "$#" -lt 1 ]; then
    echo "Lỗi: Thiếu tham số đường dẫn bản sao lưu!"
    echo "Cách dùng: sudo $0 <TIMESTAMP_HOẶC_ĐƯỜNG_DẪN_THƯ_MỤC_BACKUP> [-y]"
    echo "Các bản backup hiện có trong $BACKUP_ROOT:"
    ls -1 "$BACKUP_ROOT" 2>/dev/null || echo "(Chưa có bản sao lưu nào)"
    exit 1
fi

TARGET="$1"
AUTO_CONFIRM="${2:-}"

# Xác định đường dẫn thư mục backup
if [ -d "$TARGET" ]; then
    BACKUP_DIR="$TARGET"
elif [ -d "$BACKUP_ROOT/$TARGET" ]; then
    BACKUP_DIR="$BACKUP_ROOT/$TARGET"
else
    echo "Lỗi: Không tìm thấy thư mục bản sao lưu: $TARGET"
    exit 1
fi

echo "=============================================================================="
echo " [CẢNH BÁO] CHUẨN BỊ KHÔI PHỤC DỮ LIỆU TAKTAK TỪ: $BACKUP_DIR"
echo " Quá trình này sẽ GHI ĐÈ toàn bộ dữ liệu database và thư mục ảnh hiện tại!"
echo "=============================================================================="

if [ "$AUTO_CONFIRM" != "-y" ]; then
    read -r -p "Bạn có chắc chắn muốn khôi phục không? (nhập 'yes' để xác nhận): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo "Hủy bỏ quá trình khôi phục theo yêu cầu người dùng."
        exit 0
    fi
fi

# Tạm dừng dịch vụ Taktak để đảm bảo tính nhất quán dữ liệu
echo "===> [1/5] Tạm dừng dịch vụ taktak.service..."
systemctl stop taktak || true

# Nạp cấu hình từ .env
ENV_FILE="/var/taktak/.env"
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1090
    export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

DB_NAME="${DB_NAME:-taktak}"
DB_USER="${DB_USERNAME:-taktak}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"

# --- 2. Khôi phục Cơ sở dữ liệu PostgreSQL ---
echo "===> [2/5] Khôi phục cơ sở dữ liệu PostgreSQL..."
DB_FILE=$(find "$BACKUP_DIR" -name "db_*.sql.gz" | head -n 1)

if [ -n "$DB_FILE" ] && [ -f "$DB_FILE" ]; then
    echo "Đang nạp file dữ liệu: $DB_FILE..."
    # Xóa và tạo lại schema sạch hoặc nạp trực tiếp qua psql
    PGPASSWORD="${DB_PASSWORD:-}" gunzip -c "$DB_FILE" | psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"
    echo "Khôi phục database PostgreSQL thành công."
else
    echo "Cảnh báo: Không tìm thấy file db_*.sql.gz trong bản sao lưu! Bỏ qua."
fi

# --- 3. Khôi phục Thư mục Ảnh riêng tư (Private Images) ---
echo "===> [3/5] Khôi phục thư mục ảnh riêng tư (/var/taktak/private-images)..."
IMAGES_FILE=$(find "$BACKUP_DIR" -name "private_images_*.tar.gz" | head -n 1)

if [ -n "$IMAGES_FILE" ] && [ -f "$IMAGES_FILE" ]; then
    # Dọn dẹp thư mục ảnh hiện tại để tránh xung đột file mồ côi
    rm -rf /var/taktak/private-images
    mkdir -p /var/taktak/private-images

    # Giải nén bản sao lưu
    tar -xzf "$IMAGES_FILE" -C /var/taktak/
    echo "Giải nén ảnh riêng tư thành công."
fi

# --- 4. Thiết lập lại phân quyền bảo mật nghiêm ngặt ---
echo "===> [4/5] Thiết lập lại phân quyền an toàn (chmod 700 taktak:taktak)..."
chmod 700 /var/taktak/private-images
chown -R taktak:taktak /var/taktak/private-images

# --- 5. Khởi động lại dịch vụ & Kiểm tra Healthcheck ---
echo "===> [5/5] Khởi động lại dịch vụ taktak.service..."
systemctl start taktak

echo "Đang đợi ứng dụng khởi động và sẵn sàng (10 giây)..."
sleep 10

if curl -s -f "http://127.0.0.1:8080/actuator/health" >/dev/null 2>&1; then
    echo "KIỂM TRA THÀNH CÔNG: Dịch vụ Taktak đã hoạt động bình thường sau khi khôi phục!"
else
    echo "Cảnh báo: Healthcheck chưa phản hồi 200 OK. Vui lòng kiểm tra log: journalctl -u taktak -n 50"
fi

echo "=============================================================================="
echo " Quá trình khôi phục hoàn tất!"
echo "=============================================================================="
