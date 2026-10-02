# Research: JDBC User Authentication

**Date**: 2026-09-26 (refreshed) | For feature `002-jdbc-user-authentication` | Output of Phase 0.

## Open questions addressed

The feature spec mandated "use Spring Security JDBC" while the project's constitution
mandates an existing five-entity RBAC/Hibernate/PostgreSQL model. Research resolved: (1) how JDBC
authentication fits a non-default JPA-managed schema with UUID keys, (2) how to keep
per-request permission freshness (constitution Principle II) while authenticating via JDBC,
(3) the correct hashing + throttle + audit approaches for Spring Security 7 / Boot 4.x.

## D-1 JDBC authentication leg — `JdbcUserDetailsManager` with custom queries

- **Decision**: Configure Spring Security JDBC authentication with
  `JdbcUserDetailsManager` (a `UserDetailsService`) backed by the existing PostgreSQL `DataSource`,
  using custom SQL against the application's own tables. Publish a `UserDetailsService`
  bean so Boot/Security auto-config uses it. `SecurityFilterChain` bean uses the lambda DSL
  (`authorizeHttpRequests`, `formLogin`, `logout`, `sessionManagement`); `.and()` and
  adapter classes are gone in Security 7.
- **Rationale**: Satisfies the mandated mechanism ("Spring Security JDBC") and FR-002/FR-006
  (own database only). `JdbcUserDetailsManager` needs only a `DataSource`; the JDBC leg
  performs no schema creation, so the SQL-managed schema (constitution III) is
  untouched.
- **Details**:
  - `usersByUsernameQuery` must return columns in order `(username, password, enabled)`:
    `SELECT u.username, u.password_hash, CASE WHEN u.status='ACTIVE' THEN true ELSE false END FROM users u WHERE u.username = ?`. Use
    `LOWER(u.username) = LOWER(?)` for case-insensitivity (D-7).
  - `setUsernameBasedPrimaryKey(false)` — the `users` table uses UUID primary keys, not the
    username.
  - `authoritiesByUsernameQuery` returns role names from `user_roles` -> `roles`
    (`SELECT u.username, r.name FROM users u JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id WHERE LOWER(u.username)=LOWER(?)`).
    These authorities are never used for authorization (D-4); they only satisfy the
    `UserDetails` contract and serve as an informational identity snapshot.
- **Alternatives considered**:
  - Spring's default JDBC schema (`users`/`authorities` with string PKs): rejected — would
    force non-UUID PKs and a parallel identity store, violating constitution Domain Model.
  - Rolling a plain JPA `UserDetailsService`: rejected — bypasses the requester's mandated
    "Spring Security JDBC" mechanism.

References: Spring Security 7.x Reference — `JdbcUserDetailsManager`/`JdbcDaoImpl`
(javadoc: `setUsernameBasedPrimaryKey`, column-portability), passwords/jdbc, password-storage,
whats-new (`.and()`/`authorizeRequests` removed, `PathPatternRequestMatcher`).

## D-2 Password storage — `DelegatingPasswordEncoder`, BCrypt default, lazy upgrade

- **Decision**: Use `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (the
  Security 7 default) as the single `PasswordEncoder` bean. Credentials are persisted in a
  single nullable `users.password_hash` column in the `{id}encoded` delegation format (e.g.
  `{bcrypt}$2a$10$...`). Wire the encoder into `DaoAuthenticationProvider` together with
  `setUserDetailsPasswordService(...)` so a successful login with an out-of-date scheme/work
  factor lazily re-hashes the stored credential (spec assumption).
- **Rationale**: BCrypt (cost 10) is Spring Security's default and OWASP-acceptable for web
  credentials; the delegating encoder keeps the scheme id on the stored string, enabling
  future upgrades (e.g. Argon2id) without schema changes and satisfying FR-005 (strong
  one-way hash, no plaintext ever). Argon2id is OWASP's headline recommendation for new
  systems, but Spring's default remains BCrypt; the delegation format keeps Argon2id as a
  drop-in future option. Java's BCrypt 72-byte cap is irrelevant here (no length cap issue
  for typical passwords).
- **Handling unverifiable credentials** (edge case + FR-004): accounts without a usable
  stored hash — store a fixed dummy `{bcrypt}`-prefixed hash of random bytes at account
  creation time; a null/empty stored hash would both blow up `DelegatingPasswordEncoder`
  (unmapped-id encoder) and create a timing side channel revealing the missing credential.
  With the dummy present, the submitted password simply fails `matches`, throwing the generic
  `BadCredentialsException`.
- **Alternatives considered**:
  - `Argon2PasswordEncoder` (16 MiB/2 iters/1 lane): marginally stronger but non-default;
    tuned parameters must be pinned; kept out of the initial scope and noted as an upgrade
    step (the `{id}` delegation format makes it a one-line change).
  - Plain `BCryptPasswordEncoder` only: rejected — no scheme metadata, blocks future upgrades
    and the lazy-rehash path.

References: Spring Security 7 `password-storage.html`; OWASP Password Storage Cheat Sheet
(Argon2id min m=19456 KiB, t=2, p=1; bcrypt legacy cost ≥10); Spring Security source
(`AbstractValidatingPasswordEncoder.matches` returns false for null/empty stored hash →
`BadCredentialsException`).

## D-3 Failed-attempt throttling — in-memory `LoginAttemptRegistry`

- **Decision**: Implement a small in-memory `LoginAttemptRegistry`
  (`ConcurrentHashMap<String, AttemptWindow>` with first-attempt timestamp + fail count,
  evicted after the window) guarding a custom `AuthenticationProvider` that wraps
  `DaoAuthenticationProvider`. Policy: block further attempts for 15 minutes once 5 failures
  are observed for a submitted username within a 15-minute window; the counter is cleared on
  successful login; the block is applied uniformly to known and unknown usernames, checked
  **before** the credential lookup so account existence is never revealed.
- **Rationale**: Spring Security core has no first-class brute-force protection
  (`ConcurrentSessionControlAuthenticationStrategy`/`SessionRegistry` concern concurrent
  sessions only). A ~40-line in-memory registry is the standard, idiomatic choice for a
  single-process application (FR-012, SC-007): bounded, deterministic, requires no additional
  service, and it does not confirm whether an account exists (the failure and the block are
  indistinguishable from wrong credentials).
- **Mechanics**: block-before-increment (failures are recorded after a failed `matches`, but
  the pre-check counts the threshold correctly without leaking a threshold+1 borrowing);
  check keyed by the **submitted, normalized username**, not IP (per-account semantics, per
  OWASP; IP counters are spoofable/NAT-prone). Success clears the window.
- **Alternatives considered**:
  - IP-keyed rate limiting (Bucket4j or filter): generic endpoint rate limiting, not account
    lockout, and IP-keying leaks/exposes nothing but is easy to bypass; rejected as the
    primary mechanism.
  - DB-persisted failure counters on `users`: durable but writes on every attempt, and lock
    state keyed by account makes enumeration possible; rejected.
  - No throttle: rejected — fails FR-012/SC-007.

References: OWASP Authentication Cheat Sheet (account lockout: 3–5 in-window attempts,
bounded duration; count on the account, not IP).

## D-4 Authorization — JDBC proves identity; the `AuthorizationManager` gates everything

- **Decision**: Keep the feature-001 architecture: a custom `AuthorizationManager<
  RequestAuthorizationContext>` (registered via `authorizeHttpRequests(...).anyRequest().access(...)`)
  is the only authorized-enforcement gate. On every request it re-resolves the account's
  active status and effective permission union from the DB
  (`user_roles` -> `roles` -> `role_permissions` -> `permissions where active`) and returns
  `AuthorizationDecision` based on the URL's required permission, populating a request
  attribute with the resolved set for the view layer. Enforcement never reads the session
  authorities baked in at login.
- **Rationale**: Satisfies constitution Principle II ("resolved before controller dispatch",
  "computed in the middleware") and FR-004/SC-006 (deactivation or a revoked permission takes
  effect on the next request, not next login). JDBC authentication's role is identity +
  status verification only.
- **Details / cautions**: do not use `hasAuthority`/`@PreAuthorize("hasRole(..)")` (they read
  stale session authorities) nor Thymeleaf `sec:authorize` for gating; render from the request
  attribute the manager populates. A short-TTL cache or an invalidation hook on grant/revoke
  avoids a DB hit per request.
- **Alternatives considered**:
  - Relying on JDBC-loaded authorities (Option B): rejected — revokes/deactivations would not
    apply until re-login, violating Principle II and SC-006.

References: Spring Security 7 `AuthorizationManager<T>.authorize(Supplier<Authentication>, T)`
`RequestAuthorizationContext` (current in 7.x), `authorize-http-requests` reference.

## D-5 Sessions — fixation protection, redirect-back, concurrent-session policy

- **Decision**: Rely on Spring Security defaults for the session guarantees and document a
  concrete concurrency policy:
  - **New session id at sign-in (FR-008)**: default session-fixation protection
    (`changeSessionId()` — active by default on Servlet 3.1+ containers).
  - **Redirect to originally requested page (FR-007)**: default `RequestCache`
    (`HttpSessionRequestCache`) saves the requested URL; the successful-login handler replays
    it; `defaultSuccessUrl("/", false)` falls back to home when no saved request exists.
  - **Authenticated visitor opening `/login` (FR-011)**: `LoginController` checks for a
    non-anonymous `Authentication` and redirects to `/` (no built-in auto-redirect exists).
  - **Expired/invalidated session mid-task**: default behavior — the next request has no
    `SecurityContext`, is not authenticated, and is redirected to `/login`; controller code
    must never render protected content pre-check.
  - **Concurrent sign-ins (defined, consistent)**: multiple sessions for the same account are
    allowed; each session is independent and remains usable; the feature does not enforce a
    maximum or invalidate the earlier session.
- **Rationale**: Reusing hardened defaults minimizes custom code and matches the spec's
  requirement that behavior be "defined and consistent" (edge cases, Story 3).
- **Alternatives considered**: Session-concurrency cap via
  `ConcurrentSessionControlAuthenticationStrategy`: adds registry state and UX ambiguity
  (forced logouts) with no FR backing; rejected.

References: Spring Security 7 sessions reference (fixation default `changeSessionId`),
servlet architecture (RequestCache/SavedRequest replay).

## D-6 Authentication event audit — success/failure/logout handlers -> `auth_event`

- **Decision**: Persist every sign-in outcome to a supporting `auth_event` table via three
  security extension points:
  - `AuthenticationSuccessHandler` (wrapping/extending
    `SavedRequestAwareAuthenticationSuccessHandler`): record `SUCCESS` with
    `authentication.getName()`.
  - `AuthenticationFailureHandler`: record `FAILURE` with the submitted identifier from
    `ex.getAuthenticationRequest().getName()` (fallback `request.getParameter("username")`);
    this works even for unknown usernames with `hideUserNotFoundExceptions=true` (default),
    without enabling user enumeration. Never log the token or its credentials.
  - `LogoutHandler` registered via `addLogoutHandler(...)`: record `SIGNOUT` with
    `authentication.getName()`; runs before the default `SecurityContextLogoutHandler`
    (appended last) so the principal is still available.
- **Rationale**: FR-013 — each event carries account identifier, UTC time, outcome; no
  password or derived secret is ever stored (data-model.md `auth_event` has no credential
  field and handlers only write the username).
- **Alternatives considered**: `@EventListener` on
  `AuthenticationSuccessEvent`/`AuthenticationFailureBadCredentialsEvent`/`LogoutSuccessEvent`:
  works and decouples from the servlet API but adds sync/async ceremony for no benefit here;
  handlers are simpler and already required for the throttle + redirects.

References: Spring Security 7 `ProviderManager.prepareException()` sets
`getAuthenticationRequest()` on every failed `AuthenticationException`; `LogoutFilter` runs
handlers then `SecurityContextLogoutHandler` last; events reference (same tokens).

## D-7 Username normalization — trim + case-insensitive match

- **Decision**: Normalize the submitted username at login: trim surrounding whitespace and
  match case-insensitively via `LOWER(u.username) = LOWER(?)` in both JDBC queries.
  Empty username/password is refused with the generic credentials message (no lookup
  bypass).
- **Rationale**: Spec assumption ("matched case-insensitively and trimmed of surrounding
  whitespace") and edge cases. SQL-level `LOWER()` keeps the policy server-side and applies
  equally to the identity and authorities queries without writing to the account row.
- **Cross-feature note**: feature-001's `user.create` currently stores username verbatim
  (`[a-zA-Z0-9._-]+`, mixed case allowed). For full consistency, store a normalized
  (trimmed, lowercased) username at creation time in 001; until then, the D-7 match rule
  guarantees predictable login behavior regardless of stored case.
- **Alternatives considered**: Lowercasing at write time only: out of this feature's scope
  (001 owns user creation). Mandating exact-case usernames in the UI: violates the spec
  assumption.

References: spec Assumptions; feature-001 data-model.md `users.username` rule.