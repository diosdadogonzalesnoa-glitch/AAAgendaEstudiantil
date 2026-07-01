import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environment/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  data: any = null;
  today: string;

  constructor(private http: HttpClient) {
    this.today = new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  ngOnInit(): void {
    this.http.get(`${environment.apiUrl}/alumno/dashboard`).subscribe({
      next: (res) => this.data = res,
      error: () => this.data = {}
    });
  }

  isPast(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date();
  }
}
