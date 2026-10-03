# Contract: Dashboard Metrics & Service Layer

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](../spec.md)

## Service Interface: `DashboardService`

```java
package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.dto.DashboardViewModel;
import org.springframework.security.core.Authentication;

public interface DashboardService {
    
    /**
     * Resolves the full dashboard view model for the authenticated user,
     * tailoring displayed metrics and widgets based on user permissions.
     *
     * @param authentication active security principal
     * @return populated DashboardViewModel
     */
    DashboardViewModel getDashboardData(Authentication authentication);
}
```

---

## Controller Contract: `DashboardController`

```java
package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.dto.DashboardViewModel;
import com.allensandiego.adm.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(Authentication authentication, Model model) {
        DashboardViewModel vm = dashboardService.getDashboardData(authentication);
        model.addAttribute("dashboard", vm);
        return "home";
    }
}
```

---

## Behavior & Invariants

1. **Unauthenticated Access**:
   - `GET /` and `GET /dashboard` are protected routes.
   - Any unauthenticated request returns HTTP 302 redirecting to `/login`.
2. **Authority Evaluation**:
   - If the requesting user holds `audit.read` or `ROLE_ADMIN` (or administrative permissions), `dashboard.canViewAudit` is set to `true`.
   - If the user lacks audit authority (e.g. standard user `jdoe`), `dashboard.canViewAudit` is `false`, and chart/recent-activity fields are left empty or omitted.
3. **Empty Data Handling**:
   - When no `auth_event` rows exist for the 7-day range, the service produces 7 consecutive daily data points with `successCount = 0` and `failureCount = 0`.
   - The JSON strings for Chart.js are always valid arrays (e.g., `[0,0,0,0,0,0,0]`) and never null or malformed.
4. **Timezone Handling**:
   - Days are bucketed using UTC dates (`DATE(occurred_at AT TIME ZONE 'UTC')`).
   - The end date is `LocalDate.now(ZoneOffset.UTC)`, and the start date is 6 days prior (total 7 days inclusive).
