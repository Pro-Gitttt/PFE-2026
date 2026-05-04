import { Component, Input, Output, EventEmitter, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Project } from '../../core/models/project.model';
import { PipelineService } from '../../core/services/pipeline.service';
import { Pipeline } from '../../core/models/pipeline.model';

@Component({
  selector: 'app-deploy-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './Deploy-modal.component.html',
  styleUrls: ['./Deploy-modal.component.scss'],
})
export class DeployModalComponent implements OnInit {

  @Input() project!: Project;
  @Output() closed = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private pipelineSvc = inject(PipelineService);

  pipelines = signal<Pipeline[]>([]);
  deploying = signal(false);
  deployError = signal('');
  deploySuccess = signal(false);

  readonly environments = [
    { value: 'DEV', label: 'Development (DEV)' },
    { value: 'STAGING', label: 'Staging (STG)' },
    { value: 'PROD', label: 'Production (PROD)' },
  ];

  form: FormGroup = this.fb.group({
    environment: ['DEV', Validators.required],
    version:     ['v1.0.0', [Validators.required, Validators.pattern(/^v\d+\.\d+\.\d+$/)]],
    jenkinsJob:  ['', Validators.required],
    pipeline:    [''],
  });

  ngOnInit(): void {
    this.pipelineSvc.getByProject(this.project.id).subscribe({
      next: data => {
        this.pipelines.set(data);
        if (data.length > 0) {
          this.form.patchValue({ pipeline: data[0].id });
        }
      },
    });
  }

  close(): void { this.closed.emit(); }

  deploy(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.deploying.set(true);
    this.deployError.set('');

    const pipelineId = this.form.value.pipeline;
    if (!pipelineId) { this.deployError.set('Sélectionnez un pipeline'); this.deploying.set(false); return; }

    this.pipelineSvc.trigger(+pipelineId, {
      commitHash: this.form.value.version,
      userId: 0,
    }).subscribe({
      next: () => {
        this.deploying.set(false);
        this.deploySuccess.set(true);
        setTimeout(() => this.close(), 1800);
      },
      error: (e) => {
        this.deployError.set(e?.error?.message ?? 'Erreur lors du déploiement');
        this.deploying.set(false);
      },
    });
  }
}
