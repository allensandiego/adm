# Quickstart: Seed Data & End-to-End Testing

**Date**: 2026-10-03 (refreshed) | Validates: [spec.md](spec.md) · [contracts/](contracts/README.md) ·
[data-model.md](data-model.md), constitution Principles I-V

## Prerequisites

- JDK 21 (satisfies constitution Principle III: Java 21 LTS or newer)
- Spring Boot 4.x.x+ (`spring-boot-starter-parent` 4.1.1 declared in `pom.xml`)
- Maven 3.9+ or the checked-in wrapper `./mvnw` (constitution Principle V)
- A headless-capable OS (developer workstation or CI runner; minimum 8GB RAM, 4 cores)
- PostgreSQL must be running and accessible using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
  (see `src/main/resources/application.properties`). Startup SQL scripts (`drop.sql`, `schema.sql`, `data.sql`) drop and recreate application tables and load seed data on startup.

## One-time setup

```sh
# Provision the Chromium browser used by Playwright (once per environment)
./mvnw -q exec:java \
  -D exec.mainClass=com.microsoft.playwright.CLI \
  -D exec.args="install chromium"
```

## Automated validation (test scripts)

```sh
# Runs full verification: unit tests (Surefire) and browser E2E suite (Failsafe)
./mvnw verify
```

Expected outcome: build succeeds under Spring Boot 4.x.x+ modular test infrastructure; `data.sql` baseline is initialized and every scenario in [contracts/e2e-scenarios.md](contracts/e2e-scenarios.md) passes. A machine-readable JUnit XML result is emitted for CI gating (FR-017).

Variants:

```sh
# Against an already-running application instance
./mvnw verify -De2e.base-url=http://localhost:8080

# Headed mode (watch the browser locally during diagnosis)
./mvnw verify -De2e.headless=false
```

On failure, inspect `target/e2e-artifacts/<scenario>/` for the trace and screenshot (FR-015).

## What the suite proves

- **Seeding (US1)**: seeded users (`adminuser`, `jdoe`, `jsmith`) and roles (`admin`, `user`, `manager`) in [contracts/seed-data.md](contracts/seed-data.md) are loaded from `data.sql`; restarting the app resets and re-seeds the dataset with zero duplicates (SC-003, constitution Principle I).
- **Sign-in (FR-007)**: valid administrator (`adminuser` / `admin123`) signs in; invalid credentials receive a generic refusal; inactive accounts are refused entry (SC-008 lockout).
- **Admin journeys (FR-008/FR-009)**: permission, role, and user management plus role assignment operate through CoreUI templates in a real browser; effective permissions equal the union of assigned roles (demonstrated with multi-role user `jsmith`).
- **Boundaries (FR-010, SC-002)**: for each protected screen, `adminuser` succeeds and restricted `jdoe` (role `user`) is refused (403) without seeing protected content (constitution Principle II).
- **Lockout guards (FR-011, SC-008)**: deleting the protected `admin` role, removing the last administrator assignment from `adminuser`, and deactivating `adminuser` are all blocked.

## Manual walkthrough (diagnosis)

1. Start the app: `./mvnw spring-boot:run` (or `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`).
2. Open `http://localhost:8080/login` and sign in as `adminuser` with password `admin123`.
3. Confirm the seeded `jdoe` and `jsmith` accounts and roles `admin` (`Administrator`), `user` (`User`), and `manager` (`Manager`) are present.
4. Sign out and sign in as `jdoe` (`password123`); confirm management screens beyond view permissions are refused (403).
5. Inspect `jsmith` to verify displayed effective permissions reflect the union of assigned roles (`user` + `manager`).
6. Deactivate a user (e.g. `jdoe` or test account) and confirm sign-in is refused even with correct credentials.
7. Restart the app; confirm `data.sql` initialization reloads the baseline with zero duplicates.

## Expected outcomes summary

| Scenario | Result |
|----------|--------|
| Valid active admin sign-in (`adminuser`) | session established, home renders |
| Invalid credentials | generic refusal, no session |
| Inactive account | refused even with correct password |
| Authorized admin screen/action (`adminuser`) | succeeds |
| Restricted persona (`jdoe`) | refused (403), no protected content |
| Multi-role persona (`jsmith`) | effective permissions equal union of `user` + `manager` |
| Final-admin / final-protected-role mutation | blocked, no change |
| Repeated starts | identical dataset from `data.sql`, zero duplicates |
| Scenario failure | trace + screenshot captured, build fails with JUnit XML |

## References

- Suite and seed interface contracts: [contracts/README.md](contracts/README.md)
- Exact seed rows and credentials: [contracts/seed-data.md](contracts/seed-data.md)
- Scenario catalogue: [contracts/e2e-scenarios.md](contracts/e2e-scenarios.md)
- Test data model and idempotency: [data-model.md](data-model.md)
- Design decisions: [research.md](research.md)
