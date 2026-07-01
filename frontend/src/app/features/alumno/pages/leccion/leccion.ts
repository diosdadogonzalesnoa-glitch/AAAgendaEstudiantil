import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-leccion',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './leccion.html',
  styleUrl: './leccion.scss'
})
export class Leccion implements OnInit {
  lesson: any = null;
  course: any = null;
  pdfUrl = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<any>(`${environment.apiUrl}/alumno/lecciones/${id}`).subscribe({
      next: (res) => {
        this.lesson = res.lesson;
        this.course = res.course;
        this.pdfUrl = `${environment.apiUrl}/courses/lessons/file/${res.lesson.storedFileName}`;
      },
      error: () => {}
    });
  }
}
