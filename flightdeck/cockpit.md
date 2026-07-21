# Cockpit — Accessibility-Manager

Focus: Validate the compiled continuous bottom-bar frost adjustment, Miuix dashboard, operation log, automatic module installation, and locked-service recovery on rooted Magisk and SukiSU Ultra devices.

## In flight

- `automatic-module-install` — implementation complete; rooted-device validation remains.
- `miuix-ui-migration` — continuous bottom-bar frost adjustment built with tests and Lint passing; rooted-device UI validation remains.

## Next

- Install `app/build/outputs/apk/debug/app-debug.apk`, confirm the bottom-bar frost slider is continuous down to 0%, run `打包模块`, then validate automatic installation plus locked-service restoration on rooted Magisk and SukiSU Ultra devices.

## Open questions

- A SukiSU Ultra device is connected, but Root authorization for the freshly reinstalled manager is currently denied; no Magisk device is connected.
- The K70-labelled ADB target runs Android 12 / API 32 with MuMu components and exposes only two registered accessibility services; connect the physical K70 host system before treating its service count as genuine-device validation.
