import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environment/environment';

@Component({
  selector: 'app-recordatorios',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recordatorios.html',
  styleUrl: './recordatorios.scss'
})
export class Recordatorios implements OnInit {
  todayTasks: any[] = [];
  tomorrowTasks: any[] = [];
  futureTasks: any[] = [];
  pendingCount = 0;
  completedCount = 0;

  constructor(private http: HttpClient) {}

  formatDate(dateStr: string | null): string {
    if (!dateStr) return 'Sin fecha';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-PE', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', hour12: false
    });
  }

  ngOnInit(): void {
    this.http.get<any>(`${environment.apiUrl}/recordatorios`).subscribe({
      next: (res) => {
        this.todayTasks    = res.todayTasks    ?? [];
        this.tomorrowTasks = res.tomorrowTasks ?? [];
        this.futureTasks   = res.futureTasks   ?? [];
        this.pendingCount  = res.pendingCount  ?? 0;
        this.completedCount = res.completedCount ?? 0;
      },
      error: () => {}
    });
  }
}
