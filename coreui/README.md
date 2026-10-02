# CoreUI Reference Gallery

**Directory**: `coreui/`  
**Version**: CoreUI Free Bootstrap Admin Template v5.5.0  
**Upstream**: [CoreUI](https://coreui.io/product/free-bootstrap-admin-template/)  

---

## Purpose

This directory serves as the **vendored design reference gallery** for the ADM application. Files in this directory are static reference implementations that guide the creation of server-rendered Thymeleaf templates in [`src/main/resources/templates/`](file:///home/allen/workspace/adm/src/main/resources/templates/).

---

## Directory & Reference Catalog

| Category | Path | Key Files & Reference Purpose |
|---|---|---|
| **Base Shells** | [`coreui/*.html`](file:///home/allen/workspace/adm/coreui/) | • [`blank.html`](file:///home/allen/workspace/adm/coreui/blank.html): Canonical admin layout shell (sidebar, topbar, breadcrumb, footer, dark/light theme switch).<br>• [`index.html`](file:///home/allen/workspace/adm/coreui/index.html): Dashboard layout and summary metrics.<br>• [`widgets.html`](file:///home/allen/workspace/adm/coreui/widgets.html): Card widgets and metric callouts.<br>• [`charts.html`](file:///home/allen/workspace/adm/coreui/charts.html): Chart visualizations. |
| **Components** | [`coreui/components/*.html`](file:///home/allen/workspace/adm/coreui/components/) | • [`tables.html`](file:///home/allen/workspace/adm/coreui/components/tables.html): Table styling, responsive wrappers, striped/hover states for list views.<br>• [`pagination.html`](file:///home/allen/workspace/adm/coreui/components/pagination.html): Pagination controls.<br>• [`badge.html`](file:///home/allen/workspace/adm/coreui/components/badge.html): Status badges (Active/Inactive/Protected).<br>• [`alerts.html`](file:///home/allen/workspace/adm/coreui/components/alerts.html): Success, warning, error alert banners.<br>• [`cards.html`](file:///home/allen/workspace/adm/coreui/components/cards.html): Detail cards and panels.<br>• [`buttons.html`](file:///home/allen/workspace/adm/coreui/components/buttons.html): Primary, secondary, outline, and small action buttons.<br>• [`modals.html`](file:///home/allen/workspace/adm/coreui/components/modals.html): Confirmation dialogs. |
| **Forms** | [`coreui/forms/*.html`](file:///home/allen/workspace/adm/coreui/forms/) | • [`validation.html`](file:///home/allen/workspace/adm/coreui/forms/validation.html): Field error states (`is-invalid`, `invalid-feedback`).<br>• [`form-control.html`](file:///home/allen/workspace/adm/coreui/forms/form-control.html): Text, email, and password inputs.<br>• [`checks-radios.html`](file:///home/allen/workspace/adm/coreui/forms/checks-radios.html): Checkbox controls for active flags and permission matrices.<br>• [`input-group.html`](file:///home/allen/workspace/adm/coreui/forms/input-group.html): Inputs with prefix/suffix icons.<br>• [`layout.html`](file:///home/allen/workspace/adm/coreui/forms/layout.html): Form grid layouts. |
| **Error Pages** | [`coreui/error-pages/*.html`](file:///home/allen/workspace/adm/coreui/error-pages/) | • [`404.html`](file:///home/allen/workspace/adm/coreui/error-pages/404.html): Not Found screen.<br>• [`500.html`](file:///home/allen/workspace/adm/coreui/error-pages/500.html): Internal Server Error screen. |
| **Authentication** | [`coreui/authentication/*.html`](file:///home/allen/workspace/adm/coreui/authentication/) | • [`login.html`](file:///home/allen/workspace/adm/coreui/authentication/login.html): Centered login form blueprint.<br>• [`register.html`](file:///home/allen/workspace/adm/coreui/authentication/register.html): User registration layout.<br>• [`reset-password.html`](file:///home/allen/workspace/adm/coreui/authentication/reset-password.html): Password reset flow screens. |

---

## Static Assets
The CSS, JS, and font assets supporting these templates are hosted in `src/main/resources/static/` (e.g. `static/css/`, `static/js/`, `static/vendors/`).
