---

description: "Task list for RBAC User Management feature implementation"
---

# Tasks: RBAC User Management

**Input**: Design documents from `/specs/001-rbac-user-management/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md,
data-model.md, contracts/

**Tests**: INCLUDED. The constitution (v2.0.0, Principle IV) mandates dual-sided
authorization tests (200 OK for permitted / 403 Forbidden for unauthorized) plus lockout
guardrail coverage, so every user story ships with its test suite.

**Organization**: Tasks are grouped by user story to enable independent implementation and
testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single Spring Boot Maven project at repository root.
- Java sources: `src/main/java/com/example/rbac/...`; tests:
  `src/test/java/com/example/rbac/...`; views: `src/main/resources/templates/...`.
- Contract details: `contracts/permissions.md`, `contracts/roles.md`, `contracts/users.md`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and buildable skeleton

- [ ] T001 Create Maven project at repository root (`pom.xml`) for Java 17 with Spring Boot
      3.x starters: web, security, thymeleaf, data-jpa, validation; plus H2 runtime
      dependency and spring-boot-starter-test + spring-security-test (test scope)
- [ ] T002 [P] Create `src/main/resources/application.yml`: embedded H2 datasource,
      `spring.jpa.hibernate.ddl-auto=update` (Hibernate-managed schema per constitution
      III), `spring.jpa.open-in-view=false`, UTC timezone, server port 8080, and
      `app.seed.admin-password` placeholder
- [ ] T003 [P] Vendor the CoreUI Admin Bootstrap 5 template assets (CSS/JS/images) under
      `src/main/resources/static/assets/` with a documented provenance note in
      `src/main/resources/static/assets/README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be
implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 [P] Create entities exactly per `data-model.md`: `User`, `Role`, `Permission`,
      `RolePermission`, `UserRole`, `AuditEvent` in `src/main/java/com/example/rbac/domain/`
      (UUID PKs, UTC timestamps, composite keys `(role_id, permission_id)` /
      `(user_id, role_id)`, cascade rules, optimistic `version` on Role/User)
- [ ] T005 [P] Create Spring Data repositories for all six entities in
      `src/main/java/com/example/rbac/domain/` (interfaces, incl. uniqueness queries)
- [ ] T006 [P] Create centralized permission catalog constants class
      `src/main/java/com/example/rbac/security/Permissions.java` with every code from
      `contracts/README.md` (no magic strings — constitution Security Implementation
      Standards)
- [ ] T007 Create `PermissionResolver` resolving effective permissions
      (User-Roles -> Roles -> Permissions, active only, active-user only) in
      `src/main/java/com/example/rbac/security/PermissionResolver.java` per D-1/research.md
- [ ] T008 Create Spring Security configuration
      `src/main/java/com/example/rbac/security/SecurityConfig.java`: deny-by-default
      (`authorizeHttpRequests` whitelisting only `/login`, `/css/**`, `/js/**`,
      `/assets/**`, `/error`), form-login, `AuthorizationManager` delegating to
      PermissionResolver BEFORE controller dispatch, and exposing resolved permissions to
      Thymeleaf request attributes (contracts/README.md security conventions)
- [ ] T009 Create `UserPrincipalService` in
      `src/main/java/com/example/rbac/security/UserPrincipalService.java` loading users by
      username (with ACTIVE check) and BCrypt password encoding, plus the `/login`
      Thymeleaf template in `src/main/resources/templates/login.html`
- [ ] T010 [P] Create `AuditService` in `src/main/java/com/example/rbac/service/AuditService.java`
      appending `audit_event` rows (actor, action, target type/id, before/after, UTC
      timestamp) per FR-014/data-model.md
- [ ] T011 [P] Create `GlobalExceptionHandler` in
      `src/main/java/com/example/rbac/web/GlobalExceptionHandler.java` mapping 403 (not
      authorized), 400 (validation), 404, and 409 (conflict) to error templates in
      `src/main/resources/templates/error/`
- [ ] T012 [P] Create the CoreUI-backed base layout and permission-driven sidebar fragment
      in `src/main/resources/templates/fragments/` rendering menu/action links only for
      granted permissions (D-2, server-side source of truth)
- [ ] T013 Create idempotent `DataSeeder` in `src/main/java/com/example/rbac/config/DataSeeder.java`:
      all permission catalog rows, protected "Super Admin" role (`isProtected=true`) with all
      permissions, and one seeded administrator user (`app.seed.admin-password`)

**Checkpoint**: Foundation ready — security pipeline, schema, seed data all present.
User story implementation can now begin.

---

## Phase 3: User Story 1 - Manage the Permission Catalog (Priority: P1) 🎯 MVP

**Goal**: Admins can list, create, view, deactivate, and reactivate permissions under a
fail-closed, permission-enforced flow.

**Independent Test**: Sign in as a user granted `permission.*` codes — full CRUD works;
sign in as a user with every code except them — 403 on all permission screens.

### Tests for User Story 1 (write FIRST, ensure they FAIL before implementation)

- [ ] T014 [P] [US1] Dual-sided authorization tests in
      `src/test/java/com/allensandiego/adm/security/PermissionAuthorizationTest.java` covering
      every GET/POST in `contracts/permissions.md`: granted role → 200, denied role → 403
- [ ] T015 [P] [US1] Service tests in `src/test/java/com/allensandiego/adm/service/PermissionServiceTest.java`:
      unique code enforcement, blank/invalid code rejection, deactivate prevents new grants,
      reactivate restores availability (FR-002/FR-003/FR-012)

### Implementation for User Story 1

- [ ] T016 [US1] Implement `PermissionService` in
      `src/main/java/com/allensandiego/adm/service/PermissionService.java` (CRUD, activate/
      deactivate, uniqueness, active-only grants) — depends on T004–T006, T010
- [ ] T017 [P] [US1] Implement `PermissionController` in
      `src/main/java/com/allensandiego/adm/web/PermissionController.java` (list w/ paging+search,
      new, detail, edit) per `contracts/permissions.md`
- [ ] T018 [P] [US1] Create Permission Thymeleaf templates (list, new, detail, edit) in
      `src/main/resources/templates/permission/`
- [ ] T019 [US1] Wire audit logging for permission mutations through `AuditService`
      (create, edit, deactivate, reactivate) with before/after values
- [ ] T020 [US1] Verify full permission story: run
      `src/test/java/com/allensandiego/adm/security/PermissionAuthorizationTest.java` and
      `PermissionServiceTest.java` green; manual pass of Quickstart steps 1 and 6

**Checkpoint**: Permission catalog fully functional and independently testable — MVP.

---

## Phase 4: User Story 2 - Manage Roles and Edit Role Permissions (Priority: P2)

**Goal**: Admins create, rename, delete roles and edit role-permission sets in a single
screen, with lockout and concurrency protections.

**Independent Test**: Sign in as a user granted `role.*` (+ `permission.view`) — role CRUD
and the role-permission editor work; a user lacking `role.permissions.edit` gets 403 on the
editor; deleting/skipping the final protected role is refused with 409.

### Tests for User Story 2 (write FIRST, ensure they FAIL before implementation)

- [ ] T021 [P] [US2] Dual-sided authorization tests in
      `src/test/java/com/example/rbac/security/RoleAuthorizationTest.java` covering every
      GET/POST in `contracts/roles.md` (incl. `role.permissions.edit` and `role.delete`)
- [ ] T022 [ ] [US2] Guardrail + concurrency tests in
      `src/test/java/com/example/rbac/guardrails/RoleGuardrailTest.java`: G1 (delete last
      protected role → 409, no change), optimistic-lock conflict (second save → 409)
      (FR-010/FR-013)
- [ ] T023 [P] [US2] Service tests in `src/test/java/com/example/rbac/service/RoleServiceTest.java`:
      role CRUD, permission-set replacement exactness, active-only grant enforcement, G1

### Implementation for User Story 2

- [ ] T024 [US2] Implement `RoleService` in
      `src/main/java/com/allensandiego/adm/service/RoleService.java` (CRUD, `savePermissionSet`
      full-replace semantics, G1 guardrail calling PermissionResolver, optimistic locking) —
      depends on T004–T010
- [ ] T025 [P] [US2] Implement `RoleController` in
      `src/main/java/com/example/rbac/web/RoleController.java` including the role-permission
      editor (list w/ paging+search, new, detail, edit, permissions, delete) per
      `contracts/roles.md`
- [ ] T026 [P] [US2] Create Role Thymeleaf templates (list, new, detail, edit,
      permissions-editor with live checkboxes, inactive permissions read-only) in
      `src/main/resources/templates/role/` — matched controller model attributes:
      roles (List<Role>), totalPages, currentPage, searchTerm, size (list.html); role (Role)
      (new/detail/edit/permissions.html); permissions (PageResult<Permission>) (permissions.html)
      RoleServiceTest green; manual pass of Quickstart steps 2 and 5 (Super Admin refusal)
- [ ] T027 [US2] Wire audit logging for role create/rename/delete and permission-set saves through AuditService in src/main/java/com/allensandiego/adm/service/RoleService.java
- [ ] T028 [US2] Verify role story: run RoleAuthorizationTest, RoleGuardrailTest, RoleServiceTest green; manual pass of Quickstart steps 2 and 5

**Checkpoint**: Roles and role-permission editing functional; builds on US1.

---

## Phase 5: User Story 3 - Manage Users and Assign Roles (Priority: P3)

**Goal**: Admins create users, view/edit accounts, activate/deactivate, and assign roles,
with effective-permission visibility and final-admin guardrails.

**Independent Test**: Sign in as a user granted `user.*` (+ `role.view`,
`permission.view`) — user CRUD and role assignment work; detail shows the exact union of
active permissions; removing the final admin access is refused with 409; deactivated users
get 403 everywhere.

### Tests for User Story 3 (write FIRST, ensure they FAIL before implementation)

- [ ] T029 [P] [US3] Dual-sided authorization tests in
      `src/test/java/com/example/rbac/security/UserAuthorizationTest.java` covering every
      GET/POST in `contracts/users.md` (incl. `user.roles.assign`, `user.activate`)
- [ ] T030 [P] [US3] Guardrail + effective-permission tests in
      `src/test/java/com/example/rbac/guardrails/UserGuardrailTest.java`: G2 (remove last
      protected-role assignment → 409), G3 (deactivate final admin → 409), deactivated user
      → empty permission set, effective set == exact union of active permissions of assigned
      roles (FR-010/FR-008/SC-005)
- [ ] T031 [P] [US3] Service tests in `src/test/java/com/example/rbac/service/UserServiceTest.java`:
      user CRUD, unique username, activate/deactivate transitions, single-assignment rule

### Implementation for User Story 3

- [ ] T032 [US3] Implement `UserService` in
      `src/main/java/com/allensandiego/adm/service/UserService.java` (CRUD, status transitions,
      `assignRoles` full-replace, effective-permission lookup via PermissionResolver,
      G2/G3 guardrails, optimistic locking) — depends on T004–T010
- [ ] T033 [P] [US3] Implement `UserController` in
        `src/main/java/com/allensandiego/adm/web/UserController.java` (list w/ paging+search, new,
        detail, edit, status, roles) per `contracts/users.md`
- [ ] T034 [P] [US3] Create User Thymeleaf templates (list, new, detail with effective
      permissions panel, edit, role-assignment multi-select) in
      `src/main/resources/templates/user/`
- [ ] T035 [US3] Wire audit logging for user create/edit/status and role-assignment changes
      through `AuditService`
- [ ] T036 [US3] Verify user story: run UserAuthorizationTest, UserGuardrailTest,
      UserServiceTest green; manual pass of Quickstart steps 3 and 4 (self-service 403 demo)

**Checkpoint**: All user stories functional; full RBAC loop demonstrable end-to-end.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T037 [P] Fail-closed suite in `src/test/java/com/example/rbac/security/FailClosedTest.java`:
      unauthenticated access to all non-whitelisted paths → 401/login redirect; deactivated
      user → 403 on every protected path (SC-003, constitution Pr. II/IV)
- [ ] T038 [P] Audit completeness test in `src/test/java/com/example/rbac/audit/AuditTest.java`:
      every mutation writes an `audit_event` with actor, UTC timestamp, before/after (SC-006)
- [ ] T039 [P] Update repository `README.md` with run instructions and deferred
      `TODO(PRODUCTION_DATABASE)` note from constitution v2.0.0
- [ ] T040 Run the full `quickstart.md` validation end-to-end (automated `mvn test` + manual
      walkthrough); record any gaps in `specs/001-rbac-user-management/notes.md`
- [ ] T041 [P] Final code review pass: no magic permission strings, no validation bypass,
      all endpoints behind the security layer; clean up TODOs

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational completion
  - US1 (P1) → US2 (P2) → US3 (P3) recommended sequential (US2/3 integrate with the US1
    catalog & permission codes), but all technically parallel after Phase 2
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational - no dependencies on other stories
- **US2 (P2)**: Can start after Foundational - needs `permission.view` for the editor's
  active-permission list; still independently testable (seed data supplies catalog)
- **US3 (P3)**: Can start after Foundational - needs `role.view`/`permission.view` for the
  effective-permission panel; independently testable with seed data

### Within Each User Story

- Tests written FIRST and FAILING before any implementation (constitution-mandated)
- Models/repos before services; services before controllers/views; audit wiring last

### Parallel Opportunities

- Phase 1 setup tasks all marked [P] — independent files
- Phase 2 foundational tasks marked [P] — independent files
- Within each story the test tasks run in parallel, and controller/template tasks run in
  parallel after the service task
- Stories can be developed in parallel by different team members once Phase 2 completes

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Dual-sided authorization tests in src/test/java/com/example/rbac/security/PermissionAuthorizationTest.java"
Task: "PermissionService tests in src/test/java/com/example/rbac/service/PermissionServiceTest.java"

# Launch implementation files (after service task) together:
Task: "PermissionController in src/main/java/com/example/rbac/web/PermissionController.java"
Task: "Permission Thymeleaf templates in src/main/resources/templates/permission/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: PermissionAuthorizationTest + PermissionServiceTest green; manual
   CRUD verified
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready (deny-by-default security live)
2. US1 → permission catalog (MVP) → Test → Deploy/Demo
3. US2 → roles + role-permission editor → Test → Deploy/Demo
4. US3 → users + role assignment → Test → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Once Foundational is done: Developer A = US1, Developer B = US2, Developer C = US3
3. Stories integrate independently; US2/US3 consume the shared catalog/seeder

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to a specific user story for traceability
- Constitution governance: each new endpoint MUST carry 200/403 tests — enforced via the
  dual-sided suites in every story phase
- Commit after each task or logical group; stop at checkpoints to validate the story
  independently

(End of file - total 323 lines)
