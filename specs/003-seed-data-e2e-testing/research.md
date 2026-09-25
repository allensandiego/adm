# Research & Decisions: Seed Data & End-to-End Testing

**Date**: 2026-09-26 (refreshed) | Companion to [plan.md](plan.md) and [spec.md](spec.md)

## Scope

The feature adds deterministic test data and a browser-driven end-to-end suite to an otherwise
stack-pinned application. There were no platform unknowns (single Spring Boot stack; the
behavior under test is defined by features 001 and 002). The open questions were the seeding
mechanism, the browser-test harness, and how to keep the suite deterministic and CI-friendly.

## Decisions

### D-1: Seeding mechanism — profile-gated idempotent application seeder

- **Decision**: Add a `TestDataSeeder` component in
  `src/main/java/com/allensandiego/adm/config/`, activated only under the `test`/`dev`
  profiles (`@Profile`). It upserts defined rows by natural key (username, role name,
  permission code) inside a transaction, using the same repositories/services as the
  application, and is safe to run on every startup.
- **Rationale**: Constitution v2.0.0 uses Hibernate-managed H2 (no migration framework), and
  FR-003/FR-004 require idempotent, environment-confined seeding. Reusing services avoids
  bypassing guardrails and keeps validation in force.
- **Alternatives considered**:
  - `data.sql` / `import.sql` — rejected: fights Hibernate schema generation and cannot express
    upsert semantics cleanly.
  - Flyway/Liquibase seed migrations — rejected: no migration framework in the stack.
  - Test-only `@Sql` scripts — rejected: not available to a normally running application, so
    headed/manual E2E and the quickstart could not use them.

### D-2: Personas — one per authority class, credentials configurable

- **Decision**: Seed exactly three personas: `e2e.admin` (all permissions via a protected
  administrator role), `e2e.viewer` (a restricted, non-administrative role granting only view
  permissions), and `e2e.inactive` (an account in the inactive state, same limited role). The
  admin password comes from a configurable property with a documented test-only default;
  others are fixed test-only values. Passwords are stored hashed (BCrypt) consistent with 002.
- **Rationale**: FR-002 requires an administrator, a restricted user, and a deactivated
  account. One persona per authority class is the minimum that makes every allow/deny
  scenario expressible.
- **Alternatives considered**: A single account toggled per scenario — rejected: mutates shared
  state and breaks order-independence; many near-duplicate users — rejected: needless drift.

### D-3: Browser automation — Microsoft Playwright for Java, Chromium, test scope

- **Decision**: Use `com.microsoft.playwright:playwright` (test scope) with JUnit 5, driving
  Chromium. Browser binaries are provisioned with the Playwright CLI
  (`com.microsoft.playwright.CLI install chromium`) run once per environment/CI job; the suite
  creates one `Playwright`/`Browser` per run and fresh `BrowserContext`/`Page` per scenario.
- **Rationale**: The requester explicitly chose Java Playwright; it offers auto-waiting
  locators (FR-016), tracing/screenshots/video on failure (FR-015), and a first-class JUnit
  integration. Chromium-only matches the spec assumption.
- **Alternatives considered**: Selenium/WebDriver — rejected: noisier waits, no built-in
  tracing; raw HTTP client tests — rejected: not real browsers (FR-006).

### D-4: Harness lifecycle — start-in-process or connect to a base URL

- **Decision**: The suite resolves the application base URL from the `e2e.base-url` system
  property / environment variable. Default behavior starts the application under test
  in-process on a random port with the `test` profile; if a URL is supplied, the suite connects
  to the already-running instance and skips startup. A readiness check polls the base URL and
  fails fast with an actionable message when unreachable.
- **Rationale**: FR-013 requires a single documented command from a fresh checkout, while the
  Edge Cases require pointing at an existing instance. One resolver covers both.
- **Alternatives considered**: Always external (`mvn spring-boot:run` separately) — rejected:
  two commands and manual readiness; Testcontainers — rejected: embedded H2, nothing to
  containerize.

### D-5: Locators — role/label first, `data-testid` for non-semantic anchors

- **Decision**: Prefer accessible locators (roles, labels, link text) that survive styling
  changes; add stable `data-testid` attributes to the CoreUI-based templates only where no
  accessible anchor exists (e.g., table rows, status badges, toasts). Tests never key off
  CoreUI CSS classes or generated ids.
- **Rationale**: Coupling to a theme's classes makes the suite brittle; accessible locators
  double as an accessibility signal, and `data-testid` is the escape hatch the UI contract
  makes explicit (contracts/e2e-scenarios.md).
- **Alternatives considered**: XPath/CSS on CoreUI classes — rejected (flaky, theme-coupled);
  screenshot diffing — rejected (not behavior assertions; noisy).

### D-6: Determinism & isolation — baseline reset, unique run data, no sleeps

- **Decision**: Each scenario begins from the seeded baseline (re-applied or reset between
  scenarios) and, for mutating journeys, uses run-unique names (e.g., a per-run suffix) so
  scenarios never collide. The suite relies exclusively on Playwright auto-waiting /
  state-based assertions (FR-016) — no fixed sleeps.
- **Rationale**: FR-012 and SC-005 require order-independent, flake-free runs; SC-003 requires
  a stable baseline.
- **Alternatives considered**: Shared mutable setup ordered by test — rejected (fragile);
  retry-on-failure plugins — rejected: hides real flakiness rather than removing it.

### D-7: Artifacts & CI result — trace/screenshot on failure, JUnit XML gate

- **Decision**: On scenario failure capture a Playwright trace plus a full-page screenshot
  (and optionally video) into `target/e2e-artifacts/<scenario>/`, and always emit a JUnit XML
  result. The suite runs under Maven Failsafe (`*IT`/E2E naming) so `mvn verify` fails the
  build on any scenario failure, providing the machine-readable gate (FR-017).
- **Rationale**: FR-015 and SC-007 require artifacts sufficient to diagnose without re-running;
  SC-001/SC-002 and CI gating need a machine-readable pass/fail.
- **Alternatives considered**: HTML-only Playwright report — kept as a secondary convenience,
  but JUnit XML is the stable CI contract.

### D-8: Configuration & profiles — `test` profile, documented properties

- **Decision**: Put seed and E2E defaults in `src/main/resources/application-test.properties`
  (seed enabled, admin password, base URL behavior). Seeding is off unless the `test`/`dev`
  profile is active, so production startup creates no test data (FR-004).
- **Rationale**: Constitution III (profile-driven configuration) and FR-004.
- **Alternatives considered**: Hardcoded constants — rejected (not environment-confined, not
  overridable in CI).

### D-9: Package alignment — use the real base package

- **Decision**: New seed and E2E code uses the repository's actual base package
  `com.allensandiego.adm`, matching `AdmApplication` and the `pom.xml` groupId
  `com.allensandiego`. The seeder references feature 001's entities/services directly under this
  package.
- **Rationale**: The executable code base is `com.allensandiego.adm` (`AdmApplication`,
  `pom.xml` groupId `com.allensandiego`). Plans must reflect the real layout to be executable.
- **Status**: Resolved. Feature 001's plan was refreshed on 2026-09-26 to use
  `com.allensandiego.adm` throughout, so no reconciliation remains outstanding.
- **Alternatives considered**: Adopting a `com.example.rbac` or `com.allensandiego.rbac` package —
  rejected: renames existing code for no functional gain and contradicts the checked-in
  application.

## Resolved unknowns

All Technical Context unknowns are resolved by D-1..D-9. No `NEEDS CLARIFICATION` items remain.
