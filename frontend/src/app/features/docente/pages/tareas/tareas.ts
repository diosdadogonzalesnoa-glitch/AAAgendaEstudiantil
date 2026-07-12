import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-tareas',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tareas.html',
  styleUrl: './tareas.scss'
})
export class Tareas implements OnInit {
  tareas: any[] = [];
  receivedCount = 0;
  notReceivedCount = 0;

  constructor(private http: HttpClient) {}

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false });
  }

  ngOnInit(): void {
    this.http.get<any>(`${environment.apiUrl}/docente/tareas`).subscribe({
      next: (res) => {
        this.tareas          = res.tasks ?? res ?? [];
        this.receivedCount   = res.receivedTasks    ?? 0;
        this.notReceivedCount = res.notReceivedTasks ?? 0;
      },
      error: () => this.tareas = []
    });
  }

  isPast(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  eliminarTarea(id: number): void {
    if (!confirm('¿Estás seguro de eliminar esta actividad?')) return;
    this.http.delete(`${environment.apiUrl}/docente/tareas/${id}`).subscribe({
      next: () => this.tareas = this.tareas.filter(t => t.id !== id),
      error: () => alert('Error al eliminar la tarea.')
    });
  }
}
