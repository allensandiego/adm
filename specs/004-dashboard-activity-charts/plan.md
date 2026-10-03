# Implementation Plan: Dashboard Activity Charts and Metrics

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-dashboard-activity-charts/spec.md`

**Note**: Plan produced by `/speckit.plan`; detailed task breakdown is produced later by `/speckit.tasks` into `tasks.md`.

## Summary

Deliver a dashboard overview home screen (`/` and `/dashboard`) built with CoreUI and Thymeleaf. The dashboard provides:
1. High-level metric overview cards (total users, active users, total roles, 24-hour authentication attempts).
2. An interactive 7-day rolling time-series line chart (contrasting successful vs failed logins) using the vendored Chart.js library.
3. A recent authentication activity table feed displaying the top 10 most recent authentication events (`AUTH_SUCCESS`, `AUTH_FAILURE`, `AUTH_SIGNOUT`) with status badges and client IPs.
4. Role-based view filtering that hides security-sensitive charts and audit feeds from unprivileged users (e.g. `jdoe`), adhering to Constitution Principle II (Fail-Closed Security).

The feature queries existing domain entities (`users`, `roles`) and the supporting `auth_event` table via efficient database aggregations, requiring zero new tables or schema migrations.

## Technical Context

**Language/Version**: Java 21 LTS (`pom.xml` targets Java 21)

**Primary Dependencies**: Spring Boot 4.1.x (`spring-boot-starter-parent` 4.1.1), Spring Security 7.x, Spring Data JPA / JDBC, Thymeleaf (`spring-boot-starter-thymeleaf`), CoreUI Bootstrap 5 admin template assets, vendored Chart.js 4.x (`src/main/resources/static/vendors/chart.js/js/chart.umd.js`). Testing via JUnit 5, MockMvc, and Playwright 1.63.0.

**Storage**: PostgreSQL. Read-only aggregations against `users`, `roles`, and `auth_event`. No new schema tables created.

**Testing**: JUnit 5 + MockMvc with Spring Security test context for controller/service aggregation logic and dual-sided authorization tests (anonymous -> 302, standard user `jdoe` -> 200 with gated widgets hidden, admin user `adminuser` -> 200 with full widgets rendered). Playwright E2E verification for chart canvas rendering.

**Target Platform**: Server-rendered admin web application; desktop and tablet web browsers.

**Project Type**: Web application (admin console) — single Spring Boot Maven project.

**Performance Goals**: Sub-second (<1.0s) dashboard page load and chart render under typical database load (SC-001).

**Constraints**: Fail-closed security; authorization checks executed before view rendering; pure server-side HTML escaping of user inputs via Thymeleaf (`th:text`); zero external CDN calls; build and test exclusively via `./mvnw`.

**Scale/Scope**: Displaying top 10 recent events and 7-day daily buckets; queries bounded by time intervals to handle up to 100k historical auth events without degradation.

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate (constitution) | Status (pre-design) | Status (post-design) | Evidence |
|---|---|---|---|
| I. Single-system, exactly five core RBAC entities; definitions vs assignments separation | PASS | PASS | `data-model.md`: Zero new entities or tables added. Pure read-only queries against existing `users`, `roles`, and supporting `auth_event`. |
| II. Fail-closed; middleware resolution before controller; mutation-safety guardrails | PASS | PASS | Protected routes `/` and `/dashboard` require authentication; view model gates sensitive audit feeds behind `audit.read` / admin authority; mutation guardrails unaffected. |
| III. Java 21+ / Spring Boot / Thymeleaf+CoreUI / PostgreSQL / JPA / UUID / Bean Validation | PASS | PASS | Built with Java 21, Spring Boot 4.1.1, Thymeleaf + CoreUI, vendored Chart.js, PostgreSQL read-only projections. |
| IV. Dual-sided authorization tests (200/403) + lockout tests | PASS | PASS | `quickstart.md`: Dual-sided testing covers anonymous redirect (302), standard user 200 with gated security fragments hidden, and admin 200 with full chart/feed rendering. |
| V. Build & Execution: `./mvnw` wrapper non-negotiable | PASS | PASS | All commands and documentation mandate `./mvnw`. |

---

## Project Structure

### Documentation (this feature)

```text
specs/004-dashboard-activity-charts/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Phase 0 decisions (D-1 through D-7)
├── data-model.md        # Phase 1 DTOs, query definitions, and schema mapping
├── quickstart.md        # Phase 1 build, test, and verification guide
├── contracts/           # Phase 1 interface contracts
│   ├── README.md
│   ├── dashboard-metrics.md
│   └── dashboard-ui.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
adm/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/allensandiego/adm/
    │   │   ├── domain/dto/
    │   │   │   ├── DashboardSummaryMetrics.java
    │   │   │   ├── DailyActivityPoint.java
    │   │   │   ├── RecentAuthActivityItem.java
    │   │   │   └── DashboardViewModel.java
    │   │   ├── repository/
    │   │   │   └── AuthEventRepository.java (time-series & recent event queries)
    │   │   ├── service/
    │   │   │   ├── DashboardService.java
    │   │   │   └── impl/DashboardServiceImpl.java
    │   │   └── web/
    │   │       └── DashboardController.java
    │   └── resources/
    │       ├── static/
    │       │   └── js/dashboard.js (optional standalone or inline in home.html)
    │       └── templates/
    │           ├── home.html
    │           └── fragments/
    │               ├── topbar.html
    │               └── sidebar.html
    └── test/
        └── java/com/allensandiego/adm/
            ├── service/DashboardServiceTest.java
            ├── web/DashboardControllerTest.java
            ├── web/DashboardSecurityTest.java
            └── e2e/DashboardE2EIT.java
```

---

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations. All checks pass without exception.*
