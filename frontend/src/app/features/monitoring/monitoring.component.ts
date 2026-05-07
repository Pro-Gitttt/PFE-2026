import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { interval, Subscription, forkJoin } from 'rxjs';
import { PrometheusService, MonitoringSnapshot, MetricPoint } from '../../core/services/prometheus.service';
import { ProjectService }      from '../../core/services/project.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector:    'app-monitoring',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './monitoring.component.html',
  styleUrls:   ['./monitoring.component.scss'],
})
export class MonitoringComponent implements OnInit, OnDestroy {

  private promSvc  = inject(PrometheusService);
  private projSvc  = inject(ProjectService);
  private notifSvc = inject(NotificationService);

  // ── Signals ────────────────────────────────────────────────────
  snapshot    = signal<MonitoringSnapshot | null>(null);
  loading     = signal(true);
  promError   = signal(false);
  lastRefresh = signal<Date>(new Date());

  totalProjects = signal(0);
  successCount  = signal(0);
  failedCount   = signal(0);
  blockedCount  = signal(0);

  private pollSub?: Subscription;

  // ── Computed ───────────────────────────────────────────────────
  readonly successRate = computed(() => {
    const total = this.successCount() + this.failedCount();
    return total === 0 ? 100 : Math.round((this.successCount() / total) * 100);
  });

  readonly servicesHealthy = computed(() => this.snapshot()?.servicesUp    ?? 0);
  readonly servicesTotal   = computed(() => this.snapshot()?.servicesTotal  ?? 5);

  readonly healthPercent = computed(() =>
    this.servicesTotal() === 0 ? 0
      : Math.round((this.servicesHealthy() / this.servicesTotal()) * 100)
  );

  readonly sortedByRps = computed(() =>
    [...(this.snapshot()?.httpRps ?? [])].sort((a, b) => b.value - a.value)
  );

  readonly sortedByMem = computed(() =>
    [...(this.snapshot()?.jvmMemory ?? [])].sort((a, b) => b.value - a.value)
  );

  readonly maxMem = computed(() =>
    Math.max(1, ...(this.snapshot()?.jvmMemory    ?? []).map(p => p.value))
  );

  readonly maxRps = computed(() =>
    Math.max(0.001, ...(this.snapshot()?.httpRps   ?? []).map(p => p.value))
  );

  // ── Lifecycle ──────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadAll();
    this.pollSub = interval(20_000).subscribe(() => this.loadAll(false));
  }

  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

  loadAll(showLoader = true): void {
    if (showLoader) this.loading.set(true);

    this.promSvc.snapshot().subscribe({
      next: snap => {
        this.snapshot.set(snap);
        this.promError.set(false);
        this.loading.set(false);
        this.lastRefresh.set(new Date());
      },
      error: () => {
        this.promError.set(true);
        this.loading.set(false);
        this.lastRefresh.set(new Date());
      },
    });

    forkJoin({
      projects:      this.projSvc.getAll(),
      notifications: this.notifSvc.getAll(),
    }).subscribe({
      next: ({ projects, notifications }) => {
        this.totalProjects.set(projects.length);
        this.successCount.set(notifications.filter(n => n.eventType === 'PIPELINE_SUCCESS').length);
        this.failedCount.set(notifications.filter(n => n.eventType === 'PIPELINE_FAILED').length);
        this.blockedCount.set(notifications.filter(n => n.eventType === 'SECURITY_BLOCKED').length);
      },
    });
  }

  // ── Per-service helpers (called from template) ─────────────────
  cpuPct(name: string): string {
    const v = this.snapshot()?.cpuUsage?.find(p => p.service === name)?.value ?? 0;
    return this.fmt(v * 100) + '%';
  }

  memMB(name: string): string {
    const v = this.snapshot()?.jvmMemory?.find(p => p.service === name)?.value ?? 0;
    return this.fmt(v, 0);
  }

  respMs(name: string): string {
    const v = this.snapshot()?.avgResponseMs?.find(p => p.service === name)?.value ?? 0;
    return this.fmt(v);
  }

  threadCount(name: string): string {
    const v = this.snapshot()?.jvmThreads?.find(p => p.service === name)?.value ?? 0;
    return this.fmt(v, 0);
  }

  // ── General helpers ────────────────────────────────────────────
  fmt(v: number, decimals = 1): string {
    return isNaN(v) || !isFinite(v) ? '—' : v.toFixed(decimals);
  }

  memBar(v: number): number {
    return Math.min(100, Math.round((v / Math.max(1, this.maxMem())) * 100));
  }

  rpsBar(v: number): number {
    return Math.min(100, Math.round((v / this.maxRps()) * 100));
  }

  svcLabel(name: string): string {
    const map: Record<string, string> = {
      'auth-service':         'Auth Service',
      'pipeline-service':     'Pipeline Service',
      'security-service':     'Security Service',
      'notification-service': 'Notification Svc',
      'api-gateway':          'API Gateway',
    };
    return map[name] ?? name;
  }

  // ── Constants ──────────────────────────────────────────────────
  readonly grafanaUrl    = 'http://192.168.56.20:32000';
  readonly prometheusUrl = 'http://192.168.56.20:32090';

  readonly knownServices = [
    'auth-service', 'pipeline-service', 'security-service',
    'notification-service', 'api-gateway',
  ];
}
