# Interface Contracts: Seed Data & End-to-End Testing

**Date**: 2026-10-03 (refreshed)

## Contract style

This feature exposes no product endpoints. Its interfaces are the *operational contracts* that
testers, CI, and the E2E harness rely on:

1. **Seed contract** — baseline data loaded at startup from `src/main/resources/data.sql`:
   [seed-data.md](seed-data.md).
2. **Suite contract** — how the E2E suite is invoked, configured, and observed:
   this file, plus the scenario catalogue [e2e-scenarios.md](e2e-scenarios.md).

## Seed contract summary

- Loaded at startup via `src/main/resources/data.sql` configured through Spring Boot 4.x SQL initialization (`spring.sql.init.data-locations=classpath:data.sql`).
- Idempotent: repeated starts reset and seed the exact baseline with no duplicates (FR-003).
- Provides seeded personas (`adminuser`, `jdoe`, `jsmith`) and their roles (`admin`, `user`, `manager`) and permissions needed by every scenario (FR-001, FR-002). Exact rows and credentials: [seed-data.md](seed-data.md).

## Suite contract

### Invocation (Spring Boot 4.x.x+ & Maven Wrapper)

| Action | Command (from repository root) |
|--------|-------------------------------|
| Install browser (once) | `./mvnw -q exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"` |
| Run full verification (app + E2E) | `./mvnw verify` |
| Run E2E against a running app | `./mvnw verify -De2e.base-url=http://localhost:8080` |
| Headed mode (local diagnosis) | `./mvnw verify -De2e.headless=false` |
| Run application locally | `./mvnw spring-boot:run` (or `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`) |

### Configuration properties

| property / env | default | meaning |
|----------------|---------|---------|
| `e2e.base-url` | empty (start in-process) | when set, connect to this running instance instead of starting one |
| `e2e.headless` | `true` | headed vs headless browser |
| `e2e.browser` | `chromium` | target engine (Chromium only per spec assumption) |
| `e2e.artifacts-dir` | `target/e2e-artifacts` | failure artifact location |

### Profiles & Modular Test Infrastructure

- Spring Boot 4.x.x+ modular test dependencies are declared: `spring-boot-starter-test`, `spring-security-test`, and explicit web testing modules `spring-boot-test` and `spring-boot-test-autoconfigure`.
- The application under test starts with SQL initialization active (`spring.sql.init.mode=always`), loading `schema.sql` and `data.sql`.
- E2E classes are named `*IT` (Maven Failsafe) and run during `./mvnw verify`; unit/MockMvc tests stay on Surefire (`./mvnw test`).

### Observability contract

- **JUnit XML** result for the E2E suite — the machine-readable CI gate (FR-017).
- On failure: Playwright **trace** + full-page **screenshot** (optionally video) under
  `target/e2e-artifacts/<scenario>/` (FR-015, SC-007).
- The suite logs the resolved base URL and startup/readiness status; an unreachable app fails
  fast with an actionable message (Spec Edge Case 3).

## Shared failure behavior

| condition | suite behavior |
|-----------|----------------|
| Application unreachable / not ready in time | fail fast with the attempted URL and a "start the app or pass -De2e.base-url" message |
| Assertion fails | capture artifacts, record the scenario/step/observed state, fail the build |
| Seed drift detected | reset and re-seed from `data.sql`; never mutate non-target baseline rows |
