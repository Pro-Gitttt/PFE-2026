import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, interval, Subscription } from 'rxjs';

import { ProjectService }      from '../../core/services/project.service';
import { NotificationService } from '../../core/services/notification.service';
import { PipelineService }     from '../../core/services/pipeline.service';
import { AuthService }         from '../../core/services/auth.service';
import { Project }             from '../../core/models/project.model';
import { NotificationItem }    from '../../core/models/notification.model';
import { PipelineExecution }   from '../../core/models/pipeline.model';

@Component({
  selector:    'app-dashboard',
  standalone:  true,
  imports:     [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls:   ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);

  projects       = signal<Project[]>([]);
  notifications  = signal<NotificationItem[]>([]);
  loading        = signal(true);
  lastRefresh    = signal<Date>(new Date());

  today = new Date();
  private pollSub?: Subscription;

  // ── Computed KPIs ─────────────────────────────────────────
  readonly kpis = computed(() => {
    const n = this.notifications();
    const p = this.projects();
    return {
      projects:    p.length,
      active:      p.filter(x => !x.status || x.status === 'ACTIVE').length,
      succeeded:   n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:      n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:     n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
      warnings:    n.filter(x => x.eventType === 'SECURITY_WARNING').length,
      total_notif: n.length,
    };
  });

  readonly successRate = computed((): number => {
    const k = this.kpis();
    const total = k.succeeded + k.failed;
    // FIX: explicit return type `: number` guarantees the signal is never undefined
    return total === 0 ? 100 : Math.round((k.succeeded / total) * 100);
  });

  readonly recentNotifications = computed(() =>
    [...this.notifications()]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 8)
  );

  readonly recentProjects = computed(() =>
    [...this.projects()]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5)
  );

  // Activity chart: last 7 days notification counts
  readonly activityDays = computed(() => {
    const days: { label: string; success: number; failed: number; security: number; date: Date }[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const label  = d.toLocaleDateString('fr-FR', { weekday: 'short' });
      const dayStr = d.toISOString().slice(0, 10);
      const dayNotifs = this.notifications().filter(n =>
        n.createdAt && n.createdAt.startsWith(dayStr)
      );
      days.push({
        label,
        date: d,
        success:  dayNotifs.filter(n => n.eventType === 'PIPELINE_SUCCESS').length,
        failed:   dayNotifs.filter(n => n.eventType === 'PIPELINE_FAILED').length,
        security: dayNotifs.filter(n => n.eventType?.startsWith('SECURITY')).length,
      });
    }
    return days;
  });

  readonly maxDayCount = computed((): number => {
    const days = this.activityDays();
    return Math.max(1, ...days.map(d => d.success + d.failed + d.security));
  });

  // Pipeline status distribution
  readonly pipelineStats = computed(() => {
    const n        = this.notifications();
    const success  = n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length;
    const failed   = n.filter(x => x.eventType === 'PIPELINE_FAILED').length;
    const security = n.filter(x => x.eventType?.startsWith('SECURITY')).length;
    const other    = Math.max(0, n.length - success - failed - security);
    return [
      { label: 'Succès',   value: success,  color: '#16a34a', bg: '#dcfce7' },
      { label: 'Échecs',   value: failed,   color: '#dc2626', bg: '#fee2e2' },
      { label: 'Sécurité', value: security, color: '#d97706', bg: '#fef3c7' },
      { label: 'Autres',   value: other,    color: '#6366f1', bg: '#e0e7ff' },
    ].filter(s => s.value > 0);
  });

  constructor(
    private projSvc:     ProjectService,
    private notifSvc:    NotificationService,
    private pipelineSvc: PipelineService,
  ) {}

  ngOnInit(): void {
    this.loadAll();
    // Auto-refresh every 30 seconds
    this.pollSub = interval(30_000).subscribe(() => this.loadAll(false));
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  loadAll(showLoader = true): void {
    if (showLoader) this.loading.set(true);
    forkJoin({
      projects:      this.projSvc.getAll(),
      notifications: this.notifSvc.getAll(),
    }).subscribe({
      next: ({ projects, notifications }) => {
        this.projects.set(projects);
        this.notifications.set(notifications);
        this.loading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: () => this.loading.set(false),
    });
  }

  barHeight(day: { success: number; failed: number; security: number }): number {
    const total = day.success + day.failed + day.security;
    return Math.round((total / this.maxDayCount()) * 100);
  }

  eventIcon(type: string): string {
    const m: Record<string, string> = {
      PIPELINE_SUCCESS:  '✓',
      PIPELINE_FAILED:   '✕',
      DEPLOYMENT_FAILED: '⊗',
      SECURITY_BLOCKED:  '🛡',
      SECURITY_WARNING:  '⚠',
      UPDATE_PROJECT:    '↻',
    };
    return m[type] ?? '•';
  }

  eventColor(type: string): string {
    if (type === 'PIPELINE_SUCCESS') return 'green';
    if (type?.startsWith('SECURITY') || type === 'PIPELINE_FAILED' || type === 'DEPLOYMENT_FAILED') return 'red';
    return 'orange';
  }

  eventLabel(type: string): string {
    const m: Record<string, string> = {
      PIPELINE_SUCCESS:  'Pipeline réussi',
      PIPELINE_FAILED:   'Pipeline échoué',
      DEPLOYMENT_FAILED: 'Déploiement échoué',
      SECURITY_BLOCKED:  'Alerte sécurité',
      SECURITY_WARNING:  'Avertissement',
      UPDATE_PROJECT:    'Projet mis à jour',
    };
    return m[type] ?? type;
  }

  pipelineChartPercent(value: number): number {
    const total = this.notifications().length;
    return total === 0 ? 0 : Math.round((value / total) * 100);
  }
}