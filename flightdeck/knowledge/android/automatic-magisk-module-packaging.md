# Android release module-sync checklist

SUMMARY: Always generate the APK's bundled Root module from canonical files, keep it compatible with Magisk and SukiSU Ultra, and derive its version code from module.prop.
READ WHEN: before any Android manager release or any change to module packaging, Root-manager detection, or automatic installation.

---

The Android manager carries `accessibility-manager-module.zip` as a generated asset. Do not maintain a second hand-copied module tree inside `app/`: `app/build.gradle` owns the explicit canonical file list and creates the ZIP during `preBuild`.

`BuildConfig.BUNDLED_MODULE_VERSION_CODE` must come from the root `module.prop`. Runtime installation compares it with both `/data/adb/modules/accessibility_manager/module.prop` and `/data/adb/modules_update/accessibility_manager/module.prop`; Magisk and SukiSU Ultra use these same installed and pending directories, so checking the pending directory prevents repeated installs before reboot on either framework.

Respect `/data/adb/modules/accessibility_manager/disable` even when the installed version is old. Automatic installation must not silently reverse a user's explicit disable action.

SukiSU Ultra `main@278d822` defines `ksud module install <ZIP>`, stores its stable daemon at `/data/adb/ksud`, and exports `KSU=true` plus `KSU_SUKISU=true` to module scripts. Its installer supplies the Magisk-compatible functions used by `customize.sh`, excludes `META-INF` during normal manager installation, and runs `service.sh` through the standard module lifecycle. The same module ZIP therefore serves both frameworks.

The Android manager must prefer `ksud` when it is available, checking the shell path, `/data/adb/ksud`, and `/data/adb/ksu/bin/ksud`, then fall back to `magisk --install-module`. Keep this selection in a unit-tested command builder and use a framework-neutral failure state when neither executable exists. The `META-INF` recovery installer remains Magisk-specific but is not used by SukiSU Ultra's normal module manager.

Before delivery, verify all of the following:

- the generated module ZIP passes a full archive test;
- `.gitattributes` keeps every Magisk shell entry on LF line endings after a Windows checkout;
- the APK contains `assets/accessibility-manager-module.zip` with the same SHA-256 as the generated ZIP;
- unit tests and Android Lint pass;
- the APK signature verifies;
- rooted Magisk and SukiSU Ultra devices each complete install, reboot, and `a11yctl` detection.

The `构建管理器` GitHub Actions workflow automates the archive, embedded-asset hash, unit test, shell regression, Lint, APK signature, and artifact checks on every main-branch push and pull request. A rooted device remains required for the final installation and behavior check.

The separate `打包模块` workflow runs for module-related changes. It uses `build.ps1` as the canonical standalone packager, rejects missing or extra ZIP entries and CRLF shell files, rebuilds twice to prove reproducibility, and uploads the versioned Magisk ZIP.
