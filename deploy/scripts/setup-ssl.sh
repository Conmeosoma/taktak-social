#!/usr/bin/env bash
# ==============================================================================
# Taktak SSL Setup Script với Let's Encrypt Certbot (Giai đoạn 8B)
# Chạy với quyền sudo trên Ubuntu 22.04 / 24.04 LTS
# Cách dùng: sudo ./setup-ssl.sh <DOMAIN> <EMAIL>
# Ví dụ: sudo ./setup-ssl.sh taktak.example.com admin@example.com
# ==============================================================================
set -euo pipefail

if [ "$#" -lt 2 ]; then
    echo "Lỗi: Thiếu tham số!"
    echo "Cách dùng: sudo $0 <DOMAIN> <EMAIL>"
    echo "Ví dụ: sudo $0 api.taktak.example.com admin@example.com"
    exit 1
fi

DOMAIN="$1"
EMAIL="$2"

echo "=============================================================================="
echo " Thiết lập SSL/TLS Let's Encrypt cho domain: $DOMAIN"
echo " Email quản trị: $EMAIL"
echo "=============================================================================="

echo "===> [1/4] Cài đặt Nginx và Certbot..."
apt-get update
apt-get install -y nginx certbot python3-certbot-nginx

# Tạo thư mục ACME challenge
mkdir -p /var/www/certbot
chown -R www-data:www-data /var/www/certbot

echo "===> [2/4] Chuẩn bị file cấu hình Nginx..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NGINX_CONF_TEMPLATE="$SCRIPT_DIR/../nginx/taktak.conf"

if [ -f "$NGINX_CONF_TEMPLATE" ]; then
    # Thay thế placeholder domain bằng domain thật
    sed "s/YOUR_DOMAIN_OR_IP/$DOMAIN/g" "$NGINX_CONF_TEMPLATE" > /etc/nginx/sites-available/taktak
    
    # Tạo symlink nếu chưa có
    if [ ! -f /etc/nginx/sites-enabled/taktak ]; then
        ln -sf /etc/nginx/sites-available/taktak /etc/nginx/sites-enabled/taktak
    fi

    # Gỡ bỏ default site của Nginx nếu còn tồn tại
    if [ -f /etc/nginx/sites-enabled/default ]; then
        rm -f /etc/nginx/sites-enabled/default
    fi

    echo "Đã sao chép cấu hình Nginx tới /etc/nginx/sites-available/taktak."
fi

echo "===> [3/4] Cấp phát chứng chỉ Let's Encrypt SSL/TLS..."
# Dùng certbot cấp chứng chỉ tự động cho Nginx
certbot --nginx \
    -d "$DOMAIN" \
    --non-interactive \
    --agree-tos \
    -m "$EMAIL" \
    --redirect

echo "===> [4/4] Kiểm tra cấu hình và reload Nginx..."
nginx -t
systemctl reload nginx

echo "===> [Kiểm tra] Kiểm tra dịch vụ tự động gia hạn chứng chỉ (Auto-Renewal)..."
systemctl status certbot.timer --no-pager || true
certbot renew --dry-run

echo ""
echo "=============================================================================="
echo " Thiết lập SSL/TLS thành công!"
echo " Tên miền: https://$DOMAIN"
echo " Chứng chỉ Let's Encrypt sẽ tự động gia hạn qua certbot.timer."
echo "=============================================================================="
