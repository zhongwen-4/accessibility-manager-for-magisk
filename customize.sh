#!/system/bin/sh

CONFIG_DIR=/data/adb/accessibility-manager

ui_print "- 正在安装无障碍管理模块"

mkdir -p "$CONFIG_DIR"

if [ ! -f "$CONFIG_DIR/config.conf" ]; then
  cp -f "$MODPATH/config.conf.example" "$CONFIG_DIR/config.conf"
fi

if [ ! -f "$CONFIG_DIR/services.list" ]; then
  cp -f "$MODPATH/services.list.example" "$CONFIG_DIR/services.list"
fi

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/customize.sh" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/system/bin/a11yctl" 0 0 0755
set_perm_recursive "$CONFIG_DIR" 0 0 0755 0644

ui_print "- 配置目录：$CONFIG_DIR"
ui_print "- 重启后可运行：su -c a11yctl help"
