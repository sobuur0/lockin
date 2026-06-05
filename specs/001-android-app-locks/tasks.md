# Tasks: Lockin Irreversible App Locks

**Input**: Design documents from `specs/001-android-app-locks/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/app-behavior.md](./contracts/app-behavior.md), [quickstart.md](./quickstart.md)

**Verification**: Automated tests are included for repeatable domain, repository, UI state, and contract behavior. Managed-device validation tasks are included where Device Owner policy effects cannot be fully proven with local test doubles.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the Android project, build configuration, and base app structure described in the implementation plan.

- [X] T001 Create Gradle Android project settings in `settings.gradle.kts`
- [X] T002 Create root Gradle build configuration in `build.gradle.kts`
- [X] T003 [P] Create dependency version catalog for Kotlin, Android Gradle Plugin, Compose, Room, Lifecycle, Navigation, AndroidX Test, JUnit, and Robolectric in `gradle/libs.versions.toml`
- [X] T004 Create Android app module build configuration with Compose, Room, Kotlin, unit test, and instrumentation test settings in `app/build.gradle.kts`
- [X] T005 Create application manifest with `MainActivity`, `LockinDeviceAdminReceiver`, `BootReceiver`, and `PackageChangeReceiver` declarations in `app/src/main/AndroidManifest.xml`
- [X] T006 [P] Create Device Admin receiver policy XML in `app/src/main/res/xml/device_admin_receiver.xml`
- [X] T007 [P] Create base application class in `app/src/main/kotlin/com/lockin/LockinApp.kt`
- [X] T008 [P] Create base Compose activity entry point in `app/src/main/kotlin/com/lockin/MainActivity.kt`
- [X] T009 [P] Create black-and-white system-theme Compose theme in `app/src/main/kotlin/com/lockin/ui/app/LockinTheme.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the local data, time, device-policy, and navigation foundations that every user story depends on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T010 [P] Create local application entity model in `app/src/main/kotlin/com/lockin/data/entities/ApplicationEntity.kt`
- [X] T011 [P] Create lock and lock application entity models in `app/src/main/kotlin/com/lockin/data/entities/LockEntities.kt`
- [X] T012 [P] Create group and mood entity models in `app/src/main/kotlin/com/lockin/data/entities/TemplateEntities.kt`
- [X] T013 [P] Create lock session and policy reconciliation entity models in `app/src/main/kotlin/com/lockin/data/entities/HistoryEntities.kt`
- [X] T014 Create Room DAO for applications in `app/src/main/kotlin/com/lockin/data/dao/ApplicationDao.kt`
- [X] T015 Create Room DAO for locks, lock applications, and lock extensions in `app/src/main/kotlin/com/lockin/data/dao/LockDao.kt`
- [X] T016 [P] Create Room DAO for groups and moods in `app/src/main/kotlin/com/lockin/data/dao/TemplateDao.kt`
- [X] T017 [P] Create Room DAO for lock sessions, statistics queries, and policy reconciliation events in `app/src/main/kotlin/com/lockin/data/dao/HistoryDao.kt`
- [X] T018 Create Room database wiring all entities and DAOs in `app/src/main/kotlin/com/lockin/data/db/LockinDatabase.kt`
- [X] T019 [P] Create monotonic and wall-clock time provider abstraction in `app/src/main/kotlin/com/lockin/domain/lock/TimeProvider.kt`
- [X] T020 [P] Create duration value objects and validation helpers in `app/src/main/kotlin/com/lockin/domain/lock/LockDuration.kt`
- [X] T021 [P] Create Device Owner state abstraction in `app/src/main/kotlin/com/lockin/device/DeviceOwnerState.kt`
- [X] T022 [P] Create DevicePolicyManager gateway interface and result types in `app/src/main/kotlin/com/lockin/device/DevicePolicyGateway.kt`
- [X] T023 Create Android DevicePolicyManager gateway implementation in `app/src/main/kotlin/com/lockin/device/AndroidDevicePolicyGateway.kt`
- [X] T095 Create Device Admin receiver implementation in `app/src/main/kotlin/com/lockin/device/LockinDeviceAdminReceiver.kt`
- [X] T024 [P] Create application catalog scanner interface and package identity model in `app/src/main/kotlin/com/lockin/domain/appcatalog/AppCatalogScanner.kt`
- [X] T025 Create Android package catalog scanner implementation in `app/src/main/kotlin/com/lockin/domain/appcatalog/AndroidAppCatalogScanner.kt`
- [X] T026 Create repository interfaces for locks, templates, apps, policy events, and statistics in `app/src/main/kotlin/com/lockin/domain/repository/Repositories.kt`
- [X] T027 Create Room-backed repositories for locks, templates, apps, policy events, and statistics in `app/src/main/kotlin/com/lockin/data/repository/RoomRepositories.kt`
- [X] T028 Create dependency container for database, repositories, policy gateway, app scanner, and time provider in `app/src/main/kotlin/com/lockin/app/LockinContainer.kt`
- [X] T029 Create root navigation graph for Device Owner gate, home, create lock, lock detail, groups, moods, and statistics in `app/src/main/kotlin/com/lockin/ui/app/LockinNavGraph.kt`
- [X] T030 [P] Create base UI components for app rows, duration input, confirmation copy, active lock cards, and empty states in `app/src/main/kotlin/com/lockin/ui/app/Components.kt`

**Checkpoint**: Foundation ready. User story implementation can now begin in priority order or in parallel by story.

---

## Phase 3: User Story 1 - Create an irreversible app lock (Priority: P1) - MVP

**Goal**: A Device Owner user can select installed apps, choose a valid duration, confirm the irreversible commitment, start a lock, extend it, and be prevented from weakening it.

**Independent Test**: Create a lock for one installed app, attempt to access or weaken it during the active period, extend it, and verify it remains active for the full resulting duration.

### Verification for User Story 1

- [ ] T031 [P] [US1] Add unit tests for duration validation and extension math in `app/src/test/kotlin/com/lockin/domain/lock/LockDurationTest.kt`
- [ ] T032 [P] [US1] Add unit tests for irreversible lock state transitions and rejected weaken actions in `app/src/test/kotlin/com/lockin/domain/lock/LockStateMachineTest.kt`
- [ ] T033 [P] [US1] Add Room integration tests for lock persistence, lock applications, extensions, and overlapping latest-end queries in `app/src/test/kotlin/com/lockin/data/LockRepositoryTest.kt`
- [ ] T034 [P] [US1] Add contract tests for Device Owner gate and create-lock validation in `app/src/test/kotlin/com/lockin/contracts/CreateLockContractTest.kt`
- [ ] T035 [P] [US1] Add ViewModel tests for create-lock confirmation, invalid input rejection, and extension confirmation in `app/src/test/kotlin/com/lockin/ui/lockcreate/CreateLockViewModelTest.kt`
- [ ] T036 [US1] Add managed-device validation steps for create lock, blocked launch attempt, and extension behavior in `specs/001-android-app-locks/quickstart.md`

### Implementation for User Story 1

- [ ] T037 [P] [US1] Implement lock state machine with active, completed, and failed-closed states in `app/src/main/kotlin/com/lockin/domain/lock/LockStateMachine.kt`
- [ ] T038 [P] [US1] Implement lock duration calculation, remaining duration checkpointing, and extension calculation in `app/src/main/kotlin/com/lockin/domain/lock/LockTiming.kt`
- [ ] T039 [P] [US1] Implement app selection eligibility rules for Lockin, launcher, and policy-exempt packages in `app/src/main/kotlin/com/lockin/domain/appcatalog/AppEligibility.kt`
- [ ] T040 [US1] Implement lock creation and extension use cases in `app/src/main/kotlin/com/lockin/domain/lock/LockUseCases.kt`
- [ ] T041 [US1] Implement initial lock policy enforcer for active locked package identifiers in `app/src/main/kotlin/com/lockin/device/LockPolicyEnforcer.kt`
- [ ] T042 [US1] Implement Device Owner gate ViewModel in `app/src/main/kotlin/com/lockin/ui/deviceowner/DeviceOwnerGateViewModel.kt`
- [ ] T043 [US1] Implement Device Owner setup, verification, and recovery guidance screen in `app/src/main/kotlin/com/lockin/ui/deviceowner/DeviceOwnerGateScreen.kt`
- [ ] T044 [US1] Implement create-lock ViewModel for installed app selection, duration selection, irreversible confirmation, and lock creation in `app/src/main/kotlin/com/lockin/ui/lockcreate/CreateLockViewModel.kt`
- [ ] T045 [US1] Implement create-lock Compose screen with app picker, minutes/hours/days/weeks/custom duration controls, and irreversible confirmation in `app/src/main/kotlin/com/lockin/ui/lockcreate/CreateLockScreen.kt`
- [ ] T046 [US1] Implement lock detail ViewModel with remaining time, rejected weaken actions, and extension flow in `app/src/main/kotlin/com/lockin/ui/lockdetail/LockDetailViewModel.kt`
- [ ] T047 [US1] Implement lock detail Compose screen with active status, blocked apps, remaining time, and extension controls in `app/src/main/kotlin/com/lockin/ui/lockdetail/LockDetailScreen.kt`
- [ ] T048 [US1] Wire create-lock, lock-detail, Device Owner gate, and active-lock routes into navigation in `app/src/main/kotlin/com/lockin/ui/app/LockinNavGraph.kt`

**Checkpoint**: User Story 1 is fully functional and testable as the MVP.

---

## Phase 4: User Story 2 - Use groups and reusable lock moods (Priority: P2)

**Goal**: A user can create named app groups and reusable moods, then start locks from them without weakening active locks.

**Independent Test**: Create a `Deep Work` group, create a mood with a default duration, start a lock from each, and verify all included apps are blocked while edits only affect future locks.

### Verification for User Story 2

- [ ] T049 [P] [US2] Add Room integration tests for group and mood persistence and app membership uniqueness in `app/src/test/kotlin/com/lockin/data/TemplateRepositoryTest.kt`
- [ ] T050 [P] [US2] Add unit tests proving group and mood edits do not mutate active locks in `app/src/test/kotlin/com/lockin/domain/templates/TemplateUseCasesTest.kt`
- [ ] T051 [P] [US2] Add contract tests for group and mood behavior in `app/src/test/kotlin/com/lockin/contracts/GroupsAndMoodsContractTest.kt`
- [ ] T052 [P] [US2] Add ViewModel tests for group creation, mood creation, and start-from-template flows in `app/src/test/kotlin/com/lockin/ui/templates/TemplatesViewModelTest.kt`

### Implementation for User Story 2

- [ ] T053 [P] [US2] Implement group domain model and validation rules in `app/src/main/kotlin/com/lockin/domain/groups/LockGroup.kt`
- [ ] T054 [P] [US2] Implement mood domain model and validation rules in `app/src/main/kotlin/com/lockin/domain/moods/Mood.kt`
- [ ] T055 [US2] Implement group and mood use cases for create, edit, archive, and start lock from template in `app/src/main/kotlin/com/lockin/domain/templates/TemplateUseCases.kt`
- [ ] T056 [US2] Implement groups ViewModel and screen for group list, group edit, and app membership selection in `app/src/main/kotlin/com/lockin/ui/groups/GroupsScreen.kt`
- [ ] T057 [US2] Implement moods ViewModel and screen for mood list, mood edit, app membership, and optional default duration in `app/src/main/kotlin/com/lockin/ui/moods/MoodsScreen.kt`
- [ ] T058 [US2] Integrate start-from-group and start-from-mood flows with lock creation use cases in `app/src/main/kotlin/com/lockin/ui/lockcreate/CreateLockViewModel.kt`
- [ ] T059 [US2] Add groups and moods navigation destinations and entry actions in `app/src/main/kotlin/com/lockin/ui/app/LockinNavGraph.kt`

**Checkpoint**: User Story 2 works independently after foundational tasks and can create active locks from reusable templates.

---

## Phase 5: User Story 3 - Enforce locks across app state changes (Priority: P2)

**Goal**: Active locks survive app restart, device reboot, package removal/reinstall, foreground blocked apps, overlapping locks, and uncertain policy state.

**Independent Test**: Create an active lock, restart Lockin, reboot the device, uninstall or reinstall the same package identifier where possible, and verify the app remains blocked until the latest lock end time.

### Verification for User Story 3

- [ ] T060 [P] [US3] Add unit tests for package-id-based reinstall reconciliation in `app/src/test/kotlin/com/lockin/device/PackageReconciliationTest.kt`
- [ ] T061 [P] [US3] Add unit tests for fail-closed enforcement decisions in `app/src/test/kotlin/com/lockin/device/FailClosedPolicyTest.kt`
- [ ] T062 [P] [US3] Add Robolectric tests for boot and package-change receiver orchestration in `app/src/test/kotlin/com/lockin/device/ReceiverReconciliationTest.kt`
- [ ] T063 [P] [US3] Add Room integration tests for policy reconciliation event recording in `app/src/test/kotlin/com/lockin/data/PolicyReconciliationRepositoryTest.kt`
- [ ] T064 [US3] Add managed-device instrumentation tests for Device Owner policy gateway calls in `app/src/androidTest/kotlin/com/lockin/device/DevicePolicyGatewayInstrumentedTest.kt`
- [ ] T065 [US3] Record manual managed-device validation for reboot, reinstall, uninstall blocking, foreground app removal, and clock-change resistance in `specs/001-android-app-locks/quickstart.md`
- [ ] T096 [US3] Add verification for scheduled lock expiration and package release while Lockin is idle in `app/src/test/kotlin/com/lockin/domain/lock/LockExpirationSchedulerTest.kt`

### Implementation for User Story 3

- [ ] T066 [P] [US3] Implement package reconciliation trigger enum and policy result mapping in `app/src/main/kotlin/com/lockin/device/PolicyReconciliation.kt`
- [ ] T067 [US3] Extend lock policy enforcer with hide, suspend, uninstall-block, release, overlap, and fail-closed behavior in `app/src/main/kotlin/com/lockin/device/LockPolicyEnforcer.kt`
- [ ] T068 [US3] Implement boot receiver to reconcile active locks before normal use after reboot in `app/src/main/kotlin/com/lockin/device/BootReceiver.kt`
- [ ] T069 [US3] Implement package change receiver for installed, changed, replaced, and removed package events in `app/src/main/kotlin/com/lockin/device/PackageChangeReceiver.kt`
- [ ] T070 [US3] Implement app startup reconciliation in `app/src/main/kotlin/com/lockin/LockinApp.kt`
- [ ] T071 [US3] Implement date, time, and time-zone configuration restriction handling during active locks in `app/src/main/kotlin/com/lockin/device/TimeRestrictionPolicy.kt`
- [ ] T072 [US3] Implement active-lock expiration reconciliation and release of packages only when no active lock covers the package identifier in `app/src/main/kotlin/com/lockin/domain/lock/LockExpirationReconciler.kt`
- [ ] T097 [US3] Implement local lock expiration scheduling and trigger wiring in `app/src/main/kotlin/com/lockin/domain/lock/LockExpirationScheduler.kt`
- [ ] T073 [US3] Add local policy reconciliation event recording to all enforcement triggers in `app/src/main/kotlin/com/lockin/device/LockPolicyEnforcer.kt`

**Checkpoint**: User Story 3 proves lock credibility across lifecycle, boot, package, and policy uncertainty events.

---

## Phase 6: User Story 4 - See active locks and local statistics (Priority: P3)

**Goal**: The home screen prioritizes active locks, and local statistics summarize completed lock history without accounts, analytics, coaching, or social behavior.

**Independent Test**: Create and complete locks, open home and statistics, verify active locks are prominent and local statistics match Room history.

### Verification for User Story 4

- [ ] T074 [P] [US4] Add unit tests for statistic summary calculations in `app/src/test/kotlin/com/lockin/domain/statistics/StatisticsCalculatorTest.kt`
- [ ] T075 [P] [US4] Add Room integration tests for statistics queries across completed sessions and moods in `app/src/test/kotlin/com/lockin/data/StatisticsRepositoryTest.kt`
- [ ] T076 [P] [US4] Add ViewModel tests for active-lock-first home state in `app/src/test/kotlin/com/lockin/ui/home/HomeViewModelTest.kt`
- [ ] T077 [P] [US4] Add ViewModel tests for local-only statistics and empty history state in `app/src/test/kotlin/com/lockin/ui/stats/StatsViewModelTest.kt`
- [ ] T078 [US4] Add UI verification notes for black-and-white light/dark theme behavior in `specs/001-android-app-locks/quickstart.md`

### Implementation for User Story 4

- [ ] T079 [P] [US4] Implement lock session completion writer in `app/src/main/kotlin/com/lockin/domain/statistics/LockSessionRecorder.kt`
- [ ] T080 [P] [US4] Implement statistic summary calculator for required local metrics in `app/src/main/kotlin/com/lockin/domain/statistics/StatisticsCalculator.kt`
- [ ] T081 [US4] Implement home ViewModel prioritizing active locks, blocked apps, and remaining time in `app/src/main/kotlin/com/lockin/ui/home/HomeViewModel.kt`
- [ ] T082 [US4] Implement home Compose screen with active-lock-first layout and no coaching copy in `app/src/main/kotlin/com/lockin/ui/home/HomeScreen.kt`
- [ ] T083 [US4] Implement statistics ViewModel for total duration, most blocked apps, unique apps, completed count, longest duration, average duration, and most used mood in `app/src/main/kotlin/com/lockin/ui/stats/StatsViewModel.kt`
- [ ] T084 [US4] Implement statistics Compose screen with local-only summaries and empty state in `app/src/main/kotlin/com/lockin/ui/stats/StatsScreen.kt`
- [ ] T085 [US4] Wire home and statistics routes into the root app shell in `app/src/main/kotlin/com/lockin/ui/app/LockinNavGraph.kt`

**Checkpoint**: User Story 4 delivers active-lock visibility and local statistics.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Harden quality gates, documentation, performance, privacy, and end-to-end validation across all stories.

- [ ] T086 [P] Add project README with Device Owner provisioning, offline-only scope, and accepted factory-reset recovery in `README.md`
- [ ] T087 [P] Add static analysis and formatting configuration for Kotlin and Android sources in `app/build.gradle.kts`
- [ ] T088 [P] Add privacy guard test that fails if analytics, account, network, VPN, website-blocking, or remote-management dependencies are introduced in `app/src/test/kotlin/com/lockin/privacy/NoNetworkOrAnalyticsDependencyTest.kt`
- [ ] T089 [P] Add UI copy review test for no coaching, screen-time, parental-control, emergency unlock, recovery PIN, pause, temporary bypass, or override wording in `app/src/test/kotlin/com/lockin/ui/copy/ProductBoundaryCopyTest.kt`
- [ ] T090 Add performance test or benchmark fixture for statistics queries over 10,000 completed sessions in `app/src/test/kotlin/com/lockin/performance/StatisticsPerformanceTest.kt`
- [ ] T091 Run `./gradlew :app:testDebugUnitTest` and record verification result in `specs/001-android-app-locks/quickstart.md`
- [ ] T092 Run `./gradlew :app:assembleDebug` and record verification result in `specs/001-android-app-locks/quickstart.md`
- [ ] T093 Run `./gradlew :app:connectedDebugAndroidTest` on a Device Owner test device and record verification result in `specs/001-android-app-locks/quickstart.md`
- [ ] T094 Complete all quickstart managed-device scenarios and record final evidence in `specs/001-android-app-locks/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; starts immediately.
- **Foundational (Phase 2)**: Depends on Phase 1; blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2; recommended MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and uses US1 lock creation use cases for start-from-template integration.
- **User Story 3 (Phase 5)**: Depends on Phase 2 and strengthens US1 enforcement for lifecycle and package events.
- **User Story 4 (Phase 6)**: Depends on Phase 2 and benefits from completed sessions created by US1, US2, and US3.
- **Polish (Phase 7)**: Depends on completed target stories.

### User Story Dependencies

- **US1 Create an irreversible app lock**: MVP; no dependency on other user stories after foundation.
- **US2 Use groups and reusable lock moods**: Can be built after foundation, but start-from-template integration depends on US1 lock creation use cases.
- **US3 Enforce locks across app state changes**: Can be built after foundation, but full end-to-end validation depends on US1 active locks.
- **US4 See active locks and local statistics**: Can build UI shell after foundation, but meaningful statistics validation depends on completed lock sessions from US1 and US2.

### Within Each User Story

- Write automated tests before implementation for repeatable domain, repository, contract, and ViewModel behavior.
- Use managed-device validation tasks where Device Owner effects cannot be proven with local test doubles.
- Implement domain rules before repositories and UI integration.
- Implement repositories before ViewModels.
- Implement ViewModels before Compose screens.
- Complete each story checkpoint before claiming that story is independently deliverable.

### Parallel Opportunities

- Setup tasks T003, T006, T007, T008, and T009 can run in parallel after T001 and T002 are known.
- Foundational entity tasks T010-T013 can run in parallel.
- Foundational DAO tasks T014-T017 can run in parallel after entities exist.
- Foundational abstractions T019-T024 and UI components T030 can run in parallel after app module setup.
- Verification tests inside each user story marked [P] can run in parallel with each other before implementation.
- US2, US3, and US4 can be started by separate contributors after Phase 2, with integration tasks sequenced after US1 where noted.

---

## Parallel Example: User Story 1

```text
Task: "T031 [P] [US1] Add unit tests for duration validation and extension math in app/src/test/kotlin/com/lockin/domain/lock/LockDurationTest.kt"
Task: "T032 [P] [US1] Add unit tests for irreversible lock state transitions and rejected weaken actions in app/src/test/kotlin/com/lockin/domain/lock/LockStateMachineTest.kt"
Task: "T033 [P] [US1] Add Room integration tests for lock persistence, lock applications, extensions, and overlapping latest-end queries in app/src/test/kotlin/com/lockin/data/LockRepositoryTest.kt"
Task: "T034 [P] [US1] Add contract tests for Device Owner gate and create-lock validation in app/src/test/kotlin/com/lockin/contracts/CreateLockContractTest.kt"
```

## Parallel Example: User Story 2

```text
Task: "T049 [P] [US2] Add Room integration tests for group and mood persistence and app membership uniqueness in app/src/test/kotlin/com/lockin/data/TemplateRepositoryTest.kt"
Task: "T050 [P] [US2] Add unit tests proving group and mood edits do not mutate active locks in app/src/test/kotlin/com/lockin/domain/templates/TemplateUseCasesTest.kt"
Task: "T051 [P] [US2] Add contract tests for group and mood behavior in app/src/test/kotlin/com/lockin/contracts/GroupsAndMoodsContractTest.kt"
Task: "T052 [P] [US2] Add ViewModel tests for group creation, mood creation, and start-from-template flows in app/src/test/kotlin/com/lockin/ui/templates/TemplatesViewModelTest.kt"
```

## Parallel Example: User Story 3

```text
Task: "T060 [P] [US3] Add unit tests for package-id-based reinstall reconciliation in app/src/test/kotlin/com/lockin/device/PackageReconciliationTest.kt"
Task: "T061 [P] [US3] Add unit tests for fail-closed enforcement decisions in app/src/test/kotlin/com/lockin/device/FailClosedPolicyTest.kt"
Task: "T062 [P] [US3] Add Robolectric tests for boot and package-change receiver orchestration in app/src/test/kotlin/com/lockin/device/ReceiverReconciliationTest.kt"
Task: "T063 [P] [US3] Add Room integration tests for policy reconciliation event recording in app/src/test/kotlin/com/lockin/data/PolicyReconciliationRepositoryTest.kt"
```

## Parallel Example: User Story 4

```text
Task: "T074 [P] [US4] Add unit tests for statistic summary calculations in app/src/test/kotlin/com/lockin/domain/statistics/StatisticsCalculatorTest.kt"
Task: "T075 [P] [US4] Add Room integration tests for statistics queries across completed sessions and moods in app/src/test/kotlin/com/lockin/data/StatisticsRepositoryTest.kt"
Task: "T076 [P] [US4] Add ViewModel tests for active-lock-first home state in app/src/test/kotlin/com/lockin/ui/home/HomeViewModelTest.kt"
Task: "T077 [P] [US4] Add ViewModel tests for local-only statistics and empty history state in app/src/test/kotlin/com/lockin/ui/stats/StatsViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First

Deliver Phase 1, Phase 2, and Phase 3 only. This produces the smallest valuable Lockin slice: a Device Owner app that can create and extend irreversible locks for selected apps, refuse operation without Device Owner status, and reject weakening actions.

### Incremental Delivery

1. Complete setup and foundation.
2. Deliver US1 and validate the MVP on a managed test device.
3. Add US2 for reusable groups and moods.
4. Add US3 for reboot, restart, reinstall, fail-closed, and clock-change hardening.
5. Add US4 for active-lock home visibility and local statistics.
6. Complete polish quality gates and full quickstart validation.

### Quality Gates

Run and record these before considering the feature complete:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- `./gradlew :app:connectedDebugAndroidTest`
- All managed-device quickstart scenarios in `specs/001-android-app-locks/quickstart.md`
