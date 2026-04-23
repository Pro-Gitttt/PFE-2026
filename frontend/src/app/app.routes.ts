import { Routes } from '@angular/router';

import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProjectsComponent } from './features/projects/projects.component';
import { PipelinesComponent } from './features/pipelines/pipelines.component';
import { SecurityComponent } from './features/security/security.component';
import { NotificationsComponent } from './features/notifications/notifications.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  { path: 'dashboard', component: DashboardComponent },
  { path: 'projects', component: ProjectsComponent },
  { path: 'pipelines', component: PipelinesComponent },
  { path: 'security', component: SecurityComponent },
  { path: 'notifications', component: NotificationsComponent },

  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  { path: '**', redirectTo: 'dashboard' }
];