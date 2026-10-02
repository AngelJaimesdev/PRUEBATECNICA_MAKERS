import { HttpClient } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Prestamo, SolicitudPrestamo } from '../models/prestamo';

@Service()
export class PrestamoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/prestamos`;

  solicitar(solicitud: SolicitudPrestamo): Observable<Prestamo> {
    return this.http.post<Prestamo>(this.url, solicitud);
  }

  misPrestamos(): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(`${this.url}/mios`);
  }

  listarTodos(): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(this.url);
  }

  aprobar(id: number): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.url}/${id}/aprobar`, null);
  }

  rechazar(id: number): Observable<Prestamo> {
    return this.http.patch<Prestamo>(`${this.url}/${id}/rechazar`, null);
  }
}
