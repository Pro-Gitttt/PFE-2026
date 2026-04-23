import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ProjectService } from '../../core/services/project.service';
import { CreateProjectRequest, Project, VcsType } from '../../core/models/project.model';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss'],
})
export class ProjectsComponent implements OnInit {

  // ✅ inject instead of constructor
  private fb = inject(FormBuilder);
  private svc = inject(ProjectService);

  // ================= STATE =================
  projects  = signal<Project[]>([]);
  loading   = signal(true);
  showForm  = signal(false);
  creating  = signal(false);
  formError = signal('');

  // ✅ NOW this works (fb already initialized)
  form: FormGroup = this.fb.group({
    name:          ['', Validators.required],
    repositoryUrl: ['', Validators.required],
    branch:        ['main', Validators.required],
    owner:         [''],
    vcsType:       ['GITHUB' as VcsType],
  });

  readonly vcsTypes: VcsType[] = ['GITHUB', 'GITLAB', 'BITBUCKET'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.svc.getAll().subscribe({
      next:  p => {
        this.projects.set(p);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggleForm(): void {
    this.showForm.update(v => !v);
    this.formError.set('');

    if (!this.showForm()) {
      this.form.reset({ branch: 'main', vcsType: 'GITHUB' });
    }
  }

  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.creating.set(true);
    this.formError.set('');

    this.svc.create(this.form.value as CreateProjectRequest).subscribe({
      next: () => {
        this.creating.set(false);
        this.toggleForm();
        this.load();
      },
      error: (e) => {
        this.formError.set(e.error?.message ?? 'Erreur lors de la création');
        this.creating.set(false);
      },
    });
  }

  delete(id: number, name: string): void {
    if (!confirm(`Supprimer le projet "${name}" ? Cette action est irréversible.`)) return;

    this.svc.delete(id).subscribe({
      next: () => this.load(),
    });
  }
}