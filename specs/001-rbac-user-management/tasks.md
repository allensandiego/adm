---

description: "Task list for RBAC User Management feature implementation"
---

# Tasks: RBAC User Management

**Input**: Design documents from `/specs/001-rbac-user-management/`

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

> **Cross-feature dependencies (plan.md)**: this is the first feature. `002-jdbc-user-authentication`
> builds its login pipeline on the `SecurityConfig`/permission-resolution fabric created here,
> and `003-seed-data-e2e-testing` drives these screens in a real browser. Get the package
> layout and the `AuthorizationManager` seam right — downstream work depends on them.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Build dependencies, configuration, and package skeleton for the RBAC console

- [ ] T001 Verify `pom.xml` already carries the RBAC stack and add nothing that is missing:
      `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`,
      `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`,
      `spring-boot-starter-security`, `com.h2database:h2` (runtime), `org.projectlombok:lombok`
      (optional), and for tests `spring-boot-starter-test` + `spring-security-test` — note the
      Spring Security test artifact is `org.springframework.security:spring-security-test`, NOT
      `spring-boot-starter-security-test`, which does not exist
- [ ] T002 [P] Configure `src/main/resources/application.properties`: embedded H2 datasource
      (`jdbc:h2:mem:adm`), `spring.jpa.hibernate.ddl-auto=update`,
      `spring.jpa.open-in-view=false`, UTC timezone, `server.port=8080`, and
      `app.seed.admin-password` for the seeder (research D-7/D-8)
- [ ] T003 [P] Create the main package skeleton `config/`, `domain/`, `security/`, `service/`,
      `web/`, `web/form/` under `src/main/java/com/allensandiego/adm/`
- [ ] T004 [P] Populate `src/main/resources/static/` with the CoreUI Admin Bootstrap 5 CSS/JS
      copied from the vendored `coreui/` template so screens render correctly (research D-6)
- [ ] T005 [P] Create the test package skeleton `security/`, `guardrails/`, `service/`, `audit/`
      under `src/test/java/com/allensandiego/adm/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, repositories, permission resolution, and the fail-closed security seam
that every user story builds on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 [P] Create the centralized permission catalog in
      `src/main/java/com/allensandiego/adm/security/Permissions.java` — all 13 codes as
      constants (`permission.view/create/edit`, `user.view/create/edit/activate/roles.assign`,
      `role.view/create/edit/delete/permissions.edit`) exactly as listed in
      `contracts/README.md`; no magic strings anywhere else (FR-015, research D-2)
- [ ] T007 [P] Create `UserStatus` enum (`ACTIVE`, `INACTIVE`) in
      `src/main/java/com/allensandiego/adm/domain/UserStatus.java`
- [ ] T008 [P] Create the `Permission` entity in
      `src/main/java/com/allensandiego/adm/domain/Permission.java` — UUID v4 `id`, unique `code`,
      `label`, non-blank `path` (<=120), `active`, UTC `createdAt`/`updatedAt` (data-model.md)
- [ ] T009 [P] Create the `Role` entity in
      `src/main/java/com/allensandiego/adm/domain/Role.java` — UUID v4 `id`, unique `name`,
      `description`, `isProtected`, JPA `@Version` `version`, UTC timestamps (D-3/D-4)
- [ ] T010 Create the `User` entity in
      `src/main/java/com/allensandiego/adm/domain/User.java` — UUID v4 `id`, unique `username`,
      `displayName`, non-blank `password` matching `[a-zA-Z0-9._-]+`, `status` (`UserStatus`),
      `@Version` `version`, UTC timestamps — depends on T007
- [ ] T011 Create the `RolePermission` mapping in
      `src/main/java/com/allensandiego/adm/domain/RolePermission.java` with composite PK
      (`role_id`, `permission_id`) and cascade delete from both parents — depends on T008, T009
- [ ] T012 Create the `UserRole` assignment in
      `src/main/java/com/allensandiego/adm/domain/UserRole.java` with composite PK
      (`user_id`, `role_id`) enforcing at most one assignment per pair (FR-007) — depends on
      T009, T010
- [ ] T013 [P] Create the supporting `AuditEvent` entity and `AuditAction` enum in
      `src/main/java/com/allensandiego/adm/domain/AuditEvent.java` and
      `src/main/java/com/allensandiego/adm/domain/AuditAction.java` — UUID `id`, nullable
      `actorId` FK -> `User`, `action`, `targetType`, `targetId`, `before`/`after` JSON,
      UTC `occurredAt`; supporting table only, not a sixth RBAC entity (FR-014, research D-5)
- [ ] T014 Create `PermissionRepository` in
      `src/main/java/com/allensandiego/adm/domain/PermissionRepository.java` with
      `existsByCode`, paged `findAll`, and the effective-permission projection query — depends
      on T008
- [ ] T015 Create `RoleRepository` in
      `src/main/java/com/allensandiego/adm/domain/RoleRepository.java` with `existsByName`,
      `countByIsProtectedTrue`, and active-permission lookups — depends on T009
- [ ] T016 Create `UserRepository` in
      `src/main/java/com/allensandiego/adm/domain/UserRepository.java` with
      `existsByUsername` and paged search — depends on T010
- [ ] T017 Create `RolePermissionRepository` in
      `src/main/java/com/allensandiego/adm/domain/RolePermissionRepository.java` —
      depends on T011
- [ ] T018 Create `UserRoleRepository` in
      `src/main/java/com/allensandiego/adm/domain/UserRoleRepository.java` — depends on T012
- [ ] T019 Create `AuditEventRepository` in
      `src/main/java/com/allensandiego/adm/domain/AuditEventRepository.java` — depends on T013
- [ ] T020 Implement `AuditService` in
      `src/main/java/com/allensandiego/adm/service/AuditService.java` exposing
      `record(actorId, action, targetType, targetId, before, after)` that appends an
      `AuditEvent` with a UTC `Instant.now()` — depends on T013, T019 (FR-014)
- [ ] T021 Implement `EffectivePermissionService` in
      `src/main/java/com/allensandiego/adm/service/EffectivePermissionService.java` resolving
      `permissions_of(user) = union of active permissions across assigned roles` in a single
      query; `INACTIVE` users resolve to an empty set (FR-008, SC-005) — depends on T014, T015,
      T016, T017, T018
- [ ] T022 Implement `GuardrailService` in
      `src/main/java/com/allensandiego/adm/service/GuardrailService.java` with
      `isLastProtectedRole(roleId)`, `wouldRemoveLastProtectedAssignment(userId, roleId)`, and
      `isLastActiveProtectedUser(userId)` backing G1/G2/G3 — depends on T015, T016, T018
      (FR-010, research D-3)
- [ ] T023 Implement `PermissionResolver` in
      `src/main/java/com/allensandiego/adm/security/PermissionResolver.java` resolving the
      current principal's effective permission set per request via
      `EffectivePermissionService` — depends on T021 (research D-1)
- [ ] T024 Create `SecurityConfig` in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java`: a
      `SecurityFilterChain` bean, `permitAll` only for `/login`, `/css/**`, `/js/**`,
      `/assets/**`, `/error`, and deny-by-default
      `authorizeHttpRequests(...).anyRequest().access(<AuthorizationManager backed by
      PermissionResolver>)` so permissions resolve BEFORE controller dispatch — depends on
      T006, T023 (FR-011, constitution Principle II, research D-1)
- [ ] T025 Expose resolved permissions to the view layer in
      `src/main/java/com/allensandiego/adm/security/PermissionViewModel.java` — a Thymeleaf
      `@ControllerAdvice`/argument resolver that puts the permission set on the request so
      fragments render only permitted menu items and buttons; cosmetic only, never the security
      boundary — depends on T023 (research D-2)
- [ ] T026 Implement the idempotent `DataSeeder` in
      `src/main/java/com/allensandiego/adm/config/DataSeeder.java`: upsert all 13 permission
      codes from T006 with their paths, the protected "Super Admin" role carrying every code, and
      one seeded administrator user with a password from `app.seed.admin-password`; safe on every
      startup — depends on T006, T014, T015, T016, T020
      (research D-7, spec Assumptions)

**Checkpoint**: Entities, repositories, permission resolution, and the fail-closed seam are
ready — user story implementation can begin

---

## Phase 3: User Story 1 - Manage the Permission Catalog (Priority: P1) 🎯 MVP

**Goal**: An authorized administrator can list, view, create, edit, deactivate, and reactivate
permissions; anyone else is refused; duplicate and blank names are rejected.

**Independent Test**: Sign in as an authorized administrator, create a permission with a unique
code, see it listed, deactivate it, and confirm an administrator lacking `permission.*` is
refused the Permissions screen.

### Tests for User Story 1 (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T027 [P] [US1] Dual-sided authorization tests in
      `src/test/java/com/allensandiego/adm/security/PermissionAuthorizationTests.java`:
      parameterized over every `/permissions*` route in `contracts/permissions.md` — a principal
      holding the required code gets 200, one holding all other codes gets 403, anonymous is
      redirected to `/login` (constitution Principle IV, SC-003)
- [ ] T028 [P] [US1] Service tests in
      `src/test/java/com/allensandiego/adm/service/PermissionServiceTests.java`: blank code and
      blank label rejected, duplicate code rejected with a clear message, code pattern
      `[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*` enforced, deactivation excluded from the effective
      set but leaving existing role grants stored, reactivation restores it (FR-002/FR-003)
- [ ] T029 [P] [US1] Contract tests in
      `src/test/java/com/allensandiego/adm/web/PermissionControllerTests.java` covering all five
      endpoints in `contracts/permissions.md` including 404 for an unknown id and 400 for
      invalid input, with `code` immutable after creation

### Implementation for User Story 1

- [ ] T030 [P] [US1] Create the validated `PermissionForm` record in
      `src/main/java/com/allensandiego/adm/web/form/PermissionForm.java` with Jakarta Bean
      Validation on `code` (non-blank, <=80, pattern), `label` (non-blank, <=120), and `path`
      (non-blank, <=120) (FR-012, research D-8)
- [ ] T031 [US1] Implement `PermissionService` in
      `src/main/java/com/allensandiego/adm/service/PermissionService.java`: paged list, detail,
      create with duplicate detection, update `label`/`path`/`active`, and an `AuditService`
      call per mutation — depends on T014, T020, T030 (FR-001/FR-002/FR-003)
- [ ] T032 [US1] Implement `PermissionController` in
      `src/main/java/com/allensandiego/adm/web/PermissionController.java` with the five
      endpoints from `contracts/permissions.md`; no authorization checks here — the
      `AuthorizationManager` already gated the request — depends on T024, T031
- [ ] T033 [P] [US1] Create the permission list view in
      `src/main/resources/templates/permissions/list.html` using the CoreUI admin layout, with
      search, paging, and an active/inactive status badge on each row
- [ ] T034 [P] [US1] Create the permission form view in
      `src/main/resources/templates/permissions/form.html` with inline validation errors and a
      summary block for a rejected submission, including the path field
- [ ] T035 [P] [US1] Create the permission detail view in
      `src/main/resources/templates/permissions/detail.html` showing `code`, `label`, `path`,
      active state, and the deactivate/reactivate action
- [ ] T036 [P] [US1] Add the Permissions sidebar entry and its create button to
      `src/main/resources/templates/fragments/sidebar.html`, rendered only when
      `permission.view`/`permission.create` are present in the resolved set (research D-2)

**Checkpoint**: User Story 1 fully functional and independently testable — this is the MVP

---

## Phase 4: User Story 2 - Manage Roles and Edit Role Permissions (Priority: P2)

**Goal**: An authorized administrator can create, view, rename, and delete roles and grant or
revoke permissions on a role in a single screen; the final protected role cannot be deleted.

**Independent Test**: Create a role, grant a set of permissions, save, reopen the role, and
confirm the saved set matches exactly; then confirm the final Super Admin role refuses
deletion.

### Tests for User Story 2 (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T037 [P] [US2] Dual-sided authorization tests in
      `src/test/java/com/allensandiego/adm/security/RoleAuthorizationTests.java`:
      parameterized over every `/roles*` route in `contracts/roles.md` — permitted 200,
      unauthorized 403, anonymous redirected (constitution Principle IV)
- [ ] T038 [P] [US2] Service tests in
      `src/test/java/com/allensandiego/adm/service/RoleServiceTests.java`: blank and duplicate
      role names rejected, rename preserves existing assignments, delete cascades its
      `role_permissions` and `user_roles` rows cleanly, deleting a role leaves other roles and
      users intact (FR-004, spec Edge Cases)
- [ ] T039 [P] [US2] Contract tests in
      `src/test/java/com/allensandiego/adm/web/RoleControllerTests.java` covering all seven
      endpoints in `contracts/roles.md` including 404 for an unknown id
- [ ] T040 [P] [US2] Guardrail G1 tests in
      `src/test/java/com/allensandiego/adm/guardrails/RoleGuardrailTest.java`: deleting the last
      protected role returns 409 with no state change; deleting a non-protected role succeeds
      (FR-010, SC-004)
- [ ] T041 [P] [US2] Concurrency tests in
      `src/test/java/com/allensandiego/adm/service/RoleConcurrencyTests.java`: two overlapping
      role-permission saves — the second is refused with 409 and the first write stands, so no
      silent overwrite occurs (FR-013, research D-4)

### Implementation for User Story 2

- [ ] T042 [P] [US2] Create the validated `RoleForm` record in
      `src/main/java/com/allensandiego/adm/web/form/RoleForm.java` (non-blank `name` <=80,
      optional `description` <=255) — depends on T030
- [ ] T043 [P] [US2] Create the `RolePermissionsForm` record in
      `src/main/java/com/allensandiego/adm/web/form/RolePermissionsForm.java` carrying the full
      submitted set of permission ids for POST-replace semantics — depends on T030
- [ ] T044 [US2] Implement `RoleService` in
      `src/main/java/com/allensandiego/adm/service/RoleService.java`: paged list, detail, create
      with duplicate detection, rename under optimistic locking, and delete guarded by G1 with
      an `AuditService` call per mutation — depends on T015, T020, T022, T042
      (FR-004, FR-010)
- [ ] T045 [US2] Implement `RolePermissionService` in
      `src/main/java/com/allensandiego/adm/service/RolePermissionService.java`: replace the
      role's whole mapping from the submitted set, refuse to newly grant an inactive permission,
      detect concurrent edits via the `Role.version` column, and audit before/after — depends
      on T011, T017, T022, T043 (FR-005, FR-003, FR-013)
- [ ] T046 [US2] Implement `RoleController` in
      `src/main/java/com/allensandiego/adm/web/RoleController.java` with the seven endpoints
      from `contracts/roles.md` — depends on T024, T044, T045
- [ ] T047 [P] [US2] Create the role list view in
      `src/main/resources/templates/roles/list.html` with search, paging, and a protected-role
      marker
- [ ] T048 [P] [US2] Create the role form view in
      `src/main/resources/templates/roles/form.html` with inline validation errors
- [ ] T049 [P] [US2] Create the role detail view in
      `src/main/resources/templates/roles/detail.html` showing the carried permission set
- [ ] T050 [P] [US2] Create the role-permission editor in
      `src/main/resources/templates/roles/permissions-editor.html`: one screen listing all
      ACTIVE permissions as checkboxes reflecting the current set, inactive permissions rendered
      read-only/unchecked, and the resulting set shown before save (FR-005, FR-003)

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 - Manage Users and Assign Roles (Priority: P3)

**Goal**: An authorized administrator can create, view, edit, activate/deactivate users and
assign roles, with the effective permission set always shown as the union of assigned roles'
permissions; the last administrator cannot be stripped of access.

**Independent Test**: Create a user, assign one or more roles, and confirm the derived access
equals the union of those roles' permissions; then confirm the final administrator cannot be
demoted or deactivated.

### Tests for User Story 3 (write FIRST, ensure they FAIL before implementation) ⚠️

- [ ] T051 [P] [US3] Dual-sided authorization tests in
      `src/test/java/com/allensandiego/adm/security/UserAuthorizationTests.java`:
      parameterized over every `/users*` route in `contracts/users.md` — permitted 200,
      unauthorized 403, anonymous redirected (constitution Principle IV)
- [ ] T052 [P] [US3] Service tests in
      `src/test/java/com/allensandiego/adm/service/UserServiceTests.java`: blank and duplicate
      usernames rejected, username pattern `[a-zA-Z0-9._-]+` and <=64 enforced, display name
      non-blank <=120, password validation `[a-zA-Z0-9._-]+` enforced, deactivation leaves
      assignments stored, reactivation restores access (FR-006, FR-012)
- [ ] T053 [P] [US3] Contract tests in
      `src/test/java/com/allensandiego/adm/web/UserControllerTests.java` covering all seven
      endpoints in `contracts/users.md` including 404 for an unknown id
- [ ] T054 [P] [US3] Guardrail G2/G3 tests in
      `src/test/java/com/allensandiego/adm/guardrails/UserGuardrailTest.java`: removing the last
      protected-role assignment (including self-demotion) returns 409; deactivating the last
      ACTIVE protected-role holder returns 409; neither changes any state (FR-010, SC-004)
- [ ] T055 [P] [US3] Effective-permission tests in
      `src/test/java/com/allensandiego/adm/service/EffectivePermissionTests.java`: a user with
      multiple roles resolves the exact union of active permissions, assigning the same role
      twice yields a single assignment, an inactive permission drops out, and an `INACTIVE` user
      resolves to an empty set (FR-007, FR-008, SC-005)

### Implementation for User Story 3

- [ ] T056 [P] [US3] Create the validated `UserForm` record in
      `src/main/java/com/allensandiego/adm/web/form/UserForm.java` (`username` non-blank <=64
      matching `[a-zA-Z0-9._-]+`, `displayName` non-blank <=120, `password` non-blank matching
      `[a-zA-Z0-9._-]+`) — depends on T030
- [ ] T057 [P] [US3] Create the `UserRolesForm` record in
      `src/main/java/com/allensandiego/adm/web/form/UserRolesForm.java` carrying the full
      submitted set of role ids for POST-replace semantics — depends on T030
- [ ] T058 [US3] Implement `UserService` in
      `src/main/java/com/allensandiego/adm/service/UserService.java`: paged searchable list,
      detail with effective permissions, create with duplicate detection and initial password
      storage per data-model.md, edit display name under optimistic locking, and activate/deactivate
      guarded by G3 — depends on T016, T020, T022, T056 (FR-006, FR-008)
- [ ] T059 [US3] Implement `UserRoleService` in
      `src/main/java/com/allensandiego/adm/service/UserRoleService.java`: replace the user's
      whole assignment set, reject unknown role ids, refuse self-demotion and any removal that
      would empty the last protected-role assignment (G2), and audit before/after — depends on
      T012, T018, T022, T057 (FR-007, FR-010)
- [ ] T060 [US3] Implement `UserController` in
      `src/main/java/com/allensandiego/adm/web/UserController.java` with the seven endpoints
      from `contracts/users.md`, passing the resolved effective permission set to the detail
      view — depends on T021, T024, T058, T059
- [ ] T061 [P] [US3] Create the user list view in
      `src/main/resources/templates/users/list.html` with search, paging, status badges, and
      assigned-role chips
- [ ] T062 [P] [US3] Create the user form view in
      `src/main/resources/templates/users/form.html` with inline validation errors including
      the password field
- [ ] T063 [P] [US3] Create the user detail view in
      `src/main/resources/templates/users/detail.html` showing account fields, status, assigned
      roles, the consolidated effective permission list, the role-assignment multi-select, and
      the activate/deactivate action (FR-008)

**Checkpoint**: All user stories independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cross-story audit/fail-closed coverage, hardening, and end-to-end validation

- [ ] T064 [P] Audit completeness tests in
      `src/test/java/com/allensandiego/adm/audit/AuditTest.java`: every permission, role,
      role-permission, user, and user-role mutation writes an `AuditEvent` carrying actor, UTC
      timestamp, and before/after values (FR-014, SC-006)
- [ ] T065 [P] Fail-closed tests in
      `src/test/java/com/allensandiego/adm/security/FailClosedTest.java`: anonymous requests to
      every non-whitelisted path are redirected to `/login`, a deactivated principal gets 403 on
      every protected path with no protected markup in the body, and no protected content is
      disclosed to a refused request (FR-011, SC-003)
- [ ] T066 [P] Concurrency tests in
      `src/test/java/com/allensandiego/adm/service/UserConcurrencyTests.java`: overlapping user
      edits and role-assignment saves — the second is refused with 409, no silent overwrite
      (FR-013)
- [ ] T067 [P] Security hardening in
      `src/main/java/com/allensandiego/adm/security/SecurityConfig.java` and
      `src/main/resources/application.properties`: CSRF enabled on all state-changing routes,
      `Cache-Control: no-store` on authenticated responses, and a final sweep confirming no
      permission code is hardcoded outside `Permissions.java` (FR-015)
- [ ] T068 [P] Documentation refresh: update
      `specs/001-rbac-user-management/quickstart.md` run commands and the endpoint tables in
      `specs/001-rbac-user-management/contracts/` if the implementation diverged; there is no
      `HELP.md` in this repository, so do not create one
- [ ] T069 Run `./mvnw test` and execute the
      `specs/001-rbac-user-management/quickstart.md` scenarios (5-minute setup walkthrough and
      the full automated suite); resolve failures until SC-001..SC-006 all pass

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: After Foundational. Owns the `Permission` entity end to end; no dependency on
  US2/US3.
- **US2 (P2)**: After Foundational. Consumes US1's `Permission` catalog when rendering the
  role-permission editor; independently testable once wired.
- **US3 (P3)**: After Foundational. Consumes US1's catalog and US2's `Role` definitions when
  rendering effective permissions; independently testable once wired.

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Forms and entities before services; services before controllers/views
- Core implementation before integration

### Parallel Opportunities

- T002–T005 in Setup can run together
- T006–T010, T013–T019, and T025 in Foundational can run together (T011/T012 and T020–T024 are
  sequential chains)
- T027–T029 (US1 tests) can run together; T037–T041 (US2 tests) can run together; T051–T055
  (US3 tests) can run together
- Within a story, the Thymeleaf view tasks are independent of each other and of the service
- T064–T068 in Polish can run together (T069 is the final serial gate)

---

## Parallel Example: User Story 2

```bash
# Launch all US2 tests together:
Task: "Dual-sided authorization tests in src/test/java/com/allensandiego/adm/security/RoleAuthorizationTests.java"
Task: "Service tests in src/test/java/com/allensandiego/adm/service/RoleServiceTests.java"
Task: "Contract tests in src/test/java/com/allensandiego/adm/web/RoleControllerTests.java"
Task: "Guardrail G1 tests in src/test/java/com/allensandiego/adm/guardrails/RoleGuardrailTest.java"
Task: "Concurrency tests in src/test/java/com/allensandiego/adm/service/RoleConcurrencyTests.java"

# Launch all US2 views together once the services land:
Task: "Create the role list view in src/main/resources/templates/roles/list.html"
Task: "Create the role form view in src/main/resources/templates/roles/form.html"
Task: "Create the role detail view in src/main/resources/templates/roles/detail.html"
Task: "Create the role-permission editor in src/main/resources/templates/roles/permissions-editor.html"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: create a permission, list it, deactivate it, confirm an unauthorized
   administrator is refused the screen
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → validate permission catalog management → MVP
3. US2 → validate role management and the role-permission editor
4. US3 → validate user management, role assignment, and effective permissions
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then:
   - Developer A: US1 (permission catalog)
   - Developer B: US2 (roles + editor), after US1's `Permission` entity
   - Developer C: US3 (users + assignment), after US2's `Role` entity
3. Stories integrate through the shared `SecurityConfig` and `Permissions.java` — run T024
   before any story work and avoid editing it afterwards

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps a task to its user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing; commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- All paths use the real base package `com.allensandiego.adm`; there is no `com.example.rbac`
  tree in this repository
- Test artifact names matter: the Spring Security test dependency is
  `org.springframework.security:spring-security-test` (see T001)
- `audit_event` is a supporting sixth table required by FR-014, not a sixth RBAC entity — see
  the Complexity Tracking table in `plan.md`
- Feature `002` will add the auth_event table and full login authentication pipeline; user password storage is initialized here per data-model.md
