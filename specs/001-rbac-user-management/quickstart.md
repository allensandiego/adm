# Quickstart: RBAC User Management

**Date**: 2026-09-26 (refreshed) | Validates: [spec.md](spec.md) · [contracts/](contracts/README.md) ·
[data-model.md](data-model.md)

## Prerequisites

- JDK 21 (`pom.xml` `<java.version>`; constitution floor is Java 17 LTS)
- Maven 3.8+ (or the IDE import of choice) — the wrapper `./mvnw` is committed
- PostgreSQL must be running and accessible using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
  (see `src/main/resources/application.properties`); startup SQL scripts drop and recreate
  the application tables before loading seed data.
- CoreUI Admin Bootstrap 5 assets present in `src/main/resources/static/` (sourced from the
  vendored `coreui/` template) so screens render unstyled-free

## Setup & run

```sh
./mvnw spring-boot:run
```

- On startup, the SQL initialization scripts idempotently create the schema and load seed data
  (see `src/main/resources/data.sql`), including all 13 permissions, the protected Administrator
  role (`admin`), and the seeded administrator user (`adminuser` / password `admin123`).
- Open `http://localhost:8080/login`, sign in as `adminuser`.

## Automated validation (start here)

```sh
./mvnw test
```

Expected outcome: all tests green, including dual-sided authorization coverage required by
constitution Principle IV — every permission boundary asserts both **200 OK** for a permitted
role and **403 Forbidden** for an unauthorized role.

Test assets proving the constitutional gates:

- **Permission enforcement (SC-003)**: parameterized tests iterate each screen/form in
  contracts/; a holder of the matching permission receives 200, a user with all other
  permissions except it receives 403.
- **Fail-closed (Pr. II)**: unauthenticated requests to every non-whitelisted path return 302
  (redirect to /login); deactivated users receive 403 on every protected path.
- **Lockout guardrails (SC-004 / FR-010)**: tests attempt G1 (delete final protected role),
  G2 (remove final protected-role assignment), G3 (deactivate final admin) and assert 409
  with zero state change.
- **Effective permissions (SC-005 / FR-008)**: with a user holding multiple roles, the
  resolved set equals the union of active permissions; deactivating a permission removes it
  from the union.
- **Concurrency (FR-013)**: two overlapping role-permission saves — the second returns 409.
- **Audit (SC-006 / FR-014)**: every mutation creates an audit_event with actor, UTC
  timestamp, and before/after.

## Manual walkthrough (5-minute setup, SC-001/SC-002)

1. **Permission**: Permissions → New → code `report.view` , label "View Reports" → save.
   See it listed (FR-001/FR-002).
2. **Role**: Roles → New → "Report Viewer" → save → open role → Permissions editor →
   check `report.view` → save (FR-004/FR-005). Reopen to confirm the saved set persists.
3. **User**: Users → New → username `alice` → save → Assign roles "Report Viewer" → save.
   Detail now shows effective permissions = `report.view` (FR-006/FR-007/FR-008).
4. **Self-service demo of fail-closed**: sign out; test login with a user lacking
   `role.permissions.edit`; open Roles → editor shows 403 "Not authorized"
   (FR-011, SC-003).
5. **Mutation safety demo**: try deleting the Super Admin role or removing the seeded admin
   from it — both are refused with a conflict warning (FR-010, SC-004).
6. **Duplicate protection**: create a permission with `report.view` again → 400 "code
   already exists" (FR-002/FR-012).

## Expected outcomes summary

| Scenario                                   | Result                             |
|--------------------------------------------|------------------------------------|
| Permitted admin action                     | 200, change applied                |
| Unauthorized / deactivated user            | 403, no content disclosed          |
| Invalid or duplicate input                 | 400, clear messages, no write      |
| Final-admin removal (G1/G2/G3)             | 409, no change                     |
| Concurrent conflicting save                | 409 conflict message               |
| Effective permissions match role union     | always consistent (SC-005)         |

## References

- Contracts and conventions: [contracts/README.md](contracts/README.md)
- Data model, constraints, guardrails: [data-model.md](data-model.md)
- Design decisions: [research.md](research.md)