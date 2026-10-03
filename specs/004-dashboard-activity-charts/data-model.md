# Data Model: Dashboard Activity Charts and Metrics

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

## Relational Schema & Entity Boundaries

In accordance with Constitution Principle I, this feature **introduces no new database entities or schema tables**. All dashboard metrics, charts, and activity feeds are computed as read-only projections over existing schema:
1. `users` (core table from Feature 001)
2. `roles` (core table from Feature 001)
3. `auth_event` (supporting table from Feature 002)

### Existing Relational Schema Referenced

```sql
-- Core users table (Feature 001)
users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Core roles table (Feature 001)
roles (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Supporting authentication audit table (Feature 002)
auth_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    username VARCHAR(100) NOT NULL,
    account_id UUID REFERENCES users(id) ON DELETE SET NULL,
    outcome VARCHAR(32) NOT NULL, -- AUTH_SUCCESS, AUTH_FAILURE, AUTH_SIGNOUT
    failure_reason VARCHAR(64),
    remote_ip VARCHAR(64) NOT NULL
);

-- Supporting indexes utilized
CREATE INDEX IF NOT EXISTS idx_auth_event_occurred_at ON auth_event (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_auth_event_outcome_occurred ON auth_event (outcome, occurred_at);
```

---

## Data Transfer Objects & Projections (Java)

### 1. `DashboardSummaryMetrics`
Aggregate counts displayed on the top summary widget cards:

| Field | Type | Description |
|---|---|---|
| `totalUsers` | `long` | Total registered accounts in `users` |
| `activeUsers` | `long` | Total accounts in `users` where `is_active = true` |
| `totalRoles` | `long` | Total defined roles in `roles` |
| `recentSuccessLogins` | `long` | Total `AUTH_SUCCESS` events in the past 24 hours |
| `recentFailedAttempts` | `long` | Total `AUTH_FAILURE` events in the past 24 hours |

### 2. `DailyActivityPoint`
Time-series bucket for chart rendering:

| Field | Type | Description |
|---|---|---|
| `date` | `LocalDate` | Calendar date (e.g. `2026-10-03`) |
| `label` | `String` | Formatted display label (e.g. `"Oct 03"`) |
| `successCount` | `long` | Number of successful authentications on this date |
| `failureCount` | `long` | Number of failed authentication attempts on this date |

### 3. `RecentAuthActivityItem`
Recent event item for the dashboard table feed:

| Field | Type | Description |
|---|---|---|
| `id` | `UUID` | Unique event ID |
| `occurredAt` | `Instant` | Event timestamp (UTC) |
| `formattedTimestamp` | `String` | Human-readable relative or ISO formatted string |
| `username` | `String` | Safe string of attempted identity |
| `outcome` | `String` | `AUTH_SUCCESS`, `AUTH_FAILURE`, or `AUTH_SIGNOUT` |
| `badgeClass` | `String` | Visual CSS badge (`badge bg-success`, `badge bg-danger`, `badge bg-secondary`) |
| `remoteIp` | `String` | Client IP address |

### 4. `DashboardViewModel`
Complete view model passed to `templates/home.html`:

| Field | Type | Description |
|---|---|---|
| `summary` | `DashboardSummaryMetrics` | Aggregate counts |
| `chartPoints` | `List<DailyActivityPoint>` | 7-day chronological data points |
| `chartLabelsJson` | `String` | Serialized JSON array of date labels for Chart.js |
| `chartSuccessesJson`| `String` | Serialized JSON array of successful counts |
| `chartFailuresJson` | `String` | Serialized JSON array of failed counts |
| `recentActivities` | `List<RecentAuthActivityItem>`| Top 10 recent authentication events |
| `canViewAudit` | `boolean` | Flag indicating whether the active user has authority to view security widgets |

---

## Aggregation Queries

### Daily Login Trends Query (7 Days)
```sql
SELECT 
    DATE(occurred_at AT TIME ZONE 'UTC') AS activity_date,
    outcome,
    COUNT(*) AS event_count
FROM auth_event
WHERE occurred_at >= :startDate
GROUP BY DATE(occurred_at AT TIME ZONE 'UTC'), outcome
ORDER BY activity_date ASC;
```

### Recent Activity Query
```sql
SELECT id, occurred_at, username, outcome, failure_reason, remote_ip
FROM auth_event
ORDER BY occurred_at DESC
LIMIT 10;
```
