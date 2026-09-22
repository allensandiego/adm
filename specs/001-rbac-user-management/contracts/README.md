# Interface Contracts: RBAC User Management

**Date**: 2026-09-21

## Contract style

Server-rendered admin web application. The interface the app exposes is HTTP: authenticated
HTML screens (GET) plus form submissions (POST). Responses are HTML; failures return the
appropriate client/security status with a user-facing message page or inline validation error.

## Security conventions (fail-closed, constitution Principle II)

- Enforce access in the security layer BEFORE any controller runs (filter-chain
  resolution). Controllers never act as the primary gate.
- Every screen/form below requires both an authenticated session AND the listed permission.
  Any missing piece → **403 Forbidden** (blocked, no content disclosed).
- `GET` reads require the matching `*.view` permission; mutations require the listed
  mutation permission.
- Deactivated users: resolved permission set is empty → all screens below return 403.
- Only whitelisted paths are public: `/login`, `/css/**`, `/js/**`, `/assets/**`, `/error`.

## Permission catalog (centralized, FR-015)

| code                    | label                    | grants                                          |
|-------------------------|--------------------------|-------------------------------------------------|
| permission.view         | View Permissions         | list + detail screens                           |
| permission.create       | Create Permission        | create screen/form                              |
| permission.edit         | Edit Permission          | label/active editing                            |
| user.view               | View Users               | list + detail screens                           |
| user.create             | Create User              | create screen/form                              |
| user.edit               | Edit User                | edit account fields                             |
| user.activate           | Activate/Deactivate User | status change                                   |
| user.roles.assign       | Assign User Roles        | add/remove role assignments                     |
| role.view               | View Roles               | list + detail screens                           |
| role.create             | Create Role              | create screen/form                              |
| role.edit               | Edit Role                | rename role                                     |
| role.delete             | Delete Role              | delete role (guarded by G1)                     |
| role.permissions.edit   | Edit Role Permissions    | role-permission editor                          |

Seed: the protected "Super Admin" role is granted every code above on startup (idempotent
seeder, see Quickstart).

## Shared failure behavior

| status | meaning                         | user sees                                  |
|--------|---------------------------------|--------------------------------------------|
| 403    | authenticated but not permitted | "Not authorized" page; no protected markup |
| 401    | not authenticated              | redirect to `/login`                       |
| 400    | invalid submission             | inline field errors + summary              |
| 409    | concurrent edit or guardrail   | conflict message; nothing written          |

## Endpoint maps

See the per-area contract files:

- [permissions.md](permissions.md) — Permission CRUD + activation.
- [roles.md](roles.md) — Role CRUD + role-permission editor.
- [users.md](users.md) — User CRUD + activation + role assignment.