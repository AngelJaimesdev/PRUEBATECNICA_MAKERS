import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Prestamo } from '../../core/models/prestamo';
import { AdminPrestamos } from './admin-prestamos';

describe('AdminPrestamos', () => {
  const url = `${environment.apiUrl}/prestamos`;
  let fixture: ComponentFixture<AdminPrestamos>;
  let http: HttpTestingController;

  const pendiente: Prestamo = {
    id: 1,
    usuarioEmail: 'usuario@test.com',
    monto: 1000,
    plazoMeses: 12,
    estado: 'PENDIENTE',
    fechaSolicitud: '2026-10-02T10:00:00',
    fechaRespuesta: null,
  };

  function boton(texto: string): HTMLButtonElement | undefined {
    return Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>,
    ).find((b) => b.textContent?.trim() === texto);
  }

  beforeEach(async () => {
    sessionStorage.setItem(
      'sesion',
      JSON.stringify({
        token: 't',
        nombre: 'Admin Admin',
        email: 'admin@test.com',
        rol: 'ADMIN',
        expiraEn: Date.now() + 60_000,
      }),
    );
    await TestBed.configureTestingModule({
      imports: [AdminPrestamos],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'en-US' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPrestamos);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(url).flush([pendiente, { ...pendiente, id: 2, estado: 'APROBADO' }]);
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });

  it('muestra los botones de decisión solo en préstamos pendientes', () => {
    expect(fixture.nativeElement.textContent).toContain('Hola, Admin');
    expect(fixture.nativeElement.querySelectorAll('li.item').length).toBe(2);
    expect(fixture.nativeElement.querySelectorAll('.item-acciones').length).toBe(1);
  });

  it('aprueba un préstamo y actualiza su estado', async () => {
    boton('Aprobar')!.click();

    const req = http.expectOne(`${url}/1/aprobar`);
    expect(req.request.method).toBe('PATCH');
    req.flush({ ...pendiente, estado: 'APROBADO', fechaRespuesta: '2026-10-02T11:00:00' });
    await fixture.whenStable();

    expect(boton('Aprobar')).toBeUndefined();
  });

  it('filtra por estado', async () => {
    boton('Pendientes 1')!.click();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelectorAll('li.item').length).toBe(1);
  });

  it('muestra el error y recarga cuando el préstamo ya fue decidido', async () => {
    boton('Rechazar')!.click();

    http
      .expectOne(`${url}/1/rechazar`)
      .flush(
        { status: 409, mensaje: 'El préstamo 1 ya fue aprobado', errores: null },
        { status: 409, statusText: 'Conflict' },
      );
    http.expectOne(url).flush([{ ...pendiente, estado: 'APROBADO' }]);
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('El préstamo 1 ya fue aprobado');
  });
});
