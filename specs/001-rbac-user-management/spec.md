# Feature Specification: RBAC User Management

**Feature Branch**: `001-rbac-user-management`

**Created**: 2026-09-21

**Status**: Draft

**Input**: User description: "Add permission, role, user management with role assignment and role permission editing."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Manage the Permission Catalog (Priority: P1)

An administrator opens the Permissions screen and sees every permission the system
understands. They can create a new permission (e.g., "Export Reports"), view its details,
deactivate it, or reactivate it. Permission names are unique and shown consistently across
roles and users.

**Why this priority**: Permissions are the root of the model — roles grant permissions and
users receive them through roles. Nothing else can be governed meaningfully until the
permission catalog can be managed.

**Independent Test**: Can be fully tested by an authorized admin creating a permission,
seeing it listed, deactivating it, and confirming an unauthorized admin is refused access to
the Permissions screen.

**Acceptance Scenarios**:

1. **Given** I am an authorized administrator, **When** I create a permission with a unique
   name, **Then** it appears in the permission list and can be referenced when editing roles.
2. **Given** I am an authorized administrator, **When** I create a permission with a
   duplicate or blank name, **Then** the system refuses it and shows a clear error message
   without creating anything.
3. **Given** a deactivated permission, **When** an administrator opens the Permissions
   screen, **Then** it is clearly marked as inactive and cannot be newly granted to roles.
4. **Given** I lack the permission to manage permissions, **When** I try to open the
   Permissions screen or perform any permission change, **Then** I am blocked and see a
   "not authorized" message rather than the screen.

---

### User Story 2 - Manage Roles and Edit Role Permissions (Priority: P2)

An administrator manages roles: creating a new role (e.g., "Report Viewer"), renaming it,
and choosing which permissions it carries. The role-permission editor lets them grant or
revoke permissions in a single screen with immediate, visible confirmation of the final
permission set.

**Why this priority**: Roles translate raw permissions into meaningful, reusable policies.
Editing role permissions is the second core capability named in the request.

**Independent Test**: Can be fully tested by an authorized admin creating a role, granting a
set of permissions, saving, and reopening the role to confirm the saved set is exactly what
they configured.

**Acceptance Scenarios**:

1. **Given** I am an authorized administrator, **When** I create a role and attach several
   permissions, **Then** the role is saved with exactly those permissions and can be assigned
   to users.
2. **Given** I am an authorized administrator, **When** I remove a permission from a role,
   **Then** the change is saved and users holding that role no longer receive that permission.
3. **Given** there is exactly one remaining "Super Admin" role containing all users with
   administrative access, **When** I attempt to delete it or remove its final
   administrative user, **Then** the system blocks the action with a clear warning and
   nothing changes.
4. **Given** I lack the permission to manage roles, **When** I attempt to create, rename, or
   edit a role's permissions, **Then** I am blocked and see a "not authorized" message.

---

### User Story 3 - Manage Users and Assign Roles (Priority: P3)

An administrator manages user accounts: creating a user, viewing account details, activating
or deactivating them, and assigning one or more roles. The screen always shows what a user
can do — the effective permission set derived from their roles — so the administrator can
verify access before saving.

**Why this priority**: Assigning roles to users delivers the end-to-end value of the system
(governing who can do what) and depends on the definition side being in place.

**Independent Test**: Can be fully tested by an authorized admin creating a user, assigning a
role, and confirming the user's derived access matches the role's permissions.

**Acceptance Scenarios**:

1. **Given** I am an authorized administrator, **When** I create a user, assign one or more
   roles, and save, **Then** the user's effective permissions are the exact union of the
   permissions of their assigned roles.
2. **Given** a user assigned to multiple roles, **When** I review their profile, **Then** I
   see all assigned roles and the consolidated list of effective permissions.
3. **Given** I deactivate a user, **When** that user next tries to use the system, **Then**
   they are treated as not authorized even though their roles remain assigned.
4. **Given** I lack the permission to manage users, **When** I attempt to create or edit a
   user, **Then** I am blocked and see a "not authorized" message.

---

### Edge Cases

- A permission that is still granted to roles is deactivated or deleted — the change must
  take effect consistently and the permission must not remain silently in use.
- A role is renamed — historical assignments must keep working; no assignment is lost.
- A user is assigned the same role twice — the system must treat it as a single assignment.
- The final administrative user attempts to remove their own administrative role — the
  action must be blocked (lockout protection).
- A role currently assigned to users is deleted — the mapping is removed cleanly and users
  lose that role's permissions; this never breaks other roles or users.
- Two administrators edit the same role's permissions at the same time — the system must
  prevent silent overwrite (the later edit must be warned or refused).
- An administrator attempts to open a management screen they are not permitted to see —
  they are blocked, and the screen content is never rendered or disclosed.
- A user is deactivated while the administrator is viewing them — saved changes must respect
  the current state and present no stale data.
- Large catalogs — long lists of permissions, roles, or users must remain navigable with
  search and paging without losing the current selection.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow authorized administrators to view a list of permissions and
  open individual permission details.
- **FR-002**: System MUST allow authorized administrators to create a new permission with the
  unique, meaningful name; blank or duplicate names MUST be rejected with an explanatory
  message.
- **FR-003**: System MUST allow authorized administrators to deactivate and reactivate a
  permission; deactivated permissions MUST not be newly grantable to roles.
- **FR-004**: System MUST allow authorized administrators to create a role, view role
  details, rename it, and delete it.
- **FR-005**: System MUST provide a role-permission editor that lets authorized
  administrators grant and revoke permissions on a role in a single screen, showing the
  resulting permission set before saving.
- **FR-006**: System MUST allow authorized administrators to create a user, view user
  details, edit account fields, and activate or deactivate the account.
- **FR-007**: System MUST allow authorized administrators to assign one or more roles to a
  user and revoke existing assignments; each role MUST be assigned at most once per user.
- **FR-008**: System MUST show, for any user, the effective permission set derived as the
  union of the permissions of their assigned roles, and MUST keep that view consistent with
  the stored configuration for that user.
- **FR-009**: System MUST apply every permission change and role assignment immediately and
  consistently so that protected screens reflect the latest configuration.
- **FR-010**: System MUST refuse, with a clear warning and no data change, any action that
  would delete the final "Super Admin" role or remove the final user with administrative
  access from it.
- **FR-011**: System MUST block, by default, any administrator who lacks the specific
  permission required for a screen or action; blocked attempts MUST return a clear
  "not authorized" result and MUST NOT disclose the protected content.
- **FR-012**: System MUST validate all submitted input (names, selections, and edits) and
  reject invalid submissions with clear messages before any change is applied.
- **FR-013**: System MUST prevent silent concurrent overwrites when two administrators edit
  the same role simultaneously, warning or refusing the later save.
- **FR-014**: System MUST record every permission definition change, role-permission change,
  user-role assignment or removal, and user account change with the actor, timestamp, and
  before/after values for auditability.
- **FR-015**: Permissions MUST be identified by a stable, centralized identifier that is
  reused consistently across roles and user-role resolution; no ad-hoc or implicit grants
  are permitted.

### Key Entities *(include if feature involves data)*

- **User**: A person with a login account; carries account fields (name, username, password,
  status) that determine whether they can act in the system.
- **Role**: A named, reusable bundle of permissions representing a policy (e.g., "Report
  Viewer"); contains a visible name and the list of permissions it carries.
- **Permission**: A single, named capability in the system (e.g., "Export Reports") with an
  associated protected path; the atomic unit that roles grant and users ultimately receive.
- **Role-Permission**: The association defining which permissions a role carries; changing
  it alters the effective access of every user holding the role.
- **User-Role**: The association defining which roles a user has been assigned; together
  with Role-Permission it determines the user's effective permissions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authorized administrator can complete the full end-to-end setup — create a
  permission, create a role with that permission, create a user, and assign the role — in
  under 5 minutes.
- **GDPR Compliance**: All audit logs (FR-014) retained for minimum 7 years; data minimization enforced; right-to-erasure requests processable within 30 days; cross-border transfer controls documented.
- **SC-002**: The most frequent task, assigning a role to an existing user, can be completed
  by an authorized administrator in under 2 minutes.
- **SC-003**: Permission enforcement is dual-sided and complete: for every management screen,
  authorized administrators succeed while unauthorized administrators are blocked, verified
  for 100% of configured permission boundaries.
- **SC-004**: 100% of attempts to delete the final "Super Admin" role or remove its final
  administrative user are blocked; the system can never be left without administrative
  access.
- **SC-005**: A user's effective permissions always match exactly the union of the
  permissions of their assigned roles; 100% consistency between the stored configuration and
  what each user can actually do.
- **SC-006**: Every permission, role, and assignment change is auditable; 100% of changes
  are recorded with actor, timestamp, and before/after values so security events can be
  reviewed.

## Assumptions

- Administrators are already authenticated by the existing login flow; the authentication
  mechanism itself is out of scope for this feature.
- GDPR compliance applies: audit logging minimum 7 years, data minimization enforced, right-to-erasure processable within 30 days, cross-border transfer controls implemented.
- A seeded "Super Admin" role with full access exists at launch and is the protected role
  referenced by FR-010.
- The system serves a single organization — no multi-tenant or cross-organization data
  partitioning is in scope.
- Permission identifiers follow one centralized, human-readable naming convention for the
  entire system; the catalog is the single source of truth.
- Standard web-app responsiveness applies: management screens load and respond within a
  couple of seconds under normal usage.
- Password self-service and reset features are out of scope; administrator-provided passwords at user creation are stored per data-model.md.
- Management screens provide search and paging for large lists.