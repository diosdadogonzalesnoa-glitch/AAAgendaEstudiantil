import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { docenteGuard, alumnoGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },

  // Auth
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },

  // Layout principal (requiere login)
  {
    path: '',
    loadComponent: () => import('./layouts/Main/main-layout/main-layout').then(m => m.MainLayout),
    canActivate: [authGuard],
    children: [

      // Alumno
      {
        path: 'alumno',
        canActivate: [alumnoGuard],
        loadChildren: () => import('./features/alumno/alumno.routes').then(m => m.ALUMNO_ROUTES)
      },

      // Docente
      {
        path: 'docente',
        canActivate: [docenteGuard],
        loadChildren: () => import('./features/docente/docente.routes').then(m => m.DOCENTE_ROUTES)
      },

      // Compartidos (perfil, recordatorios)
      {
        path: 'perfil',
        loadComponent: () => import('./features/shared-features/perfil/perfil').then(m => m.Perfil)
      },
      {
        path: 'recordatorios',
        loadComponent: () => import('./features/shared-features/recordatorios/recordatorios').then(m => m.Recordatorios)
      }
    ]
  },

  { path: '**', redirectTo: 'auth/login' }
];
