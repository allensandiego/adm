# Contracts: Roles

**Date**: 2026-09-21 | Security model: [README.md](README.md)

| Method | Path              | Permission          | Purpose                         | Success                          | Failure                        |
|--------|-------------------|---------------------|---------------------------------|----------------------------------|--------------------------------|
| GET    | /roles            | role.view            | List all roles (paged)          | 200, list                       | 403 if lacking permission      |
| GET    | /roles/new        | role.create          | New-role form                   | 200, form                       | 403 if lacking permission      |
| POST   | /roles            | role.create          | Create a role                   | redirect to detail              | 400 blank/duplicate name       |
| GET    | /roles/{id}       | role.view            | Role detail incl. permissions   | 200, detail                     | 404 unknown; 403 if lacking    |
| POST   | /roles/{id}/edit  | role.edit            | Rename role                     | redirect to detail              | 400 invalid name; 409 conflict |
| POST   | /roles/{id}/permissions | role.permissions.edit | Save grant/revoke set   | redirect to detail              | 400 no such permission; 409    |
| POST   | /roles/{id}/delete | role.delete          | Delete a role                   | redirect to list                | 409 guardrail (G1); 403         |

## Role-permission editor (FR-005)

- Single screen listing all ACTIVE permissions with checkboxes reflecting the role's current
  set; the resulting set is shown before save.
- Submit replaces the role's mapping: only granted checkboxes persist (POST semantics — the
  full submitted set becomes the new mapping).
- Inactive permissions are rendered read-only/unchecked and cannot be newly granted
  (FR-003).
- Guardrail G1: deleting a protected role that is the last protected role → **409** with
  "Cannot delete the final administrative role". Nothing changes (FR-010).

## Concurrency (FR-013)

- Saves are optimistic-locked; a concurrent edit by another administrator → **409** with a
  "role changed by another user — reload and retry" message; no overwrite is applied.