<!--
Sync Impact Report
Version change: 0.0.0 -> 1.0.0
Modified principles:
- Placeholder principle 1 -> I. Spec-Driven Scope
- Placeholder principle 2 -> II. Evidence-Based Planning
- Placeholder principle 3 -> III. Testable Incremental Delivery
- Placeholder principle 4 -> IV. Operational Quality Gates
- Placeholder principle 5 -> V. Simplicity and Traceability
Added sections:
- Delivery Constraints
- Development Workflow
Removed sections:
- None
Templates requiring updates:
- UPDATED .specify/templates/plan-template.md
- UPDATED .specify/templates/spec-template.md
- UPDATED .specify/templates/tasks-template.md
- AUDITED .specify/templates/commands/*.md (directory absent; no update required)
- AUDITED .specify/extensions/*/commands/*.md (no update required)
- AUDITED AGENTS.md (no update required)
Follow-up TODOs:
- None
-->
# Lockin Constitution

## Core Principles

### I. Spec-Driven Scope
Every feature MUST start from a written specification that defines user value,
prioritized user stories, acceptance scenarios, functional requirements, edge
cases, assumptions, and measurable success criteria. Requirements MUST be stated
in product terms before implementation details are chosen, unless a technical
constraint is itself part of the user-visible requirement.

Rationale: Lockin uses Spec Kit to prevent implementation work from outrunning
validated scope and to keep decisions reviewable before code changes begin.

### II. Evidence-Based Planning
Implementation plans MUST be based on the current repository, current feature
specification, and any existing plan referenced from agent guidance. Plans MUST
record the actual language, dependencies, storage, testing approach, platform,
project structure, constraints, and unresolved research questions. Placeholder
technical choices MUST NOT proceed into task generation unless they are marked
as explicit clarifications or resolved in research.

Rationale: Accurate plans reduce rework and keep generated tasks aligned with
the real codebase rather than generic scaffolding.

### III. Testable Incremental Delivery
Each user story MUST be independently deliverable, independently verifiable, and
ordered by business priority. Every implementation plan and task list MUST state
how each story will be verified. Automated contract, integration, or unit tests
MUST be used where the behavior is repeatable in the project test stack; manual
verification is acceptable only when the plan records why automation is not
practical for that slice.

Rationale: Independent increments let the project validate value early while
keeping regressions visible as features accumulate.

### IV. Operational Quality Gates
Feature work MUST identify and verify quality gates relevant to the change:
formatting, static analysis, error handling, logging or observability,
configuration, security, performance, and data integrity. Plans MUST define
concrete constraints when a feature affects reliability, user data, external
interfaces, or runtime behavior. Tasks MUST include the checks needed to prove
those constraints before the feature is considered complete.

Rationale: Quality work is cheaper and more reliable when it is planned as part
of the feature instead of deferred to cleanup.

### V. Simplicity and Traceability
Solutions MUST use the smallest architecture that satisfies the specification
and current constraints. New abstractions, dependencies, services, or storage
boundaries MUST be justified by a concrete requirement or documented risk.
Specs, plans, tasks, code, and verification evidence MUST remain traceable to
the user stories and requirements they satisfy.

Rationale: A traceable, simple system is easier to review, maintain, and change
without weakening future feature delivery.

## Delivery Constraints

All generated feature artifacts MUST remain aligned:

- Specifications define user-visible behavior and measurable outcomes.
- Plans explain the selected technical approach, real project structure, and
  constitution check results.
- Task lists group work by independently verifiable user story and include
  cross-cutting verification tasks.
- Implementation work follows the established project conventions and avoids
  unrelated refactors unless the plan documents the need.

Any feature that handles user data, authentication, authorization, payments,
external APIs, background processing, or persistent state MUST include explicit
security, failure-mode, and data-integrity requirements before implementation
tasks are generated.

## Development Workflow

The standard workflow is specification, clarification when needed, planning,
task generation, implementation, and verification. A phase MUST NOT advance
while required placeholders remain unresolved, except for items explicitly
marked as clarifications with an owner or decision point.

Before implementation begins, the Constitution Check in the feature plan MUST
pass or document each violation with a simpler rejected alternative. Before work
is marked complete, the task list MUST show the verification performed for each
delivered user story and for applicable operational quality gates.

## Governance

This constitution supersedes conflicting local process guidance for Spec Kit
feature work in this repository. Amendments MUST be made by editing this file,
updating affected templates or runtime guidance in the same change, and adding
or updating the Sync Impact Report.

Versioning follows semantic versioning:

- MAJOR: Removes or redefines principles or governance in a backward
  incompatible way.
- MINOR: Adds a new principle or materially expands required guidance.
- PATCH: Clarifies wording, fixes errors, or makes non-semantic refinements.

Compliance review is required at plan time through the Constitution Check and at
completion time through task verification. Reviewers MUST block feature progress
when artifacts omit required scope, planning evidence, verification, or quality
gate information without a documented exception.

**Version**: 1.0.0 | **Ratified**: 2026-06-04 | **Last Amended**: 2026-06-04
