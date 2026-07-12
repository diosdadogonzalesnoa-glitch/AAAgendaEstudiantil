import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/autenticacion/auth.service';

export const docenteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isDocente()) return true;

  router.navigate(['/alumno/dashboard']);
  return false;
};

export const alumnoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAlumno()) return true;

  router.navigate(['/docente/dashboard']);
  return false;
};
