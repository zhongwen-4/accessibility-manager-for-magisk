# ⚠ Accessibility service list can be complete but smaller than expected

SUMMARY: Compare the manager, AccessibilityManager, and PackageManager counts before changing enumeration code; a virtual profile or current user may genuinely register only a few accessibility services.
READ WHEN: when the manager shows fewer accessibility services than a user expects on a connected Android device.

---

The manager uses `AccessibilityManager.getInstalledAccessibilityServiceList()`, which asks Android for installed services in the current user; it does not intentionally restrict the list to enabled services. Only components that declare an Android accessibility service with the `android.accessibilityservice.AccessibilityService` intent and `BIND_ACCESSIBILITY_SERVICE` permission belong in this list. Ordinary apps and accessibility features implemented internally by Settings or SystemUI do not.

Before changing the repository or adding package-visibility declarations, compare three sources:

```text
adb shell dumpsys accessibility
adb shell cmd package query-services --user 0 --components -a android.accessibilityservice.AccessibilityService
adb shell cmd package query-services --user 0 --components --query-flags 512 -a android.accessibilityservice.AccessibilityService
```

Also inspect `adb shell pm list users`, the manager's structured log count, the Android API level, build fingerprint, and virtualization packages. If `installedServiceCount`, the package queries, and the manager count match, the manager is not dropping entries; investigate the current Android user, work/second-space profiles, disabled or uninstalled packages, and whether the ADB target is a virtual environment rather than the host device.

The July 2026 target at `192.168.31.50:5555` claimed Redmi K70 model `23113RKC6C` / codename `vermeer`, but reported Android 12 / API 32 and registered MuMu components such as `com.netease.mumu.cloner`, even though a physical Redmi K70 launched with Android 14. Android, PackageManager, and the manager all reported exactly two services: Lawnchair and MT Manager's Activity Record service. This is consistent with a virtual profile spoofing K70 properties, not an app-side enumeration failure. Adding `<queries>` would not create services that the target system itself has not registered.
