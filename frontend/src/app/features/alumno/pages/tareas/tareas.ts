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
  stats: any = {};
  openDeliveries: Record<number, boolean> = {};
  selectedFiles: Record<number, File[]> = {};

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<any>(`${environment.apiUrl}/alumno/tareas`).subscribe({
      next: (res) => {
        this.tareas = res.tasks ?? [];
        this.stats = {
          totalTasks: res.totalTasks ?? 0,
          pendingTasks: res.pendingTasks ?? 0,
          completedTasks: res.completedTasks ?? 0,
          overdueTasks: res.overdueTasks ?? 0
        };
      },
      error: () => { this.tareas = []; }
    });
  }

  isPast(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', hour12: false
    });
  }

  toggleDelivery(taskId: number): void {
    this.openDeliveries[taskId] = !this.openDeliveries[taskId];
  }

  onFileChange(event: Event, taskId: number, maxFiles: number = 20): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const existing = this.selectedFiles[taskId] ?? [];
    const incoming = Array.from(input.files);
    const combined = [...existing, ...incoming];
    const unique = combined.filter((f, i, arr) =>
      arr.findIndex(x => x.name === f.name && x.size === f.size) === i
    );
    this.selectedFiles[taskId] = unique.slice(0, maxFiles);
    input.value = '';
  }

  removeFile(taskId: number, index: number): void {
    const files = [...(this.selectedFiles[taskId] ?? [])];
    files.splice(index, 1);
    this.selectedFiles[taskId] = files;
  }

  submitDelivery(taskId: number, event: Event): void {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const textarea = form.querySelector('textarea') as HTMLTextAreaElement;
    const answerText = textarea?.value?.trim() ?? '';
    const files = this.selectedFiles[taskId] ?? [];

    if (!answerText && !files.length) {
      alert('Debes escribir una respuesta o subir al menos un archivo.');
      return;
    }

    const formData = new FormData();
    formData.append('answerText', answerText);
    files.forEach(f => formData.append('files', f));

    this.http.post(`${environment.apiUrl}/alumno/tareas/${taskId}/entregar`, formData).subscribe({
      next: () => {
        const tarea = this.tareas.find(t => t.id === taskId);
        if (tarea) {
          tarea.submitted = true;
          tarea.submissionStatus = 'PENDIENTE_CALIFICACION';
          tarea.attemptNumber = (tarea.attemptNumber ?? 0) + 1;
        }
        if (this.stats.pendingTasks > 0) this.stats.pendingTasks--;
        this.openDeliveries[taskId] = false;
      },
      error: (err: any) => alert(err?.error?.error ?? 'Error al enviar la entrega.')
    });
  }
}
