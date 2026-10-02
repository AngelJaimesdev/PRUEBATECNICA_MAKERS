import { HttpClient } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, Sesion } from '../models/usuario';

const CLAVE_SESION = 'sesion';

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly sesionActual = signal<Sesion | null>(this.restaurarSesion());

  readonly sesion = this.sesionActual.asReadonly();
  readonly esAdmin = computed(() => this.sesionActual()?.rol === 'ADMIN');

  login(email: string, password: string): Observable<Sesion> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(
        map((respuesta) => ({
          token: respuesta.token,
          nombre: respuesta.nombre,
          email: respuesta.email,
          rol: respuesta.rol,
          expiraEn: Date.now() + respuesta.expiraEnSegundos * 1000,
        })),
        tap((sesion) => this.guardarSesion(sesion)),
      );
  }

  logout(): void {
    this.limpiarSesion();
    this.router.navigate(['/login']);
  }

  estaAutenticado(): boolean {
    const sesion = this.sesionActual();
    if (sesion && Date.now() < sesion.expiraEn) {
      return true;
    }
    if (sesion) {
      this.limpiarSesion();
    }
    return false;
  }

  token(): string | null {
    return this.sesionActual()?.token ?? null;
  }

  rutaInicio(): string {
    return this.esAdmin() ? '/admin' : '/prestamos';
  }

  private guardarSesion(sesion: Sesion): void {
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    this.sesionActual.set(sesion);
  }

  private limpiarSesion(): void {
    sessionStorage.removeItem(CLAVE_SESION);
    this.sesionActual.set(null);
  }

  private restaurarSesion(): Sesion | null {
    try {
      const guardada = sessionStorage.getItem(CLAVE_SESION);
      const sesion = guardada ? (JSON.parse(guardada) as Sesion) : null;
      return sesion && Date.now() < sesion.expiraEn ? sesion : null;
    } catch {
      return null;
    }
  }
}
