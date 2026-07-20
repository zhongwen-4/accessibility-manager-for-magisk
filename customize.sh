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
set_perm "$MODPATH/install-manager.sh" 0 0 0755
set_perm "$MODPATH/system/bin/a11yctl" 0 0 0755
set_perm_recursive "$CONFIG_DIR" 0 0 0755 0644

if [ -r "$MODPATH/manager.apk" ]; then
  ui_print "- 正在安装无障碍管理器"
  sh "$MODPATH/install-manager.sh"
  manager_result=$?
  case "$manager_result" in
    0) ui_print "- 管理器安装完成" ;;
    2) ui_print "- 当前无法调用系统包管理器，将在开机后安装管理器" ;;
    *) ui_print "- 管理器安装失败，将在开机后重试" ;;
  esac
fi

ui_print "- 配置目录：$CONFIG_DIR"
ui_print "- 重启后可运行：su -c a11yctl help"
