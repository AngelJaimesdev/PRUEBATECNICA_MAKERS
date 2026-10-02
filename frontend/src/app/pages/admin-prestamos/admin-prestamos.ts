import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { mensajeDeError } from '../../core/models/error-api';
import { EstadoPrestamo, Prestamo } from '../../core/models/prestamo';
import { PrestamoService } from '../../core/services/prestamo';
import { Encabezado } from '../../shared/encabezado/encabezado';

type Filtro = EstadoPrestamo | 'TODOS';

@Component({
  imports: [CurrencyPipe, DatePipe, Encabezado],
  selector: 'app-admin-prestamos',
  styleUrl: './admin-prestamos.css',
  templateUrl: './admin-prestamos.html',
})
export class AdminPrestamos implements OnInit {
  private readonly prestamoService = inject(PrestamoService);

  protected readonly filtros: { valor: Filtro; etiqueta: string }[] = [
    { valor: 'TODOS', etiqueta: 'Todos' },
    { valor: 'PENDIENTE', etiqueta: 'Pendientes' },
    { valor: 'APROBADO', etiqueta: 'Aprobados' },
    { valor: 'RECHAZADO', etiqueta: 'Rechazados' },
  ];

  protected readonly prestamos = signal<Prestamo[]>([]);
  protected readonly filtro = signal<Filtro>('TODOS');
  protected readonly cargando = signal(true);
  protected readonly procesando = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    return filtro === 'TODOS'
      ? this.prestamos()
      : this.prestamos().filter((p) => p.estado === filtro);
  });

  protected readonly conteo = computed(() => {
    const lista = this.prestamos();
    return {
      TODOS: lista.length,
      PENDIENTE: lista.filter((p) => p.estado === 'PENDIENTE').length,
      APROBADO: lista.filter((p) => p.estado === 'APROBADO').length,
      RECHAZADO: lista.filter((p) => p.estado === 'RECHAZADO').length,
    };
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected aprobar(prestamo: Prestamo): void {
    this.decidir(prestamo, this.prestamoService.aprobar(prestamo.id));
  }

  protected rechazar(prestamo: Prestamo): void {
    this.decidir(prestamo, this.prestamoService.rechazar(prestamo.id));
  }

  private decidir(prestamo: Prestamo, peticion: Observable<Prestamo>): void {
    this.procesando.set(prestamo.id);
    this.error.set(null);
    peticion.subscribe({
      next: (actualizado) => {
        this.prestamos.update((lista) =>
          lista.map((p) => (p.id === actualizado.id ? actualizado : p)),
        );
        this.procesando.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeDeError(err));
        this.procesando.set(null);
        if (err.status === 409) {
          this.cargar();
        }
      },
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.prestamoService.listarTodos().subscribe({
      next: (lista) => {
        this.prestamos.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeDeError(err));
        this.cargando.set(false);
      },
    });
  }
}
