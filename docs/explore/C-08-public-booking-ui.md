# Relevamiento — C-08 `public-booking-ui`

> **Naturaleza del documento:** exploración de solo lectura. No contiene propuestas de diseño, decisiones de implementación ni código nuevo.
> **Fecha:** 2026-11-23 · **Rama/repo:** `turnos-odontologia` (directorio de trabajo actual).
> **Estado:** C-08 pendiente en el roadmap; `openspec list --json` devuelve `{"changes":[]}` y `{"root":{"path":"C:\\Users\\valen\\turnos-odontologia","source":"nearest"}}`.
> **No se proponen ni implementan cambios.**

---

## 1. Ficha del change en el roadmap

Fuente: `CHANGES.md:268-283` (FASE 3 "Reserva pública", `CHANGES.md:249`).

| Atributo | Valor |
|---|---|
| Identificador | C-08 `public-booking-ui` |
| Estado | `[ ]` pendiente (`CHANGES.md:269`) |
| Scope | "Interfaz pública de reserva en TypeScript vanilla" (`CHANGES.md:270`) |
| Selector de fecha | Dentro de la ventana permitida (`CHANGES.md:271`) |
| Formulario | Listado de slots libres y datos del paciente (`CHANGES.md:272`) |
| Confirmación | Confirmación del turno creado (`CHANGES.md:273`) |
| Errores | Manejo visible de 409 y 422 (`CHANGES.md:274`) |
| Enlace | "ya tengo un turno" que abre la página GET y ejecuta la acción con POST (`CHANGES.md:275`) |
| Verificación | Verificación manual documentada; sin framework (`CHANGES.md:276`) |
| Dependencias | C-07 (`CHANGES.md:277`) |
| Governance | BAJO (`CHANGES.md:278`) |
| Leer antes | `knowledge-base/07_flujos_principales.md §Flujo 2`, `knowledge-base/06_funcionalidades.md §US-020 / §US-021`, `knowledge-base/02_descripcion_general.md §API REST`, `knowledge-base/08_arquitectura_propuesta.md §Estructura de directorios` (`CHANGES.md:279-283`) |

Contexto de planificación:

- Árbol de dependencias: `C-05 appointments-core → C-09 token-confirmation → C-07 public-booking-api → C-08 public-booking-ui` (`CHANGES.md:38-41`); C-07 también requiere C-06 (`CHANGES.md:45`).
- GATE 5 habilita C-07 con `C-09 ✓ + C-06 ✓` (`CHANGES.md:77-79`); GATE 6 habilita C-08 con `C-07 ✓` (`CHANGES.md:81-82`).
- Plan de 5 personas: C-08 queda en el paso 7, asignado a **Persona E (Frontend)** (`CHANGES.md:104-115`, específicamente `:112`).
- C-08 **no** integra el camino crítico de 9 changes (`CHANGES.md:95-99`).
- C-07 figura pendiente en el roadmap (`CHANGES.md:252`) y C-09 completo (`CHANGES.md:290`), pero en disco existe un change archivado `openspec/changes/archive/2026-11-23-public-booking-api/` con implementación, tests y spec delta; no hay carpeta de change para C-08 (`CHANGES.md:268`).

---

## 2. Contrato relevado de la knowledge base

### 2.1 `knowledge-base/07_flujos_principales.md` — §Flujo 2 (reserva pública), `:13-26`

- Disparador: el paciente abre el enlace público; actores: paciente, frontend público, API, repositorio de pacientes y de turnos (`:15-16`).
- Pasos: `GET /api/public/slots` dentro de 24 hs–30 días (`:18`); la API calcula slots libres con turnos activos y duración de 30 minutos (`:19`); carga nombre, apellido, DNI y email (`:20`); reutiliza o crea `Patient` por DNI (`:21`); crea `Appointment` en `PENDING` dentro de transacción (`:22`); llama a `AppointmentTokenService.issue(appointment)` (`:23`); C-09 persiste solo el hash y devuelve el token plano una sola vez (`:24`); guarda `OutboxMessage` simulado con el enlace (`:25`); si la franja estaba tomada, devuelve 409 y no crea entidades (`:26`).
- "Casos de error" (`:71-77`): slot ocupado → 409; DNI no válido → 422 **con campo a corregir** (`:74`); token vencido/usado → 410 o 400 según mensaje del API (`:75`); turno pasado → 409/422 sin cambios (`:76`); base no disponible → 503 sin exponer credenciales (`:77`).
- Flujo 3 (`:28-46`) complementa la parte tokenizada: `GET /confirm` y `GET /cancel` "solo muestran información y las acciones disponibles; no cambian el turno" (`:38`), `POST` ejecuta la acción (`:39`), 404 token inexistente / 410 vencido / 409 estado impide la operación (`:43`), y "Devuelve una página con el estado final" (`:46`).

### 2.2 `knowledge-base/06_funcionalidades.md` — §US-020 y §US-021

- **US-020 — Consultar horarios** (`:40-41`): "El paciente ve horarios libres desde 24hs hasta 30 días."
- **US-021 — Reservar turno** (`:43-44`): "El paciente elige un slot libre y queda PENDING; conflicto devuelve 409."
- Contexto de la misma épica: US-022 cancela hasta 24 hs antes (`:46-47`) y US-023 permite reservar nuevamente tras cancelar (`:49-50`). US-002 exige nombre, apellido, DNI y email válidos, DNI normalizado y reutilización por DNI (`:20-25`).

### 2.3 `knowledge-base/02_descripcion_general.md` — §API REST, `:51-68`

- Lista de endpoints aplicables a la reserva pública (`:61-66`):
  - `GET /api/public/slots?date=...`
  - `POST /api/public/appointments`
  - `GET /api/public/appointments/{token}/confirm` — consulta informativa, no confirma
  - `POST /api/public/appointments/{token}/confirm` — ejecuta la confirmación
  - `GET /api/public/appointments/{token}/cancel` — consulta informativa, no cancela
  - `POST /api/public/appointments/{token}/cancel` — ejecuta la cancelación
- Stack declarado: frontend TypeScript vanilla, sin frameworks (`:12`); plantillas/UI HTML/CSS/TypeScript compilado, diseño visual diferido (`:13`).

### 2.4 `knowledge-base/08_arquitectura_propuesta.md` — §Estructura de directorios, `:23-44`

```text
frontend/
└── src/
    ├── admin/
    ├── public-reservation/
    ├── shared/
    └── main.ts
```

- La estructura propuesta para el frontend figura en `:36-41`; el backend se organiza en paquetes `auth/`, `patient/`, `appointment/`, `waitinglist/`, `messaging/`, `api/`, `config/` (`:27-35`).
- Seguridad/architectura relacionada: sesión de 30 minutos, rutas administrativas autenticadas, token de 32 bytes en base64url con hash SHA-256 (`:46-56`), perfiles `local`, `test` y `seed` (`:71-75`).

---

## 3. Contrato HTTP real de los endpoints públicos

Todos los controladores públicos son `@RestController` (`PublicBookingController.java:16-18`, `PublicAppointmentTokenController.java:12-14`) y devuelven JSON. Los manejadores públicos construyen bodies con `Map.of("error", ...)` / `Map.of("message", ..., "status", ...)`; **no usan `ProblemDetail`** en ninguno de los dos advices dedicados (`PublicBookingExceptionHandler.java:19-65`, `PublicAppointmentTokenExceptionHandler.java:12-42`).

### 3.1 Tabla de endpoints

| Método | Ruta | Éxito (estado + body) | Errores (estado + body) | Regla que dispara el error | Fuentes |
|---|---|---|---|---|---|
| `GET` | `/api/public/slots?date={yyyy-MM-dd}` | `200` · `{"slots":[{"start":"<OffsetDateTime>","end":"<OffsetDateTime>"}]}` (`PublicSlotsResponse.java:6`, `AvailabilitySlot.java:3`) | `422` `{"error":"La fecha solicitada está fuera de la ventana pública"}`; `422` `{"error":"Datos de reserva inválidos"}`; `503` `{"error":"Servicio temporalmente no disponible"}` | Ventana: `date == null` o `!(from < to)` con `from=max(inicioDía, now+24h)`, `to=min(finDía, now+30d)`; parámetro ausente/no ISO/no parseable; `dentists.findAll().size() != 1` | Controlador `PublicBookingController.java:25-28`; reglas `PublicBookingService.java:52-64, 91-95`; handler `PublicBookingExceptionHandler.java:23-27, 29-33, 59-64`; DTO de fecha `@DateTimeFormat(iso=DATE)` en `PublicBookingController.java:26` |
| `POST` | `/api/public/appointments` | `201` · `{"startAt":"<Instant ISO>","durationMinutes":30,"status":"PENDING"}` (`PublicBookingResponse.java:5`; `PublicBookingController.java:30-33`) | `422` `{"error":"La fecha solicitada está fuera de la ventana pública"}`; `422` `{"error":"La franja solicitada no está disponible"}`; `422` `{"error":"Datos de reserva inválidos"}`; `409` `{"error":"La franja solicitada ya está ocupada"}`; `503` `{"error":"Servicio temporalmente no disponible"}`; `500` `{"error":"No se pudo procesar la reserva"}` | Ver detalle en §3.2 | `PublicBookingController.java:30-33`; `PublicBookingService.java:66-104`; `PublicBookingRequest.java:9-13`; handler `PublicBookingExceptionHandler.java:23-64` |
| `GET` | `/api/public/appointments/{token}/confirm` | `200` · `{"startAt":"<Instant>","status":"<Estado>"}` (`PublicAppointmentTokenController.java:35-39`) | `404` `{"error":"Token inexistente o inválido"}`; `410` `{"error":"Token vencido"}` | `tokens.resolve(token)` valida formato 43 chars base64url, existencia por hash SHA-256 y `now < expiresAt` antes de leer el turno | `PublicAppointmentTokenController.java:23-27`; `AppointmentTokenService.java:22-33`; handler `PublicAppointmentTokenExceptionHandler.java:15-23` |
| `GET` | `/api/public/appointments/{token}/cancel` | `200` · `{"startAt":"<Instant>","status":"<Estado>"}` (mismo método `informative`) | `404` `{"error":"Token inexistente o inválido"}`; `410` `{"error":"Token vencido"}` | Igual que el GET de confirm; sin delegación a servicios de dominio | `PublicAppointmentTokenController.java:29-33, 35-39`; `AppointmentTokenService.java:22-33`; handler `:15-23` |
| `POST` | `/api/public/appointments/{token}/confirm` | `200` · `{"message":"confirmación exitosa","status":"CONFIRMED"}`; idempotente `200` · `{"message":"ya confirmado","status":"CONFIRMED"}` | `409` `{"error":"El turno no puede confirmarse"}`; `409` `{"error":"No se pudo confirmar el turno"}`; `404` token inválido; `410` token vencido | Estado `CANCELLED`/`ABSENT`/`ATTENDED` → 409 directo; `IllegalStateException` de `AppointmentService.changeStatus` → 409 genérico; token resuelto primero en todos los casos | `PublicAppointmentTokenController.java:41-63`; handler `PublicAppointmentTokenExceptionHandler.java:15-23, 37-41`; `AppointmentService.java:83-100, 115-123` |
| `POST` | `/api/public/appointments/{token}/cancel` | `200` · `{"message":"cancelación exitosa","status":"CANCELLED"}`; idempotente `200` · `{"message":"ya cancelado","status":"CANCELLED"}` | `409` `{"error":"Debe contactarse al consultorio para cancelar el turno"}`; `409` `{"error":"No se pudo cancelar el turno"}`; `404` token inválido; `410` token vencido | `CancellationWindowClosedException` si `startAt < now + 24h` y actor `PATIENT`; otra `IllegalStateException` al cancellar; token resuelto primero | `PublicAppointmentTokenController.java:65-83`; `AppointmentService.java:66-81`; handler `PublicAppointmentTokenExceptionHandler.java:15-35` |

### 3.2 Detalle de 409 y 422 en la reserva

| Estado | Body exacto | Regla | Origen de la excepción | Mapeo HTTP |
|---|---|---|---|---|
| `409` | `{"error":"La franja solicitada ya está ocupada"}` | `existsByDentistIdAndActiveSlot(dentistId, startAt)` o violación de unicidad de `active_slot` al guardar | `AppointmentService.java:47-55` → `AppointmentConflictException.java:3-6` | `PublicBookingExceptionHandler.java:47-51` |
| `409` (token) | `{"error":"El turno no puede confirmarse"}` | Estado del turno `CANCELLED`, `ABSENT` o `ATTENDED` | `PublicAppointmentTokenController.java:49-53` | Directo en el controlador |
| `409` (token) | `{"error":"No se pudo confirmar el turno"}` / `{"error":"No se pudo cancelar el turno"}` | Transición de estado rechazada por la máquina de estados | `PublicAppointmentTokenController.java:55-61, 75-77`; `AppointmentService.java:115-123` | `PublicAppointmentTokenExceptionHandler.java:31-41` |
| `409` (token) | `{"error":"Debe contactarse al consultorio para cancelar el turno"}` | Cancelación con menos de 24 hs de antelación | `AppointmentService.java:75-78` → `CancellationWindowClosedException.java:3-7` | `PublicAppointmentTokenExceptionHandler.java:25-29` |
| `422` | `{"error":"La fecha solicitada está fuera de la ventana pública"}` | `date == null` / rango diario vacío en `GET`; `startAt == null` o fuera de `[now+24h, now+30d]` en `POST` | `PublicBookingService.java:53, 58, 97-104`; `PublicBookingWindow.java:35-42` | `PublicBookingExceptionHandler.java:23-27` |
| `422` | `{"error":"La franja solicitada no está disponible"}` | `availability.isOpenSlot(...)` es falso: fuera de grilla, fuera de horario o bloqueado por `ScheduleBlock` | `PublicBookingService.java:70-72`; `AvailabilityService.java:72-84` | `PublicBookingExceptionHandler.java:35-39` |
| `422` | `{"error":"Datos de reserva inválidos"}` | Bean Validation (`nombre`/`apellido` `@NotBlank @Size(max=100)`, `startAt` `@NotNull`), JSON ilegible, parámetro faltante o type mismatch; DNI/email inválidos vía `PatientService.upsert` | `PublicBookingRequest.java:9-13`; `PatientService.java:22-39, 54-60`; `Patient.java:45-57` | `PublicBookingExceptionHandler.java:41-45, 59-64` |

Notas de comportamiento observado:

- El upsert y la creación del turno ocurren **después** de las validaciones de ventana y franja (`PublicBookingService.java:66-80`); la anotación `@Transactional` está en `book()` (`:66`), no en `slots()`.
- El token plano no se devuelve en la respuesta de reserva: solo se usa para el `OutboxMessage` (`PublicBookingService.java:76-89`; `PublicBookingResponse.java:5`). Los links del body son relativos, sin host (`:87-88`).
- `AppointmentTokenService.issue` genera 32 bytes en base64url sin padding (43 caracteres), persiste solo SHA-256 y calcula `expiresAt = min(startAt, now+7d)` (`AppointmentTokenService.java:35-57`; `AppointmentToken.java:20-24, 39-44`).
- Precedencia de advices: los dos handlers públicos llevan `@Order(Ordered.HIGHEST_PRECEDENCE)` (`PublicBookingExceptionHandler.java:19-21`, `PublicAppointmentTokenExceptionHandler.java:12-14`). Existen además `PatientExceptionHandler.java:12-21` (global, `ProblemDetail` 400) y `AppointmentExceptionHandler.java:13-33` (`basePackages = "com.turnosodontologia.appointment"`): los tests HTTP confirman que el POST responde 422 con `{"error":...}` y no 400/`ProblemDetail` (`PublicBookingApiTests.java:200-213, 301-317`).

---

## 4. Estado actual del frontend y cómo se sirve

### 4.1 Inventario de `frontend/`

| Archivo | Contenido observado |
|---|---|
| `frontend/index.html` | 5 líneas; `<div id="app"></div>` y `<script type="module" src="/main.js">` (`:1-5`). Título "Reserva de turnos". |
| `frontend/admin.html` | 5 líneas; `<h1>Administración</h1>` y el mismo script `/main.js` (`:1-5`). |
| `frontend/src/main.ts` | 5 líneas: selecciona `#app` y asigna el texto `"Turnos odontología"` (`:1-5`). No hay lógica de slots, formulario, fetch ni errores. |
| `frontend/tsconfig.json` | `target` y `module` ES2020, `strict: true`, `rootDir: "src"`, `outDir: "dist"`, `include: ["src/**/*.ts"]` (`:1-10`). |
| `frontend/package.json` | Único script `build`: `tsc -p tsconfig.json && node -e "...copyFileSync('index.html','dist/index.html'); ...copyFileSync('admin.html','dist/admin.html')"` (`:4-6`); devDependency `typescript ^5.9.3` (`:7-9`). No hay script `dev`/servidor propio. |
| `frontend/package-lock.json` | Presente (lockfile de npm). |
| `frontend/dist/` | No existe en el árbol de trabajo. |

No hay archivos under `frontend/src/admin/`, `frontend/src/public-reservation/` ni `frontend/src/shared/`: la estructura propuesta en `knowledge-base/08_arquitectura_propuesta.md:36-41` no está materializada. Tampoco hay referencias en `frontend/` a `/api/public/slots`, `/api/public/appointments` ni a "ya tengo un turno".

### 4.2 Cómo se compila y se sirve

- **Compilación:** `npm run build` en `frontend/` (`frontend/package.json:5`); documentado en `README.md:129-141` junto con `npm.cmd install`. TypeScript se compila a `frontend/dist/main.js` y las dos HTML se copian a `frontend/dist/` (`frontend/tsconfig.json:6-9`, `frontend/package.json:5`).
- **Sin tarea Gradle de frontend:** `backend/build.gradle:1-36` declara Spring Boot 3.3.13, Java 17 y starters `web`, `data-jpa`, `validation`, `security`, H2/PostgreSQL de test; no hay plugin ni task que invoque npm.
- **Recursos estáticos del backend:** `backend/src/main/resources/static/index.html`, `static/admin.html` y `static/js/main.js` existen y son páginas mínimas (`:1-5` cada una; `main.js` solo hace `console.log('turnos-odontologia')`).
- **Configuración de recursos:** `spring.web.resources.static-locations: classpath:/static/,file:../frontend/dist/` en `backend/src/main/resources/application-local.yml:6-8` y `application-test.yml:7-9`. No aparece en `application.yml:1-26`.
- **No hay `WebMvcConfigurer`, `addResourceHandlers`, controlador con `forward:`/`redirect:` ni controlador que sirva `index.html`** en `backend/src/main` (búsqueda exhaustiva). El `/` se resuelve por el mecanismo de welcome page por defecto de Spring Boot sobre las static locations.
- **Seguridad:** `/`, `/index.html`, `/admin.html`, `/js/**`, `/error`, `/api/public/**`, `/api/auth/login` y `/api/health` están `permitAll` (`AuthSecurityConfiguration.java:60-66`); el resto exige autenticación. CSRF está deshabilitado globalmente (`:52-54`).
- En el orden configurado, `classpath:/static/` precede a `file:../frontend/dist/`, por lo que `static/index.html`, `static/admin.html` y `static/js/main.js` pueden resolver antes que los artefactos compilados con las mismas rutas relativas al root.

---

## 5. Mapeo spec → implementación

### 5.1 `public-booking`

Ubicaciones: spec main `openspec/specs/public-booking/spec.md:1-90` (3 requirements) y delta archivado `openspec/changes/archive/2026-11-23-public-booking-api/specs/public-booking/spec.md:1-90` (`## ADDED Requirements` en `:7`). El delta y la spec main contienen el mismo conjunto de 15 escenarios; la main reemplaza el encabezado de delta por `## Purpose`/`## Requirements`.

| Requisito / escenario (spec main) | Implementación observada | Evidencia de test |
|---|---|---|
| Consultar disponibilidad pública (`:9-10`) | `PublicBookingService.slots` acota el día con `max/min` y usa `AvailabilityService.freeSlots` (`:52-64`); `ZoneId` del `application.yml:3` | — |
| Fecha parcialmente dentro de la ventana (`:12-15`) | `PublicBookingService.java:55-62` | `PublicBookingApiTests.java:110-122` |
| Hoy queda fuera de la ventana (`:17-19`) | `PublicBookingService.java:58` + `PublicBookingExceptionHandler.java:23-27` | `PublicBookingApiTests.java:124-129` |
| Último día parcialmente dentro (`:21-24`) | `PublicBookingService.java:57` | `PublicBookingApiTests.java:140-153` |
| Fecha completa fuera de la ventana (`:26-28`) | `PublicBookingService.java:58` | `PublicBookingApiTests.java:132-138` |
| Date faltante o sin parsear (`:30-32`) | `PublicBookingExceptionHandler.java:59-64` | `PublicBookingApiTests.java:187-198` |
| No hay slots libres (`:34-36`) | `AvailabilityService.java:39-69`; respuesta `slots: []` | `PublicBookingApiTests.java:155-161` |
| Reservar un turno público (`:38-39`) | `PublicBookingService.book` (`:66-80`) y `minimalResponse` (`:82-84`) | `PublicBookingApiTests.java:245-258` |
| Reserva exitosa (`:41-46`) | `PublicBookingController.java:30-33`; `PublicBookingResponse.java:5` | `PublicBookingApiTests.java:245-283` (incluye ausencia de `token`, `dentistId`, `patientId`, `activeSlot`, `id`) |
| Franja reenviada desde disponibilidad (`:48-50`) | `AvailabilityService.isOpenSlot` (`:72-84`); reutilización por DNI en `PatientService.upsert:22-39` | `PublicBookingApiTests.java:291-299` |
| StartAt fuera de la grilla o bloqueado (`:52-55`) | `PublicBookingService.java:70-72` | `PublicBookingApiTests.java:227-241` |
| Solapamiento (`:57-60`) | `AppointmentService.create:47-55` + handler `:47-51`; rollback de la transacción en `PublicBookingService.java:66` | `PublicBookingApiTests.java:319-334, 338-367` |
| Validación inválida (`:62-65`) | `PublicBookingRequest.java:9-13`, `PatientService.java:22-39`, handler `:41-64` | `PublicBookingApiTests.java:200-213, 301-317` |
| Ventana inválida en el POST (`:67-70`) | `PublicBookingService.java:97-104` | `PublicBookingApiTests.java:215-225` |
| Odontólogo público no disponible (`:72-75`) | `PublicBookingService.publicDentist:91-95` + handler `:29-33` | `PublicBookingApiTests.java:163-183` |
| Integrar emisión de token por C-09 (`:77-78`) | `PublicBookingService.java:76` delega en `AppointmentTokenService.issue:35-57` | — |
| Token asociado al turno creado (`:80-84`) | `AppointmentTokenService.java:39-56`; `AppointmentToken.java:16-24` | `PublicBookingApiTests.java:268-282` (token de 43 caracteres extraído del body) |
| Bandeja de salida de reserva (`:86-90`) | `PublicBookingService.java:77-79, 86-89` | `PublicBookingApiTests.java:272-282` |

### 5.2 `token-confirmation`

Ubicaciones: spec main `openspec/specs/token-confirmation/spec.md:1-99` (4 requirements, 16 escenarios) y delta archivado `openspec/changes/archive/2026-11-23-token-confirmation/specs/token-confirmation/spec.md:1-95` (`## ADDED Requirements` en `:3`). El conjunto de nombres de escenarios coincide entre ambos; el delta está identificado como "# Token confirmation".

| Requisito / escenario (spec main) | Implementación observada | Evidencia de test |
|---|---|---|
| Validar token reutilizable antes del estado (`:9-10`) | `AppointmentTokenService.resolve:22-33` (formato 43 chars, hash SHA-256, `now < expiresAt`) | `PublicAppointmentTokenHttpTest.java:30-38, 87-100` |
| Token válido confirmado una sola vez (`:12-15`) | `PublicAppointmentTokenController.confirm:41-63` | `PublicAppointmentTokenHttpTest.java:102-119` |
| Segunda confirmación sin efectos (`:17-20`) | `PublicAppointmentTokenController.java:46-48` | `PublicAppointmentTokenHttpTest.java:121-134` |
| Token vencido con precedencia sobre CONFIRMED (`:22-25`) | `AppointmentTokenService.java:29-31` se ejecuta antes del controlador | `PublicAppointmentTokenHttpTest.java:244-251` |
| Token vencido por turno pasado (`:27-30`) | `AppointmentTokenService.java:45-47` (`expiresAt = min(startAt, now+7d)`) | `PublicAppointmentTokenHttpTest.java:87-100` (vencimiento simulado) |
| Token inexistente (`:32-35`) | `PublicTokenInvalidException` → 404 | `PublicAppointmentTokenHttpTest.java:30-38` |
| Cancelar por servicio centralizado (`:37-38`) | `PublicAppointmentTokenController.java:69-77` → `AppointmentService.cancel:66-81` | `PublicAppointmentTokenHttpTest.java:168-185` |
| Cancelación de turno confirmado (`:40-43`) | `PublicAppointmentTokenController.java:65-83` | `PublicAppointmentTokenHttpTest.java:168-185` |
| Menos de 24 horas (`:45-48`) | `AppointmentService.java:75-78` + handler token `:25-29` | `PublicAppointmentTokenHttpTest.java:187-205` |
| Doble cancelación idempotente (`:50-53`) | `AppointmentService.java:71-73`; controller `:79-82` | `PublicAppointmentTokenHttpTest.java:225-242` |
| Repetición cancelada fuera de plazo (`:55-58`) | `AppointmentService.java:71-73` evalúa `CANCELLED` antes de los 24 h | `PublicAppointmentTokenHttpTest.java:225-242` (resultado simulado) |
| Proteger confirmaciones imposibles y GET informativo (`:60-61`) | `PublicAppointmentTokenController.java:23-39, 49-53` | `PublicAppointmentTokenHttpTest.java:58-85, 136-166` |
| Turno cancelado no se puede confirmar (`:63-66`) | `PublicAppointmentTokenController.java:49-53` → 409 | `PublicAppointmentTokenHttpTest.java:136-149` |
| GET confirm solo informa (`:68-71`) | `PublicAppointmentTokenController.java:23-27, 35-39` | `PublicAppointmentTokenHttpTest.java:58-85` |
| GET cancel solo informa (`:73-76`) | `PublicAppointmentTokenController.java:29-33, 35-39` | `PublicAppointmentTokenHttpTest.java:58-85` |
| C-09 emite y C-07 consume (`:78-79`) | `AppointmentTokenService.issue:35-57` + `PublicBookingService.java:76` | `PublicBookingApiTests.java:268-282` |
| Persiste solo hash / devuelve plano (`:81-84`) | `AppointmentTokenService.java:39-43`; `AppointmentToken.java:20-24, 39-44` | `PublicBookingApiTests.java:268-270, 278-279` |
| Reemitir reemplaza el token (`:86-89`) | `AppointmentTokenService.java:49-55` | No identificado en los tests leídos |
| Vencimiento `min(startAt, now+7d)` (`:91-94`) | `AppointmentTokenService.java:44-47` | No identificado en los tests leídos |
| Búsqueda por hash (`:96-99`) | `AppointmentTokenService.java:27-28` | `PublicAppointmentTokenHttpTest.java:30-38` (mock) |

---

## 6. Observaciones / gaps (solo hallazgos)

1. `CHANGES.md:269` marca C-08 pendiente y `CHANGES.md:252` marca C-07 pendiente, pero en disco C-07 tiene implementación, tests HTTP y change archivado (`openspec/changes/archive/2026-11-23-public-booking-api/`). No hay change activo ni directorio `openspec/changes/<C-08>`.
2. No existe carpeta `design-system/` en la raíz del repositorio; `design-system/README.md` tampoco está presente.
3. La KB describe `GET /confirm` y `GET /cancel` como páginas que "muestran información y las acciones disponibles" (`knowledge-base/07_flujos_principales.md:38`, `:46`) y el scope de C-08 habla de "abre la página GET" (`CHANGES.md:275`), mientras que los controladores actuales devuelven exclusivamente JSON (`PublicAppointmentTokenController.java:23-39`).
4. No se encontró en `frontend/` ni en `backend/src/main/resources/static/` ningún enlace o texto "ya tengo un turno" ni un formulario que envíe `POST` a los endpoints de token (búsqueda por contenido).
5. La respuesta de reserva no incluye token ni identificadores (`PublicBookingResponse.java:5`); el token plano solo aparece en el `OutboxMessage` (`PublicBookingService.java:86-89`).
6. Los links del `OutboxMessage` son relativos (`/api/public/appointments/{token}/...`, `PublicBookingService.java:87-88`); `APP_PUBLIC_URL` aparece documentado en `README.md:95,106` y en `knowledge-base/08_arquitectura_propuesta.md:68`, pero no hay lectura de esa variable en el código Java relevante (búsqueda por `APP_PUBLIC_URL|public-url|publicUrl`).
7. `knowledge-base/07_flujos_principales.md:74` indica "422 con campo a corregir" para DNI inválido; el body observado es `{"error":"Datos de reserva inválidos"}`, sin identificar el campo (`PublicBookingExceptionHandler.java:41-45, 59-64`).
8. `PatientExceptionHandler.java:12-21` es un `@RestControllerAdvice` global que devuelve `ProblemDetail` con 400; los handlers públicos usan `@Order(HIGHEST_PRECEDENCE)` y bodies `Map`, con Test HTTP que fija 422 (`PublicBookingApiTests.java:200-213`). `AppointmentExceptionHandler.java:13` además aplica a todo el paquete `appointment`, incluidos los controladores públicos.
9. `spring.web.resources.static-locations` está declarado solo en los perfiles `local` y `test` (`application-local.yml:6-8`, `application-test.yml:7-9`); `application.yml` no lo declara. La carpeta `frontend/dist/` no existe actualmente.
10. `frontend/index.html:4` carga `/main.js` y `backend/src/main/resources/static/index.html:4` carga `/js/main.js`; no hay step de build que copie el resultado de TypeScript dentro de `static/js/`.
11. El rate limit figura explícitamente como no implementado en v1 en el scope de C-07 (`CHANGES.md:258`) y en el design archivado (`.../public-booking-api/design.md:24`).
12. No se ejecutaron comandos de build ni tests durante este relevamiento; las afirmaciones sobre respuestas HTTP se basan en código fuente y en los tests leídos (`PublicBookingApiTests.java`, `PublicAppointmentTokenHttpTest.java`). No se modificó ningún otro archivo ni se realizaron operaciones de git.

**No se proponen ni implementan cambios.**
