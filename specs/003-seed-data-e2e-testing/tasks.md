---
description: "Task list for Seed Data & End-to-End Testing feature implementation"
---

# Tasks: Seed Data & End-to-End Testing

**Input**: Design documents from `/specs/003-seed-data-e2e-testing/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md,
data-model.md, contracts/

**Tests**: INCLUDED. This feature *is* a test suite — the E2E scenarios are the deliverable
(FR-006..FR-011), and the constitution (Principle IV) mandates dual-sided authorization
coverage plus lockout-guard tests. Seed verification is also test-backed.

**Organization**: Tasks are grouped by user story to enable independent implementation and
testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot Maven project at repository root.
- Base package (actual): `com.allensandiego.adm`.
- Seeder (main): `src/main/java/com/allensandiego/adm/config/`.
- E2E suite (test): `src/test/java/com/allensandiego/adm/e2e/` with a `support/` subpackage.
- Test resources / artifacts: `src/test/resources/e2e/` and `target/e2e-artifacts/`.
- Screens under test come from the vendored CoreUI asset set at `coreui/`, rendered as
  Thymeleaf templates in `src/main/resources/templates/`.

> **Cross-feature dependency (from plan/research D-9)**: features `001-rbac-user-management`
> (entities, services, screens) and `002-jdbc-user-authentication` (sign-in, hashed
> credentials, account status) MUST be implemented first; this feature seeds and tests their
> behavior. Reconcile the `com.allensandiego.rbac` placeholder in 001 with `com.allensandiego.adm`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: E2E build tooling and environment configuration

- [ ] T001 Add test-scope `com.microsoft.playwright:playwright` and configure
      `maven-failsafe-plugin` (runs `*IT` / E2E classes) plus `exec-maven-plugin` (browser
      install) in `pom.xml`
- [ ] T002 [P] Create `src/main/resources/application-test.properties` with seed and E2E
      defaults: seeding enabled, `app.seed.admin-password`, and `e2e.base-url` /
      `e2e.headless` / `e2e.artifacts-dir` per contracts/README.md
- [ ] T003 [P] Create the E2E package skeleton `src/test/java/com/allensandiego/adm/e2e/` and
      `.../e2e/support/`, plus `src/test/resources/e2e/`, with a short package README noting
      `*IT` naming and the Failsafe wiring
- [ ] T004 [P] Document suite invocation, browser provisioning, and artifact locations in
      `HELP.md` per contracts/README.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core seeding mechanics and the Playwright harness that every story relies on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Create the profile-gated idempotent seeder scaffold in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` with `@Profile({"test","dev"})`,
      a transactional upsert helper keyed by natural key, and no-op behavior when the profile
      is inactive (depends on T001..T004)
- [ ] T006 [P] Create `src/test/java/com/allensandiego/adm/e2e/support/E2EConfig.java`
      resolving base URL (start-in-process vs `-De2e.base-url`), headless flag, browser engine,
      artifacts dir, and persona credentials from configuration
- [ ] T007 [P] Create the harness in `src/test/java/com/allensandiego/adm/e2e/support/E2EBase.java`:
      application start-or-connect, readiness polling with fail-fast message, per-scenario
      `BrowserContext`/`Page`, and teardown
- [ ] T008 [P] Create `src/test/java/com/allensandiego/adm/e2e/support/FailureArtifacts.java`
      capturing a Playwright trace and full-page screenshot on scenario failure under
      `target/e2e-artifacts/<scenario>/`

**Checkpoint**: Seeder mechanism and browser harness ready — user stories can now begin.

---

## Phase 3: User Story 1 - Reproducible Seeded Dataset (Priority: P1) 🎯 MVP

**Goal**: A known, fixed dataset (administrator, restricted, deactivated personas with their
roles/permissions/assignments) exists on every test/dev start, idempotently and confined to
those environments.

**Independent Test**: Start the environment twice and confirm the dataset (accounts, roles,
permissions, assignments) is identical with zero duplicates, and that a profile-less start
seeds nothing.

### Implementation for User Story 1

- [ ] T009 [US1] Seed the 13-code permission catalog (all active) in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` per
      `contracts/seed-data.md`
- [ ] T010 [US1] Seed the `Super Admin` (protected, all codes) and `Report Viewer` (3 view
      codes) roles and their `role_permissions` mappings in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`
- [ ] T011 [US1] Seed the `e2e.admin`, `e2e.viewer`, and `e2e.inactive` personas with hashed
      passwords, statuses, and `user_roles` assignments in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`
- [ ] T012 [US1] Enforce idempotent reconciliation (create-if-missing, correct-if-drifted,
      never delete unrelated rows) and profile confinement/off-by-default in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`
- [ ] T013 [P] [US1] Idempotency + confinement dataset test in
      `src/test/java/com/allensandiego/adm/config/TestDataSeederIT.java`: run the seeder twice,
      assert identical counts/values and zero duplicates (SC-003); assert no seed rows under the
      default profile (FR-004)
- [ ] T014 [US1] Verify US1 independently: restart the app twice in the `test` profile and
      confirm the dataset is unchanged; record the observed baseline in
      `specs/003-seed-data-e2e-testing/notes.md`

**Checkpoint**: Seeded baseline is deterministic and independently verifiable — MVP.

---

## Phase 4: User Story 2 - End-to-End Verification of Core Admin Journeys (Priority: P2)

**Goal**: One command signs in as the seeded administrator in a real browser and walks the
core management journeys, asserting results on screen.

**Independent Test**: Run the browser-driven US2 scenarios against the seeded application and
confirm sign-in, permission/role/user journeys, and effective-permission display all pass
without manual interaction.

### Tests for User Story 2 ⚠️

> Write these scenarios first; they must fail against a missing/incorrect app, then pass once
> the application (features 001/002) and selectors are correct.

- [ ] T015 [P] [US2] Implement sign-in scenarios E2E-AUTH-01/02/03 in
      `src/test/java/com/allensandiego/adm/e2e/AuthE2ETest.java` per
      `contracts/e2e-scenarios.md`
- [ ] T016 [P] [US2] Implement permission-journey scenarios E2E-PERM-01/02 in
      `src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java`
- [ ] T017 [US2] Implement role-journey and effective-permission scenarios E2E-ROLE-01 and
      E2E-EFF-01 in `src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java`
- [ ] T018 [US2] Implement user-journey scenario E2E-USER-01 in
      `src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java`

### Implementation for User Story 2

- [ ] T019 [US2] Add `data-testid` anchors required by US2 to the CoreUI-based Thymeleaf
      templates in `src/main/resources/templates/` (role/label locators first; testids only
      where no accessible anchor exists) per research D-5
- [ ] T020 [US2] Run the US2 scenarios headless and stabilize locators/waits until green
      (state-based waits only; no fixed sleeps) using `./mvnw verify -Dtest=...E2ETest`
- [ ] T021 [US2] Verify US2 independently and record results in
      `specs/003-seed-data-e2e-testing/notes.md`

**Checkpoint**: Sign-in and core admin journeys verified end-to-end in a real browser.

---

## Phase 5: User Story 3 - End-to-End Verification of Permission Boundaries and Lockout Guards (Priority: P3)

**Goal**: The suite proves both sides of every protected boundary and that the lockout
guardrails block dangerous mutations in the real UI.

**Independent Test**: Run the boundary/lockout scenarios and confirm each protected screen
yields both an authorized success and a refusal, and that final-admin/final-role mutations are
blocked with no state change.

### Tests for User Story 3 ⚠️

- [ ] T022 [P] [US3] Implement allow/deny boundary scenarios E2E-DENY-01/02 and E2E-ALLOW-01 in
      `src/test/java/com/allensandiego/adm/e2e/PermissionBoundaryE2ETest.java` per
      `contracts/e2e-scenarios.md`
- [ ] T023 [US3] Implement lockout-guard scenarios E2E-LOCK-01/02/03 in
      `src/test/java/com/allensandiego/adm/e2e/PermissionBoundaryE2ETest.java`

### Implementation for User Story 3

- [ ] T024 [US3] Add `data-testid` anchors for the "not authorized" page and lockout conflict
      warnings/toasts to `src/main/resources/templates/` as needed
- [ ] T025 [US3] Run the US3 scenarios headless and stabilize until green (state-based waits
      only) using `./mvnw verify -Dtest=...E2ETest`
- [ ] T026 [US3] Verify US3 independently and record results in
      `specs/003-seed-data-e2e-testing/notes.md`

**Checkpoint**: All permission boundaries and lockout guards proven end-to-end.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Determinism, CI gating, and documentation across all stories

- [ ] T027 [P] Add the repeat-run determinism check (20 consecutive suite runs, SC-005) and
      document the JUnit XML CI gate in `docs/` or `HELP.md`
- [ ] T028 [P] Add a profile-safety test asserting a default/profile-less startup creates no
      seed rows in `src/test/java/com/allensandiego/adm/config/SeedProfileSafetyTest.java`
- [ ] T029 [P] Update `HELP.md` / `README.md` with run instructions, profiles, and artifact
      locations per `quickstart.md`
- [ ] T030 Run the full `quickstart.md` validation end-to-end (`./mvnw verify` plus the manual
      walkthrough) and record any gaps in `specs/003-seed-data-e2e-testing/notes.md`
- [ ] T031 [P] Final review pass: no fixed sleeps, order-independent scenarios, no CoreUI CSS
      class selectors, seeding disabled outside test/dev, no secrets committed

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational completion
  - US1 (P1) → US2 (P2) → US3 (P3) recommended, but all are independently testable
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — no dependency on other stories; supplies the
  baseline that US2/US3 assert against
- **US2 (P2)**: Can start after Foundational — requires US1's personas to sign in and drive
  journeys
- **US3 (P3)**: Can start after Foundational — requires US1's personas and US2's harness
  patterns; independently testable

### Within Each User Story

- Scenarios written first and demonstrated failing, then stabilized to green
- Seeder content before the idempotency test; selectors/testids before stabilization
- Story verified independently before moving to the next priority

### Parallel Opportunities

- Phase 1 tasks T002/T003/T004 run in parallel (different files)
- Phase 2 tasks T006/T007/T008 run in parallel (different support files)
- US2 tests T015/T016 run in parallel (different files); US3 tests T022 runs parallel to US2
- Polish tasks T027/T028/T029/T031 run in parallel (different files)

---

## Parallel Example: User Story 2

```bash
# Launch the independent scenario files together:
Task: "Sign-in scenarios E2E-AUTH-01/02/03 in src/test/java/com/allensandiego/adm/e2e/AuthE2ETest.java"
Task: "Permission journeys E2E-PERM-01/02 in src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java"

# Support layer built together in Phase 2:
Task: "E2EConfig in src/test/java/com/allensandiego/adm/e2e/support/E2EConfig.java"
Task: "E2EBase harness in src/test/java/com/allensandiego/adm/e2e/support/E2EBase.java"
Task: "FailureArtifacts in src/test/java/com/allensandiego/adm/e2e/support/FailureArtifacts.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (seeded baseline)
4. **STOP and VALIDATE**: T013 idempotency/confinement test green; two restarts identical
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → seeder mechanism + browser harness ready
2. US1 → deterministic baseline → Test → MVP
3. US2 → sign-in + admin journeys green → Test → Demo
4. US3 → boundaries + lockout guards green → Test → Demo
5. Polish → determinism, CI gate, docs

### Parallel Team Strategy

With multiple developers, once Foundational is done: Developer A = US1, Developer B = US2,
Developer C = US3. US2/US3 depend on US1's personas, so coordinate the baseline first or run
US1 lead.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps each task to a specific user story for traceability
- Constitution Principle IV: every protected boundary MUST assert both the permitted (success)
  and unauthorized (refused) sides — covered by T015/T016/T022/T023
- Commit after each task or logical group; stop at checkpoints to validate stories
- Avoid: fixed sleeps, CSS-class selectors, shared mutable state, cross-story dependencies that
  break independence
