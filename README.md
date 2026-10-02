# Sistema de Gestión de Préstamos Bancarios

API REST en Spring Boot y frontend en Angular para solicitar, aprobar y consultar préstamos bancarios.

- Los **usuarios** solicitan préstamos (monto y plazo) y consultan su estado.
- Los **administradores** ven todas las solicitudes y las aprueban o rechazan.

La parte teórica de la prueba está en [TEORIA.md](TEORIA.md).

## Stack

| Capa | Tecnologías |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA (Hibernate), Hibernate Validator, Spring Security + JWT (OAuth2 Resource Server), Ehcache 3 (JCache), H2, Lombok |
| Frontend | Angular 22 (standalone, signals, Reactive Forms, guards e interceptor funcionales, lazy loading) |
| Pruebas | JUnit 5, Mockito, AssertJ, MockMvc, spring-security-test / Vitest |

## Cómo ejecutar

Requisitos: Java 21, Maven 3.9+ y Node 22+.

### Backend (puerto 8090)
```bash
cd backend
mvn spring-boot:run
```
- API: `http://localhost:8090/api`
- Consola H2: `http://localhost:8090/h2-console` (JDBC URL `jdbc:h2:mem:prestamosdb`, usuario `sa`, sin contraseña)

### Frontend (puerto 4200)
```bash
cd frontend
npm install
npm start
```
Abrir `http://localhost:4200`.

### Usuarios de prueba

| Rol | Email | Contraseña |
|---|---|---|
| Usuario | usuario@test.com | 123 |
| Administrador | admin@test.com | 123 |

Al iniciar, el usuario tiene una solicitud pendiente de $1.000 a 12 meses.

### Pruebas
```bash
cd backend && mvn test
cd frontend && npm test -- --watch=false
```

## Endpoints

| Método | Ruta | Rol | Respuestas |
|---|---|---|---|
| POST | `/api/auth/login` | Público | 200, 400, 401 |
| POST | `/api/prestamos` | USER | 201, 400, 401, 403 |
| GET | `/api/prestamos/mios` | Autenticado | 200, 401 |
| GET | `/api/prestamos/{id}` | Dueño o ADMIN | 200, 403, 404 |
| GET | `/api/prestamos?estado=PENDIENTE` | ADMIN | 200, 403 |
| PATCH | `/api/prestamos/{id}/aprobar` | ADMIN | 200, 403, 404, 409 |
| PATCH | `/api/prestamos/{id}/rechazar` | ADMIN | 200, 403, 404, 409 |
| GET / POST / PUT / DELETE | `/api/usuarios` | ADMIN | 200, 201, 204, 400, 404, 409 |

Ejemplo de solicitud:
```json
{ "monto": 5000000, "plazoMeses": 36 }
```

Todos los errores usan el mismo formato:
```json
{
  "timestamp": "2026-10-02T15:40:12",
  "status": 400,
  "mensaje": "Datos inválidos",
  "errores": { "monto": "El monto mínimo es 1.000" }
}
```

## Arquitectura

### Backend: arquitectura hexagonal

```
com.prueba.prestamos
├── domain            Modelo y reglas de negocio en Java puro (sin Spring ni JPA)
│   ├── model         Prestamo, Usuario, EstadoPrestamo, Rol
│   ├── exception     ReglaNegocioException, RecursoNoEncontradoException, AccesoDenegadoException
│   └── port          Puertos de salida: PrestamoRepositoryPort, UsuarioRepositoryPort, CodificadorPasswordPort
├── application       Casos de uso: PrestamoService, UsuarioService (transacciones y caché)
└── infrastructure    Adaptadores
    ├── persistence   Entidades JPA, repositorios Spring Data y adaptadores de los puertos
    ├── web           Controladores REST, DTOs con validaciones y manejo global de errores
    ├── security      Spring Security, emisión y validación de JWT, BCrypt
    └── config        Caché y datos iniciales
```

Las dependencias apuntan hacia el dominio: el dominio no conoce a JPA ni a HTTP. Cambiar H2 por PostgreSQL, o JPA por otra tecnología, solo afecta a los adaptadores.

### Frontend

```
src/app
├── core
│   ├── models        Interfaces de Prestamo, Sesion y ErrorApi
│   ├── services      AuthService (estado de sesión con signals) y PrestamoService (HTTP)
│   ├── guards        usuarioGuard y adminGuard
│   └── interceptors  authInterceptor (agrega el token y cierra sesión ante un 401)
├── pages             login, usuario-prestamos, admin-prestamos (carga diferida)
└── shared            encabezado
```

## Decisiones técnicas

**Reglas de negocio en el dominio.** Un préstamo nace `PENDIENTE` y solo desde ese estado puede aprobarse o rechazarse. El monto va de 1.000 a 500.000.000 y el plazo de 1 a 120 meses. Las mismas reglas se validan en los DTOs (Hibernate Validator), en el dominio y en el formulario de Angular.

**Transacciones y concurrencia.** Aprobar y rechazar son `@Transactional`: leer, validar el estado y guardar ocurren en una sola transacción. La entidad tiene `@Version` (bloqueo optimista). Si dos administradores deciden el mismo préstamo al mismo tiempo, solo uno gana y el otro recibe `409 Conflict`.

**Caché con Ehcache.** La consulta de "mis préstamos", la más repetitiva, se cachea por usuario (máximo 1.000 entradas, 10 minutos de vida). La caché se invalida al solicitar, aprobar o rechazar. El `CacheManager` es *transaction-aware*: la invalidación ocurre después del commit, así no se puede volver a cachear un estado viejo. La vista del administrador no se cachea porque necesita datos frescos.

**Seguridad.**
- El login valida credenciales con BCrypt y emite un JWT firmado (HS256) con el rol como claim.
- La API es stateless; CSRF está deshabilitado porque el token viaja en el header `Authorization` y no en una cookie.
- Las reglas por rol están centralizadas en `SecurityConfig`. Un usuario que intenta aprobar recibe `403`.
- El email del usuario siempre se toma del token, nunca de un parámetro, para que nadie pueda consultar ni crear préstamos a nombre de otro.
- La clave del JWT se puede inyectar con la variable de entorno `JWT_SECRET`.

**Los préstamos no se eliminan.** En un sistema financiero las solicitudes son registros auditables. Una vez enviada, el usuario no puede cancelarla; el "borrado" lógico es el rechazo por parte del administrador. El CRUD completo, con eliminación, está en usuarios, y no se puede eliminar un usuario con préstamos.

**No hay registro público.** En un banco los clientes no se auto-registran; los usuarios los crea un administrador mediante `/api/usuarios`.

**Spring WebFlux no se usó.** El enunciado lo deja a criterio "en los casos que lo ameriten". Spring Data JPA y el driver JDBC son bloqueantes, así que WebFlux no aportaría rendimiento real y complicaría el código. Lo usaría para integraciones con servicios externos o streaming de eventos.

**Frontend.**
- `AuthService` es la única fuente de verdad de la sesión, con signals.
- Los guards protegen las rutas por rol. Son experiencia de usuario: la autorización real la hace el backend.
- La sesión se guarda en `sessionStorage`: sobrevive a recargar la página, se borra al cerrar la pestaña y se valida su expiración. En producción preferiría access token en memoria y refresh token en cookie `HttpOnly`.
- El formulario pide monto **y plazo**, porque el enunciado lo exige aunque el mockup solo muestre el monto.

## Mejoras con más tiempo

- PostgreSQL con migraciones (Flyway) y Testcontainers para las pruebas de integración.
- Refresh tokens y cierre de sesión del lado del servidor.
- Paginación en el listado del administrador y documentación con OpenAPI/Swagger.
- Docker Compose para levantar todo con un solo comando.
