# Phase 0 Research: Lockin Irreversible App Locks

## Decision: Require Device Owner for normal operation

**Decision**: Lockin will treat Device Owner status as the operating boundary. If Device Owner status is missing, the app will show only setup, verification, and recovery guidance and will not expose lock creation, active lock management, groups, moods, or statistics.

**Rationale**: The specification requires irreversible app locks and refusal of normal operation when Device Owner status is unavailable. Android package policy operations used for hiding, suspending, and uninstall blocking are restricted to device/profile owners or delegated policy holders. Lockin is explicitly not a general screen-time or coaching app, so degraded non-Device Owner behavior would weaken the product promise.

**Alternatives considered**:

- Accessibility-service blocking: rejected because it is easier to disable, does not satisfy Device Owner mandatory operation, and would create an override surface.
- Usage access monitoring: rejected because it is detection after launch rather than policy enforcement.
- Normal launcher hiding only: rejected because it does not block all launch surfaces or notifications.

**Sources**:

- Android `DevicePolicyManager` reference: https://developer.android.com/reference/android/app/admin/DevicePolicyManager

## Decision: Enforce locks with package policy, keyed by package identifier

**Decision**: Store each blocked app by package identifier. While a lock is active, reconcile each package identifier to the current installed package and apply policy in this order:

1. Refuse operation if Lockin is not Device Owner.
2. For installed blocked packages, hide the app where supported.
3. Suspend the package where supported to prevent activity launch, hide notifications, and remove it from recents.
4. Block uninstall while the package is actively locked.
5. On boot, app startup, package install/change, and lock extension or expiration, reconcile persisted active locks to current installed packages.

**Rationale**: The spec requires hidden apps where possible, notification suppression, immediate closure of already running apps, reinstall resistance, and survival across reboot and app restart. Android documentation states suspended packages cannot start activities, have hidden notifications, and do not appear in recents. It also states suspended state no longer applies after uninstall unless uninstall is blocked, so Lockin must persist package identifiers and reapply policy if the same package identifier appears again before expiration.

**Alternatives considered**:

- Only `setApplicationHidden`: rejected because hiding alone is not enough to cover notification suppression and all launch attempts.
- Only `setPackagesSuspended`: rejected because Android notes suspension no longer applies after uninstall, requiring package-id persistence and uninstall blocking.
- VPN or website filtering: rejected because explicitly out of scope.

**Sources**:

- Android `DevicePolicyManager.setPackagesSuspended`: https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setPackagesSuspended(android.content.ComponentName,%20java.lang.String[],%20boolean)
- Android `DevicePolicyManager.setApplicationHidden`: https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setApplicationHidden(android.content.ComponentName,%20java.lang.String,%20boolean)
- Android `DevicePolicyManager.setUninstallBlocked`: https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setUninstallBlocked(android.content.ComponentName,%20java.lang.String,%20boolean)

## Decision: Use Room as the canonical local data source

**Decision**: Store lock commitments, groups, moods, app identities, lock sessions, extension events, and statistics inputs in Room. Repositories expose observable flows from Room to the UI and policy enforcer.

**Rationale**: Lockin is offline-only and needs relational data: many apps per lock, many apps per group or mood, extension history, overlapping active locks by package identifier, and aggregate statistics. Android DataStore guidance recommends Room instead of DataStore for large or complex datasets, partial updates, and referential integrity. Android offline-first guidance recommends local data sources as the source of truth.

**Alternatives considered**:

- DataStore only: rejected because lock history, package membership, and statistics need relational queries and partial updates.
- JSON files: rejected because they make transactional updates and aggregate queries more fragile.
- Backend database: rejected because no backend or cloud synchronization is in scope.

**Sources**:

- Android DataStore guidance: https://developer.android.com/topic/libraries/architecture/datastore
- Android offline-first data layer guidance: https://developer.android.com/topic/architecture/data-layer/offline-first

## Decision: Preserve lock duration with monotonic time and date/time restrictions

**Decision**: Persist both display wall-clock timestamps and monotonic elapsed-time anchors for active locks. While the device is running, remaining time and completion checks use monotonic elapsed time. During active locks, Device Owner policy will prevent the user from configuring device date, time, and time zone where supported. On boot, Lockin reconciles persisted active locks before normal UI access.

**Rationale**: The spec requires manual clock changes not to shorten locks and requires locks to survive reboot. Monotonic elapsed time prevents in-session clock changes from shortening an active lock. Device Owner date/time restrictions reduce the main offline bypass vector across reboot or power-off without adding network dependency, accounts, cloud verification, PINs, emergency unlocks, or override mechanisms.

**Alternatives considered**:

- Trust wall-clock end time only: rejected because manually moving the clock forward could end locks early.
- Require network time: rejected because Lockin has no network dependency and is offline-first.
- Recovery PIN or emergency unlock: rejected because explicitly out of scope.

**Sources**:

- Android `UserManager.DISALLOW_CONFIG_DATE_TIME`: https://developer.android.com/reference/kotlin/android/os/UserManager#DISALLOW_CONFIG_DATE_TIME
- Android Device Owner time policy guidance: https://developer.android.com/reference/android/app/admin/DevicePolicyManager#setAutoTimePolicy(int)

## Decision: Validate Device Owner behavior on managed devices

**Decision**: Use unit and local integration tests for domain behavior, persistence, and UI state, but require instrumented validation on a fresh managed emulator or test device provisioned as Device Owner for enforcement behavior.

**Rationale**: Device Owner policy APIs depend on Android system state and security restrictions. Test doubles can verify call intent, but they cannot prove app hiding, suspension, notification suppression, uninstall blocking, boot reconciliation, or package reinstall behavior.

**Alternatives considered**:

- Unit tests only: rejected because they cannot validate system policy effects.
- Manual-only validation: rejected because most domain and persistence behavior is repeatable and should be automated.
