import { Routes } from "@angular/router";
import { RegisterComponent } from "../features/auth/register/register.component";
import { DashboardComponent } from "../features/dashboard/dashboard.component";
import { NotificationsComponent } from "../features/notifications/notifications.component";
import { PipelinesComponent } from "../features/pipelines/pipelines.component";
import { ProjectsComponent } from "../features/projects/projects.component";
import { SecurityComponent } from "../features/security/security.component";

export const SHELL_ROUTES: Routes = [
  {
    path: 'dashboard',
    component: DashboardComponent,
  },
  {
    path: 'projects',
    component: ProjectsComponent,
  },
  {
    path: 'pipelines',
    component: PipelinesComponent,
  },
  {
    path: 'security',
    component: SecurityComponent,
  },
  {
    path: 'notifications',
    component: NotificationsComponent,
  },

  // ✅ ADD THIS
  {
    path: 'register',
    component: RegisterComponent,
  },

  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];