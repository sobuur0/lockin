# Feature Specification: Lockin Irreversible App Locks

**Feature Branch**: `001-android-app-locks`

**Created**: 2026-06-04

**Status**: Draft

**Input**: User description: "Build Lockin. Lockin is an Android Device Owner application that allows users to create irreversible locks on specific applications. The application is not parental control, screen time tracking, habit tracking, or productivity coaching. Its purpose is to make selected distraction applications impossible to access for a chosen duration. Android only, single user, offline first, no backend, no cloud synchronization, no user accounts, no analytics. Factory reset or device wipe is the accepted recovery mechanism. Users can create locks, lock groups, and reusable templates. Active locks cannot be reduced, paused, deleted, or bypassed, but may be extended. Blocked apps should be hidden where possible, suppress notifications, close if already running, survive reinstall, reboot, and app restart. User experience should be black and white, follow system light and dark theme, focus home on active locks, and collect statistics locally."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create an irreversible app lock (Priority: P1)

A single device user selects one or more installed distraction apps, chooses a duration, confirms the irreversible nature of the lock, and starts a lock that makes those apps inaccessible until the duration expires.

**Why this priority**: This is the core value of Lockin. Without a trustworthy irreversible lock, the product does not fulfill its purpose.

**Independent Test**: Can be fully tested by creating a lock for at least one installed app, attempting to access or weaken the lock during the active period, and verifying the app remains inaccessible until the full duration completes.

**Acceptance Scenarios**:

1. **Given** Lockin has the required device control status and at least one lockable app is installed, **When** the user selects apps, chooses a duration, acknowledges the lock is irreversible except by device wipe or factory reset, and confirms, **Then** the selected apps become blocked for the full chosen duration.
2. **Given** a lock is active, **When** the user tries to reduce its duration, pause it, delete it, or bypass it from within Lockin, **Then** Lockin does not provide or accept any action that weakens the active lock.
3. **Given** a lock is active, **When** the user extends it, **Then** the lock remains active until the later end time and the extension cannot be reversed.

---

### User Story 2 - Use groups and reusable lock moods (Priority: P2)

A user creates named groups of apps and reusable moods, such as "Deep Work", so they can quickly start consistent locks without rebuilding the same app selection every time.

**Why this priority**: Groups and moods reduce setup friction and make the core lock behavior practical for repeated use.

**Independent Test**: Can be fully tested by creating a group with multiple apps, saving a reusable mood with a default duration, starting a lock from it, and confirming all included apps are blocked.

**Acceptance Scenarios**:

1. **Given** the user has installed apps available, **When** they create a lock group named "Deep Work" containing Twitter, LinkedIn, Reddit, and YouTube, **Then** the group is saved and can be selected as a unit for future locks.
2. **Given** a reusable mood exists with apps and a default duration, **When** the user starts a lock from that mood, **Then** Lockin creates an active lock using the mood's apps and duration after final confirmation.
3. **Given** a lock is already active, **When** the user creates a new lock group containing additional apps, **Then** those additional apps can be blocked through a new lock without weakening any existing active lock.

---

### User Story 3 - Enforce locks across app state changes (Priority: P2)

A user trusts that active locks continue working if the device restarts, Lockin restarts, a blocked app is already running, or a blocked app is removed and later reinstalled.

**Why this priority**: The lock must remain credible under common bypass attempts and ordinary device events.

**Independent Test**: Can be fully tested by creating an active lock, restarting the device or Lockin, reinstalling a blocked app during the active period, and attempting to launch the app.

**Acceptance Scenarios**:

1. **Given** a blocked app is already open when a lock begins, **When** the lock becomes active, **Then** the blocked app is closed or removed from the foreground promptly and cannot be reopened.
2. **Given** a lock is active, **When** the device reboots or Lockin restarts, **Then** the active lock is restored automatically and all blocked apps remain inaccessible.
3. **Given** a lock is active for an app that is uninstalled, **When** that same app is reinstalled before the lock expires, **Then** the app is blocked again without requiring user action.

---

### User Story 4 - See active locks and local statistics (Priority: P3)

A user opens Lockin and immediately sees what is currently locked, when each lock ends, and local statistics about their completed lock history.

**Why this priority**: Active lock visibility builds trust, and local statistics provide useful feedback without turning the product into analytics, habit coaching, or social tracking.

**Independent Test**: Can be fully tested by creating and completing locks, opening the home and statistics views, and verifying the information is accurate and stored only on the device.

**Acceptance Scenarios**:

1. **Given** one or more locks are active, **When** the user opens Lockin, **Then** the home screen emphasizes active locks, blocked apps, and remaining time before completed or historical information.
2. **Given** locks have completed, **When** the user opens statistics, **Then** Lockin shows total lock duration, most blocked apps, total unique apps blocked, completed lock session count, and other useful local summaries.
3. **Given** the device changes between system light and dark appearance, **When** the user opens Lockin, **Then** the interface follows the system appearance using a black and white visual design and no in-app theme switcher.

### Edge Cases

- Lockin is opened without the required device control status: it must refuse normal operation and show only the prerequisite state and recovery/setup guidance.
- A user selects a zero, negative, or otherwise invalid duration: the lock cannot be created.
- A user attempts to create a lock with no apps selected: the lock cannot be created.
- A user manually changes the device clock during an active lock: the lock must not end earlier than the originally committed duration.
- Two active locks include the same app with different end times: the app remains blocked until the latest applicable end time.
- A blocked app is unavailable, disabled, or uninstalled during an active lock: the lock record remains active and reapplies if the app becomes available before the lock expires.
- The device has no completed lock history: statistics show an empty state without suggesting accounts, cloud sync, analytics, coaching, or social features.
- A lock reaches its end while the device is powered off: the lock is completed on next startup only if the committed duration has fully elapsed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Lockin MUST operate only for a single local device user on Android devices.
- **FR-002**: Lockin MUST refuse lock creation, lock management, and statistics access when the required Device Owner status is unavailable.
- **FR-002A**: Lockin MUST refuse normal operation when Device Owner status is unavailable. The application may only display Device Owner setup, verification, and recovery guidance until Device Owner status is restored.
- **FR-003**: Lockin MUST make clear before confirmation that active locks cannot be reduced, paused, deleted, or bypassed from within the product, and that factory reset or device wipe is the accepted recovery mechanism.
- **FR-004**: Users MUST be able to select installed applications and create a lock for a chosen duration.
- **FR-005**: Users MUST be able to choose lock durations in minutes, hours, days, weeks, or a custom duration.
- **FR-006**: Lockin MUST reject lock creation when no applications are selected or the selected duration is not valid.
- **FR-007**: Once a lock becomes active, Lockin MUST prevent any in-product action that reduces its duration, pauses it, deletes it, or bypasses it.
- **FR-008**: Users MUST be able to extend an active lock, and any extension MUST be irreversible once confirmed.
- **FR-009**: Users MUST be able to create, name, edit, and reuse lock groups containing multiple applications, except that editing a group MUST NOT weaken locks that are already active.
- **FR-010**: Users MUST be able to create, name, edit, and reuse moods that include an app selection and optional default duration.
- **FR-011**: Lockin MUST allow additional applications to become blocked through new locks or lock groups while preserving all existing active lock commitments.
- **FR-012**: For every active lock, each covered application MUST remain inaccessible until the latest applicable lock end time.
- **FR-013**: Lockin MUST hide blocked applications from normal app launch surfaces where device capabilities allow.
- **FR-014**: If a blocked application remains visible through any surface, Lockin MUST still prevent the user from successfully opening or using it during the active lock.
- **FR-015**: Lockin MUST prevent notifications from blocked applications from appearing during the active lock where device capabilities allow.
- **FR-016**: If a blocked application is running when a lock begins or becomes applicable, Lockin MUST remove it from the foreground promptly.
- **FR-017**: Active locks MUST survive device reboot and Lockin restart without user action.
- **FR-018**: Lock enforcement is tied to the application's package identifier rather than a specific installation instance. Reinstalling a blocked application MUST NOT remove or weaken an active lock. If an application with the same package identifier is installed again before lock expiration, the lock MUST automatically reapply.
- **FR-019**: Manual changes to the device clock MUST NOT cause an active lock to end earlier than the duration originally confirmed by the user.
- **FR-020**: Lockin MUST function without backend services, cloud synchronization, user accounts, remote management, social features, or network dependency.
- **FR-021**: Lockin MUST NOT collect analytics or transmit usage, lock, application, or statistics data off the device.
- **FR-022**: Lockin MUST store lock state, groups, moods, and statistics locally so they survive Lockin restart and device reboot.
- **FR-023**: The home screen MUST prioritize active locks, including blocked apps and remaining time, over historical summaries.
- **FR-024**: Lockin MUST collect local statistics including total lock duration, most blocked applications, total unique applications blocked, completed lock session count, longest lock duration, average lock duration, and most frequently used mood.
- **FR-025**: Lockin MUST present a black and white visual design that follows the system light or dark appearance and MUST NOT provide an in-app theme switcher.
- **FR-026**: Lockin MUST explicitly exclude parental controls, screen time tracking, habit coaching, productivity coaching, remote management, website blocking, VPN filtering, multi-user support, and social features from v1 behavior.
- **FR-027**: When an active lock is extended, the extension duration MUST be added to the remaining lock duration. For example, if a lock has 1 hour remaining and the user extends it by 5 hours, the resulting lock duration becomes 6 hours remaining from the moment of extension. The resulting lock end time MUST equal the current remaining duration plus the newly confirmed extension.

### Key Entities *(include if feature involves data)*

- **Application**: An installed app that can be selected for locking; identified by stable app identity, display name, availability state, and whether it is currently covered by an active lock.
- **Lock**: An irreversible commitment for one or more applications over a confirmed duration; includes selected applications, start time, committed end time, status, source, and extension history.
- **Lock Group**: A named reusable collection of applications that can be selected together when creating a lock.
- **Mood**: A reusable lock template containing an app selection and optional default duration for quick lock creation.
- **Lock Session**: A completed or active occurrence of a lock used for history and local statistics.
- **Statistic Summary**: Locally derived counts and durations from lock sessions, such as total duration, most blocked apps, unique blocked app count, completed session count, longest lock, average lock duration, and most frequently used mood.

### Quality & Compliance Requirements *(include when applicable)*

- **QC-001**: Lock state, lock end commitments, group definitions, moods, and statistics MUST remain accurate after Lockin restart and device reboot.
- **QC-002**: Lockin MUST preserve user privacy by keeping all lock and statistics data on the device and avoiding analytics or account-linked collection.
- **QC-003**: Lock enforcement MUST fail closed for active locks: when Lockin cannot determine whether an app is still locked, the app must remain blocked until the lock state is resolved.
- **QC-004**: User-facing copy for irreversible actions MUST be direct and non-coaching; it must explain the commitment without framing the product as productivity, habit tracking, parental control, or screen time management.
- **QC-005**: The product MUST maintain a minimal black and white interface with readable contrast in both system light and dark appearance.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time eligible user can create an irreversible lock for at least one installed application in under 2 minutes.
- **SC-002**: 100% of active lock attempts to reduce, pause, delete, or bypass a lock through Lockin are rejected.
- **SC-003**: In validation testing, blocked applications cannot be successfully opened during the active lock period across direct launch attempts, app restart, device reboot, and reinstall scenarios.
- **SC-004**: A blocked application already in the foreground is removed from active use within 5 seconds of the lock becoming active.
- **SC-005**: 100% of active locks remain active for at least the full confirmed duration, including after manual device clock changes.
- **SC-006**: 100% of active lock, group, mood, and statistics data needed for operation remains available after Lockin restart and device reboot.
- **SC-007**: Users can start a lock from a saved group or mood in under 30 seconds after the group or mood exists.
- **SC-008**: Statistics shown for completed sessions match the underlying local lock history for total lock duration, most blocked applications, total unique applications blocked, completed lock session count, longest lock duration, average lock duration, and most frequently used mood.
- **SC-009**: No lock, app, or statistics data is sent to a backend, cloud service, analytics service, account system, or remote management system.
- **SC-010**: In user acceptance testing, at least 90% of participants understand before confirming that an active lock cannot be undone except by factory reset or device wipe.

## Traceability *(mandatory)*

- **TR-001**: Create an irreversible app lock -> FR-001, FR-002, FR-002A, FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, FR-012, FR-019, FR-027 -> SC-001, SC-002, SC-005, SC-010
- **TR-002**: Use groups and reusable lock moods -> FR-009, FR-010, FR-011, FR-022 -> SC-007
- **TR-003**: Enforce locks across app state changes -> FR-012, FR-013, FR-014, FR-015, FR-016, FR-017, FR-018, FR-019, QC-001, QC-003 -> SC-003, SC-004, SC-005, SC-006
- **TR-004**: See active locks and local statistics -> FR-020, FR-021, FR-022, FR-023, FR-024, FR-025, QC-002, QC-004, QC-005 -> SC-006, SC-008, SC-009
- **TR-005**: Edge cases for invalid setup, invalid lock input, overlapping locks, unavailable apps, and no history -> FR-002, FR-002A, FR-006, FR-012, FR-018, FR-019, FR-024 -> SC-002, SC-003, SC-005, SC-008

## Assumptions

- The intended user is the device owner using a single personal Android device.
- Device Owner status is provisioned outside normal Lockin operation; Lockin may explain the prerequisite but must not pretend to enforce irreversible locks without it.
- Factory reset or device wipe is an acceptable escape hatch and is not considered an in-product bypass.
- The app list available for locking is limited to installed applications that can be identified reliably by the device.
- Website blocking, VPN filtering, multi-user support, remote management, parent-child controls, social features, backend services, cloud synchronization, user accounts, analytics, habit tracking, screen time tracking, and productivity coaching are outside v1 scope.
- Local statistics are intended to summarize lock usage on the device, not to score, coach, rank, or compare the user.
