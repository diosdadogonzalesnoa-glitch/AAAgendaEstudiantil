import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-estudio',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './estudio.html',
  styleUrl: './estudio.scss'
})
export class Estudio implements OnInit, OnDestroy {
  courses: any[] = [];
  allPendingPractices: any[] = [];
  practicesForCourse: any[] = [];
  courseStats: any[] = [];
  weeklyStudyHours = 0;
  totalSessions = 0;

  selectedCourseId: number | null = null;
  selectedTaskId: number | null = null;

  running = false;
  elapsed = 0;
  startedAt: Date | null = null;
  private timer: any = null;

  /* ── Práctica en curso ── */
  questions: any[] = [];
  currentIndex = 0;
  answers: Record<number, any> = {};
  loadingQuestions = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<any>(`${environment.apiUrl}/alumno/estudio`).subscribe({
      next: (res) => {
        this.courses = res.courses ?? [];
        this.allPendingPractices = res.pendingPracticeTasks ?? [];
        this.weeklyStudyHours = res.weeklyStudyHours ?? 0;
        this.totalSessions = res.totalSessions ?? 0;
        this.courseStats = res.courseStats ?? [];
      },
      error: () => {}
    });
  }

  ngOnDestroy(): void { this.stopTimer(); }

  onCourseChange(): void {
    this.selectedTaskId = null;
    this.practicesForCourse = this.allPendingPractices.filter(
      p => p.course?.id === this.selectedCourseId
    );
  }

  get canStart(): boolean {
    return !!this.selectedCourseId && !!this.selectedTaskId;
  }

  get formattedTime(): string {
    const h = Math.floor(this.elapsed / 3600);
    const m = Math.floor((this.elapsed % 3600) / 60);
    const s = this.elapsed % 60;
    return [h, m, s].map(v => String(v).padStart(2, '0')).join(':');
  }

  get currentQuestion(): any {
    return this.questions[this.currentIndex] ?? null;
  }

  setAnswer(questionId: number, value: any): void {
    this.answers[questionId] = value;
  }

  next(): void {
    if (this.currentIndex < this.questions.length - 1) this.currentIndex++;
  }

  prev(): void {
    if (this.currentIndex > 0) this.currentIndex--;
  }

  toggleTimer(): void {
    if (this.running) {
      this.finishSession();
    } else {
      this.startSession();
    }
  }

  private startSession(): void {
    if (!this.selectedTaskId) return;
    this.loadingQuestions = true;
    this.http.get<any>(`${environment.apiUrl}/alumno/tareas/${this.selectedTaskId}/practica`).subscribe({
      next: (res) => {
        if (res.submitted) {
          alert('Esta práctica ya fue entregada.');
          this.loadingQuestions = false;
          return;
        }
        this.questions = res.questions ?? [];
        this.currentIndex = 0;
        this.answers = {};
        this.startedAt = new Date();
        this.elapsed = 0;
        this.running = true;
        this.timer = setInterval(() => this.elapsed++, 1000);
        this.loadingQuestions = false;
      },
      error: () => { this.loadingQuestions = false; }
    });
  }

  private finishSession(): void {
    this.stopTimer();
    const finishedAt = new Date();

    const formattedAnswers: Record<string, string> = {};
    Object.keys(this.answers).forEach(qId => {
      formattedAnswers[`answer_${qId}`] = String(this.answers[Number(qId)]);
    });

    const payload: any = {
      courseId: this.selectedCourseId,
      taskId: this.selectedTaskId,
      startedAt: this.startedAt!.toISOString().slice(0, 19),
      finishedAt: finishedAt.toISOString().slice(0, 19),
      answers: formattedAnswers
    };

    this.http.post(`${environment.apiUrl}/alumno/tareas/${this.selectedTaskId}/practica/submit`, payload).subscribe({
      next: () => {
        this.totalSessions++;
        const minutes = Math.round(this.elapsed / 60);
        this.weeklyStudyHours = Math.round((this.weeklyStudyHours + minutes / 60) * 10) / 10;
        const stat = this.courseStats.find(s => s.courseId === this.selectedCourseId);
        if (stat) {
          stat.sessions++;
          stat.totalHours = Math.round((stat.totalHours + minutes / 60) * 10) / 10;
          stat.weeklyHours = Math.round((stat.weeklyHours + minutes / 60) * 10) / 10;
        }
        this.elapsed = 0;
        this.questions = [];
        this.answers = {};
        this.currentIndex = 0;
        this.allPendingPractices = this.allPendingPractices.filter(p => p.id !== this.selectedTaskId);
        this.practicesForCourse = this.practicesForCourse.filter(p => p.id !== this.selectedTaskId);
        this.selectedTaskId = null;
      },
      error: () => { this.elapsed = 0; }
    });
  }

  private stopTimer(): void {
    if (this.timer) { clearInterval(this.timer); this.timer = null; }
    this.running = false;
  }
}
