# Feature Specification: Seed Data & End-to-End Testing

**Feature Branch**: `003-seed-data-e2e-testing`

**Created**: 2026-09-21

**Status**: Draft

**Input**: User description: "Implemented seeded test data and end to end testing using java playwright"

## Clarifications

### Session 2026-09-21

- Q: Should the end-to-end suite drive application screens built from the vendored CoreUI templates in `coreui/`, rather than custom or test-only markup? → A: Yes — all application screens are built from the vendored CoreUI templates in `coreui/`, and the suite drives those real screens.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reproducible Seeded Dataset (Priority: P1)

A developer or tester starts the application in a test or development environment and finds a
known, fixed dataset already present: accounts, roles, permissions, and their assignments
covering both an all-powerful administrator and ordinary restricted users. The same dataset
appears every time the environment is started, with no duplicates and no manual setup.

**Why this priority**: Every downstream verification depends on a stable, known starting
state. Without deterministic seed data, end-to-end checks cannot assert predictable outcomes.

**Independent Test**: Can be fully tested by starting the environment twice and confirming
the dataset (users, roles, permissions, role_permissions, user_roles) is identical in all business-relevant fields and contains no duplicates.

**Acceptance Scenarios**:

1. **Given** a fresh environment, **When** the application starts, **Then** the known set of
   accounts, roles, permissions, and assignments exists and is immediately usable.
2. **Given** an environment where the dataset already exists, **When** the application starts
   again, **Then** the dataset is unchanged and no duplicate records are created.
3. **Given** the seeded dataset, **When** a tester inspects it, **Then** it includes at least
   one fully-privileged administrator, one limited user, and one deactivated account, each
   with documented credentials.
4. **Given** a production environment, **When** the application starts, **Then** no seeded
   test data is created.

---

### User Story 2 - End-to-End Verification of Core Admin Journeys (Priority: P2)

A tester runs one command and watches a real browser automatically sign in as the seeded
administrator and walk through the core management journeys — creating and editing
permissions, roles, and users, and assigning roles — asserting the results on screen exactly
as a human operator would see them.

**Why this priority**: This is the primary value of end-to-end testing: proving the assembled
application works for real users, not just in unit isolation.

**Independent Test**: Can be fully tested by running the browser-driven suite against a
started application and confirming every core journey passes without manual interaction.

**Acceptance Scenarios**:

1. **Given** the application and seeded data are running, **When** the suite starts, **Then**
   it launches a real browser and signs in without manual steps.
2. **Given** a signed-in administrator, **When** the suite manages the permission catalog,
   roles with their permissions, and users with role assignments, **Then** each journey is
   asserted against the visible result and passes.
3. **Given** a user assigned multiple roles, **When** the suite opens that account,
   **Then** it confirms the displayed effective permissions equal the union of those roles'
   permissions.
4. **Given** any failing step, **When** the run finishes, **Then** it produces a clear
   failure report with the step, observed state, and diagnostic artifacts.

---

### User Story 3 - End-to-End Verification of Permission Boundaries and Lockout Guards (Priority: P3)

The suite proves both sides of every permission boundary in a real browser: the authorized
administrator succeeds, while a restricted account is refused and never sees protected
content. It also proves the lockout guardrails: attempts to delete the final administrative
role or remove the last administrator are blocked.

**Why this priority**: Confirms the security posture holds in the deployed, assembled system,
where unit tests alone cannot guarantee the integrated filter chain and UI behave correctly.

**Independent Test**: Can be fully tested by running the boundary scenarios and confirming
every protected screen yields both a success path and a refusal path.

**Acceptance Scenarios**:

1. **Given** a restricted account, **When** the suite navigates to a management screen it
   lacks permission for, **Then** access is refused and the protected content is not shown.
2. **Given** the administrator account, **When** the suite performs the same navigation,
   **Then** the screen renders and the operation succeeds.
3. **Given** the final administrative role or last administrator, **When** the suite attempts
   to delete the role or remove the administrator, **Then** the action is blocked with a
   visible warning and the dataset is unchanged.
4. **Given** the deactivated seeded account, **When** the suite attempts to sign in, **Then**
   entry is refused even with correct credentials.

---

### Edge Cases

- The seed is applied to an environment that already contains user-created records — seeding
  must not overwrite or corrupt them.
- A seed run is interrupted midway — the next start must converge to the complete dataset
  without duplicates or partial state.
- The application is not running, or the configured address is unreachable, when the suite
  starts — the suite must fail fast with an actionable message.
- A seeded account's password is changed during a manual session — re-seeding must restore the
  documented state or leave it explicitly consistent.
- Two suite runs execute concurrently against the same environment — they must not corrupt each
  other's data or produce false failures.
- Browser assets load slowly or a network call is delayed — assertions must wait for the
  expected state rather than fail on fixed timing.
- A test is re-run in isolation after a failure — it must pass independently, without relying
  on data or ordering from other tests.
- The suite runs headless in automation and headed on a workstation — both modes must behave
  identically.
- A test fails — screenshots, traces, or equivalent artifacts must be captured for the failing
  step without requiring a rerun.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a deterministic, documented seed dataset containing all five core RBAC entities: users, roles, permissions, role_permissions (mapping), and user_roles (assignments).
- **FR-002**: The seed dataset MUST include at least: a fully-privileged administrator, a
  restricted non-administrator, and a deactivated account, with documented credentials.
- **FR-003**: Seeding MUST be idempotent: repeated starts MUST converge to the same state
  without creating duplicates or mutating unrelated records.
- **FR-004**: Seeding of test personas and test scenarios MUST be confined to test/development environments and MUST NOT introduce
  test data into a production environment.
- **FR-005**: The seed dataset MUST be the single documented source of truth for end-to-end
  tests, so tests never depend on data created in a previous test run.
- **FR-006**: System MUST provide an automated end-to-end test suite that drives a real browser
  through the running application, exercising it as an end user would.
- **FR-007**: The suite MUST cover the sign-in journeys: valid credentials succeed, invalid
  credentials are refused with a generic message, and deactivated accounts are refused.
- **FR-008**: The suite MUST cover the core administrative journeys in a browser: permission
  catalog management, role creation and permission editing, and user creation with role
  assignment.
- **FR-009**: The suite MUST verify that a user's displayed effective permissions equal the
  union of the permissions of their assigned roles.
- **FR-010**: The suite MUST verify both sides of every protected permission boundary: the
  authorized account succeeds and the unauthorized account is refused without seeing protected
  content.
- **FR-011**: The suite MUST verify the lockout guardrails: the final administrative role (`admin` / Administrator) cannot be deleted, the last administrator cannot be removed from their role, and the last active administrator account cannot be deactivated.
- **FR-012**: Each end-to-end test MUST be independently runnable and order-independent,
  passing on its own from the seeded starting state.
- **FR-013**: The suite MUST start or connect to the application automatically, requiring only
  a single documented command from a fresh checkout.
- **FR-014**: The suite MUST execute headless by default for automation while supporting a
  headed mode for local diagnosis, with identical assertions in both modes.
- **FR-015**: On failure, the suite MUST capture diagnostic artifacts (traces and full-page screenshots) for the failing step and report the failing journey, step, and observed state.
- **FR-016**: The suite MUST synchronize on expected application states (using state-based readiness checks) rather than fixed delays, so that normal render and network variability does not cause failures.
- **FR-017**: The suite MUST produce a clear machine-readable pass/fail result suitable for
  a continuous integration gate.
- **FR-018**: The suite MUST drive the application's actual production screens and MUST NOT depend on separate test-only pages.

### Key Entities *(include if feature involves data)*

- **Seed Dataset**: The fixed, versioned set of users, roles, permissions, grants,
  assignments, and account statuses that every test run starts from.
- **Test Persona**: A named, documented seeded account paired with the authority it is meant
  to exercise (administrator, restricted user, deactivated user).
- **End-to-End Scenario**: A browser-driven journey with a defined start state, user actions,
  and asserted visible outcome.
- **Verification Artifact**: The record produced by a scenario run — result plus screenshots
  or traces captured on failure for diagnosis.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the core administrative journeys (permission catalog, role creation/permission editing, user creation with role assignment) are exercised end-to-end through a real browser.
- **SC-002**: 100% of protected screens have both an authorized success scenario and an unauthorized refusal scenario — verifying dual-sided authorization.
- **SC-003**: Repeated seeding produces zero duplicates and an identical dataset across 10 consecutive application restarts with database reset between runs.
- **SC-004**: The full end-to-end suite completes in under 10 minutes on a standard development machine.
- **SC-005**: The suite is deterministic: 20 consecutive runs yield identical pass/fail results with zero flaky failures.
- **SC-006**: A new contributor can run the entire suite with one documented command and no
  manual environment changes.
- **SC-007**: Every failed scenario leaves diagnostic artifacts sufficient to identify the failing step without re-running.
- **SC-008**: 100% of lockout guardrail attempts (final admin role deletion, last admin removal, last active admin deactivation) are proven blocked by end-to-end scenarios — mutations rejected with conflict errors, no state change.

## Assumptions

- The end-to-end suite is implemented with Java Playwright (the mechanism specified by the
  requester) and drives a real browser against the running application.
- The behavior under test is defined by features 001-rbac-user-management (entities, services, screens) and 002-jdbc-user-authentication (sign-in, hashed credentials, account status); this feature verifies them end-to-end without adding new product behavior.
- The application screens exercised by the suite are built from the vendored CoreUI template set under `coreui/` (sign-in, dashboard, tables, forms, and error pages), consistent with constitution Principle III (Thymeleaf+CoreUI stack); no separate test-only pages are used.
- Seeded data is activated only under test/development profiles; the default runtime remains
  free of test accounts.
- The primary target browser is Chromium (single-engine focus per spec); Playwright supports headed/headless modes identically, no cross-browser requirements.
- The test environment uses PostgreSQL (constitution Principle III); schema and seed data are initialized by the configured SQL scripts.
- The application can be launched by the suite or pointed at an already-running instance via
  configuration.
- Browser automation runs headless in continuous integration and headed locally, with no
  differences in assertions.
- Seeded credentials are test-only, documented, and never used in production; passwords stored hashed (BCrypt) via feature 002 authentication model.
- Standard developer workstation (8GB RAM, 4 cores) and CI runners can host a headless Chromium browser; Playwright provisions binaries automatically.
