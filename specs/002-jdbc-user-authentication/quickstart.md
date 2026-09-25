# Quickstart: JDBC User Authentication — Validation Guide

**Date**: 2026-09-26 (refreshed) | Contracts: [contracts/](contracts/README.md) | Data model:
[data-model.md](data-model.md) | Research: [research.md](research.md)

This is a run/validation guide, not an implementation reference. It proves the feature
works end to end.

## Prerequisites

- Java 21 and Maven wrapper (`./mvnw`) in the repo root.
- Feature-001 RBAC model and permission enforcement in place (the five core tables plus the
  `authorization` middleware it defines). Feature 002 rides on that fabric.
- A valid active account with a set password. **Seed accounts arrive with feature 003**
  (`specs/003-seed-data-e2e-testing`); until then, use the automated test fixtures below,
  which register an ACTIVE user with a known BCrypt-encoded password and a deactivated
  user — no manual DB edits.

## Run the automated checks

```bash
./mvnw test
```

Expected: all suites green, including:

- **Login flows** (`security/LoginFlowTests`) — success → 302 to saved request or `/`; new
  session id issued; wrong password / unknown username → `/login?error` with the single
  generic message; empty fields refused.
- **Deactivation refusal** — correct password on an `INACTIVE` account is refused
  (SC-003); a user deactivated mid-session loses access on the next request (SC-006).
- **Throttle** (`security/ThrottleTests`) — 5 failures in the window block subsequent
  attempts for the period; blocked attempts stay generic (SC-007).
- **Auth-event audit** — `SUCCESS`, `FAILURE`, `SIGNOUT` rows recorded with username, UTC
  time, outcome; the repository stores no password material (SC-008).
- **Dual-sided authorization** (`security/AuthorizationTests`) — anonymous → 302 to
  `/login`; authenticated with permission → 200; authenticated without permission → 403
  (constitution Principle IV).

## Manual smoke test

1. Start the app: `./mvnw spring-boot:run`
2. Open `http://localhost:8080/` unauthenticated → you are redirected to the sign-in screen
   (FR-001/FR-007); the page's css/js assets load.
3. With valid credentials: submit → you land on home (or the originally requested page);
   browser shows a new session id (`JSESSIONID` cookie) (FR-008).
4. Open `/login` while signed in → redirected to home (FR-011).
5. Sign out (`POST /logout`) → back to `/login?logout`; re-opening a protected URL returns
   you to the sign-in screen, including via browser history (SC-005).
6. With invalid credentials → single generic "invalid credentials" message; the error text
   never distinguishes "unknown username" from "wrong password" (SC-002).
7. Wrong-password 5× within the window → subsequent attempts are refused while the throttle
   is active, still showing only the generic message (FR-012).
8. `.mvn/`-level H2 files or the console (if enabled) show `auth_event` rows for every
   attempt and sign-out, none containing password data (FR-013).

## Success-to-scenario mapping

| Success criterion | Where verified |
|-------------------|----------------|
| SC-001 (home <10s) | Manual smoke step 3 (UI-level; standard responsiveness) |
| SC-002 (generic refusal) | LoginFlowTests wrong-password/unknown/empty; smoke step 6 |
| SC-003 (deactivated refused) | Deactivation refusal tests |
| SC-004 (no plaintext recoverable) | Hash asserted `{bcrypt}...` at write; audit has no credential field |
| SC-005 (post-signout re-entry → login) | Logout tests; smoke step 5 |
| SC-006 (denied-by-default after sign-in) | Dual-sided AuthorizationTests |
| SC-007 (bounded guessing) | ThrottleTests |
| SC-008 (every event reviewable) | Auth-event audit tests |

## Notes / dependencies

- Seed login data is owned by feature 003; this feature's tests create their own fixtures so
  validation does not block on seeding.
- Contract and model details: see [contracts/authentication.md](contracts/authentication.md)
  (flows, status codes, session policy, throttle numbers) and [data-model.md](data-model.md)
  (the `password_hash` addition and `auth_event` schema).