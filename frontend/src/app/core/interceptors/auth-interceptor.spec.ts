import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth';
import { authInterceptor } from './auth-interceptor';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    sessionStorage.setItem(
      'sesion',
      JSON.stringify({
        token: 'token-prueba',
        nombre: 'Usuario',
        email: 'usuario@test.com',
        rol: 'USER',
        expiraEn: Date.now() + 60_000,
      }),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', children: [] }]),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('agrega el token Bearer a las peticiones del API', () => {
    httpClient.get(`${environment.apiUrl}/prestamos/mios`).subscribe();

    const req = http.expectOne(`${environment.apiUrl}/prestamos/mios`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-prueba');
    req.flush([]);
  });

  it('no envía el token a servidores externos', () => {
    httpClient.get('https://otro-dominio.com/datos').subscribe();

    const req = http.expectOne('https://otro-dominio.com/datos');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('cierra la sesión cuando el API responde 401', () => {
    httpClient.get(`${environment.apiUrl}/prestamos/mios`).subscribe({ error: () => undefined });

    http
      .expectOne(`${environment.apiUrl}/prestamos/mios`)
      .flush({ mensaje: 'Token expirado' }, { status: 401, statusText: 'Unauthorized' });

    expect(auth.estaAutenticado()).toBe(false);
  });
});
