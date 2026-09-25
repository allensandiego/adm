# Data Model: JDBC User Authentication

**Date**: 2026-09-26 (refreshed) | Derived from spec FR-001..FR-014 and constitution v2.0.0; extends the
feature-001 data model ([data-model.md](../../001-rbac-user-management/data-model.md)).

## Conventions

- Primary keys: UUID (v4) on every table.
- Timestamps: stored in UTC.
- Foreign keys: strict. The `users` entity is extended by one column; exactly five core RBAC
  tables remain; one supporting `auth_event` table is added (see Complexity Tracking in
  `plan.md`, mirroring the 001 `audit_event` precedent).
- No credential material beyond the one-way hash is ever persisted; auth-event rows carry no
  password or derived secret.

## Entities

### users (extended — one new column)

Feature 001 defines `users` (id UUID, username unique, display_name, status
`ACTIVE`/`INACTIVE`, version, created_at, updated_at). This feature adds:

| field         | type   | rules                                                                 |
|---------------|--------|-----------------------------------------------------------------------|
| password_hash | string | nullable; `{id}encoded` delegation format, e.g. `{bcrypt}$2a$10$...`; strong one-way hash only (FR-005). `NULL` → sign-in is refused (unverifiable credential) without crashing or disclosure (FR-004/FR-014). Stored hash must not be a plaintext password, and must never be written to logs or events. |

- The hashing scheme is identified by the `{id}` prefix on the stored value (D-2); lazy
  re-hash to a stronger scheme happens on the next successful sign-in (spec assumption)
  through `UserDetailsPasswordService`.
- State transitions `ACTIVE <-> INACTIVE` (001) become security-relevant: `INACTIVE` maps to
  the JDBC `enabled=false` flag, so sign-in is refused even with a correct password
  (FR-004), and mid-session deactivation strips effective access on the next request via
  the per-request authorization check (D-4).

### auth_event (supporting — new)

Append-only security-review stream for FR-013. Not an RBAC entity; listed explicitly for
transparency, as with 001's `audit_event`.

| field       | type      | rules                                                                 |
|-------------|-----------|-----------------------------------------------------------------------|
| id          | UUID      | PK (v4)                                                               |
| username    | string    | the submitted account identifier (trimmed; as entered — stored lower for matches); may reference an unknown account on failure (D-6) |
| account_id  | UUID      | FK -> users, nullable (unknown account on failed attempt)             |
| outcome     | enum      | `SUCCESS` / `FAILURE` / `SIGNOUT`                                     |
| occurred_at | timestamp | UTC, set once                                                         |

- Every successful sign-in, failed sign-in, and sign-out is recorded with account
  identifier, outcome, and time (FR-013/SC-008). Throttled-but-blocked attempts are
  recorded as `FAILURE` (the attempt was refused).
- No password, password hash, or password-derived value appears in any column or index.

## Security-relevant read model (JDBC leg)

`JdbcUserDetailsManager` reads identity from `users` via custom queries (research D-1,
contracts/authentication.md):

- users query (columns in order): `(username, password_hash, enabled)` where
  `enabled = (status='ACTIVE')`; matched with `LOWER(username) = LOWER(?)` (D-7).
- authorities query (informational only — never used for authorization, D-4):
  role names via `user_roles` -> `roles`, same case-insensitive match.

## Throttle state (in-memory, D-3)

Not persisted. `LoginAttemptRegistry` keeps per-normalized-username `AttemptWindow` entries
(first-attempt timestamp, failure count) evicted after the window; block = 5 failures within
15 minutes → 15-minute refusal window. Keyed by the submitted (normalized) username for known
and unknown accounts alike, so the block never discloses account existence (FR-003/FR-012).

## Authorization resolution (unchanged from 001, now gated by authentication)

- Sign-in establishes identity only (FR-010): JDBC authenticates (username + password +
  enabled). Authorization remains the per-request union
  `permissions_of(user) = union of p where u_roles(role) and role_permissions(p, role) and p.active`,
  computed in the middleware `AuthorizationManager` before controller dispatch (constitution
  Principle II; research D-4).
- Rules do not change from 001: deactivated users → empty effective set; inactive
  permissions drop out; guardrails G1/G2/G3 and optimistic locking stay as-is.

## Validation summary

- No new mutation contract in this feature: the only writes are (a) `password_hash` set on
  account creation/update by the 001 user-edit flow (Jakarta Bean Validation + service-layer
  uniqueness already apply there) and (b) append-only `auth_event` rows written by the
  security handlers (no inbound user payload).
- `users.password_hash` is validated at write time as a well-formed `{id}...` string of the
  registered encoders; a malformed or missing prefix is treated as unverifiable (login
  refused) rather than a storage error (FR-014).

## Concurrency

- Session concurrency policy (D-5): concurrent sessions for one account are allowed and
  remain independent; the feature enforces no maximum and invalidates no earlier session.
- Differs from 001's row-level optimistic locking (unchanged); auth events are append-only
  and require no locking.