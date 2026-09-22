# Authentication Contract: Sign-In, Sign-Out, Session

**Date**: 2026-09-21 | Part of the feature-002 interface contracts ([README](README.md)).

## Endpoints

| Method | Path      | Auth      | Behavior                                                                                              |
|--------|-----------|-----------|-------------------------------------------------------------------------------------------------------|
| GET    | `/login`  | anonymous | Renders the sign-in screen (CoreUI template). Its assets load without authentication (FR-001).        |
| POST   | `/login`  | anonymous | Spring Security `UsernamePasswordAuthenticationFilter` processes `username`/`password` parameters.    |
| POST   | `/logout` | any       | Ends the session (CSRF-protected); returns to `/login?logout`. (FR-009)                               |
| GET    | `/`       | required  | Home screen; also the default destination after sign-in when no original request was saved.           |

`/login` is whitelisted (public) along with `/css/**`, `/js/**`, `/assets/**`, `/error`; all
other paths are denied to unauthenticated visitors with a redirect to `/login` (FR-001/
FR-007). Views never render protected content behind the login gate.

## Sign-in success flow

1. User submits username (+ case-insensitive, trimmed — D-7) and password.
2. `JdbcUserDetailsManager` loads identity from the application's own `users` table
   (FR-002/FR-006); credentials verified with `DelegatingPasswordEncoder` against
   `password_hash` (FR-005). A lazily upgraded scheme is re-hashed on success (spec
   assumption).
3. Session id is rotated via default session-fixation protection (`changeSessionId()`) — a
   NEW session identifier is issued at sign-in (FR-008).
4. The saved original request (if any) is replayed; otherwise the user lands on `/` (FR-007).
5. `AUTH_SUCCESS` event is recorded (FR-013). The throttle counter for the username is
   cleared (D-3).

## Sign-in refusal flow

Any of the following routes to `302 → /login?error` with one generic message (SC-002):

- wrong password; unknown username; empty username/password (FR-003);
- deactivated account with a correct password (FR-004);
- unverifiable/missing stored credential (FR-004/FR-014);
- throttled account (FR-012).

Every refusal writes an `AUTH_FAILURE` `auth_event` with the submitted identifier
(FR-013/SC-008), and a failure increments the lose-throttle counter. No refusal message
discloses which case occurred (FR-003).

## Sign-out flow

`POST /logout` runs the custom `LogoutHandler` (records `AUTH_SIGNOUT`, FR-013/FR-009), then
the default handler invalidates the session and clears the `SecurityContext`; the user is
returned to `/login?logout`. Any later protected request is redirected to `/login`, including
via browser history (SC-005).

## Session policy

- Session fixation: protected by default (new session id at sign-in).
- Expired/invalidated mid-task: next request is unauthenticated → redirect to `/login`; no
  protected content is leaked (edge case / SC-005).
- Already-authenticated user opens `/login`: redirected to `/` (FR-011).
- Concurrent sign-ins: allowed; sessions are independent; no session is invalidated when the
  same account signs in elsewhere (documented, consistent policy — D-5/edge case).

## Throttle (FR-012 / SC-007)

In-memory, per normalized username, uniform for known and unknown accounts (no existence
disclosure):

- window: 5 failed attempts within 15 minutes;
- on threshold: further attempts refused for 15 minutes (counts as `FAILURE` event);
- success clears the window.

## Test contract (dual-sided, constitution principle IV)

Every boundary in this feature is covered by explicit authorization tests (quickstart.md):

| scenario                                   | expected result                          |
|--------------------------------------------|------------------------------------------|
| anonymous GET on any protected page        | 302 → `/login` (no content served)        |
| GET `/login` anonymous                     | 200, sign-in screen + assets              |
| POST valid active account credentials      | 302 → saved request or `/` (home); new session id |
| POST wrong password / unknown username     | 302 → `/login?error`; generic message     |
| POST deactivated account correct password  | 302 → `/login?error`                      |
| 5× failed attempts in window               | later attempts refused (302 `/login?error`) with `FAILURE` events |
| POST `/logout` while signed in             | 302 → `/login?logout`; protected URLs thereafter → `/login` |
| signed-in GET `/login`                     | 302 → `/`                                 |