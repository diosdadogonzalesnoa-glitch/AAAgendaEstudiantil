import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-tarea-entregas',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './tarea-entregas.html',
  styleUrl: './tarea-entregas.scss'
})
export class TareaEntregas implements OnInit {
  tarea: any = null;
  submissions: any[] = [];
  pendingStudents: any[] = [];
  practiceAnswersMap: Record<number, any[]> = {};
  submissionFiles: Record<number, any[]> = {};
  grades: Record<number, number | null> = {};
  feedbacks: Record<number, string> = {};

  expandedAnswers = new Set<number>();
  editingFeedback = new Set<number>();
  feedbackSaved: Record<number, boolean> = {};

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<any>(`${environment.apiUrl}/docente/tareas/${id}/entregas`).subscribe({
      next: (res) => {
        this.tarea = res.task;
        this.submissions = res.submissions ?? [];
        this.pendingStudents = res.pendingStudents ?? [];
        this.practiceAnswersMap = res.practiceAnswersMap ?? {};
        this.submissionFiles = res.submissionFiles ?? {};
        this.submissions.forEach(s => {
          this.feedbacks[s.id] = s.feedback ?? '';
          this.feedbackSaved[s.id] = !!(s.feedback);
        });
      },
      error: () => {}
    });
  }

  toggleAnswers(id: number): void {
    if (this.expandedAnswers.has(id)) {
      this.expandedAnswers.delete(id);
    } else {
      this.expandedAnswers.add(id);
    }
  }

  startEditFeedback(id: number): void {
    this.editingFeedback.add(id);
  }

  cancelEditFeedback(id: number): void {
    this.editingFeedback.delete(id);
    const sub = this.submissions.find(s => s.id === id);
    if (sub) this.feedbacks[id] = sub.feedback ?? '';
  }

  gradeSubmission(submissionId: number): void {
    const grade = this.grades[submissionId];
    if (grade == null) return;
    this.http.post(`${environment.apiUrl}/docente/entregas/${submissionId}/calificar`, { grade }).subscribe({
      next: () => window.location.reload(),
      error: () => alert('Error al calificar.')
    });
  }

  saveFeedback(submissionId: number): void {
    const feedback = this.feedbacks[submissionId] ?? '';
    this.http.post(`${environment.apiUrl}/docente/entregas/${submissionId}/feedback`, { feedback }).subscribe({
      next: () => {
        const sub = this.submissions.find(s => s.id === submissionId);
        if (sub) sub.feedback = feedback;
        this.feedbackSaved[submissionId] = true;
        this.editingFeedback.delete(submissionId);
      },
      error: () => alert('Error al guardar retroalimentación.')
    });
  }

  markWrittenAnswer(answer: any, sub: any, correct: boolean): void {
    this.http.put(`${environment.apiUrl}/docente/answers/${answer.id}/correct`, { correct }).subscribe({
      next: (res: any) => {
        answer.correct = correct;
        sub.grade = res.grade;
        this.feedbackSaved[sub.id] = !!(sub.feedback);
      },
      error: () => alert('Error al actualizar la respuesta.')
    });
  }

  downloadFile(storedName: string): string {
    return `${environment.apiUrl}/tasks/download/${storedName}`;
  }

  formatDate(raw: string): string {
    if (!raw) return '';
    return new Date(raw).toLocaleDateString('es-PE', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', hour12: false
    });
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
  }

  correctCount(subId: number): number {
    return (this.practiceAnswersMap[subId] ?? []).filter(a => a.correct === true).length;
  }

  totalQuestions(subId: number): number {
    return (this.practiceAnswersMap[subId] ?? []).length;
  }

  pendingWrittenCount(subId: number): number {
    return (this.practiceAnswersMap[subId] ?? []).filter(a => a.question?.writtenQuestion && a.correct == null).length;
  }
}
