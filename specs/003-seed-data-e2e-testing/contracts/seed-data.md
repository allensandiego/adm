# Seed Data Contract

**Date**: 2026-10-03 (refreshed) | Implements FR-001..FR-005, SC-003, constitution Principle I (five RBAC entities).

## Guarantees

1. **Declarative SQL Seed**: loaded at startup via `src/main/resources/data.sql` and Spring Boot 4.x SQL initialization (`spring.sql.init.data-locations=classpath:data.sql` following `schema.sql` and `drop.sql`).
2. **Idempotent & Deterministic**: startup initialization drops and recreates schema then loads exact baseline records, ensuring zero duplicates and deterministic state (FR-003, constitution Principle I).
3. **Single source of truth**: E2E scenarios assert against the seeded baseline from `data.sql` and create only run-unique records on top of it (FR-005, FR-012).
4. **Known credentials**: test passwords use Spring Security 7.x / Spring Boot 4.x `{bcrypt}` delegation format with documented plaintext passwords.

## Seeded Users (`data.sql`)

| username | password (plaintext) | email | status | roles (code / name) | effective permissions |
|----------|----------------------|-------|--------|---------------------|-----------------------|
| `adminuser` | `admin123` | `admin@example.com` | ACTIVE (`enabled=true`) | `admin` / Administrator | all 13 permission codes |
| `jdoe` | `password123` | `john@example.com` | ACTIVE (`enabled=true`) | `user` / User | `permission.view`, `role.view`, `user.view` (view-only) |
| `jsmith` | `password123` | `jane@example.com` | ACTIVE (`enabled=true`) | `user` / User, `manager` / Manager | union of User and Manager (`permission.view`, `user.view`, `user.edit`, `user.activate`, `user.roles.assign`, `role.view`) |

> **Note on Deactivated Account Testing**: All seeded users in `data.sql` are active (`enabled=true`). Scenarios verifying inactive user behavior (`E2E-AUTH-03`, `E2E-DENY-02`) toggle account status to disabled via `/users/{id}/status` using the administrator persona (`adminuser`), or create a disabled user fixture.

## Seeded Roles (`data.sql`)

| code | name | protected | permissions granted | purpose |
|------|------|-----------|---------------------|---------|
| `admin` | Administrator | yes | all 13 codes | full administrative access; lockout guard target (FR-011) |
| `user` | User | no | `permission.view`, `role.view`, `user.view` | restricted access; drives 403 boundary verification (FR-010) |
| `manager` | Manager | no | `permission.view`, `user.view`, `user.edit`, `user.activate`, `user.roles.assign`, `role.view` | operational oversight; multi-role union verification with `user` |

## Permission catalog

All 13 centralized permissions:
`permission.view`, `permission.create`, `permission.edit`, `user.view`, `user.create`,
`user.edit`, `user.activate`, `user.roles.assign`, `role.view`, `role.create`, `role.edit`,
`role.delete`, `role.permissions.edit` — all active (`enabled=true`).

## Expected effective permissions (assertable)

- `adminuser`: 13 permissions (union of Administrator grants).
- `jdoe`: exactly `{permission.view, role.view, user.view}` — union of User grants; no mutation permission is present (drives every 403 boundary).
- `jsmith`: exactly `{permission.view, user.view, user.edit`, `user.activate`, `user.roles.assign`, `role.view}` — union of User and Manager grants (proves multi-role resolution).
- Deactivated user (when toggled): empty set regardless of assigned roles (inactive status wins).

## Idempotency & Initialization

- Managed through `drop.sql`, `schema.sql`, and `data.sql` executed at startup via Spring Boot 4.x SQL initialization (`spring.sql.init.mode=always`).
- Fixed UUIDs in `data.sql` provide predictable identifiers across application restarts.
- Restarting the application resets tables and re-seeds the baseline deterministically (SC-003).

## Dependencies

- Requires the five RBAC tables defined in `schema.sql`.
- Requires BCrypt password verification with Spring Security 7.x `{id}` delegation format in Spring Boot 4.x.x+.
