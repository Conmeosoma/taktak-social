#!/usr/bin/env bash
# Bootstrap HTTP -> cấp certificate -> kiểm tra và bật HTTPS.
set -euo pipefail
[[ $# = 2 ]] || { echo 'Usage: setup-ssl.sh <domain> <email>' >&2; exit 1; }
DOMAIN="$1"; EMAIL="$2"
[[ "${#DOMAIN}" -le 253 && "$DOMAIN" =~ ^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$ && "$DOMAIN" = *.* ]] || exit 1
[[ "$EMAIL" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]] || exit 1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Override chỉ để test trên root fixture, không thay cấu hình máy host.
CONFIG_ROOT="${TAKTAK_NGINX_ROOT:-/etc/nginx}"
CERT_ROOT="${TAKTAK_CERT_ROOT:-/etc/letsencrypt}"
WEB_ROOT="${TAKTAK_ACME_ROOT:-/var/www/certbot}"
apt-get update
apt-get install -y nginx certbot python3-certbot-nginx
mkdir -p "$WEB_ROOT" "$CONFIG_ROOT/sites-available" "$CONFIG_ROOT/sites-enabled"
chown www-data:www-data "$WEB_ROOT"
TARGET="$CONFIG_ROOT/sites-available/taktak"
if [[ ! -s "$CERT_ROOT/live/$DOMAIN/fullchain.pem" || ! -s "$CERT_ROOT/live/$DOMAIN/privkey.pem" ]]; then
    sed "s/YOUR_DOMAIN_OR_IP/$DOMAIN/g" "$SCRIPT_DIR/../nginx/taktak-http.conf" > "$TARGET"
    ln -sfn "$TARGET" "$CONFIG_ROOT/sites-enabled/taktak"
    nginx -t
    systemctl reload nginx
fi
certbot certonly --webroot -w "$WEB_ROOT" -d "$DOMAIN" -m "$EMAIL" \
    --non-interactive --agree-tos --keep-until-expiring --deploy-hook "systemctl reload nginx"
[[ -s "$CERT_ROOT/live/$DOMAIN/fullchain.pem" && -s "$CERT_ROOT/live/$DOMAIN/privkey.pem" ]] || { echo 'Missing issued certificate' >&2; exit 1; }
PREVIOUS="$(mktemp "$CONFIG_ROOT/sites-available/.taktak-previous-XXXXXXXX")"
HAD_PREVIOUS=0
if [[ -f "$TARGET" ]]; then cp -- "$TARGET" "$PREVIOUS"; HAD_PREVIOUS=1; fi
finish() {
    local result=$?
    trap - EXIT
    if [[ "$result" != 0 && "$HAD_PREVIOUS" = 1 ]]; then cp -- "$PREVIOUS" "$TARGET"; fi
    rm -f -- "$PREVIOUS"
    exit "$result"
}
trap finish EXIT
sed "s/YOUR_DOMAIN_OR_IP/$DOMAIN/g" "$SCRIPT_DIR/../nginx/taktak.conf" > "$TARGET"
ln -sfn "$TARGET" "$CONFIG_ROOT/sites-enabled/taktak"
nginx -t
systemctl reload nginx
certbot renew --dry-run
echo "TLS_OK=https://$DOMAIN"
