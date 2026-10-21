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
apt-get install -y openssl util-linux

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

# App chỉ được ghi ảnh/log; backup và chương trình bảo trì thuộc root.
chmod 700 /var/taktak/private-images
chmod 700 /var/taktak/backups
chmod 750 /var/taktak/scripts
chmod 750 /var/log/taktak
chown -R taktak:taktak /var/taktak/private-images /var/log/taktak
# Script backup chạy root: app user không được quyền thay script/JAR/env.
chown root:root /var/taktak /var/taktak/scripts /var/taktak/backups
chmod 755 /var/taktak /var/taktak/scripts
chmod 700 /var/taktak/backups
install -d -m 700 -o root -g root /etc/taktak
if [ ! -f /etc/taktak/backup.key ]; then
    (umask 077; openssl rand -hex 32 > /etc/taktak/backup.key)
fi
chmod 600 /etc/taktak/backup.key

echo "===> [4/8] Cấu hình bảo mật và tối ưu bộ nhớ PostgreSQL (1 vCPU, 1 GB RAM)..."
PG_CONF=$(find /etc/postgresql/ -name "postgresql.conf" | head -n 1)
if [ -n "$PG_CONF" ]; then
    sed -i "s/^#*listen_addresses = .*/listen_addresses = '127.0.0.1'/" "$PG_CONF"
    # Tối ưu RAM cho VPS cấu hình thấp (chống tràn RAM)
    sed -i "s/^shared_buffers = .*/shared_buffers = 64MB/" "$PG_CONF"
    sed -i "s/^#*work_mem = .*/work_mem = 4MB/" "$PG_CONF"
    sed -i "s/^#*maintenance_work_mem = .*/maintenance_work_mem = 32MB/" "$PG_CONF"
    sed -i "s/^max_connections = .*/max_connections = 30/" "$PG_CONF"
    systemctl restart postgresql
    echo "PostgreSQL đã được cấu hình tối ưu tài nguyên và chỉ lắng nghe 127.0.0.1."
fi

echo "===> [5/8] Cấu hình tường lửa UFW (Mở SSH, 80, 443; Chặn tuyệt đối 8080 & 5432)..."
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH-Default'
CURRENT_SSH_PORT=$(ss -tulpn | grep sshd | awk '{print $5}' | awk -F: '{print $NF}' | head -n 1)
if [ -n "$CURRENT_SSH_PORT" ] && [ "$CURRENT_SSH_PORT" != "22" ]; then
    ufw allow "${CURRENT_SSH_PORT}/tcp" comment 'SSH-Active'
    echo "Đã mở cổng SSH đang hoạt động: ${CURRENT_SSH_PORT}/tcp"
fi
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
    chown root:root /var/taktak/scripts/backup.sh
fi

if [ -f "$SCRIPT_DIR/restore.sh" ]; then
    cp "$SCRIPT_DIR/restore.sh" /var/taktak/scripts/restore.sh
    chmod 750 /var/taktak/scripts/restore.sh
    chown root:root /var/taktak/scripts/restore.sh
fi

if [ -f "$SCRIPT_DIR/update.sh" ]; then
    cp "$SCRIPT_DIR/update.sh" /var/taktak/scripts/update.sh
    chmod 750 /var/taktak/scripts/update.sh
    chown root:root /var/taktak/scripts/update.sh
fi

if [ -f "$SCRIPT_DIR/rollback.sh" ]; then
    cp "$SCRIPT_DIR/rollback.sh" /var/taktak/scripts/rollback.sh
    chmod 750 /var/taktak/scripts/rollback.sh
    chown root:root /var/taktak/scripts/rollback.sh
fi
install -m 750 -o root -g root "$SCRIPT_DIR/common.sh" /var/taktak/scripts/common.sh
install -m 640 -o root -g root "$SCRIPT_DIR/reconcile-restore.sql" /var/taktak/scripts/reconcile-restore.sql

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
echo "    sudo chown root:root /var/taktak/.env"
echo " 2. Đặt file taktak-server.jar vào /var/taktak/taktak-server.jar"
echo " 3. Đặt file firebase-admin.json vào /var/taktak/firebase-admin.json (nếu dùng FCM)"
echo " 4. Khởi động dịch vụ backend: sudo systemctl enable --now taktak"
echo " 5. Cấp phát chứng chỉ HTTPS Let's Encrypt cho tên miền:"
echo "    sudo deploy/scripts/setup-ssl.sh <DOMAIN> <EMAIL>"
echo " 6. Kiểm tra healthcheck: curl -I https://<DOMAIN>/actuator/health"
echo " 7. Giữ bản sao backup.key ở nơi an toàn tách khỏi VPS; mất key sẽ không giải mã được backup."
echo "=============================================================================="
