# Locked accessibility service recovery

SUMMARY: Keep immediate service toggling separate from persistent locking; only locked components belong in `services.list` and are restored by the Magisk background loop.
READ WHEN: before changing service controls, `a11yctl` persistence semantics, the manager's lock UI, or the module background service.

---

The manager exposes two distinct operations. The switch calls `a11yctl enable` or `disable` and changes only the current Android setting. The lock calls `a11yctl add` or `remove`; locking also enables the service immediately, while unlocking leaves its current enabled state unchanged.

`/data/adb/accessibility-manager/services.list` is therefore the lock list, not a mirror of every currently enabled service. The manager reads it through `a11yctl configured` and disables the off switch for locked rows because the background module would immediately reverse that operation.

`service.sh` applies the lock list after boot and then every `LOCK_WATCH_INTERVAL` seconds. The default is 3 seconds. The fallback must stay in `service.sh`, not only in `config.conf.example`, so existing installations that predate the setting still gain automatic restoration after the module update. A value of `0` intentionally disables runtime polling and keeps boot-only restoration.

Service descriptions come from `AccessibilityServiceInfo.loadDescription`. Treat missing or broken application resources as an empty description and show localized fallback text in the UI.

Device validation must cover both ways a locked service can be lost: remove its component from `enabled_accessibility_services`, and set the global `accessibility_enabled` flag to `0`. Confirm restoration while the manager app is closed, then unlock and confirm the module no longer restores it.
