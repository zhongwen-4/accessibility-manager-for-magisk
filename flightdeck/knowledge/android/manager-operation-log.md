# Manager operation log checklist

SUMMARY: Always keep manager logs bounded, structured, private, and free of raw Root command output while recording enough high-level events to diagnose user-visible operations.
READ WHEN: before adding manager operations, changing log persistence, or exposing diagnostic data from the Android manager.

---

The manager persists operation logs as structured JSON in private `SharedPreferences`. Keep at most 200 entries with timestamp, severity, and a localized high-level message. Storage order is oldest first; the UI reverses it for newest-first display.

Record app startup, module checks and outcomes, service-list loads, service toggle outcomes, and reboot requests. Never persist raw shell commands, stdout, stderr, Root authorization details, or unrelated device data.

The Miuix log page provides explicit copy and clear actions. Copying is user initiated and includes formatted timestamps and severity; clearing removes persisted and in-memory entries together.
