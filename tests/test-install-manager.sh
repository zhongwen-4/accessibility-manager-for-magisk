#!/usr/bin/env sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TEST_ROOT=$(mktemp -d)
MODULE_DIR="$TEST_ROOT/module"
MOCK_BIN="$TEST_ROOT/bin"
SCRIPT="$MODULE_DIR/install-manager.sh"

cleanup() {
  rm -rf "$TEST_ROOT"
}
trap cleanup EXIT HUP INT TERM

mkdir -p "$MODULE_DIR" "$MOCK_BIN" "$TEST_ROOT/data/local/tmp"
cp "$ROOT_DIR/install-manager.sh" "$SCRIPT"

cat > "$MOCK_BIN/getprop" <<'EOF'
#!/usr/bin/env sh
printf '%s\n' "${MOCK_BOOT_COMPLETED:-1}"
EOF

cat > "$MOCK_BIN/pm" <<'EOF'
#!/usr/bin/env sh
count=0
[ ! -r "$MOCK_ROOT/pm-count" ] || count=$(cat "$MOCK_ROOT/pm-count")
count=$((count + 1))
printf '%s\n' "$count" > "$MOCK_ROOT/pm-count"
printf '%s\n' "$*" >> "$MOCK_ROOT/pm-args"
case "${MOCK_PM_MODE:-success}" in
  success) exit 0 ;;
  fallback) [ "$count" -gt 1 ] ;;
  failure) exit 1 ;;
esac
EOF

cat > "$MOCK_BIN/restorecon" <<'EOF'
#!/usr/bin/env sh
exit 0
EOF

chmod +x "$MOCK_BIN"/* "$SCRIPT"
export PATH="$MOCK_BIN:$PATH"
export MOCK_ROOT="$TEST_ROOT"
export A11Y_MANAGER_TEMP_DIR="$TEST_ROOT/data/local/tmp"

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

reset_case() {
  rm -f "$MODULE_DIR/manager.apk" "$TEST_ROOT/pm-count" "$TEST_ROOT/pm-args"
  rm -rf "$TEST_ROOT/data/local/tmp"
  mkdir -p "$TEST_ROOT/data/local/tmp"
}

reset_case
sh "$SCRIPT" || fail "missing APK should be a no-op"

reset_case
printf 'apk' > "$MODULE_DIR/manager.apk"
if MOCK_BOOT_COMPLETED=0 sh "$SCRIPT"; then
  fail "installer should defer before boot completion"
else
  code=$?
  [ "$code" -eq 2 ] || fail "deferred install should return 2"
fi
[ -f "$MODULE_DIR/manager.apk" ] || fail "deferred install should retain APK"

reset_case
printf 'apk' > "$MODULE_DIR/manager.apk"
MOCK_PM_MODE=success sh "$SCRIPT" || fail "manager install should succeed"
[ ! -e "$MODULE_DIR/manager.apk" ] || fail "successful install should remove bundled APK"
[ "$(cat "$TEST_ROOT/pm-count")" = "1" ] || fail "successful install should call pm once"
grep -q -- "install -r -g --user 0 $TEST_ROOT/data/local/tmp/accessibility-manager-" "$TEST_ROOT/pm-args" || \
  fail "manager install should use staged APK and grant flags"

reset_case
printf 'apk' > "$MODULE_DIR/manager.apk"
MOCK_PM_MODE=fallback sh "$SCRIPT" || fail "fallback install should succeed"
[ "$(cat "$TEST_ROOT/pm-count")" = "2" ] || fail "fallback install should call pm twice"
[ ! -e "$MODULE_DIR/manager.apk" ] || fail "fallback success should remove bundled APK"

reset_case
printf 'apk' > "$MODULE_DIR/manager.apk"
if MOCK_PM_MODE=failure sh "$SCRIPT"; then
  fail "failed package install should return non-zero"
fi
[ -f "$MODULE_DIR/manager.apk" ] || fail "failed install should retain APK for retry"

printf 'All manager installer tests passed.\n'
