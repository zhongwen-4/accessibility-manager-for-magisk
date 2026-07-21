# Local Windows Android build checklist

SUMMARY: Always point the current PowerShell process at `D:\Android\Sdk` before running this project's Gradle Android tasks; the required API 36 SDK is installed there but is not configured globally.
READ WHEN: before any local Windows build, unit-test run, Android Lint run, APK signing check, or SDK repair for the Android manager.

---

The local Android SDK is installed at `D:\Android\Sdk`. It contains command-line tools under `cmdline-tools\latest`, Android platform 36, Build Tools 35.0.0 and 36.0.0, and Platform Tools. The project deliberately has no `local.properties`, and the machine has no global `ANDROID_HOME`, so set both variables for the current PowerShell process before invoking Gradle:

```powershell
$env:ANDROID_HOME = 'D:\Android\Sdk'
$env:ANDROID_SDK_ROOT = 'D:\Android\Sdk'
.\gradlew.bat --no-daemon clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Google's command-line tools installer is at `D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`. Package-manifest and Platform Tools downloads can time out transiently on this network; after licenses are accepted, rerun only the missing package names rather than replacing the SDK.

JDK 25 prints native-access warnings from Gradle and `apksigner`, but the verified Gradle 9.2.0 / Android Gradle Plugin 8.13.1 build completes successfully. Use Build Tools 36.0.0 `apksigner.bat` to verify the resulting APK's v2 signature.
