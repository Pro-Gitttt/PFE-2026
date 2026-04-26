import { Routes } from '@angular/router';
 
export const SHELL_ROUTES: Routes = [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../features/dashboard/dashboard.component')
        .then(m => m.DashboardComponent),
  },
  {
    path: 'projects',
    loadComponent: () =>
      import('../features/projects/projects.component')
        .then(m => m.ProjectsComponent),
  },
  {
    path: 'pipelines',
    loadComponent: () =>
      import('../features/pipelines/pipelines.component')
        .then(m => m.PipelinesComponent),
  },
  {
    path: 'security',
    loadComponent: () =>
      import('../features/security/security.component')
        .then(m => m.SecurityComponent),
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('../features/notifications/notifications.component')
        .then(m => m.NotificationsComponent),
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
 