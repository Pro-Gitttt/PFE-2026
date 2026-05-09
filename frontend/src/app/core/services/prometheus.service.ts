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
  value:  [number, string];
}

export interface ServiceHealth {
  name:   string;
  up:     boolean;
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

  // Proxied through gateway: /prometheus/** → prometheus:9090/**
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
      catchError(err => {
        console.warn('[Prometheus] query failed:', promql, err?.status);
        return of([]);
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
  snapshot(): Observable<MonitoringSnapshot> {
    // Use simple PromQL without job filter — works with any Spring Boot actuator scrape
    return forkJoin({
      // up metric — use just 'up' to catch all scraped targets
      up:         this.query('up'),
      // HTTP request rate — Spring Boot 3.x metric name
      rps:        this.query('sum by (app) (rate(http_server_requests_seconds_count[5m]))'),
      // 5xx error rate
      errors:     this.query('sum by (app) (rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m]))'),
      // JVM heap used bytes → MB
      jvmMem:     this.query('sum by (app) (jvm_memory_used_bytes{area="heap"}) / 1048576'),
      // JVM live threads
      jvmThreads: this.query('jvm_threads_live_threads'),
      // Process CPU usage
      cpu:        this.query('process_cpu_usage'),
      // Average response time in ms
      respTime:   this.query(
        'sum by (app) (rate(http_server_requests_seconds_sum[5m])) / sum by (app) (rate(http_server_requests_seconds_count[5m])) * 1000'
      ),
    }).pipe(
      map(({ up, rps, errors, jvmMem, jvmThreads, cpu, respTime }) => {

        // Filter 'up' to only our Spring Boot services
        const knownApps = [
          'auth-service', 'pipeline-service', 'security-service',
          'notification-service', 'api-gateway'
        ];

        const allUp = up.filter(r => {
          const appLabel = r.metric['app'] ?? r.metric['instance'] ?? '';
          return knownApps.some(k => appLabel.includes(k));
        });

        // If filtering returns nothing, use all 'up' results
        const upResults = allUp.length > 0 ? allUp : up;

        const services: ServiceHealth[] = upResults.map(r => ({
          name: r.metric['app'] ?? r.metric['instance'] ?? 'unknown',
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
      catchError(err => {
        console.warn('[Prometheus] snapshot failed:', err);
        return of({
          servicesUp: 0, servicesTotal: 5, services: [],
          httpRps: [], httpErrors: [], jvmMemory: [],
          jvmThreads: [], cpuUsage: [], avgResponseMs: [],
        });
      })
    );
  }

  private toPoints(results: PrometheusResult[]): MetricPoint[] {
    return results.map(r => ({
      service: r.metric['app'] ?? r.metric['instance'] ?? 'unknown',
      value:   parseFloat(r.value[1]) || 0,
    }));
    return results
      .map(r => ({
        service: r.metric['app'] ?? r.metric['instance'] ?? 'unknown',
        value:   parseFloat(r.value[1]) || 0,
      }))
      .filter(p => isFinite(p.value) && !isNaN(p.value));
  }
}
