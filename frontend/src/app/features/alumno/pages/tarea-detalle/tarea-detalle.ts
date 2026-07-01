import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-tarea-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './tarea-detalle.html',
  styleUrl: './tarea-detalle.scss'
})
export class TareaDetalle implements OnInit {
  tarea: any = null;

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<any>(`${environment.apiUrl}/alumno/tareas/${id}`).subscribe({
      next: (res) => this.tarea = res,
      error: () => {}
    });
  }
}
