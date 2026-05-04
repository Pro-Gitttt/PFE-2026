import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';

import { ProjectService } from '../../core/services/project.service';
import { PipelineService } from '../../core/services/pipeline.service';

import {
  Project
} from '../../core/models/project.model';

import {
  Pipeline,
  PipelineExecution,
  PipelineStatus,
  TriggerExecutionRequest
} from '../../core/models/pipeline.model';

@Component({
  selector: 'app-pipelines',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.scss'],
})
export class PipelinesComponent implements OnInit {

  projects = signal<Project[]>([]);
  pipelines = signal<Pipeline[]>([]);
  executions = signal<PipelineExecution[]>([]);
  

  selectedProject = signal<Project | null>(null);
  selectedPipeline = signal<Pipeline | null>(null);

  triggering = signal(false);
  triggerError = signal('');

  commitCtrl = new FormControl('', [
    Validators.required,
    Validators.minLength(5),
  ]);

  constructor(
    private projSvc: ProjectService,
    private plSvc: PipelineService,
  ) {}

  ngOnInit(): void {
    this.projSvc.getAll().subscribe({
      next: data => this.projects.set(data),
    });
  }

  onProjectChange(ev: Event): void {
    const id = +(ev.target as HTMLSelectElement).value;

    const project = this.projects().find(p => p.id === id) ?? null;
    this.selectedProject.set(project);

    this.selectedPipeline.set(null);
    this.executions.set([]);

    if (!id) {
      this.pipelines.set([]);
      return;
    }

    this.plSvc.getByProject(id).subscribe({
      next: data => this.pipelines.set(data),
    });
  }

  selectPipeline(p: Pipeline): void {
    this.selectedPipeline.set(p);
    this.loadExecutions(p.id);
  }

  loadExecutions(pipelineId: number): void {
    this.plSvc.getExecutions(pipelineId).subscribe({
      next: data => this.executions.set(data),
      error: err => console.error(err),
    });
  }

  trigger(): void {
    const pipeline = this.selectedPipeline();
    if (!pipeline || this.commitCtrl.invalid) return;

    this.triggering.set(true);
    this.triggerError.set('');

    const body: TriggerExecutionRequest = {
      commitHash: this.commitCtrl.value!,
      userId: 0, // backend extracts from JWT
    };

    this.plSvc.trigger(pipeline.id, body).subscribe({
      next: () => {
        this.triggering.set(false);
        this.commitCtrl.reset();
        this.loadExecutions(pipeline.id);
      },
      error: (e: any) => {
        this.triggerError.set(e?.error?.message ?? 'Error triggering pipeline');
        this.triggering.set(false);
      },
    });
  }

  statusClass(status: PipelineStatus): string {
    return {
      SUCCESS: 'ok',
      FAILED: 'fail',
      RUNNING: 'run',
      CREATED: 'info',
      CANCELLED: 'warn',
    }[status] ?? '';
  }

  getDuration(start: string, end: string): string {
    const ms = new Date(end).getTime() - new Date(start).getTime();

    if (ms < 60000) return `${Math.round(ms / 1000)}s`;

    return `${Math.round(ms / 60000)}m`;
  }
}