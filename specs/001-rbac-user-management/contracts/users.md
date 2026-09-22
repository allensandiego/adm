# Contracts: Users

**Date**: 2026-09-21 | Security model: [README.md](README.md)

| Method | Path              | Permission      | Purpose                          | Success                          | Failure                        |
|--------|-------------------|-----------------|----------------------------------|----------------------------------|--------------------------------|
| GET    | /users            | user.view       | List users (paged, searchable)   | 200, list with status/roles      | 403 if lacking permission      |
| GET    | /users/new        | user.create     | New-user form                    | 200, form                        | 403 if lacking permission      |
| POST   | /users            | user.create     | Create a user                    | redirect to detail               | 400 blank/duplicate username   |
| GET    | /users/{id}       | user.view       | User detail + effective perms    | 200, detail                      | 404 unknown; 403 if lacking    |
| POST   | /users/{id}/edit  | user.edit       | Edit display name                | redirect to detail               | 400 invalid; 403; 409 conflict |
| POST   | /users/{id}/status| user.activate   | Activate/Deactivate user         | redirect to detail               | 409 guardrail (G3)              |
| POST   | /users/{id}/roles | user.roles.assign | Save assigned role set        | redirect to detail               | 400 unknown role; 409 (G2)     |

## User detail (FR-008 / FR-006)

- Detail shows account fields, status, assigned roles, and the consolidated effective
  permission list derived from assigned roles (union, active permissions only).
- Effective view updates consistently regardless of the screen it is opened from (FR-009).

## Role assignment (FR-007)

- Assignments are chosen via multi-select of all ACTIVE roles.
- Submit replaces the user's assigned set (single assignment per role enforced by the
  (user_id, role_id) key).
- Guardrail G2: removing a protected-role assignment from the final administrative user →
  **409** "Cannot remove the final administrative access". No change (FR-010).

## Status transitions

- `ACTIVE -> INACTIVE`: user loses access immediately; assignments remain stored.
- Guardrail G3: deactivating the last ACTIVE user holding a protected role → **409**
  "Cannot deactivate the final administrator". No change.
- `INACTIVE -> ACTIVE`: restores access consistent with current assignments.