# CHANGES — Secuencia de Implementación

> Índice canónico de todos los changes del proyecto **turnos-odontologia**.
> Cada change es atómico: un agente puede implementarlo en una sesión (~4-6 horas).
> **Leer este archivo antes de empezar un item.**

---

## Cómo usar este documento

1. Identificar el item a implementar y verificar que sus dependencias figuran completas en este roadmap.
2. Leer los docs de la knowledge-base indicados en "Leer antes".
3. Acordar el alcance y los criterios de aceptación del item.
4. Implementar, verificar los criterios acordados y registrar la evidencia.
5. Marcar el checkbox `[x]` en este archivo al completar la verificación técnica.

Conservá los IDs de las fichas al editar o reordenar. Si el proyecto usa ClickUp,
este roadmap conserva el alcance y las dependencias. Consultá el responsable y el estado operativo
en ClickUp y el último dato confirmado en `.active-stack/roadmap-state.json`.

**Plazo:** entrega 2026-11-24. El camino crítico son 9 changes. Si el tiempo aprieta,
el primer recorte es la automatización de la lista de espera (C-11), que puede quedar
como aviso manual documentado sin romper el resto del MVP.

---

## Árbol de dependencias

```
C-01 foundation-setup
  └── C-02 core-models
        ├── C-03 dentist-auth
        │     └── C-05 appointments-core
        ├── C-04 schedule-rules
        │     └── C-05 appointments-core
        └── C-06 patient-records

C-05 appointments-core
  └── C-09 token-confirmation
        ├── C-07 public-booking-api
        │     └── C-08 public-booking-ui
        └── C-10 outbox-and-reminders
              └── C-11 waiting-list

> C-07 también requiere C-06 (registro de pacientes), además de C-09.

C-03 + C-05 + C-06 + C-10 + C-11
  ├── C-12 admin-agenda-ui
  └── C-13 seed-and-demo

C-01 … C-13
  └── C-14 test-suite-and-hardening
```

### Paralelismo por fase

> Cada "gate" es un punto de sincronización. Los changes dentro de un grupo pueden ejecutarse en paralelo.

```
GATE 0: ninguna
  → C-01 foundation-setup            [Persona A]

GATE 1: C-01 ✓
  → C-02 core-models                 [Persona A]

GATE 2: C-02 ✓                     ← PRIMER FORK (3 paralelos)
  → C-03 dentist-auth                [Persona B]
  → C-04 schedule-rules              [Persona C]
  → C-06 patient-records             [Persona E]

GATE 3: C-03 + C-04 ✓
  → C-05 appointments-core           [Persona B]

GATE 4: C-05 ✓
  → C-09 token-confirmation          [Persona D]

GATE 5: C-09 ✓ + C-06 ✓
  → C-07 public-booking-api          [Persona B]
  → C-10 outbox-and-reminders        [Persona D]

GATE 6: C-07 ✓
  → C-08 public-booking-ui           [Persona E]

GATE 7: C-10 ✓
  → C-11 waiting-list                [Persona C]

GATE 8: C-05 + C-06 + C-10 + C-11 ✓
  → C-12 admin-agenda-ui             [Persona E]
  → C-13 seed-and-demo               [Persona C]

GATE 9: C-01 … C-13 ✓
  → C-14 test-suite-and-hardening    [Persona A]
```

### Camino crítico (9 changes — mínimo irreducible)

```
C-01 → C-02 → C-03 → C-05 → C-09 → C-10 → C-11 → C-12 → C-14
```

### Plan óptimo con 5 personas

```
Paso │ Persona A (Cimientos/Cierre) │ Persona B (Turnos)        │ Persona C (Agenda/Espera) │ Persona D (Token/Mail)      │ Persona E (Frontend)
─────┼──────────────────────────────┼───────────────────────────┼───────────────────────────┼─────────────────────────────┼──────────────────────────
  1  │ C-01 foundation-setup        │ —                         │ —                         │ —                           │ —
  2  │ C-02 core-models             │ —                         │ —                         │ —                           │ —
  3  │ —                            │ C-03 dentist-auth         │ C-04 schedule-rules       │ —                           │ C-06 patient-records
  4  │ —                            │ C-05 appointments-core    │ —                         │ —                           │ —
  5  │ —                            │ —                         │ —                         │ C-09 token-confirmation     │ —
  6  │ —                            │ C-07 public-booking-api   │ —                         │ C-10 outbox-and-reminders   │ —
  7  │ —                            │ —                         │ C-11 waiting-list         │ —                           │ C-08 public-booking-ui
  8  │ —                            │ —                         │ C-13 seed-and-demo        │ —                           │ C-12 admin-agenda-ui
  9  │ C-14 test-suite-and-hardening│ —                         │ —                         │ —                           │ —
```

---

## FASE 0 — Cimientos

### [C-01] `foundation-setup`
- **Estado**: `[x]` completo
- **Scope**: Scaffolding del monorepo y configuración base
  - Estructura: `backend/`, `frontend/`, `database/`, `knowledge-base/`
  - `backend/`: Spring Boot 3.x con Gradle Wrapper, toolchain Java 17, dependencias `web`, `data-jpa`, `validation`, `security`, `h2`, `postgresql`, `test`
  - Perfiles `application-local.yml` (PostgreSQL) y `application-test.yml` (H2 en modo PostgreSQL)
  - `GET /api/health` público
  - `frontend/`: `tsconfig.json`, compilación TypeScript vanilla, `index.html` público y `admin.html`
  - `.env.example` con las variables de `08_arquitectura_propuesta.md`
  - README con comandos de arranque y ejecución de tests
  - Tests: el contexto Spring carga con perfil `test`
- **Dependencias**: ninguna
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/01_vision_y_objetivos.md` §Alcance v1
  - `knowledge-base/02_descripcion_general.md` §Stack tecnológico
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios
  - `knowledge-base/08_arquitectura_propuesta.md` §Perfiles

---

## FASE 1 — Núcleo de dominio y acceso

### [C-02] `core-models`
- **Estado**: `[x]` completo
- **Scope**: Entidades JPA, enums, repositorios y esquema inicial
  - Modelos: `Dentist`, `Patient`, `Appointment`, `AppointmentToken`, `AppointmentEvent`, `WaitingListEntry`, `OutboxMessage`, `ScheduleBlock`
  - Enums: `AppointmentStatus`, `CreatedBy`, `Actor`, `WaitingListStatus`, `OutboxStatus`
  - `Appointment.dentist_id` FK a `Dentist`
  - `Appointment.active_slot` nullable y único: igual a `start_at` mientras el estado no sea `CANCELLED`; `NULL` al cancelar; lo mantiene el servicio, nunca se carga a mano
  - `ScheduleBlock` para bloqueo manual de día o franja por el odontólogo
  - Campos de auditoría `created_at`, `updated_at`, `deleted_at` (soft delete)
  - Repositorios Spring Data por entidad
  - Esquema inicial y creación del `Dentist` base desde variables de entorno
  - Tests: mapeo JPA, `active_slot` único, dos turnos activos en la misma franja fallan, se puede reservar de nuevo una franja cuyo turno fue cancelado, paridad H2/PostgreSQL, `ScheduleBlock`
- **Dependencias**: C-01
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §ERD
  - `knowledge-base/04_modelo_de_datos.md` §Entidades
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones aplicados
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-05

### [C-03] `dentist-auth`
- **Estado**: `[x]` completo
- **Scope**: Autenticación del odontólogo con sesión
  - `POST /api/auth/login` con email y contraseña, validación BCrypt
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
  - Sesión en cookie HttpOnly con expiración por 30 minutos de inactividad
  - `SecurityFilterChain`: `/api/public/**` y páginas públicas abiertas; el resto exige autenticación
  - Creación del `Dentist` inicial desde `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`
  - Tests: login correcto, credenciales inválidas, ruta protegida 401, expiración de sesión
- **Dependencias**: C-02
- **Governance**: CRITICO
- **Leer antes**:
  - `knowledge-base/03_actores_y_roles.md` §Autenticación
  - `knowledge-base/04_modelo_de_datos.md` §Dentist
  - `knowledge-base/07_flujos_principales.md` §Flujo 1
  - `knowledge-base/08_arquitectura_propuesta.md` §Seguridad

---

## FASE 2 — Agenda y turnos

### [C-04] `schedule-rules`
- **Estado**: `[x]` completo
- **Handoff de integración**:
  - Aplicar `database/schedule-rules.sql` en la base PostgreSQL administrada por el líder antes de arrancar el perfil `local`.
  - Verificar `ddl-auto: validate`, el arranque completo y la paridad H2/PostgreSQL; registrar la evidencia antes de marcar C-04 como cerrado.
  - Procedimiento y criterios: `database/README.md` §`Schedule rules (C-04)`.
  - Este entorno de desarrollo no pudo ejecutar `psql`/`pg_isready` ni comprobar una instancia PostgreSQL; no aprobar ni archivar este change hasta completar esas comprobaciones.
- **Scope**: Disponibilidad, franjas y ventana pública
  - Configuración de horario: lunes a viernes, 9:00-13:00 y 14:00-19:00, timezone `America/Argentina/Buenos_Aires`
  - Generación de slots de 30 minutos
  - Persistencia de `ScheduleBlock` para bloqueos manuales
  - `GET/PATCH /api/schedule` para configuración y `POST/DELETE /api/schedule/blocks` para bloqueos
  - `GET /api/schedule/availability?from=&to=`
  - Cálculo de ventana pública desde 24hs hasta 30 días
  - Tests: límites de horario, pausa de almuerzo, fines de semana, timezone, bloqueo que excluye slots
- **Dependencias**: C-02
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Appointment
  - `knowledge-base/05_reglas_de_negocio.md` §Reserva
  - `knowledge-base/07_flujos_principales.md` §Flujo 2
  - `knowledge-base/10_preguntas_abiertas.md` §POA-01 / §POA-02

### [C-05] `appointments-core`
- **Estado**: `[x]` completo
- **Scope**: Creación de turnos, estados y agenda del odontólogo
  - Servicio de creación de `Appointment` con paciente, inicio y duración fija de 30 minutos
  - Mantener `active_slot` al crear (`start_at`) y al cancelar (`NULL`)
  - Prevención de solapamiento en transacción, unique de `active_slot` y respuesta 409
  - Máquina de estados `PENDING → CONFIRMED | CANCELLED | ABSENT | ATTENDED` con validación de transiciones
  - Registro de `AppointmentEvent` en cada cambio de estado
  - `GET /api/appointments?from=&to=` para agenda diaria y semanal
  - `AppointmentService.cancel(appointmentId, actor, reason)` como único punto de cancelación: valida la transición y la regla de 24hs si el actor es `PATIENT`, pone `CANCELLED`, deja `active_slot` en `NULL`, registra `AppointmentEvent` y publica `AppointmentCancelledEvent` (`appointmentId`, `startAt`, `actor`) solo si la transición ocurrió
  - `PATCH /api/appointments/{id}/status` invoca ese servicio; nunca cambia el estado directamente
  - Una segunda cancelación responde "ya cancelado" y no publica evento
  - Tests: dos turnos activos en la misma franja fallan con 409; se puede reservar de nuevo una franja cancelada; paridad H2/PostgreSQL; transiciones inválidas; evento publicado una vez al cancelar y ninguna al repetir
- **Dependencias**: C-02, C-03, C-04
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Appointment
  - `knowledge-base/05_reglas_de_negocio.md` §Estados / §Cancelación
  - `knowledge-base/07_flujos_principales.md` §Flujo 5
  - `knowledge-base/08_arquitectura_propuesta.md` §Patrones aplicados / §Eventos de dominio
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-07

### [C-06] `patient-records`
- **Estado**: `[x]` completo
- **Scope**: Alta y reutilización de pacientes
  - Normalización de DNI: quitar puntos y espacios, aceptar solo 7 u 8 dígitos, rechazar letras
  - Validación de email con formato estándar
  - Upsert de `Patient` por DNI
  - `GET /api/patients` y `POST /api/patients` para el odontólogo
  - Búsqueda por DNI
  - Tests: DNI válido e inválido, email inválido, reutilización por DNI
- **Dependencias**: C-02
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Patient
  - `knowledge-base/05_reglas_de_negocio.md` §RN-RS-05 / §RN-RS-06
  - `knowledge-base/10_preguntas_abiertas.md` §Integridad de datos

---

## FASE 3 — Reserva pública

### [C-07] `public-booking-api`
- **Estado**: `[x]` completo
- **Scope**: API pública de disponibilidad y reserva
  - `GET /api/public/slots?date=` con ventana 24hs-30 días
  - `POST /api/public/appointments` con nombre, apellido, DNI y email
  - Upsert de `Patient` + `Appointment` en `PENDING`, usando el componente de C-09 para generar el token opaco de 32 bytes en base64url, persistir únicamente su hash SHA-256 y permitir su búsqueda por hash; crear también el `OutboxMessage`
  - Respuesta 409 ante solapamiento y 422 ante validación
  - Rate limit no implementado en v1 (documentado)
  - Tests: reserva feliz, solapamiento 409, ventana inválida, DNI inválido
- **Dependencias**: C-05, C-06, C-09
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 2
  - `knowledge-base/05_reglas_de_negocio.md` §Reserva
  - `knowledge-base/04_modelo_de_datos.md` §AppointmentToken
  - `knowledge-base/10_preguntas_abiertas.md` §Seguridad

### [C-08] `public-booking-ui`
- **Estado**: `[x]` completo
- **Scope**: Interfaz pública de reserva en TypeScript vanilla
  - Selector de fecha dentro de la ventana permitida
  - Listado de slots libres y formulario de datos del paciente
  - Confirmación del turno creado
  - Manejo visible de errores 409 y 422
  - Enlace "ya tengo un turno" que abre la página GET y ejecuta la acción con POST
  - Verificación manual documentada; sin framework
- **Dependencias**: C-07
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/07_flujos_principales.md` §Flujo 2
  - `knowledge-base/06_funcionalidades.md` §US-020 / §US-021
  - `knowledge-base/02_descripcion_general.md` §API REST
  - `knowledge-base/08_arquitectura_propuesta.md` §Estructura de directorios

---

## FASE 4 — Validación por mail y lista de espera

### [C-09] `token-confirmation`
- **Estado**: `[x]` completo
- **Scope**: Confirmación y cancelación por token reutilizable e idempotente
  - Componente reutilizable para generar un token opaco de 32 bytes en base64url, persistir solo su hash SHA-256 —nunca el token plano— y buscar tokens por hash
  - Vencimiento al inicio del turno o a los 7 días, lo que ocurra primero
  - El token no se consume al confirmar: un único token por turno sirve para confirmar y cancelar
  - `GET /api/public/appointments/{token}/confirm` solo muestra la página; `POST .../confirm` cambia `PENDING → CONFIRMED`
  - `GET /api/public/appointments/{token}/cancel` solo muestra la página; `POST .../cancel` cancela
  - Toda cancelación invoca `AppointmentService.cancel(appointmentId, PATIENT, reason)`; no cambia el estado directamente ni publica eventos por su cuenta
  - Confirmar idempotente: si ya está `CONFIRMED`, responde "ya confirmado" sin repetir efectos
  - Cancelar idempotente: si ya está `CANCELLED`, responde "ya cancelado" sin repetir efectos ni avisos
  - Cancelación permitida hasta 24hs antes, incluso si el turno está `CONFIRMED`; con menos de 24hs se rechaza y se deriva al odontólogo
  - Un turno `CANCELLED` no se puede confirmar
  - Tests: token válido, vencido, confirmar dos veces, cancelar después de confirmar, cancelar dos veces, cancelar con menos de 24hs, turno `CANCELLED` no confirma, turno pasado
- **Dependencias**: C-05
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §AppointmentToken
  - `knowledge-base/05_reglas_de_negocio.md` §Confirmación
  - `knowledge-base/07_flujos_principales.md` §Flujo 3
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-07
  - `knowledge-base/10_preguntas_abiertas.md` §Seguridad

### [C-10] `outbox-and-reminders`
- **Estado**: `[x]` completo
- **Scope**: Bandeja de salida simulada y recordatorios
  - Persistencia de `OutboxMessage` con estados `PENDING` y `SIMULATED_SENT`
  - `GET /api/outbox/messages` para listado del odontólogo
  - Generación de recordatorio 24-48hs antes del turno con link tokenizado
  - Disparador local de recordatorios para la demo (scheduler o endpoint)
  - Tests: creación del mensaje, link correcto, no duplicación
- **Dependencias**: C-05, C-09
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §OutboxMessage
  - `knowledge-base/07_flujos_principales.md` §Flujo 3
  - `knowledge-base/05_reglas_de_negocio.md` §RN-CF-05
  - `knowledge-base/10_preguntas_abiertas.md` §POA-04

### [C-11] `waiting-list`
- **Estado**: `[x]` completo
- **Scope**: Lista de espera FIFO con aviso al liberar horario
  - Entidad `WaitingListEntry` ordenada por `created_at`
  - `POST /api/waiting-list` y `GET /api/waiting-list`
  - Escucha `AppointmentCancelledEvent` con `@TransactionalEventListener(AFTER_COMMIT)` y `@Transactional(REQUIRES_NEW)`
  - No modifica clases de C-05; solo consume el evento
  - Al recibir el evento: tomar la primera entrada `WAITING`, marcarla `NOTIFIED` y crear `OutboxMessage`
  - Sin asignación ni confirmación automática del turno
  - Tests: orden FIFO, un solo aviso por evento, listener probado publicando el evento a mano, transición de estados
- **Dependencias**: C-05, C-10
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §WaitingListEntry
  - `knowledge-base/05_reglas_de_negocio.md` §Lista de espera
  - `knowledge-base/07_flujos_principales.md` §Flujo 4
  - `knowledge-base/08_arquitectura_propuesta.md` §Eventos de dominio
  - `knowledge-base/09_decisiones_y_supuestos.md` §DD-07 / §SU-03

---

## FASE 5 — Interfaces y demo

### [C-12] `admin-agenda-ui`
- **Estado**: `[x]` completado
- **Scope**: Interfaz administrativa del odontólogo
  - Pantalla de login con email y contraseña
  - Agenda diaria y semanal con estado de cada turno
  - Acciones: confirmar, marcar ausente, marcar atendido y cancelar
  - Listado de pacientes y detalle mínimo
  - Bandeja de salida simulada desde `GET /api/outbox/messages`, con links de prueba
  - Lista de espera visible
  - Manejo de sesión expirada y errores de API
- **Dependencias**: C-03, C-05, C-06, C-10, C-11
- **Governance**: MEDIO
- **Leer antes**:
  - `knowledge-base/06_funcionalidades.md` §Épica 1 / §Épica 3 / §Épica 5 / §Épica 6
  - `knowledge-base/07_flujos_principales.md` §Flujo 1 / §Flujo 5
  - `knowledge-base/03_actores_y_roles.md` §RBAC
  - `knowledge-base/02_descripcion_general.md` §API REST

### [C-13] `seed-and-demo`
- **Estado**: `[x]` completado
- **Scope**: Datos ficticios y arranque reproducible
  - Seed determinista relativo a la fecha de ejecución
  - 1 odontólogo ficticio, 5-10 pacientes, turnos en distintos estados
  - 3 entradas de lista de espera y mensajes de outbox
  - Script de arranque documentado en README
  - Solo datos ficticios; sin datos reales
  - Tests: seed idempotente
- **Dependencias**: C-02, C-03, C-05, C-10, C-11
- **Governance**: BAJO
- **Leer antes**:
  - `knowledge-base/04_modelo_de_datos.md` §Seed data inicial
  - `knowledge-base/06_funcionalidades.md` §US-050
  - `knowledge-base/10_preguntas_abiertas.md` §Seguridad
  - `knowledge-base/08_arquitectura_propuesta.md` §Perfiles

---

## FASE 6 — Calidad y cierre

### [C-14] `test-suite-and-hardening`
- **Estado**: `[x]` completado
- **Scope**: Suite de pruebas y endurecimiento final
  - JUnit 5 + H2 en modo PostgreSQL
  - Cobertura de reglas: solapamiento 409, ventana 24hs-30 días, DNI, token vencido, confirmación y cancelación idempotentes, cancelación con menos de 24hs, transiciones de estado y FIFO
  - Paridad H2/PostgreSQL para `active_slot` único y para la reserva posterior a una cancelación
  - Verificación de diferencias entre H2 y PostgreSQL
  - Revisión de respuestas 4xx/5xx sin filtrar detalles internos
  - Checklist de demo end-to-end desde la bandeja de salida
  - Tests: suite completa en verde con `gradlew test`
- **Dependencias**: C-01, C-02, C-03, C-04, C-05, C-06, C-07, C-08, C-09, C-10, C-11, C-12, C-13
- **Governance**: ALTO
- **Leer antes**:
  - `knowledge-base/05_reglas_de_negocio.md` (completo)
  - `knowledge-base/07_flujos_principales.md` §Casos de error
  - `knowledge-base/10_preguntas_abiertas.md` §Seguridad
  - `knowledge-base/06_funcionalidades.md` §US-051

---

## Resumen

| Fase | Changes | IDs |
|---|---|---|
| 0 — Cimientos | 1 | C-01 |
| 1 — Núcleo de dominio y acceso | 2 | C-02, C-03 |
| 2 — Agenda y turnos | 3 | C-04, C-05, C-06 |
| 3 — Reserva pública | 2 | C-07, C-08 |
| 4 — Validación por mail y lista de espera | 3 | C-09, C-10, C-11 |
| 5 — Interfaces y demo | 2 | C-12, C-13 |
| 6 — Calidad y cierre | 1 | C-14 |

**Total:** 14 changes.
**Camino crítico:** 9 changes.
**Primer item elegible:** `C-01` (`foundation-setup`).
