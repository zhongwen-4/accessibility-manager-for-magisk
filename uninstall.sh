#!/system/bin/sh

MODDIR=${0%/*}
CONFIG_DIR=${A11Y_MANAGER_CONFIG_DIR:-/data/adb/accessibility-manager}
A11YCTL="$MODDIR/system/bin/a11yctl"

cleanup_failed=0
if [ -d "$CONFIG_DIR" ]; then
  if [ -x "$A11YCTL" ]; then
    A11Y_MANAGER_CONFIG_DIR="$CONFIG_DIR" sh "$A11YCTL" disable-configured --quiet || cleanup_failed=1
  else
    cleanup_failed=1
  fi

  if [ "$cleanup_failed" -ne 0 ]; then
    if command -v log >/dev/null 2>&1; then
      log -t AccessibilityManager "Failed to disable configured accessibility services during uninstall"
    else
      printf '%s\n' "Failed to disable configured accessibility services during uninstall" >&2
    fi
  fi
fi

if [ "$cleanup_failed" -eq 0 ]; then
  rm -rf "$CONFIG_DIR"
else
  exit 1
fi
