import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/autenticacion/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class Navbar {
  @Output() sidebarToggle = new EventEmitter<void>();

  constructor(public auth: AuthService) {}

  get userName(): string {
    return this.auth.getCurrentUser()?.name?.split(' ')[0] ?? '';
  }

  get userRole(): string {
    return this.auth.getCurrentUser()?.role === 'DOCENTE' ? 'Docente' : 'Estudiante';
  }

  toggleSidebar(): void {
    this.sidebarToggle.emit();
  }

  logout(): void {
    this.auth.logout();
  }
}
