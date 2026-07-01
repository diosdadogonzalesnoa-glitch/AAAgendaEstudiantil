import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-lecciones',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './lecciones.html',
  styleUrl: './lecciones.scss'
})
export class Lecciones implements OnInit {
  lecciones: any[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<any[]>(`${environment.apiUrl}/docente/lecciones`).subscribe({
      next: (res) => this.lecciones = res,
      error: () => this.lecciones = []
    });
  }
}
