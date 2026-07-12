import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-curso-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './curso-detalle.html',
  styleUrl: './curso-detalle.scss'
})
export class CursoDetalle implements OnInit {
  curso: any = null;
  tareas: any[] = [];
  lessonGroups: any[] = [];

  /* publish panel */
  activeTab = 'tarea';
  newTarea = { title: '', description: '', dueDate: '', priority: 'MEDIA', taskType: 'TAREA', maxAttempts: 1, maxFiles: 20 };
  newPractica = { title: '', description: '', dueDate: '' };
  newLesson = { title: '', description: '', groupTitle: '' };
  lessonFile: File | null = null;

  /* practice question builder (create) */
  practiceQuestions: Array<{
    questionText: string;
    writtenQuestion: boolean;
    options: Array<{ optionText: string; correct: boolean }>;
  }> = [];
  practiceError = '';
  practiceSuccess = false;

  /* practice edit panel */
  editingPracticeId: number | null = null;
  editPracticeForm = { title: '', description: '', dueDate: '' };
  editPracticeQuestions: Array<{
    questionText: string;
    writtenQuestion: boolean;
    options: Array<{ optionText: string; correct: boolean }>;
  }> = [];
  editPracticeError = '';
  editPracticeSuccess = false;
  editPracticeHasSubmissions = false;

  /* edit panel */
  editingTaskId: number | null = null;
  editForm = { description: '', dueDate: '', maxAttempts: 1, maxFiles: 20 };

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<any>(`${environment.apiUrl}/docente/cursos/${id}`).subscribe({
      next: (res) => {
        this.curso = res.course ?? res;
        this.tareas = res.tasks ?? [];
        this.lessonGroups = res.lessonGroups ?? [];
      },
      error: () => {}
    });
  }

  showTab(tab: string): void { this.activeTab = tab; }

  publicarTarea(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.post(`${environment.apiUrl}/docente/cursos/${id}/tareas`, this.newTarea).subscribe({
      next: () => {
        this.newTarea = { title: '', description: '', dueDate: '', priority: 'MEDIA', taskType: 'TAREA', maxAttempts: 1, maxFiles: 20 };
        this.ngOnInit();
      },
      error: () => alert('Error al publicar tarea.')
    });
  }

  openEdit(task: any): void {
    this.editingTaskId = task.id;
    const due = task.dueDate ? new Date(task.dueDate).toISOString().slice(0, 16) : '';
    this.editForm = {
      description: task.description ?? '',
      dueDate: due,
      maxAttempts: task.maxAttempts ?? 1,
      maxFiles: task.maxFiles ?? 20
    };
  }

  cancelEdit(): void {
    this.editingTaskId = null;
  }

  saveEdit(): void {
    if (!this.editingTaskId) return;
    const payload = {
      description: this.editForm.description,
      dueDate: this.editForm.dueDate,
      maxAttempts: this.editForm.maxAttempts,
      maxFiles: this.editForm.maxFiles
    };
    this.http.put(`${environment.apiUrl}/docente/tareas/${this.editingTaskId}`, payload).subscribe({
      next: () => {
        const t = this.tareas.find(x => x.id === this.editingTaskId);
        if (t) {
          t.description = payload.description;
          t.dueDate = payload.dueDate;
          t.maxAttempts = payload.maxAttempts;
          t.maxFiles = payload.maxFiles;
        }
        this.editingTaskId = null;
      },
      error: () => alert('Error al guardar cambios.')
    });
  }

  addQuestion(): void {
    this.practiceQuestions.push({
      questionText: '',
      writtenQuestion: false,
      options: [
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
      ]
    });
  }

  removeQuestion(index: number): void {
    this.practiceQuestions.splice(index, 1);
  }

  setCorrect(q: { options: Array<{ optionText: string; correct: boolean }> }, optIdx: number): void {
    q.options.forEach((o, i) => o.correct = i === optIdx);
  }

  toggleWritten(qIdx: number): void {
    const q = this.practiceQuestions[qIdx];
    q.writtenQuestion = !q.writtenQuestion;
    if (!q.writtenQuestion && q.options.length === 0) {
      q.options = [
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
      ];
    }
  }

  publicarPractica(): void {
    this.practiceError = '';
    const count = this.practiceQuestions.length;
    if (count !== 5 && count !== 10 && count !== 20) {
      this.practiceError = `Debes agregar exactamente 5, 10 o 20 preguntas. Tienes ${count}.`;
      return;
    }
    for (let i = 0; i < this.practiceQuestions.length; i++) {
      const q = this.practiceQuestions[i];
      if (!q.questionText.trim()) {
        this.practiceError = `La pregunta ${i + 1} no tiene enunciado.`;
        return;
      }
      if (!q.writtenQuestion) {
        const filled = q.options.filter(o => o.optionText.trim()).length;
        if (filled < 2) {
          this.practiceError = `La pregunta ${i + 1} necesita al menos 2 alternativas.`;
          return;
        }
        if (!q.options.some(o => o.correct)) {
          this.practiceError = `La pregunta ${i + 1} no tiene respuesta correcta marcada.`;
          return;
        }
      }
    }
    const id = this.route.snapshot.paramMap.get('id');
    const payload = { ...this.newPractica, questions: this.practiceQuestions };
    this.http.post(`${environment.apiUrl}/docente/cursos/${id}/practicas`, payload).subscribe({
      next: () => {
        this.newPractica = { title: '', description: '', dueDate: '' };
        this.practiceQuestions = [];
        this.practiceSuccess = true;
        setTimeout(() => { this.practiceSuccess = false; }, 3000);
        this.ngOnInit();
      },
      error: (err) => {
        this.practiceError = err?.error?.error ?? 'Error al publicar práctica.';
      }
    });
  }

  openEditPractica(task: any): void {
    this.editingPracticeId = task.id;
    this.editPracticeError = '';
    this.editPracticeSuccess = false;
    this.editPracticeHasSubmissions = (task.submissionsCount ?? 0) > 0;
    const due = task.dueDate ? new Date(task.dueDate).toISOString().slice(0, 16) : '';
    this.editPracticeForm = { title: task.title ?? '', description: task.description ?? '', dueDate: due };
    this.http.get<any[]>(`${environment.apiUrl}/docente/practicas/${task.id}/preguntas`).subscribe({
      next: (qs) => {
        this.editPracticeQuestions = qs.map(q => ({
          questionText: q.questionText,
          writtenQuestion: q.writtenQuestion,
          options: q.writtenQuestion ? [] : (q.options ?? []).map((o: any) => ({
            optionText: o.optionText,
            correct: o.correct
          }))
        }));
      },
      error: () => { this.editPracticeError = 'Error al cargar preguntas.'; }
    });
  }

  cancelEditPractica(): void {
    this.editingPracticeId = null;
    this.editPracticeQuestions = [];
    this.editPracticeError = '';
  }

  addEditQuestion(): void {
    this.editPracticeQuestions.push({
      questionText: '',
      writtenQuestion: false,
      options: [
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
      ]
    });
  }

  removeEditQuestion(index: number): void {
    this.editPracticeQuestions.splice(index, 1);
  }

  setEditCorrect(q: { options: Array<{ optionText: string; correct: boolean }> }, optIdx: number): void {
    q.options.forEach((o, i) => o.correct = i === optIdx);
  }

  toggleEditWritten(qIdx: number): void {
    const q = this.editPracticeQuestions[qIdx];
    q.writtenQuestion = !q.writtenQuestion;
    if (!q.writtenQuestion && q.options.length === 0) {
      q.options = [
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
        { optionText: '', correct: false },
      ];
    }
  }

  saveEditPractica(): void {
    this.editPracticeError = '';
    if (!this.editingPracticeId) return;

    if (!this.editPracticeHasSubmissions) {
      const count = this.editPracticeQuestions.length;
      if (count !== 5 && count !== 10 && count !== 20) {
        this.editPracticeError = `Debes tener exactamente 5, 10 o 20 preguntas. Tienes ${count}.`;
        return;
      }
      for (let i = 0; i < this.editPracticeQuestions.length; i++) {
        const q = this.editPracticeQuestions[i];
        if (!q.questionText.trim()) {
          this.editPracticeError = `La pregunta ${i + 1} no tiene enunciado.`;
          return;
        }
        if (!q.writtenQuestion) {
          const filled = q.options.filter(o => o.optionText.trim()).length;
          if (filled < 2) { this.editPracticeError = `La pregunta ${i + 1} necesita al menos 2 alternativas.`; return; }
          if (!q.options.some(o => o.correct)) { this.editPracticeError = `La pregunta ${i + 1} no tiene respuesta correcta.`; return; }
        }
      }
    }

    const payload: any = { ...this.editPracticeForm };
    if (!this.editPracticeHasSubmissions) payload.questions = this.editPracticeQuestions;

    this.http.put(`${environment.apiUrl}/docente/practicas/${this.editingPracticeId}`, payload).subscribe({
      next: () => {
        this.editPracticeSuccess = true;
        const task = this.tareas.find(t => t.id === this.editingPracticeId);
        if (task) {
          task.title = this.editPracticeForm.title;
          task.description = this.editPracticeForm.description;
          task.dueDate = this.editPracticeForm.dueDate;
        }
        setTimeout(() => { this.editingPracticeId = null; this.editPracticeSuccess = false; }, 1500);
      },
      error: (err) => { this.editPracticeError = err?.error?.error ?? 'Error al guardar.'; }
    });
  }

  onLessonFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.lessonFile = input.files?.[0] ?? null;
  }

  publicarLeccion(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const fd = new FormData();
    fd.append('title', this.newLesson.title);
    fd.append('description', this.newLesson.description);
    fd.append('groupTitle', this.newLesson.groupTitle);
    if (this.lessonFile) fd.append('file', this.lessonFile);
    this.http.post(`${environment.apiUrl}/docente/cursos/${id}/lecciones`, fd).subscribe({
      next: () => {
        this.newLesson = { title: '', description: '', groupTitle: '' };
        this.lessonFile = null;
        this.ngOnInit();
      },
      error: () => alert('Error al publicar lección.')
    });
  }

  eliminarTarea(tareaId: number): void {
    if (!confirm('¿Eliminar esta tarea?')) return;
    this.http.delete(`${environment.apiUrl}/docente/tareas/${tareaId}`).subscribe({
      next: () => this.tareas = this.tareas.filter(t => t.id !== tareaId),
      error: () => alert('Error al eliminar.')
    });
  }

  eliminarLeccion(lessonId: number): void {
    if (!confirm('¿Eliminar esta lección?')) return;
    this.http.delete(`${environment.apiUrl}/docente/lecciones/${lessonId}`).subscribe({
      next: () => {
        this.lessonGroups = this.lessonGroups.map(g => ({
          ...g,
          lessons: g.lessons.filter((l: any) => l.id !== lessonId)
        })).filter(g => g.lessons.length > 0);
      },
      error: () => alert('Error al eliminar lección.')
    });
  }
}
