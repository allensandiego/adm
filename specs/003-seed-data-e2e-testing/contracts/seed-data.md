# Seed Data Contract

**Date**: 2026-09-21 | Implements FR-001..FR-005, SC-003.

## Guarantees

1. **Confined**: applied only under `test`/`dev` profiles. A production start creates none of
   these rows (FR-004).
2. **Idempotent**: every start converges to exactly the rows below; no duplicates (FR-003).
3. **Single source of truth**: E2E scenarios assert against this baseline and create only
   run-unique records on top of it (FR-005, FR-012).
4. **Known credentials**: the values below are documented test-only credentials; nothing is
   secret-derived beyond the configurable admin password.

## Personas

| username | password | status | roles | effective permissions |
|----------|----------|--------|-------|-----------------------|
| `e2e.admin` | `app.seed.admin-password` (property; test default documented in quickstart) | ACTIVE | Super Admin | all 13 codes |
| `e2e.viewer` | `e2e-viewer-pw` (test-only) | ACTIVE | Report Viewer | `permission.view`, `role.view`, `user.view` |
| `e2e.inactive` | `e2e-inactive-pw` (test-only) | INACTIVE | Report Viewer | none (account inactive) |

## Roles

| role | protected | permissions |
|------|-----------|-------------|
| Super Admin | yes | all 13 codes (FR-010 lockout target) |
| Report Viewer | no | `permission.view`, `role.view`, `user.view` |

## Permission catalog

`permission.view`, `permission.create`, `permission.edit`, `user.view`, `user.create`,
`user.edit`, `user.activate`, `user.roles.assign`, `role.view`, `role.create`, `role.edit`,
`role.delete`, `role.permissions.edit` — all active.

## Expected effective permissions (assertable)

- `e2e.admin`: 13 permissions (union of Super Admin grants).
- `e2e.viewer`: exactly `{permission.view, role.view, user.view}` — the union of Report Viewer
  grants; no mutation permission is present (drives every 403 boundary).
- `e2e.inactive`: empty set regardless of stored assignment (inactive status wins).

## Reconciliation rules

- Natural keys: user `username`, role `name`, permission `code`.
- Mappings reconciled to the documented set; unrelated end-user rows are never removed.
- Re-running the seeder 10 times (SC-003) leaves counts and values unchanged.

## Dependencies

- Requires the five RBAC entities and their validation/services from feature 001 to exist.
- Requires hashed credentials and account status from feature 002's authentication model.
