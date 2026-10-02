import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Rol } from '../models/usuario';
import { adminGuard } from './admin-guard';

describe('adminGuard', () => {
  function ejecutar(rol?: Rol): boolean | UrlTree {
    sessionStorage.clear();
    if (rol) {
      sessionStorage.setItem(
        'sesion',
        JSON.stringify({
          token: 't',
          nombre: 'n',
          email: 'e@test.com',
          rol,
          expiraEn: Date.now() + 60_000,
        }),
      );
    }
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
    return TestBed.runInInjectionContext(
      () =>
        adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot) as boolean | UrlTree,
    );
  }

  function ruta(resultado: boolean | UrlTree): string {
    return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
  }

  afterEach(() => sessionStorage.clear());

  it('permite el acceso al administrador', () => {
    expect(ejecutar('ADMIN')).toBe(true);
  });

  it('redirige a un usuario normal a sus préstamos', () => {
    expect(ruta(ejecutar('USER'))).toBe('/prestamos');
  });

  it('redirige al login si no hay sesión', () => {
    expect(ruta(ejecutar())).toBe('/login');
  });
});
