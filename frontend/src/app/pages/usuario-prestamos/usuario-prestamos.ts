import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { mensajeDeError } from '../../core/models/error-api';
import { Prestamo } from '../../core/models/prestamo';
import { PrestamoService } from '../../core/services/prestamo';
import { Encabezado } from '../../shared/encabezado/encabezado';

@Component({
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, Encabezado],
  selector: 'app-usuario-prestamos',
  styleUrl: './usuario-prestamos.css',
  templateUrl: './usuario-prestamos.html',
})
export class UsuarioPrestamos implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly prestamoService = inject(PrestamoService);

  protected readonly plazos = [6, 12, 18, 24, 36, 48, 60, 120];
  protected readonly prestamos = signal<Prestamo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly exito = signal<string | null>(null);
  protected readonly pendientes = computed(
    () => this.prestamos().filter((p) => p.estado === 'PENDIENTE').length,
  );

  protected readonly form = this.fb.group({
    monto: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(1000),
      Validators.max(500000000),
    ]),
    plazoMeses: this.fb.nonNullable.control(12, [
      Validators.required,
      Validators.min(1),
      Validators.max(120),
    ]),
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected invalido(campo: 'monto' | 'plazoMeses'): boolean {
    const control = this.form.controls[campo];
    return control.invalid && control.touched;
  }

  protected solicitar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.exito.set(null);

    const { monto, plazoMeses } = this.form.getRawValue();
    this.prestamoService.solicitar({ monto: monto!, plazoMeses }).subscribe({
      next: (nuevo) => {
        this.prestamos.update((lista) => [nuevo, ...lista]);
        this.form.reset({ monto: null, plazoMeses: 12 });
        this.exito.set('Solicitud enviada. Quedó pendiente de aprobación.');
        this.enviando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(mensajeDeError(err));
        this.enviando.set(false);
      },
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.prestamoService.misPrestamos().subscribe({
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
