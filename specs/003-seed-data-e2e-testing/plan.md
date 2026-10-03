# Implementation Plan: Seed Data & End-to-End Testing

**Branch**: `003-seed-data-e2e-testing` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-seed-data-e2e-testing/spec.md`

**Note**: Plan produced by `/speckit.plan`; the detailed task breakdown is produced later by
`/speckit.tasks` into `tasks.md`.

## Summary

Deliver a deterministic, idempotent seeded dataset initialized from `src/main/resources/data.sql`
(administrator `adminuser`, restricted user `jdoe`, and multi-role user `jsmith` with the
roles/permissions/assignments they need) and a Java Playwright end-to-end suite that drives the real
CoreUI-rendered screens of the running application. The suite proves sign-in behavior, the core
administrative journeys, effective-permission correctness, both sides of every permission boundary, and
the lockout guardrails — running headless with one command (`./mvnw verify`), aligned with Spring Boot
4.x.x+ modular testing infrastructure, and leaving diagnostic artifacts on failure. Design decisions in
[research.md](research.md); seed and scenario contracts in [contracts/](contracts/README.md); test data
model in [data-model.md](data-model.md); run/validation guide in [quickstart.md](quickstart.md).

## Technical Context

**Language/Version**: Java 21 (`pom.xml` `<java.version>21</java.version>`; satisfies constitution Principle III requirement of Java 21 LTS or newer).

**Primary Dependencies**: Spring Boot 4.1.1 (Maven parent) and Spring Security 7.x. For testing under
Spring Boot 4.x modularization, `pom.xml` provides `spring-boot-starter-test`, `spring-security-test`,
and the explicit test modules `spring-boot-test` and `spring-boot-test-autoconfigure`. Browser automation
uses `com.microsoft.playwright:playwright` 1.63.0 (test scope) with JUnit 5 and Maven Failsafe plugin
binding (`*IT` naming) for the E2E verification gate (research D-3, D-7).

**Storage**: PostgreSQL; the base schema and seed data are initialized directly at startup by SQL scripts
(`src/main/resources/drop.sql`, `schema.sql`, `data.sql`) via Spring Boot 4.x SQL initialization
(`spring.sql.init.*`). Hibernate schema generation is disabled per constitution Principle III.

**Testing**: Java Playwright driving Chromium end-to-end against the running application, paired with
JUnit 5 and MockMvc tests from features 001 and 002. E2E scenarios are order-independent; a machine-readable
JUnit XML result feeds the CI gate.

**Target Platform**: JVM on developer workstations and CI runners that can host a headless
Chromium browser; application served over HTTP on localhost.

**Project Type**: Web application (server-rendered admin console) — this feature configures SQL-seeded
test data and adds the Playwright test suite to the Spring Boot Maven project.

**Performance Goals**: Full E2E suite completes in under 10 minutes (SC-004); seed initialization
via `data.sql` executes in < 5s and is idempotent across restarts (SC-003).

**Constraints**: Deterministic and idempotent seeding via `data.sql`; headless-first with headed mode for
diagnosis; order-independent, retry-free scenarios; drive real CoreUI screens (FR-018); capture artifacts
on failure; fail fast when application under test is unreachable; use Maven wrapper `./mvnw` per
constitution Principle V.

**Scale/Scope**: 3 seed users (`adminuser`, `jdoe`, `jsmith`) and 3 roles (`admin`, `user`, `manager`) over
the 5 RBAC entities from `data.sql`; ~3 scenario classes covering sign-in, admin journeys, effective
permissions, permission boundaries, and lockout guards (~15–20 scenarios total).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate (constitution) | Status (pre-design) | Status (post-design) | Evidence |
|---------------------|---------------------|----------------------|----------|
| I. Single system; exactly five core RBAC entities; definitions vs assignments separation | PASS | PASS | Seed in `data.sql` populates only the five RBAC entities; personas are rows, not new entities (data-model.md) |
| II. Fail-closed; middleware resolution before controller; mutation-safety guardrails | PASS | PASS | E2E asserts allow/deny and lockout guards at the HTTP/UI boundary (contracts/e2e-scenarios.md, SC-002/SC-008) |
| III. Java 21+/Spring Boot/Thymeleaf+CoreUI/PostgreSQL/JPA/UUID/Bean Validation | PASS | PASS | Technical Context; screens under test are vendored CoreUI templates; Playwright is test-scope only; Spring Boot 4.1.1 + Java 21 |
| IV. Dual-sided authorization tests (200/403) + lockout tests mandatory | PASS | PASS | E2E suite is the dual-sided layer across the assembled app (SC-002, FR-010, FR-011) |
| Domain Model: strict FKs, cascade teardown, lockout rows protected, UTC | PASS | PASS | Initialized via `schema.sql` and `data.sql`; lockout rows protected by guardrails |
| Security Implementation Standards: deny-by-default, resolved permissions exposed to views, centralized codes | PASS | PASS | Seeded personas exercise enforced boundaries; BCrypt hashes with `{bcrypt}` delegation format in `data.sql` |
| Governance: every feature/spec/test plan reviewed against Principles I–IV | PASS | PASS | This plan; no violations requiring Complexity Tracking table |

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
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
adm/                                       # root package com.allensandiego.adm
├── pom.xml                                   # Spring Boot 4.1.1, modular test starters, Playwright, Failsafe
├── coreui/                                   # vendored CoreUI templates = screens under test
└── src/
    ├── main/
    │   ├── java/com/allensandiego/adm/        # application code
    │   └── resources/
    │       ├── drop.sql                       # table teardown script
    │       ├── schema.sql                     # table creation script (five RBAC tables)
    │       ├── data.sql                       # seeded baseline: adminuser, jdoe, jsmith, roles, mappings
    │       └── application.properties         # base config + spring.sql.init.* datasource initialization
    └── test/
        ├── java/com/allensandiego/adm/
        │   └── e2e/
        │       ├── support/                    # Playwright lifecycle, base URL, fixtures
        │       ├── AuthE2EIT.java              # sign-in journeys (US2/US3)
        │       ├── AdminJourneysE2EIT.java     # permission/role/user journeys (US2)
        │       └── PermissionBoundaryE2EIT.java  # allow/deny + lockout guards (US3)
        └── resources/
            └── e2e/                            # scenario expectations and test configurations
```

**Structure Decision**: Single Spring Boot Maven project rooted at the repository
(`com.allensandiego.adm`). Seed data is maintained declaratively in `src/main/resources/data.sql` and
loaded via standard Spring Boot 4.x SQL initialization. The Playwright suite is placed under
`src/test/java/.../e2e/` with a small `support/` harness. E2E scenarios are separate classes
per journey family so they remain independently runnable (FR-012). The upstream CoreUI
template stays vendored in `coreui/` and is the markup the suite drives (FR-018).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | — | — |

