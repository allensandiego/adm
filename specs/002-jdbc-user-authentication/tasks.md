---
description: "Task list for JDBC User Authentication feature implementation"
---

# Tasks: JDBC User Authentication

**Input**: Design documents from `/specs/002-jdbc-user-authentication/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: INCLUDED. The constitution (v2.0.0, Principle IV) mandates explicit tests for both
sides of every permission boundary (200 OK permitted / 403 Forbidden unauthorized) and
lockout-critical behavior, and the contract test table in `contracts/authentication.md`
enumerates the required scenarios. Every user story ships with its suite.

**Organization**: Tasks are grouped by user story to enable independent implementation and
testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot Maven project at repository root.
- Base package (actual): `com.allensandiego.adm` — feature 001's `com.allensandiego.rbac`
  placeholder is reconciled to this.
- Main sources: `src/main/java/com/allensandiego/adm/`; tests:
  `src/test/java/com/allensandiego/adm/`; views: `src/main/resources/templates/`.
- Config: `src/main/resources/application.properties` (embedded H2, Hibernate `ddl-auto`).

> **Cross-feature dependency (plan.md)**: feature `001-rbac-user-management` MUST be
> implemented first — this feature reuses its `User`/`Role`/`Permission` entities, its
> per-request `PermissionResolver`/`AuthorizationManager`, its shared layout, and its
> `DataSeeder`. Feature `003-seed-data-e2e-testing` provides seeded accounts for manual smoke
> runs only; this feature's tests create their own fixtures.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Build dependencies and configuration for the auth surface

- [X] T001 Verify and extend `pom.xml` for the auth surface: ensure
      `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`,
      `spring-boot-starter-validation`, `com.h2database:h2` (runtime), and
      `spring-boot-starter-test` (test) are present alongside `spring-boot-starter-security`
      and `spring-boot-starter-data-jdbc`; keep `spring-boot-starter-security-test` (test)
- [X] T002 [P] Configure `src/main/resources/application.properties`: embedded H2 datasource,
      `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.open-in-view=false`, UTC timezone,
      `server.port=8080`, and logging that never emits credentials (auth loggers at INFO, no
      request-body logging)
- [X] T003 [P] Create the package skeleton directories `config/`, `domain/`, `security/`,
      `service/`, `web/` under `src/main/java/com/allensandiego/adm/` and `security/`,
      `service/` under `src/test/java/com/allensandiego/adm/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persistence and credential primitives that every user story relies on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 [P] Add the nullable `passwordHash` field to the feature-001 `User` entity in
      `src/main/java/com/allensandiego/adm/domain/User.java` — stores `{id}encoded`
      credential strings (e.g. `{bcrypt}$2a$...`), never plaintext (FR-005)
- [X] T005 [P] Create the `AuthOutcome` enum (`SUCCESS`, `FAILURE`, `SIGNOUT`) in
      `src/main/java/com/allensandiego/adm/domain/AuthOutcome.java`
- [X] T006 Create the `AuthEvent` JPA entity in
      `src/main/java/com/allensandiego/adm/domain/AuthEvent.java` per `data-model.md`: UUID
      id, `username`, nullable `account_id` FK -> `User`, `outcome`, `occurred_at` (UTC) —
      depends on T005
- [X] T007 Create `AuthEventRepository` in
      `src/main/java/com/allensandiego/adm/domain/AuthEventRepository.java`
      (`JpaRepository<AuthEvent, UUID>` plus lookups by `outcome`/`username`) — depends on T006
- [X] T008 [P] Create the `PasswordEncoder` bean in
      `src/main/java/com/allensandiego/adm/config/PasswordEncoderConfig.java` using
      `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (research D-2)
- [X] T009 Implement `AuthEventService` in
      `src/main/java/com/allensandiego/adm/service/AuthEventService.java` exposing
      `record(String username, UUID accountId, AuthOutcome outcome)` that appends an
      `AuthEvent` with `Instant.now()` in UTC and never accepts credential material (FR-013) —
      depends on T006, T007

**Checkpoint**: Persistence + credential primitives ready — user story implementation can begin

---

## Phase 3: User Story 1 - Sign In with Account Credentials (Priority: P1) 🎯 MVP

**Goal**: A stored account can sign in with its credentials, land on the originally
requested page (or home), and receive a single generic message when credentials are invalid.

**Independent Test**: Open any protected page unauthenticated → redirected to `/login`;
submit valid stored credentials → home (or the originally requested page); submit bad
credentials → `/login?error` with one generic message.

### Tests for User Story 1 (write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T010 [P] [US1] Login flow tests in
      `src/test/java/com/allensandiego/adm/security/LoginFlowTests.java`: anonymous GET on a
      protected URL → 302 `/login`; anonymous GET `/login` → 200; POST valid active
      credentials → 302 to the saved request or `/`; the session id changes after login;
      wrong password and unknown username → 302 `/login?error` with a single generic message;
      empty username/password → the same generic error (`contracts/authentication.md`)
- [X] T011 [P] [US1] Dual-sided authorization tests in
      `src/test/java/com/allensandiego/adm/security/AuthorizationBoundaryTests.java`:
      anonymous → 302 `/login`; authenticated with the required permission → 200;
      authenticated without it → 403 (constitution Principle IV)

### Implementation for User Story 1

- [X] T012 [US1] Create `JdbcUserDetailsServiceConfig` in
      `src/main/java/com/allensandiego/adm/security/JdbcUserDetailsServiceConfig.java`: a
      `JdbcUserDetailsManager` bean with `usersByUsernameQuery` returning
      `(username, password_hash, enabled)` where `enabled = (status='ACTIVE')` and matched via
      `LOWER(username)=LOWER(?)`, and an `authoritiesByUsernameQuery` returning role names via
      `user_roles` -> `roles`; call `setUsernameBasedPrimaryKey(false)` (D-1/D-7)
- [X] T013 [US1] Create `AuthenticationProviderConfig` in
      `src/main/java/com/allensandiego/adm/security/AuthenticationProviderConfig.java`: a
      `DaoAuthenticationProvider` bean wired to the JDBC `UserDetailsService` (T012), the
      `PasswordEncoder` (T008), and `UserDetailsPasswordService` for lazy re-hash; keep
      `hideUserNotFoundExceptions=true` (D-2)
- [X] T014 [US1] Implement `AuthEventSuccessHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthEventSuccessHandler.java` as a
      `SavedRequestAwareAuthenticationSuccessHandler` subclass: record `SUCCESS` via
      `AuthEventService`, reset the throttle window when present (US3 hook), then delegate to
      the default SavedRequest replay / home redirect (FR-007/FR-013)
- [X] T015 [US1] Implement `AuthEventFailureHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthEventFailureHandler.java`: record
      `FAILURE` using `ex.getAuthenticationRequest().getName()` (fallback
      `request.getParameter("username")`), redirect `/login?error`, and never log or store the
      submitted password (D-6/FR-013)
- [X] T016 [US1] Create `SecurityConfig` in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java`: a
      `SecurityFilterChain` bean with `permitAll` for `/login`, `/css/**`, `/js/**`,
      `/assets/**`, `/error`; `formLogin` (`loginPage("/login")`,
      `loginProcessingUrl("/login")`, the T014/T015 handlers, `defaultSuccessUrl("/", false)`,
      `failureUrl("/login?error")`); `sessionManagement` default `changeSessionId`; default
      `csrf`; and `authorizeHttpRequests` deny-by-default
      `.anyRequest().access(<feature-001 AuthorizationManager>)` (FR-001/FR-007/FR-008/FR-010)
- [X] T017 [US1] Create the CoreUI sign-in view
      `src/main/resources/templates/login.html`: POST `/login` with a CSRF hidden field,
      username/password inputs, a single generic error block bound to `?error`, a sign-out
      notice bound to `?logout`, and assets referenced from `/assets/**` or `/css/**`
      (FR-001/FR-003)
- [X] T018 [US1] Create `LoginController` in
      `src/main/java/com/allensandiego/adm/web/LoginController.java`: GET `/login` renders
      `login` for anonymous users and redirects authenticated users to `/`; GET `/` renders
      the feature-001 home page (FR-011)

**Checkpoint**: User Story 1 fully functional and independently testable

---

## Phase 4: User Story 2 - Sign Out and End the Session (Priority: P2)

**Goal**: An explicit sign-out ends the session and makes protected content unreachable.

**Independent Test**: Sign in, POST `/logout`, then request a protected page → redirected to
`/login`.

### Tests for User Story 2 (write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T019 [P] [US2] Sign-out tests in
      `src/test/java/com/allensandiego/adm/security/LogoutTests.java`: signed-in POST
      `/logout` → 302 `/login?logout`; a later protected GET → 302 `/login`; an
      `AUTH_SIGNOUT` event is persisted (FR-009/SC-005)

### Implementation for User Story 2

- [X] T020 [US2] Implement `AuthEventLogoutHandler` in
      `src/main/java/com/allensandiego/adm/security/AuthEventLogoutHandler.java`
      implementing `LogoutHandler`: record `SIGNOUT` via `AuthEventService` using the passed
      `Authentication` parameter (not the security context holder)
- [X] T021 [US2] Register sign-out in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java`:
      `.logout(l -> l.logoutUrl("/logout").addLogoutHandler(authEventLogoutHandler)
      .logoutSuccessUrl("/login?logout").invalidateHttpSession(true)
      .clearAuthentication(true))` — depends on T016
- [X] T022 [US2] Add a CSRF-protected sign-out form (POST `/logout`) to the shared
      authenticated layout/header fragment in `src/main/resources/templates/fragments/`
      created by feature 001

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 - Refuse Accounts That Must Not Enter (Priority: P3)

**Goal**: Deactivated accounts are refused even with a correct password, and repeated failed
attempts are throttled without disclosing whether an account exists.

**Independent Test**: Deactivate a stored account and attempt sign-in with its correct
password → refused; make five failed attempts → further attempts are throttled.

### Tests for User Story 3 (write FIRST, ensure they FAIL before implementation) ⚠️

- [X] T023 [P] [US3] Deactivation refusal tests in
      `src/test/java/com/allensandiego/adm/security/DeactivationRefusalTests.java`: correct
      password on an `INACTIVE` account → 302 `/login?error`; a user deactivated mid-session
      loses access on the next request with no protected content (FR-004/SC-003/SC-006)
- [X] T024 [P] [US3] Throttle tests in
      `src/test/java/com/allensandiego/adm/security/ThrottleTests.java`: five failures within
      the window cause subsequent attempts to be refused with the generic message; an unknown
      username is throttled identically (no existence disclosure); a successful sign-in
      clears the counter (FR-012/SC-007)
- [X] T025 [P] [US3] Unverifiable-credential tests in
      `src/test/java/com/allensandiego/adm/security/UnverifiableCredentialTests.java`: a
      null/empty/dummy stored hash is refused generically with no exception leak or crash
      (FR-004/FR-014)
- [X] T026 [P] [US3] Auth-event audit tests in
      `src/test/java/com/allensandiego/adm/security/AuthEventAuditTests.java`: `SUCCESS`,
      `FAILURE`, and `SIGNOUT` rows are recorded with username, UTC timestamp, and outcome,
      and contain no password or hash (FR-013/SC-008)

### Implementation for User Story 3

- [X] T027 [US3] Implement `LoginAttemptRegistry` in
      `src/main/java/com/allensandiego/adm/service/LoginAttemptRegistry.java`: a
      `ConcurrentHashMap` sliding-window registry (5 failures / 15 minutes → 15-minute block)
      keyed by normalized username with `isBlocked`, `recordFailure`, and `reset` (D-3)
- [X] T028 [US3] Implement `ThrottledAuthenticationProvider` in
      `src/main/java/com/allensandiego/adm/security/ThrottledAuthenticationProvider.java`:
      check `registry.isBlocked(normalizedUsername)` BEFORE delegating and throw a generic
      `BadCredentialsException` when blocked; on failure call `recordFailure`, on success call
      `reset` — depends on T013, T027
- [X] T029 [US3] Register `ThrottledAuthenticationProvider` in
      `src/main/java/com/allensandiego/adm/security/AuthenticationProviderConfig.java` so the
      throttle wraps the JDBC provider — depends on T013, T028
- [X] T030 [US3] Extend `src/main/java/com/allensandiego/adm/security/AuthEventFailureHandler.java`
      to map `DisabledException`, `LockedException`, `BadCredentialsException`, and
      empty-credential failures to the single generic `/login?error` message and a `FAILURE`
      event (FR-003/FR-004) — depends on T015
- [X] T031 [US3] Implement `CredentialService` in
      `src/main/java/com/allensandiego/adm/service/CredentialService.java`: encode plaintext
      through the `PasswordEncoder` into `{id}hash`, and store a dummy `{bcrypt}` hash of
      random bytes when a credential is missing/unverifiable so matches fail without crashing
      or leaking timing (D-2/FR-014)
- [X] T032 [US3] Add generic auth-error safety in
      `src/main/java/com/allensandiego/adm/web/AuthErrorHandler.java` (or extend feature 001's
      `GlobalExceptionHandler`): authentication or credential-storage errors resolve to a
      generic response/redirect and never disclose internal details (FR-014)

**Checkpoint**: All user stories independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Hardening, unit coverage, and end-to-end validation

- [X] T033 [P] Add password-encoding unit tests in
      `src/test/java/com/allensandiego/adm/service/PasswordEncodingTests.java`: encoded values
      carry the `{bcrypt}` prefix, verify accepts/rejects correctly, the dummy hash never
      matches, and `upgradeEncoding` reports stale schemes
- [X] T034 [P] Add `LoginAttemptRegistryTests` in
      `src/test/java/com/allensandiego/adm/service/LoginAttemptRegistryTests.java`: threshold
      boundary, window expiry, reset-on-success, and identical behavior for known/unknown
      usernames
- [X] T035 [P] Security hardening: in `src/main/resources/application.properties` and
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java` ensure credentials
      are never logged, CSRF is enabled for login/logout, and authenticated responses send
      `Cache-Control: no-store` (FR-005/FR-013)
- [X] T036 [P] Update documentation: `HELP.md` and the run commands in
      `specs/002-jdbc-user-authentication/quickstart.md` if they changed
- [X] T037 Run `./mvnw test` and execute the
      `specs/002-jdbc-user-authentication/quickstart.md` smoke scenarios; resolve failures so
      SC-001..SC-008 all pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: After Foundational. Delivers the login pipeline; no dependency on US2/US3.
- **US2 (P2)**: After Foundational. Depends on US1's `SecurityConfig` (T016) for the filter
  chain; independently testable once wired.
- **US3 (P3)**: After Foundational. Extends US1's failure handler/provider; independently
  testable once wired.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Entities before repositories; services before handlers/controllers
- Core implementation before integration

### Parallel Opportunities

- T002/T003 in Setup can run together
- T004, T005, T008 in Foundational can run together (T006/T007 are sequential)
- T010/T011 (US1 tests) can run together; T023–T026 (US3 tests) can run together
- T033/T034/T035/T036 in Polish can run together

---

## Parallel Example: User Story 3

```bash
# Launch all US3 tests together:
Task: "Deactivation refusal tests in src/test/java/com/allensandiego/adm/security/DeactivationRefusalTests.java"
Task: "Throttle tests in src/test/java/com/allensandiego/adm/security/ThrottleTests.java"
Task: "Unverifiable-credential tests in src/test/java/com/allensandiego/adm/security/UnverifiableCredentialTests.java"
Task: "Auth-event audit tests in src/test/java/com/allensandiego/adm/security/AuthEventAuditTests.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: sign in, redirect-back, generic refusal
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → validate → MVP
3. US2 → validate sign-out
4. US3 → validate deactivation refusal + throttle
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then: Developer A → US1; Developer B → US2 (after T016); Developer C → US3 (after T015)
3. Stories integrate via the shared `SecurityConfig` (run T016/T021/T029 sequentially)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps a task to its user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing; commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- Feature 001's `com.allensandiego.rbac` placeholder is reconciled to the actual base package
  `com.allensandiego.adm`; all paths above use the actual package
