# Miuix Android toolchain compatibility checklist

SUMMARY: Always align Kotlin, Compose, compile SDK, Android Gradle Plugin, and Gradle with the selected Miuix Android artifact before upgrading any one of them.
READ WHEN: before any Miuix, Compose, Kotlin, Android Gradle Plugin, Gradle, or compile SDK upgrade in the Android manager.

---

The manager currently uses Miuix `0.8.8`. Its published Android metadata requires compile SDK 36 and Kotlin stdlib 2.3.20, while its JetBrains Compose Foundation 1.10.3 dependency maps to AndroidX Compose Foundation 1.10.5. Keep the AndroidX Compose BOM on `2026.03.00` unless the Miuix version changes.

The verified toolchain is Kotlin/Compose plugin 2.3.20, Android Gradle Plugin 8.13.1, Gradle 9.2.0, compile/target SDK 36, Activity Compose 1.13.0, and Core KTX 1.18.0.

Do not independently upgrade to Core 1.19: it requires compile SDK 37 and Android Gradle Plugin 9.1. Miuix 0.9.3 likewise moves the project to the API 37/Kotlin 2.4 generation. Treat either upgrade as a coordinated toolchain migration, then rerun unit tests, APK assembly, and Android Lint.

Android Gradle Plugin 8.7 cannot reliably run current Kotlin Compose Lint detectors; use the verified toolchain instead of suppressing detector failures.

SukiSU Ultra `main@278d822` uses Miuix 0.9.2, Kotlin 2.4, Android Gradle Plugin 9.2.1, and API 37. When adapting its manager UI to this API 36 project, port the `HomeMiuix.kt` layout primitives and the standard `BottomBarMiuix.kt` navigation branch. Its custom blurred `FloatingBottomBar` depends on the newer Miuix blur stack and must wait for a coordinated API 37 toolchain migration.
