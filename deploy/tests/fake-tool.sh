#!/usr/bin/env bash
# Fake các công cụ đặc quyền/DB; chỉ dùng trong root fixture của maintenance-test.sh.
set -euo pipefail
tool="$(basename "$0")"
[[ -n "${CASE_ROOT:-}" && "$CASE_ROOT" = */maintenance.* ]] || exit 90
echo "$tool $*" >> "$CASE_ROOT/calls"
case "$tool" in
  chown|apt-get|flock) exit 0;;
  pg_dump)
    [[ "${PGPASSWORD:-}" = "$TEST_PASSWORD" ]] || exit 91
    [[ ! -f "$CASE_ROOT/fail_dump" ]] || exit 92
    while [[ $# -gt 0 ]]; do if [[ "$1" = -f ]]; then cp "$CASE_ROOT/db" "$2"; exit 0; fi; shift; done
    exit 93;;
  pg_restore)
    [[ "$1" != --list ]] || { test -s "$2"; exit $?; }
    [[ "${PGPASSWORD:-}" = "$TEST_PASSWORD" && "$*" = *--single-transaction* && "$*" = *--exit-on-error* ]] || exit 94
    source_file="${!#}"
    if [[ -f "$CASE_ROOT/fail_restore" && "$source_file" = */db.dump ]]; then exit 95; fi
    cp "$source_file" "$CASE_ROOT/db";;
  psql) [[ "${PGPASSWORD:-}" = "$TEST_PASSWORD" && "$*" = *ON_ERROR_STOP=1* ]] || exit 96
    if [[ "$*" = *'COPY (SELECT'* ]]; then exit 0; fi
    if [[ "$*" = *--single-transaction* ]]; then
        cat >/dev/null
        [[ ! -f "$CASE_ROOT/fail_reconcile" ]] || exit 102
        exit 0
    fi
    printf '%s\n' 11111111-1111-1111-1111-111111111111.jpg avatars/22222222-2222-2222-2222-222222222222.jpg;;
  systemctl)
    case "$1" in
      show)
        [[ ! -f "$CASE_ROOT/missing_guard" ]] || exit 0
        printf '{ path=/usr/bin/test ; argv[]=/usr/bin/test ! -e %s/.restore-quarantine ; ignore_errors=no ; }\n' "$TAKTAK_ROOT";;
      is-active) test -f "$CASE_ROOT/active";;
      stop) rm -f -- "$CASE_ROOT/active";;
      start|restart)
        [[ ! -e "$TAKTAK_ROOT/.restore-quarantine" ]] || exit 103
        if [[ -f "$CASE_ROOT/fail_start_once" ]]; then rm -f -- "$CASE_ROOT/fail_start_once"; exit 97; fi
        touch "$CASE_ROOT/active";;
      reload) :;;
      *) exit 98;;
    esac;;
  curl)
    [[ ! -f "$CASE_ROOT/fail_health" ]] || exit 1
    if [[ -f "$TAKTAK_ROOT/taktak-server.jar" ]]; then ! grep -q bad "$TAKTAK_ROOT/taktak-server.jar"; fi;;
  certbot)
    if [[ "$1" = certonly ]]; then
      [[ "$*" = *--webroot* ]] || exit 99
      ! grep -q ssl_certificate "$TAKTAK_NGINX_ROOT/sites-available/taktak" || exit 100
      mkdir -p "$TAKTAK_CERT_ROOT/live/api.example.test"
      printf 'fixture' > "$TAKTAK_CERT_ROOT/live/api.example.test/fullchain.pem"
      printf 'fixture' > "$TAKTAK_CERT_ROOT/live/api.example.test/privkey.pem"
    fi;;
  nginx)
    if grep -q ssl_certificate "$TAKTAK_NGINX_ROOT/sites-available/taktak"; then
      test -s "$TAKTAK_CERT_ROOT/live/api.example.test/fullchain.pem"
      test -s "$TAKTAK_CERT_ROOT/live/api.example.test/privkey.pem"
    fi;;
  *) exit 101;;
esac
