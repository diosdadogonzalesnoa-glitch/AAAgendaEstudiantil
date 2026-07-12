import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';
import { AuthService } from '../../../../core/services/autenticacion/auth.service';

@Component({
  selector: 'app-curso-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './curso-detalle.html',
  styleUrl: './curso-detalle.scss'
})
export class CursoDetalle implements OnInit {
  curso: any = null;
  tareas: any[] = [];
  lessonGroups: any[] = [];
  taskStatus: Record<number, string> = {};
  taskGrades: Record<number, number> = {};
  activeTab = 'taskPanel';

  get isDocente(): boolean { return this.auth.isDocente(); }
  get isAlumno(): boolean { return this.auth.isAlumno(); }

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private auth: AuthService
  ) {}

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<any>(`${environment.apiUrl}/alumno/cursos/${id}`).subscribe({
      next: (res) => {
        this.curso = res.course;
        this.tareas = res.tasks ?? [];
        this.lessonGroups = res.lessonGroups ?? [];
        this.taskStatus = res.taskStatus ?? {};
        this.taskGrades = res.taskGrades ?? {};
      },
      error: () => {}
    });
  }

  showTab(tab: string): void { this.activeTab = tab; }

  getTaskStatusClass(taskId: number): string {
    const s = this.taskStatus[taskId];
    if (s === 'CALIFICADO') return this.taskGrades[taskId] >= 12 ? 'student-approved' : 'student-failed';
    if (s === 'PENDIENTE_CALIFICACION') return 'student-pending-grade';
    return 'student-warning';
  }

  getTaskStatusText(task: any): string {
    const s = this.taskStatus[task.id];
    if (s === 'CALIFICADO') {
      const g = this.taskGrades[task.id];
      return g >= 12 ? `✅ Nota final: ${g}/20 - Aprobado` : `❌ Nota final: ${g}/20 - Desaprobado`;
    }
    if (s === 'PENDIENTE_CALIFICACION') return '📝 Tarea enviada correctamente. Será calificada por el docente.';
    if (task.dueDate && new Date(task.dueDate) < new Date()) return '⏰ Actividad vencida. No se registró ninguna entrega.';
    return '⚠️ Recuerda resolver esta actividad desde la sección de Tareas.';
  }
}
