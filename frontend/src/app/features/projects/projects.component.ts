import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';

import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import { CreateProjectRequest, Project, VcsType } from '../../core/models/project.model';
import { DeployModalComponent } from '../Deploy/Deploy-modal.component';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DeployModalComponent],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss'],
})
export class ProjectsComponent implements OnInit {

  private fb  = inject(FormBuilder);
  private svc = inject(ProjectService);
  readonly auth = inject(AuthService);

  projects  = signal<Project[]>([]);
  loading   = signal(true);
  showForm  = signal(false);
  formError = signal('');
  deployingProject = signal<Project | null>(null);

  form: FormGroup = this.fb.group({
    name:          ['', Validators.required],
    repositoryUrl: ['', Validators.required],
    branch:        ['main', Validators.required],
    owner:         [''],
    vcsType:       [VcsType.GITHUB, Validators.required],
  });

  readonly vcsTypes = Object.values(VcsType);

  readonly canCreate = computed(() => ['ADMIN', 'DEV', 'DEVOPS'].includes(this.auth.role));
  readonly canDeploy = computed(() => ['ADMIN', 'DEVOPS'].includes(this.auth.role));
  readonly canDelete = computed(() => ['ADMIN', 'DEV', 'DEVOPS'].includes(this.auth.role));

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.svc.getAll().subscribe({
      next: data => { this.projects.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  toggleForm(): void {
    this.showForm.update(v => !v);
    this.formError.set('');
    if (!this.showForm()) {
      this.form.reset({ name: '', repositoryUrl: '', branch: 'main', owner: '', vcsType: VcsType.GITHUB });
    }
  }

  create(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const payload: CreateProjectRequest = {
      name:          this.form.value.name?.trim(),
      repositoryUrl: this.form.value.repositoryUrl?.trim(),
      branch:        this.form.value.branch?.trim(),
      owner:         this.form.value.owner?.trim() || undefined,
      vcsType:       this.form.value.vcsType,
    };
    this.svc.create(payload).subscribe({
      next: () => { this.toggleForm(); this.load(); },
      error: (e) => this.formError.set(e.error?.message ?? 'Erreur création'),
    });
  }

  delete(id: number, name: string): void {
    if (!confirm(`Supprimer "${name}" ?`)) return;
    this.svc.delete(id).subscribe({ next: () => this.load() });
  }

  openDeploy(project: Project): void { this.deployingProject.set(project); }
  closeDeploy(): void { this.deployingProject.set(null); }
}
