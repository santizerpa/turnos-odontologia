# Relevamiento — C-12 `admin-agenda-ui`

> **Naturaleza del documento:** exploración de solo lectura y análisis. No contiene propuestas de diseño, decisiones de implementación, pasos de ejecución ni código nuevo.
> **Fecha:** 2026-11-24 · **HEAD:** `a8e2f9c` · **Estado:** C-12 pendiente en el roadmap; no existe change OpenSpec activo asociado.
> **Modo:** `openspec-explore` en modo exploratorio/lectura. No se creó ni modificó ningún change OpenSpec; el trabajo posterior corresponde a `openspec-propose`.

---

## 1. Resumen ejecutivo

- C-12 figura **pendiente** (`[ ]`) en `CHANGES.md:352`, con governance **MEDIO** (`CHANGES.md:362`) y dependencias **C-03, C-05, C-06, C-10, C-11** (`CHANGES.md:361`). Está en FASE 5 junto a C-13 (`CHANGES.md:349-368`) y es el penúltimo eslabón del camino crítico (`CHANGES.md:98`).
- `openspec list --json` devuelve `{"changes":[]}` con `root.path = C:\Users\valen\turnos-odontologia`; hay **15 specs main** y **11 changes archivados**, ninguno para C-12. C-12 existe solo como ítem del roadmap.
- El backend ya expone todos los contratos HTTP que la UI administrativa necesita: autenticación por sesión, agenda, cambio de estado, pacientes, bandeja, lista de espera y enlaces públicos de confirmación/cancelación. En este relevamiento se documentan **14 endpoints** que C-12 consumiría: **10 del flujo administrativo** (uno de ellos público, `POST /api/auth/login`) y **4 de los enlaces tokenizados que aparecen en la bandeja**.
- No existe aún UI administrativa: `frontend/admin.html:1-5` es un shell de cinco líneas con `<h1>Administración</h1>` y el mismo `<script type="module" src="/main.js">` que la página pública; `frontend/src/main.ts:1-7` solo monta la reserva pública. El `admin.html` estático del backend carga `/js/main.js` (`backend/src/main/resources/static/admin.html:4`) y ese script solo hace `console.log('turnos-odontologia')` (`backend/src/main/resources/static/js/main.js:1`).
- **Hallazgo de seguridad concreto:** `/admin.html` es público, pero `/main.js` **no** está en la `permitList`; una petición sin sesión a `/main.js` cae en `anyRequest().authenticated()` y responde **401 JSON** (`{"error":"No autenticado"}`), sin redirect. No hay test que cubra `/main.js`; los tests cubren `/js/main.js`. Ver §5.
- Los formatos de error **no son uniformes**: la mayoría son `Map.of("error", <mensaje>)`, pero las validaciones de pacientes y el 500 de la bandeja usan `ProblemDetail` de Spring. Ver §4.4.

---

## 2. Ficha del change en el roadmap

Fuente: `CHANGES.md:351-367`.

| Atributo | Valor |
|---|---|
| Identificador | C-12 `admin-agenda-ui` |
| Estado | `[ ]` pendiente (`:352`) |
| Scope | "Interfaz administrativa del odontólogo" (`:353`) |
| Login | Pantalla con email y contraseña (`:354`) |
| Agenda | Diaria y semanal con estado de cada turno (`:355`) |
| Acciones | Confirmar, marcar ausente, marcar atendido y cancelar (`:356`) |
| Pacientes | Listado y detalle mínimo (`:357`) |
| Bandeja | Simulada desde `GET /api/outbox/messages`, con links de prueba (`:358`) |
| Lista de espera | Visible (`:359`) |
| Errores | Manejo de sesión expirada y errores de API (`:360`) |
| Dependencias | C-03, C-05, C-06, C-10, C-11 (`:361`) |
| Governance | MEDIO (`:362`) |
| Leer antes | `knowledge-base/06_funcionalidades.md` §Épica 1/3/5/6; `knowledge-base/07_flujos_principales.md` §Flujo 1/5; `knowledge-base/03_actores_y_roles.md` §RBAC; `knowledge-base/02_descripcion_general.md` §API REST (`:363-367`) |

Contexto de planificación:

- Árbol: `C-03 + C-05 + C-06 + C-10 + C-11 → C-12 admin-agenda-ui` (`CHANGES.md:47-48`).
- GATE 8: con `C-05 + C-06 + C-10 + C-11 ✓` se habilitan C-12 y C-13, con C-12 asignado a **Persona E** (`CHANGES.md:87-89`, `:113`).
- Las cinco dependencias figuran completas (`[x]`) en `CHANGES.md:165`, `:210`, `:232`, `:313`, `:329`.
- Camino crítico: `... → C-11 → C-12 → C-14` (`CHANGES.md:98`).
- `US-011` establece que **no existe carga manual de turnos por el odontólogo** (`knowledge-base/06_funcionalidades.md:33`); el scope de C-12 no menciona creación de turnos ni de pacientes.

---

## 3. Normativa y contexto relevante leído

### 3.1 Knowledge-base ("Leer antes")

- **`06_funcionalidades.md`**
  - Épica 1, `US-001`: login con email/contraseña, error controlado, hash seguro, sesión que expira o se invalida, rutas administrativas autenticadas (`:3-16`).
  - Épica 3, `US-010` ver agenda diaria/semanal con estados; `US-011` sin carga manual; `US-012` marcar confirmado, ausente o atendido conservando historial (`:27-36`).
  - Épica 5, `US-030` a `US-033`: mensaje con link seguro, confirmación por POST idempotente, cancelación hasta 24 hs antes y bandeja visible con link público de prueba (`:52-68`).
  - Épica 6, `US-040` inscripción pública y consulta privada de la cola; `US-041` aviso FIFO sin asignación automática (`:70-76`).
- **`07_flujos_principales.md`**
  - Flujo 1: error sin revelar qué dato es incorrecto; la frontend carga agenda y turnos tras el login (`:3-11`).
  - Flujo 5: la cancelación manual del odontólogo pasa por `AppointmentService.cancel(appointmentId, DENTIST, reason)`; el listener de C-11 notifica a la lista de espera (`:65-72`).
  - Casos de error: solapamiento 409, DNI 422, token vencido 410/400, base no disponible 503 sin credenciales (`:74-80`).
- **`03_actores_y_roles.md`**: un solo rol autenticado; odontólogo con acceso a agenda, pacientes, turnos, lista de espera y lectura de bandeja; anónimo sin acceso; cookie de sesión de 30 minutos de inactividad; pacientes sin login (`:11-35`).
- **`02_descripcion_general.md` §API REST**: inventario de rutas, incluidas `GET/POST/PATCH /api/appointments`, `PATCH /api/appointments/{id}/status`, `GET/POST/PATCH /api/patients`, `GET /api/waiting-list`, `GET /api/outbox/messages` protegida con sesión (`:51-71`).

### 3.2 Specs main vigentes (`openspec/specs/`)

| Spec | Escenarios normativos relevantes para C-12 |
|---|---|
| `autenticacion-odontologo/spec.md` | Login 200 con `id, email, nombre, apellido` + cookie `HttpOnly`, sin hash (`:13-18`); 401 genérico para contraseña incorrecta, email inexistente y `deleted_at` (`:20-31`, `:94-99`); sesión de 30 min y 401 al usar una inválida sin redirección (`:33-50`); rutas protegidas 401 y páginas/recursos públicos abiertos (`:52-72`); `me`/`logout` (`:74-88`); BCrypt (`:101-109`); cookie `SameSite=Lax` sin CSRF (`:111-124`). |
| `gestion-turnos/spec.md` | Agenda autenticada por `from`/`to` ordenada por inicio, rango inválido y 401 sin sesión (`:43-56`); matriz de transiciones PENDING→CONFIRMED/CANCELLED/ABSENT y CONFIRMED→CANCELLED/ABSENT/ATTENDED (`:30-41`); `PATCH /api/appointments/{id}/status` delega al servicio, cancelación por `DENTIST`, idempotencia, 404 y 401 (`:79-109`); solapamiento 409 (`:18-28`). |
| `gestion-pacientes/spec.md` | Alta/upsert por DNI normalizado, 201/200 (`:9-35`); nombre y apellido obligatorios con `400` (`:37-53`); email normalizado en `Patient` y `400` sin PII (`:55-71`); listado paginado `page`/`size` con metadatos (`:73-84`); búsqueda por DNI formateado, página vacía y `400` (`:86-102`); errores consistentes sin DNI/email (`:104-115`). |
| `outbox-reminders/spec.md` | Bandeja protegida: 401 sin sesión y elementos con `id, appointmentId, recipientEmail, subject, body, status, type, createdAt, sentAt`, sin DNI/hash/token (`:82-94`); `POST /api/outbox/reminders/run` responde `200` con `{created, failed}` (`:95-103`); fallo inesperado responde `500` con mensaje general fijo (`:56-60`); despacho idempotente (`:69-80`); reemisión de token invalida el link anterior (`:61-67`). |
| `lista-espera/spec.md` | Consulta autenticada FIFO por `created_at`, con estado y preferencias de fecha sin DNI; 401 sin sesión (`:49-58`); inscripción pública 201/400 (`:9-31`); una sola entrada elegible notificada por cancelación, sin asignar turno (`:60-102`). |

### 3.3 Artefactos archivados consultados

- `2026-11-22-dentist-auth/design.md:17,23,36,42-52,99-105`: sesión de servidor pensada para el frontend vanilla de C-12; C-12 queda como dueño del login y del manejo de sesión expirada; CSRF deshabilitado con mitigación `SameSite=Lax` y revisión pendiente en C-14; `/admin.html` queda como shell vacío abierto; 401 uniforme sin redirects.
- `2026-11-23-appointments-core/design.md:18,36,46`: sin UI; `AppointmentService.cancel(...)` es el único camino de cancelación; `GET`/`PATCH` sin sesión reciben 401 de la cadena existente.
- `2026-11-22-patient-records/design.md:18,35,41`: sin UI ni endpoints nuevos; DTO + `ProblemDetail` centralizado.
- `2026-11-24-outbox-and-reminders/design.md:59-67`: contrato de API y privacidad de los tres endpoints de bandeja; 500 solo para fallos fuera del procesamiento por turno.
- `2026-11-24-waiting-list/design.md:26,49,67`: sin UI; listener `AFTER_COMMIT` con transacción nueva; HTTP cubre inscripción anónima y consulta 401/con sesión.
- `2026-11-24-public-booking-ui/design.md:13,30-35,68,96`: todos los cuerpos de error son JSON con un único campo `error`; los `GET` de token devuelven JSON, no HTML; doble fuente de HTML estático (`classpath:/static/` y `file:../frontend/dist/`) ya registrada como riesgo fuera de alcance de C-08.

---

## 4. Contratos HTTP reales que C-12 consumiría

Notación: ` Instant ` = ISO-8601 con hora (`DateTimeFormat.ISO.DATE_TIME`); ` cookie ` = `JSESSIONID` de la sesión del odontólogo. Los formatos de error se detallan en §4.4. Todas las rutas marcadas **Autenticada** caen en `anyRequest().authenticated()` (`AuthSecurityConfiguration.java:65-66`).

### 4.1 Autenticación y sesión

| Ruta | Método | Request | Éxito | Errores | Acceso | Evidencia |
|---|---|---|---|---|---|---|
| `/api/auth/login` | POST | JSON `{"email": string, "password": string}`; sin Bean Validation; `null`/blank → 401. Sin cookie requerida. | **200** `{id, email, firstName, lastName}`; `Set-Cookie: JSESSIONID=...; HttpOnly; SameSite=Lax`. Sin hash. | **401** `{"error":"Credenciales inválidas"}` para credenciales erróneas, email inexistente, odontólogo dado de baja, hash no BCrypt o campos vacíos; sin `Set-Cookie`. | Público (`permitAll`) | `AuthController.java:26,34-49,65-71`; `LoginRequest.java:3`; `DentistProfileResponse.java:7-16`; `AuthSessionService.java:18,32-37`; `application.yml:20-26`; `AuthCookieAttributesTests.java:57-59`; `AuthLoginTests.java:35-82`; spec `autenticacion-odontologo:13-31,94-124`. |
| `/api/auth/me` | GET | Sin body; cookie `JSESSIONID`. | **200** `{id, email, firstName, lastName}`, sin `passwordHash`/`password_hash`. | **401** `{"error":"No autenticado"}` (sin sesión, sesión inexistente o inválida). | Autenticada | `AuthController.java:51-57`; `JsonAuthenticationEntryPoint.java:28-34`; `AuthSessionExpiryTests.java:41-45`; `AuthLoginTests.java:36-50`. |
| `/api/auth/logout` | POST | Sin body; cookie `JSESSIONID`. Sin token CSRF. | **204** sin cuerpo; sesión invalidada y cookie `JSESSIONID` limpiada. | **401** `{"error":"No autenticado"}` sin sesión. | Autenticada | `AuthController.java:59-63`; `AuthSessionService.java:39-42`; `AuthRouteSecurityTests.java:61-80`; spec `autenticacion-odontologo:84-88,121-124`. |

### 4.2 Datos administrativos

| Ruta | Método | Request | Éxito | Errores | Acceso | Evidencia |
|---|---|---|---|---|---|---|
| `/api/appointments?from=&to=` | GET | Query ** obligatorios **`from` y `to` como `Instant` ISO; cookie. | **200** array ordenado por `startAt` de `{id, dentistId, patientId, startAt, durationMinutes, status, activeSlot}`; `activeSlot` puede ser `null` fuera de este endpoint. | **401** `{"error":"No autenticado"}`; **422** `{"error":"Rango de agenda inválido"}` si `from >= to` o nulos; **400** `{"error":"Parámetros from y to inválidos o faltantes"}` si faltan o no parsean. | Autenticada | `AppointmentController.java:22-27`; `AppointmentResponse.java:6-12`; `AppointmentService.java:58-64`; `AppointmentRepository.java:10-11`; `AppointmentExceptionHandler.java:15-33`; `AppointmentApiTests.java:48-67`; `AuthRouteSecurityTests.java:42-44`. |
| `/api/appointments/{id}/status` | PATCH | Path `UUID`; JSON `{"status":"CONFIRMED\|ABSENT\|ATTENDED\|CANCELLED", "reason": string\|null}`; cookie. No usa `@Valid`. | **200** para `CANCELLED`: `{"cancelled": true\|false, "message":"cancelado"\|"ya cancelado"}`; para los otros estados: `{"status":"<ESTADO>", "message":"estado actualizado"}`. | **401** `{"error":"No autenticado"}`; **404** `{"error":"Turno no encontrado: <uuid>"}`; **422** `{"error":"Transición de estado inválida"}`; **409** `{"error":"La franja solicitada ya está ocupada"}` ante `AppointmentConflictException`/`DataIntegrityViolationException`. | Autenticada | `AppointmentController.java:29-38`; `AppointmentStatusRequest.java:3`; `AppointmentService.java:66-123`; `CancelAppointmentResult.java:3-10`; `AppointmentExceptionHandler.java:15-33`; `AppointmentApiTests.java:69-92`; `AppointmentConflictHttpTest.java:29-41`. |
| `/api/patients?dni=&page=&size=` | GET | Query opcionales: `dni` (normalizado), `page` default `0` `@Min(0)`, `size` default `10` `@Min(1) @Max(100)`; cookie. | **200** `{"content":[{"id","dni","nombre","apellido","email"}], "page":n, "size":n, "totalElements":n, "totalPages":n}`; sin coincidencias → `content: []` con metadatos. | **401** `{"error":"No autenticado"}`; **400** `ProblemDetail` con `detail="Datos de paciente inválidos."` para DNI inválido o `page`/`size` fuera de rango. | Autenticada | `PatientController.java:19,28-33`; `PatientResponse.java:5-10`; `PatientPageResponse.java:7-13`; `PatientService.java:41-60`; `PatientExceptionHandler.java:12-21`; `PatientControllerTest.java:46-83`; `PatientIntegrationTest.java:67-75`. |
| `/api/outbox/messages` | GET | Sin body ni parámetros; cookie. | **200** array de `{id, appointmentId, recipientEmail, subject, body, status, type, createdAt, sentAt}`; `appointmentId` y `sentAt` pueden ser `null`. `body` contiene los enlaces tokenizados en texto plano. | **401** `{"error":"No autenticado"}`; ante `RuntimeException` del controlador, **500** `ProblemDetail` (ver §4.4). | Autenticada | `OutboxController.java:26-29`; `OutboxMessageDto.java:6-29`; `C10OutboxHttpTests.java:88-98,128-133`; `PublicBookingService.java:86-89`; `ReminderPerAppointmentTransaction.java:25-27`. |
| `/api/outbox/reminders/run` | POST | Sin body; cookie. | **200** `{"created": n, "failed": m}`; `failed` cuenta fallos aislados por turno y no expone mensajes internos. | **401** `{"error":"No autenticado"}`; fallo inesperado fuera del ciclo por turno → **500** `ProblemDetail` con `title="Error interno"`. | Autenticada | `OutboxController.java:31-35`; `ReminderGenerationService.java:28-52`; `OutboxExceptionHandler.java:10-21`; `C10OutboxHttpTests.java:81-82`; `C10OutboxErrorHttpTests.java:46-67`; spec `outbox-reminders:56-60,95-103`. |
| `/api/outbox/dispatch` | POST | Sin body; cookie. | **200** `{"dispatched": n}`; repetirlo devuelve `dispatched = 0`. | **401** `{"error":"No autenticado"}`; mismo **500** `ProblemDetail` del advice de bandeja. | Autenticada | `OutboxController.java:37-40`; `C10OutboxHttpTests.java:83-86`; `OutboxExceptionHandler.java:10-21`; spec `outbox-reminders:69-80`. |
| `/api/waiting-list` | GET | Sin body ni paginación; cookie. | **200** array ordenado por `created_at` ascendente y `id` en empate, con `{id, nombre, apellido, email, requestedDateFrom, requestedDateTo, status}`; **sin DNI**. | **401** `{"error":"No autenticado"}`. | Autenticada | `WaitingListController.java:25-28`; `WaitingListAdminResponse.java:6-19`; `WaitingListQueryService.java:17-22`; `WaitingListApiTest.java:85-112`; spec `lista-espera:49-58`. |

### 4.3 Enlaces tokenizados expuestos por la bandeja

Estas cuatro rutas son **públicas** (`/api/public/**`, `AuthSecurityConfiguration.java:61`): no requieren ni consumen la cookie del odontólogo. Son los enlaces que aparecen dentro de `body` de `GET /api/outbox/messages` (`PublicBookingService.java:86-89`, `ReminderPerAppointmentTransaction.java:25-27`).

| Ruta | Método | Request | Éxito | Errores | Acceso | Evidencia |
|---|---|---|---|---|---|---|
| `/api/public/appointments/{token}/confirm` | GET | Path `token` plano del body del mail. | **200** `{"startAt":"<Instant.toString()>", "status":"PENDING"}` (u otro estado); solo informativo, no cambia el turno. | **404** `{"error":"Token inexistente o inválido"}`; **410** `{"error":"Token vencido"}`. | Público | `PublicAppointmentTokenController.java:23-27,35-39`; `PublicAppointmentTokenExceptionHandler.java:15-23`; `openspec/changes/archive/2026-11-24-public-booking-ui/design.md:35`. |
| `/api/public/appointments/{token}/confirm` | POST | Path `token`; sin body. | **200** `{"message":"confirmación exitosa", "status":"CONFIRMED"}` o `{"message":"ya confirmado", "status":"CONFIRMED"}`. | **409** `{"error":"El turno no puede confirmarse"}` (estado `CANCELLED`/`ABSENT`/`ATTENDED`) o `{"error":"No se pudo confirmar el turno"}`; **404**/**410** de token. | Público | `PublicAppointmentTokenController.java:41-63`; `PublicAppointmentTokenExceptionHandler.java:15-23,37-41`. |
| `/api/public/appointments/{token}/cancel` | GET | Path `token`. | **200** `{"startAt":"<Instant>", "status":"<ESTADO>"}`; solo informativo. | **404** `{"error":"Token inexistente o inválido"}`; **410** `{"error":"Token vencido"}`. | Público | `PublicAppointmentTokenController.java:29-33,35-39`; `PublicAppointmentTokenExceptionHandler.java:15-23`. |
| `/api/public/appointments/{token}/cancel` | POST | Path `token`; sin body. | **200** `{"message":"cancelación exitosa", "status":"CANCELLED"}` o `{"message":"ya cancelado", "status":"CANCELLED"}`. | **409** `{"error":"Debe contactarse al consultorio para cancelar el turno"}` (< 24 hs) o `{"error":"No se pudo cancelar el turno"}`; **404**/**410** de token. | Público | `PublicAppointmentTokenController.java:65-83`; `PublicAppointmentTokenExceptionHandler.java:15-35`; `AppointmentService.java:67-81`. |

No se incluyen en las tablas `POST /api/patients`, `GET /api/public/slots`, `POST /api/public/appointments` ni `/api/schedule/**`: pertenecen a C-06/C-07/C-04/C-08 y el scope de C-12 (`CHANGES.md:353-360`) no prevé alta de pacientes ni carga manual de turnos (`knowledge-base/06_funcionalidades.md:33`).

### 4.4 Formatos exactos de error

**401 sin sesión (cualquier ruta autenticada, incluidas `/api/appointments`, `/api/patients`, `/api/outbox/**` y `/api/waiting-list`)**

```json
{"error":"No autenticado"}
```

`Content-Type: application/json`, encoding UTF-8, sin redirect. Lo escribe `JsonAuthenticationEntryPoint.java:28-34`; lo comprueban `AuthRouteSecurityTests.java:37-45`, `C10OutboxHttpTests.java:128-133` y `WaitingListApiTest.java:100-101`.

**401 de login** (no pasa por el entry point, sino por el controlador):

```json
{"error":"Credenciales inválidas"}
```

`AuthController.java:26,38-48,65-67`; `AuthLoginTests.java:53-82`.

**409**

```json
{"error":"La franja solicitada ya está ocupada"}
```

`AppointmentExceptionHandler.java:25-28` y `AppointmentConflictHttpTest.java:29-41`; mismo texto en reserva pública (`PublicBookingExceptionHandler.java:47-51`). Otras variantes 409 de los enlaces tokenizados figuran en §4.3 (`PublicAppointmentTokenController.java:52`; `PublicAppointmentTokenExceptionHandler.java:25-41`).

**422**

```json
{"error":"Transición de estado inválida"}
{"error":"Rango de agenda inválido"}
```

`AppointmentExceptionHandler.java:20-23` mapea `IllegalArgumentException`/`IllegalStateException` a 422; los mensajes salen de `AppointmentService.java:60,122`. Confirmado por `AppointmentApiTests.java:61-63,83-85`. En la API pública además existen `{"error":"La fecha solicitada está fuera de la ventana pública"}`, `{"error":"La franja solicitada no está disponible"}` y `{"error":"Datos de reserva inválidos"}` (`PublicBookingExceptionHandler.java:23-64`).

**400 / 500 como `ProblemDetail`**

`PatientExceptionHandler.java:12-21` es un `@RestControllerAdvice` **global** y devuelve `ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos de paciente inválidos.")`. Con Spring Boot 3.3.13 (`backend/build.gradle:3`; Spring Framework 6.1.21 en el classpath local), el cuerpo serializado es:

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Datos de paciente inválidos."}
```

`instance` no aparece porque es `null` y el mixin Jackson de `ProblemDetail` usa `@JsonInclude(NON_EMPTY)`; `title="Bad Request"` se deriva del reason phrase del status. Los tests afirman `$.status = 400`, `$.title = "Bad Request"` y `$.detail = "Datos de paciente inválidos."` (`PatientControllerTest.java:70-83,97-113,172-187`; `PatientIntegrationTest.java:57-63`), pero **no** afirman explícitamente `type` ni `instance`: esos dos campos provienen del comportamiento por defecto del framework, no de código del proyecto.

El advice de bandeja es `@RestControllerAdvice(assignableTypes = OutboxController.class)` con `@Order(Ordered.HIGHEST_PRECEDENCE)` y sobrescribe el título (`OutboxExceptionHandler.java:10-21`):

```json
{"type":"about:blank","title":"Error interno","status":500,"detail":"Error interno al procesar la bandeja"}
```

Lo comprueban `C10OutboxErrorHttpTests.java:56-66` (`$.title`, `$.detail`, y ausencia del mensaje interno, `Exception` y `at com.`). Igual que en el caso anterior, `instance` queda omitido por ser `null`.

**Otros errores documentados**: 404 de turno `{"error":"Turno no encontrado: <uuid>"}` (`AppointmentNotFoundException.java:5-9`; `AppointmentExceptionHandler.java:15-18`); 404 de token `{"error":"Token inexistente o inválido"}` y 410 `{"error":"Token vencido"}` (`PublicAppointmentTokenExceptionHandler.java:15-23`); 400 de inscripción pública `{"error":"Datos de inscripción inválidos."}` (`WaitingListExceptionHandler.java:15-22`).

---

## 5. Seguridad de recursos estáticos: `/main.js`, `/admin.html`, `/index.html` y CSS

### 5.1 Reglas efectivas (cita literal)

`backend/src/main/java/com/turnosodontologia/auth/config/AuthSecurityConfiguration.java:60-66`:

```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/api/auth/login", "/api/public/**", "/api/health", "/", "/index.html", "/admin.html", "/js/**", "/error")
    .permitAll()
    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
    .permitAll()
    .anyRequest()
    .authenticated())
```

No hay otros matchers, `WebSecurityCustomizer`, `ignoring()` ni `addResourceHandlers` en `backend/src/main/java` (búsqueda exhaustiva sobre el código fuente). La cadena deshabilita CSRF explícitamente (`:52-54`), `httpBasic`, `formLogin` y `logout` de Spring Security (`:67-69`), y usa `HttpSessionSecurityContextRepository` (`:28-31`).

`PathRequest` se importa de `org.springframework.boot.autoconfigure.security.servlet.PathRequest` (`AuthSecurityConfiguration.java:4`). En el jar local `spring-boot-autoconfigure-3.3.13.jar`, `org.springframework.boot.autoconfigure.security.StaticResourceLocation` declara los patrones `/css/**`, `/js/**`, `/images/**`, `/webjars/**`, `/favicon.*` y `/*/icon-*`; **no figura `/main.js` ni `/*.js` en la raíz**.

### 5.2 Dónde está cada archivo

| Recurso | Ubicación real | ¿Coincide con un patrón permitido? |
|---|---|---|
| `/admin.html`, `/index.html` | Listados literalmente en `permitAll` (`AuthSecurityConfiguration.java:61`); existen en `backend/src/main/resources/static/` (`:1-5` de cada HTML) y en `frontend/dist/` según el build (`frontend/package.json:5`). | Sí, literal. |
| `/js/main.js` | `backend/src/main/resources/static/js/main.js:1`; patrón `/js/**`. | Sí, por `/js/**` y por `atCommonLocations()`. |
| `/main.js` (raíz) | `frontend/dist/main.js` (compilado de `frontend/src/main.ts`) y referenciado por `frontend/index.html:4` y `frontend/admin.html:4`. | **No.** No está en la lista literal ni en los patrones comunes de Spring Boot. |
| `/css/**`, imágenes, favicon | Solo patrones de `atCommonLocations()`. | Sí, por el matcher de recursos comunes. |

Orden de static locations: `application-local.yml:6-8` pone `file:../frontend/dist/` **antes** que `classpath:/static/`; `application-test.yml:7-9` pone `classpath:/static/` **antes**. `application.yml` no declara static locations (`:1-26`).

### 5.3 Evidencia de tests y conclusión

| Petición sin sesión | Resultado observado en tests | Fuente |
|---|---|---|
| `GET /` | 200 | `AuthRouteSecurityTests.java:49` |
| `GET /index.html` | 200 | `AuthRouteSecurityTests.java:50`; `FoundationApplicationTests.java:42` |
| `GET /admin.html` | 200 | `AuthRouteSecurityTests.java:51`; `FoundationApplicationTests.java:43` |
| `GET /js/main.js` | 200 | `FoundationApplicationTests.java:44` |
| `GET /js/app.js` | 404 (no 401): el matcher permite la ruta, el archivo no existe | `AuthRouteSecurityTests.java:52` |
| `GET /api/auth/me`, `GET /api/appointments` | 401 con `{"error":"No autenticado"}` | `AuthRouteSecurityTests.java:37-45` |
| `GET /main.js` | **Sin test.** Única coincidencia de `main.js` en tests es `/js/main.js` | búsqueda en `backend/src/test/java`; `FoundationApplicationTests.java:44` |

**Conclusión concreta:** una petición sin sesión a `/main.js` responde **401 JSON `{"error":"No autenticado"}`**, no es redirigida y no se sirve el script. La cadena evalúa la autorización antes de la resolución del recurso, por lo que aunque en el perfil `local` el archivo exista en `frontend/dist/main.js` (`application-local.yml:8`), la solicitud es rechazada por `anyRequest().authenticated()` y el entry point JSON (`JsonAuthenticationEntryPoint.java:28-34`). En el perfil `test`, además, `classpath:/static/` no contiene `/main.js` (solo `/js/main.js`). Los shells de `frontend/` cargan precisamente esa ruta bloqueada (`frontend/index.html:4`, `frontend/admin.html:4`), mientras que los shells de `backend/src/main/resources/static/` cargan la ruta permitida `/js/main.js` (`static/index.html:4`, `static/admin.html:4`).

Esta conclusión se deriva de configuración, patrones del framework y cobertura de tests; **no se inició el servidor ni se ejecutaron tests** durante este relevamiento.

---

## 6. Preguntas abiertas / riesgos

1. **Script bloqueado frente a shells públicos.** `frontend/index.html:4` y `frontend/admin.html:4` cargan `/main.js`, pero esa ruta no está permitida sin sesión (§5). No hay artefacto en `openspec/` que registre este desajuste.
2. **Cobertura de tests.** No existe un testMockMvc ni HTTP que pida `/main.js` sin sesión; los tests cubren `/js/main.js`, `/index.html` y `/admin.html` (`FoundationApplicationTests.java:40-45`, `AuthRouteSecurityTests.java:47-59`).
3. **Doble fuente de HTML.** `classpath:/static/` y `file:../frontend/dist/` contienen HTML con nombres iguales y distinto orden en `local` y `test` (`application-local.yml:8`, `application-test.yml:9`). Ya fue señalado como riesgo en `openspec/changes/archive/2026-11-24-public-booking-ui/verification.md:8,84`.
4. **Agenda sin turnos cancelados.** `GET /api/appointments` filtra `activeSlotIsNotNull` (`AppointmentService.java:62`, `AppointmentRepository.java:10-11`) y `active_slot` pasa a `NULL` al cancelar (`CHANGES.md:218`). Un turno `CANCELLED` no aparece en el rango devuelto, aunque la spec hable de "estado de cada turno" (`gestion-turnos/spec.md:43-49`).
5. **Identidad de pacientes en la UI.** `/api/appointments` expone solo `patientId` (`AppointmentResponse.java:6-7`); `/api/waiting-list` expone nombre y email pero no DNI (`WaitingListAdminResponse.java:6-14`); no existe `GET /api/patients/{id}` (`PatientController.java:28-43`). El significado de "detalle mínimo" (`CHANGES.md:357`) no está precisado en las specs.
6. **Formatos de error mixtos.** Turnos, auth, bandeja (401) y lista de espera usan `{"error": ...}`; pacientes y el 500 de bandeja usan `ProblemDetail` (§4.4). La spec `gestion-pacientes` exige "error consistente" sin fijar el esquema (`:104-115`).
7. **Pérdida de detalle de vencimiento.** Para la UI, una sesión expirada y una cookie inexistente producen el mismo 401; el test no simula el paso de 30 minutos, solo verifica la configuración y una cookie inexistente (`AuthSessionExpiryTests.java:19-21,33-45`).
8. **CSRF.** Está deshabilitado globalmente con cookie de sesión (`AuthSecurityConfiguration.java:52-54`) y la revisión quedó registrada para C-14 (`openspec/changes/archive/2026-11-22-dentist-auth/design.md:36-40,105`).
9. **Listados sin paginación.** `GET /api/outbox/messages` usa `findAll()` (`OutboxController.java:28`) y `GET /api/waiting-list` devuelve una lista completa (`WaitingListQueryService.java:17-22`), a diferencia de `/api/patients`, que acota `size` a 100 (`PatientController.java:31`).
10. **Enlaces de prueba como texto.** El DTO de bandeja no tiene campo de enlace (`OutboxMessageDto.java:6-16`); los links están dentro de `body` como texto relativo (`PublicBookingService.java:86-89`, `ReminderPerAppointmentTransaction.java:25-27`). Los `GET` de esos enlaces devuelven JSON, no una página HTML (`PublicAppointmentTokenController.java:35-39`).
11. **Validez de los links tras recordatorio.** Generar un recordatorio reemite el token y deja sin efecto el link de reserva anterior, según la spec `outbox-reminders:61-67` y el diseño archivado (`2026-11-24-outbox-and-reminders/design.md:23-30`).
12. **Cuerpo inválido en `PATCH`.** `AppointmentExceptionHandler.java:15-33` no declara manejador para `HttpMessageNotReadableException`; un `status` que no sea un `AppointmentStatus` válido no tiene formato de error verificado en tests (`AppointmentApiTests.java:69-92`).
13. **Error 503 documentado pero no implementado.** `knowledge-base/07_flujos_principales.md:80` describe 503 para base no disponible sin credenciales; no hay un manejador 503 en los controladores relevados, salvo el 503 de ventana/reserva pública (`PublicBookingExceptionHandler.java:29-33`).

---

## 7. Estado y verificación

- No se creó ni modificó código fuente, configuración, artefactos OpenSpec, tests ni documentación existente. No se creó un change OpenSpec: `openspec list --json` confirma `changes: []`.
- No se ejecutaron builds ni tests (`./gradlew`, `npm`, servidores), en cumplimiento del modo de solo lectura.
- Fuentes leídas: `CHANGES.md`, `AGENTS.md`, `.atl/skill-registry.md`, `openspec/config.yaml`, `docs/explore/C-06-patient-records.md`, `docs/explore/C-10-explore.md`, KB `02`, `03`, `06`, `07`, las cinco specs main indicadas, once directorios de `openspec/changes/archive/`, controladores/DTO/handlers/config de seguridad de `backend/src/main/java`, recursos de `backend/src/main/resources`, tests de `backend/src/test/java` y archivos de `frontend/` (`index.html`, `admin.html`, `src/main.ts`, `package.json`).
- Verificación adicional de framework realizada en modo lectura sobre los jars locales del caché Gradle: `spring-web-6.1.21.jar` (`ProblemDetail`: `type = about:blank`, `title` derivado del reason phrase), `spring-web-6.2.7-sources.jar` (`ProblemDetailJacksonMixin`: `@JsonInclude(NON_EMPTY)`) y `spring-boot-autoconfigure-3.3.13.jar` (`StaticResourceLocation`: patrones de recursos comunes).
- Único archivo incorporado al repositorio como resultado de esta tarea: este documento.
