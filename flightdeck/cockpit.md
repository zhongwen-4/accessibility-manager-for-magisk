# Cockpit — Accessibility-Manager

Focus: Verify the continuous bottom-bar frost adjustment, then validate the Miuix dashboard, operation log, automatic module installation, and locked-service recovery on rooted Magisk and SukiSU Ultra devices.

## In flight

- `automatic-module-install` — implementation complete; rooted-device validation remains.
- `miuix-ui-migration` — continuous bottom-bar frost adjustment implemented; CI/build and rooted-device UI validation remain.

## Next

- Run `构建管理器` and confirm the bottom-bar frost slider is continuous down to 0%, run `打包模块`, then install the APK artifact on rooted Magisk and SukiSU Ultra devices and validate automatic installation plus locked-service restoration on both.

## Open questions

- A SukiSU Ultra device is connected, but Root authorization for the freshly reinstalled manager is currently denied; no Magisk device is connected.
- This Windows environment has no Android SDK, so the latest manager change needs CI or another configured Android build environment for Gradle verification.
