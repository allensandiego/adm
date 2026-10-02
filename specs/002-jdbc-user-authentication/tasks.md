---

description: "Task list for JDBC User Authentication feature implementation"
---

# Tasks: JDBC User Authentication

**Input**: Design documents from `/specs/002-jdbc-user-authentication/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: INCLUDED. The constitution (v2.0.0, Principle IV) mandates explicit tests for both
sides of every permission boundary (200 OK permitted / 403 Forbidden unauthorized) plus
lockout-critical behavior, and Governance rejects any endpoint or permission change that ships
without them. Every user story carries its own suite.

**Organization**: Tasks are grouped by user story to enable independent implementation and
testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot Maven project at repository root, artifact `adm`.
- Base package: `com.allensandiego.adm` (matches `AdmApplication` and the `pom.xml` groupId
  `com.allensandiego`).
- Main sources: `src/main/java/com/allensandiego/adm/`; tests:
  `src/test/java/com/allensandiego/adm/`; views: `src/main/resources/templates/`.
- Config: `src/main/resources/application.properties` (embedded H2, Hibernate `ddl-auto`).
- Static assets: `src/main/resources/static/` (CSS/JS copied from the vendored `coreui/`
  template at the repo root).

> **Cross-feature dependencies (plan.md)**: this feature depends on feature 001's
> `SecurityConfig`/permission-resolution fabric. The login pipeline builds on top of the
> existing RBAC model; authentication establishes identity, while authorization (feature 001)
> gates protected resources. Get the `JdbcUserDetailsManager` and throttling seam right —
> downstream features depend on them.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Build dependencies, configuration, package skeleton, and audit event support for authentication

- [X] T001 Verify `pom.xml` already carries the required stack: feature 001's
      `spring-boot-starter-security`, `spring-security-test`, `com.h2database:h2` (runtime),
      plus `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`,
      `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`, and for tests
      `spring-boot-starter-test` — nothing additional needed beyond 001's stack
- [X] T002 [P] Confirm `src/main/resources/application.properties` has feature 001's settings:
      embedded H2 datasource (`jdbc:h2:mem:adm`), `spring.jpa.hibernate.ddl-auto=update`,
      `spring.jpa.open-in-view=false`, UTC timezone, `server.port=8080`, and
      `app.seed.admin-password` for the seeder (research D-7/D-8)
- [X] T003 [P] Create the test package skeleton `security/`, `guardrails/`, `service/`, `audit/`
      under `src/test/java/com/allensandiego/adm/` — depends on existing 001 structure
- [X] T004 [P] Create the `LoginAttemptRegistry` in-memory throttle state manager in
      `src/main/java/com/allensandiego/adm/security/LoginAttemptRegistry.java`:
      `ConcurrentHashMap<String, AttemptWindow>` keyed by normalized username (trimmed, lower),
      storing first-attempt timestamp and failure count; evict after 15-minute window
      (research D-3, FR-012)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, repositories, JDBC authentication manager, password encoding, and audit events
that every user story builds on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 [P] Create the `PasswordEncoder` wrapper in
      `src/main/java/com/allensandiego/adm/security/PasswordEncoderWrapper.java`:
      delegate to `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (BCrypt default),
      expose `encode()` and `matches()` methods, wire into `DaoAuthenticationProvider` via
      `setUserDetailsPasswordService(...)` for lazy re-hash on sign-in success (research D-2)
- [X] T006 Create the `AuthEvent` entity in
      `src/main/java/com/allensandiego/adm/domain/AuthEvent.java`: UUID v4 `id`, nullable
      `username` (string, submitted account identifier), nullable `accountId` FK -> `User`,
      `outcome` enum (`SUCCESS`, `FAILURE`, `SIGNOUT`), UTC `occurredAt`; supporting table only,
      not an RBAC entity (data-model.md FR-013)
- [X] T007 Create the `AuthEventOutcome` enum in
      `src/main/java/com/allensandiego/adm/domain/AuthEventOutcome.java`: `SUCCESS`,
      `FAILURE`, `SIGNOUT` — no other values (FR-013, data-model.md)
- [X] T008 Create `AuthEventRepository` in
      `src/main/java/com/allensandiego/adm/domain/AuthEventRepository.java` with
      `findAllByOutcome`, paged queries for security review — depends on T006 (FR-013/SC-008)
- [X] T009 Implement `AuthEventService` in
      `src/main/java/com/allensandiego/adm/service/AuthEventService.java` exposing
      `record(username, accountId, outcome)` that appends an `AuthEvent` with a UTC timestamp;
      never log passwords or hashes (FR-013/SC-008) — depends on T006, T007, T008
- [X] T010 Create the `JdbcUserDetailsManager` bean in
      `src/main/java/com/allensandiego/adm/security/JdbcUserDetailsManager.java`:
      extend `JdbcUserDetailsManager`, override `usersByUsernameQuery()` and
      `authoritiesByUsernameQuery()` with custom SQL against existing H2 tables:
      - users query: `(username, password_hash, enabled)` columns, `LOWER(u.username)=LOWER(?)` match
        (research D-1/D-7)
      - authorities query: role names via `user_roles` -> `roles`, same case-insensitive match
        (informational only, never used for authorization — research D-4)
      - call `setUsernameBasedPrimaryKey(false)` since users table uses UUID PKs
      - inject the existing H2 `DataSource` from feature 001 (research D-1)
- [X] T011 Create `SecurityConfig` updates in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java`:
      - add `AuthenticationManager` bean using `DaoAuthenticationProvider` with the new
        `JdbcUserDetailsManager` and `PasswordEncoderWrapper` (research D-2)
      - configure `HttpSessionCreationPolicyRegistry` for session fixation protection
        (`changeSessionId()` active by default on Servlet 3.1+) (FR-008, research D-5)
      - wire `SavedRequestAwareAuthenticationSuccessHandler` and
        `AuthenticationFailureHandler` for login flow (research D-6)
      - register custom `LogoutHandler` before `SecurityContextLogoutHandler` to record signouts
        (research D-6)

**Checkpoint**: Entities, repositories, JDBC authentication manager, password encoding, and audit events
are ready — user story implementation can begin

---

## Phase 3: User Story 1 - Sign In with Account Credentials (Priority: P1) 🎯 MVP

**Goal**: An unauthenticated operator can sign in with valid credentials; wrong/unknown/deactivated
credentials are refused with a single generic message; empty submissions fail without disclosure.

**Independent Test**: Open the application unauthenticated, enter a valid stored account's username
and password, and confirm the home screen renders; then submit wrong credentials and verify
a generic "invalid credentials" message appears without revealing which field was wrong.

### Tests for User Story 1 (write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T012 [P] [US1] Dual-sided authorization tests in
      `src/test/java/com/allensandiego/adm/security/JdbcAuthenticationTests.java`:
      parameterized over sign-in scenarios from `contracts/authentication.md` — valid active
      account gets authenticated session, wrong password/unknown username/deactivated account
      get 302 to `/login?error` with generic message (FR-003, SC-002)
- [X] T013 [P] [US1] Edge case tests in
      `src/test/java/com/allensandiego/adm/security/JdbcAuthenticationEdgeTests.java`:
      empty username/password rejected, whitespace-only submissions fail, same account already
      signed in elsewhere still succeeds (multiple sessions allowed), unverifiable/missing hash
      refused without crash or disclosure (FR-004/FR-014)
- [X] T014 [P] [US1] Event recording tests in
      `src/test/java/com/allensandiego/adm/security/AuthEventRecordingTests.java`:
      every sign-in outcome writes an `AuthEvent` with username, UTC timestamp, and correct
      outcome; never logs passwords or hashes (FR-013/SC-008)

### Implementation for User Story 1

- [X] T015 [P] [US1] Create the validated `LoginRequest` record in
      `src/main/java/com/allensandiego/adm/web/form/LoginRequest.java`: `username` non-blank,
      trimmed (max 64 chars matching `[a-zA-Z0-9._-]+`), `password` non-blank — Jakarta Bean
      Validation (FR-012)
- [X] T016 [US1] Implement `LoginController` in
      `src/main/java/com/allensandiego/adm/web/LoginController.java`:
      - GET `/login` renders sign-in screen (CoreUI template), assets load without auth (FR-001)
      - POST `/login` processes form submission via Spring Security's
        `UsernamePasswordAuthenticationFilter`, redirects to saved request or `/` on success,
        `/login?error` on failure (FR-007/FR-008)
      - GET `/` is default success URL when no original request saved; also home for authenticated
        users opening sign-in screen (FR-011) — depends on T009, T010, T015
- [X] T017 [US1] Implement `AuthenticationSuccessHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthenticationSuccessHandler.java`:
      extends/wraps `SavedRequestAwareAuthenticationSuccessHandler`, records `SUCCESS` event via
      `AuthEventService` with account identifier, clears throttle counter on success (research D-6)
      — depends on T009, T017
- [X] T018 [US1] Implement `AuthenticationFailureHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthenticationFailureHandler.java`:
      records `FAILURE` event via `AuthEventService` with submitted username (from
      `ex.getAuthenticationRequest().getName()` or fallback to request parameter), works for
      unknown usernames without enabling enumeration, configures `hideUserNotFoundExceptions=true`
      (research D-6) — depends on T009, T017
- [X] T019 [US1] Implement throttling guard in
      `src/main/java/com/allensandiego/adm/security/ThrottledAuthenticationProvider.java`:
      custom provider wrapping `DaoAuthenticationProvider`, checks `LoginAttemptRegistry` before
      credential lookup to block attempts after 5 failures within 15 minutes, applies uniformly
      to known and unknown usernames so account existence is never revealed (research D-3) —
      depends on T004, T010
- [X] T020 [US1] Create the sign-in screen view in
      `src/main/resources/templates/login.html` using CoreUI admin layout with username/password
      fields, submit button, and error message area for generic "invalid credentials" feedback

**Checkpoint**: User Story 1 fully functional and independently testable — this is the MVP gateway

---

## Phase 4: User Story 2 - Sign Out and End the Session (Priority: P2)

**Goal**: A signed-in operator can sign out immediately; any subsequent attempt to reach protected
content sends them back to the sign-in screen.

**Independent Test**: Sign in, select sign out, and confirm protected pages are no longer reachable
and redirect to `/login`; include testing via browser history to ensure session termination works.

### Tests for User Story 2 (write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T021 [P] [US2] Sign-out flow tests in
      `src/test/java/com/allensandiego/adm/security/LogoutFlowTests.java`:
      POST `/logout` while signed in records `SIGNOUT` event, invalidates session and clears
      SecurityContext, returns to `/login?logout`, subsequent protected URLs redirect to `/login`
      (FR-009/SC-005)
- [X] T022 [P] [US2] Session termination tests in
      `src/test/java/com/allensandiego/adm/security/SessionTerminationTests.java`:
      expired session mid-task redirects to `/login` on next request, no protected content leaked,
      browser history navigation after logout also requires re-authentication (SC-005)

### Implementation for User Story 2

- [X] T023 [US2] Update `SecurityConfig` in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java`:
      - configure `LogoutSuccessHandler` to redirect to `/login?logout` (default behavior)
      - register custom `LogoutHandler` via `addLogoutHandler()` before default handler to record
        signout events (research D-6) — depends on T009, T017
- [X] T024 [US2] Update `AuthenticationSuccessHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthenticationSuccessHandler.java`:
      ensure session fixation protection (`changeSessionId()`) is active so new session id issued
      at each sign-in (FR-008, research D-5) — depends on T017

**Checkpoint**: User Stories 1 AND 2 both work independently — authentication cycle complete

---

## Phase 5: User Story 3 - Refuse Accounts That Must Not Enter (Priority: P3)

**Goal**: An account that has been deactivated or is throttled attempts to sign in are refused,
and the fact that the password was correct is not disclosed.

**Independent Test**: Deactivate a stored account, attempt sign-in with its correct password, and
confirm entry is refused; also verify throttled accounts cannot bypass the lockout window.

### Tests for User Story 3 (write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T025 [P] [US3] Deactivation refusal tests in
      `src/test/java/com/allensandiego/adm/security/DeactivationRefusalTests.java`:
      deactivated account with correct password refused sign-in, no session created, generic
      error message (FR-004, SC-003)
- [X] T026 [P] [US3] Throttle lockout tests in
      `src/test/java/com/allensandiego/adm/security/ThrottleLockoutTests.java`:
      5 failed attempts within window triggers 15-minute block, blocked attempts recorded as
      `FAILURE` events, success clears window, unknown usernames also throttled (no existence
      disclosure) — FR-012, SC-007
- [X] T027 [P] [US3] Username normalization tests in
      `src/test/java/com/allensandiego/adm/security/UsernameNormalizationTests.java`:
      submitted username trimmed of whitespace, matched case-insensitively via SQL `LOWER()`,
      empty username/password refused with generic message (spec Assumptions, D-7)

### Implementation for User Story 3

- [X] T028 [US3] Update `JdbcUserDetailsManager` in
      `src/main/java/com/allensandiego/adm/security/JdbcUserDetailsManager.java`:
      ensure SQL queries use `CASE WHEN u.status='ACTIVE' THEN true ELSE false END` for the
      enabled column, so JDBC treats INACTIVE accounts as disabled (FR-004, research D-1)
- [X] T029 [US3] Update throttling logic in
      `src/main/java/com/allensandiego/adm/security/ThrottledAuthenticationProvider.java`:
      throttle applies uniformly to known and unknown usernames, check before credential lookup
      so account existence is never revealed (FR-012, research D-3) — depends on T019
- [X] T030 [US3] Update `AuthenticationFailureHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthenticationFailureHandler.java`:
      record failures for deactivated accounts and throttled attempts as `FAILURE` events
      (FR-013) — depends on T018

**Checkpoint**: All user stories independently functional — authentication with guardrails complete

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cross-story audit/fail-closed coverage, hardening, and end-to-end validation

- [X] T031 [P] Audit completeness tests in
      `src/test/java/com/allensandiego/adm/audit/AuthEventAuditTest.java`:
      every successful sign-in, failed sign-in, and sign-out writes an `AuthEvent` with actor
      (username), UTC timestamp, and outcome; 100% of authentication events are reviewable
      (FR-013/SC-008)
- [X] T032 [P] Fail-closed tests in
      `src/test/java/com/allensandiego/adm/security/FailClosedAuthTest.java`:
      unauthenticated requests to protected paths redirect to `/login`, no protected content
      disclosed, deactivated principals get refused access (FR-011/SC-003)
- [X] T033 [P] Password hash validation tests in
      `src/test/java/com/allensandiego/adm/security/PasswordHashTests.java`:
      `{id}encoded` delegation format enforced, null/empty stored hash refused without crash,
      BCrypt hashes work correctly (FR-005)
- [X] T034 [P] Security hardening in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java`:
      CSRF enabled on all state-changing routes (`/login`, `/logout`), ensure no plaintext
      passwords appear in logs or events, verify session fixation protection is active (FR-014)
- [X] T035 [P] Documentation refresh: update
      `specs/002-jdbc-user-authentication/quickstart.md` run commands and the endpoint tables
      in `contracts/authentication.md` if the implementation diverged; there is no `HELP.md`
      in this repository, so do not create one
- [X] T036 Run `./mvnw test` and execute the authentication scenarios from
      `specs/002-jdbc-user-authentication/quickstart.md`; resolve failures until SC-001..SC-008
      all pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup**: No dependencies — can start immediately
- **Phase 2: Foundational**: Depends on existing feature 001 infrastructure — BLOCKS all user stories
- **Phase 3+: User Stories**: All depend on Phase 2
  - User stories can then proceed in priority order (P1 → P2 → P3)
  - Or sequentially if single developer
- **Phase 6: Polish**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: After Phase 2. Owns the sign-in/sign-out flows; no dependency on US2/US3.
- **US2 (P2)**: After Phase 2. Builds on US1's session management; independently testable once wired.
- **US3 (P3)**: After Phase 2. Extends US1/US2 with guardrails; independently testable.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Core security infrastructure before handlers/controllers
- Views after controller endpoints are stable

### Parallel Opportunities

- T001–T004 in Phase 1 can run together
- T005–T011 in Phase 2 can run together (entities/repositories parallel, manager/config sequential)
- T012–T014 (US1 tests) can run together; T021–T022 (US2 tests) can run together; T025–T027 (US3 tests)
  can run together
- Within a story, the Thymeleaf view tasks are independent of each other and of the service layer
- T031–T035 in Phase 6 can run together (T036 is the final serial gate)

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together:
Task: "Dual-sided authorization tests in src/test/java/com/allensandiego/adm/security/JdbcAuthenticationTests.java"
Task: "Edge case tests in src/test/java/com/allensandiego/adm/security/JdbcAuthenticationEdgeTests.java"
Task: "Event recording tests in src/test/java/com/allensandiego/adm/security/AuthEventRecordingTests.java"

# Launch US1 implementation tasks (forms, controller, handlers, manager):
Task: "Create LoginRequest form and implement LoginController"
Task: "Implement AuthenticationSuccessHandler with event recording"
Task: "Implement AuthenticationFailureHandler with generic error handling"
Task: "Update JdbcUserDetailsManager with custom queries"

# Launch US1 views after controller is stable:
Task: "Create sign-in screen view in src/main/resources/templates/login.html"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (infrastructure and test skeleton)
2. Complete Phase 2: Foundational (JDBC manager, password encoding, audit events)
3. Complete Phase 3: User Story 1 (sign-in flow)
4. **STOP and VALIDATE**: create a user account via feature 001, sign in with correct credentials,
   verify home screen renders; submit wrong credentials, confirm generic error message
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → authentication infrastructure ready
2. US1 → validate sign-in/sign-out and credential verification → MVP gateway
3. US2 → validate session termination and fail-closed behavior
4. US3 → validate guardrails (deactivation, throttling)
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

1. Team completes Phase 1 + Phase 2 together
2. Then:
   - Developer A: US1 (sign-in flow)
   - Developer B: US2 (session management), after US1's handlers land
   - Developer C: US3 (guardrails), after throttle logic is stable
3. Stories integrate through the shared `SecurityConfig` and `JdbcUserDetailsManager` — run T010
   before any story work and avoid editing it afterwards

---

## Success Criteria Mapping

| Criterion | Implementation Tasks |
|-----------|---------------------|
| SC-001: Valid account reaches home in <10s | T015, T016, T017 |
| SC-002: 100% invalid credentials refused with generic message | T018, T029 |
| SC-003: Deactivated accounts refused sign-in | T028, T025 |
| SC-004: Zero plaintext passwords recoverable | T005, T033 |
| SC-005: After sign-out, protected content redirects to login | T023, T021 |
| SC-006: Protected screens denied by default after sign-in without permission | Feature 001's `SecurityConfig` + T019 |
| SC-007: Repeated failed attempts throttled within first few tries | T019, T026 |
| SC-008: Every auth event recorded with actor and timestamp | T009, T031 |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps a task to its user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing; commit after each task or logical group
- All paths use the real base package `com.allensandiego.adm`; there is no `com.example.auth`
  tree in this repository
- Test artifact names matter: Spring Security test dependency is
  `org.springframework.security:spring-security-test` (see T001)
- `auth_event` is a supporting table required by FR-013; it does not affect the RBAC entities
- Feature 002 depends on feature 001's `SecurityConfig`, `User` entity, and H2 datasource
