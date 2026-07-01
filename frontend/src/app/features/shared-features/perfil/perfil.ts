import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/autenticacion/auth.service';
import { User } from '../../../core/models/user.model';
import { environment } from '../../../environment/environment';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss'
})
export class Perfil implements OnInit {
  user: User | null = null;
  cursosCount = 0;
  showEdit = false;
  newName = '';

  constructor(private auth: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.user = this.auth.getCurrentUser();
    this.newName = this.user?.name ?? '';
    const endpoint = this.user?.role === 'DOCENTE'
      ? `${environment.apiUrl}/docente/cursos`
      : `${environment.apiUrl}/alumno/cursos`;
    this.http.get<any[]>(endpoint).subscribe({
      next: (res) => {
        this.cursosCount = this.user?.role === 'DOCENTE'
          ? res.filter(c => c.status !== 'TERMINADO').length
          : res.length;
      },
      error: () => this.cursosCount = 0
    });
  }

  toggleEdit(): void { this.showEdit = !this.showEdit; }

  guardar(): void {
    if (!this.newName.trim()) return;
    this.http.post(`${environment.apiUrl}/perfil/actualizar`, { name: this.newName }).subscribe({
      next: () => {
        if (this.user) this.user = { ...this.user, name: this.newName };
        this.showEdit = false;
      },
      error: () => alert('No se pudo actualizar el nombre.')
    });
  }
}
