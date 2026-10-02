import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin-guard';
import { usuarioGuard } from './core/guards/usuario-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'Iniciar sesión',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'prestamos',
    title: 'Mis préstamos',
    canActivate: [usuarioGuard],
    loadComponent: () =>
      import('./pages/usuario-prestamos/usuario-prestamos').then((m) => m.UsuarioPrestamos),
  },
  {
    path: 'admin',
    title: 'Gestionar préstamos',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./pages/admin-prestamos/admin-prestamos').then((m) => m.AdminPrestamos),
  },
  { path: '**', redirectTo: 'login' },
];
