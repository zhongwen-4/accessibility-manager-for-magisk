#!/usr/bin/env sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CTL="$ROOT_DIR/system/bin/a11yctl"
TEST_ROOT=$(mktemp -d)
MOCK_BIN="$TEST_ROOT/bin"
CONFIG_DIR="$TEST_ROOT/config"

cleanup() {
  rm -rf "$TEST_ROOT"
}
trap cleanup EXIT HUP INT TERM

mkdir -p "$MOCK_BIN" "$CONFIG_DIR"
export MOCK_ROOT="$TEST_ROOT"
export A11Y_MANAGER_CONFIG_DIR="$CONFIG_DIR"
export PATH="$MOCK_BIN:$PATH"

cat > "$MOCK_BIN/cmd" <<'EOF'
#!/usr/bin/env sh
if [ "$1 $2" = "activity get-current-user" ]; then
  printf '10\n'
  exit 0
fi
exit 1
EOF

cat > "$MOCK_BIN/am" <<'EOF'
#!/usr/bin/env sh
printf '10\n'
EOF

cat > "$MOCK_BIN/settings" <<'EOF'
#!/usr/bin/env sh
if [ "$1" = "--user" ]; then
  user=$2
  shift 2
else
  user=0
fi

operation=$1
namespace=$2
key=$3
value=${4-}
[ "$namespace" = "secure" ] || exit 1

case "$operation:$key" in
  get:enabled_accessibility_services)
    if [ -f "$MOCK_ROOT/enabled.$user" ]; then
      cat "$MOCK_ROOT/enabled.$user"
    else
      printf 'null\n'
    fi
    ;;
  put:enabled_accessibility_services)
    printf '%s\n' "$value" > "$MOCK_ROOT/enabled.$user"
    ;;
  put:accessibility_enabled)
    printf '%s\n' "$value" > "$MOCK_ROOT/global.$user"
    ;;
  *) exit 1 ;;
esac
EOF

chmod +x "$MOCK_BIN/cmd" "$MOCK_BIN/am" "$MOCK_BIN/settings"

cat > "$CONFIG_DIR/config.conf" <<'EOF'
USER_ID=auto
MODE=ensure
ALLOW_EMPTY_EXACT=0
EOF
: > "$CONFIG_DIR/services.list"

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_equal() {
  expected=$1
  actual=$2
  label=$3
  [ "$actual" = "$expected" ] || fail "$label (expected '$expected', got '$actual')"
}

run_ctl() {
  sh "$CTL" "$@"
}

alpha=com.example.alpha/.AlphaService
beta=com.example.beta/com.example.beta.BetaService

assert_equal 10 "$(run_ctl user)" "current user detection"
printf '%s\n' "$alpha" > "$TEST_ROOT/enabled.10"
assert_equal "$alpha" "$(run_ctl enabled)" "enabled listing"

run_ctl add "$beta" >/dev/null
assert_equal "$beta" "$(run_ctl configured)" "managed add"
run_ctl apply >/dev/null
assert_equal "$alpha:$beta" "$(cat "$TEST_ROOT/enabled.10")" "ensure preserves existing service"
assert_equal 1 "$(cat "$TEST_ROOT/global.10")" "global accessibility flag enabled"

run_ctl apply >/dev/null
assert_equal "$alpha:$beta" "$(cat "$TEST_ROOT/enabled.10")" "apply does not duplicate components"
run_ctl disable "$alpha" >/dev/null
assert_equal "$beta" "$(cat "$TEST_ROOT/enabled.10")" "disable removes only target"

status=$(run_ctl status "$beta")
printf '%s\n' "$status" | grep -qx '已启用=是' || fail "status enabled flag"
printf '%s\n' "$status" | grep -qx '已配置=是' || fail "status configured flag"

run_ctl remove "$beta" >/dev/null
assert_equal "$beta" "$(cat "$TEST_ROOT/enabled.10")" "remove does not disable service"
run_ctl capture >/dev/null
assert_equal "$beta" "$(run_ctl configured)" "capture enabled services"

mkdir "$CONFIG_DIR/.lock"
printf '99999999\n' > "$CONFIG_DIR/.lock/pid"
run_ctl add "$alpha" >/dev/null
assert_equal "$beta
$alpha" "$(run_ctl configured)" "stale lock recovery"

cat > "$CONFIG_DIR/config.conf" <<'EOF'
USER_ID=10
MODE=exact
ALLOW_EMPTY_EXACT=0
EOF
: > "$CONFIG_DIR/services.list"
printf '%s\n' "$alpha" > "$TEST_ROOT/enabled.10"
if run_ctl apply >/dev/null 2>&1; then
  fail "empty exact configuration should be rejected"
fi
assert_equal "$alpha" "$(cat "$TEST_ROOT/enabled.10")" "rejected exact apply preserves state"

printf '%s\n' "$beta" > "$CONFIG_DIR/services.list"
run_ctl apply >/dev/null
assert_equal "$beta" "$(cat "$TEST_ROOT/enabled.10")" "exact mode replaces enabled list"
run_ctl disable "$beta" >/dev/null
assert_equal '' "$(cat "$TEST_ROOT/enabled.10")" "disable last service clears list"
assert_equal 0 "$(cat "$TEST_ROOT/global.10")" "global accessibility flag disabled"

printf 'All a11yctl tests passed.\n'
