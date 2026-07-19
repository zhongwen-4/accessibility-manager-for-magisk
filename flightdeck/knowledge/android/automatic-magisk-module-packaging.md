# Android release module-sync checklist

SUMMARY: Always generate the APK's bundled Magisk module from the canonical root module files and derive its version code from module.prop.
READ WHEN: before any Android manager release or any change to module packaging and automatic installation.

---

The Android manager carries `accessibility-manager-module.zip` as a generated asset. Do not maintain a second hand-copied module tree inside `app/`: `app/build.gradle` owns the explicit canonical file list and creates the ZIP during `preBuild`.

`BuildConfig.BUNDLED_MODULE_VERSION_CODE` must come from the root `module.prop`. Runtime installation compares it with both `/data/adb/modules/accessibility_manager/module.prop` and `/data/adb/modules_update/accessibility_manager/module.prop`; checking the pending directory prevents repeated installs before reboot.

Respect `/data/adb/modules/accessibility_manager/disable` even when the installed version is old. Automatic installation must not silently reverse a user's explicit disable action.

Before delivery, verify all of the following:

- the generated module ZIP passes a full archive test;
- `.gitattributes` keeps every Magisk shell entry on LF line endings after a Windows checkout;
- the APK contains `assets/accessibility-manager-module.zip` with the same SHA-256 as the generated ZIP;
- unit tests and Android Lint pass;
- the APK signature verifies;
- a rooted Magisk device completes install, reboot, and `a11yctl` detection.

The `构建管理器` GitHub Actions workflow automates the archive, embedded-asset hash, unit test, shell regression, Lint, APK signature, and artifact checks on every main-branch push and pull request. A rooted device remains required for the final installation and behavior check.

The separate `打包模块` workflow runs for module-related changes. It uses `build.ps1` as the canonical standalone packager, rejects missing or extra ZIP entries and CRLF shell files, rebuilds twice to prove reproducibility, and uploads the versioned Magisk ZIP.
