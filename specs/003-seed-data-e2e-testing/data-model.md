# Data Model: Seed Data & End-to-End Testing

**Date**: 2026-09-26 (refreshed) | Derived from spec FR-001..FR-018 and decisions D-1..D-9.

This feature introduces **no new product entities**. It specifies the seeded *test data* over all five RBAC entities (users, roles, permissions, role_permissions, user_roles) defined in constitution Principle I and feature 001, plus the E2E run concepts. References: [../001-rbac-user-management/data-model.md](../001-rbac-user-management/data-model.md).

## Conventions

- Seed rows use natural keys (permission `code`, role `name`, user `username`) as the
  idempotency key; no fixed UUIDs are required.
- Seeding upserts within one transaction: create-if-missing, correct-if-drifted, never delete
  end-user-created rows.
- Row timestamps are UTC, consistent with constitution Principle I (UTC semantics for audit trails).
- Passwords are stored hashed (BCrypt, feature 002 authentication model) and never in plaintext; only the documented test-only values are recoverable by testers.
- Seeding is active only under the `test`/`dev` profiles (FR-004, constitution Principle III).

## Seed dataset composition

### Permissions (catalog from feature 001, contracts/README.md)

All 13 centralized permission codes are seeded: `permission.view`, `permission.create`,
`permission.edit`, `user.view`, `user.create`, `user.edit`, `user.activate`,
`user.roles.assign`, `role.view`, `role.create`, `role.edit`, `role.delete`,
`role.permissions.edit`. Seeded as active.

### Roles

| role | protected | permissions granted | purpose |
|------|-----------|---------------------|---------|
| Super Admin | yes | all 13 codes | full-authority persona; lockout target (G1/G2/G3) |
| Report Viewer | no | `permission.view`, `role.view`, `user.view` | restricted persona; exercises allow/deny boundaries |

### Users (personas)

| username | password source | status | roles | expected effective permissions |
|----------|-----------------|--------|-------|--------------------------------|
| `e2e.admin` | `app.seed.admin-password` (test-only default documented) | ACTIVE | Super Admin | all 13 codes |
| `e2e.viewer` | fixed test-only value | ACTIVE | Report Viewer | the 3 view codes |
| `e2e.inactive` | fixed test-only value | INACTIVE | Report Viewer | empty (inactive status wins) |

### Mappings

- `role_permissions`: Super Admin → all codes; Report Viewer → its 3 codes.
- `user_roles`: `e2e.admin` → Super Admin; `e2e.viewer` → Report Viewer; `e2e.inactive` →
  Report Viewer.

## Idempotency & reconciliation rules

1. Look up by natural key; insert when absent.
2. When present, reconcile mutable attributes (permission `label`/`active`, role
   `description`, user `display_name`/`status`) to the documented seed values.
3. Reconcile mappings to the exact documented set (add missing; do not remove unrelated
   mappings belonging to non-seed roles/users).
4. Running the seeder N times yields identical counts and values (SC-003); concurrent starts
   must converge without duplicates.

## E2E run concepts

- **Seeded baseline**: the state after the seeder runs, against which every scenario asserts.
- **Run-unique mutation data**: mutating scenarios create records with a per-run suffix so they
  never collide with the baseline or with each other (FR-012).
- **Scenario**: a browser journey with a start state, actions, and asserted visible outcome;
  catalogued in [contracts/e2e-scenarios.md](contracts/e2e-scenarios.md).
- **Artifact set**: trace + screenshot (optionally video) captured on failure, plus the JUnit
  result, under `target/e2e-artifacts/<scenario>/`.

## Validation

- Bean Validation and service-layer uniqueness from feature 001 remain the only write paths; the seeder calls services/repositories rather than bypassing validation (D-1, constitution Principle III).
- Effective-permission expectations in the persona table must match constitution Principle II and feature 001's resolution rule (union of active permissions of assigned roles; inactive user → empty).
