#!/usr/bin/env bash
# Chỉ chạy khi người dùng duyệt bật FCM và restart service đang có.
set -euo pipefail
[[ $(id -u) == 0 ]] || { echo 'ROOT_REQUIRED'; exit 1; }
app=/var/taktak
env_file=$app/.env
staged=$app/.qa-secrets-staging/firebase-admin.json
credential=$app/firebase-admin.json
[[ -f $env_file && ! -L $env_file && -f $staged && ! -L $staged ]] || exit 1
if [[ -e $credential ]]; then
    [[ ! -L $credential ]] && cmp --silent "$staged" "$credential" || { echo 'EXISTING_CREDENTIAL_REQUIRES_REVIEW'; exit 1; }
fi
[[ $(grep -c '^FCM_ENABLED=false$' "$env_file") == 1 ]] || { echo 'UNEXPECTED_FCM_CONFIGURATION'; exit 1; }
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["type"]=="service_account" and d["project_id"]=="taktak-71ed5"' "$staged"
backup=$(mktemp "$app/.env.pre-fcm.XXXXXX")
cp --preserve=mode,ownership "$env_file" "$backup"
install -o taktak -g taktak -m 600 "$staged" "$credential"
candidate=$(mktemp "$app/.env.fcm-candidate.XXXXXX")
sed 's/^FCM_ENABLED=false$/FCM_ENABLED=true/' "$env_file" > "$candidate"
chmod 600 "$candidate"
chown --reference="$env_file" "$candidate"
mv -- "$candidate" "$env_file"
recover() {
    cp --preserve=mode,ownership "$backup" "$env_file"
    systemctl restart taktak.service || true
    echo 'FCM_ENABLE_FAILED_CONFIGURATION_RESTORED'
    exit 1
}
trap recover ERR
systemctl restart taktak.service
healthy=0
for attempt in $(seq 1 120); do
    if curl --fail --silent --max-time 2 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
        healthy=1; break
    fi
    sleep 1
done
[[ $healthy == 1 ]] || recover
trap - ERR
echo "FCM_ENABLED_HEALTH_UP; ENV_BACKUP=$backup"
# Bản gốc local và credential đã cài vẫn giữ; xóa đúng bản staging tạm.
rm -- "$staged"
