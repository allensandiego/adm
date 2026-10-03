# Tasks: Dashboard Activity Charts and Metrics

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

This task list breaks down the implementation of Feature 004 into dependency-ordered phases. Every user story is independently testable and delivers an incremental slice of value.

---

## Phase 1: Setup (Data Transfer Objects & Contracts)

**Purpose**: Define the immutable transfer models and view contracts used across the service and web layers.

- [ ] T001 [P] Create `DashboardSummaryMetrics` record in `src/main/java/com/allensandiego/adm/domain/dto/DashboardSummaryMetrics.java` (FR-002, FR-003)
- [ ] T002 [P] Create `DailyActivityPoint` record in `src/main/java/com/allensandiego/adm/domain/dto/DailyActivityPoint.java` (FR-004)
- [ ] T003 [P] Create `RecentAuthActivityItem` record in `src/main/java/com/allensandiego/adm/domain/dto/RecentAuthActivityItem.java` (FR-005, FR-006)
- [ ] T004 [P] Create `DashboardViewModel` in `src/main/java/com/allensandiego/adm/domain/dto/DashboardViewModel.java` (FR-001, FR-007)

---

## Phase 2: Foundational (Repository Aggregations & Query Layer)

**Purpose**: Implement efficient database-level aggregations and query projections needed for dashboard metrics.

- [ ] T005 Implement count methods in `UserRepository` and `RoleRepository` for active user and role totals (`src/main/java/com/allensandiego/adm/repository/`)
- [ ] T006 Add time-bucketed aggregation query method `findDailyActivityCounts(Instant since)` to `AuthEventRepository` in `src/main/java/com/allensandiego/adm/repository/AuthEventRepository.java` (FR-004)
- [ ] T007 Add top-N query method `findTop10ByOrderByOccurredAtDesc()` to `AuthEventRepository` (FR-005)
- [ ] T008 [P] Add repository unit/slice tests verifying the 7-day group-by aggregation and recent activity ordering in `src/test/java/com/allensandiego/adm/repository/AuthEventRepositoryTest.java`

**Checkpoint**: Foundational query layer verified — service and view implementations can proceed.

---

## Phase 3: User Story 1 - System Overview Metrics (Priority: P1) 🎯 MVP

**Goal**: Deliver a functioning dashboard landing page (`/` and `/dashboard`) with summary metric cards showing total users, active users, total roles, and 24-hour authentication totals.

**Independent Test**: Log in as `adminuser`, navigate to `/`, and verify that the 4 summary metric cards render with values matching database counts.

### Tests for User Story 1
- [ ] T009 [P] [US1] Unit test for `DashboardService.getDashboardData()` summary metric aggregation in `src/test/java/com/allensandiego/adm/service/DashboardServiceTest.java`
- [ ] T010 [P] [US1] Controller slice test for `GET /` and `GET /dashboard` verifying model attributes and view name in `src/test/java/com/allensandiego/adm/web/DashboardControllerTest.java`

### Implementation for User Story 1
- [ ] T011 [US1] Implement `DashboardService` and `DashboardServiceImpl` to calculate `DashboardSummaryMetrics` (FR-002, FR-003)
- [ ] T012 [US1] Implement `DashboardController` in `src/main/java/com/allensandiego/adm/web/DashboardController.java` mapping `/` and `/dashboard` to `templates/home.html` (FR-001)
- [ ] T013 [US1] Create or update `src/main/resources/templates/home.html` with CoreUI summary metric card layout (FR-002, FR-003, `contracts/dashboard-ui.md`)

**Checkpoint**: User Story 1 complete! Baseline dashboard MVP is fully operational.

---

## Phase 4: User Story 2 - Visual Login Activity & Security Trends Chart (Priority: P2)

**Goal**: Render an interactive line chart tracking daily successful sign-ins vs failed attempts over the rolling 7-day window.

**Independent Test**: Seed multiple days of authentication events; verify the chart renders two distinct lines (success and failure) with zero-filled counts for empty dates.

### Tests for User Story 2
- [ ] T014 [P] [US2] Unit test for 7-day chronological zero-filling and JSON array serialization in `DashboardServiceTest`

### Implementation for User Story 2
- [ ] T015 [US2] Implement 7-day timeline generation and zero-filling logic in `DashboardServiceImpl` producing serialized JSON labels and counts (FR-004)
- [ ] T016 [US2] Integrate the chart container canvas (`#login-activity-chart`) and vendored Chart.js client script into `src/main/resources/templates/home.html` (FR-004, `contracts/dashboard-ui.md`)
- [ ] T017 [US2] Ensure responsive container styles and zero-count edge-case handling without client-side console errors (FR-009, SC-004)

**Checkpoint**: User Stories 1 and 2 working together. Dashboard now features visual time-series trends.

---

## Phase 5: User Story 3 - Recent User Login Activity Feed (Priority: P3)

**Goal**: Render a table widget displaying the 10 most recent authentication events with timestamps, usernames, outcome badges, and client IPs.

**Independent Test**: Perform login attempts (both valid and invalid usernames); verify that events appear at the top of the recent activity table with accurate status badges and safely escaped text.

### Tests for User Story 3
- [ ] T018 [P] [US3] Unit test in `DashboardServiceTest` verifying mapping of `auth_event` rows to `RecentAuthActivityItem` with badge CSS classes
- [ ] T019 [P] [US3] Security test verifying HTML escaping of malicious username inputs in `DashboardControllerTest` (FR-008, SC-005)

### Implementation for User Story 3
- [ ] T020 [US3] Implement recent activity mapping in `DashboardServiceImpl` with outcome-to-badge resolution (`bg-success`, `bg-danger`, `bg-secondary`) (FR-005, FR-006)
- [ ] T021 [US3] Add the recent authentication activity table widget and empty-state placeholder to `src/main/resources/templates/home.html` (FR-005, FR-009)

**Checkpoint**: User Stories 1, 2, and 3 complete. Real-time audit visibility is active on the dashboard.

---

## Phase 6: User Story 4 - Role-Based Dashboard Widget Visibility & Security Gating (Priority: P4)

**Goal**: Enforce Constitution Principle II by hiding security charts and recent login streams from non-administrative users (e.g. `jdoe`).

**Independent Test**: Log in as `jdoe` (User) and verify that security charts and recent login tables are absent from the DOM; log in as `adminuser` and verify all elements are present.

### Tests for User Story 4
- [ ] T022 [P] [US4] Dual-sided authorization tests in `src/test/java/com/allensandiego/adm/web/DashboardSecurityTest.java`:
  - Anonymous request -> 302 Redirect to `/login`
  - Standard user (`jdoe`) -> 200 OK, `canViewAudit == false`, audit widgets not rendered in HTML
  - Admin user (`adminuser`) -> 200 OK, `canViewAudit == true`, all widgets rendered

### Implementation for User Story 4
- [ ] T023 [US4] Add authority evaluation logic in `DashboardServiceImpl` to conditionally set `canViewAudit` based on user permissions (`audit.read` or admin authority) (FR-007)
- [ ] T024 [US4] Add `th:if="${dashboard.canViewAudit}"` guards in `src/main/resources/templates/home.html` surrounding the chart card and recent activity table (FR-007)

**Checkpoint**: Security boundaries verified. Non-admin users receive a clean overview without sensitive audit leakage.

---

## Phase 7: Polish & E2E Verification

**Purpose**: Perform end-to-end browser testing and verify full build reproducibility.

- [ ] T025 [P] Create Playwright E2E test `src/test/java/com/allensandiego/adm/e2e/DashboardE2EIT.java` testing login as `adminuser`, dashboard load, card counts, and chart canvas visibility
- [ ] T026 Execute `./mvnw clean test-compile` to verify compilation across all modules per Constitution Principle V
- [ ] T027 Execute `./mvnw verify` to validate unit, integration, and E2E test suites against seeded database

---

## Dependencies & Execution Order

- **Phase 1 (Setup)**: Can start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 models.
- **Phase 3 (User Story 1 - MVP)**: Depends on Phase 2 queries.
- **Phase 4 (User Story 2)**: Depends on Phase 3 baseline dashboard.
- **Phase 5 (User Story 3)**: Depends on Phase 3 baseline dashboard (can proceed in parallel with Phase 4).
- **Phase 6 (User Story 4)**: Depends on Phases 3, 4, 5 view components.
- **Phase 7 (Polish & E2E)**: Depends on all prior phases.
