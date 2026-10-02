<!--
Sync Impact Report
- Version change: 2.1.0 -> 3.0.0
- Modified principles: III. Tech Stack & Engine (PostgreSQL and SQL-managed schema)
- Added sections: none
- Removed sections: none
- Follow-up TODOs: none
-->

# RBAC Admin Web App Constitution

## Core Principles

### I. Architectural Boundaries

The application MUST be a single-system architecture. It MUST manage exactly five core domain
entities: Users, Roles, Permissions, Role-Permissions (mapping), and User-Roles (assignments).

There MUST be clean data separation between authorization definitions (Roles/Permissions) and
identity assignments (Users/User-Roles). Definition rows MUST be grantable and reusable
independently of any specific user; assignment rows MUST reference definitions by key only and
MUST never inline definition content.

Rationale: Decoupling definitions from assignments keeps policy auditable, versionable, and
reusable across users while identities remain a pure membership concern.

### II. Security & Authorization (Fail-Closed)

All endpoints and UI components MUST be locked down by default. No route, controller,
Thymeleaf view, or fragment is accessible without an explicit, verified permission. Access MUST
be granted only when the requesting user's active permissions demonstrably include the
required authority.

Middleware enforcement is non-negotiable: every API request MUST pass through an authorization
layer (Spring Security filter chain) that resolves the user's active permissions BEFORE the
request reaches any controller. Controllers MUST NOT act as the primary authorization gate.

Mutation Safety MUST guard against accidental lockout. The system MUST prevent deletion of the
final permitted 'Super Admin' role and MUST prevent removing the last user holding
administrative permissions, including self-demotion. Such mutations MUST be rejected with a
conflict-style error and MUST NEVER silently succeed.

Rationale: Single points of failure on privilege elevation (one role, one admin user) can
permanently and irreversibly lock every operator out of the system.

### III. Tech Stack & Engine

The server MUST be Java 21 LTS or newer built with Spring Boot. Thymeleaf MUST render
server-side views using the CoreUI Admin Bootstrap 5 template. PostgreSQL MUST be used for
persistence through Spring Data JPA and JDBC. Hibernate schema generation MUST be disabled;
the SQL initialization scripts define and seed the schema. All primary keys MUST be UUIDs
(v4 or v7).

Jakarta Bean Validation MUST be applied to every inbound API payload and every data mutation;
no unvalidated data MAY be written to the persistence layer.

### IV. Testing Standards

Authorization testing is mandatory. Test suites MUST explicitly verify both sides of every
permission boundary: permitted roles receive 200 OK, and unauthorized roles receive 403
Forbidden. Lockout guardrails MUST be covered by tests proving the system cannot be driven
into a state with zero administrative access.

Rationale: Permission regressions are silent and dangerous; only dual-sided assertions prove a
boundary is both open to the right actors and closed to everyone else.

### V. Build & Execution (NON-NEGOTIABLE)

All build, compile, test, and execution commands MUST use the Maven wrapper (`./mvnw`) for
consistency across environments. NEVER invoke `mvn` directly from system PATH; agents must
always use `./mvnw {command}` to ensure correct dependency resolution and build reproducibility.

## Domain Model & Relational Integrity

- Single logical database schema. There MUST be no multi-tenant walls or cross-tenant data
  partitioning of any kind.
- The schema MUST contain exactly five core tables: users, roles, permissions,
  role_permissions (mapping), and user_roles (assignment).
- Authorization definitions (roles, permissions) MUST live in definition tables; identity
  assignments (users, user_roles) MUST live in assignment tables. Strict foreign keys MUST
  link assignments to definitions; orphaned or dangling references are forbidden.
- All foreign keys MUST be declared with cascade rules for clean relational teardowns.
  Removal of a definition MUST cascade to its mapping/assignment rows. Lockout-critical rows
  (the final Super Admin role and the final administrative user) MUST be protected by
  application-level guardrails per Principle II and MUST NOT be cascade-deletable.
- Primary keys MUST be UUIDs (v4 or v7) across all tables. Timestamps MUST be stored with
  UTC semantics to support audit trails.

## Security Implementation Standards

- Spring Security MUST be configured deny-by-default. Only explicitly whitelisted endpoints
  (login, static assets, error page) MAY be publicly reachable; everything else requires
  authentication and permission verification.
- The active permission set for a request MUST be resolved from User-Roles -> Roles ->
  Permissions in the middleware and MUST be computed before controller dispatch. Resolved
  permissions MUST be made available to views so server-rendered UI components (menu items,
  buttons, fragments) reflect the same authority the backend enforces.
- Server-side enforcement is the source of truth. UI hiding of unauthorized components is
  cosmetic and MUST NEVER be treated as a security boundary.
- Every permission interaction (grant, revoke, assignment, removal) MUST be rejected
  harmlessly when it would hollow out the Super Admin role or empty it of users.
- Permission identifiers MUST be explicit and centralized; no magic strings or implicit
  grants are permitted.

## Governance

This constitution supersedes all other conventions and ad-hoc practices. Every feature,
schema change, and endpoint MUST comply with these principles; deviations require a formal
amendment, not a local override.

Amendment procedure: any amendment MUST be proposed with the principle change, its rationale,
and an adoption/migration plan, then reviewed and adopted before implementation.

Versioning policy: Semantic Versioning applies. MAJOR for removal or redefinition of a
principle; MINOR for an added principle/section or materially expanded guidance; PATCH for
clarifications and wording refinements.

Compliance review: every feature specification, data migration, security change, and test
plan MUST be reviewed against Principles I-IV before adoption. Any new or modified endpoint
or permission change MUST include explicit 200 OK / 403 Forbidden authorization tests or be
rejected.

**Version**: 3.0.0 | **Ratified**: 2026-09-21 | **Last Amended**: 2026-10-02