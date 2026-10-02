# Parte Teórica — Prueba Técnica Spring Boot + Angular

Las respuestas se apoyan en un mismo hilo: en un sistema financiero primero va la **consistencia y la seguridad del dinero** y después el rendimiento. Cuando hay que elegir, sacrifico velocidad antes que exactitud.

---

## 1. Optimización en sistemas financieros

**Pregunta:** ¿Qué estrategias aplicarías para mejorar el rendimiento y la escalabilidad de una aplicación bancaria con transacciones en tiempo real (concurrencia, caché, patrones)?

### Concurrencia
- **Bloqueo sobre el saldo.** Cuando se debita una cuenta uso **bloqueo pesimista** (`SELECT ... FOR UPDATE`, en JPA `@Lock(PESSIMISTIC_WRITE)`). Así dos débitos simultáneos no leen el mismo saldo y dejan la cuenta en negativo. En registros con poca contención (por ejemplo, actualizar un préstamo) uso **bloqueo optimista** con `@Version`: no bloquea filas y, si hay conflicto, falla con un error que se puede reintentar.
- **Evitar deadlocks.** En una transferencia entre dos cuentas siempre bloqueo primero la de menor id. Así dos transferencias cruzadas (A→B y B→A) no se quedan esperando la una a la otra.
- **Idempotencia.** Cada operación lleva una *idempotency key* (un UUID que envía el cliente). Si la red falla y el cliente reintenta, la operación no se aplica dos veces.
- **Procesamiento asíncrono.** Lo que no necesita respuesta inmediata (notificaciones, reportes, auditoría) va a una cola (Kafka/RabbitMQ). Si se particiona por número de cuenta, las operaciones de una misma cuenta se procesan en orden y las de cuentas distintas en paralelo.
- **Hilos.** Con Java 21 uso **virtual threads** para cargas bloqueantes de I/O, o **WebFlux** en los endpoints que solo hacen consultas o agregan datos de varios servicios.

### Caché
- **Qué sí cacheo:** datos que se leen mucho y cambian poco, como tasas de interés, parámetros, catálogos y el estado de una solicitud. Uso caché local (Caffeine/Ehcache) o distribuida (Redis) cuando hay varias instancias.
- **Qué NO cacheo:** el saldo que se usa para *decidir* un débito. Ese siempre se lee de la base de datos dentro de la transacción. Un saldo viejo en caché puede autorizar un sobregiro.
- **Invalidación:** patrón *cache-aside* con TTL corto, más `@CacheEvict` o `@CachePut` cuando el dato cambia. Por ejemplo, al aprobar un préstamo se invalida su estado en caché.

### Base de datos
- Índices sobre las columnas de búsqueda (cuenta, fecha, estado) y paginación en todas las consultas de listas.
- Pool de conexiones (HikariCP) bien dimensionado y escrituras por lotes (*batch*) en procesos masivos.
- **Réplicas de lectura** para consultas y reportes, para que la base principal quede libre para escrituras.
- Tabla de movimientos **particionada por fecha**, porque crece sin límite.

### Patrones de diseño
- **CQRS:** separa el modelo de escritura (transacciones) del de lectura (consultas de saldo e historial), y cada lado escala por su cuenta.
- **Ledger *append-only* (event sourcing ligero):** los movimientos nunca se actualizan ni se borran, solo se agregan. Eso da trazabilidad y facilita la auditoría.
- **Circuit Breaker, Retry y Bulkhead (Resilience4j):** si un servicio externo (por ejemplo, una central de riesgo) se cae, no arrastra al resto del sistema.
- **Strategy:** para tipos de transacción con reglas distintas (transferencia, pago, retiro) sin llenar el código de `if`.

### Escalabilidad
- Servicios **stateless**: la sesión va en el JWT, no en el servidor. Así se escala horizontalmente detrás de un balanceador (Kubernetes con HPA).
- **Medir antes de optimizar**: métricas con Micrometer y Prometheus, y trazas distribuidas, para atacar el cuello de botella real y no uno supuesto.

---

## 2. Seguridad en APIs financieras

**Pregunta:** ¿Cómo protegerías una API con información sensible de cuentas contra SQL Injection, CSRF, XSS y otros ataques comunes?

| Ataque | Cómo lo mitigo |
|---|---|
| **Inyección SQL** | Nunca concateno strings en consultas. Uso Spring Data JPA o consultas parametrizadas (`:param`), que separan el código SQL de los datos. Además valido la entrada con Bean Validation (`@NotNull`, `@Positive`, `@Pattern`) y el usuario de la base de datos tiene **mínimo privilegio** (sin DROP ni DDL). |
| **XSS** | Angular escapa por defecto todo lo que se interpola (`{{ }}`). Evito `innerHTML` y `bypassSecurityTrust*`. En el backend envío las cabeceras `Content-Security-Policy` y `X-Content-Type-Options: nosniff`, y respondo siempre JSON con su `Content-Type` correcto. |
| **CSRF** | Depende de dónde viaje la credencial. Si es un **JWT en la cabecera `Authorization`**, el navegador no lo envía solo, así que CSRF no aplica y se puede deshabilitar en Spring Security (es lo que hago en la parte práctica). Si la sesión va en **cookie**, activo el token CSRF (`CookieCsrfTokenRepository`, que Angular soporta con `XSRF-TOKEN`) y uso cookies `SameSite=Strict`. |
| **Acceso a datos ajenos (IDOR/BOLA)** | Es el ataque **número 1 del OWASP API Top 10**. No basta con estar autenticado: verifico que el recurso pertenezca al usuario. El usuario se toma del token, nunca de un parámetro que envía el cliente. Por ejemplo, `GET /prestamos/mios` en lugar de `GET /prestamos?usuarioId=5`. |
| **Fuerza bruta / abuso** | *Rate limiting* por IP y por usuario, bloqueo temporal tras varios intentos fallidos y MFA para operaciones sensibles. |
| **Robo de credenciales o tokens** | Contraseñas con **BCrypt**. JWT de vida corta (15 min) y *refresh token* rotativo. Solo HTTPS (TLS 1.2+) con HSTS. |

**Otras capas:**
- **Autorización por roles** en Spring Security (`@PreAuthorize("hasRole('ADMIN')")`) para que solo el administrador apruebe préstamos.
- **Datos sensibles:** cifrado en reposo, números de cuenta enmascarados (`****1234`), nada sensible en los logs y secretos en un *vault*, no en el código.
- **Errores:** respuestas genéricas sin *stack traces* ni detalles internos, que dan pistas al atacante.
- **CORS** restringido a los orígenes conocidos.
- **Dependencias** escaneadas (OWASP Dependency-Check, Snyk) en el pipeline.
- **Perímetro:** API Gateway con WAF y mTLS entre microservicios.
- **Auditoría:** quién hizo qué y cuándo, en un log inmutable.

---

## 3. Transacciones en sistemas distribuidos

**Pregunta:** ¿Cómo implementarías la consistencia y el manejo de errores en una API de transferencias entre cuentas que están en servicios diferentes?

Cuando las cuentas están en servicios distintos, ya no existe un único `@Transactional` que cubra todo.

**¿Por qué no 2PC (*two-phase commit*)?** Bloquea recursos mientras espera a todos los participantes, escala mal y, si el coordinador se cae, los recursos quedan bloqueados. Por eso uso el **patrón Saga**.

### Saga orquestada (la que elegiría para transferencias)
Un orquestador controla el flujo y el estado de la transferencia:

```
PENDIENTE → FONDOS_RETENIDOS → ACREDITADA → COMPLETADA
                  ↓ (falla el crédito)
             COMPENSANDO → REVERSADA / FALLIDA
```

1. **Retener fondos** en la cuenta origen (servicio A). Es una transacción local con bloqueo de la fila del saldo.
2. **Acreditar** la cuenta destino (servicio B).
3. **Confirmar** la retención como débito definitivo.
4. Si el paso 2 falla, se ejecuta la **compensación**: liberar la retención o registrar un movimiento de reverso. **Nunca se borra nada**: cada corrección queda como un movimiento nuevo, para que haya trazabilidad.

Prefiero la orquestación a la coreografía para transferencias porque el flujo es crítico y conviene tenerlo explícito y en un solo lugar.

### Piezas que garantizan la consistencia
- **Transactional Outbox:** el cambio de negocio y el evento a publicar se guardan **en la misma transacción local** (tabla `outbox`). Después un proceso lo publica en la cola. Así nunca queda un cambio guardado sin su evento, ni un evento publicado sin su cambio.
- **Idempotencia:** cada transferencia tiene un id único y los consumidores registran los mensajes ya procesados. Con eso un reintento o un mensaje duplicado no acredita dos veces.
- **Reintentos con *backoff* exponencial** para fallos temporales, y una **Dead Letter Queue** para los mensajes que fallan siempre, con alerta para revisión manual.
- **Timeouts** en cada paso y un **proceso de conciliación** periódico que detecta transferencias atascadas y las completa o las compensa.
- **Contabilidad de doble partida:** todo débito tiene su crédito, así que la suma de movimientos cuadra y se puede verificar.

### Manejo de errores hacia el cliente
- `202 Accepted` con el id de la transferencia, y un endpoint para consultar su estado (el proceso es asíncrono).
- `422 Unprocessable Entity` para reglas de negocio (saldo insuficiente), `409 Conflict` para operaciones duplicadas o en conflicto y `503` si un servicio no está disponible.
- Un formato de error uniforme con un código de negocio, para que el front sepa qué mostrar.

Esto da **consistencia eventual**: por unos instantes la transferencia está "en proceso", pero el sistema **siempre** termina en un estado correcto y auditable.

---

## 4. Pruebas unitarias y de integración

**Pregunta:** ¿Cómo diseñarías la suite de pruebas de una API bancaria? ¿Qué herramientas usarías?

Sigo la **pirámide de pruebas**: muchas unitarias (rápidas), menos de integración y pocas *end-to-end*.

### Unitarias — JUnit 5 + Mockito + AssertJ
- Prueban la **lógica de negocio aislada**: dominio y casos de uso, con los repositorios simulados (*mocks*). Con arquitectura hexagonal esto es natural, porque el dominio no depende de Spring.
- Cubren las reglas críticas: no se puede aprobar un préstamo que ya fue rechazado, el monto debe ser positivo, un usuario no puede ver préstamos ajenos, saldo insuficiente.
- Uso `@ParameterizedTest` para los **casos límite** (monto 0, negativo, el máximo permitido).
- Pruebo tanto el camino feliz como los errores.

### Pruebas de capa (*slices*)
- `@WebMvcTest` + **MockMvc**: rutas, validaciones, códigos HTTP y formato de errores.
- `spring-security-test` (`@WithMockUser(roles = "USER")`) para verificar que un usuario normal recibe **403** al intentar aprobar.
- `@DataJpaTest`: consultas personalizadas del repositorio.

### Integración — `@SpringBootTest` + Testcontainers
- **Testcontainers** levanta un PostgreSQL real en Docker. H2 no siempre se comporta igual que la base de producción (bloqueos, tipos, SQL específico).
- Pruebo el flujo completo (HTTP → servicio → BD), que la transacción hace **rollback** si algo falla a mitad de camino, y la **concurrencia**: dos aprobaciones simultáneas del mismo préstamo y solo una debe ganar.
- **WireMock** para simular servicios externos (central de riesgo), incluidos sus fallos y timeouts.

### Otras capas
- **Contrato:** Spring Cloud Contract o Pact, para que un cambio en la API no rompa a sus consumidores.
- **E2E:** Cypress o Playwright, solo para los flujos clave (login → solicitar → aprobar).
- **Rendimiento:** Gatling o JMeter. **Seguridad:** OWASP ZAP en el pipeline.
- **Frontend:** Jasmine/Karma o Jest, con `HttpTestingController` para los servicios HTTP.

### Calidad de la suite
- **JaCoCo** con un umbral mínimo de cobertura, más **PIT (mutation testing)**, que mide si las pruebas detectan errores de verdad y no solo pasan por las líneas.
- Pruebas independientes entre sí, datos de prueba con *builders* y nombres descriptivos (`aprobarPrestamoRechazadoLanzaExcepcion`).
- Todo corre en el **CI** y bloquea el *merge* si falla.

---

## 5. Front-end: estado y autenticación

**Pregunta:** En una app bancaria que muestra saldos, ¿cómo gestionarías el estado y la autenticación en el front, garantizando seguridad y coherencia?

### Autenticación
- **Login:** idealmente con OAuth2/OIDC (*Authorization Code* + PKCE). En un esquema propio, el backend devuelve un JWT.
- **Dónde guardo el token:**
  - **Access token** (vida corta) **en memoria**, en un servicio. No lo pongo en `localStorage`, porque cualquier XSS lo puede leer.
  - **Refresh token** en una **cookie `HttpOnly`, `Secure`, `SameSite=Strict`**, que JavaScript no puede leer.
- **Interceptor HTTP:** agrega `Authorization: Bearer <token>` a cada petición. Si recibe un 401, intenta renovar el token y, si no puede, cierra la sesión.
- **Guards** (`canActivate` / `canMatch`): protegen las rutas por autenticación y por rol (por ejemplo, `/admin` solo para ADMIN). Aclaración importante: **el guard es experiencia de usuario, no seguridad**. Cualquiera puede manipular el front, así que la autorización real siempre la valida el backend.
- **Cierre de sesión por inactividad**, y al hacer logout se limpia todo el estado en memoria.

### Estado y coherencia de datos
- **Una sola fuente de verdad:** un servicio con **signals** (o NgRx si la app es grande y se necesita trazabilidad de acciones con DevTools) guarda las cuentas y los saldos. Los componentes leen de ahí y no hacen cada uno su propia petición.
- **El saldo nunca se calcula en el front.** Siempre viene del backend. Después de cada operación se vuelve a consultar, o se actualiza en tiempo real con WebSocket o SSE.
- **Inmutabilidad:** el estado se reemplaza, no se muta. Así los cambios son predecibles y se detectan bien.
- **Evitar el doble envío:** el botón se deshabilita mientras la petición está en curso y la operación lleva una *idempotency key*.
- **Estados explícitos** de carga, error y vacío, para que el usuario nunca vea un saldo viejo como si fuera actual.
- **Nada sensible en `localStorage`.** Los números de cuenta se muestran enmascarados.

---

## 6. Spring Boot

### a. ¿Qué pasa internamente con `@SpringBootApplication` y cómo afecta el arranque?

Es una anotación compuesta por tres:

1. **`@SpringBootConfiguration`** (que es un `@Configuration`): marca la clase como fuente de definiciones de beans.
2. **`@EnableAutoConfiguration`**: activa la autoconfiguración. Spring Boot lee la lista de clases de autoconfiguración del archivo `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` de los starters, y cada una se aplica **solo si se cumplen sus condiciones**: `@ConditionalOnClass` (la librería está en el classpath), `@ConditionalOnMissingBean` (yo no definí ese bean) o `@ConditionalOnProperty`.
3. **`@ComponentScan`**: escanea el paquete de esa clase **y sus subpaquetes** buscando `@Component`, `@Service`, `@Repository`, `@Controller`, etc. Por eso la clase principal va en el paquete raíz.

**Secuencia de arranque** (`SpringApplication.run(...)`):
1. Detecta el tipo de aplicación (servlet o reactiva) según el classpath.
2. Prepara el `Environment`: lee `application.properties`/`yml`, variables de entorno, argumentos y el perfil activo.
3. Crea el `ApplicationContext`, registra las definiciones de beans (escaneo + autoconfiguración) y evalúa las condiciones.
4. Hace *refresh* del contexto: crea los beans singleton, inyecta dependencias y crea los proxies (transacciones, caché, seguridad).
5. Arranca el **servidor embebido** (Tomcat, o Netty en WebFlux).
6. Ejecuta los `CommandLineRunner` / `ApplicationRunner` y publica `ApplicationReadyEvent`.

**Efecto práctico:** con solo agregar `spring-boot-starter-data-jpa`, Boot ve Hibernate en el classpath y configura el `DataSource`, el `EntityManagerFactory` y el `TransactionManager` sin escribir configuración. Para ver qué se aplicó y por qué se arranca con `--debug` (*condition evaluation report*).

### b. Ciclo de vida de un bean y cómo intervenir

```
1. Definición (BeanDefinition)       ← BeanFactoryPostProcessor puede modificarla
2. Instanciación (constructor)
3. Inyección de dependencias
4. Interfaces *Aware (BeanNameAware, ApplicationContextAware…)
5. BeanPostProcessor.postProcessBeforeInitialization
6. Inicialización: @PostConstruct → afterPropertiesSet() → init-method
7. BeanPostProcessor.postProcessAfterInitialization  ← aquí se crean los proxies AOP
8. Bean listo para usarse
9. Al cerrar el contexto: @PreDestroy → destroy() → destroy-method
```

**Puntos donde puedo intervenir:**
- **`@PostConstruct` / `@PreDestroy`**: los más usados. Por ejemplo, precargar una caché al iniciar o cerrar conexiones al apagar.
- **`BeanPostProcessor`**: modifica o envuelve **cualquier** bean. Así implementa Spring internamente `@Transactional`, `@Cacheable` y `@Async`: devuelve un proxy en lugar del objeto original.
- **`BeanFactoryPostProcessor`**: cambia las definiciones antes de que se creen los beans (por ejemplo, la resolución de `${propiedades}`).
- **`SmartLifecycle`**: controla el orden de arranque y parada de componentes (consumidores de colas, por ejemplo).
- `@Lazy` (crear al primer uso), `@DependsOn` (orden de creación) y los eventos del contexto (`@EventListener(ApplicationReadyEvent.class)`).

**Alcances (*scopes*):** `singleton` (por defecto, una instancia por contexto), `prototype` (una nueva cada vez; Spring **no** llama a sus métodos de destrucción), y `request` / `session` en aplicaciones web.

**Detalle importante** (sale del paso 7): si un método de la misma clase llama a otro método `@Transactional`, **la transacción no se aplica**, porque la llamada no pasa por el proxy (*self-invocation*).

### c. Personalizar la autoconfiguración sin romper "convención sobre configuración"

La idea es **ajustar solo lo necesario y dejar que Boot haga el resto**. De la opción menos invasiva a la más invasiva:

1. **Propiedades y perfiles** (`application.yml`, `application-prod.yml`): la mayoría de autoconfiguraciones se ajustan así (`spring.datasource.*`, `spring.jpa.*`, `spring.cache.*`, `server.port`). No se escribe código.
2. **Customizers que expone Boot**: interfaces pensadas para modificar un bean autoconfigurado **sin reemplazarlo**, como el customizer del mapper JSON de Jackson, `WebMvcConfigurer` o `RestTemplateBuilder`/`RestClient.Builder`.
3. **Definir mi propio bean**: como las autoconfiguraciones usan `@ConditionalOnMissingBean`, si yo declaro, por ejemplo, mi `PasswordEncoder` o mi `SecurityFilterChain`, Boot respeta el mío y retira el suyo. Reemplazo solo esa pieza y no toda la configuración.
4. **Excluir una autoconfiguración** concreta: `@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)` o `spring.autoconfigure.exclude`. Es el último recurso.
5. **Crear mi propia autoconfiguración o starter**: una clase `@AutoConfiguration` con condicionales y propiedades tipadas (`@ConfigurationProperties`), registrada en el archivo `AutoConfiguration.imports`. Así otros equipos obtienen valores por defecto sensatos que también pueden sobrescribir. Es la misma filosofía de Boot aplicada a lo propio.

Para verificar qué autoconfiguración se aplicó y por qué, uso `--debug` o el endpoint `/actuator/conditions`.
