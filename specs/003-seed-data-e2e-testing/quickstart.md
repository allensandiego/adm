# Quickstart: Seed Data & End-to-End Testing

**Date**: 2026-09-26 (refreshed) | Validates: [spec.md](spec.md) · [contracts/](contracts/README.md) ·
[data-model.md](data-model.md)

## Prerequisites

- JDK 21 (satisfies the constitution's 17+ requirement)
- Maven 3.9+ or the checked-in wrapper `./mvnw`
- A headless-capable OS (developer workstation or CI runner)
- No external database — embedded H2 with Hibernate-managed schema (constitution v2.0.0)

## One-time setup

```sh
# Provision the Chromium browser used by Playwright (once per environment)
./mvnw -q exec:java \
  -D exec.mainClass=com.microsoft.playwright.CLI \
  -D exec.args="install chromium"
```

## Automated validation (start here)

```sh
# Runs the app in the test profile (seeding enabled) and the full E2E suite
./mvnw verify
```

Expected outcome: build succeeds; the seeder has created the documented baseline, and every
scenario in [contracts/e2e-scenarios.md](contracts/e2e-scenarios.md) passes. A machine-readable
JUnit XML result is the CI gate (FR-017).

Variants:

```sh
# Against an already-running application
./mvnw verify -De2e.base-url=http://localhost:8080

# Watch the browser locally
./mvnw verify -De2e.headless=false
```

On failure, inspect `target/e2e-artifacts/<scenario>/` for the trace and screenshot (FR-015).

## What the suite proves

- **Seeding (US1)**: the personas in [contracts/seed-data.md](contracts/seed-data.md) exist and
  are idempotent; restarting the app produces the same dataset with no duplicates (SC-003).
- **Sign-in (FR-007)**: valid admin signs in; invalid credentials get one generic message;
  the inactive persona is refused.
- **Admin journeys (FR-008/FR-009)**: permission, role, and user management plus role
  assignment work in a real browser; effective permissions equal the union of assigned roles.
- **Boundaries (FR-010, SC-002)**: for each protected screen the admin succeeds and the
  restricted persona is refused without seeing protected content.
- **Lockout guards (FR-011, SC-008)**: deleting the final protected role, removing the last
  administrator assignment, and deactivating the last administrator are all blocked.

## Manual walkthrough (diagnosis)

1. Start the app in a test/dev profile, e.g. `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`.
2. Open `http://localhost:8080/login` and sign in as `e2e.admin` (password from
   `app.seed.admin-password`).
3. Confirm the seeded `e2e.viewer` and `e2e.inactive` accounts and the Report Viewer role are
   present.
4. Sign out and sign in as `e2e.viewer`; confirm management screens beyond view permissions are
   refused.
5. Re-run the seeder path by restarting the app; confirm no duplicate rows appear.

## Expected outcomes summary

| Scenario | Result |
|----------|--------|
| Valid active admin sign-in | session established, home renders |
| Invalid credentials | generic refusal, no session |
| Inactive account | refused even with correct password |
| Authorized admin screen/action | succeeds |
| Restricted persona | refused, no protected content |
| Final-admin / final-protected-role mutation | blocked, no change |
| Repeated starts | identical dataset, zero duplicates |
| Scenario failure | trace + screenshot captured, build fails with JUnit XML |

## References

- Suite and seed interface contracts: [contracts/README.md](contracts/README.md)
- Exact seed rows and credentials: [contracts/seed-data.md](contracts/seed-data.md)
- Scenario catalogue: [contracts/e2e-scenarios.md](contracts/e2e-scenarios.md)
- Test data model and idempotency: [data-model.md](data-model.md)
- Design decisions: [research.md](research.md)
