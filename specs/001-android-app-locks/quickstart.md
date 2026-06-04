# Quickstart Validation Guide: Lockin Irreversible App Locks

## Prerequisites

- Android Studio or command-line Android SDK installed.
- A fresh emulator or physical test device that can be provisioned as Device Owner.
- `adb` available on PATH.
- No personal accounts configured on the managed test device before Device Owner provisioning.

## Build and Test Commands

Run from repository root after implementation tasks create the Android project:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Expected outcome:

- Build succeeds.
- Unit tests pass for domain rules, duration math, statistics, and repository behavior.
- Instrumented tests pass on the managed test device for app startup, persistence, and policy orchestration checks.

## Device Owner Provisioning

Install the debug build on a fresh test device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-device-owner com.lockin/.device.LockinDeviceAdminReceiver
```

Expected outcome:

- Device Owner provisioning succeeds.
- Launching Lockin shows the normal active-lock home experience.

If provisioning is unavailable, launch Lockin and verify:

- Lock creation is not reachable.
- Group, mood, and statistics views are not reachable.
- Only Device Owner setup, verification, and recovery guidance is shown.

## Scenario 1: Create an Irreversible Lock

1. Open Lockin on a Device Owner test device.
2. Select one installed non-exempt app.
3. Choose a duration.
4. Confirm the irreversible lock message.
5. Attempt to open the blocked app.
6. Attempt to reduce, pause, delete, or bypass the lock from Lockin.

Expected outcome:

- Lock is active.
- Blocked app cannot be used.
- Lock weakening actions are not available or are rejected.
- Lock state survives app restart.

## Scenario 2: Extend an Active Lock

1. Create a lock with a short duration.
2. Wait until the lock has a known remaining duration.
3. Extend it by a positive duration.

Expected outcome:

- Resulting remaining duration equals current remaining duration plus extension duration.
- Extension cannot be reversed.
- The locked package remains blocked without interruption.

## Scenario 3: Group and Mood Reuse

1. Create a group named `Deep Work` with multiple installed apps.
2. Create a mood from the same app set with a default duration.
3. Start a lock from the group.
4. Start a separate lock from the mood after the first completes or with a different package set.

Expected outcome:

- Group and mood are reusable.
- Locks created from group or mood block all selected package identifiers.
- Editing a group or mood after lock creation does not weaken active locks.

## Scenario 4: Reboot, Restart, and Reinstall Enforcement

1. Create an active lock for a test app.
2. Restart Lockin.
3. Reboot the device.
4. Attempt to uninstall the locked app.
5. If uninstall is possible on the test package, reinstall the same package identifier before lock expiration.

Expected outcome:

- Active lock is restored after Lockin restart.
- Active lock is restored after device reboot.
- Uninstall is blocked where policy allows.
- If the same package identifier appears again before expiration, enforcement reapplies automatically.
- If policy state is uncertain, Lockin fails closed and continues treating the package as blocked.

## Scenario 5: Local Statistics

1. Complete several locks with different apps and moods.
2. Open statistics.

Expected outcome:

- Statistics show total lock duration, most blocked applications, total unique applications blocked, completed lock session count, longest lock duration, average lock duration, and most frequently used mood.
- Values match local lock session history.
- No account, cloud sync, analytics, social, or coaching surface appears.

## Scenario 6: Clock Change Resistance

1. Create an active lock.
2. Attempt to change date, time, or time zone from system settings.
3. Attempt to use a forward clock change to complete a lock early.

Expected outcome:

- Date/time configuration is restricted where Device Owner policy supports it.
- Manual clock changes do not shorten the active lock.
- Lock remains active for at least the full confirmed duration.
