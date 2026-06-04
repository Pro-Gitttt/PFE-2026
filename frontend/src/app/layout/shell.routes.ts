import { Routes } from '@angular/router';
import { roleGuard } from '../core/guards/role.guard';

export const SHELL_ROUTES: Routes = [
  // ── Dashboards par rôle ───────────────────────────────────────
  {
    path: 'dashboard',
    loadComponent: () => import('../features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  {
    path: 'dashboard-devops',
    loadComponent: () => import('../features/dashboard-devops/dashboard-devops.component').then(m => m.DashboardDevopsComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  {
    path: 'dashboard-dev',
    loadComponent: () => import('../features/dashboard-dev/dashboard-dev.component').then(m => m.DashboardDevComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEV'] },
  },
  // ── Autres routes ─────────────────────────────────────────────
  {
    path: 'projects',
    loadComponent: () => import('../features/projects/projects.component').then(m => m.ProjectsComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEV', 'DEVOPS'] },
  },
  {
    path: 'pipelines',
    loadComponent: () => import('../features/pipelines/pipelines.component').then(m => m.PipelinesComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEV', 'DEVOPS'] },
  },
  {
    path: 'security',
    loadComponent: () => import('../features/security/security.component').then(m => m.SecurityComponent),
  },
  {
    path: 'monitoring',
    loadComponent: () => import('../features/monitoring/monitoring.component').then(m => m.MonitoringComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  {
    path: 'notifications',
    loadComponent: () => import('../features/notifications/notifications.component').then(m => m.NotificationsComponent),
  },
  {
    path: 'audit-logs',
    loadComponent: () =>
      import('../features/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  {
    path: 'admin',
    loadComponent: () => import('../features/admin/admin.component').then(m => m.AdminComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
