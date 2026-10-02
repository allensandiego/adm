# Research & Decisions: RBAC User Management

**Date**: 2026-09-21 (refreshed 2026-09-26)

## Scope

The feature spec defines permission/role/user management with role assignment and
role-permission editing. The constitution (v2.0.0) fully pins the runtime stack, so the only
open questions were integration patterns and enforcement mechanics. There were no codebase or
platform unknowns to research (fresh repository; single stack prescribed).

## Decisions

### D-1: Permission enforcement — centralized filter-chain resolution

- **Decision**: Resolve the actor's effective permission set (User-Roles -> Roles ->
  Permissions) once, in the Spring Security layer (an AuthorizationManager on the
  filter chain), before controller dispatch. Controllers receive the resolved authority and
  never perform as the primary gate.
- **Rationale**: Constitution Principle II mandates middleware enforcement and denies
  controller-as-gate; a single resolution point keeps logic centralized and auditable.
- **Alternatives considered**:
  - Method-level `@PreAuthorize` on every controller — rejected: scatters authority
    expressions, still runs at the AOP layer but duplicates resolution; the constitution
    explicitly requires resolution before the controller.
  - Per-controller manual checks — rejected: violates fail-closed and centralization.

### D-2: UI visibility vs. enforcement

- **Decision**: Resolved permissions are attached to the request and exposed to Thymeleaf
  fragments so menu items, buttons, and links are rendered only for permitted actions.
  Server-side enforcement remains the source of truth.
- **Rationale**: Constitution Security Implementation Standards.
- **Alternatives considered**: Client-side-only hiding — rejected, cosmetic only.

### D-3: Lockout protection — protected-role flag + service guardrails

- **Decision**: Mark the seeded "Super Admin" role as protected (`isProtected=true`).
  Service-level guardrails reject any mutation that would (a) delete the last protected role,
  (b) remove the last assignment of a protected role, or (c) deactivate the last active user
  holding a protected role. Rejected with a conflict-style error; no data change.
- **Rationale**: Constitution Principle II and Domain Model ("application-level guardrails";
  lockout-critical rows "MUST NOT be cascade-deletable").
- **Alternatives considered**: DB-level triggers — rejected: policy belongs in the application
  per the constitution; relational constraints remain appropriate for data integrity.

### D-4: Concurrency — optimistic locking

- **Decision**: Use a version column on Roles (and Users) with JPA optimistic locking; a save
  that would overwrite a concurrent administrator's edit is refused with a conflict message
  (FR-013).
- **Rationale**: Cheap, standard, covers the dual-admin edit edge case.
- **Alternatives considered**: Pessimistic row locks — rejected: complexity unjustified for
  admin-console scale.

### D-5: Audit trail — dedicated supporting table

- **Decision**: Append structured audit records (actor, action, target, before/after, UTC
  timestamp) to a dedicated `audit_event` table (FR-014).
- **Rationale**: Constitution mandates "exactly five core tables" for the RBAC domain; the
  audit log is a supporting, non-RBAC table required by FR-014 and the governance compliance
  expectation. It introduces no new RBAC entity and does not affect permission resolution.
- **Alternatives considered**: App-log-only — rejected, not auditable/reviewable; JPA entity
  listeners appending rows — chosen mechanism (audit as persistence concern, not user flow).

### D-6: Embedding UI with CoreUI + Thymeleaf

- **Decision**: Lay out CoreUI Admin Bootstrap 5 static assets (CSS/JS) under static resources
  and build server-rendered Thymeleaf fragments (sidebar/nav) driven by resolved permissions.
- **Rationale**: Constitution Principle III; server-rendered means authority is available at
  render time with no client round-trip.
- **Alternatives considered**: Plain Bootstrap (non-admin) — rejected, loses the required
  admin template; JS SPA rendering — rejected by the Thymeleaf mandate.

### D-7: Schema management

- **Decision**: PostgreSQL with Hibernate `ddl-auto=none`; `drop.sql`, `schema.sql`, and
  `data.sql` initialize the schema and seed data at startup. No migration framework is in
  play.
- **Rationale**: Constitution Principle III requires PostgreSQL and SQL-managed schema
  initialization.
- **Alternatives considered**: None — stack is fixed.

### D-8: Validation

- **Decision**: Jakarta Bean Validation on all form/query submission records; names and codes
  non-blank and length-bounded; codes constrained to `[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*`;
  uniqueness enforced at service layer with duplicate detection (FR-002, FR-012).
- **Rationale**: Constitution Principle III mandates Bean Validation on every inbound payload
  and mutation.
- **Alternatives considered**: None meaningful.

### D-9: Pinned runtime versions

- **Decision**: Target Java 21 and Spring Boot 4.1.1 as declared in the repository `pom.xml`
  (`<java.version>21</java.version>`, `spring-boot-starter-parent` 4.1.1), not the Java 17 /
  Spring Boot 3.x baseline the constitution states as a floor.
- **Rationale**: Constitution Principle III sets Java 17 LTS as a *minimum* ("or newer"), so
  Java 21 is compliant. The build is the authoritative source for the exact versions actually
  resolved and present in the local repository; planning against a version the build does not
  use would produce a plan that cannot compile.
- **Alternatives considered**:
  - Downgrading `pom.xml` to Java 17 / Spring Boot 3.x — rejected: not requested, and it would
    regress the already-working build.
  - Leaving the version unstated — rejected: leaves a NEEDS CLARIFICATION in Technical Context.

### D-10: Root package and configuration file

- **Decision**: Place all new code under the existing root package `com.allensandiego.adm`
  (artifact `adm`, single-module Maven project at the repository root) and extend the existing
  `src/main/resources/application.properties`. The upstream CoreUI template stays vendored in
  `coreui/`; its CSS/JS are served from `src/main/resources/static/`.
- **Rationale**: The repository already establishes this package and this configuration file;
  aligning with them avoids a parallel `com.example.rbac` tree and a competing
  `application.yml` that would shadow the active configuration.
- **Alternatives considered**: New `com.example.rbac` package and `application.yml` — rejected:
  duplicates the application root and splits configuration across two files.

## Resolved unknowns

All Technical Context unknowns resolved by D-1..D-10. No `NEEDS CLARIFICATION` items remain.