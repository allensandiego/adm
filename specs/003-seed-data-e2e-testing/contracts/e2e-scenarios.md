# E2E Scenario Contract

**Date**: 2026-09-26 (refreshed) | Implements FR-006..FR-018, SC-001/SC-002/SC-005/SC-008, constitution Principle IV (dual-sided authorization).

## Conventions

- Every scenario starts from the [seed baseline](seed-data.md) and is **order-independent**
  (FR-012, constitution Principle IV).
- Mutating scenarios use run-unique names (suffix from the run id) and never assert on rows
  created by another scenario.
- Locators: accessible roles/labels first; `data-testid` for non-semantic anchors (D-5).
- Result: each scenario emits a JUnit result; failures capture trace + screenshot under
  `target/e2e-artifacts/<scenario>/` (FR-015, SC-007).

## User Story 2 — sign-in and core admin journeys (P2)

| ID | Given / When / Then | FR | Class |
|----|---------------------|----|-------|
| E2E-AUTH-01 | Given unauthenticated, When a protected URL is opened, Then the sign-in screen appears; after valid admin credentials, Then the originally requested page (or home) renders | FR-007 | `AuthE2ETest` |
| E2E-AUTH-02 | Given the sign-in screen, When wrong password or unknown username is submitted, Then a single generic "invalid credentials" message shows and no session is created | FR-007 (feature 002 BCrypt authentication) | `AuthE2ETest` |
| E2E-AUTH-03 | Given the `e2e.inactive` persona, When correct credentials are submitted, Then entry is refused and no session is created | FR-007, US3 (SC-008 lockout) | `AuthE2ETest` |
| E2E-PERM-01 | Given signed-in admin, When a new permission is created, Then it appears in the list and is selectable in the role editor | FR-008 | `AdminJourneysE2ETest` |
| E2E-PERM-02 | Given signed-in admin, When a blank or duplicate permission code is submitted, Then a clear validation message shows and nothing is created | FR-008 | `AdminJourneysE2ETest` |
| E2E-ROLE-01 | Given signed-in admin, When a role is created with a permission set and reopened, Then the saved set is exactly what was configured | FR-008 | `AdminJourneysE2ETest` |
| E2E-USER-01 | Given signed-in admin, When a user is created and a role assigned, Then the assignment persists and is shown on the user detail | FR-008 | `AdminJourneysE2ETest` |
| E2E-EFF-01 | Given a user with multiple roles, When their detail is opened, Then displayed effective permissions equal the union of the roles' active permissions; deactivating a permission removes it from the union | FR-009 | `AdminJourneysE2ETest` |

## User Story 3 — permission boundaries and lockout guards (P3)

| ID | Given / When / Then | FR | Class |
|----|---------------------|----|-------|
| E2E-DENY-01 | Given the `e2e.viewer` persona, When each management screen/action beyond its view permissions is attempted, Then access is refused and no protected content is rendered | FR-010 (constitution Principle II) | `PermissionBoundaryE2ETest` |
| E2E-ALLOW-01 | Given the admin persona, When the same navigation/actions are performed, Then each succeeds (dual-sided counterpart of E2E-DENY-01) | FR-010 | `PermissionBoundaryE2ETest` |
| E2E-DENY-02 | Given the `e2e.inactive` persona (session simulated), When any protected screen is requested, Then access is refused | FR-010, SC-008 | `PermissionBoundaryE2ETest` |
| E2E-LOCK-01 | Given the final protected role (Super Admin), When deletion is attempted, Then it is blocked with a visible warning and the dataset is unchanged | FR-011, SC-008 (constitution Principle II) | `PermissionBoundaryE2ETest` |
| E2E-LOCK-02 | Given the last administrator assignment, When removal (including self-demotion) is attempted, Then it is blocked and nothing changes | FR-011, SC-008 | `PermissionBoundaryE2ETest` |
| E2E-LOCK-03 | Given the last active administrator, When deactivation is attempted, Then it is blocked and nothing changes | FR-011, SC-008 | `PermissionBoundaryE2ETest` |

## Cross-cutting behaviors (apply to every scenario)

| Behavior | Requirement |
|----------|-------------|
| Runs headless by default and identically headed | FR-014 |
| State-based waiting only (no fixed sleeps) | FR-016 |
| Order-independent, individually runnable | FR-012 |
| Uses seeded baseline / run-unique data only | FR-005 |
| Produces JUnit XML for the CI gate | FR-017 |
| Failure leaves diagnostic artifacts | FR-015 |
| Fails fast when the app is unreachable | Edge Case 3 |

## Coverage traceability

- **SC-001**: E2E-PERM-01, E2E-ROLE-01, E2E-USER-01, E2E-EFF-01 (permission/role/user/assignment
  journeys).
- **SC-002**: E2E-ALLOW-01 paired with E2E-DENY-01/E2E-DENY-02 for every protected screen.
- **SC-005**: repeated full-suite runs must yield identical results (enforced by CI schedule).
- **SC-008**: E2E-LOCK-01/02/03.
