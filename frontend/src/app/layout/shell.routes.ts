import { Routes } from '@angular/router';
import { roleGuard } from '../core/guards/role.guard';

export const SHELL_ROUTES: Routes = [
  // ── Role-specific dashboards ────────────────────────────────
  {
    path: 'dashboard',
    loadComponent: () => import('../features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
  // ── Projects (all 3 roles, but DEV gets different view via component logic) ──
  {
    path: 'projects',
    loadComponent: () => import('../features/projects/projects.component').then(m => m.ProjectsComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEV', 'DEVOPS'] },
  },
  // ── Pipelines (shared — embedded in projects for DEV, standalone for ADMIN/DEVOPS) ──
  {
    path: 'pipelines',
    loadComponent: () => import('../features/pipelines/pipelines.component').then(m => m.PipelinesComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  // ── Security (ADMIN + DEVOPS only) ──
  {
    path: 'security',
    loadComponent: () => import('../features/security/security.component').then(m => m.SecurityComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  // ── Monitoring (ADMIN + DEVOPS only) ──
  {
    path: 'monitoring',
    loadComponent: () => import('../features/monitoring/monitoring.component').then(m => m.MonitoringComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  // ── Notifications (all roles) ──
  {
    path: 'notifications',
    loadComponent: () => import('../features/notifications/notifications.component').then(m => m.NotificationsComponent),
  },
  // ── Audit Logs (ADMIN + DEVOPS) ──
  {
    path: 'audit-logs',
    loadComponent: () => import('../features/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN', 'DEVOPS'] },
  },
  // ── Admin (ADMIN only) ──
  {
    path: 'admin',
    loadComponent: () => import('../features/admin/admin.component').then(m => m.AdminComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
