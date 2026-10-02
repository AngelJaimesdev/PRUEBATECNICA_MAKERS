import { HttpErrorResponse } from '@angular/common/http';

export interface ErrorApi {
  timestamp: string;
  status: number;
  mensaje: string;
  errores: Record<string, string> | null;
}

export function mensajeDeError(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.';
  }
  const cuerpo = error.error as ErrorApi | null;
  if (cuerpo?.errores) {
    return Object.values(cuerpo.errores).join('. ');
  }
  return cuerpo?.mensaje ?? 'Ocurrió un error inesperado';
}
