import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-docente-estudio',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './estudio.html',
  styleUrl: './estudio.scss'
})
export class DocenteEstudio implements OnInit {
  courses: any[] = [];
  statsByCourse: any = {};
  selectedCourseId: string | null = null;
  loading = true;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<any>(`${environment.apiUrl}/docente/estudio`).subscribe({
      next: (res) => {
        this.courses = res.courses ?? [];
        this.statsByCourse = res.statsByCourse ?? {};
        if (this.courses.length > 0) {
          this.selectedCourseId = String(this.courses[0].id);
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  selectCourse(id: number): void {
    this.selectedCourseId = String(id);
  }

  get selectedStats(): any[] {
    if (!this.selectedCourseId) return [];
    return this.statsByCourse[this.selectedCourseId] ?? [];
  }
}
