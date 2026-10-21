#!/usr/bin/env bash
# Chi cai script/SQL/guard da duoc duyet; khong goi restore, SQL, restart hoac migration.
set -euo pipefail
umask 077
[[ $(id -u) == 0 && $# == 3 ]] || exit 1
for digest in "$@"; do [[ $digest =~ ^[a-f0-9]{64}$ ]] || exit 1; done
stage=$(cd "$(dirname "$0")" && pwd)
app=/var/taktak
scripts=$app/scripts
dropins=/etc/systemd/system/taktak.service.d
guard=$dropins/30-restore-quarantine.conf
unit=/etc/systemd/system/taktak.service
[[ -d $scripts && ! -L $scripts && ! -L $app && -f $unit && ! -L $unit ]] || exit 1
[[ ! -L $dropins && ! -e $app/.restore-quarantine && ! -L $app/.restore-quarantine ]] || exit 1
[[ ! -e $guard && ! -L $guard ]] || { echo 'EXISTING_DROPIN_REQUIRES_REVIEW'; exit 1; }
[[ ! -e $scripts/reconcile-restore.sql && ! -L $scripts/reconcile-restore.sql ]] || { echo 'EXISTING_SQL_REQUIRES_REVIEW'; exit 1; }
[[ -f $scripts/restore.sh && ! -L $scripts/restore.sh ]] || exit 1
[[ ! -e $scripts/.restore-security-candidate && ! -L $scripts/.restore-security-candidate && ! -L $app/maintenance-backups ]] || exit 1
[[ $(stat -c '%U:%G:%a' "$scripts") == root:root:755 ]] || exit 1
exec 9> "$app/.maintenance.lock"
flock -n 9 || { echo 'MAINTENANCE_BUSY'; exit 1; }
systemctl is-active --quiet taktak.service
[[ $(systemctl show taktak.service -p NeedDaemonReload --value) == no ]] || exit 1
hash() { sha256sum "$1" | cut -d' ' -f1; }
[[ $(hash "$scripts/restore.sh") == c6a7ca4624217470f8312e770c573210c7a902d1caea195bfc76a96a27ed3810 ]] || { echo 'RESTORE_CHANGED_SINCE_REVIEW'; exit 1; }
[[ $(hash "$scripts/common.sh") == 524208b6830f343f4b8e86d9e18d1d87c5207c376e4f6f6f4f7ceba7f42f40a4 ]] || exit 1
[[ $(hash "$scripts/backup.sh") == 6f437ea3abfc2e08a58b9cb909c67eca93d533de78793defb063d2ba09326adf ]] || exit 1
pid=$(systemctl show taktak.service -p MainPID --value)
jar_hash=$(hash "$app/taktak-server.jar")
env_hash=$(hash "$app/.env")
unit_hash=$(hash "$unit")
curl -fsS --max-time 5 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'
mkdir -p "$app/maintenance-backups"
chmod 700 "$app/maintenance-backups"
backup=$(mktemp -d "$app/maintenance-backups/restore-security-20261021-XXXXXXXX")
cp -p -- "$scripts/restore.sh" "$backup/restore.sh"
cp -p -- "$unit" "$backup/taktak.service"
mkdir "$backup/candidate"
for name in restore.sh reconcile-restore.sql 30-restore-quarantine.conf; do
    [[ -f $stage/$name && ! -L $stage/$name ]] || exit 1
    sed 's/\r$//' "$stage/$name" > "$backup/candidate/$name"
done
[[ $(hash "$backup/candidate/restore.sh") == "$1" && $(hash "$backup/candidate/reconcile-restore.sql") == "$2" && $(hash "$backup/candidate/30-restore-quarantine.conf") == "$3" ]] || exit 1
bash -n "$backup/candidate/restore.sh"
{ cat "$unit"; printf '\n'; cat "$backup/candidate/30-restore-quarantine.conf"; } > "$backup/candidate/taktak.service"
systemd-analyze verify "$backup/candidate/taktak.service"
changed=0
rollback() {
    local result=$?
    trap - EXIT
    if [[ $result != 0 && $changed == 1 ]]; then
        # Chi quay lui config/script vua cai, khong dong vao data hoac process.
        install -o root -g root -m 750 "$backup/restore.sh" "$scripts/restore.sh"
        if [[ -e $scripts/reconcile-restore.sql ]]; then mv -- "$scripts/reconcile-restore.sql" "$backup/failed-reconcile-restore.sql"; fi
        if [[ -e $guard ]]; then mv -- "$guard" "$backup/failed-30-restore-quarantine.conf"; fi
        systemctl daemon-reload || true
        echo "ROLLOUT_ROLLED_BACK_NO_RESTART; BACKUP=$backup"
    fi
    exit "$result"
}
trap rollback EXIT
changed=1
install -o root -g root -m 640 "$backup/candidate/reconcile-restore.sql" "$scripts/reconcile-restore.sql"
install -d -o root -g root -m 755 "$dropins"
install -o root -g root -m 644 "$backup/candidate/30-restore-quarantine.conf" "$guard"
install -o root -g root -m 750 "$backup/candidate/restore.sh" "$scripts/.restore-security-candidate"
mv -- "$scripts/.restore-security-candidate" "$scripts/restore.sh"
systemctl daemon-reload
systemctl show taktak.service -p ExecStartPre --value | grep -Fq \
    'path=/usr/bin/test ; argv[]=/usr/bin/test ! -e /var/taktak/.restore-quarantine ; ignore_errors=no'
sudo -u taktak /usr/bin/test ! -e "$app/.restore-quarantine"
[[ $(hash "$scripts/restore.sh") == "$1" && $(hash "$scripts/reconcile-restore.sql") == "$2" && $(hash "$guard") == "$3" ]] || exit 1
[[ $(hash "$unit") == "$unit_hash" && $(hash "$app/taktak-server.jar") == "$jar_hash" && $(hash "$app/.env") == "$env_hash" ]] || exit 1
for sample in 1 2 3 4; do
    systemctl is-active --quiet taktak.service
    [[ $(systemctl show taktak.service -p MainPID --value) == "$pid" ]] || exit 1
    curl -fsS --max-time 5 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'
    echo "HEALTH_SAMPLE_$sample=UP_PID_UNCHANGED"
    if [[ $sample != 4 ]]; then sleep 10; fi
done
changed=0
trap - EXIT
echo "RESTORE_SECURITY_PRODUCTION_INSTALLED; BACKUP=$backup"
echo 'SQL_INSTALLED_NOT_EXECUTED; BASE_UNIT_JAR_ENV_UNCHANGED; NO_RESTART_OR_DATA_RESTORE'
stat -c '%a %U:%G %n' "$scripts/restore.sh" "$scripts/reconcile-restore.sql" "$guard"
