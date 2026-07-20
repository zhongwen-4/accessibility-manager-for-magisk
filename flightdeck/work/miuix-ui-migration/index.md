# Index — Miuix UI migration

## State

Implementation complete. Awaiting UI and Root behavior validation on a rooted Android device.

## Next

Run the `构建管理器` workflow once, then install its APK artifact on a rooted Magisk device; inspect the dashboard, services, and log pages in light/dark modes, then verify module installation, reboot, refresh, logging, and service toggling.

## Read now

- `flightdeck/knowledge/android/automatic-magisk-module-packaging.md`
- `flightdeck/knowledge/android/miuix-compose-toolchain-compatibility.md`
- `flightdeck/knowledge/android/manager-operation-log.md`
- `flightdeck/knowledge/android/locked-accessibility-service-recovery.md`

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
- Adapted the homepage directly from SukiSU Ultra `HomeMiuix.kt` and the standard branch of `BottomBarMiuix.kt` at `main@278d822`, preserving its TopAppBar, three-card status layout, oversized offset status icon, Tilt feedback, information-card typography, and navigation structure.
- Added the GPL-3.0 source revision and adapted-file attribution to `THIRD_PARTY_NOTICES.md`.
- Added a persistent Miuix log page with severity icons, newest-first records, copy and clear actions, and high-level events for module and accessibility-service operations.
- Manager version advanced to 2.3.0 (7).
- Service cards now expand on tap to show the app-provided accessibility description and component name, with localized fallback text when no description is declared.
- Each service row now has an independent lock control for automatic restoration; locked services cannot be switched off until unlocked.
- Manager version advanced to 2.4.0 (8); eight unit tests and Android Lint pass.
- The services page now exposes a Miuix search action and inline query field. Filtering matches localized service labels, accessibility descriptions, package names, and full component names without changing the underlying enabled or locked state.
- Connected-device validation confirmed the empty search hint, live filtering, matching-result count, clear action, and preserved service controls. Manager version advanced to 2.6.0 (10); 13 unit tests and Android Lint pass.
- Search now includes an inline filter action for system/user applications and disabled/enabled services. Selections are ORed within each category and ANDed across application type and service state; an empty or fully selected category is unrestricted.
- Connected-device validation confirmed the filter panel, enabled-only result reduction, active filter tint, result summary, and unchanged lock/switch controls. Manager version advanced to 2.7.0 (11); 13 unit tests pass and Android Lint reports no issues.

## Open questions

- Rooted-device UI and behavior validation requires a connected Android device.
- Dashboard screenshots could not be captured because no Android device or emulator is connected.
- Log persistence, clipboard output, and clear behavior require connected-device validation.
- The shell regression test could not be rerun in this Windows environment because no Bash or WSL distribution is installed.
