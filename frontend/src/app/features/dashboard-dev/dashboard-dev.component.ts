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
  selector:    'app-dashboard-dev',
  standalone:  true,
  imports:     [CommonModule, RouterLink],
  templateUrl: './dashboard-dev.component.html',
  styleUrls:   ['./dashboard-dev.component.scss'],
})
export class DashboardDevComponent implements OnInit, OnDestroy {

  readonly auth = inject(AuthService);

  projects      = signal<Project[]>([]);
  notifications = signal<NotificationItem[]>([]);
  loading       = signal(true);
  lastRefresh   = signal<Date>(new Date());

  private pollSub?: Subscription;

  // ── KPIs ──────────────────────────────────────────────────────
  readonly kpis = computed(() => {
    const n = this.notifications();
    const p = this.projects();
    return {
      projects:  p.length,
      succeeded: n.filter(x => x.eventType === 'PIPELINE_SUCCESS').length,
      failed:    n.filter(x => x.eventType === 'PIPELINE_FAILED').length,
      blocked:   n.filter(x => x.eventType === 'SECURITY_BLOCKED').length,
      total:     n.length,
    };
  });

  readonly successRate = computed((): number => {
    const k = this.kpis();
    const t = k.succeeded + k.failed;
    return t === 0 ? 100 : Math.round((k.succeeded / t) * 100);
  });

  readonly passedTests = computed(() => this.kpis().succeeded);
  readonly failedTests = computed(() => this.kpis().failed);
  readonly totalTests  = computed(() => this.kpis().succeeded + this.kpis().failed);

  // ── Build history (last 5 notifs) ─────────────────────────────
  readonly buildHistory = computed(() =>
    [...this.notifications()]
      .sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      .slice(0, 5)
  );

  // ── Project lookup ────────────────────────────────────────────
  readonly projectMap = computed(() => {
    const map: Record<number, string> = {};
    this.projects().forEach(p => { map[p.id] = p.name; });
    return map;
  });

  // ── Test rate per stage (simulated from notif data) ───────────
  readonly stageRates = computed(() => [
    { name: 'Build',  rate: this.successRate(), prev: Math.max(0, this.successRate() - 5) },
    { name: 'Tests',  rate: this.successRate(), prev: Math.max(0, this.successRate() - 3) },
    { name: 'Sonar',  rate: Math.min(100, this.successRate() + 5), prev: this.successRate() },
    { name: 'Deploy', rate: this.kpis().failed === 0 ? 100 : this.successRate(), prev: this.successRate() },
  ]);

  // ── Progress bar width for donut alternative ──────────────────
  readonly passArcR  = 40;
  readonly donutCirc = computed(() => 2 * Math.PI * this.passArcR);
  readonly passOffset = computed(() =>
    this.donutCirc() * (1 - this.successRate() / 100)
  );

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

  projectName(id: number): string {
    return this.projectMap()[id] ?? `Projet #${id}`;
  }

  buildStatusClass(type: string): string {
    if (type === 'PIPELINE_SUCCESS') return 'status-success';
    if (type === 'PIPELINE_FAILED' || type === 'DEPLOYMENT_FAILED') return 'status-failed';
    return 'status-abandoned';
  }

  buildStatusLabel(type: string): string {
    if (type === 'PIPELINE_SUCCESS') return 'RÉUSSI';
    if (type === 'PIPELINE_FAILED')  return 'ÉCHEC';
    if (type === 'DEPLOYMENT_FAILED') return 'ÉCHEC DÉPL.';
    return 'ABANDONNÉ';
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
