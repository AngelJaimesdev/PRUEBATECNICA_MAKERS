import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth';
import { Encabezado } from './encabezado';

describe('Encabezado', () => {
  let fixture: ComponentFixture<Encabezado>;

  beforeEach(async () => {
    sessionStorage.setItem(
      'sesion',
      JSON.stringify({
        token: 't',
        nombre: 'Usuario',
        email: 'usuario@test.com',
        rol: 'USER',
        expiraEn: Date.now() + 60_000,
      }),
    );
    await TestBed.configureTestingModule({
      imports: [Encabezado],
      providers: [provideHttpClient(), provideRouter([{ path: 'login', children: [] }])],
    }).compileComponents();

    fixture = TestBed.createComponent(Encabezado);
    fixture.componentRef.setInput('saludo', 'Bienvenido');
    await fixture.whenStable();
  });

  afterEach(() => sessionStorage.clear());

  it('saluda al usuario de la sesión', () => {
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Bienvenido, Usuario');
  });

  it('cierra la sesión al presionar el botón', () => {
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();

    expect(TestBed.inject(AuthService).estaAutenticado()).toBe(false);
  });
});
