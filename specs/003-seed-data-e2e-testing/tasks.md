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
- Base package (actual): `com.allensandiego.adm` (research D-9; matches `AdmApplication` and the
  `com.allensandiego` groupId in `pom.xml`).
- Seeder (main): `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`.
- E2E suite (test): `src/test/java/com/allensandiego/adm/e2e/` with a `support/` subpackage.
- Test resources / artifacts: `src/test/resources/e2e/` and `target/e2e-artifacts/`.
- Screens under test come from the vendored CoreUI asset set at `coreui/`, rendered as
  Thymeleaf templates in `src/main/resources/templates/`.
- Results log: `specs/003-seed-data-e2e-testing/notes.md` (created in T005).

> **Cross-feature dependency (from plan/research D-9)**: features `001-rbac-user-management`
> (entities, services, screens) and `002-jdbc-user-authentication` (sign-in, hashed
> credentials, account status) MUST be implemented first; this feature seeds and tests their
> behavior. Both now use the real base package `com.allensandiego.adm`, so **no package
> reconciliation remains outstanding** — the seeder imports 001's entities/services directly.

> ⚠️ **Suite-class naming contract**: `contracts/e2e-scenarios.md` pins the scenario classes as
> `AuthE2ETest`, `AdminJourneysE2ETest`, and `PermissionBoundaryE2ETest`. Failsafe's default
> include is `**/*IT.java` only, so these `*E2ETest` names would be **silently skipped** unless
> T001 adds an explicit `<includes>` pattern. T001 is load-bearing — verify it before writing
> any scenario.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: E2E build tooling and environment configuration

- [ ] T001 Configure the E2E build in `pom.xml`: add `<scope>test</scope>` to the existing
      `com.microsoft.playwright:playwright` 1.63.0 dependency (already present, currently
      unscoped); add `maven-failsafe-plugin` bound to `integration-test`/`verify` with explicit
      `<includes>` of `**/*E2ETest.java` and `**/*IT.java` (the `*E2ETest` pattern is required —
      see the naming contract above); add `<properties><failsafeArgLine>-De2e.base-url=${e2e.base-url}
      -De2e.headless=${e2e.headless} -De2e.browser=${e2e.browser} -De2e.artifacts-dir=${e2e.artifacts-dir}</failsafeArgLine></properties>`
      with matching `<properties>` defaults; add `exec-maven-plugin` for the Playwright browser
      install; and exclude `**/*E2ETest.java` from `maven-surefire-plugin` so 001/002 unit and
      MockMvc tests stay on `mvn test` (research D-3/D-7, `contracts/README.md`)
- [ ] T002 [P] Create `src/main/resources/application-test.properties` with seed and E2E
      defaults: seeding enabled, `app.seed.admin-password`, and `e2e.base-url` /
      `e2e.headless` / `e2e.browser` / `e2e.artifacts-dir` per `contracts/README.md`
- [ ] T003 [P] Create the E2E package skeleton `src/test/java/com/allensandiego/adm/e2e/` and
      `.../e2e/support/`, plus `src/test/resources/e2e/`, with a short package README noting
      the `*E2ETest` naming, the Failsafe wiring from T001, and why `-Dit.test=` (not `-Dtest=`)
      selects a single scenario class
- [ ] T004 [P] Create `HELP.md` at the repository root with suite invocation, browser
      provisioning (`./mvnw -q exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"`),
      the `test` profile requirement, and artifact locations per `contracts/README.md` and
      `quickstart.md` (SC-006)
- [ ] T005 [P] Create `specs/003-seed-data-e2e-testing/notes.md` as the run-results log used by
      T016, T023, T028, and T033, with placeholder headings per user story

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core seeding mechanics and the Playwright harness that every story relies on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Create the profile-gated idempotent seeder scaffold in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` with `@Profile({"test","dev"})`,
      a transactional upsert helper keyed by natural key (`username` / `name` / `code`), and
      no-op behavior when the profile is inactive (depends on T001..T005)
- [ ] T007 [P] Create `src/test/java/com/allensandiego/adm/e2e/support/E2EConfig.java`
      resolving base URL (start-in-process vs `-De2e.base-url`), headless flag, browser engine,
      artifacts dir, and persona credentials from configuration
- [ ] T008 Create the harness in `src/test/java/com/allensandiego/adm/e2e/support/E2EBase.java`:
      application start-or-connect, readiness polling with a fail-fast message naming the
      attempted URL, per-scenario `BrowserContext`/`Page`, and teardown (depends on T007)
- [ ] T009 [P] Create `src/test/java/com/allensandiego/adm/e2e/support/FailureArtifacts.java`
      capturing a Playwright trace and full-page screenshot on scenario failure under
      `target/e2e-artifacts/<scenario>/`
- [ ] T010 [P] Create `src/test/java/com/allensandiego/adm/e2e/support/RunUniqueData.java`
      providing the per-run id suffix used by mutating scenarios so they stay order-independent
      (FR-012, research D-6), plus a sign-in helper for each of the three personas

**Checkpoint**: Seeder mechanism and browser harness ready — user stories can now begin.

---

## Phase 3: User Story 1 - Reproducible Seeded Dataset (Priority: P1) 🎯 MVP

**Goal**: A known, fixed dataset (administrator, restricted, deactivated personas with their
roles/permissions/assignments) exists on every test/dev start, idempotently and confined to
those environments.

**Independent Test**: Start the environment 10 times and confirm the dataset (accounts, roles,
permissions, assignments) is identical with zero duplicates, and that a profile-less start
seeds nothing.

### Implementation for User Story 1

> All five tasks write `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`, so they
> are sequential by construction and carry no `[P]` marker.

- [ ] T011 [US1] Seed the 13-code permission catalog (all active) in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` per
      `contracts/seed-data.md`
- [ ] T012 [US1] Seed the `Super Admin` (protected, all codes) and `Report Viewer` (3 view
      codes) roles and their `role_permissions` mappings in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` (depends on T011)
- [ ] T013 [US1] Seed the `e2e.admin`, `e2e.viewer`, and `e2e.inactive` personas with hashed
      passwords, statuses, and `user_roles` assignments in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` (depends on T012)
- [ ] T014 [US1] Enforce idempotent reconciliation (create-if-missing, correct-if-drifted,
      never delete unrelated rows) and profile confinement/off-by-default in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java` (depends on T013)
- [ ] T015 [US1] Idempotency + confinement dataset test in
      `src/test/java/com/allensandiego/adm/config/TestDataSeederIT.java`: run the seeder across
      **10 consecutive starts** and assert identical counts/values and zero duplicates (SC-003);
      assert no seed rows under the default profile (FR-004) (depends on T014)
- [ ] T016 [US1] Verify US1 independently: restart the app 10 times in the `test` profile and
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

- [ ] T017 [P] [US2] Implement sign-in scenarios E2E-AUTH-01/02/03 in
      `src/test/java/com/allensandiego/adm/e2e/AuthE2ETest.java` per
      `contracts/e2e-scenarios.md`
- [ ] T018 [P] [US2] Implement permission-journey scenarios E2E-PERM-01/02 in
      `src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java`

### Implementation for User Story 2

> T019 and T020 extend the same `AdminJourneysE2ETest.java` file created in T018, so they are
> sequential and carry no `[P]` marker.

- [ ] T019 [US2] Implement role-journey and effective-permission scenarios E2E-ROLE-01 and
      E2E-EFF-01 in `src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java`
      (depends on T018)
- [ ] T020 [US2] Implement user-journey scenario E2E-USER-01 in
      `src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java` (depends on T019)
- [ ] T021 [US2] Add `data-testid` anchors required by US2 to the CoreUI-based Thymeleaf
      templates in `src/main/resources/templates/` (role/label locators first; testids only
      where no accessible anchor exists) per research D-5
- [ ] T022 [US2] Run the US2 scenarios headless and stabilize locators/waits until green
      (state-based waits only; no fixed sleeps) using
      `./mvnw verify -Dit.test='AuthE2ETest,AdminJourneysE2ETest'` — `failsafe:integration-test`
      selects classes via `-Dit.test=`, so `-Dtest=` would match nothing
- [ ] T023 [US2] Verify US2 independently and record results in
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

- [ ] T024 [P] [US3] Implement allow/deny boundary scenarios E2E-DENY-01/02 and E2E-ALLOW-01 in
      `src/test/java/com/allensandiego/adm/e2e/PermissionBoundaryE2ETest.java` per
      `contracts/e2e-scenarios.md`
- [ ] T025 [US3] Implement lockout-guard scenarios E2E-LOCK-01/02/03 in
      `src/test/java/com/allensandiego/adm/e2e/PermissionBoundaryE2ETest.java` (depends on T024)

### Implementation for User Story 3

- [ ] T026 [US3] Add `data-testid` anchors for the "not authorized" page and lockout conflict
      warnings/toasts to `src/main/resources/templates/` as needed
- [ ] T027 [US3] Run the US3 scenarios headless and stabilize until green (state-based waits
      only) using `./mvnw verify -Dit.test=PermissionBoundaryE2ETest`
- [ ] T028 [US3] Verify US3 independently and record results in
      `specs/003-seed-data-e2e-testing/notes.md`

**Checkpoint**: All permission boundaries and lockout guards proven end-to-end.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Determinism, CI gating, and documentation across all stories

- [ ] T029 [P] Add the repeat-run determinism harness (20 consecutive suite runs must yield an
      identical pass/fail result, SC-005) as a shell script under `scripts/` and record the
      result in `specs/003-seed-data-e2e-testing/notes.md`
- [ ] T030 [P] Add the suite runtime budget check in
      `src/test/java/com/allensandiego/adm/e2e/SuiteDurationTest.java` asserting the full suite
      completes in under 10 minutes (SC-004)
- [ ] T031 [P] Document the JUnit XML output location and the CI gate wiring in `HELP.md`
      (the Failsafe `*-failsafe.xml` reports under `target/failsafe-reports/` must fail the build
      on any scenario failure, FR-017)
- [ ] T032 Run the full `quickstart.md` validation end-to-end (`./mvnw verify` plus the manual
      walkthrough) and record any gaps in `specs/003-seed-data-e2e-testing/notes.md`
- [ ] T033 [P] Final review pass over `src/test/java/com/allensandiego/adm/e2e/` and
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`: no fixed sleeps,
      order-independent scenarios, no CoreUI CSS class selectors, seeding disabled outside
      test/dev, no secrets committed, and no `*E2ETest` class left outside the Failsafe
      includes declared in `pom.xml`

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
- **US3 (P3)**: Can start after Foundational — requires US1's personas and the Phase 2 harness;
  independently testable

### Within Each User Story

- Scenarios written first and demonstrated failing, then stabilized to green
- Seeder content before the idempotency test; selectors/testids before stabilization
- Tasks sharing one file (T011–T014 seeder, T018–T020 `AdminJourneysE2ETest`, T024–T025
  `PermissionBoundaryE2ETest`) are sequential by construction and never marked `[P]`
- Story verified independently before moving to the next priority

### Parallel Opportunities

- Phase 1: T002/T003/T004/T005 run in parallel (different files)
- Phase 2: T007/T009/T010 run in parallel (different support files); T008 follows T007
- Phase 3: T015 is a distinct test file but depends on the completed seeder, so it is not `[P]`
- US2: T017 (`AuthE2ETest`) and T018 (`AdminJourneysE2ETest`) run in parallel (different
  files); T019/T020 follow in the same file
- US3: T024 starts the boundary class; T025 continues it — not parallel. US3 work may be
  interleaved with US2 by a second developer, but it does not start until its own phase opens
- Polish: T029/T030/T031/T033 run in parallel (different files)

---

## Parallel Example: User Story 2

```bash
# Launch the independent scenario files together:
Task: "Implement sign-in scenarios E2E-AUTH-01/02/03 in src/test/java/com/allensandiego/adm/e2e/AuthE2ETest.java"
Task: "Implement permission-journey scenarios E2E-PERM-01/02 in src/test/java/com/allensandiego/adm/e2e/AdminJourneysE2ETest.java"

# Support layer built together in Phase 2:
Task: "E2EConfig in src/test/java/com/allensandiego/adm/e2e/support/E2EConfig.java"
Task: "FailureArtifacts in src/test/java/com/allensandiego/adm/e2e/support/FailureArtifacts.java"
Task: "RunUniqueData in src/test/java/com/allensandiego/adm/e2e/support/RunUniqueData.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (seeded baseline)
4. **STOP and VALIDATE**: T015 idempotency/confinement test green; 10 restarts identical
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → seeder mechanism + browser harness ready
2. US1 → deterministic baseline → Test → MVP
3. US2 → sign-in + admin journeys green → Test → Demo
4. US3 → boundaries + lockout guards green → Test → Demo
5. Polish → determinism, runtime budget, CI gate, docs

### Parallel Team Strategy

With multiple developers, once Foundational is done: Developer A = US1, Developer B = US2,
Developer C = US3. US2/US3 depend on US1's personas, so coordinate the baseline first or run
US1 lead.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps each task to a specific user story for traceability
- Constitution Principle IV: every protected boundary MUST assert both the permitted (success)
  and unauthorized (refused) sides — covered by T017/T018/T024/T025
- T001 gates the entire suite: without the explicit `**/*E2ETest.java` Failsafe include, every
  scenario in this feature is silently skipped and `./mvnw verify` reports a false pass
- Failsafe selects a single class with `-Dit.test=<Class>`, not `-Dtest=` (T022, T027)
- SC-003 requires 10 seeding passes; SC-004 requires the suite under 10 minutes; SC-005 requires
  20 identical suite runs
- Commit after each task or logical group; stop at checkpoints to validate stories
- Avoid: fixed sleeps, CSS-class selectors, shared mutable state, cross-story dependencies that
  break independence
