#!/usr/bin/env bash
# ==============================================================================
# Taktak UFW Firewall Setup Script (Giai đoạn 8B)
# Chạy với quyền sudo trên Ubuntu 22.04 / 24.04 LTS
# ==============================================================================
set -euo pipefail

echo "=============================================================================="
echo " Thiết lập Tường lửa UFW cho Taktak VPS"
echo "=============================================================================="

echo "===> [1/4] Cài đặt UFW nếu chưa có..."
apt-get update
apt-get install -y ufw

echo "===> [2/4] Thiết lập chính sách mặc định (Default Policies)..."
# Chặn toàn bộ traffic vào, cho phép traffic ra
ufw default deny incoming
ufw default allow outgoing

echo "===> [3/4] Mở các cổng dịch vụ công khai cần thiết..."
# SSH (cần thiết để không bị khóa kết nối quản trị)
ufw allow 22/tcp comment 'SSH Remote Access'

# HTTP & HTTPS (Nginx Reverse Proxy & Certbot)
ufw allow 80/tcp comment 'HTTP Nginx & Certbot ACME'
ufw allow 443/tcp comment 'HTTPS Nginx TLS'

# Lưu ý: Cổng 8080 (Spring Boot Server) và cổng 5432 (PostgreSQL Database)
# TUYỆT ĐỐI KHÔNG mở ra internet công khai. Chúng chỉ được phép truy cập
# nội bộ từ 127.0.0.1 (loopback) hoặc private docker network.

echo "===> [4/4] Kích hoạt tường lửa UFW..."
ufw --force enable

echo ""
echo "=== Trạng thái UFW sau khi cấu hình ==="
ufw status verbose

echo "=============================================================================="
echo " Thiết lập tường lửa UFW hoàn tất!"
echo " Các cổng mở công khai: 22 (SSH), 80 (HTTP), 443 (HTTPS)."
echo " Các cổng nội bộ được bảo vệ: 8080 (Spring Boot), 5432 (Postgres)."
echo "=============================================================================="
