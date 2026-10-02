import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Sesion } from '../models/usuario';
import { AuthService } from './auth';

describe('AuthService', () => {
  let http: HttpTestingController;

  function crearServicio(sesionGuardada?: Sesion): AuthService {
    sessionStorage.clear();
    if (sesionGuardada) {
      sessionStorage.setItem('sesion', JSON.stringify(sesionGuardada));
    }
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', children: [] }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  afterEach(() => http.verify());

  it('inicia sesión como administrador y guarda el token', () => {
    const service = crearServicio();

    service.login('admin@test.com', '123').subscribe();
    const req = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@test.com', password: '123' });
    req.flush({
      token: 'abc',
      tipo: 'Bearer',
      expiraEnSegundos: 3600,
      nombre: 'Admin Admin',
      email: 'admin@test.com',
      rol: 'ADMIN',
    });

    expect(service.estaAutenticado()).toBe(true);
    expect(service.esAdmin()).toBe(true);
    expect(service.token()).toBe('abc');
    expect(service.rutaInicio()).toBe('/admin');
    expect(sessionStorage.getItem('sesion')).not.toBeNull();
  });

  it('cierra la sesión y limpia el almacenamiento', () => {
    const service = crearServicio({
      token: 'abc',
      nombre: 'Usuario',
      email: 'usuario@test.com',
      rol: 'USER',
      expiraEn: Date.now() + 60_000,
    });
    expect(service.estaAutenticado()).toBe(true);

    service.logout();

    expect(service.estaAutenticado()).toBe(false);
    expect(service.token()).toBeNull();
    expect(sessionStorage.getItem('sesion')).toBeNull();
  });

  it('descarta una sesión expirada', () => {
    const service = crearServicio({
      token: 'abc',
      nombre: 'Usuario',
      email: 'usuario@test.com',
      rol: 'USER',
      expiraEn: Date.now() - 1000,
    });

    expect(service.estaAutenticado()).toBe(false);
  });
});
