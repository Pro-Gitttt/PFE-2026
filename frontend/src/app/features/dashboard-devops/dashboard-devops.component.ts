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
import { Pipeline }            from '../../core/models/pipeline.model';

@Component({
  selector:    'app-dashboard-devops',
  standalone:  true,
  imports:     [CommonModule, RouterLink],
  templateUrl: './dashboard-devops.component.html',
  styleUrls:   ['./dashboard-devops.component.scss'],
})
export class DashboardDevopsComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);

  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  loading       = signal(true);
  lastRefresh   = signal<Date>(new Date());
  today         = new Date();

  private pollSub?: Subscription;

  // ── KPIs ──────────────────────────────────────────────────────
  readonly kpis = computed(() => {
    const n = this.notifications();
    const p = this.projects();
    return {
      projects:     p.length,
      active:       p.filter(x => !x.status || x.status === 'ACTIVE').length,
      succeeded:    n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:       n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:      n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
      warnings:     n.filter(x => x.eventType === 'SECURITY_WARNING').length,
      deployFailed: n.filter(x => x.eventType === 'DEPLOYMENT_FAILED').length,
      total_notif:  n.length,
    };
  });

  readonly successRate = computed((): number => {
    const k = this.kpis();
    const total = k.succeeded + k.failed;
    return total === 0 ? 100 : Math.round((k.succeeded / total) * 100);
  });

  readonly changeFailRate = computed(() => {
    const k = this.kpis();
    const total = k.succeeded + k.failed;
    if (total === 0) return '0%';
    return Math.round((k.failed / total) * 100) + '%';
  });

  readonly securityScore = computed(() => {
    const blocked = this.kpis().blocked;
    const total   = this.kpis().total_notif;
    if (total === 0) return 100;
    return Math.max(0, Math.round(100 - (blocked / total) * 100));
  });

  readonly deployFrequency = computed(() => {
    const n = this.notifications();
    const successes = n.filter(x => x.eventType === 'PIPELINE_SUCCESS');
    if (successes.length === 0) return { value: 0 };
    return { value: parseFloat((successes.length / 14).toFixed(1)) };
  });

  // ── 14-day activity for bar chart ─────────────────────────────
  readonly activityDays = computed(() => {
    const days: { label: string; success: number; failed: number; security: number; total: number }[] = [];
    for (let i = 13; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dayStr = d.toISOString().slice(0, 10);
      const dayN = this.notifications().filter(n => n.createdAt?.startsWith(dayStr));
      const success  = dayN.filter(n => n.eventType === 'PIPELINE_SUCCESS').length;
      const failed   = dayN.filter(n => n.eventType === 'PIPELINE_FAILED').length;
      const security = dayN.filter(n => n.eventType?.startsWith('SECURITY')).length;
      days.push({
        label: i === 0 ? 'Auj.' : i === 1 ? 'Hier' : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }),
        success, failed, security,
        total: success + failed + security,
      });
    }
    return days;
  });

  readonly maxDayCount = computed((): number =>
    Math.max(1, ...this.activityDays().map(d => d.total))
  );

  readonly chartW = 700;
  readonly chartH = 140;
  readonly barGap = 4;
  readonly barWidth = computed(() => (this.chartW - 14 * this.barGap) / 14);

  readonly svgBars = computed(() => {
    const days = this.activityDays();
    const max  = this.maxDayCount();
    const bw   = this.barWidth();
    return days.map((d, i) => {
      const x      = i * (bw + this.barGap);
      const sucH   = max === 0 ? 0 : (d.success  / max) * (this.chartH - 20);
      const failH  = max === 0 ? 0 : (d.failed   / max) * (this.chartH - 20);
      const secH   = max === 0 ? 0 : (d.security / max) * (this.chartH - 20);
      const totalH = sucH + failH + secH;
      return { x, bw, d, totalH, sucH, failH, secH, y: this.chartH - 20 - totalH };
    });
  });

  // ── Security alerts ───────────────────────────────────────────
  readonly securityAlerts = computed(() =>
    this.notifications()
      .filter(n => n.eventType?.startsWith('SECURITY'))
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 8)
  );

  // ── Recent pipelines ──────────────────────────────────────────
  readonly recentNotifs = computed(() =>
    [...this.notifications()]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 12)
  );

  // ── Project lookup map ────────────────────────────────────────
  readonly projectMap = computed(() => {
    const map: Record<number, string> = {};
    this.projects().forEach(p => { map[p.id] = p.name; });
    return map;
  });

  constructor(
    private projSvc:  ProjectService,
    private notifSvc: NotificationService,
  ) {}

  ngOnInit(): void {
    this.loadAll();
    this.pollSub = interval(30_000).subscribe(() => this.loadAll(false));
  }

  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

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

  projectName(projectId: number): string {
    return this.projectMap()[projectId] ?? `Projet #${projectId}`;
  }

  eventIcon(type: string): string {
    const m: Record<string, string> = {
      PIPELINE_SUCCESS: '✓', PIPELINE_FAILED: '✕',
      DEPLOYMENT_FAILED: '⊗', SECURITY_BLOCKED: '🛡',
      SECURITY_WARNING: '⚠', UPDATE_PROJECT: '↻',
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
      PIPELINE_SUCCESS: 'Réussi', PIPELINE_FAILED: 'Échoué',
      DEPLOYMENT_FAILED: 'Dépl. échoué', SECURITY_BLOCKED: 'Sécurité bloqué',
      SECURITY_WARNING: 'Avertissement', UPDATE_PROJECT: 'Mis à jour',
    };
    return m[type] ?? type;
  }

  timeAgo(dateStr: string | null): string {
    if (!dateStr) return '—';
    const diff = Date.now() - new Date(dateStr).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1)  return 'à l\'instant';
    if (m < 60) return `il y a ${m}m`;
    const h = Math.floor(m / 60);
    if (h < 24) return `il y a ${h}h`;
    return `il y a ${Math.floor(h / 24)}j`;
  }
}
