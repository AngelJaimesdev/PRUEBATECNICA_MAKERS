export type EstadoPrestamo = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';

export interface Prestamo {
  id: number;
  usuarioEmail: string;
  monto: number;
  plazoMeses: number;
  estado: EstadoPrestamo;
  fechaSolicitud: string;
  fechaRespuesta: string | null;
}

export interface SolicitudPrestamo {
  monto: number;
  plazoMeses: number;
}
