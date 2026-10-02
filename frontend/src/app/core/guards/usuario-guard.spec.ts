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
import { usuarioGuard } from './usuario-guard';

describe('usuarioGuard', () => {
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
        usuarioGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot) as boolean | UrlTree,
    );
  }

  function ruta(resultado: boolean | UrlTree): string {
    return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
  }

  afterEach(() => sessionStorage.clear());

  it('permite el acceso al usuario', () => {
    expect(ejecutar('USER')).toBe(true);
  });

  it('redirige al administrador a su panel', () => {
    expect(ruta(ejecutar('ADMIN'))).toBe('/admin');
  });

  it('redirige al login si no hay sesión', () => {
    expect(ruta(ejecutar())).toBe('/login');
  });
});
