import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-practica',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './practica.html',
  styleUrl: './practica.scss'
})
export class Practica implements OnInit {
  tarea: any = null;
  questions: any[] = [];
  submission: any = null;
  answers: Record<number, any> = {};

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<any>(`${environment.apiUrl}/alumno/tareas/${id}/practica`).subscribe({
      next: (res) => {
        this.tarea = res.tarea;
        this.questions = res.questions ?? [];
        this.submission = res.submission ?? null;
      },
      error: () => {}
    });
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', hour12: false
    });
  }

  setAnswer(questionId: number, value: any): void {
    this.answers[questionId] = value;
  }

  submit(): void {
    if (!this.tarea) return;
    const formattedAnswers: Record<string, string> = {};
    Object.keys(this.answers).forEach(qId => {
      formattedAnswers[`answer_${qId}`] = String(this.answers[Number(qId)]);
    });
    this.http.post(`${environment.apiUrl}/alumno/tareas/${this.tarea.id}/practica/submit`, { answers: formattedAnswers }).subscribe({
      next: () => window.location.reload(),
      error: () => alert('Error al enviar la práctica.')
    });
  }
}
