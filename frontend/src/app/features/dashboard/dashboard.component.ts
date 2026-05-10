import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, interval, Subscription } from 'rxjs';

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
export class DashboardComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);

  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  loading       = signal(true);
  lastRefresh   = signal<Date>(new Date());
  today         = new Date();

  private pollSub?: Subscription;

  // ── KPI Cards ──────────────────────────────────────────────────
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
    return total === 0 ? 100 : Math.round((k.succeeded / total) * 100);
  });

  // ── Last 14 days activity for bar chart ───────────────────────
  readonly activityDays = computed(() => {
    const days: { label: string; success: number; failed: number; security: number; total: number }[] = [];
    for (let i = 13; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      const dayStr = d.toISOString().slice(0, 10);
      const dayNotifs = this.notifications().filter(n => n.createdAt?.startsWith(dayStr));
      const success  = dayNotifs.filter(n => n.eventType === 'PIPELINE_SUCCESS').length;
      const failed   = dayNotifs.filter(n => n.eventType === 'PIPELINE_FAILED').length;
      const security = dayNotifs.filter(n => n.eventType?.startsWith('SECURITY')).length;
      days.push({
        label: i === 0 ? "Auj." : i === 1 ? "Hier" : d.toLocaleDateString('fr-FR', { day:'numeric', month:'short' }),
        success, failed, security,
        total: success + failed + security,
      });
    }
    return days;
  });

  readonly maxDayCount = computed((): number =>
    Math.max(1, ...this.activityDays().map(d => d.total))
  );

  // ── SVG bar chart dimensions ──────────────────────────────────
  readonly chartW = 700;
  readonly chartH = 160;
  readonly barGap = 4;

  readonly barWidth = computed(() =>
    (this.chartW - (14 * this.barGap)) / 14
  );

  svgBars = computed(() => {
    const days  = this.activityDays();
    const max   = this.maxDayCount();
    const bw    = this.barWidth();
    return days.map((d, i) => {
      const x = i * (bw + this.barGap);
      const totalH  = max === 0 ? 0 : (d.total  / max) * (this.chartH - 20);
      const sucH    = max === 0 ? 0 : (d.success / max) * (this.chartH - 20);
      const failH   = max === 0 ? 0 : (d.failed  / max) * (this.chartH - 20);
      const secH    = max === 0 ? 0 : (d.security/ max) * (this.chartH - 20);
      return { x, bw, d, totalH, sucH, failH, secH,
               y: this.chartH - 20 - totalH };
    });
  });

  // ── Donut chart for pipeline status ───────────────────────────
  readonly donutSegments = computed(() => {
    const k = this.kpis();
    const total = k.succeeded + k.failed + k.blocked + k.warnings;
    if (total === 0) return [];
    const r = 54;
    const circ = 2 * Math.PI * r;
    const items = [
      { label: 'Succès',    value: k.succeeded, color: '#16a34a' },
      { label: 'Échecs',    value: k.failed,    color: '#dc2626' },
      { label: 'Bloqués',  value: k.blocked,   color: '#d97706' },
      { label: 'Alertes',  value: k.warnings,  color: '#6366f1' },
    ].filter(s => s.value > 0);
    let offset = 0;
    return items.map(seg => {
      const dash   = (seg.value / total) * circ;
      const gap    = circ - dash;
      const result = { ...seg, dash, gap, offset };
      offset += dash;
      return result;
    });
  });

  // ── Success rate sparkline ─────────────────────────────────────
  readonly sparklinePoints = computed(() => {
    const days = this.activityDays();
    const pts = days.map((d, i) => {
      const total = d.success + d.failed;
      const rate  = total === 0 ? 100 : Math.round((d.success / total) * 100);
      return { x: i * (100 / 13), y: 100 - rate };
    });
    return pts.map(p => `${p.x},${p.y}`).join(' ');
  });

  // ── Recent notifications ───────────────────────────────────────
  readonly recentNotifications = computed(() =>
    [...this.notifications()]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 10)
  );

  // ── Recent projects ────────────────────────────────────────────
  readonly recentProjects = computed(() =>
    [...this.projects()]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5)
  );

  // ── DORA-style metrics ─────────────────────────────────────────
  readonly deployFrequency = computed(() => {
    const n = this.notifications();
    const successes = n.filter(x => x.eventType === 'PIPELINE_SUCCESS');
    if (successes.length === 0) return { value: 0, label: 'Aucun déploiement' };
    const perDay = (successes.length / 14).toFixed(1);
    return { value: parseFloat(perDay), label: `${perDay} / jour` };
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
      PIPELINE_SUCCESS: 'Pipeline réussi', PIPELINE_FAILED: 'Pipeline échoué',
      DEPLOYMENT_FAILED: 'Déploiement échoué', SECURITY_BLOCKED: 'Alerte sécurité',
      SECURITY_WARNING: 'Avertissement', UPDATE_PROJECT: 'Projet mis à jour',
    };
    return m[type] ?? type;
  }

  timeAgo(dateStr: string): string {
    const diff = Date.now() - new Date(dateStr).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1)  return 'à l\'instant';
    if (m < 60) return `il y a ${m}m`;
    const h = Math.floor(m / 60);
    if (h < 24) return `il y a ${h}h`;
    return `il y a ${Math.floor(h / 24)}j`;
  }
}
