import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectService }      from '../../core/services/project.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService }         from '../../core/services/auth.service';
import { Project }             from '../../core/models/project.model';
import { NotificationItem }    from '../../core/models/notification.model';
 
@Component({
  selector:    'app-dashboard',
  standalone:  true,
  imports:     [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls:   ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
 
  readonly auth = inject(AuthService);
 
  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  loading       = signal(true);

  today = new Date();
 
  readonly kpis = computed(() => {
    const n = this.notifications();
    return {
      projects:  this.projects().length,
      succeeded: n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:    n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:   n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
    };
  });
 
  constructor(
    private projSvc:  ProjectService,
    private notifSvc: NotificationService,
  ) {}
 
  ngOnInit(): void {
    forkJoin({
      projects:      this.projSvc.getAll(),
      notifications: this.notifSvc.getAll(),
    }).subscribe({
      next: ({ projects, notifications }) => {
        this.projects.set(projects);
        this.notifications.set(notifications);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
 
  eventIcon(type: string): string {
    const icons: Record<string, string> = {
      PIPELINE_SUCCESS: '✓',
      PIPELINE_FAILED:  '✕',
      SECURITY_BLOCKED: '🛡',
      SECURITY_WARNING: '⚠',
    };
    return icons[type] ?? '•';
  }
 
  eventColor(type: string): string {
    if (type === 'PIPELINE_SUCCESS')                         return 'green';
    if (type === 'PIPELINE_FAILED' || type === 'SECURITY_BLOCKED') return 'red';
    return 'orange';
  }
}
 