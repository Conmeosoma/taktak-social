#!/usr/bin/env bash
# ==============================================================================
# Taktak VPS Ubuntu Setup Script (Giai đoạn 8A & 8B)
# Chạy với quyền sudo trên Ubuntu 22.04 / 24.04 LTS
# ==============================================================================
set -euo pipefail

echo "===> [1/8] Cập nhật hệ thống và cài đặt gói phụ thuộc..."
apt-get update
apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    postgresql \
    postgresql-contrib \
    nginx \
    certbot \
    python3-certbot-nginx \
    ufw \
    curl \
    ca-certificates \
    logrotate

echo "===> [2/8] Tạo user dịch vụ hệ thống không phải root (taktak)..."
if ! id -u taktak >/dev/null 2>&1; then
    useradd --system --no-create-home --shell /bin/false taktak
    echo "Đã tạo user taktak thành công."
else
    echo "User taktak đã tồn tại."
fi

echo "===> [3/8] Thiết lập cấu trúc thư mục ứng dụng, scripts và private images..."
mkdir -p /var/taktak/private-images
mkdir -p /var/taktak/backups
mkdir -p /var/taktak/scripts
mkdir -p /var/log/taktak

# Quyền hạn chế tối đa: chỉ user taktak được đọc/ghi ảnh riêng tư và bản sao lưu
chmod 700 /var/taktak/private-images
chmod 700 /var/taktak/backups
chmod 750 /var/taktak/scripts
chmod 750 /var/log/taktak
chown -R taktak:taktak /var/taktak /var/log/taktak

echo "===> [4/8] Cấu hình bảo mật PostgreSQL (chỉ lắng nghe local loopback 127.0.0.1)..."
PG_CONF=$(find /etc/postgresql/ -name "postgresql.conf" | head -n 1)
if [ -n "$PG_CONF" ]; then
    sed -i "s/#listen_addresses = 'localhost'/listen_addresses = '127.0.0.1'/g" "$PG_CONF"
    sed -i "s/listen_addresses = '*'/listen_addresses = '127.0.0.1'/g" "$PG_CONF"
    systemctl restart postgresql
    echo "PostgreSQL đã được cấu hình chỉ lắng nghe 127.0.0.1."
fi

echo "===> [5/8] Cấu hình tường lửa UFW (Mở 22, 80, 443; Chặn tuyệt đối 8080 & 5432)..."
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP Nginx & Certbot'
ufw allow 443/tcp comment 'HTTPS Nginx TLS'
# Cổng 8080 (Spring Boot) và 5432 (PostgreSQL) TUYỆT ĐỐI không mở ra ngoài
ufw --force enable

echo "===> [6/8] Cài đặt cấu hình Logrotate..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/../logrotate/taktak" ]; then
    cp "$SCRIPT_DIR/../logrotate/taktak" /etc/logrotate.d/taktak
    chmod 644 /etc/logrotate.d/taktak
    echo "Đã cài đặt cấu hình /etc/logrotate.d/taktak."
fi

echo "===> [7/8] Cài đặt script Backup & Restore và Systemd Timer..."
if [ -f "$SCRIPT_DIR/backup.sh" ]; then
    cp "$SCRIPT_DIR/backup.sh" /var/taktak/scripts/backup.sh
    chmod 750 /var/taktak/scripts/backup.sh
    chown taktak:taktak /var/taktak/scripts/backup.sh
fi

if [ -f "$SCRIPT_DIR/restore.sh" ]; then
    cp "$SCRIPT_DIR/restore.sh" /var/taktak/scripts/restore.sh
    chmod 750 /var/taktak/scripts/restore.sh
    chown taktak:taktak /var/taktak/scripts/restore.sh
fi

if [ -f "$SCRIPT_DIR/update.sh" ]; then
    cp "$SCRIPT_DIR/update.sh" /var/taktak/scripts/update.sh
    chmod 750 /var/taktak/scripts/update.sh
    chown taktak:taktak /var/taktak/scripts/update.sh
fi

if [ -f "$SCRIPT_DIR/rollback.sh" ]; then
    cp "$SCRIPT_DIR/rollback.sh" /var/taktak/scripts/rollback.sh
    chmod 750 /var/taktak/scripts/rollback.sh
    chown taktak:taktak /var/taktak/scripts/rollback.sh
fi

if [ -f "$SCRIPT_DIR/../systemd/taktak-backup.service" ]; then
    cp "$SCRIPT_DIR/../systemd/taktak-backup.service" /etc/systemd/system/
fi

if [ -f "$SCRIPT_DIR/../systemd/taktak-backup.timer" ]; then
    cp "$SCRIPT_DIR/../systemd/taktak-backup.timer" /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable --now taktak-backup.timer
    echo "Đã kích hoạt taktak-backup.timer (chạy backup tự động 02:00 AM mỗi ngày)."
fi

echo "===> [8/8] Cài đặt dịch vụ systemd taktak.service..."
if [ -f "$SCRIPT_DIR/../systemd/taktak.service" ]; then
    cp "$SCRIPT_DIR/../systemd/taktak.service" /etc/systemd/system/taktak.service
    systemctl daemon-reload
    echo "Dịch vụ taktak.service đã được đăng ký với systemd."
fi

echo ""
echo "=============================================================================="
echo " Thiết lập môi trường VPS Ubuntu hoàn tất!"
echo " Các bước tiếp theo:"
echo " 1. Sao chép deploy/.env.example thành /var/taktak/.env và điền các secrets:"
echo "    sudo cp deploy/.env.example /var/taktak/.env"
echo "    sudo chmod 600 /var/taktak/.env"
echo "    sudo chown taktak:taktak /var/taktak/.env"
echo " 2. Đặt file taktak-server.jar vào /var/taktak/taktak-server.jar"
echo " 3. Đặt file firebase-admin.json vào /var/taktak/firebase-admin.json (nếu dùng FCM)"
echo " 4. Khởi động dịch vụ backend: sudo systemctl enable --now taktak"
echo " 5. Cấp phát chứng chỉ HTTPS Let's Encrypt cho tên miền:"
echo "    sudo deploy/scripts/setup-ssl.sh <DOMAIN> <EMAIL>"
echo " 6. Kiểm tra healthcheck: curl -I https://<DOMAIN>/actuator/health"
echo "=============================================================================="
