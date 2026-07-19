# Index — automatic module install

## State

Implementation complete. Awaiting validation on a rooted device running Magisk.

## Next

Install the generated APK on a rooted Magisk device; verify automatic installation, reboot, and service toggling.

## Read now

- `flightdeck/knowledge/android/automatic-magisk-module-packaging.md`

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

## Open questions

- Rooted-device behavior remains unverified because no Android device is connected.
