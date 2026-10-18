#!/usr/bin/env bash
# ==============================================================================
# Taktak Production Safe Update Script (Giai đoạn 8C)
# Tự động cập nhật ứng dụng với kiểm tra Healthcheck và tự động Rollback khi lỗi
# Chạy với quyền sudo trên Ubuntu VPS
# Cách dùng: sudo ./update.sh <ĐƯỜNG_DẪN_TỚI_FILE_JAR_MỚI>
# Ví dụ: sudo ./update.sh /tmp/taktak-server-0.0.2-SNAPSHOT.jar
# ==============================================================================
set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Lỗi: Thiếu file JAR mới!"
    echo "Cách dùng: sudo $0 <NEW_JAR_PATH>"
    echo "Ví dụ: sudo $0 /tmp/taktak-server-new.jar"
    exit 1
fi

NEW_JAR="$1"
APP_DIR="/var/taktak"
TARGET_JAR="$APP_DIR/taktak-server.jar"
BACKUP_JAR="$APP_DIR/taktak-server.jar.bak"
HEALTHCHECK_URL="http://127.0.0.1:8080/actuator/health"
MAX_ATTEMPTS=15
SLEEP_INTERVAL=2

if [ ! -f "$NEW_JAR" ]; then
    echo "Lỗi: Không tìm thấy file JAR tại: $NEW_JAR"
    exit 1
fi

echo "=============================================================================="
echo " Bắt đầu quy trình cập nhật an toàn Taktak Server"
echo " File cập nhật: $NEW_JAR ($(du -h "$NEW_JAR" | cut -f1))"
echo "=============================================================================="

# --- 1. Tự động sao lưu Database trước khi Update ---
echo "===> [1/5] Sao lưu dữ liệu an toàn trước khi cập nhật..."
if [ -f "$APP_DIR/scripts/backup.sh" ]; then
    bash "$APP_DIR/scripts/backup.sh" || echo "Cảnh báo: Không thể tạo bản sao lưu snapshot tự động. Tiếp tục..."
fi

# --- 2. Sao lưu file JAR hiện tại ---
echo "===> [2/5] Sao lưu phiên bản JAR hiện tại thành $BACKUP_JAR..."
if [ -f "$TARGET_JAR" ]; then
    cp -f "$TARGET_JAR" "$BACKUP_JAR"
fi

# --- 3. Thay thế file JAR mới & Phân quyền an toàn ---
echo "===> [3/5] Nạp phiên bản JAR mới vào $TARGET_JAR..."
cp -f "$NEW_JAR" "$TARGET_JAR"
chown taktak:taktak "$TARGET_JAR"
chmod 750 "$TARGET_JAR"

# --- 4. Khởi động lại dịch vụ taktak.service ---
echo "===> [4/5] Khởi động lại dịch vụ taktak.service..."
systemctl restart taktak

# --- 5. Giám sát Healthcheck & Cơ chế Tự động Rollback ---
echo "===> [5/5] Kiểm tra trạng thái sẵn sàng (Healthcheck) qua $HEALTHCHECK_URL..."
IS_HEALTHY=false

for i in $(seq 1 $MAX_ATTEMPTS); do
    echo "Lần thử $i/$MAX_ATTEMPTS: Đang thăm dò..."
    if curl -s -f "$HEALTHCHECK_URL" >/dev/null 2>&1; then
        IS_HEALTHY=true
        break
    fi
    sleep "$SLEEP_INTERVAL"
done

if [ "$IS_HEALTHY" = true ]; then
    echo ""
    echo "=============================================================================="
    echo " CẬP NHẬT THÀNH CÔNG! Dịch vụ Taktak hoạt động ổn định (200 OK)."
    echo " Phiên bản dự phòng: $BACKUP_JAR"
    echo "=============================================================================="
    exit 0
else
    echo ""
    echo "=============================================================================="
    echo " [CẢNH BÁO NGUY HIỂM] Healthcheck không phản hồi 200 OK sau $((MAX_ATTEMPTS * SLEEP_INTERVAL)) giây!"
    echo " Đang kích hoạt cơ chế TỰ ĐỘNG ROLLBACK về phiên bản trước..."
    echo "=============================================================================="

    if [ -f "$BACKUP_JAR" ]; then
        cp -f "$BACKUP_JAR" "$TARGET_JAR"
        chown taktak:taktak "$TARGET_JAR"
        chmod 750 "$TARGET_JAR"
        systemctl restart taktak
        echo "Đã khôi phục file JAR trước đó và khởi động lại dịch vụ."
        
        # Kiểm tra lại healthcheck của bản cũ
        sleep 5
        if curl -s -f "$HEALTHCHECK_URL" >/dev/null 2>&1; then
            echo "ROLLBACK THÀNH CÔNG: Hệ thống đã trở về trạng thái ổn định cũ."
        else
            echo "LỖI NGHIÊM TRỌNG: Dịch vụ không thể khôi phục tự động! Vui lòng kiểm tra log: journalctl -u taktak -n 50"
        fi
    else
        echo "Lỗi: Không tìm thấy file $BACKUP_JAR để rollback!"
    fi
    exit 1
fi
