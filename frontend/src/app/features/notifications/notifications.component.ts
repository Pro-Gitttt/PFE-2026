import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationItem, EventType } from '../../core/models/notification.model';

type FilterType = 'ALL' | 'EMAIL' | 'SLACK' | 'SENT' | 'FAILED' | 'PENDING';

@Component({
  selector:    'app-notifications',
  standalone:  true,
  imports:     [CommonModule, DatePipe],
  templateUrl: './notifications.component.html',
  styleUrls:   ['./notifications.component.scss'],
})
export class NotificationsComponent implements OnInit, OnDestroy {

  all      = signal<NotificationItem[]>([]);
  filter   = signal<FilterType>('ALL');
  loading  = signal(true);
  error    = signal<string | null>(null);

  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  readonly filtered = computed(() => {
    const f = this.filter();
    const data = this.all();
    if (f === 'ALL')     return data;
    if (f === 'EMAIL')   return data.filter(n => n.channel === 'EMAIL');
    if (f === 'SLACK')   return data.filter(n => n.channel === 'SLACK');
    if (f === 'SENT')    return data.filter(n => n.status  === 'SENT');
    if (f === 'FAILED')  return data.filter(n => n.status  === 'FAILED');
    if (f === 'PENDING') return data.filter(n => n.status  === 'PENDING');
    return data;
  });

  readonly counts = computed(() => ({
    all:     this.all().length,
    failed:  this.all().filter(n => n.status === 'FAILED').length,
    pending: this.all().filter(n => n.status === 'PENDING').length,
  }));

  readonly filters: { key: FilterType; label: string }[] = [
    { key: 'ALL',     label: 'Toutes' },
    { key: 'EMAIL',   label: 'Email' },
    { key: 'SLACK',   label: 'Slack' },
    { key: 'SENT',    label: 'Envoyées' },
    { key: 'PENDING', label: 'En attente' },
    { key: 'FAILED',  label: 'Échouées' },
  ];

  constructor(private svc: NotificationService) {}

  ngOnInit(): void {
    this.load();
    // Auto-refresh every 15 seconds
    this.refreshTimer = setInterval(() => this.load(), 15000);
  }

  ngOnDestroy(): void {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  load(): void {
    this.svc.getAll().subscribe({
      next: n => {
        // Sort newest first
        const sorted = [...n].sort((a, b) => {
          const da = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const db = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return db - da;
        });
        this.all.set(sorted);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Impossible de charger les notifications.');
        console.error('[Notifications] Load error:', err);
      },
    });
  }

  setFilter(f: FilterType): void { this.filter.set(f); }

  iconFor(type: EventType): string {
    const map: Record<string, string> = {
      PIPELINE_SUCCESS: '✓',
      PIPELINE_FAILED:  '✕',
      SECURITY_BLOCKED: '🛡',
      SECURITY_WARNING: '⚠',
      DEPLOYMENT_FAILED:'✕',
    };
    return map[type] ?? '•';
  }

  iconColorClass(type: EventType): string {
    if (type === 'PIPELINE_SUCCESS')                                      return 'green';
    if (type === 'PIPELINE_FAILED' || type === 'SECURITY_BLOCKED'
     || type === 'DEPLOYMENT_FAILED')                                     return 'red';
    return 'orange';
  }

  statusClass(status: string): string {
    if (status === 'SENT')    return 'status-sent';
    if (status === 'FAILED')  return 'status-failed';
    if (status === 'PENDING') return 'status-pending';
    return '';
  }

  formatDate(d: string | null): string {
    if (!d) return '—';
    try { return new Date(d).toLocaleString('fr-FR'); }
    catch { return d; }
  }
}
