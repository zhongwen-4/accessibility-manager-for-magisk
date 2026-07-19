# Index — Miuix UI migration

## State

Implementation complete. Awaiting UI and Root behavior validation on a rooted Android device.

## Next

Run the `构建管理器` workflow once, then install its APK artifact on a rooted Magisk device; inspect the dashboard, floating navigation, light/dark UI states, and verify module installation, reboot, refresh, and service toggling.

## Read now

- `flightdeck/knowledge/android/automatic-magisk-module-packaging.md`
- `flightdeck/knowledge/android/miuix-compose-toolchain-compatibility.md`

## Read if

## Progress

Done:
- Replaced the Java/XML Material Views activity and adapter with a Kotlin Compose screen built from Miuix components.
- Added Miuix top app bar, service cards, application icons, switches, progress indicators, empty/error/retry states, and reboot-required handling.
- Preserved automatic bundled Magisk module installation and Root-based service toggling.
- Upgraded the verified Android toolchain to Kotlin 2.3.20, Android Gradle Plugin 8.13.1, Gradle 9.2.0, and API 36.
- Added Miuix attribution and third-party notice.

Verified:
- Five unit tests pass and Android Lint reports no issues.
- The debug APK assembles and its v2 signature verifies.
- The generated Magisk module ZIP is readable and byte-identical to the APK asset.
- The `构建管理器` workflow now tests, verifies, packages, and uploads the manager APK.
- Restored the service list as the permanent Miuix homepage and moved automatic-install progress, errors, retry, and reboot actions into an inline status card.
- Rebuilt the homepage around the SukiSU Ultra reference: large page title, module status card, service metrics, manager/device information, and Miuix floating navigation to the service list.
- Manager version advanced to 2.1.0 (5).

## Open questions

- Rooted-device UI and behavior validation requires a connected Android device.
- Dashboard screenshots could not be captured because no Android device or emulator is connected.
- The shell regression test could not be rerun in this Windows environment because no Bash or WSL distribution is installed.
