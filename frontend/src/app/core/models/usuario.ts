export type Rol = 'USER' | 'ADMIN';

export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEnSegundos: number;
  nombre: string;
  email: string;
  rol: Rol;
}

export interface Sesion {
  token: string;
  nombre: string;
  email: string;
  rol: Rol;
  expiraEn: number;
}
