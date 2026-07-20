#!/system/bin/sh

MODDIR=${0%/*}
MANAGER_APK="$MODDIR/manager.apk"
TEMP_DIR=${A11Y_MANAGER_TEMP_DIR:-/data/local/tmp}
TEMP_APK="$TEMP_DIR/accessibility-manager-$$.apk"

[ -r "$MANAGER_APK" ] || exit 0

if [ "$(getprop sys.boot_completed)" != "1" ] || ! command -v pm >/dev/null 2>&1; then
  exit 2
fi

cleanup() {
  rm -f "$TEMP_APK"
}
trap cleanup EXIT HUP INT TERM

cp -f "$MANAGER_APK" "$TEMP_APK" || exit 1
chmod 0644 "$TEMP_APK" || exit 1
chown 2000:2000 "$TEMP_APK" 2>/dev/null || true
restorecon "$TEMP_APK" 2>/dev/null || true

if pm install -r -g --user 0 "$TEMP_APK" >/dev/null 2>&1 || \
   pm install -r --user 0 "$TEMP_APK" >/dev/null 2>&1; then
  rm -f "$MANAGER_APK"
  exit 0
fi

exit 1
