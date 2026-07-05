import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { environment } from '../../../../environment/environment';
import { AuthService } from '../../../../core/services/autenticacion/auth.service';

@Component({
  selector: 'app-leccion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './leccion.html',
  styleUrl: './leccion.scss'
})
export class Leccion implements OnInit {
  lesson: any = null;
  pdfUrl: SafeResourceUrl = '';
  blobUrl = '';
  pdfLoading = false;
  pdfError = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private sanitizer: DomSanitizer,
    private auth: AuthService
  ) {}

  get isDocente(): boolean { return this.auth.isDocente(); }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const role = this.isDocente ? 'docente' : 'alumno';
    this.http.get<any>(`${environment.apiUrl}/${role}/lecciones/${id}`).subscribe({
      next: (res) => {
        this.lesson = res.lesson;
        this.loadPdf(res.lesson.storedFileName);
      },
      error: () => {}
    });
  }

  private loadPdf(storedFileName: string): void {
    this.pdfLoading = true;
    this.pdfError = false;
    this.http.get(`${environment.apiUrl}/lessons/file/${storedFileName}`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.blobUrl = URL.createObjectURL(blob);
        this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.blobUrl);
        this.pdfLoading = false;
      },
      error: () => {
        this.pdfLoading = false;
        this.pdfError = true;
      }
    });
  }

  openPdf(): void {
    if (this.blobUrl) window.open(this.blobUrl, '_blank');
  }

  goBack(): void {
    const courseId = this.lesson?.course?.id;
    if (this.isDocente && courseId) {
      this.router.navigate(['/docente/cursos', courseId]);
    } else if (courseId) {
      this.router.navigate(['/alumno/cursos', courseId]);
    } else {
      window.history.back();
    }
  }

  formatDate(d: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' });
  }
}
