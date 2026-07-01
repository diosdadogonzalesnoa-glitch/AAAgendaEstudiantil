import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AuthService } from '../../../../core/services/autenticacion/auth.service';

@Component({
  selector: 'app-cursos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cursos.html',
  styleUrl: './cursos.scss'
})
export class Cursos implements OnInit {
  cursos: any[] = [];
  showForm = false;
  showEditForm = false;
  userName = '';

  dias  = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];
  horas = ['7am', '8am', '9am', '10am', '11am', '12pm', '1pm', '2pm', '3pm', '4pm', '5pm', '6pm', '7pm', '8pm', '9pm'];

  newDia  = '';
  newHora = '';
  newCurso = { name: '', teacher: '', schedule: '' };

  editDia  = '';
  editHora = '';
  editCurso: any = { id: null, name: '', teacher: '', schedule: '' };

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

  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    this.userName = this.auth.getCurrentUser()?.name ?? '';
    this.cargar();
  }

  getGradient(index: number): string {
    return this.gradients[index % this.gradients.length];
  }

  getInitials(name: string): string {
    return name.trim().split(' ').filter(w => w).map(w => w[0]).join('').toUpperCase().slice(0, 2);
  }

  cargar(): void {
    this.http.get<any[]>(`${environment.apiUrl}/docente/cursos`).subscribe({
      next: (res) => this.cursos = res,
      error: () => this.cursos = []
    });
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.showEditForm = false;
    this.newDia  = '';
    this.newHora = '';
    this.newCurso = { name: '', teacher: this.userName, schedule: '' };
  }

  agregarCurso(): void {
    if (!this.newDia || !this.newHora) return;
    this.newCurso.teacher  = this.userName;
    this.newCurso.schedule = `${this.newDia} ${this.newHora}`;
    this.http.post(`${environment.apiUrl}/docente/cursos`, this.newCurso).subscribe({
      next: () => {
        this.newCurso = { name: '', teacher: '', schedule: '' };
        this.newDia  = '';
        this.newHora = '';
        this.showForm = false;
        this.cargar();
      },
      error: () => alert('Error al crear el curso.')
    });
  }

  editarCurso(curso: any): void {
    this.editCurso = { ...curso };
    const parts = (curso.schedule ?? '').split(' ');
    this.editDia  = parts[0] ?? '';
    this.editHora = parts[1] ?? '';
    this.showEditForm = true;
    this.showForm = false;
  }

  actualizarCurso(): void {
    if (!this.editDia || !this.editHora) return;
    this.editCurso.teacher  = this.userName;
    this.editCurso.schedule = `${this.editDia} ${this.editHora}`;
    this.http.put(`${environment.apiUrl}/docente/cursos/${this.editCurso.id}`, this.editCurso).subscribe({
      next: () => { this.showEditForm = false; this.cargar(); },
      error: () => alert('Error al actualizar el curso.')
    });
  }

  toggleStatus(curso: any): void {
    const next = curso.status === 'TERMINADO' ? 'ACTIVO' : 'TERMINADO';
    this.http.patch(`${environment.apiUrl}/docente/cursos/${curso.id}/status`, { status: next }).subscribe({
      next: () => curso.status = next,
      error: () => {}
    });
  }

  eliminarCurso(id: number): void {
    if (!confirm('¿Seguro que deseas eliminar este curso?')) return;
    this.http.delete(`${environment.apiUrl}/docente/cursos/${id}`).subscribe({
      next: () => this.cargar(),
      error: () => alert('Error al eliminar el curso.')
    });
  }
}
