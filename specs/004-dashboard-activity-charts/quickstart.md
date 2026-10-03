# Quickstart: Dashboard Activity Charts and Metrics

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

This quickstart guides testing, verifying, and developing the Dashboard feature.

## Prerequisites

- Java 21 LTS (`java -version`)
- Maven wrapper (`./mvnw`)
- Running PostgreSQL database (or test database container)

## 1. Build and Compile

Always use `./mvnw` per Constitution Principle V:

```bash
./mvnw clean test-compile
```

## 2. Execute Tests

### Unit and MVC Slice Tests
Run controller and service unit tests verifying metric aggregations, permission checks, and view models:

```bash
./mvnw test -Dtest="DashboardControllerTest,DashboardServiceTest"
```

### Authorization Boundaries (Dual-Sided Testing)
Verify dual-sided authorization per Constitution Principle IV:
- Anonymous user -> Redirects (302) to `/login`.
- Standard user (`jdoe` / `password123`) -> 200 OK, metric cards visible, chart and recent audit stream hidden (`th:if="${dashboard.canViewAudit}"` evaluated to false).
- Admin user (`adminuser` / `admin123`) -> 200 OK, metric cards, 7-day trend chart, and top 10 activity rows all visible.

```bash
./mvnw test -Dtest="DashboardSecurityTest"
```

### Full E2E Playwright Verification
Run the Playwright E2E test verifying dashboard rendering and chart initialization:

```bash
./mvnw verify -Dit.test="DashboardE2EIT"
```

## 3. Manual Inspection & Verification

1. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Navigate to `http://localhost:8080/`.
3. Sign in as `adminuser` / `admin123`:
   - Inspect top metric cards: Total Users, Active Users, Roles, 24h Sign-ins.
   - Inspect 7-day login activity chart (confirm Chart.js canvas initializes).
   - Inspect recent activity table with status badges (`AUTH_SUCCESS`, `AUTH_FAILURE`).
4. Sign out and sign in as `jdoe` / `password123`:
   - Confirm standard overview renders.
   - Confirm audit chart and recent activity table are not present in the DOM.
