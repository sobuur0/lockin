# Lockin

Lockin is an offline Android Device Owner app for creating irreversible app locks. A confirmed lock hides, suspends, and uninstall-blocks selected apps until the committed duration has fully elapsed.

## Product Boundaries

- Lockin is local-only. It has no accounts, sync, analytics, remote management, network access, VPN behavior, or website blocking.
- Lockin is for a disposable managed test device or a device intentionally provisioned for this purpose.
- Active locks cannot be reduced, deleted, or bypassed inside Lockin. The accepted recovery path is factory reset or device wipe.
- The app follows the phone's current light or dark theme and uses only black and white in the product UI.

## Build

From the Android project:

```bash
cd android-app
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Device Owner Setup

Use a fresh emulator or test device with no personal accounts configured.

```bash
cd android-app
./gradlew installDebug
adb shell dpm set-device-owner com.lockin/.device.LockinDeviceAdminReceiver
```

Launch Lockin after provisioning:

```bash
adb shell am start -n com.lockin/.MainActivity
```

If Device Owner setup fails because accounts, profiles, or another owner already exist, use a fresh test device or factory reset the test device before provisioning again.

## Testing Notes

Do not press Android Studio Run/rerun while waiting for a lock to expire. Android Studio force-stops the app during reinstall, and Android cancels pending alarms for force-stopped packages until the app is launched again.

For managed-device validation, use `specs/001-android-app-locks/quickstart.md`. It includes the Device Owner setup path, app lock scenarios, reboot/reinstall checks, expiration release checks, theme checks, and the accepted factory-reset recovery path.
