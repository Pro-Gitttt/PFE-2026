// ── login.component.ts ─────────────────────────────────────────
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
 
@Component({
  selector:    'app-login',
  standalone:  true,
  imports:     [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls:   ['./login.component.scss'],
})
export class LoginComponent {
  form: FormGroup;

  loading  = signal(false);
  error    = signal('');
  showPass = signal(false);

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  get f() { return this.form.controls; }

  togglePass(): void { this.showPass.update(v => !v); }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');

    this.auth.login(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => {
        const msg = e.status === 401
          ? 'Identifiants incorrects'
          : 'Erreur serveur, réessayez';
        this.error.set(msg);
        this.loading.set(false);
      },
    });
  }
}