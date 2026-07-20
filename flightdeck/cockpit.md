# Cockpit — Accessibility-Manager

Focus: Validate the Miuix dashboard, operation log, automatic module installation, and locked-service recovery on rooted Magisk and SukiSU Ultra devices.

## In flight

- `automatic-module-install` — implementation complete; rooted-device validation remains.
- `miuix-ui-migration` — implementation complete; rooted-device UI validation remains.

## Next

- Run the `构建管理器` and `打包模块` workflows once, then install the APK artifact on rooted Magisk and SukiSU Ultra devices and validate automatic installation plus locked-service restoration on both.

## Open questions

- A SukiSU Ultra device is connected, but Root authorization for the freshly reinstalled manager is currently denied; no Magisk device is connected.
