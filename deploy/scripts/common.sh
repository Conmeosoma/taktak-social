#!/usr/bin/env bash
# Helper dùng chung; không eval/source nội dung .env.
set -euo pipefail
umask 077
die() { echo "ERROR: $*" >&2; exit 1; }
APP_ROOT="$(realpath -e -- "${TAKTAK_ROOT:-/var/taktak}")"
case "$APP_ROOT" in /|/var|/tmp|/home|/etc|/usr|/opt) die 'Application root is too broad.';; esac
[[ "$APP_ROOT" = /* && -d "$APP_ROOT" ]] || die 'Invalid application root.'
SERVICE="${TAKTAK_SERVICE:-taktak}"
[[ "$SERVICE" =~ ^[a-zA-Z0-9_-]+$ ]] || die 'Invalid service name.'
ENV_FILE="$APP_ROOT/.env"
BACKUP_ROOT="$APP_ROOT/backups"
IMAGE_DIR="$APP_ROOT/private-images"
KEY_FILE="${TAKTAK_BACKUP_KEY_FILE:-/etc/taktak/backup.key}"

load_database() {
    [[ -f "$ENV_FILE" ]] || die 'Missing local environment file.'
    while IFS='=' read -r name value || [[ -n "$name" ]]; do
        case "$name" in DB_URL|DB_USERNAME|DB_PASSWORD|DB_NAME|DB_HOST|DB_PORT|IMAGE_STORAGE_ROOT) ;; *) continue;; esac
        value="${value%$'\r'}"
        if [[ "$value" = \"*\" || "$value" = \'*\' ]]; then value="${value:1:${#value}-2}"; fi
        printf -v "$name" '%s' "$value"
    done < "$ENV_FILE"
    if [[ -n "${DB_URL:-}" ]]; then
        [[ "$DB_URL" =~ ^jdbc:postgresql://([^/:]+)(:([0-9]+))?/([a-zA-Z_][a-zA-Z0-9_]*)(\?.*)?$ ]] || die 'Unsupported DB_URL.'
        DB_HOST="${BASH_REMATCH[1]}"; DB_PORT="${BASH_REMATCH[3]:-5432}"; DB_NAME="${BASH_REMATCH[4]}"
    fi
    DB_HOST="${DB_HOST:-127.0.0.1}"; DB_PORT="${DB_PORT:-5432}"; DB_NAME="${DB_NAME:-taktak}"
    DB_USER="${DB_USERNAME:-taktak}"
    [[ "$DB_NAME" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ && "$DB_PORT" =~ ^[0-9]+$ ]] || die 'Invalid database settings.'
    [[ -n "${DB_PASSWORD:-}" ]] || die 'Missing DB_PASSWORD.'
    [[ "${IMAGE_STORAGE_ROOT:-$IMAGE_DIR}" = "$IMAGE_DIR" ]] || die 'Storage root differs from the managed image directory.'
}
database_dump() {
    PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -Fc -f "$1"
}
database_restore() {
    PGPASSWORD="$DB_PASSWORD" pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        --clean --if-exists --exit-on-error --single-transaction "$1"
}
lock_operations() {
    if [[ "${TAKTAK_LOCK_HELD:-0}" != 1 ]]; then
        exec 9> "$APP_ROOT/.maintenance.lock"
        flock -n 9 || die 'Another maintenance operation is running.'
        export TAKTAK_LOCK_HELD=1
    fi
}
require_key() {
    [[ -f "$KEY_FILE" && ! -L "$KEY_FILE" && $(wc -c < "$KEY_FILE") -ge 32 ]] || die 'Missing or short backup key file.'
    [[ -z "$(find "$KEY_FILE" -maxdepth 0 -perm /077 -print)" ]] || die 'Backup key must not be group/world accessible.'
}
make_stage() {
    [[ ! -L "$BACKUP_ROOT" ]] || die 'Backup directory must not be a symlink.'
    mkdir -p "$BACKUP_ROOT"; chmod 700 "$BACKUP_ROOT"
    STAGE="$(mktemp -d "$BACKUP_ROOT/.pending-XXXXXXXX")"
}
remove_stage() {
    [[ -n "${STAGE:-}" && -d "$STAGE" && ! -L "$STAGE" ]] || return 0
    local resolved; resolved="$(realpath -e -- "$STAGE")"
    [[ "$resolved" = "$BACKUP_ROOT"/.pending-* ]] || die 'Refusing unsafe temporary cleanup.'
    rm -rf -- "$resolved"
}
healthy() {
    local attempt
    for attempt in $(seq 1 "${TAKTAK_HEALTH_ATTEMPTS:-15}"); do
        if curl --connect-timeout 2 --max-time 3 -fsS "${TAKTAK_HEALTH_URL:-http://127.0.0.1:8080/actuator/health}" >/dev/null; then return 0; fi
        sleep "${TAKTAK_HEALTH_INTERVAL:-2}"
    done
    return 1
}
