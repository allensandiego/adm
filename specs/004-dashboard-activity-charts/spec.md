# Feature Specification: Dashboard Activity Charts and Metrics

**Feature Branch**: `004-dashboard-activity-charts`

**Created**: 2026-10-03

**Status**: Draft

**Input**: User description: "A dashboard page to show charts and user logins activities"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - System Overview Metrics (Priority: P1)

An authenticated administrator or authorized operator logs into the application and views the dashboard landing page (`/` or `/dashboard`). They immediately see high-level summary cards showing core system statistics: total users, active users, total defined roles, and authentication attempt counts over the last 24 hours (successful logins and failed attempts).

**Why this priority**: Delivers the primary value of a dashboard home screen: immediate visibility into key system volume and operational health without having to query database tables or individual entity screens.

**Independent Test**: Can be tested independently by logging in as an administrator, navigating to `/`, and asserting that summary cards display accurate counts matching the seeded database state.

**Acceptance Scenarios**:

1. **Given** an authenticated user with dashboard view access, **When** they navigate to `/`, **Then** the dashboard overview displays summary cards for Total Users, Active Users, Total Roles, and 24-Hour Authentication Attempts.
2. **Given** known seeded data (3 users, 2 active users, 2 roles), **When** the dashboard loads, **Then** the card values precisely reflect the persisted database totals.
3. **Given** an unauthenticated visitor, **When** they attempt to load the dashboard, **Then** they are redirected to the sign-in page.

---

### User Story 2 - Visual Login Activity & Security Trends Chart (Priority: P2)

An administrator views an interactive visual chart on the dashboard tracking authentication activity over a rolling time window (e.g., daily breakdown across the last 7 days). The chart visually contrasts successful sign-ins against failed sign-in attempts, enabling operators to spot unusual authentication spikes, brute-force attempts, or traffic trends at a glance.

**Why this priority**: Visualizing time-series activity allows immediate anomaly detection that raw numbers and tables cannot convey.

**Independent Test**: Can be tested independently by seeding authentication events across multiple days, navigating to the dashboard, and confirming the chart canvas renders with distinct series for successful and failed authentication attempts.

**Acceptance Scenarios**:

1. **Given** authenticated attempts recorded over the past 7 days, **When** the dashboard renders, **Then** a time-series chart presents daily data points for both successful logins and failed attempts.
2. **Given** days within the window with zero login events, **When** the chart renders, **Then** those dates display a count of zero rather than omitting dates or breaking the timeline.
3. **Given** a responsive viewport (desktop or tablet), **When** the page size changes, **Then** the chart adjusts dynamically to fit within its container card without horizontal scrolling.

---

### User Story 3 - Recent User Login Activity Feed (Priority: P3)

An administrator reviews the most recent authentication events in a dedicated dashboard table widget. The widget lists the most recent authentication activities (e.g., top 10 events) showing the timestamp, attempted username/identity, event outcome (`AUTH_SUCCESS`, `AUTH_FAILURE`, `AUTH_SIGNOUT`), and remote client IP address, formatted with visual status badges.

**Why this priority**: Provides immediate operational context for recent logins, allowing administrators to verify user activity and investigate failed attempts in real time.

**Independent Test**: Can be tested independently by triggering a failed login and a successful login, then inspecting the recent activity widget on the dashboard to verify that both events appear with their respective status badges, timestamps, and client IP addresses.

**Acceptance Scenarios**:

1. **Given** recorded authentication attempts in the system, **When** an administrator views the dashboard, **Then** the recent activity widget displays the most recent events in reverse chronological order (newest first).
2. **Given** a failed login attempt with an unrecognized username, **When** reviewing the recent activity feed, **Then** the attempted username is safely displayed with an `AUTH_FAILURE` badge and the client IP address.
3. **Given** a user sign-out event, **When** the activity feed is refreshed, **Then** the event is displayed with an `AUTH_SIGNOUT` indicator.

---

### User Story 4 - Role-Based Dashboard Widget Visibility & Security Gating (Priority: P4)

A standard non-administrative user (e.g., `jdoe` with standard user privileges) logs into the application and reaches the dashboard. The system displays a personalized welcome card and non-sensitive overview information, while hiding sensitive system-wide security charts and login audit feeds that require administrative audit privileges.

**Why this priority**: Enforces Principle II (Security & Authorization Fail-Closed), ensuring non-administrative personnel cannot observe system-wide authentication activity or sensitive IP addresses.

**Independent Test**: Can be tested independently by logging in as `jdoe` (User) and verifying that security activity charts and recent login event feeds are not rendered in the DOM, whereas logging in as `adminuser` (Admin) renders all widgets.

**Acceptance Scenarios**:

1. **Given** a user with administrator/audit privileges (`adminuser`), **When** they view the dashboard, **Then** all widgets (metrics cards, activity chart, and recent login activity table) are visible.
2. **Given** a standard user lacking audit permissions (`jdoe`), **When** they view the dashboard, **Then** sensitive authentication trend charts and the recent login activity feed are not rendered.
3. **Given** an unauthorized direct request to any dashboard metrics API endpoint, **When** called by an unprivileged user, **Then** the server responds with HTTP 403 Forbidden.

---

### Edge Cases

- **Zero Activity State**: When no authentication events exist (e.g., brand-new deployment), the chart displays an empty-state baseline with zero counts across the timeline, and the recent activity widget displays an informative "No recent login activity recorded" message rather than broken UI elements or script errors.
- **Malicious Username Input**: When an authentication failure occurs with input containing HTML/script tags (e.g., `<script>alert(1)</script>`), the username is strictly sanitized and escaped when rendered in the recent activity feed to prevent cross-site scripting (XSS).
- **Unknown Username Failures**: When a login failure occurs with an unknown account, the activity feed gracefully handles the null user association while safely displaying the submitted identifier string.
- **Timezone and Clock Consistency**: All event timestamps are stored and aggregated in UTC, and displayed on the dashboard with clear timezone context (or localized to the user's browser/session) to prevent date-boundary aggregation discrepancies.
- **High-Volume Event Stream**: When thousands of authentication events exist, the dashboard metrics and chart aggregation use bounded time-window SQL aggregation queries (`BETWEEN ... AND ...` with `GROUP BY`), and the recent activity list is capped at a fixed limit (e.g., 10 entries) to prevent database overload and memory pressure.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a dashboard view accessible to authenticated users upon landing on `/` or `/dashboard`.
- **FR-002**: System MUST aggregate and display core entity summary metrics on the dashboard, including total user count, active user count, and total role count.
- **FR-003**: System MUST calculate aggregate authentication activity metrics over a rolling 24-hour window, showing total successful sign-ins and total failed sign-in attempts.
- **FR-004**: System MUST render an interactive time-series chart showing daily authentication attempt counts over a 7-day rolling window, visually distinguishing successful sign-ins from failed attempts.
- **FR-005**: System MUST provide a recent authentication activity widget displaying the 10 most recent authentication events in reverse chronological order, including timestamp, attempted username, event outcome status, and remote client IP address.
- **FR-006**: System MUST format event outcomes with distinct visual status indicators (e.g., success badge for `AUTH_SUCCESS`, danger badge for `AUTH_FAILURE`, secondary/info badge for `AUTH_SIGNOUT`).
- **FR-007**: System MUST enforce authorization checks such that sensitive login activity charts and recent authentication audit feeds are only visible and accessible to users holding appropriate administrative/audit permissions.
- **FR-008**: System MUST escape and sanitize all rendered data fields in the activity feed, ensuring special characters and potential script payloads in attempted usernames are safely displayed.
- **FR-009**: System MUST handle empty or missing event datasets gracefully, displaying clear empty states without throwing client-side JavaScript or server-side template errors.

### Key Entities *(include if feature involves data)*

- **Authentication Event (`auth_event`)**: Persisted record of an authentication outcome, including UUID primary key, timestamp (UTC), attempted username, outcome status (`AUTH_SUCCESS`, `AUTH_FAILURE`, `AUTH_SIGNOUT`), failure reason, and client IP address.
- **Dashboard Metric Summary**: Aggregated transfer object containing counts for total users, active users, total roles, 24-hour success count, and 24-hour failure count.
- **Daily Activity Point**: Time-bucketed aggregation item containing date, count of successful sign-ins, and count of failed attempts for chart rendering.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The dashboard page loads and renders all widgets and charts in under 1.0 second on a standard environment with up to 100,000 historical authentication events.
- **SC-002**: 100% of recorded authentication events within the reporting time window are accurately reflected in the summary counts and chart data series.
- **SC-003**: Security boundaries are verified: unauthenticated visitors are redirected (302 -> /login), and unauthorized users receive 403 Forbidden on metrics endpoints or see a sanitized non-admin dashboard view.
- **SC-004**: Chart component renders cleanly across common desktop and tablet viewport widths (>= 768px) with zero layout shifting or horizontal scroll overflow.
- **SC-005**: Security tests verify that zero unsanitized script payloads can be executed via the activity feed from malformed login attempts.

---

## Assumptions

- The application uses CoreUI template components (`coreui/charts.html`, `coreui/widgets.html`, `coreui/index.html`) and Chart.js for chart visualization.
- The underlying `auth_event` table created in Feature 002 is the source of truth for all authentication events.
- The default time window for the activity trend chart is 7 days, and the default limit for the recent activity widget is 10 events.
- Core domain entity counts are derived from the existing five RBAC tables defined in the application Constitution (`users`, `roles`, etc.).
