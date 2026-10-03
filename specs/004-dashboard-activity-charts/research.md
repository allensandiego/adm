# Phase 0 Research: Dashboard Activity Charts and Metrics

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

## Decisions and Architecture Choices

### D-1: Dashboard Routing and Controller Structure
- **Context**: The application requires a default landing page upon successful authentication (`/` and `/dashboard`).
- **Options**:
  1. Have multiple controllers handle different partial views.
  2. Provide a single dedicated `DashboardController` (`@Controller`) mapping `GET /` and `GET /dashboard` to render `templates/home.html`, with a helper `DashboardService` encapsulating all metric queries.
- **Decision**: Option 2. `DashboardController` provides the authoritative view endpoint. It populates a `DashboardViewModel` containing summary metrics, chart series data, and recent activity items.
- **Rationale**: Keeps controller thin, avoids duplicating queries between `/` and `/dashboard`, and provides clean separation of concerns.

### D-2: Core Metrics Aggregation Strategy
- **Context**: FR-002 requires aggregating core entity counts (Total Users, Active Users, Total Roles) and FR-003 requires 24-hour authentication metrics (successful and failed attempts).
- **Options**:
  1. In-memory counting by loading entire tables via JPA repository `findAll()`.
  2. Direct count queries via existing Spring Data JPA repositories (`count()`, `countByIsActiveTrue()`) and JDBC template queries on `auth_event`.
- **Decision**: Option 2. Execute lightweight `SELECT COUNT(*)` queries.
- **Rationale**: Constant-memory execution ($O(1)$ heap overhead), sub-millisecond execution over PostgreSQL indexes, and prevents out-of-memory errors as user and auth tables scale.

### D-3: Time-Series Authentication Statistics (Rolling 7 Days)
- **Context**: FR-004 requires rendering an interactive time-series chart showing daily authentication attempt counts over a 7-day rolling window, distinguishing between successes and failures.
- **Options**:
  1. Fetch individual `auth_event` rows for the past 7 days and group them in Java code.
  2. Execute a single SQL `GROUP BY date_trunc('day', occurred_at), outcome` query on `auth_event` over the range `[NOW() - INTERVAL '7 days', NOW()]`, then map missing dates to 0 counts in Java.
- **Decision**: Option 2. Database-level aggregation with zero-filling in `DashboardService`.
- **Rationale**: If thousands of login attempts occur over 7 days, database grouping returns at most ~14 rows (7 days $\times$ 2 outcomes), preserving minimal network transfer and fast sub-second dashboard rendering (SC-001).

### D-4: Chart Visualization Architecture
- **Context**: Spec requires rendering interactive line/bar charts compatible with the CoreUI template (`coreui/charts.html`, `coreui/index.html`).
- **Options**:
  1. Server-side SVG rendering.
  2. Client-side Chart.js (already vendored in `static/vendors/chart.js/js/chart.umd.js` and CoreUI utilities) consuming structured JSON data injected by Thymeleaf via a data attribute or inline script block.
- **Decision**: Option 2. Use the vendored Chart.js library with responsive options (`responsive: true, maintainAspectRatio: false`). Pass dates, success series, and failure series serialized into standard JSON arrays via Thymeleaf model attributes (`th:data-labels`, `th:data-successes`, `th:data-failures`).
- **Rationale**: Leverages existing, tested CoreUI assets without adding external CDN dependencies, complies with Principle III, and guarantees responsive mobile/desktop display without horizontal scroll.

### D-5: Recent Login Activity Stream & XSS Mitigation
- **Context**: FR-005 and FR-008 require displaying the 10 most recent authentication events in reverse chronological order, with safe rendering of arbitrary user inputs (e.g. invalid attempted usernames).
- **Options**:
  1. Raw unescaped HTML strings.
  2. Standard Thymeleaf text escaping (`th:text="${item.username}"`) with bounded SQL limit (`SELECT ... FROM auth_event ORDER BY occurred_at DESC LIMIT 10`).
- **Decision**: Option 2. Query top 10 rows using indexed order (`occurred_at DESC`). Render all user-supplied fields using Thymeleaf `th:text` which automatically HTML-escapes content, preventing stored/reflected XSS.
- **Rationale**: Ensures security against injection payloads from failed login attempts while keeping rendering fast and reliable.

### D-6: Role-Based Dashboard Widget Visibility & Security Gating
- **Context**: FR-007 requires that sensitive security feeds and trend charts are hidden from unprivileged users (e.g., standard user `jdoe`).
- **Options**:
  1. Separate dashboard endpoints for admins and users.
  2. Single `/` endpoint where the backend computes a boolean authority flag (`canViewAudit`) based on the active user's permissions, and Thymeleaf conditionally renders audit/chart fragments using `th:if="${canViewAudit}"`. Direct REST/metric endpoints enforce `@PreAuthorize("hasAuthority('audit.read')")` or Spring Security's `AuthorizationManager`.
- **Decision**: Option 2. Single unified dashboard view with conditional fragment rendering based on explicit permissions (`audit.read` / `user.read`). Unprivileged users see a clean overview / welcome widget without leaking security data.
- **Rationale**: Aligns with Principle II (Fail-Closed Security). View-layer filtering provides clean UX while backend authorization ensures data is never leaked to unauthorized sessions.

### D-7: Database Impact & Constitution Principle I Compliance
- **Context**: The Constitution Principle I requires single-system architecture with exactly five core domain entities (`users`, `roles`, `permissions`, `role_permissions`, `user_roles`).
- **Finding**: This feature introduces **zero new database tables or entities**. All metrics read from the five core tables and the supporting `auth_event` table established in Feature 002.
- **Index Check**: Ensure `auth_event(occurred_at DESC)` and `auth_event(occurred_at, outcome)` are supported by indexes for high-speed time-bucket queries.
