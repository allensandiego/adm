---

description: "Task list for Seed Data & End-to-End Testing feature implementation"
---

# Tasks: Seed Data & End-to-End Testing

**Input**: Design documents from `/specs/003-seed-data-e2e-testing/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/e2e-scenarios.md

**Tests**: INCLUDED. The constitution (v2.0.0, Principle IV) mandates explicit tests for both
sides of every permission boundary plus lockout-critical behavior. Every end-to-end scenario
must be independently runnable and order-independent.

**Organization**: Tasks are grouped by user story to enable independent implementation and
testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot Maven project at repository root, artifact `adm`.
- Base package: `com.allensandiego.adm` (matches `AdmApplication` and the `pom.xml` groupId
  `com.allensandiego`).
- Main sources: `src/main/java/com/allensandiego/adm/`; tests:
  `src/test/java/com/allensandiego/adm/`; views: `src/main/resources/templates/`.
- Config: `src/main/resources/application.properties` (embedded H2, Hibernate `ddl-auto`).
- E2E test sources: `src/test/java/com/allensandiego/adm/e2e/`.
- Static assets: `src/main/resources/static/` (CSS/JS copied from the vendored `coreui/`
  template at the repo root).

> **Cross-feature dependencies**: this feature depends on features 001-rbac-user-management
> (entities, services, screens) and 002-jdbc-user-authentication (sign-in, hashed credentials,
> account status). Feature 003 verifies them end-to-end without adding new product behavior.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Build dependencies, configuration, and E2E harness for browser-driven testing

- [ ] T001 Verify `pom.xml` carries the E2E stack and add nothing that is missing:
      `com.microsoft.playwright:playwright` (test scope, for browser automation),
      `org.junit.jupiter:junit-jupiter-engine`, `org.junit.jupiter:junit-jupiter-api`,
      `org.springframework.boot:spring-boot-starter-test` (already present from 001), and
      Maven Failsafe plugin configuration in `pom.xml` to run E2E tests (`*IT`/`E2E` naming)
- [ ] T002 Configure `src/main/resources/application-test.properties`: seed enabled, admin password
      default value (research D-8), base URL behavior (start-in-process or connect), and browser
      headless mode defaults — this file is profile-gated and never merged to production
- [ ] T003 Create the E2E harness package `src/test/java/com/allensandiego/adm/e2e/` with
      subpackages `config/`, `support/`, `scenarios/` for test organization (research D-9)
- [ ] T004 Implement the `E2EConfig` class in
      `src/test/java/com/allensandiego/adm/e2e/config/E2EConfig.java`: resolves the application
      base URL from `e2e.base-url` system property/environment variable, starts the application
      in-process on a random port with the `test` profile if no URL is supplied, and skips startup
      if a URL is provided (research D-4)
- [ ] T005 Implement the readiness check utility in
      `src/test/java/com/allensandiego/adm/e2e/support/ReadinessChecker.java`: polls the base URL
      for a readiness endpoint or health check, fails fast with an actionable message when
      unreachable (Edge Case 3, research D-4)
- [ ] T006 Create the Playwright browser setup in
      `src/test/java/com/allensandiego/adm/e2e/support/BrowserFactory.java`: provisions Chromium
      via Playwright CLI (`com.microsoft.playwright.CLI install chromium`), creates one
      `Playwright`/`Browser` per run, and a fresh `BrowserContext`/`Page` per scenario (research D-3)
- [ ] T007 Implement the page object base class in
      `src/test/java/com/allensandiego/adm/e2e/support/BasePage.java`: provides helper methods for
      state-based waiting (Playwright auto-waiting, FR-016), navigation to a URL, and failure
      artifact capture (trace + screenshot on exception) (research D-5, FR-015)

---

## Phase 2: Seed Data Implementation (FR-001 through FR-008)

**Purpose**: Implement deterministic, idempotent seeded test data for all five RBAC entities

**⚠️ CRITICAL**: Feature 003 depends on feature 001's entities and services being complete

- [ ] T008 Create the `TestDataSeeder` component in
      `src/main/java/com/allensandiego/adm/config/TestDataSeeder.java`: activated only under
      the `test`/`dev` profiles (`@Profile`), upserts defined rows by natural key (username, role
      name, permission code) inside a transaction, uses the same repositories/services as the
      application, and is safe to run on every startup (research D-1, FR-003)
- [ ] T009 Implement the permission seeding logic in `TestDataSeeder`: inserts all 13 centralized
      permission codes (`permission.view/create/edit`, `user.view/create/edit/activate/roles.assign`,
      `role.view/create/edit/delete/permissions.edit`) as active from `Permissions.java` (T006 from 001)
- [ ] T010 Implement the role seeding logic in `TestDataSeeder`: creates "Super Admin" with all 13
      codes and "Report Viewer" with only view permissions (`permission.view`, `role.view`,
      `user.view`) (data-model.md, research D-2)
- [ ] T011 Implement the user seeding logic in `TestDataSeeder`: creates three personas:
      `e2e.admin` (ACTIVE, Super Admin role), `e2e.viewer` (ACTIVE, Report Viewer role), and
      `e2e.inactive` (INACTIVE, Report Viewer role) with documented credentials (data-model.md,
      research D-2)
- [ ] T012 Implement the mapping seeding logic in `TestDataSeeder`: upserts `role_permissions` for
      Super Admin → all codes and Report Viewer → its 3 codes; upserts `user_roles` assignments
      for all three personas (data-model.md)
- [ ] T013 Verify idempotency: repeated seeding runs produce identical counts and values without
      duplicates (SC-003, data-model.md section "Idempotency & reconciliation rules")
- [ ] T014 Verify environment gating: seeding is off unless the `test`/`dev` profile is active;
      production startup creates no test data (FR-004)

---

## Phase 3: User Story 1 - Sign-In Journeys (FR-007, FR-008 part)

**Goal**: The suite covers sign-in journeys: valid credentials succeed, invalid credentials are
refused with a generic message, and deactivated accounts are refused (SC-008 lockout guardrail
testing in US3).

**Independent Test**: Each scenario is independently runnable from the seeded starting state.

### Tests for User Story 1 (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T015 [P] [US1] Sign-in success test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AuthE2ETest.java`: given the application
      and seeded data are running, when a real browser signs in as `e2e.admin` with valid
      credentials, then it reaches the dashboard without manual steps (FR-007)
- [ ] T016 [P] [US1] Invalid credentials test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AuthE2ETest.java`: given the sign-in
      screen, when wrong password or unknown username is submitted, then a single generic
      "invalid credentials" message shows and no session is created (FR-007)
- [ ] T017 [P] [US1] Inactive account test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AuthE2ETest.java`: given the
      `e2e.inactive` persona, when correct credentials are submitted, then entry is refused and
      no session is created (FR-007, US3 SC-008)

### Implementation for User Story 1

- [ ] T018 [P] [US1] Create the `AuthE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AuthE2ETest.java`: extends `BasePage`,
      signs in as each persona, and asserts the visible outcomes (FR-007, research D-5)

---

## Phase 4: User Story 2 - Core Admin Journeys (FR-008 part)

**Goal**: The suite covers core administrative journeys in a browser: permission catalog management,
role creation and permission editing, and user creation with role assignment.

**Independent Test**: Each scenario is independently runnable from the seeded starting state.

### Tests for User Story 2 (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T019 [P] [US2] Permission catalog test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AdminJourneysE2ETest.java`: given signed-in
      admin, when a new permission is created via the CoreUI form, then it appears in the list and
      is selectable in the role editor (FR-008, SC-001)
- [ ] T020 [P] [US2] Permission validation test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AdminJourneysE2ETest.java`: given signed-in
      admin, when a blank or duplicate permission code is submitted, then a clear validation message
      shows and nothing is created (FR-008)
- [ ] T021 [P] [US2] Role creation test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AdminJourneysE2ETest.java`: given signed-in
      admin, when a role is created with a permission set and reopened, then the saved set is exactly
      what was configured (FR-008, SC-001)
- [ ] T022 [P] [US2] User creation test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AdminJourneysE2ETest.java`: given signed-in
      admin, when a user is created and a role assigned, then the assignment persists and is shown on
      the user detail (FR-008, SC-001)
- [ ] T023 [P] [US2] Effective permissions test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AdminJourneysE2ETest.java`: given a user with
      multiple roles, when their detail is opened, then displayed effective permissions equal the union
      of the roles' active permissions; deactivating a permission removes it from the union (FR-009, SC-001)

### Implementation for User Story 2

- [ ] T024 [P] [US2] Create the `AdminJourneysE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/AdminJourneysE2ETest.java`: extends `BasePage`,
      signs in as admin, and executes each core journey with assertions (FR-008)
- [ ] T025 [P] [US2] Create the permission catalog page object in
      `src/test/java/com/allensandiego/adm/e2e/support/PermissionPage.java`: locators for list, form,
      detail views using accessible roles/labels and `data-testid` where needed (research D-5)
- [ ] T026 [P] [US2] Create the role management page object in
      `src/test/java/com/allensandiego/adm/e2e/support/RolePage.java`: locators for list, form, detail,
      and permission editor views (research D-5)
- [ ] T027 [P] [US2] Create the user management page object in
      `src/test/java/com/allensandiego/adm/e2e/support/UserPage.java`: locators for list, form, detail,
      and role assignment views (research D-5)

---

## Phase 5: User Story 3 - Permission Boundaries and Lockout Guards (FR-010, FR-011, SC-008)

**Goal**: The suite proves both sides of every permission boundary in a real browser: the authorized
administrator succeeds, while a restricted account is refused and never sees protected content. It
also proves the lockout guardrails: attempts to delete the final administrative role or remove the
last administrator are blocked.

**Independent Test**: Each scenario is independently runnable from the seeded starting state.

### Tests for User Story 3 (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T028 [P] [US3] Deny viewer test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: given the
      `e2e.viewer` persona, when each management screen/action beyond its view permissions is attempted,
      then access is refused and no protected content is rendered (FR-010, SC-002)
- [ ] T029 [P] [US3] Allow admin test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: given the
      admin persona, when the same navigation/actions are performed as E2E-DENY-01, then each succeeds
      (dual-sided counterpart of E2E-DENY-01, FR-010)
- [ ] T030 [P] [US3] Deny inactive test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: given the
      `e2e.inactive` persona (session simulated), when any protected screen is requested, then access
      is refused (FR-010, SC-008)
- [ ] T031 [P] [US3] Lockout final role test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: given the
      final protected role (Super Admin), when deletion is attempted, then it is blocked with a visible
      warning and the dataset is unchanged (FR-011, SC-008)
- [ ] T032 [P] [US3] Lockout last assignment test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: given the last
      administrator assignment, when removal (including self-demotion) is attempted, then it is blocked
      and nothing changes (FR-011, SC-008)
- [ ] T033 [P] [US3] Lockout last active user test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: given the last
      active administrator, when deactivation is attempted, then it is blocked and nothing changes (FR-011,
      SC-008)

### Implementation for User Story 3

- [ ] T034 [P] [US3] Create the `PermissionBoundaryE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/PermissionBoundaryE2ETest.java`: extends
      `BasePage`, signs in as each persona, and asserts allow/deny outcomes (FR-010)
- [ ] T035 [P] [US3] Create the lockout scenario helpers in
      `src/test/java/com/allensandiego/adm/e2e/support/LockoutHelpers.java`: methods to attempt deletion
      of the final role, removal of the last admin assignment, and deactivation of the last active user
      (FR-011)

---

## Phase 6: Cross-Cutting Behaviors (FR-012 through FR-018)

**Purpose**: Implement cross-scenario behaviors that apply to every end-to-end test

### Tests for Cross-Cutting Behaviors (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T036 [P] [Cross] Determinism test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/DeterminismE2ETest.java`: 10 consecutive
      application restarts with database reset produce identical seeded state (SC-003)
- [ ] T037 [P] [Cross] Order-independence test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/DeterminismE2ETest.java`: 20 consecutive
      suite runs yield identical pass/fail results with zero flaky failures (SC-005)
- [ ] T038 [P] [Cross] Artifact capture test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/ArtifactE2ETest.java`: when a scenario fails,
      then trace + screenshot are captured under `target/e2e-artifacts/<scenario>/` (FR-015, SC-007)
- [ ] T039 [P] [Cross] Fail-fast test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/ConnectivityE2ETest.java`: when the application
      is not running or the configured address is unreachable, then the suite fails fast with an actionable
      message (Edge Case 3)
- [ ] T040 [P] [Cross] Headless/headed test in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/ModeE2ETest.java`: the suite runs headless by
      default and identically when headed, with no differences in assertions (FR-014)

### Implementation for Cross-Cutting Behaviors

- [ ] T041 [P] [Cross] Create the `DeterminismE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/DeterminismE2ETest.java`: runs repeated seeding
      and suite executions to verify determinism (SC-003, SC-005)
- [ ] T042 [P] [Cross] Create the `ArtifactE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/ArtifactE2ETest.java`: triggers expected failures
      and verifies artifacts are captured correctly (FR-015, SC-007)
- [ ] T043 [P] [Cross] Create the `ConnectivityE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/ConnectivityE2ETest.java`: tests fail-fast behavior
      when the application is unreachable (Edge Case 3)
- [ ] T044 [P] [Cross] Create the `ModeE2ETest` class in
      `src/test/java/com/allensandiego/adm/e2e/scenarios/ModeE2ETest.java`: verifies identical behavior
      in headless and headed modes (FR-014)
- [ ] T045 [P] [Cross] Create the suite runner utility in
      `src/test/java/com/allensandiego/adm/e2e/support/SuiteRunner.java`: orchestrates the full E2E suite,
      captures JUnit XML results for CI gating (FR-017)

---

## Phase 7: Polish & Integration

**Purpose**: Cross-story validation, documentation, and final gate

- [ ] T046 Run `./mvnw verify` and execute the full E2E suite; resolve failures until SC-001..SC-008 all pass
- [ ] T047 Verify the seeded dataset includes at least: a fully-privileged administrator, one limited user,
      and one deactivated account, each with documented credentials (FR-002)
- [ ] T048 Confirm the suite drives the application's actual production screens (CoreUI templates in `coreui/`)
      and does not depend on separate test-only pages (FR-018)
- [ ] T049 Update `specs/003-seed-data-e2e-testing/quickstart.md` with run commands for the E2E suite
      (if this file exists, otherwise create it)
- [ ] T050 Document the seeded credentials in `specs/003-seed-data-e2e-testing/data-model.md` and ensure
      they are test-only and never used in production

---

## Dependencies & Execution Order

### Feature Dependencies

- **Feature 001 (RBAC User Management)**: MUST be complete before this feature begins
  - Entities: `Permission`, `Role`, `User`, `RolePermission`, `UserRole`
  - Services: `EffectivePermissionService`, `GuardrailService`
  - Screens: Permission catalog, role management, user management screens
- **Feature 002 (JDBC User Authentication)**: MUST be complete before this feature begins
  - Sign-in pipeline with hashed credentials (BCrypt)
  - Account status handling (ACTIVE/INACTIVE)

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — can start immediately
- **Phase 2 (Seed Data)**: Depends on Feature 001 entities and services being complete
- **Phase 3-5 (User Stories)**: All depend on Phase 2 seed data implementation
- **Phase 6 (Cross-Cutting)**: Depends on Phases 3-5 scenarios being implemented
- **Phase 7 (Polish)**: Depends on all desired phases being complete

### Parallel Opportunities

- T001-T007 in Setup can run together
- T008-T014 in Seed Data can run together (sequential chain within the seeder)
- T015-T018 (US1), T019-T027 (US2), T028-T035 (US3) can run in parallel after seed data lands
- T036-T045 in Cross-Cutting can run together
- T046-T050 in Polish are sequential validation steps

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Support classes (page objects, helpers) before scenario tests
- Scenario tests can run in parallel once support is ready

---

## Success Criteria Validation

| Criterion | Task(s) | Verification |
|-----------|---------|--------------|
| SC-001: 100% of core admin journeys exercised | T019-T023, T024-T027 | Run `AuthE2ETest` and `AdminJourneysE2ETest`; all pass |
| SC-002: Dual-sided authorization testing | T028-T030, T029, T034 | Run `PermissionBoundaryE2ETest`; allow/deny pairs both pass |
| SC-003: Zero duplicates across restarts | T013, T036 | Run seeder 10 times; verify identical counts and values |
| SC-004: Suite completes in under 10 minutes | T046 | Measure total suite runtime on standard dev machine |
| SC-005: Deterministic results (20 runs) | T037 | Run suite 20 times; verify identical pass/fail results |
| SC-006: Single command execution | T049 | Document `./mvnw verify -Pe2e` or equivalent |
| SC-007: Artifacts on failure | T038, T042 | Trigger a known failure; verify trace + screenshot captured |
| SC-008: Lockout guardrails proven blocked | T031-T033 | Run lockout scenarios; verify 409 errors and no state change |

---

## Implementation Strategy

### MVP First (Seed Data Only)

1. Complete Phase 1: Setup (E2E harness)
2. Complete Phase 2: Seed Data implementation
3. **STOP and VALIDATE**: run seeder twice, verify identical dataset, no duplicates
4. Deploy/demo seed data if ready

### Incremental Delivery

1. Setup + Seed Data → foundation ready
2. US1 (Sign-In Journeys) → validate authentication flows
3. US2 (Core Admin Journeys) → validate permission/role/user management
4. US3 (Permission Boundaries) → validate security guardrails
5. Cross-Cutting behaviors → deterministic, artifact-capturing suite

### Parallel Team Strategy

1. Complete Setup + Seed Data together
2. Then:
   - Developer A: US1 (Sign-In scenarios)
   - Developer B: US2 (Admin journeys), after seed data
   - Developer C: US3 (Boundaries), after US2
3. Stories integrate through shared seeded state — run Phase 6 before final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps a task to its user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing; commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- All paths use the real base package `com.allensandiego.adm`
- E2E tests use Playwright with Chromium (single browser focus)
- Seeded credentials are test-only, documented, and never used in production
- Passwords stored hashed (BCrypt) via feature 002 authentication model
- Standard developer workstation (8GB RAM, 4 cores) and CI runners can host headless Chromium

(End of file - total 365 lines)
