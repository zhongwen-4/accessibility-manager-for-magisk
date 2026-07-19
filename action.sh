#!/system/bin/sh

MODDIR=${0%/*}
A11YCTL="$MODDIR/system/bin/a11yctl"

print_line() {
  if command -v ui_print >/dev/null 2>&1; then
    ui_print "$*"
  else
    printf '%s\n' "$*"
  fi
}

print_line "正在应用无障碍服务配置..."
if "$A11YCTL" apply; then
  print_line ""
  print_line "模块配置的服务："
  "$A11YCTL" configured
  print_line ""
  print_line "当前已启用的服务："
  "$A11YCTL" enabled
else
  print_line "应用失败，请检查服务组件名和模块日志。"
  exit 1
fi
