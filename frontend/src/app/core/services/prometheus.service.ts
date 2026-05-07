import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PrometheusResult {
  metric: Record<string, string>;
  value:  [number, string]; // [timestamp, value]
}

export interface ServiceHealth {
  name:    string;
  up:      boolean;
  uptime:  number; // seconds
}

export interface MetricPoint {
  service: string;
  value:   number;
}

export interface MonitoringSnapshot {
  servicesUp:       number;
  servicesTotal:    number;
  services:         ServiceHealth[];
  httpRps:          MetricPoint[];   // HTTP requests/sec per service
  httpErrors:       MetricPoint[];   // 5xx error rate per service
  jvmMemory:        MetricPoint[];   // JVM heap used MB per service
  jvmThreads:       MetricPoint[];   // JVM live threads per service
  cpuUsage:         MetricPoint[];   // process CPU per service
  avgResponseMs:    MetricPoint[];   // avg HTTP response time ms
}

@Injectable({ providedIn: 'root' })
export class PrometheusService {

  private base = `${environment.apiPrometheus}/api/v1`;

  constructor(private http: HttpClient) {}

  /** Fire a single instant query */
  query(promql: string): Observable<PrometheusResult[]> {
    return this.http.get<any>(`${this.base}/query`, {
      params: { query: promql }
    }).pipe(
      map(r => (r?.data?.result ?? []) as PrometheusResult[]),
      catchError(() => of([]))
    );
  }

  /** Fetch all monitoring data in one call */
  snapshot(): Observable<MonitoringSnapshot> {
    return forkJoin({
      up:         this.query('up{job="spring-boot-services"}'),
      rps:        this.query('sum by (app) (rate(http_server_requests_seconds_count[5m]))'),
      errors:     this.query('sum by (app) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))'),
      jvmMem:     this.query('sum by (app) (jvm_memory_used_bytes{area="heap"}) / 1024 / 1024'),
      jvmThreads: this.query('jvm_threads_live_threads'),
      cpu:        this.query('process_cpu_usage'),
      respTime:   this.query('sum by (app) (rate(http_server_requests_seconds_sum[5m])) / sum by (app) (rate(http_server_requests_seconds_count[5m])) * 1000'),
    }).pipe(
      map(({ up, rps, errors, jvmMem, jvmThreads, cpu, respTime }) => {
        const services: ServiceHealth[] = up.map(r => ({
          name:   r.metric['app'] ?? r.metric['instance'] ?? 'unknown',
          up:     r.value[1] === '1',
          uptime: 0,
        }));

        return {
          servicesUp:    services.filter(s => s.up).length,
          servicesTotal: services.length,
          services,
          httpRps:      this.toPoints(rps),
          httpErrors:   this.toPoints(errors),
          jvmMemory:    this.toPoints(jvmMem),
          jvmThreads:   this.toPoints(jvmThreads),
          cpuUsage:     this.toPoints(cpu),
          avgResponseMs: this.toPoints(respTime),
        } as MonitoringSnapshot;
      })
    );
  }

  /** Range query for time-series charts (last N minutes) */
  range(promql: string, minutes = 60): Observable<any[]> {
    const end   = Math.floor(Date.now() / 1000);
    const start = end - minutes * 60;
    return this.http.get<any>(`${this.base}/query_range`, {
      params: { query: promql, start: start.toString(), end: end.toString(), step: '60' }
    }).pipe(
      map(r => r?.data?.result ?? []),
      catchError(() => of([]))
    );
  }

  private toPoints(results: PrometheusResult[]): MetricPoint[] {
    return results.map(r => ({
      service: r.metric['app'] ?? r.metric['instance'] ?? 'unknown',
      value:   parseFloat(r.value[1]) || 0,
    }));
  }
}
