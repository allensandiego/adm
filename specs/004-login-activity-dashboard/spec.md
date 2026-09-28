# Feature Specification: Login Activity Dashboard

**Feature Branch**: `004-login-activity-dashboard`

**Created**: 2026-09-27

**Status**: Draft

**Input**: User description: "Login activity dashboard to track and display user login attempts, successful logins, failed logins, and related audit information."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Login Activity Log (Priority: P1)

An administrator can view a paginated table of recent login activities showing username, IP address, timestamp, status (success/failure), and reason for failure (if applicable). The log is filterable by date range, user, and status.

**Why this priority**: Provides essential audit trail visibility required by Principle II (Security & Authorization) and governance requirements for tracking authentication events.

**Independent Test**: Can be fully tested by navigating to the login activity page as an admin with `VIEW_LOGIN_ACTIVITY` permission and verifying the table displays correctly with filters working. Delivers immediate value through compliance and security monitoring.

**Acceptance Scenarios**:

1. **Given** a user has performed multiple login attempts (success and failure), **When** the administrator views the login activity dashboard, **Then** all login events are displayed in a paginated table sorted by most recent first
2. **Given** the administrator applies a date range filter, **When** they submit the filter, **Then** only login events within the selected date range are shown
3. **Given** the administrator filters by status "Failed", **When** they view results, **Then** only failed login attempts are displayed with their failure reasons

---

### User Story 2 - Monitor Suspicious Login Patterns (Priority: P2)

An administrator can identify potentially suspicious login activity by viewing aggregated statistics including failed login counts per user, top IP addresses with failures, and time-based patterns. The system highlights accounts with excessive failed attempts.

**Why this priority**: Enables proactive security monitoring and threat detection without requiring external SIEM tools. Supports Principle II's requirement for comprehensive audit trails.

**Independent Test**: Can be tested by generating multiple failed login attempts from different IPs and verifying the dashboard aggregates and flags suspicious patterns correctly. Delivers value through early threat detection.

**Acceptance Scenarios**:

1. **Given** a user has more than 5 failed login attempts within 10 minutes, **When** the administrator views the dashboard summary, **Then** that account is flagged with a warning indicator
2. **Given** multiple failed logins from the same IP address, **When** the administrator views aggregated statistics, **Then** the IP is highlighted as potentially suspicious

---

### User Story 3 - Export Login Activity Report (Priority: P3)

An authorized administrator can export login activity data to CSV format for external analysis or compliance reporting. The export respects the same permission boundaries and filters applied in the dashboard view.

**Why this priority**: Supports regulatory compliance requirements and enables offline security analysis. Lower priority because it's a convenience feature that doesn't block core audit functionality.

**Independent Test**: Can be tested by applying filters on the dashboard, clicking export, and verifying the CSV file contains only the filtered data with correct formatting. Delivers value through compliance reporting capabilities.

**Acceptance Scenarios**:

1. **Given** the administrator has applied specific filters to the login activity view, **When** they click the export button, **Then** a CSV file is downloaded containing only the filtered records
2. **Given** an unauthorized user attempts to access the export endpoint, **When** they make the request, **Then** they receive 403 Forbidden

---

### Edge Cases

- What happens when login activity data exceeds pagination limits (e.g., >10,000 records)? System paginates with server-side filtering and enforces maximum date range of 90 days per query.
- How does system handle concurrent access to the dashboard during high authentication volume? Dashboard queries use read replicas or cached aggregates where available; stale data up to 5 minutes is acceptable for audit purposes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST persist every login attempt (success and failure) with timestamp (UTC), username, IP address, user agent (truncated to 256 chars), and failure reason if applicable
- **FR-002**: System MUST display login activities in a paginated table with columns: timestamp, username, IP address, status, failure reason
- **FR-003**: Users MUST be able to filter login activity by date range (max 90 days), username, and status (success/failure)
- **FR-004**: System MUST aggregate and display summary statistics: total attempts, success rate, failed attempts per user, top suspicious IPs
- **FR-005**: System MUST flag accounts with more than 5 failed attempts within a 10-minute window as potentially compromised
- **FR-006**: Authorized users MUST be able to export filtered login activity data to CSV format
- **FR-007**: Login activity records MUST be retained for a minimum of 90 days; older records may be archived or purged per retention policy

### Key Entities *(include if feature involves data)*

- **LoginEvent**: Represents a single authentication attempt with attributes: UUID (primary key), timestamp (UTC), username, IP address, user agent, status (SUCCESS/FAILURE), failure reason, associated userId (foreign key to users table)
- **LoginAggregates**: Cached summary of login patterns including failed count per user within time windows, suspicious IP counts, and daily success/failure ratios

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administrators can view the most recent 1,000 login events within 2 seconds of page load
- **SC-002**: System accurately flags accounts with >5 failed attempts in 10 minutes within 1 minute of the threshold being crossed
- **SC-003**: 95% of login events are persisted within 1 second of authentication attempt completion
- **SC-004**: Export functionality generates CSV files under 10MB for queries covering up to 30 days of activity

## Assumptions

- Administrators with `VIEW_LOGIN_ACTIVITY` permission can access the dashboard; this permission is granted by default to Super Admin role
- Login events are logged after authentication attempts reach the persistence layer, not before password verification
- User agent strings are truncated and sanitized before storage to prevent log injection attacks
- IP address logging complies with applicable data privacy regulations in deployment jurisdictions
