# Data Model: Seed Data & End-to-End Testing

**Date**: 2026-10-03 (refreshed) | Derived from spec FR-001..FR-018 and `data.sql`.

This feature introduces **no new product entities**. It specifies the seeded *test data* over all five RBAC entities (users, roles, permissions, role_permissions, user_roles) defined in constitution Principle I and feature 001, sourced from `src/main/resources/data.sql`, plus the E2E run concepts. References: [../001-rbac-user-management/data-model.md](../001-rbac-user-management/data-model.md).

## Conventions

- Seed rows are defined declaratively in `src/main/resources/data.sql` and initialized at application startup via Spring Boot 4.x SQL initialization (`spring.sql.init.mode=always`, `spring.sql.init.data-locations=classpath:data.sql`).
- Deterministic UUIDs are assigned to each entity in `data.sql` for stable references across test scenarios.
- Row timestamps use database defaults (`now()`), consistent with constitution Principle I (UTC semantics for audit trails).
- Passwords are stored hashed (BCrypt) using Spring Security 7.x `{bcrypt}` delegation format for Spring Boot 4.x.x+.
- The application executes `drop.sql`, `schema.sql`, and `data.sql` on startup, ensuring clean, idempotent convergence to the baseline.

## Seed dataset composition (`src/main/resources/data.sql`)

### Permissions (catalog)

All 13 centralized permission codes with static UUIDs:
- `44444444-4444-4444-8444-444444444401`: `permission.view` ('View Permissions', `/permissions`)
- `44444444-4444-4444-8444-444444444402`: `permission.create` ('Create Permission', `/permissions/new`)
- `44444444-4444-4444-8444-444444444403`: `permission.edit` ('Edit Permission', `/permissions/{id}/edit`)
- `44444444-4444-4444-8444-444444444404`: `user.view` ('View Users', `/users`)
- `44444444-4444-4444-8444-444444444405`: `user.create` ('Create User', `/users/new`)
- `44444444-4444-4444-8444-444444444406`: `user.edit` ('Edit User', `/users/{id}/edit`)
- `44444444-4444-4444-8444-444444444407`: `user.activate` ('Activate/Deactivate User', `/users/{id}/status`)
- `44444444-4444-4444-8444-444444444408`: `user.roles.assign` ('Assign User Roles', `/users/{id}/roles`)
- `44444444-4444-4444-8444-444444444409`: `role.view` ('View Roles', `/roles`)
- `44444444-4444-4444-8444-444444444410`: `role.create` ('Create Role', `/roles/new`)
- `44444444-4444-4444-8444-444444444411`: `role.edit` ('Edit Role', `/roles/{id}/edit`)
- `44444444-4444-4444-8444-444444444412`: `role.delete` ('Delete Role', `/roles/{id}/delete`)
- `44444444-4444-4444-8444-444444444413`: `role.permissions.edit` ('Edit Role Permissions', `/roles/{id}/permissions`)

### Roles

| id | code | name | protected | permissions granted | purpose |
|----|------|------|-----------|---------------------|---------|
| `11111111-1111-4111-8111-111111111111` | `admin` | Administrator | yes | all 13 codes | full-authority persona; lockout target (G1/G2/G3, FR-011) |
| `22222222-2222-4222-8222-222222222222` | `user` | User | no | `permission.view`, `role.view`, `user.view` | restricted persona; exercises allow/deny boundaries (FR-010) |
| `33333333-3333-4333-8333-333333333333` | `manager` | Manager | no | `permission.view`, `user.view`, `user.edit`, `user.activate`, `user.roles.assign`, `role.view` | operational access; exercises multi-role resolution |

### Users (personas)

| id | username | password (plaintext) | email | name | status | roles | expected effective permissions |
|----|----------|----------------------|-------|------|--------|-------|--------------------------------|
| `aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa` | `adminuser` | `admin123` | `admin@example.com` | Alice Admin | ACTIVE (`enabled=true`) | `admin` | all 13 codes |
| `bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb` | `jdoe` | `password123` | `john@example.com` | John Doe | ACTIVE (`enabled=true`) | `user` | the 3 view codes |
| `cccccccc-cccc-4ccc-8ccc-cccccccccccc` | `jsmith` | `password123` | `jane@example.com` | Jane Smith | ACTIVE (`enabled=true`) | `user`, `manager` | union of `user` + `manager` (6 codes) |

> **Deactivated account testing**: All seeded users in `data.sql` are active (`enabled=true`). Deactivated account verification (`E2E-AUTH-03`, `E2E-DENY-02`) deactivates an account (e.g. `jdoe` or test fixture) via the `/users/{id}/status` endpoint/UI or creates an inactive user fixture.

### Mappings

- `role_permissions`:
  - `admin` (`11111111-1111-4111-8111-111111111111`) → all 13 permissions (`44444444-4444-4444-8444-444444444401` through `...413`).
  - `user` (`22222222-2222-4222-8222-222222222222`) → `permission.view`, `user.view`, `role.view`.
  - `manager` (`33333333-3333-4333-8333-333333333333`) → `permission.view`, `user.view`, `user.edit`, `user.activate`, `user.roles.assign`, `role.view`.
- `user_roles`:
  - `adminuser` (`aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa`) → `admin` (`11111111-1111-4111-8111-111111111111`).
  - `jdoe` (`bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb`) → `user` (`22222222-2222-4222-8222-222222222222`).
  - `jsmith` (`cccccccc-cccc-4ccc-8ccc-cccccccccccc`) → `user` (`22222222-2222-4222-8222-222222222222`) and `manager` (`33333333-3333-4333-8333-333333333333`).

## Idempotency & Initialization

1. Database initialization scripts (`drop.sql`, `schema.sql`, `data.sql`) are configured in `application.properties` and execute cleanly on application startup.
2. Fixed UUIDs in `data.sql` ensure exact reproducibility.
3. Restarting the application re-runs the initialization sequence, producing identical entity counts and attribute values without duplicates (SC-003).

## E2E run concepts

- **Seeded baseline**: the state populated by `data.sql`, against which every scenario asserts.
- **Run-unique mutation data**: mutating scenarios create records with a per-run suffix so they
  never collide with the baseline or with each other (FR-012).
- **Scenario**: a browser journey with a start state, actions, and asserted visible outcome;
  catalogued in [contracts/e2e-scenarios.md](contracts/e2e-scenarios.md).
- **Artifact set**: trace + screenshot (optionally video) captured on failure, plus the JUnit
  result, under `target/e2e-artifacts/<scenario>/`.

## Validation

- Jakarta Bean Validation and service-layer uniqueness from feature 001 enforce consistency on any scenario mutation.
- Effective-permission expectations in the persona table match constitution Principle II and feature 001's resolution rule (union of active permissions of assigned roles; inactive user → empty).
- Test scripts and test execution align with Spring Boot 4.x.x+ modular test infrastructure.
