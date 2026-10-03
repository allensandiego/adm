# Research & Decisions: Seed Data & End-to-End Testing

**Date**: 2026-10-03 (refreshed) | Companion to [plan.md](plan.md) and [spec.md](spec.md)

## Scope

The feature adds deterministic test data and a browser-driven end-to-end suite to an otherwise
stack-pinned application. The behavior under test is defined by features 001 and 002. Key decisions
cover the seeding mechanism via `data.sql`, personas, the browser-test harness, and alignment
with the Spring Boot 4.x.x+ modular runtime and testing ecosystem.

## Decisions

### D-1: Seeding mechanism — declarative SQL initialization via `data.sql`

- **Decision**: Use `src/main/resources/data.sql` as the single source of truth for seed data,
  executed on startup via Spring Boot 4.x SQL initialization
  (`spring.sql.init.mode=always`, `spring.sql.init.schema-locations=classpath:drop.sql,classpath:schema.sql`,
  `spring.sql.init.data-locations=classpath:data.sql`).
- **Rationale**: The repository constitution and Spring Boot configuration already define SQL-managed
  schema initialization with PostgreSQL. Loading `data.sql` alongside `schema.sql` and `drop.sql`
  guarantees clean, deterministic reset and seeding across restarts without introducing custom programmatic
  seeders. Static UUIDs ensure predictable keys across E2E test runs.
- **Alternatives considered**:
  - Custom programmatic `TestDataSeeder` component — rejected: duplicates SQL seed scripts and adds
    unnecessary service/repository overhead during bootstrap.
  - Flyway/Liquibase migrations — rejected: no migration framework in the stack.

### D-2: Personas — use seeded user data from `data.sql`

- **Decision**: Adopt the three seeded user accounts from `src/main/resources/data.sql` as the standard
  E2E test personas:
  1. `adminuser` (Alice Admin, password `admin123`, role `admin` / Administrator): has all 13 permissions;
     serves as the administrator persona and lockout guard target.
  2. `jdoe` (John Doe, password `password123`, role `user` / User): restricted view-only access
     (`permission.view`, `role.view`, `user.view`); exercises 403 access refusals.
  3. `jsmith` (Jane Smith, password `password123`, roles `user` and `manager`): multi-role user;
     exercises effective permissions union (`permission.view`, `user.view`, `user.edit`, `user.activate`,
     `user.roles.assign`, `role.view`).
  - **Deactivated account testing**: All seeded users in `data.sql` are active (`enabled=true`). Deactivation
    scenarios (`E2E-AUTH-03`, `E2E-DENY-02`) toggle user status via `/users/{id}/status` or create an inactive fixture.
- **Rationale**: Reusing the existing `data.sql` seeded accounts eliminates duplicate test data and ensures
  that manual inspection and automated E2E tests share identical starting state and credentials.
- **Alternatives considered**: Separate synthetic E2E accounts (`e2e.admin`, etc.) — rejected: diverges from
  the existing `data.sql` source of truth.

### D-3: Browser automation & test framework — Spring Boot 4.x.x+ modular testing & Java Playwright

- **Decision**: Use `com.microsoft.playwright:playwright` (test scope, v1.63.0) with JUnit 5, driving
  Chromium. Browser binaries are provisioned via `com.microsoft.playwright.CLI install chromium`.
  To align with **Spring Boot 4.x.x+**, test dependencies in `pom.xml` explicitly declare the modular test
  starters:
  - `org.springframework.boot:spring-boot-starter-test`
  - `org.springframework.security:spring-security-test`
  - `org.springframework.boot:spring-boot-test` (explicit modular test starter)
  - `org.springframework.boot:spring-boot-test-autoconfigure` (explicit web test autoconfigure starter)
- **Rationale**: Spring Boot 4.0/4.1 modularized the test infrastructure away from monolithic autoconfiguration
  JARs into dedicated test modules. Explicit declaration avoids `ClassNotFoundException` during slice and
  integration testing. Java Playwright provides built-in auto-waiting (FR-016), failure tracing (FR-015), and
  headless/headed execution.
- **Alternatives considered**: Selenium/WebDriver — rejected: brittle waits, heavier configuration.

### D-4: Harness lifecycle — start-in-process or connect to a base URL

- **Decision**: The suite resolves the application base URL from the `e2e.base-url` system
  property / environment variable. Default behavior starts the application under test
  in-process on a random port using Spring Boot 4's `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`;
  if a URL is supplied, the suite connects to the already-running instance. A readiness check polls the base URL
  and fails fast with an actionable message when unreachable.
- **Rationale**: FR-013 requires a single documented command (`./mvnw verify`), while developer workflows
  benefit from pointing at an external instance.

### D-5: Locators — role/label first, `data-testid` for non-semantic anchors

- **Decision**: Prefer accessible locators (roles, labels, link text) that survive styling
  changes; add stable `data-testid` attributes to the CoreUI-based templates only where no
  accessible anchor exists (e.g., table rows, status badges, toasts). Tests never key off
  CoreUI CSS classes or generated ids.
- **Rationale**: Coupling to theme classes makes the suite brittle; accessible locators
  double as an accessibility signal.

### D-6: Determinism & isolation — baseline reset, unique run data, no sleeps

- **Decision**: Each scenario begins from the seeded baseline from `data.sql` and, for mutating journeys,
  uses run-unique names (e.g., a per-run suffix) so scenarios never collide. The suite relies exclusively on
  Playwright auto-waiting / state-based assertions (FR-016) — no fixed sleeps.
- **Rationale**: FR-012 and SC-005 require order-independent, flake-free runs; SC-003 requires a stable baseline.

### D-7: Artifacts & CI result — trace/screenshot on failure, JUnit XML gate

- **Decision**: On scenario failure capture a Playwright trace plus a full-page screenshot
  into `target/e2e-artifacts/<scenario>/`, and always emit a JUnit XML result. The suite runs under
  Maven Failsafe (`*IT`/E2E naming) so `./mvnw verify` fails the build on any scenario failure,
  providing the machine-readable gate (FR-017).
- **Rationale**: FR-015 and SC-007 require artifacts sufficient to diagnose without re-running;
  SC-001/SC-002 and CI gating need a machine-readable pass/fail.

### D-8: Configuration & test scripts — Spring Boot 4.x.x+ alignment

- **Decision**: Database initialization is configured in `src/main/resources/application.properties`
  via `spring.sql.init.*`. Test-specific overrides (E2E base URL, Playwright settings) are configured via
  system properties or `application-test.properties`. Test execution commands strictly use the Maven wrapper
  `./mvnw` per constitution Principle V (`./mvnw verify`, `./mvnw test`).
- **Rationale**: Aligns with Spring Boot 4.x properties and constitution Principle V.

### D-9: Package alignment — use the real base package

- **Decision**: E2E test code uses the repository's actual base package `com.allensandiego.adm`,
  matching `AdmApplication` and the `pom.xml` groupId `com.allensandiego`.
- **Rationale**: Reflects the real layout of the codebase.

## Resolved unknowns

All Technical Context unknowns are resolved by D-1..D-9. No `NEEDS CLARIFICATION` items remain.
