# Data Model: Lockin Irreversible App Locks

## Entity: Application

Represents an app identity that can be locked.

**Fields**:

- `packageId`: Stable package identifier. Primary identity for enforcement and reinstall behavior.
- `displayName`: User-facing app name from the current installed package.
- `iconRef`: Local icon reference or cache key for UI display.
- `isInstalled`: Whether an app with this package identifier is currently installed.
- `isLockinApp`: Whether the package is Lockin itself.
- `isPolicyExempt`: Whether Android policy reports the package cannot be hidden, suspended, or uninstall-blocked.
- `lastSeenAt`: Last time the package was observed by app catalog reconciliation.

**Validation rules**:

- `packageId` is required and unique.
- Lockin itself, the active launcher, required package services, and other policy-exempt packages cannot be selected for blocking.
- Reinstall behavior is keyed by `packageId`, not installation instance.

## Entity: Lock

Represents an irreversible active or completed lock commitment.

**Fields**:

- `id`: Unique local identifier.
- `status`: `scheduled`, `active`, `completed`, or `failedClosed`.
- `createdAtWallTime`: Wall-clock time shown for user history.
- `startedAtWallTime`: Display start time.
- `startedAtElapsedRealtime`: Monotonic start anchor while the current boot is running.
- `committedEndWallTime`: Display end time after all extensions.
- `remainingDurationAtLastCheckpoint`: Last persisted remaining duration.
- `lastCheckpointElapsedRealtime`: Monotonic checkpoint anchor.
- `sourceType`: `manual`, `group`, or `mood`.
- `sourceId`: Optional local group or mood identifier.
- `confirmationTextVersion`: Version of irreversible confirmation copy accepted by the user.

**Relationships**:

- Has many `LockApplication` rows.
- Has many `LockExtension` rows.
- Produces one `LockSession` when completed.

**Validation rules**:

- A lock must include at least one lockable application.
- Duration must be greater than zero.
- Active locks cannot move to deleted, paused, bypassed, or reduced states.
- Completion is allowed only after the committed remaining duration has fully elapsed.
- If lock state cannot be reconciled, status becomes or remains `failedClosed` and enforcement continues.

**State transitions**:

```text
scheduled -> active -> completed
active -> failedClosed -> active
failedClosed -> completed
```

No transition exists to pause, delete, bypass, shorten, or emergency unlock.

## Entity: LockApplication

Join entity between `Lock` and `Application`.

**Fields**:

- `lockId`: Parent lock identifier.
- `packageId`: Application package identifier.
- `addedAt`: When the package became covered by the lock.

**Validation rules**:

- `(lockId, packageId)` is unique.
- Removing a row from an active lock is prohibited.
- If multiple active locks cover the same package, enforcement continues until the latest applicable end time.

## Entity: LockExtension

Records an irreversible extension to an active lock.

**Fields**:

- `id`: Unique local identifier.
- `lockId`: Parent lock identifier.
- `confirmedAtWallTime`: Display time of extension confirmation.
- `confirmedAtElapsedRealtime`: Monotonic confirmation anchor.
- `previousRemainingDuration`: Remaining duration at the moment of confirmation.
- `extensionDuration`: Newly confirmed extension duration.
- `resultingRemainingDuration`: `previousRemainingDuration + extensionDuration`.
- `resultingEndWallTime`: Display end time after extension.

**Validation rules**:

- Extension duration must be greater than zero.
- `resultingRemainingDuration` must equal current remaining duration plus newly confirmed extension.
- Extensions cannot be reversed or reduced.

## Entity: LockGroup

Reusable named collection of applications.

**Fields**:

- `id`: Unique local identifier.
- `name`: Required user-facing name.
- `createdAt`: Creation time.
- `updatedAt`: Last edit time.
- `isArchived`: Whether hidden from normal selection.

**Relationships**:

- Has many `LockGroupApplication` rows.
- Can be used as a source for new locks.

**Validation rules**:

- Name is required.
- Group must contain at least one lockable application to start a lock.
- Editing a group does not mutate existing active locks.

## Entity: Mood

Reusable lock template containing apps and an optional default duration.

**Fields**:

- `id`: Unique local identifier.
- `name`: Required user-facing name.
- `defaultDuration`: Optional positive duration.
- `createdAt`: Creation time.
- `updatedAt`: Last edit time.
- `isArchived`: Whether hidden from normal selection.

**Relationships**:

- Has many `MoodApplication` rows.
- Can be used as a source for new locks.

**Validation rules**:

- Name is required.
- Mood must contain at least one lockable application to start a lock.
- If provided, default duration must be greater than zero.
- Editing a mood does not mutate existing active locks.

## Entity: LockSession

Historical session generated when a lock completes.

**Fields**:

- `id`: Unique local identifier.
- `lockId`: Source lock.
- `startedAtWallTime`: Display start time.
- `completedAtWallTime`: Display completion time.
- `totalCommittedDuration`: Final committed duration including extensions.
- `sourceType`: `manual`, `group`, or `mood`.
- `sourceId`: Optional source group or mood identifier.

**Relationships**:

- Has many `LockSessionApplication` rows copied from the completed lock.

**Validation rules**:

- Created only once per completed lock.
- Total committed duration must be greater than zero.

## Entity: Statistic Summary

Derived read model from lock sessions and session applications.

**Fields**:

- `totalLockDuration`
- `mostBlockedApplications`
- `totalUniqueApplicationsBlocked`
- `completedLockSessionCount`
- `longestLockDuration`
- `averageLockDuration`
- `mostFrequentlyUsedMood`

**Validation rules**:

- Statistics are computed only from local lock session history.
- Empty history returns zero counts and empty rankings.
- Statistics must not transmit data off device.

## Entity: PolicyReconciliationEvent

Local audit record for policy application attempts and fail-closed recovery.

**Fields**:

- `id`: Unique local identifier.
- `occurredAt`: Local event time.
- `trigger`: `appStart`, `boot`, `packageChanged`, `lockCreated`, `lockExtended`, `lockExpired`, or `manualVerify`.
- `packageId`: Optional package identifier.
- `result`: `applied`, `alreadyApplied`, `policyExempt`, `deviceOwnerMissing`, or `failedClosed`.
- `message`: Local diagnostic text.

**Validation rules**:

- Events are local only and must not be analytics.
- Device Owner missing events do not weaken persisted active locks.
