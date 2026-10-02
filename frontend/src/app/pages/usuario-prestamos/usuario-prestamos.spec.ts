import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Prestamo } from '../../core/models/prestamo';
import { UsuarioPrestamos } from './usuario-prestamos';

describe('UsuarioPrestamos', () => {
  const url = `${environment.apiUrl}/prestamos`;
  let fixture: ComponentFixture<UsuarioPrestamos>;
  let http: HttpTestingController;

  const prestamo = (id: number, estado: Prestamo['estado']): Prestamo => ({
    id,
    usuarioEmail: 'usuario@test.com',
    monto: 1000 * id,
    plazoMeses: 12,
    estado,
    fechaSolicitud: '2026-10-02T10:00:00',
    fechaRespuesta: null,
  });

  beforeEach(async () => {
    sessionStorage.setItem(
      'sesion',
      JSON.stringify({
        token: 't',
        nombre: 'Usuario',
        email: 'usuario@test.com',
        rol: 'USER',
        expiraEn: Date.now() + 60_000,
      }),
    );
    await TestBed.configureTestingModule({
      imports: [UsuarioPrestamos],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'en-US' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsuarioPrestamos);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('muestra el saludo y los préstamos del usuario', async () => {
    http.expectOne(`${url}/mios`).flush([prestamo(1, 'PENDIENTE'), prestamo(2, 'APROBADO')]);
    await fixture.whenStable();

    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Bienvenido, Usuario');
    expect(fixture.nativeElement.querySelectorAll('li.item').length).toBe(2);
    expect(texto).toContain('1 pendientes');
  });

  it('agrega el préstamo solicitado al inicio de la lista', async () => {
    http.expectOne(`${url}/mios`).flush([prestamo(1, 'APROBADO')]);
    await fixture.whenStable();

    const monto = fixture.nativeElement.querySelector('#monto') as HTMLInputElement;
    monto.value = '5000';
    monto.dispatchEvent(new Event('input'));
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );

    const req = http.expectOne(url);
    expect(req.request.body).toEqual({ monto: 5000, plazoMeses: 12 });
    req.flush(prestamo(9, 'PENDIENTE'));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('li.item').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Solicitud enviada');
  });
});
