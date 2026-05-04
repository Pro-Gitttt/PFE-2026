import { Routes } from '@angular/router';
import { roleGuard } from '../core/guards/role.guard';

export const SHELL_ROUTES: Routes = [
  {
    path: 'dashboard',
    loadComponent: () => import('../features/dashboard/dashboard.component').then(m => m.DashboardComponent),
  },
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
    path: 'notifications',
    loadComponent: () => import('../features/notifications/notifications.component').then(m => m.NotificationsComponent),
  },
  {
    path: 'admin',
    loadComponent: () => import('../features/admin/admin.component').then(m => m.AdminComponent),
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
