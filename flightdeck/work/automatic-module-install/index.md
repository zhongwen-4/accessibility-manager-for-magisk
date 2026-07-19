# Index — automatic module install

## State

Implementation complete. Awaiting validation on a rooted device running Magisk.

## Next

Run the `构建管理器` and `打包模块` workflows once, then install the APK artifact on a rooted Magisk device; verify automatic installation, reboot, and service toggling.

## Read now

- `flightdeck/knowledge/android/automatic-magisk-module-packaging.md`
- `flightdeck/knowledge/android/locked-accessibility-service-recovery.md`
- `flightdeck/knowledge/android/manager-operation-log.md`

## Read if

## Progress

Done:
- Generate the bundled module ZIP from canonical module files during Android builds.
- Detect installed, pending, mounted, outdated, and disabled module states.
- Automatically install or update through `magisk --install-module` after Root authorization.
- Show retry, disabled-module, failure, and reboot-required UI states.

Verified:
- Five unit tests pass.
- Android Lint reports no issues.
- APK signature verifies.
- Bundled module archive is valid and byte-identical to the APK asset.
- Existing `a11yctl` regression tests pass.
- The `构建管理器` workflow now runs the automated checks and verifies the embedded module before uploading the APK.
- Module setup states now appear inside the Miuix homepage instead of replacing the service list; controls remain disabled until the module is ready.
- The `打包模块` workflow now regression-tests, reproducibly packages, validates, and uploads the standalone Magisk ZIP.
- Module detection, installation, reboot requirements, and failures now produce bounded high-level entries in the manager log without storing raw Root output.
- Module v1.0.3 restores locked services every three seconds by default, including on upgraded installations whose existing config lacks the new interval key.
- The manager now treats `services.list` as an explicit lock list; ordinary switches no longer silently change persistent recovery state.
- Eight manager unit tests pass, Android Lint reports no issues, APK 2.4.0 (8) has a valid v2 signature, and its generated module asset hash matches the embedded asset.

## Open questions

- Rooted-device behavior remains unverified because no Android device is connected.
- Automatic restoration while the manager is closed remains unverified on a rooted device.
