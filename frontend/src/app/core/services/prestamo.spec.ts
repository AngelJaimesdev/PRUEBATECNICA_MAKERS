import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { PrestamoService } from './prestamo';

describe('PrestamoService', () => {
  const url = `${environment.apiUrl}/prestamos`;
  let service: PrestamoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PrestamoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('solicita un préstamo con monto y plazo', () => {
    service.solicitar({ monto: 5000, plazoMeses: 12 }).subscribe();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ monto: 5000, plazoMeses: 12 });
    req.flush({});
  });

  it('consulta los préstamos del usuario autenticado', () => {
    service.misPrestamos().subscribe();

    expect(http.expectOne(`${url}/mios`).request.method).toBe('GET');
  });

  it('aprueba y rechaza con PATCH', () => {
    service.aprobar(7).subscribe();
    service.rechazar(8).subscribe();

    expect(http.expectOne(`${url}/7/aprobar`).request.method).toBe('PATCH');
    expect(http.expectOne(`${url}/8/rechazar`).request.method).toBe('PATCH');
  });
});
