# Interface Contracts: JDBC User Authentication

**Date**: 2026-09-26 (refreshed)

## Contract style

Server-rendered admin web application. The interface the app exposes is HTTP: the sign-in
screen (GET), the processed login submission (POST), the sign-out action (POST), and all
existing authenticated HTML screens from feature 001. Responses are HTML; security failures
are HTTP redirects to the sign-in screen; authorization failures are the 403 page.

## Security conventions (fail-closed, constitution Principle II)

- Authentication is Spring Security JDBC against the application's own H2 database (FR-002/
  FR-006). It establishes **identity only** (FR-010); authorization is never derived from
  the session's baked-in authorities.
- Authorization is enforced per request by the middleware `AuthorizationManager` from the
  feature-001 design, which re-resolves the account's active status and effective permission
  set before any controller runs (research D-4). Controllers never act as the primary gate.
- Deactivated accounts are refused sign-in at login (JDBC `enabled=false`) **and** stripped
  of access mid-session (per-request re-check) — FR-004/SC-003.
- Only whitelisted paths are public: `/login`, `/css/**`, `/js/**`, `/assets/**`, `/error`.
  Everything else requires both an authenticated session and the required permission
  (permission catalog unchanged from [001 contracts](../contracts/README.md)).
- Single, generic error messaging: invalid credentials, a blocked (throttled) attempt, an
  empty username/password, a disabled account, and an unverifiable credential all surface the
  same generic "invalid credentials" message — the UI never reveals which field was wrong,
  whether the account exists, or whether it is disabled (FR-003/FR-004/FR-012/SC-002).
- No passwords or password-derived secrets are ever written to logs, responses, or the
  `auth_event` table (FR-005/FR-013).

## Permission catalog

Unchanged from feature 001 — this feature adds no permissions. The protected "Super Admin"
role continues to carry every catalog code via the 001 seeder.

## Endpoint map (this feature)

See [authentication.md](authentication.md) — `/login` (GET screen, POST submission),
`/logout` (POST), and the home redirect for signed-in visitors.

## Shared failure behavior

| status                     | meaning                          | user sees                                     |
|----------------------------|----------------------------------|-----------------------------------------------|
| 302 → `/login`             | not authenticated (any protected URL) | sign-in screen; original URL preserved for return (FR-007) |
| 302 → `/login?error`       | invalid/throttled/disabled/empty credentials | sign-in screen with the single generic message |
| 302 → `/login?logout`      | sign-out complete                 | sign-in screen                                |
| 200                        | `/login` (anonymous) or home after valid sign-in | sign-in screen / home               |
| 403                        | authenticated but not permitted  | "Not authorized" page; no protected markup    |
| 302 → `/`                  | authenticated user opens `/login` | home (FR-011)                                 |

## UI Layout & Reference Templates

The sign-in screen in `src/main/resources/templates/login.html` is built using CoreUI Free Bootstrap Admin Template v5.5.0:
- Reference Blueprint: [`coreui/authentication/login.html`](file:///home/allen/workspace/adm/coreui/authentication/login.html) — Documents centered layout, input groups, and alert placement.
- Full reference documentation: [`coreui/README.md`](file:///home/allen/workspace/adm/coreui/README.md) and [`src/main/resources/templates/README.md`](file:///home/allen/workspace/adm/src/main/resources/templates/README.md).