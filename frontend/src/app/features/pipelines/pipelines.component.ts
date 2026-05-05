import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { PipelineService } from '../../core/services/pipeline.service';
import { ProjectService } from '../../core/services/project.service';

import {
  Pipeline,
  PipelineExecution,
  PipelineStatus
} from '../../core/models/pipeline.model';

@Component({
  selector: 'app-pipelines',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pipelines.component.html',
  styleUrls: ['./pipelines.component.scss']
})
export class PipelinesComponent implements OnInit {

  // ================= SIGNALS =================
  projects = signal<any[]>([]);
  pipelines = signal<Pipeline[]>([]);
  executions = signal<PipelineExecution[]>([]);

  selectedProject = signal<any | null>(null);
  selectedPipeline = signal<Pipeline | null>(null);

  triggering = signal(false);
  triggerError = signal('');

  commitCtrl = new FormControl('', [Validators.required]);

  constructor(
    private pipelineService: PipelineService,
    private projectService: ProjectService
  ) {}

  // ================= INIT =================
  ngOnInit(): void {
    this.loadProjects();
  }

  // ================= LOAD DATA =================
  loadProjects() {
    this.projectService.getAll().subscribe({
      next: res => this.projects.set(res),
      error: () => console.error('Error loading projects')
    });
  }

  onProjectChange(event: any) {
    const projectId = +event.target.value;

    if (!projectId) return;

    this.selectedProject.set(projectId);
    this.selectedPipeline.set(null);
    this.executions.set([]);

    this.pipelineService.getByProject(projectId).subscribe({
      next: res => this.pipelines.set(res),
      error: () => console.error('Error loading pipelines')
    });
  }

  selectPipeline(p: Pipeline) {
    this.selectedPipeline.set(p);

    this.pipelineService.getExecutions(p.id).subscribe({
      next: res => this.executions.set(res),
      error: () => console.error('Error loading executions')
    });
  }

  // ================= TRIGGER =================
  trigger() {
    if (!this.selectedPipeline() || this.commitCtrl.invalid) return;

    this.triggering.set(true);
    this.triggerError.set('');

    this.pipelineService.trigger(
      this.selectedPipeline()!.id,
      {
        commitHash: this.commitCtrl.value!,
        userId: 'admin' // TODO: replace with real user from AuthService
      }
    ).subscribe({
      next: () => {
        this.triggering.set(false);
        this.commitCtrl.reset();

        // reload executions
        this.selectPipeline(this.selectedPipeline()!);
      },
      error: () => {
        this.triggerError.set('Erreur lors du trigger');
        this.triggering.set(false);
      }
    });
  }

  // ================= HELPERS =================

  statusClass(status: PipelineStatus): string {
    return {
      CREATED: 'status-created',
      RUNNING: 'status-running',
      SUCCESS: 'status-success',
      FAILED: 'status-failed',
      CANCELLED: 'status-cancelled',
      PENDING: 'status-pending'   // ✅ FIXED
    }[status] || '';
  }

  getDuration(start: string, end: string): string {
    const diff = new Date(end).getTime() - new Date(start).getTime();
    const seconds = Math.floor(diff / 1000);
    return `${seconds}s`;
  }
}