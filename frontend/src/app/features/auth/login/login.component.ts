import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit {

  form: FormGroup;

  loading = signal(false);
  error = signal('');
  showPass = signal(false);
  ready = signal(false);

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

  ngOnInit(): void {
    setTimeout(() => this.ready.set(true), 50);

    // FIX: signal must be called
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  get f() {
    return this.form.controls;
  }

  togglePass(): void {
    this.showPass.update(v => !v);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    const payload = this.form.getRawValue(); // ✅ safer than form.value

    this.auth.login(payload).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },

      error: (e) => {
        console.error('LOGIN ERROR:', e);

        this.error.set(
          e.error?.message ||
          e.error?.error ||
          (e.status === 401
            ? 'Identifiants incorrects'
            : 'Erreur serveur, réessayez')
        );

        this.loading.set(false);
      }
    });
  }
}