# Contract: Dashboard UI & View Layer

**Branch**: `004-dashboard-activity-charts` | **Date**: 2026-10-03 | **Spec**: [spec.md](../spec.md)

## View: `templates/home.html`

The dashboard view integrates with the CoreUI base layout fragments (`layout.html` or `sidebar.html` / `topbar.html`).

### Document Structure Contract

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
  <title>Dashboard - RBAC Admin Console</title>
  <!-- CoreUI CSS & Simplebar -->
  <link th:href="@{/vendors/simplebar/css/simplebar.css}" rel="stylesheet">
  <link th:href="@{/css/vendors/simplebar.css}" rel="stylesheet">
  <link th:href="@{/css/style.css}" rel="stylesheet">
</head>
<body>
  <div class="wrapper d-flex flex-column min-vh-100 bg-light">
    <!-- Topbar Fragment -->
    <div th:replace="~{fragments/topbar :: topbar}"></div>

    <div class="body flex-grow-1 px-3">
      <div class="container-lg">
        
        <!-- Section 1: Summary Metric Cards (FR-002, FR-003) -->
        <div class="row" id="dashboard-metric-cards">
          <!-- Total Users Card -->
          <div class="col-sm-6 col-lg-3">
            <div class="card mb-4 text-white bg-primary">
              <div class="card-body pb-0 d-flex justify-content-between align-items-start">
                <div>
                  <div class="fs-4 fw-semibold" id="metric-total-users" th:text="${dashboard.summary.totalUsers}">0</div>
                  <div>Total Users</div>
                </div>
              </div>
            </div>
          </div>
          <!-- Active Users Card -->
          <div class="col-sm-6 col-lg-3">
            <div class="card mb-4 text-white bg-success">
              <div class="card-body pb-0 d-flex justify-content-between align-items-start">
                <div>
                  <div class="fs-4 fw-semibold" id="metric-active-users" th:text="${dashboard.summary.activeUsers}">0</div>
                  <div>Active Users</div>
                </div>
              </div>
            </div>
          </div>
          <!-- Total Roles Card -->
          <div class="col-sm-6 col-lg-3">
            <div class="card mb-4 text-white bg-warning">
              <div class="card-body pb-0 d-flex justify-content-between align-items-start">
                <div>
                  <div class="fs-4 fw-semibold" id="metric-total-roles" th:text="${dashboard.summary.totalRoles}">0</div>
                  <div>Roles Defined</div>
                </div>
              </div>
            </div>
          </div>
          <!-- 24h Sign-in Attempts Card -->
          <div class="col-sm-6 col-lg-3">
            <div class="card mb-4 text-white bg-info">
              <div class="card-body pb-0 d-flex justify-content-between align-items-start">
                <div>
                  <div class="fs-4 fw-semibold" id="metric-recent-logins">
                    <span th:text="${dashboard.summary.recentSuccessLogins}">0</span>
                    <small class="fs-6 fw-normal text-white-50">(<span th:text="${dashboard.summary.recentFailedAttempts}">0</span> failed)</small>
                  </div>
                  <div>24h Sign-ins</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Section 2: Login Activity Trend Chart (FR-004, Gated by FR-007) -->
        <div class="card mb-4" id="dashboard-chart-card" th:if="${dashboard.canViewAudit}">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h4 class="card-title mb-0">Authentication Activity</h4>
                <div class="small text-medium-emphasis">Rolling 7-Day Trend (Successes vs Failures)</div>
              </div>
            </div>
            <div class="c-chart-wrapper" style="height: 300px; margin-top: 20px;">
              <canvas id="login-activity-chart" height="300"
                      th:data-labels="${dashboard.chartLabelsJson}"
                      th:data-successes="${dashboard.chartSuccessesJson}"
                      th:data-failures="${dashboard.chartFailuresJson}">
              </canvas>
            </div>
          </div>
        </div>

        <!-- Section 3: Recent Authentication Activities Feed (FR-005, FR-006, FR-008) -->
        <div class="card mb-4" id="dashboard-recent-activity-card" th:if="${dashboard.canViewAudit}">
          <div class="card-header fw-bold">Recent Authentication Activity</div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover mb-0" id="table-recent-activity">
                <thead class="table-light">
                  <tr>
                    <th>Timestamp (UTC)</th>
                    <th>Account / Username</th>
                    <th>Outcome</th>
                    <th>Client IP</th>
                  </tr>
                </thead>
                <tbody>
                  <tr th:if="${#lists.isEmpty(dashboard.recentActivities)}">
                    <td colspan="4" class="text-center text-muted py-3" id="recent-activity-empty">No recent authentication activity recorded</td>
                  </tr>
                  <tr th:each="act : ${dashboard.recentActivities}" class="activity-row">
                    <td th:text="${act.formattedTimestamp}">2026-10-03 10:00:00</td>
                    <td class="fw-semibold text-break" th:text="${act.username}">adminuser</td>
                    <td>
                      <span class="badge" th:classappend="${act.badgeClass}" th:text="${act.outcome}">AUTH_SUCCESS</span>
                    </td>
                    <td class="font-monospace small" th:text="${act.remoteIp}">127.0.0.1</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>

  <!-- Vendors & CoreUI JavaScript -->
  <script th:src="@{/vendors/@coreui/coreui/js/coreui.bundle.min.js}"></script>
  <script th:src="@{/vendors/chart.js/js/chart.umd.js}"></script>
  
  <!-- Inline Chart Initialization -->
  <script th:if="${dashboard.canViewAudit}">
    document.addEventListener("DOMContentLoaded", function () {
      const canvas = document.getElementById('login-activity-chart');
      if (!canvas) return;
      
      const labels = JSON.parse(canvas.getAttribute('th:data-labels') || canvas.dataset.labels || '[]');
      const successes = JSON.parse(canvas.getAttribute('th:data-successes') || canvas.dataset.successes || '[]');
      const failures = JSON.parse(canvas.getAttribute('th:data-failures') || canvas.dataset.failures || '[]');

      new Chart(canvas, {
        type: 'line',
        data: {
          labels: labels,
          datasets: [
            {
              label: 'Successful Sign-ins',
              backgroundColor: 'rgba(46, 184, 92, 0.1)',
              borderColor: '#2eb85c',
              pointBackgroundColor: '#2eb85c',
              data: successes,
              tension: 0.3,
              fill: true
            },
            {
              label: 'Failed Attempts',
              backgroundColor: 'rgba(229, 83, 83, 0.1)',
              borderColor: '#e55353',
              pointBackgroundColor: '#e55353',
              data: failures,
              tension: 0.3,
              fill: true
            }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: {
              beginAtZero: true,
              ticks: { precision: 0 }
            }
          }
        }
      });
    });
  </script>
</body>
</html>
```
