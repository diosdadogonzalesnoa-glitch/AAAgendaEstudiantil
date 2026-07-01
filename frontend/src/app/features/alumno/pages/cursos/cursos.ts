import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-cursos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cursos.html',
  styleUrl: './cursos.scss'
})
export class Cursos implements OnInit {
  cursos: any[] = [];
  search = '';
  searchMode = false;

  private gradients = [
    'linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%)',
    'linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%)',
    'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
    'linear-gradient(135deg, #10b981 0%, #059669 100%)',
    'linear-gradient(135deg, #ec4899 0%, #8b5cf6 100%)',
    'linear-gradient(135deg, #f97316 0%, #eab308 100%)',
    'linear-gradient(135deg, #6366f1 0%, #06b6d4 100%)',
    'linear-gradient(135deg, #14b8a6 0%, #3b82f6 100%)',
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.cargarMisCursos();
  }

  getGradient(index: number): string {
    return this.gradients[index % this.gradients.length];
  }

  getInitials(name: string): string {
    return name.trim().split(' ').filter(w => w).map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  cargarMisCursos(): void {
    this.http.get<any[]>(`${environment.apiUrl}/alumno/cursos`).subscribe({
      next: (res) => { this.cursos = res; this.searchMode = false; },
      error: () => { this.cursos = []; }
    });
  }

  buscar(): void {
    if (!this.search.trim()) { this.cargarMisCursos(); return; }
    this.http.get<any[]>(`${environment.apiUrl}/alumno/cursos/buscar?search=${encodeURIComponent(this.search)}`).subscribe({
      next: (res) => { this.cursos = res; this.searchMode = true; },
      error: () => { this.cursos = []; this.searchMode = true; }
    });
  }

  inscribirse(cursoId: number): void {
    this.http.post(`${environment.apiUrl}/alumno/cursos/inscribir/${cursoId}`, {}).subscribe({
      next: () => { this.search = ''; this.cargarMisCursos(); },
      error: () => alert('No se pudo inscribir al curso.')
    });
  }
}
