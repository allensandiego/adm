# Application Thymeleaf Templates & CoreUI Reference Guide

**Application Templates Directory**: `src/main/resources/templates/`  
**Upstream Reference Source**: [`coreui/`](file:///home/allen/workspace/adm/coreui/) (CoreUI Free Bootstrap Admin Template v5.5.0)  
**Template Engine**: Thymeleaf (`xmlns:th="http://www.thymeleaf.org"`)  

---

## 1. Architecture: Reference vs. Application Templates

- **Reference Gallery (`coreui/`)**: Contains the vendored CoreUI HTML files. These are reference blueprints and static design specifications for layouts, forms, tables, error screens, and components.
- **Application Views (`src/main/resources/templates/`)**: Contains the actual production, server-rendered Thymeleaf HTML pages and fragments used at runtime by Spring Boot controllers.

---

## 2. CoreUI Reference Mapping Guide

When building application views under `src/main/resources/templates/`, consult the corresponding reference files in `coreui/`:

### 2.1 Base Layout & Navigation Shell
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/fragments/sidebar.html`<br>`templates/fragments/topbar.html`<br>`templates/layout/default.html` | [`coreui/blank.html`](file:///home/allen/workspace/adm/coreui/blank.html) | Extract `#sidebar` into sidebar fragment with dynamic menu items gated by permissions (`th:if="${#sets.contains(permissions, '...')}"`). Extract `<header class="header ...">` into topbar fragment with user avatar and theme mode switcher (`js/color-modes.js`). |

### 2.2 Authentication & Account Screens
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/login.html` | [`coreui/authentication/login.html`](file:///home/allen/workspace/adm/coreui/authentication/login.html) | Centered card container (`bg-body-tertiary min-vh-100 d-flex flex-row align-items-center`), input groups with icons, CSRF protection, and error alert blocks (`th:if="${param.error}"`). |
| `templates/authentication/register.html` | [`coreui/authentication/register.html`](file:///home/allen/workspace/adm/coreui/authentication/register.html) | Multi-field registration form styling. |
| Password flow screens | [`coreui/authentication/reset-password.html`](file:///home/allen/workspace/adm/coreui/authentication/reset-password.html)<br>[`coreui/authentication/change-password.html`](file:///home/allen/workspace/adm/coreui/authentication/change-password.html)<br>[`coreui/authentication/check-email.html`](file:///home/allen/workspace/adm/coreui/authentication/check-email.html) | Single-input card flows and status confirmation message patterns. |

### 2.3 List Screens (Permissions, Roles, Users)
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/permissions/list.html`<br>`templates/roles/list.html`<br>`templates/users/list.html` | [`coreui/components/tables.html`](file:///home/allen/workspace/adm/coreui/components/tables.html)<br>[`coreui/components/pagination.html`](file:///home/allen/workspace/adm/coreui/components/pagination.html)<br>[`coreui/components/badge.html`](file:///home/allen/workspace/adm/coreui/components/badge.html) | Wrap lists in `.card .mb-4` with action buttons in `.card-header`. Use `.table .table-hover .table-striped .align-middle`. Display status using badges (`.badge .bg-success` for Active, `.badge .bg-secondary` for Inactive/Protected). Use `.pagination .justify-content-end` for Spring Data Page navigation. |

### 2.4 Form & Editor Screens
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/permissions/form.html`<br>`templates/roles/form.html`<br>`templates/users/form.html` | [`coreui/forms/validation.html`](file:///home/allen/workspace/adm/coreui/forms/validation.html)<br>[`coreui/forms/form-control.html`](file:///home/allen/workspace/adm/coreui/forms/form-control.html)<br>[`coreui/forms/input-group.html`](file:///home/allen/workspace/adm/coreui/forms/input-group.html)<br>[`coreui/forms/layout.html`](file:///home/allen/workspace/adm/coreui/forms/layout.html) | Use `.form-label`, `.form-control`, `.form-select`. Implement Bean Validation feedback with `th:classappend="${#fields.hasErrors('field')} ? 'is-invalid' : ''"` and `.invalid-feedback`. |
| `templates/roles/permissions-editor.html`<br>`templates/users/roles-editor.html` | [`coreui/forms/checks-radios.html`](file:///home/allen/workspace/adm/coreui/forms/checks-radios.html)<br>[`coreui/components/list-group.html`](file:///home/allen/workspace/adm/coreui/components/list-group.html) | Multi-selection matrix using `.form-check`, `.form-check-input`, and `.list-group` to assign permissions to roles or roles to users. |

### 2.5 Detail & Inspection Screens
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/permissions/detail.html`<br>`templates/roles/detail.html`<br>`templates/users/detail.html` | [`coreui/components/cards.html`](file:///home/allen/workspace/adm/coreui/components/cards.html)<br>[`coreui/components/list-group.html`](file:///home/allen/workspace/adm/coreui/components/list-group.html)<br>[`coreui/components/buttons.html`](file:///home/allen/workspace/adm/coreui/components/buttons.html) | Card layout with key-value definition lists, carried permissions list, status badges, and action button toolbars (Edit, Deactivate/Activate). |

### 2.6 Error & Conflict Screens
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/error.html` | [`coreui/error-pages/404.html`](file:///home/allen/workspace/adm/coreui/error-pages/404.html)<br>[`coreui/error-pages/500.html`](file:///home/allen/workspace/adm/coreui/error-pages/500.html)<br>[`coreui/components/alerts.html`](file:///home/allen/workspace/adm/coreui/components/alerts.html) | Centered error card or alert layout displaying HTTP status (403, 404, 409, 500) and user-friendly error reason without disclosing internal stack traces. |

### 2.7 Home & Overview Screens
| Target Application View | CoreUI Reference File | Implementation Notes |
|---|---|---|
| `templates/home.html` | [`coreui/index.html`](file:///home/allen/workspace/adm/coreui/index.html)<br>[`coreui/widgets.html`](file:///home/allen/workspace/adm/coreui/widgets.html)<br>[`coreui/charts.html`](file:///home/allen/workspace/adm/coreui/charts.html) | Summary widgets, user count cards, role overview, and quick-action shortcuts. |

---

## 3. Implementation Rules for Production Templates

1. **Static Resource References**:
   Assets must be resolved via Spring context paths (`th:href="@{/css/...}"`, `th:src="@{/js/...}"`, `th:src="@{/vendors/...}"`). Static files are served from `src/main/resources/static/`.
2. **Security & Permission Gating**:
   Use Thymeleaf Security dialect (`sec:authorize`) or the resolved request permissions set (`#sets.contains(permissions, 'permission.code')`) to conditionally render action buttons and navigation links. Server-side middleware remains the authoritative gate.
3. **E2E Testing Attributes**:
   In accordance with Feature 003, all actionable components (buttons, links, inputs, status badges) must include stable `data-testid` attributes (e.g. `data-testid="btn-login"`, `data-testid="input-username"`).
