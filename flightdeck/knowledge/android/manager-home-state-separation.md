# Manager startup state separation checklist

SUMMARY: Always keep the accessibility-service homepage independent from Magisk module installation state so Root setup cannot replace or hide the list.
READ WHEN: before changing manager startup, service-list loading, automatic module installation, or Root-control availability.

---

The Android framework can enumerate installed and enabled accessibility services without Root. Load that list immediately when the manager starts, before queuing Magisk detection or installation.

Do not represent list loading and module setup as one mutually exclusive screen state. The Miuix homepage remains the permanent screen; module installing, disabled, failed, and reboot-required states appear in an inline status card. Keep service switches disabled until the module reports `READY`, while refresh and module retry remain available.
