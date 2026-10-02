import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let http: HttpTestingController;

  function escribir(selector: string, valor: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  function enviar(): void {
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit'),
    );
  }

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('no llama al API si el formulario está vacío y muestra las validaciones', async () => {
    enviar();
    await fixture.whenStable();

    http.expectNone(`${environment.apiUrl}/auth/login`);
    expect(fixture.nativeElement.textContent).toContain('El email es obligatorio');
    expect(fixture.nativeElement.textContent).toContain('La contraseña es obligatoria');
  });

  it('valida el formato del email', async () => {
    escribir('#email', 'no-es-un-email');
    escribir('#password', '123');
    enviar();
    await fixture.whenStable();

    http.expectNone(`${environment.apiUrl}/auth/login`);
    expect(fixture.nativeElement.textContent).toContain('Ingrese un email válido');
  });

  it('muestra el mensaje del backend cuando las credenciales son inválidas', async () => {
    escribir('#email', 'usuario@test.com');
    escribir('#password', 'incorrecta');
    enviar();

    http
      .expectOne(`${environment.apiUrl}/auth/login`)
      .flush(
        { status: 401, mensaje: 'Credenciales inválidas', errores: null },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Credenciales inválidas');
  });
});
