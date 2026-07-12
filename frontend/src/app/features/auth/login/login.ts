import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/autenticacion/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  form: FormGroup;
  error = '';
  success = '';
  loading = false;
  selectedRole: 'ESTUDIANTE' | 'DOCENTE' | '' = '';
  showPassword = false;

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  selectRole(role: 'ESTUDIANTE' | 'DOCENTE'): void {
    this.selectedRole = role;
  }

  submit(): void {
    if (this.form.invalid) return;
    if (!this.selectedRole) {
      this.error = 'Selecciona un rol antes de continuar.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.success = '';

    this.auth.login(this.form.value).subscribe({
      next: (res) => {
        if (res.role === 'DOCENTE') {
          this.router.navigate(['/docente/dashboard']);
        } else {
          this.router.navigate(['/alumno/dashboard']);
        }
      },
      error: (err) => {
        this.error = err.error?.error ?? 'Error al iniciar sesión';
        this.loading = false;
      }
    });
  }
}
