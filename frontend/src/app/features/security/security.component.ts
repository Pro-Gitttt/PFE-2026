import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService }  from '../../core/services/project.service';
import { SecurityService } from '../../core/services/security.service';
import { Project }         from '../../core/models/project.model';
import { SecurityScan, SeverityLevel, Vulnerability } from '../../core/models/security.model';
 
@Component({
  selector:    'app-security',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './security.component.html',
  styleUrls:   ['./security.component.scss'],
})
export class SecurityComponent implements OnInit {
 
  projects    = signal<Project[]>([]);
  scans       = signal<SecurityScan[]>([]);
  selectedId  = signal<number | null>(null);
  loading     = signal(false);
 
  // Active scan (first scan detail open)
  activeScanId = signal<number | null>(null);
 
  readonly totalVulns = computed(() =>
    this.scans().reduce(
      (acc, s) => acc + (s.vulnerabilities?.length ?? 0), 0
    )
  );
 
  constructor(
    private projSvc: ProjectService,
    private secSvc:  SecurityService,
  ) {}
 
  ngOnInit(): void {
    this.projSvc.getAll().subscribe({ next: p => this.projects.set(p) });
  }
 
  onProjectChange(ev: Event): void {
    const id = +(ev.target as HTMLSelectElement).value;
    this.selectedId.set(id || null);
    this.scans.set([]);
    this.activeScanId.set(null);
 
    if (id) {
      this.loading.set(true);
      this.secSvc.getByProject(id).subscribe({
        next:  s => { this.scans.set(s); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    }
  }
 
  toggleScan(id: number): void {
    this.activeScanId.update(v => (v === id ? null : id));
  }
 
  countBySeverity(scan: SecurityScan, sev: SeverityLevel): number {
    return scan.vulnerabilities?.filter(v => v.severity === sev).length ?? 0;
  }
 
  scoreLabel(score: number): string {
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Acceptable';
    if (score >= 40) return 'Risqué';
    return 'Critique';
  }
 
  scoreClass(score: number): string {
    if (score >= 70) return 'good';
    if (score >= 40) return 'warn';
    return 'bad';
  }
 
  sevClass(sev: string): string {
    return sev.toLowerCase();
  }
}