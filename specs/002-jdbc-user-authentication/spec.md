# Feature Specification: JDBC User Authentication

**Feature Branch**: `002-jdbc-user-authentication`

**Created**: 2026-09-21

**Status**: Draft

**Input**: User description: "For authentication, use spring security jdbc."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign In with Account Credentials (Priority: P1)

An operator opens the application and is presented with a sign-in screen because they are not
yet authenticated. They enter their username and password and submit. On success they land on
the application home screen and can continue to the areas their account is allowed to use.

**Why this priority**: Without a working sign-in, no protected capability can be reached at
all. This is the gateway that every other feature depends on.

**Independent Test**: Can be fully tested by opening the application unauthenticated, entering
a valid stored account's username and password, and confirming the home screen renders.

**Acceptance Scenarios**:

1. **Given** I am not signed in, **When** I open any protected page, **Then** I am taken to
   the sign-in screen instead of the protected content.
2. **Given** I have a valid, active account, **When** I submit the correct username and
   password, **Then** I am signed in and returned to the page I originally requested (or the
   home screen if none was requested).
3. **Given** I have a valid, active account, **When** I submit a wrong password or an unknown
   username, **Then** sign-in is refused and I see a single, generic "invalid credentials"
   message that does not reveal which field was wrong.

---

### User Story 2 - Sign Out and End the Session (Priority: P2)

A signed-in operator chooses to sign out. The session ends immediately, and any subsequent
attempt to reach protected content sends them back to the sign-in screen.

**Why this priority**: Ending access is essential on shared or unattended workstations, but it
is only meaningful once sign-in exists.

**Independent Test**: Can be fully tested by signing in, selecting sign out, and confirming
protected pages are no longer reachable.

**Acceptance Scenarios**:

1. **Given** I am signed in, **When** I choose sign out, **Then** my session ends and I am
   returned to the sign-in screen.
2. **Given** I have signed out, **When** I navigate back to a protected page, **Then** I am
   sent to the sign-in screen rather than seeing the protected content.

---

### User Story 3 - Refuse Accounts That Must Not Enter (Priority: P3)

An account that has been disabled or deactivated in administration attempts to sign in. Even
with the correct password, sign-in is refused, and the fact that the password was correct is
not disclosed.

**Why this priority**: It closes the loop between account administration and authentication so
that deactivation is a real access control, not a cosmetic flag.

**Independent Test**: Can be fully tested by deactivating a stored account, attempting sign-in
with its correct password, and confirming entry is refused.

**Acceptance Scenarios**:

1. **Given** my account has been deactivated, **When** I submit my correct username and
   password, **Then** sign-in is refused and no session is created.
2. **Given** my account is active, **When** I attempt repeated incorrect passwords, **Then**
   further attempts are throttled for a period without ever confirming the account exists.

---

### Edge Cases

- Username submitted with surrounding whitespace or different letter casing — the sign-in
  rule must treat usernames consistently and predictably.
- Empty username or empty password — submission is rejected with the generic credentials
  message and no lookup bypass.
- A signed-in user opens the sign-in screen again — they are sent to the home screen rather
  than shown a redundant login form.
- A password is stored and later changed by an administrator — the previous password no
  longer works and the new one does.
- The account's password was never set or is stored in a form the system cannot verify —
  sign-in is refused without crashing or exposing internals.
- The session expires or is invalidated while the user is mid-task — the next action returns
  them to sign-in without leaking the requested content.
- Sign-in succeeds while the same account is already signed in elsewhere — the behavior is
  defined and consistent (latest session remains usable, or the prior one is invalidated),
  with no corruption of either session.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST present a sign-in screen to unauthenticated visitors and MUST allow
  that screen's own assets to load without authentication.
- **FR-002**: System MUST verify submitted credentials against the accounts and credential
  material held in the application's own database; no external identity source participates
  in verification.
- **FR-003**: System MUST refuse sign-in on invalid credentials and MUST return one generic
  message that does not disclose whether the username or the password was incorrect.
- **FR-004**: System MUST refuse sign-in for accounts that are deactivated or otherwise not
  eligible, even when the supplied password is correct.
- **FR-005**: System MUST store passwords only in a strong one-way hashed form and MUST never
  persist or transmit plaintext passwords.
- **FR-006**: System MUST NOT load or process credentials from any source other than the
  system's own stored account data; there is no external identity provider in this scope.
- **FR-007**: System MUST redirect unauthenticated access to any protected resource to the
  sign-in screen, preserving the originally requested destination for return after sign-in.
- **FR-008**: System MUST establish an authenticated session on success and MUST issue a new
  session identifier at sign-in so that a pre-authentication session cannot be reused.
- **FR-009**: System MUST provide a sign-out action that ends the authenticated session and
  returns the user to the sign-in screen.
- **FR-010**: System MUST treat authentication and authorization as separate: signing in
  establishes identity only, and every protected screen and action MUST still be denied by
  default unless the account holds the required permission.
- **FR-011**: System MUST redirect an already-authenticated user away from the sign-in screen
  to the home screen.
- **FR-012**: System MUST throttle repeated failed sign-in attempts for an account for a
  bounded period so that password guessing is not practical, without confirming whether the
  account exists.
- **FR-013**: System MUST record each successful sign-in, failed sign-in, and sign-out with
  the account identifier, time, and outcome for security review, and MUST NOT record
  passwords or password-derived secrets in those records.
- **FR-014**: System MUST surface a safe, generic result when credential storage is missing or
  unreadable, and MUST NOT disclose internal error details to the visitor.

### Key Entities *(include if feature involves data)*

- **Account**: A stored identity that can sign in; carries a unique login name, credential
  material, and an eligibility status that governs whether sign-in is allowed.
- **Credential**: The hashed secret associated with an account, together with the hashing
  scheme used to verify a submitted password.
- **Sign-in Event**: A record of an authentication attempt or termination, capturing the
  account identifier, time, and outcome for security review.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user with a valid, active account can reach the home screen in under 10
  seconds from submitting credentials.
- **SC-002**: 100% of invalid credential attempts are refused with a message that reveals
  neither whether the username exists nor which field was wrong.
- **SC-003**: 100% of deactivated accounts are refused sign-in even with a correct password.
- **SC-004**: Zero plaintext passwords are recoverable from storage at any time, verified
  across the full account set.
- **SC-005**: After sign-out, 100% of attempts to reopen protected content land on the
  sign-in screen, including via browser history.
- **SC-006**: 100% of protected screens remain denied by default after sign-in unless the
  account holds the required permission.
- **SC-007**: Repeated failed attempts are throttled within the first few tries; automated
  guessing attempts cannot exceed a small, bounded number of attempts per account per minute.
- **SC-008**: Every successful sign-in, failed sign-in, and sign-out is recorded with actor
  and timestamp; 100% of authentication events are reviewable.

## Assumptions

- Authentication is implemented with Spring Security JDBC authentication (the mechanism
  specified by the requester) reading account and authority data from the application's own
  PostgreSQL database, consistent with the project constitution's tech stack.
- Accounts are created and managed by administrators through the existing user administration
  capability; no self-registration, invite, or password-reset flow is in scope for this
  feature.
- The unique login name is the username; usernames are matched case-insensitively and trimmed
  of surrounding whitespace.
- The existing account eligibility/status flag is the source of truth for whether sign-in is
  permitted.
- Credential upgrades to stronger hashing may happen lazily at next successful sign-in; no
  bulk migration is in scope.
- Single organization and a single local identity store; SSO, social login, multi-factor
  authentication, and "remember me" persistence are out of scope.
- A seeded administrator account exists so that the system can be signed into for the first
  time after launch.
- Standard web-app responsiveness applies: the sign-in screen responds within a couple of
  seconds under normal usage.
