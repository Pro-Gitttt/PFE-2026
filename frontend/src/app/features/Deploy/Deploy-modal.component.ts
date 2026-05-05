import {
  Component, Input, Output, EventEmitter,
  signal, inject, OnInit, OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Project } from '../../core/models/project.model';
import { PipelineService } from '../../core/services/pipeline.service';
import { Pipeline, PipelineExecution, PipelineStatus } from '../../core/models/pipeline.model';

type DeployStep = 'form' | 'progress' | 'success' | 'error';

interface StageDisplay {
  name: string;
  status: 'waiting' | 'running' | 'success' | 'failed';
}

@Component({
  selector: 'app-deploy-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './Deploy-modal.component.html',
  styleUrls: ['./Deploy-modal.component.scss'],
})
export class DeployModalComponent implements OnInit, OnDestroy {

  @Input() project!: Project;
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private pipelineSvc = inject(PipelineService);

  step          = signal<DeployStep>('form');
  pipelines     = signal<Pipeline[]>([]);
  deploying     = signal(false);
  deployError   = signal('');
  execution     = signal<PipelineExecution | null>(null);
  executionId   = signal<number | null>(null);
  elapsedSeconds = signal(0);
  stages        = signal<StageDisplay[]>([]);
  currentStageIdx = signal(0);
  logLines      = signal<string[]>([]);

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private tickTimer: ReturnType<typeof setInterval> | null = null;
  private startEpoch = 0;

  readonly environments = [
    { value: 'DEV',     label: 'Development (DEV)' },
    { value: 'STAGING', label: 'Staging (STG)' },
    { value: 'PROD',    label: 'Production (PROD)' },
  ];

  form: FormGroup = this.fb.group({
    environment: ['DEV', Validators.required],
    version:     ['v1.0.0', [Validators.required, Validators.pattern(/^v\d+\.\d+\.\d+$/)]],
    jenkinsJob:  ['', Validators.required],
    pipeline:    ['', Validators.required],
  });

  ngOnInit(): void {
    this.pipelineSvc.getByProject(this.project.id).subscribe({
      next: data => {
        this.pipelines.set(data);
        if (data.length > 0) {
          this.form.patchValue({
            pipeline:   data[0].id,
            jenkinsJob: data[0].jenkinsJobName ?? '',
          });
        }
      },
    });
  }

  ngOnDestroy(): void { this.stopTimers(); }

  close(): void { this.stopTimers(); this.closed.emit(); }

  onPipelineChange(event: Event): void {
    const id = +(event.target as HTMLSelectElement).value;
    const pl = this.pipelines().find(p => p.id === id);
    if (pl) this.form.patchValue({ jenkinsJob: pl.jenkinsJobName ?? '' });
  }

  deploy(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.deploying.set(true);
    this.deployError.set('');
    this.initStages();
    this.step.set('progress');
    this.startEpoch = Date.now();
    this.startTick();

    const pipelineId = +this.form.value.pipeline;
    const version    = this.form.value.version as string;

    this.pipelineSvc.trigger(pipelineId, { commitHash: version, userId: 0 }).subscribe({
      next: (exec) => {
        this.execution.set(exec);
        this.executionId.set(exec.id);
        this.deploying.set(false);
        this.appendLog(`✅ Pipeline déclenché — Execution #${exec.id}`);
        this.appendLog(`⏳ Statut: ${exec.status}`);
        this.startPolling(exec.id);
      },
      error: (e) => {
        this.stopTimers();
        this.deploying.set(false);
        this.deployError.set(e?.error?.message ?? 'Erreur lors du déploiement');
        this.step.set('error');
      },
    });
  }

  private startPolling(execId: number): void {
    this.pollTimer = setInterval(() => {
      this.pipelineSvc.getExecution(execId).subscribe({
        next: (exec) => {
          this.execution.set(exec);
          this.syncStages(exec);
          this.appendLog(`🔄 Statut: ${exec.status}`);
          if (exec.status === 'SUCCESS') {
            this.stopTimers();
            this.markAllStages('success');
            this.appendLog('🎉 Build terminé avec succès!');
            this.appendLog('📦 Artifact ZIP disponible sur Jenkins');
            setTimeout(() => this.step.set('success'), 600);
          } else if (exec.status === 'FAILED' || exec.status === 'CANCELLED') {
            this.stopTimers();
            this.markAllPendingFailed();
            this.appendLog(`❌ Build échoué: ${exec.status}`);
            this.deployError.set(`Le déploiement a échoué (${exec.status})`);
            this.step.set('error');
          } else if (exec.status === 'RUNNING') {
            this.advanceStages();
          }
        },
        error: () => this.appendLog('⚠️ Erreur lors du polling du statut'),
      });
    }, 4000);
  }

  private readonly defaultStageNames = [
    'Checkout du code', 'Build Maven', 'Tests Unitaires',
    'Analyse Sécurité', 'Package ZIP', 'Déploiement Jenkins',
  ];

  private initStages(): void {
    this.stages.set(this.defaultStageNames.map(name => ({ name, status: 'waiting' as const })));
    this.currentStageIdx.set(0);
  }

  private syncStages(exec: PipelineExecution): void {
    if (exec.stages && exec.stages.length > 0) {
      this.stages.set(exec.stages.map(s => ({
        name:   s.stageName,
        status: this.mapStatus(s.status),
      })));
    }
  }

  private advanceStages(): void {
    const idx = this.currentStageIdx();
    if (idx < this.stages().length) {
      this.stages.update(list => list.map((s, i) => {
        if (i < idx)   return { ...s, status: 'success' as const };
        if (i === idx) return { ...s, status: 'running' as const };
        return s;
      }));
      this.currentStageIdx.update(v => Math.min(v + 1, this.stages().length - 1));
    }
  }

  private markAllStages(status: 'success' | 'failed'): void {
    this.stages.update(list => list.map(s => ({ ...s, status })));
  }

  private markAllPendingFailed(): void {
    this.stages.update(list =>
      list.map(s =>
        s.status === 'waiting' || s.status === 'running'
          ? { ...s, status: 'failed' as const }
          : s
      )
    );
  }

  private mapStatus(s: PipelineStatus): StageDisplay['status'] {
    if (s === 'SUCCESS') return 'success';
    if (s === 'FAILED')  return 'failed';
    if (s === 'RUNNING') return 'running';
    return 'waiting';
  }

  private appendLog(msg: string): void {
    const ts = new Date().toLocaleTimeString('fr-FR');
    this.logLines.update(l => [...l.slice(-49), `[${ts}] ${msg}`]);
  }

  private startTick(): void {
    this.elapsedSeconds.set(0);
    this.tickTimer = setInterval(() => {
      this.elapsedSeconds.set(Math.floor((Date.now() - this.startEpoch) / 1000));
    }, 1000);
  }

  private stopTimers(): void {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null; }
    if (this.tickTimer) { clearInterval(this.tickTimer); this.tickTimer = null; }
  }

  get elapsedFormatted(): string {
    const s = this.elapsedSeconds();
    const m = Math.floor(s / 60);
    return m > 0 ? `${m}m ${s % 60}s` : `${s}s`;
  }

  get progressPercent(): number {
    const total   = this.stages().length || 1;
    const done    = this.stages().filter(s => s.status === 'success').length;
    const running = this.stages().some(s => s.status === 'running') ? 0.5 : 0;
    return Math.round(((done + running) / total) * 100);
  }

  get jenkinsBuildUrl(): string {
    const exec = this.execution();
    return (exec as any)?.jenkinsBuildUrl ?? '';
  }

  retry(): void {
    this.stopTimers();
    this.deployError.set('');
    this.logLines.set([]);
    this.execution.set(null);
    this.executionId.set(null);
    this.elapsedSeconds.set(0);
    this.step.set('form');
  }
}
