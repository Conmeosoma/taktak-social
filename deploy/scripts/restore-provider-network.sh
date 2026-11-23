#!/usr/bin/env bash
# Khôi phục mạng Internet DHCP, giữ route SSH qua VLAN quản trị.
set -euo pipefail
vlan_id=$(tr -d '[:space:]' < /etc/changip_vlan.conf)
[[ "$vlan_id" =~ ^[0-9]+$ ]] && (( vlan_id > 0 && vlan_id < 4095 && vlan_id != 50 ))
management_iface=$(ip -o -4 addr show | awk '$2 ~ /\.50$/ {print $2; exit}')
test -n "$management_iface"
physical_iface=${management_iface%.50}
internet_iface="$physical_iface.$vlan_id"
management_ip=$(ip -o -4 addr show dev "$management_iface" | awk '{split($4,a,"/"); print a[1]; exit}')
management_network=$(ip -4 route show dev "$management_iface" proto kernel | awk 'NR==1 {print $1}')
management_gateway=$(ip -4 route show default dev "$management_iface" | awk 'NR==1 {print $3}')
if [ -z "$management_gateway" ]; then
    management_gateway=$(ip -4 route show table 200 default | awk 'NR==1 {print $3}')
fi
test -n "$management_gateway"
ip route replace "$management_network" dev "$management_iface" src "$management_ip" table 200
ip route replace default via "$management_gateway" dev "$management_iface" table 200
if ! ip rule show | grep -F "from $management_ip " | grep -Eq 'lookup (200|ssh_rt)( |$)'; then
    ip rule add from "$management_ip/32" table 200 priority 100
fi
if ! ip link show "$internet_iface" >/dev/null 2>&1; then
    ip link add link "$physical_iface" name "$internet_iface" type vlan id "$vlan_id"
fi
ip link set "$internet_iface" up
if ! ip -o -4 addr show dev "$internet_iface" | grep -q 'inet '; then
    timeout 60 dhclient -1 "$internet_iface"
fi
gateway=$(awk -v iface="$internet_iface" '
    /^lease[[:space:]]*\{/ {matched=0}
    /interface "/ {matched=($2 == "\"" iface "\";")}
    matched && /option routers/ {last=$3; gsub(/;/,"",last)}
    END {print last}' /var/lib/dhcp/dhclient.leases)
test -n "$gateway"
ip route replace default via "$gateway" dev "$internet_iface"
ip -4 route get 1.1.1.1 | grep -Fq "dev $internet_iface"
