import { Routes } from '@angular/router';

export const DOCENTE_ROUTES: Routes = [
  { path: 'dashboard',           loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard) },
  { path: 'cursos',              loadComponent: () => import('./pages/cursos/cursos').then(m => m.Cursos) },
  { path: 'cursos/:id',          loadComponent: () => import('./pages/curso-detalle/curso-detalle').then(m => m.CursoDetalle) },
  { path: 'tareas',              loadComponent: () => import('./pages/tareas/tareas').then(m => m.Tareas) },
  { path: 'tareas/:id/entregas', loadComponent: () => import('./pages/tarea-entregas/tarea-entregas').then(m => m.TareaEntregas) },
  { path: 'estudio',             loadComponent: () => import('./pages/estudio/estudio').then(m => m.DocenteEstudio) },
  { path: 'lecciones',           loadComponent: () => import('./pages/lecciones/lecciones').then(m => m.Lecciones) },
  { path: 'leccion/:id',         loadComponent: () => import('../alumno/pages/leccion/leccion').then(m => m.Leccion) },
  { path: '',                    redirectTo: 'dashboard', pathMatch: 'full' }
];
