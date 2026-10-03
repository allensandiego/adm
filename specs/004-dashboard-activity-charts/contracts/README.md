# Feature 004 Contracts: Dashboard Activity Charts and Metrics

This directory contains the behavioral and interface contracts for Feature 004:

- [dashboard-metrics.md](dashboard-metrics.md) — Backend service, repository query, and controller model contracts.
- [dashboard-ui.md](dashboard-ui.md) — Frontend view template structure, CoreUI mappings, Chart.js configuration, and DOM contract.

## UI Reference Mappings

All dashboard views are composed using CoreUI Bootstrap 5 assets:
- **Metrics Cards**: Reference from [`coreui/widgets.html`](file:///home/allen/workspace/adm/coreui/widgets.html) and [`coreui/index.html`](file:///home/allen/workspace/adm/coreui/index.html) lines 740-875.
- **Traffic / Login Trend Chart**: Reference from [`coreui/index.html`](file:///home/allen/workspace/adm/coreui/index.html) lines 890-915 (`id="main-chart"`) and [`coreui/charts.html`](file:///home/allen/workspace/adm/coreui/charts.html).
- **Recent Activity Table**: Reference from [`coreui/components/tables.html`](file:///home/allen/workspace/adm/coreui/components/tables.html) and [`coreui/components/badge.html`](file:///home/allen/workspace/adm/coreui/components/badge.html).
