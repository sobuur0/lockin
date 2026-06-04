# App Behavior Contracts: Lockin Irreversible App Locks

## Contract: Device Owner Gate

**Applies to**: FR-002, FR-002A, FR-020, FR-021

**Given** Lockin is launched without Device Owner status  
**Then** Lockin must show only Device Owner setup, verification, and recovery guidance.

**Allowed actions**:

- View setup guidance.
- Verify current Device Owner status.
- View recovery guidance that identifies factory reset or device wipe as the accepted recovery mechanism.

**Disallowed actions**:

- Create locks.
- Extend locks.
- Manage groups or moods.
- View statistics.
- Access active lock controls.
- Use any unlock, bypass, recovery PIN, pause, temporary override, or emergency path.

## Contract: Create Lock

**Applies to**: FR-003, FR-004, FR-005, FR-006, FR-007, FR-012

**Inputs**:

- One or more lockable package identifiers.
- A positive duration in minutes, hours, days, weeks, or custom duration.
- User confirmation of irreversible lock behavior.

**Success result**:

- Lock is persisted locally before returning success.
- Lock status becomes active.
- Selected packages become covered until the committed end time.
- Policy reconciliation is requested immediately.

**Failure result**:

- Missing Device Owner status returns the Device Owner gate.
- Empty app selection is rejected.
- Invalid or non-positive duration is rejected.
- Policy-exempt packages are rejected before confirmation.

## Contract: Extend Active Lock

**Applies to**: FR-008, FR-027

**Inputs**:

- Active lock identifier.
- Positive extension duration.
- User confirmation that the extension is irreversible.

**Success result**:

- Current remaining duration is calculated at confirmation time.
- Resulting remaining duration equals current remaining duration plus newly confirmed extension.
- Resulting end time is updated to match the resulting remaining duration.
- Extension history is recorded.
- Policy reconciliation continues without interruption.

**Failure result**:

- Completed locks cannot be extended.
- Missing Device Owner status returns the Device Owner gate.
- Non-positive extension duration is rejected.
- No action may reduce, pause, delete, bypass, or override the active lock.

## Contract: Package Enforcement

**Applies to**: FR-012, FR-013, FR-014, FR-015, FR-016, FR-017, FR-018, FR-019, QC-001, QC-003

**Triggers**:

- Lock created.
- Lock extended.
- Lock expired.
- Lockin app started.
- Device boot completed.
- Package installed, changed, replaced, or removed.
- User requests policy verification from the Device Owner guidance surface.

**For each active locked package identifier**:

- If Lockin is not Device Owner, normal operation remains refused and local lock state is not weakened.
- If the package is installed, Lockin attempts to hide it where supported.
- If the package is installed, Lockin attempts to suspend it where supported.
- If the package is installed, Lockin attempts to block uninstall while the active lock covers it.
- If the package is uninstalled, the lock remains active by package identifier and reapplies when the same package identifier appears before expiration.
- If policy state cannot be determined, enforcement fails closed and the package remains treated as blocked.

**Expiration behavior**:

- A package is released only when no active or fail-closed lock covers its package identifier.
- Overlapping locks release a package only after the latest applicable end time.

## Contract: Groups and Moods

**Applies to**: FR-009, FR-010, FR-011

**Group behavior**:

- A group has a required name and one or more package identifiers.
- A group can be used to create a new lock.
- Editing a group changes only future locks and never weakens active locks.

**Mood behavior**:

- A mood has a required name, one or more package identifiers, and optional default duration.
- A mood can be used to create a new lock.
- Editing a mood changes only future locks and never weakens active locks.

## Contract: Home and Statistics

**Applies to**: FR-023, FR-024, FR-025, QC-002, QC-004, QC-005

**Home behavior**:

- Active locks, blocked apps, and remaining time are the first priority.
- Completed or historical summaries do not displace active lock status.
- Visual design remains black and white and follows system light or dark appearance.
- No theme switcher is shown.

**Statistics behavior**:

- Statistics are local-only derived data.
- Required statistics are total lock duration, most blocked applications, total unique applications blocked, completed lock session count, longest lock duration, average lock duration, and most frequently used mood.
- Empty history shows an empty state without accounts, cloud sync, analytics, coaching, rankings, or social features.
