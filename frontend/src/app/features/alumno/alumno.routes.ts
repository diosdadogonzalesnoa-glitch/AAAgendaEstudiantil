import { Routes } from '@angular/router';

export const ALUMNO_ROUTES: Routes = [
  { path: 'dashboard',       loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.Dashboard) },
  { path: 'cursos',          loadComponent: () => import('./pages/cursos/cursos').then(m => m.Cursos) },
  { path: 'cursos/:id',      loadComponent: () => import('./pages/curso-detalle/curso-detalle').then(m => m.CursoDetalle) },
  { path: 'tareas',          loadComponent: () => import('./pages/tareas/tareas').then(m => m.Tareas) },
  { path: 'tareas/:id',      loadComponent: () => import('./pages/tarea-detalle/tarea-detalle').then(m => m.TareaDetalle) },
  { path: 'practica/:id',    loadComponent: () => import('./pages/practica/practica').then(m => m.Practica) },
  { path: 'leccion/:id',     loadComponent: () => import('./pages/leccion/leccion').then(m => m.Leccion) },
  { path: 'estudio',         loadComponent: () => import('./pages/estudio/estudio').then(m => m.Estudio) },
  { path: '',                redirectTo: 'dashboard', pathMatch: 'full' }
];
