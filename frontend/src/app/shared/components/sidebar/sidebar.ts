import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/autenticacion/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {
  isCollapsed = false;
  mobileOpen = false;

  constructor(public auth: AuthService) {}

  get isDocente(): boolean { return this.auth.isDocente(); }
  get isAlumno(): boolean { return this.auth.isAlumno(); }
  get userName(): string { return this.auth.getCurrentUser()?.name ?? ''; }
  get userRole(): string { return this.auth.getCurrentUser()?.role === 'DOCENTE' ? 'DOCENTE' : 'ALUMNO'; }

  getInitials(): string {
    const parts = this.userName.trim().split(' ').filter(w => w.length > 0);
    if (parts.length === 0) return '?';
    if (parts.length === 1) return parts[0][0].toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
    document.body.classList.toggle('sidebar-collapsed', this.isCollapsed);
  }

  toggleMobile(): void { this.mobileOpen = !this.mobileOpen; }
  closeMobile(): void { this.mobileOpen = false; }
  logout(): void { this.auth.logout(); }
}
