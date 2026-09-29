# Contracts: Permissions

**Date**: 2026-09-21 | Security model: [README.md](README.md)

| Method | Path             | Permission          | Purpose                          | Success                          | Failure                        |
|--------|------------------|---------------------|----------------------------------|----------------------------------|--------------------------------|
| GET    | /permissions     | permission.view      | List all permissions (paged)     | 200, list with status badges     | 403 if lacking permission      |
| GET    | /permissions/new | permission.create    | New-permission form              | 200, form                        | 403 if lacking permission      |
| POST   | /permissions     | permission.create    | Create a permission              | redirect to detail               | 400 duplicate/blank code/label/path |
| GET    | /permissions/{id} | permission.view     | Permission detail                | 200, detail with active state   | 404 unknown id; 403 if lacking |
| POST   | /permissions/{id}/edit | permission.edit | Update label, path, or active flag | redirect to detail         | 400 invalid; 403 if lacking     |

## Rules

- `code` is immutable after creation (a change would break existing role grants / audit
  trail); only `label`, `path`, and `active` are editable.
- Creating/deleting permissions is by code uniqueness; duplicate code → 400 with "code
  already exists".
- Deactivating a permission (`active=false`) keeps existing role grants stored but removes
  the permission from every user's effective set (FR-003). It cannot be newly granted to
  roles while inactive (see role editor).
- Deletion of a permission is not exposed in v1 (FR-003 covers lifecycle via
  active/inactive). Permissions are the atomic catalog; removal happens via deactivation to
  preserve audit and definitions.

## Validation (FR-002, FR-012, FR-015)

- code: non-blank, `[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)*`, <=80 chars, unique.
- label: non-blank, <=120 chars.
- path: non-blank, <=120 chars.