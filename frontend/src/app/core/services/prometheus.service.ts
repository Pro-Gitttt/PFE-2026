import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PrometheusResult {
  metric: Record<string, string>;
  value:  [number, string];
}

export interface ServiceHealth {
  name: string;
  up:   boolean;
}

export interface MetricPoint {
  service: string;
  value:   number;
}

export interface MonitoringSnapshot {
  servicesUp:    number;
  servicesTotal: number;
  services:      ServiceHealth[];
  httpRps:       MetricPoint[];
  httpErrors:    MetricPoint[];
  jvmMemory:     MetricPoint[];
  jvmThreads:    MetricPoint[];
  cpuUsage:      MetricPoint[];
  avgResponseMs: MetricPoint[];
}

@Injectable({ providedIn: 'root' })
export class PrometheusService {

  private base = `${environment.apiPrometheus}/api/v1`;

  constructor(private http: HttpClient) {}

  query(promql: string): Observable<PrometheusResult[]> {
    return this.http.get<any>(`${this.base}/query`, {
      params: { query: promql }
    }).pipe(
      map(r => (r?.data?.result ?? []) as PrometheusResult[]),
      catchError(err => {
        console.warn('[Prometheus] query failed:', promql, err?.status);
        return of([]);
      })
    );
  }

  snapshot(): Observable<MonitoringSnapshot> {
    return forkJoin({
      up:         this.query('up'),
      rps:        this.query('sum by (application, app, job) (rate(http_server_requests_seconds_count[5m]))'),
      errors:     this.query('sum by (application, app, job) (rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m]))'),
      jvmMem:     this.query('sum by (application, app, job) (jvm_memory_used_bytes{area="heap"}) / 1048576'),
      jvmThreads: this.query('sum by (application, app, job) (jvm_threads_live_threads)'),
      cpu:        this.query('sum by (application, app, job) (process_cpu_usage)'),
      respTime:   this.query(
        'sum by (application, app, job) (rate(http_server_requests_seconds_sum[5m])) / sum by (application, app, job) (rate(http_server_requests_seconds_count[5m])) * 1000'
      ),
    }).pipe(
      map(({ up, rps, errors, jvmMem, jvmThreads, cpu, respTime }) => {

        const knownApps = [
          'auth-service', 'pipeline-service', 'security-service',
          'notification-service', 'api-gateway',
          'Auth Service', 'Pipeline Service', 'Security Service', 'API Gateway'
        ];

        const getLabel = (r: PrometheusResult) =>
          r.metric['application'] ?? r.metric['app'] ?? r.metric['job'] ?? r.metric['instance'] ?? '';

        const allUp = up.filter(r => {
          const appLabel = getLabel(r);
          return knownApps.some(k => appLabel.toLowerCase().includes(k.toLowerCase()));
        });

        const upResults = allUp.length > 0 ? allUp : up;

        const services: ServiceHealth[] = upResults.map(r => ({
          name: r.metric['application'] ?? r.metric['app'] ?? r.metric['job'] ?? r.metric['instance'] ?? 'unknown',
          up:   r.value[1] === '1',
        }));

        return {
          servicesUp:    services.filter(s => s.up).length,
          servicesTotal: services.length || 5,
          services,
          httpRps:       this.toPoints(rps),
          httpErrors:    this.toPoints(errors),
          jvmMemory:     this.toPoints(jvmMem),
          jvmThreads:    this.toPoints(jvmThreads),
          cpuUsage:      this.toPoints(cpu),
          avgResponseMs: this.toPoints(respTime),
        };
      }),
      catchError(() => of({
        servicesUp: 0, servicesTotal: 5, services: [],
        httpRps: [], httpErrors: [], jvmMemory: [],
        jvmThreads: [], cpuUsage: [], avgResponseMs: [],
      }))
    );
  }

  private toPoints(results: PrometheusResult[]): MetricPoint[] {
    return results
      .map(r => ({
        service: r.metric['application'] ?? r.metric['app'] ?? r.metric['job'] ?? r.metric['instance'] ?? 'unknown',
        value:   parseFloat(r.value[1]) || 0,
      }))
      .filter(p => isFinite(p.value) && !isNaN(p.value));
  }
}
