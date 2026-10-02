import { Component, inject, input } from '@angular/core';
import { AuthService } from '../../core/services/auth';

@Component({
  imports: [],
  selector: 'app-encabezado',
  styleUrl: './encabezado.css',
  templateUrl: './encabezado.html',
})
export class Encabezado {
  readonly saludo = input.required<string>();
  protected readonly auth = inject(AuthService);
}
