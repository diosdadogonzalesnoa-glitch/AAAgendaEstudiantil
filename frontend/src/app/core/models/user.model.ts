export interface User {
  id: number;
  name: string;
  email: string;
  role: 'DOCENTE' | 'ESTUDIANTE';
}

export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: 'DOCENTE' | 'ESTUDIANTE';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: 'DOCENTE' | 'ESTUDIANTE';
}
