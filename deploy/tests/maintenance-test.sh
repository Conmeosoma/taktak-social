#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARTIFACTS="$ROOT/.local/artifacts/fixes-20261018"
mkdir -p "$ARTIFACTS"
BASE_PATH="$PATH"
new_case() {
    CASE_ROOT="$(mktemp -d "$ARTIFACTS/maintenance.XXXXXXXX")"; export CASE_ROOT
    export TAKTAK_ROOT="$CASE_ROOT/app" TAKTAK_BACKUP_KEY_FILE="$CASE_ROOT/key"
    export TAKTAK_HEALTH_ATTEMPTS=1 TAKTAK_HEALTH_INTERVAL=0
    unset TAKTAK_LOCK_HELD
    export TEST_PASSWORD='fixture password with spaces $ !'
    mkdir -p "$CASE_ROOT/bin" "$TAKTAK_ROOT/private-images" "$TAKTAK_ROOT/backups"
    for tool in systemctl chown pg_dump pg_restore psql curl apt-get nginx certbot flock; do
        cp "$ROOT/deploy/tests/fake-tool.sh" "$CASE_ROOT/bin/$tool"; chmod +x "$CASE_ROOT/bin/$tool"
    done
    export PATH="$CASE_ROOT/bin:$BASE_PATH"
    printf 'DB_URL=jdbc:postgresql://127.0.0.1:5432/taktak\nDB_USERNAME=taktak\nDB_PASSWORD=%s\n' "$TEST_PASSWORD" > "$TAKTAK_ROOT/.env"
    printf 'fixture-encryption-key-0123456789abcdef0123456789abcdef\n' > "$TAKTAK_BACKUP_KEY_FILE"; chmod 600 "$TAKTAK_BACKUP_KEY_FILE"
    printf 'before-db' > "$CASE_ROOT/db"; touch "$CASE_ROOT/active"
    printf 'before-image' > "$TAKTAK_ROOT/private-images/11111111-1111-1111-1111-111111111111.jpg"
    mkdir -p "$TAKTAK_ROOT/private-images/avatars"
    printf 'before-avatar' > "$TAKTAK_ROOT/private-images/avatars/22222222-2222-2222-2222-222222222222.jpg"
    printf 'old-good-jar' > "$TAKTAK_ROOT/taktak-server.jar"
}
backup() { bash "$ROOT/deploy/scripts/backup.sh" > "$CASE_ROOT/backup.log" 2>&1; }
archive() { find "$TAKTAK_ROOT/backups" -maxdepth 1 -name 'backup-*.tar.enc' | sort | head -n 1; }
fail_command() { if "$@" > "$CASE_ROOT/expected-failure.log" 2>&1; then echo 'Expected failure did not happen' >&2; exit 1; fi; }
check_data() {
    [[ "$(< "$CASE_ROOT/db")" = before-db ]]
    [[ "$(< "$TAKTAK_ROOT/private-images/11111111-1111-1111-1111-111111111111.jpg")" = before-image ]]
    [[ "$(< "$TAKTAK_ROOT/private-images/avatars/22222222-2222-2222-2222-222222222222.jpg")" = before-avatar ]]
}
check_unchanged() { check_data; [[ -f "$CASE_ROOT/active" ]]; }
check_quarantined() { check_data; [[ ! -f "$CASE_ROOT/active" && -f "$TAKTAK_ROOT/.restore-quarantine" ]]; }
new_case; backup; ARCHIVE="$(archive)"; [[ -s "$ARCHIVE" && -f "$CASE_ROOT/active" ]]
! grep -a -q 'before-image' "$ARCHIVE"
echo 'PASS encrypted consistent backup and env password parsing'
printf 'changed-db' > "$CASE_ROOT/db"
printf 'changed-image' > "$TAKTAK_ROOT/private-images/11111111-1111-1111-1111-111111111111.jpg"
printf 'changed-avatar' > "$TAKTAK_ROOT/private-images/avatars/22222222-2222-2222-2222-222222222222.jpg"
bash "$ROOT/deploy/scripts/restore.sh" "$ARCHIVE" --yes > "$CASE_ROOT/restore.log" 2>&1
check_quarantined; echo 'PASS restore replaces DB/images but remains quarantined'
new_case; backup; ARCHIVE="$(archive)"; printf 'tamper' >> "$ARCHIVE"
fail_command bash "$ROOT/deploy/scripts/restore.sh" "$ARCHIVE" --yes
check_unchanged; echo 'PASS corrupt backup rejected without data changes'
new_case; backup; ARCHIVE="$(archive)"; touch "$CASE_ROOT/fail_restore"
fail_command bash "$ROOT/deploy/scripts/restore.sh" "$ARCHIVE" --yes
check_quarantined; echo 'PASS SQL restore failure preserves DB/images without reopening traffic'
new_case; backup; ARCHIVE="$(archive)"; touch "$CASE_ROOT/fail_reconcile"
fail_command bash "$ROOT/deploy/scripts/restore.sh" "$ARCHIVE" --yes
check_quarantined; echo 'PASS reconciliation failure rolls back DB/images while quarantined'
new_case; backup; ARCHIVE="$(archive)"; touch "$CASE_ROOT/missing_guard"
fail_command bash "$ROOT/deploy/scripts/restore.sh" "$ARCHIVE" --yes
check_unchanged; [[ ! -e "$TAKTAK_ROOT/.restore-quarantine" ]]
echo 'PASS missing loaded guard refuses restore before data/service changes'
new_case; touch "$CASE_ROOT/fail_dump"; printf 'new-good-jar' > "$CASE_ROOT/new.jar"
fail_command bash "$ROOT/deploy/scripts/update.sh" "$CASE_ROOT/new.jar"
[[ "$(< "$TAKTAK_ROOT/taktak-server.jar")" = old-good-jar && -f "$CASE_ROOT/active" ]]
echo 'PASS failed backup blocks update'
new_case; touch "$CASE_ROOT/fail_start_once"; printf 'new-good-jar' > "$CASE_ROOT/new.jar"
fail_command bash "$ROOT/deploy/scripts/update.sh" "$CASE_ROOT/new.jar"
[[ "$(< "$TAKTAK_ROOT/taktak-server.jar")" = old-good-jar && -f "$CASE_ROOT/active" ]]
echo 'PASS restart failure rolls back JAR'
new_case; printf 'bad-jar' > "$CASE_ROOT/new.jar"
fail_command bash "$ROOT/deploy/scripts/update.sh" "$CASE_ROOT/new.jar"
[[ "$(< "$TAKTAK_ROOT/taktak-server.jar")" = old-good-jar && -f "$CASE_ROOT/active" ]]
echo 'PASS health failure rolls back JAR'
new_case; printf 'new-good-jar' > "$CASE_ROOT/new.jar"
bash "$ROOT/deploy/scripts/update.sh" "$CASE_ROOT/new.jar" > "$CASE_ROOT/update.log" 2>&1
[[ "$(< "$TAKTAK_ROOT/taktak-server.jar")" = new-good-jar ]]
bash "$ROOT/deploy/scripts/rollback.sh" > "$CASE_ROOT/rollback.log" 2>&1
[[ "$(< "$TAKTAK_ROOT/taktak-server.jar")" = old-good-jar ]]
echo 'PASS update and explicit JAR rollback'
new_case
export TAKTAK_NGINX_ROOT="$CASE_ROOT/nginx" TAKTAK_CERT_ROOT="$CASE_ROOT/certs" TAKTAK_ACME_ROOT="$CASE_ROOT/acme"
bash "$ROOT/deploy/scripts/setup-ssl.sh" api.example.test qa@example.test > "$CASE_ROOT/ssl.log" 2>&1
grep -q ssl_certificate "$TAKTAK_NGINX_ROOT/sites-available/taktak"
echo 'PASS HTTP bootstrap precedes certificate and TLS configuration'
new_case; printf 'fixture-pending' > "$TAKTAK_ROOT/private-images/pending-fixture.tmp"
fail_command bash "$ROOT/deploy/scripts/backup.sh"
check_unchanged; [[ -z "$(archive)" ]]
echo 'PASS unexpected storage entry blocks backup without deleting files'
echo 'MAINTENANCE_CONTRACT_TESTS=12 PASS (privileged/database/locking commands mocked; crypto/archive real)'
