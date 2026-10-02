# Interface Contracts: Seed Data & End-to-End Testing

**Date**: 2026-09-26 (refreshed)

## Contract style

This feature exposes no product endpoints. Its interfaces are the *operational contracts* that
testers, CI, and the E2E harness rely on:

1. **Seed contract** — what data must exist after startup in a test/dev profile:
   [seed-data.md](seed-data.md).
2. **Suite contract** — how the E2E suite is invoked, configured, and observed:
   this file, plus the scenario catalogue [e2e-scenarios.md](e2e-scenarios.md).

## Seed contract summary

- Active only under `test`/`dev` profiles; never in production (FR-004).
- Idempotent: repeated starts produce the same rows with no duplicates (FR-003).
- Provides the three personas and their roles/permissions needed by every scenario (FR-001,
  FR-002). Exact rows and credentials: [seed-data.md](seed-data.md).

## Suite contract

### Invocation

| Action | Command (from repository root) |
|--------|-------------------------------|
| Install browser (once) | `./mvnw -q exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"` |
| Run everything (app + E2E) | `./mvnw verify` |
| Run E2E against a running app | `./mvnw verify -De2e.base-url=http://localhost:8080` |
| Headed mode (local diagnosis) | `./mvnw verify -De2e.headless=false` |

### Configuration properties

| property / env | default | meaning |
|----------------|---------|---------|
| `e2e.base-url` | empty (start in-process) | when set, connect to this running instance instead of starting one |
| `e2e.headless` | `true` | headed vs headless browser |
| `e2e.browser` | `chromium` | target engine (Chromium only per spec assumption) |
| `e2e.artifacts-dir` | `target/e2e-artifacts` | failure artifact location |
| `app.seed.admin-password` | test-only default | seeded administrator password |

### Profiles

- The application under test starts with the `test` profile so seeding is enabled.
- E2E classes are named `*IT` (Failsafe) and run during `./mvnw verify`; unit/MockMvc tests from
  features 001/002 stay on Surefire (`./mvnw test`).

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
| Seed drift detected | reconcile to documented seed data; never delete unrelated rows |
