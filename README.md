# Lockin

**Status: Experimental managed-device prototype.**

Lockin is an offline Android Device Owner app for creating irreversible app locks. A confirmed lock hides, suspends, and uninstall-blocks selected apps until the committed duration has fully elapsed.

Lockin is not currently a normal consumer Android app for an already-set-up personal phone. It is built around Android Device Owner mode, which is intended for fresh, factory-reset, or deliberately managed devices.

## Demo

https://github.com/user-attachments/assets/f2331097-e393-4bcb-8cf5-1e9db2f7afcc

This demo is shown at 3x speed to keep the walkthrough short.

## Product Boundaries

- Lockin is local-only. It has no accounts, sync, analytics, remote management, network access, VPN behavior, or website blocking.
- Lockin is for a disposable managed test device or a device intentionally provisioned for this purpose.
- Lockin should not be installed on a personal everyday phone unless the user fully understands Android Device Owner provisioning and accepts factory-reset recovery.
- Active locks cannot be reduced, deleted, or bypassed inside Lockin. The accepted recovery path is factory reset or device wipe.
- The app follows the phone's current light or dark theme and uses only black and white in the product UI.

## Important Caveats

### Can I add a Google account after provisioning?

Usually yes. Once Lockin is already set as Device Owner, adding a Google account should not remove that status. For testing, a disposable emulator or spare test device is still recommended.

### Can I provision Lockin on my normal phone without removing my current Google account?

Not as true Device Owner. Android intentionally blocks Device Owner provisioning on an already-used device with existing accounts, users, profiles, or another owner. This prevents an app from silently taking over a personal phone.

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

## Managed-Device Validation

Use a disposable emulator or test device.

1. Install and provision Lockin as Device Owner.
2. Create a short lock for a non-essential app such as Chrome.
3. Confirm the locked app disappears or cannot be launched while the lock is active.
4. Extend the lock and confirm the remaining time increases.
5. Reboot the device before the lock expires and confirm the app is still locked.
6. Open Lockin and confirm the lock timer continues counting down live.
7. Wait for the timer to end and confirm the app returns automatically.
8. Switch between light and dark mode and confirm Lockin stays black and white while matching the phone's current theme.

Useful adb checks:

```bash
adb shell dpm list-owners
adb shell monkey -p com.android.chrome 1
adb reboot
adb wait-for-device
```

If an app remains hidden after expiry, open Lockin to trigger startup reconciliation. If the device needs to be recovered before a lock expires, factory reset or wipe the managed test device.
