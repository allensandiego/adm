# Data Model: RBAC User Management

**Date**: 2026-09-26 (refreshed) | Derived from spec FR-001..FR-015 and constitution v2.1.0.

## Conventions

- Primary keys: UUID (v4) on every table.
- Timestamps: `created_at` / `updated_at` stored in UTC.
- Foreign keys: strict, every reference enforced. Mapping/assignment rows cascade on removal
  of their parent definition (clean relational teardown).
- Exactly five core RBAC tables; one supporting `audit_event` table (see Constitutions Check
  note in `plan.md`).
- All mutations validate via Jakarta Bean Validation before any persistence write.

## Entities

### users

Represents a person with a login account.

| field        | type      | rules                                                     |
|--------------|-----------|-----------------------------------------------------------|
| id           | UUID      | PK (v4)                                                   |
| username     | string    | unique, non-blank, <=64 chars, `[a-zA-Z0-9._-]+`          |
| password     | string    | inbound credential format `[a-zA-Z0-9._-]+`; stored hashed per Feature 002 (zero plaintext) |
| display_name | string    | non-blank, <=120 chars                                    |
| status       | enum      | `ACTIVE` / `INACTIVE`; default `ACTIVE`                   |
| version      | long      | optimistic locking (FR-013)                               |
| created_at   | timestamp | UTC, set once                                             |
| updated_at   | timestamp | UTC, on every change                                      |

Relationships:
- `user_roles` (1..n): cascade delete with the user (removing a user removes assignments).

State transitions: `ACTIVE <-> INACTIVE`. Deactivating strips effective access immediately
while assignments remain stored (rollback is a reactivation).

### roles

A named, reusable bundle of permissions.

| field         | type     | rules                                      |
|---------------|----------|--------------------------------------------|
| id            | UUID     | PK (v4)                                    |
| name          | string   | unique, non-blank, <=80 chars              |
| description   | string   | optional, <=255 chars                      |
| is_protected  | boolean  | default false; `true` only for Super Admin |
| version       | long     | optimistic locking (FR-013)                |
| created_at    | timestamp| UTC                                        |
| updated_at    | timestamp| UTC                                        |

Relationships:
- `role_permissions` (1..n): cascade delete — deleting a role removes its mappings.
- `user_roles` (1..n): cascade delete — deleting a role removes its assignments.
- Guardrail: `is_protected` role cannot be deleted while it is the last protected role, and
  its last assignment cannot be removed while it is the last protected-role assignment in the
  system (FR-010; G1/G2 in Quickstart).

### permissions

A single named capability; the atomic grant unit.

| field         | type    | rules                                          |
|---------------|---------|------------------------------------------------|
| id            | UUID    | PK (v4)                                        |
| code          | string  | unique, centralized id, `[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*`, <=80 chars (FR-015) |
| label         | string  | non-blank, <=120 chars                         |
| path          | string  | non-blank, <=120 chars                         |
| active        | boolean | default true (FR-003)                          |
| created_at    | timestamp| UTC                                           |
| updated_at    | timestamp| UTC                                           |

Relationships:
- `role_permissions` (1..n): cascade delete — deleting a permission removes its mappings.

State transitions: `active <-> inactive`. Inactive permissions cannot be newly granted to
roles (FR-003); existing grants remain stored but are excluded from effective-permission
resolution.

### role_permissions (mapping)

Connects roles to permissions; the role-permission editor mutates this table.

| field         | type  | rules                                    |
|---------------|-------|------------------------------------------|
| role_id       | UUID  | FK -> roles, cascade delete              |
| permission_id | UUID  | FK -> permissions, cascade delete        |
| PK            | (role_id, permission_id) — enforces at most one mapping per pair |

### user_roles (assignment)

Connects users to roles; the user-role assignment screen mutates this table.

| field   | type | rules                                    |
|---------|------|------------------------------------------|
| user_id | UUID | FK -> users, cascade delete              |
| role_id | UUID | FK -> roles, cascade delete              |
| PK      | (user_id, role_id) — enforces at most one assignment per pair (FR-007) |

## Effective permission resolution

`permissions_of(user) = union of p where u_roles(role) and role_permissions(p, role) and p.active`

- Computed in the security layer once per request (D-1) and exposed to views (D-2).
- Deactivated users resolve to an empty set regardless of stored assignments.
- Inactive permissions drop out of the union (FR-003).

## Lockout guardrails (application-level, FR-010 / FR-014)

- **G1**: Deleting a protected role is refused when it is the last protected role.
- **G2**: Removing a User-Role row is refused when the role is protected and no other user
  would retain a protected-role assignment afterward (covers self-demotion).
- **G3**: Deactivating a user is refused when that user is the last ACTIVE user holding any
  protected-role assignment.
- All guardrail rejections return a conflict-style error with clear messaging; the mutation
  is rejected before any write. Guardrails are enforced in the service layer and also tested
  (SC-004).

## Concurrency

- `roles` and `users` carry `version`; concurrent conflicting saves (two admins editing the
  same role) are refused with a conflict message (FR-013).

## Validation summary (FR-002, FR-012)

- Non-blank names/codes/labels with length bounds.
- Code pattern enforced centrally (FR-015).
- Uniqueness (username, role name, permission code) enforced with duplicate detection and
  clear user-facing errors.
- Applied by Jakarta Bean Validation on all inbound records and by service-layer uniqueness
  checks before any write (constitution Principle III).