# Implementation Plan: Seed Data & End-to-End Testing

**Branch**: `003-seed-data-e2e-testing` | **Date**: 2026-09-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-seed-data-e2e-testing/spec.md`

**Note**: Plan produced by `/speckit.plan`; the detailed task breakdown is produced later by
`/speckit.tasks` into `tasks.md`.

## Summary

Deliver a deterministic, idempotent seeded dataset (administrator, restricted, and
deactivated personas plus the roles/permissions/assignments they need) and a Java Playwright
end-to-end suite that drives the real CoreUI-rendered screens of the running application. The
suite proves sign-in behavior, the core administrative journeys, effective-permission
correctness, both sides of every permission boundary, and the lockout guardrails — running
headless with one command and leaving diagnostic artifacts on failure. Design decisions in
[research.md](research.md); seed and scenario contracts in [contracts/](contracts/README.md);
test data model in [data-model.md](data-model.md); run/validation guide in
[quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21 (`pom.xml` `<java.version>21</java.version>`; satisfies constitution Principle III requirement of Java 17+ LTS)

**Primary Dependencies**: Spring Boot 4.1.1 (Maven parent). For test support,
`com.microsoft.playwright:playwright` 1.63.0 (already declared in `pom.xml`) plus JUnit 5 via
`spring-boot-starter-test` and `spring-security-test`. The application under test adds the
features 001/002 stack (Spring Web, Security, Thymeleaf, Validation, H2) during their
implementation; this feature adds test-scope dependencies only, plus the Maven Failsafe
plugin binding for the E2E gate (research D-3, D-7).

**Storage**: Embedded H2 (test/development profiles), Hibernate-managed schema per
constitution v2.0.0; seeding is an idempotent application-level seeder (no migration tool),
consistent with feature 001 decision D-7.

**Testing**: Java Playwright driving Chromium end-to-end against the running application,
paired with the existing JUnit 5 / MockMvc suites from features 001 and 002. E2E scenarios are
order-independent; a JUnit XML result feeds the CI gate.

**Target Platform**: JVM on developer workstations and CI runners that can host a headless
Chromium browser; application served over HTTP on localhost.

**Project Type**: Web application (server-rendered admin console) — this feature adds
seed/test-support code to the single Spring Boot Maven project.

**Performance Goals**: Full E2E suite completes in under 10 minutes (SC-004); seed convergence
is effectively instantaneous (target < 5s) and idempotent across restarts (SC-003).

**Constraints**: Deterministic and idempotent seeding; test data confined to test/dev profiles
(FR-004); headless-first with a headed mode for diagnosis; order-independent, retry-free
scenarios; drive the real CoreUI screens (FR-018); capture artifacts on failure; fail fast when
the application under test is unreachable.

**Scale/Scope**: ~3 seed personas over the 5 RBAC entities; ~3 scenario classes covering
sign-in, admin journeys, effective permissions, permission boundaries, and lockout guards
(~15–20 scenarios total).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate (constitution) | Status (pre-design) | Status (post-design) | Evidence |
|---------------------|---------------------|----------------------|----------|
| I. Single system; exactly five core RBAC entities; definitions vs assignments separation | PASS | PASS | Seed populates only the five RBAC entities; personas are data, not new entities (data-model.md) |
| II. Fail-closed; middleware resolution before controller; mutation-safety guardrails | PASS | PASS | E2E asserts allow/deny and lockout guards at the HTTP/UI boundary (contracts/e2e-scenarios.md, SC-002/SC-008) |
| III. Java 17+/Spring Boot/Thymeleaf+CoreUI/H2 via Hibernate ddl-auto/JPA/UUID/Bean Validation | PASS | PASS | Technical Context; screens under test are the vendored CoreUI templates (clarification 2026-09-21); Playwright is test-scope only (research D-3) |
| IV. Dual-sided authorization tests (200/403) + lockout tests mandatory | PASS | PASS | The E2E suite is the dual-sided layer across the assembled app (SC-002, FR-010, FR-011) |
| Domain Model: strict FKs, cascade teardown, lockout rows protected, UTC | PASS | PASS | Seeder reuses the 001 entities and guardrails; it never bypasses service guardrails (research D-1) |
| Security Implementation Standards: deny-by-default, resolved permissions exposed to views, centralized codes | PASS | PASS | Restricted/deactivated personas exercise the enforced boundaries; no magic strings in the seeder (research D-2) |
| Governance: every feature/spec/test plan reviewed against Principles I–IV | PASS | PASS | This plan; no violations requiring the Complexity Tracking table |

No gate violations. Playwright and the E2E harness are test-only additions and do not alter
the product stack.

## Project Structure

### Documentation (this feature)

```text
specs/003-seed-data-e2e-testing/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output — decisions D-1..D-9
├── data-model.md        # Phase 1 output — seed dataset, personas, idempotency
├── quickstart.md        # Phase 1 output — validation/run guide
├── contracts/           # Phase 1 output — seed + E2E suite interface contracts
│   ├── README.md
│   ├── seed-data.md
│   └── e2e-scenarios.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
adm/                                       # root package com.allensandiego.adm
├── pom.xml                                   # Playwright 1.63.0 present; add Failsafe (research D-3/D-7)
├── coreui/                                   # vendored CoreUI templates = screens under test
└── src/
    ├── main/
    │   ├── java/com/allensandiego/adm/
    │   │   └── config/
    │   │       └── TestDataSeeder.java        # profile-gated idempotent seeder (D-1/D-8)
    │   └── resources/
    │       ├── application.properties         # base config (features 001/002)
    │       └── application-test.properties     # seed + E2E defaults (D-8)
    └── test/
        ├── java/com/allensandiego/adm/
        │   └── e2e/
        │       ├── support/                    # Playwright lifecycle, base URL, fixtures
        │       ├── AuthE2ETest.java            # sign-in journeys (US2/US3)
        │       ├── AdminJourneysE2ETest.java   # permission/role/user journeys (US2)
        │       └── PermissionBoundaryE2ETest.java  # allow/deny + lockout guards (US3)
        └── resources/
            └── e2e/                            # seed expectations, scenario data
```

**Structure Decision**: Single Spring Boot Maven project rooted at the repository
(`com.allensandiego.adm`). Test-support code sits beside the production code it seeds: the
profile-gated seeder in `.../config/`, and the Playwright suite under
`src/test/java/.../e2e/` with a small `support/` harness. E2E scenarios are separate classes
per journey family so they remain independently runnable (FR-012). The upstream CoreUI
template stays vendored in `coreui/` and is the markup the suite drives (FR-018).

## Complexity Tracking

> Fill ONLY if Constitution Check has violations that must be justified.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | — | — |
