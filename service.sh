#!/system/bin/sh

MODDIR=${0%/*}
CONFIG_DIR=/data/adb/accessibility-manager
A11YCTL="$MODDIR/system/bin/a11yctl"
LOG_TAG=AccessibilityManager

log_message() {
  log -t "$LOG_TAG" "$*"
}

config_value() {
  key=$1
  fallback=$2
  value=
  if [ -r "$CONFIG_DIR/config.conf" ]; then
    value=$(sed -n "s/^[[:space:]]*$key[[:space:]]*=[[:space:]]*//p" "$CONFIG_DIR/config.conf" | tail -n 1 | tr -d '\r' | sed 's/[[:space:]]*$//')
  fi
  [ -n "$value" ] && printf '%s\n' "$value" || printf '%s\n' "$fallback"
}

until [ "$(getprop sys.boot_completed)" = "1" ]; do
  sleep 2
done

boot_delay=$(config_value BOOT_DELAY 10)
case "$boot_delay" in
  ''|*[!0-9]*) boot_delay=10 ;;
esac
[ "$boot_delay" -gt 0 ] && sleep "$boot_delay"

if "$A11YCTL" apply --quiet; then
  log_message "已应用无障碍服务配置"
else
  log_message "应用无障碍服务配置失败"
fi

watch_interval=$(config_value WATCH_INTERVAL 0)
case "$watch_interval" in
  ''|*[!0-9]*) watch_interval=0 ;;
esac

while [ "$watch_interval" -gt 0 ]; do
  sleep "$watch_interval"
  "$A11YCTL" apply --quiet || log_message "定时应用配置失败"
done
