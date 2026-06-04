# Implementation Plan: Lockin Irreversible App Locks

**Branch**: `001-android-app-locks` | **Date**: 2026-06-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-android-app-locks/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Build Lockin as a single Android Device Owner app that lets one local user create irreversible application locks, reusable lock groups, and moods. The implementation will store all commitments locally, enforce active locks by package identifier through device-owner policy controls, refuse normal operation without Device Owner status, and expose a black-and-white Compose UI focused on active locks and local statistics.

The technical approach is offline-only: Room is the canonical local source of truth for locks, groups, moods, app identities, lock sessions, and statistic inputs. A small policy enforcement layer reconciles Room state with Android device policy on app startup, device boot, package install/change events, and lock extension or expiration events.

## Technical Context

**Language/Version**: Kotlin 2.x; Android SDK target should use the current stable SDK available during implementation; minSdk 24 to support package suspension behavior required for launch blocking and notification suppression.

**Primary Dependencies**: Jetpack Compose, AndroidX Lifecycle ViewModel, Kotlin coroutines and Flow, Room, AndroidX Navigation Compose, AndroidX Test, JUnit, Robolectric where platform shadowing is sufficient, Android platform `DevicePolicyManager`, `DeviceAdminReceiver`, package broadcasts, and local scheduling primitives.

**Storage**: Room database stored on device only. No backend, cloud sync, account store, analytics SDK, or remote configuration. Small UI preferences, if needed, may use local DataStore, but lock commitments and statistics remain in Room because they require relational queries, partial updates, and integrity constraints.

**Testing**: JVM unit tests for domain rules, duration math, statistics, and repositories; Room in-memory tests for data integrity; Robolectric tests for app state and receiver orchestration where possible; instrumented tests on a managed emulator or physical test device for Device Owner policy behavior; manual managed-device validation for behaviors Android does not expose reliably in local test doubles.

**Target Platform**: Android only, single local user, Device Owner provisioned device, API 24+.

**Project Type**: Mobile app.

**Performance Goals**: Home screen reflects active lock state within 1 second after launch; lock confirmation persists local state before policy application returns to the user; active enforcement reconciliation completes within 5 seconds for normal app counts; statistics queries render within 1 second for at least 10,000 completed sessions.

**Constraints**: Device Owner is mandatory; app must fail closed for active locks; no network dependency; no user accounts; no analytics; no unlock, pause, temporary bypass, recovery PIN, emergency override, website blocking, VPN filtering, multi-user support, or remote management. Active locks can only be extended, and extension duration is added to remaining duration.

**Scale/Scope**: Single-device, single-user v1 app; expected scale is hundreds of installed apps, hundreds of groups or moods, and at least 10,000 historical lock sessions for local statistics.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Spec-Driven Scope**: PASS. The specification defines user stories, acceptance scenarios, edge cases, functional requirements, assumptions, and measurable success criteria for the v1 product boundary.
- **Evidence-Based Planning**: PASS. This plan reflects the current repository state, which contains Spec Kit scaffolding but no Android source tree yet. Technical decisions are grounded in the spec and Phase 0 research.
- **Testable Incremental Delivery**: PASS. Each user story maps to independent implementation slices with automated tests where practical and managed-device validation for Device Owner enforcement.
- **Operational Quality Gates**: PASS. Plan identifies formatting, static analysis, device-policy failure modes, local data integrity, privacy, performance, and managed-device validation gates.
- **Simplicity and Traceability**: PASS. A single Android app module and a small set of local layers are sufficient. No backend, account, analytics, or extra service boundaries are introduced.

**Gate Result**: PASS. No constitution violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-android-app-locks/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── app-behavior.md
└── tasks.md
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle/
└── libs.versions.toml

app/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/com/lockin/
│   │   │   ├── LockinApp.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── device/
│   │   │   │   ├── LockinDeviceAdminReceiver.kt
│   │   │   │   ├── DeviceOwnerState.kt
│   │   │   │   ├── DevicePolicyGateway.kt
│   │   │   │   ├── LockPolicyEnforcer.kt
│   │   │   │   ├── BootReceiver.kt
│   │   │   │   └── PackageChangeReceiver.kt
│   │   │   ├── domain/
│   │   │   │   ├── lock/
│   │   │   │   ├── appcatalog/
│   │   │   │   ├── groups/
│   │   │   │   ├── moods/
│   │   │   │   └── statistics/
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   ├── dao/
│   │   │   │   ├── entities/
│   │   │   │   └── repository/
│   │   │   └── ui/
│   │   │       ├── app/
│   │   │       ├── home/
│   │   │       ├── lockcreate/
│   │   │       ├── lockdetail/
│   │   │       ├── groups/
│   │   │       ├── moods/
│   │   │       ├── stats/
│   │   │       └── deviceowner/
│   │   └── res/
│   │       └── xml/device_admin_receiver.xml
│   ├── test/
│   │   └── kotlin/com/lockin/
│   └── androidTest/
│       └── kotlin/com/lockin/
```

**Structure Decision**: Use one Android application module under `app/`. Keep domain rules independent from Android policy APIs so duration math, irreversible-state validation, statistics, and repository behavior are testable without a managed device. Isolate `DevicePolicyManager` calls in `device/` because these behaviors require Device Owner status and managed-device validation.

## Complexity Tracking

No constitution violations or complexity exceptions.

## Phase 0 Research Summary

Phase 0 research is captured in [research.md](./research.md). Decisions resolved:

- Device Owner is the product boundary; without it, only setup, verification, and recovery guidance is shown.
- Active locks are persisted by package identifier and reconciled to installed package state.
- Enforcement uses hidden and suspended package policy where available, uninstall blocking while active, boot/package-change reconciliation, and fail-closed local state.
- Room is the canonical local source of truth because the feature needs relational history, many-to-many app membership, extension history, and statistics.
- Duration integrity uses monotonic elapsed time while running plus Device Owner date/time configuration restrictions to prevent user clock edits from shortening locks.

## Phase 1 Design Summary

Phase 1 design artifacts:

- [data-model.md](./data-model.md) defines entities, relationships, validation rules, and state transitions.
- [contracts/app-behavior.md](./contracts/app-behavior.md) defines Device Owner gate, lock creation, extension, enforcement, groups, moods, and statistics behavior contracts.
- [quickstart.md](./quickstart.md) defines end-to-end validation scenarios and managed-device checks.

## Post-Design Constitution Check

- **Spec-Driven Scope**: PASS. Design artifacts trace to the current spec without adding out-of-scope unlock, bypass, account, analytics, backend, VPN, or website-blocking behavior.
- **Evidence-Based Planning**: PASS. Android source structure is explicit for a new repository, and research documents the key platform and storage decisions.
- **Testable Incremental Delivery**: PASS. Quickstart and contracts define validation paths for each user story, including managed-device checks where automation is limited.
- **Operational Quality Gates**: PASS. Fail-closed enforcement, local privacy, persistence across boot/restart, and package-id reinstall behavior are represented in design and validation artifacts.
- **Simplicity and Traceability**: PASS. Single-module app, local Room database, and isolated device policy gateway are the smallest architecture that satisfies the spec.

**Gate Result**: PASS. Ready for task generation.
