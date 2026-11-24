# Design

## Context

Véase `proposal.md` — Why. El diseño parte de este estado real:

- `schedule/` solo contiene `ScheduleBlock` (con `dentist`, `startAt`, `endAt`, `reason` y auditoría/soft delete heredados) y un repositorio vacío. No existe configuración de horario ni cálculo de slots.
- `Appointment` ya persiste `dentist`, `patient`, `startAt`, `durationMinutes` fijo en 30, `status`, `activeSlot` único nullable y `cancelledAt`. `AppointmentStatus` incluye `PENDING`, `CONFIRMED`, `CANCELLED`, `ABSENT` y `ATTENDED`.
- `AuditableEntity` aporta `createdAt`, `updatedAt`, `deletedAt` y `markDeleted(Instant)`. No se modifica.
- C-03 dejó sesión de servidor, `DentistPrincipal.id()` y `anyRequest().authenticated()`; `/api/schedule/**` ya queda protegido sin tocar la cadena. `JsonAuthenticationEntryPoint` responde 401 JSON.
- No hay `@RestControllerAdvice` global ni beans de `Clock`/`ZoneId`. El perfil `test` usa H2 en modo PostgreSQL con `create-drop`; `local` usa PostgreSQL con `ddl-auto: validate` y no hay Flyway, Liquibase ni `schema.sql`.
- `04_modelo_de_datos.md` declara que los slots no se persisten y que se calculan desde la configuración, los bloqueos y los turnos activos.

## Goals / Non-Goals

**Goals:**

- Una configuración semanal persistente por odontólogo, editable por reemplazo total, con defaults reproducibles.
- Un motor de slots determinista y barato, reutilizable por C-05 y C-07, sin persistir slots.
- Una política de ventana pública (24 horas a 30 días) aislada y expuesta a los clientes para no duplicar constantes.
- Bloqueos manuales con baja lógica y aislamiento por odontólogo.
- Verificación determinista en Windows: sin `Instant.now()`, sin `ZoneId.systemDefault()` y sin `Thread.sleep`.
- Un camino explícito para crear las tablas nuevas en `local` sin introducir un sistema de migraciones.

**Non-Goals:**

- Crear turnos, cambiar estados o implementar la garantía de doble reserva: C-05.
- Endpoints públicos `/api/public/**`, rate limiting o UI: C-07, C-08 y C-12.
- Feriados automáticos, duración variable, reprogramación, múltiples odontólogos por consultorio o múltiples sedes.
- Modificar `ScheduleBlock`, `Appointment`, `AuditableEntity`, la seguridad de C-03 o las versiones de Java/Spring Boot.
- Flyway, Liquibase o `schema.sql` administrado por Spring.

## Decisions

### D1 — `ScheduleConfiguration` persistente con franjas como `@ElementCollection`

Se agrega `schedule/ScheduleConfiguration` que extiende `AuditableEntity`:

- `id: UUID` con `GenerationType.UUID`.
- `dentist: Dentist` con `@OneToOne(optional = false)` y `@JoinColumn(name = "dentist_id", nullable = false, unique = true)`.
- `windows: List<ScheduleWindow>` como `@ElementCollection(fetch = FetchType.LAZY)` con `@CollectionTable(name = "schedule_windows", joinColumns = @JoinColumn(name = "schedule_configuration_id"))`.
- `ScheduleWindow` es un `@Embeddable` sin identidad propia: `dayOfWeek` como `@Enumerated(EnumType.STRING)` de `java.time.DayOfWeek`, `startTime` y `endTime` como `LocalTime`.
- La lista se ordena por día y hora de inicio al leerla; no se agrega `@OrderColumn` para no sumar una columna artificial al esquema manual.

La timezone **no** se duplica en la tabla: es una constante de despliegue en `app.schedule.timezone`, expuesta por la API. Así una fila no puede quedar con una zona distinta a la del cálculo y no hay dos fuentes de verdad.

Alternativas descartadas: hardcodear el horario sin persistencia (impide que el odontólogo lo edite), guardar las franjas como JSON en una columna (opaco, difícil de validar y de consultar) y crear una entidad `ScheduleWindow` con id propio (no tiene identidad ni ciclo de vida independiente).

### D2 — Siembra perezosa de la configuración por defecto

`app.schedule.default-windows` declara las franjas iniciales (lunes a viernes, 09:00–13:00 y 14:00–19:00). La primera lectura de la configuración de un odontólogo sin fila la crea dentro de una transacción; las lecturas siguientes reutilizan la fila.

`dentist_id` único es la defensa contra duplicados. Si dos primeras lecturas compiten, la perdedora reintenta una vez la búsqueda en una transacción nueva en lugar de propagar un error de integridad. La siembra es la única escritura que produce un `GET /api/schedule`, y solo ocurre la primera vez.

Alternativa descartada: sembrar todas las configuraciones al arrancar. Agrega un seed de arranque que C-04 no necesita, obliga a conocer a todos los odontólogos y complica el perfil `test`.

### D3 — `Clock` y `ZoneId` inyectables

Se declara un bean `Clock` (`Clock.systemUTC()`) y la zona se resuelve desde `app.schedule.timezone` en `ScheduleProperties`. El dominio y los servicios de agenda no llaman a `Instant.now()` ni a `ZoneId.systemDefault()`; reciben el `Clock` y la zona por constructor. Las pruebas usan un `@TestConfiguration` con un `Clock.fixed(...)` primario.

Se mantiene `Instant` en `ScheduleBlock` y en `Appointment.activeSlot`/`startAt`; la conversión a `LocalDate`/`LocalTime` ocurre solo en el borde de cálculo y de serialización. No se migran columnas ni se cambia el modelo de C-02. `AuditableEntity` conserva su implementación actual de `created_at`/`updated_at`; C-04 no la modifica ni la usa para calcular disponibilidad.

Alternativas descartadas: usar la zona del sistema (resultados distintos en Windows y en CI), cambiar las columnas a `timestamp without time zone` (migración y pérdida de información) y un `Clock` estático en tests (no permite composición por prueba).

### D4 — Grilla de slots anclada al inicio de cada franja

Para cada fecha del rango y cada franja del día correspondiente:

1. Se toma `window.startTime` como ancla local.
2. Se avanza de 30 en 30 minutos mientras `start.plusMinutes(30) <= window.endTime`; la validación de configuración rechaza previamente toda franja con duración menor a 30 minutos.
3. Se convierte cada inicio local a `Instant` con la zona argentina.
4. Se incluye el slot solo si `slot.start >= from` y `slot.end <= to` (contención en el rango pedido).
5. Se ordena cronológicamente.

Con la configuración por defecto, 09:00–13:00 aporta 8 slots y 14:00–19:00 aporta 10: 18 por día hábil y 90 por semana. No existe slot a las 13:00 ni a las 19:00, y la pausa 13:00–14:00 queda vacía. La validación rechaza con 422 cualquier franja menor a 30 minutos; una franja de exactamente 30 minutos produce un slot.

Un bloqueo o un turno excluye un slot si sus intervalos se intersectan en semántica half-open: `bloque.start < slot.end && bloque.end > slot.start`. Por eso un bloqueo 10:10–10:20 elimina el slot 10:00–10:30 y deja intactos 09:30–10:00 y 10:30–11:00. No se “redondea” el bloqueo ni se inventan slots desplazados.

Alternativas descartadas: persistir slots (la KB lo prohíbe y multiplica filas), anclar la grilla a la hora en punto global (rompe franjas que no empiezan en :00/:30) y recortar bloqueos a la grilla (pierde precisión y puede liberar minutos bloqueados).

### D5 — Consultas de repositorio half-open y portables

Se agregan métodos derivados de Spring Data, sin SQL nativo ni `EXTRACT(DOW)`:

- `ScheduleConfigurationRepository.findByDentistId(UUID)`.
- `ScheduleBlockRepository.findByDentistIdAndDeletedAtIsNullAndStartAtLessThanAndEndAtGreaterThan(UUID, Instant to, Instant from)` para bloqueos que solapan el rango.
- `AppointmentRepository.findByDentistIdAndDeletedAtIsNullAndActiveSlotIsNotNullAndStartAtGreaterThanEqualAndStartAtLessThan(UUID, Instant lookbackFrom, Instant to)` para turnos activos; `lookbackFrom` es `from - 30 minutos` para detectar un turno no alineado que empieza antes del rango y lo intersecta.

El filtro de solapamiento final se hace en Java sobre esa lista corta. H2 en modo PostgreSQL y PostgreSQL soportan los mismos operadores `<`, `>` y `=`, por lo que la paridad no depende de funciones de fecha.

Alternativa descartada: una consulta con `EXTRACT(DOW FROM start_at)` y aritmética de fechas en SQL. Divergen los dialectos y las pruebas dejarían de ser portables.

### D6 — Disponibilidad interna separada de la política pública

`AvailabilityService` expone:

- `List<Slot> freeSlots(UUID dentistId, Instant from, Instant to)`: calcula todos los slots de la configuración y descuenta bloqueos y turnos activos. No aplica ninguna ventana.
- `boolean isOpenSlot(UUID dentistId, Instant start)`: verdadero solo si `start` coincide exactamente con un inicio de la grilla configurada, no está bloqueado y no está ocupado. Es el contrato que usará C-05 antes de crear un turno.

La disponibilidad es advisory: no reserva nada ni persiste slots. La garantía final contra la doble reserva sigue siendo `UNIQUE(active_slot)` + 409 de C-05; C-04 no la reemplaza ni la debilita.

`PublicBookingWindow` es un componente aparte, construido desde `app.schedule.public-window`, con `earliest(clock)`, `latest(clock)` y `validate(from, to, clock)`. `GET /api/schedule/availability` lo aplica; el cálculo interno y C-05 no. C-07 reutilizará el mismo componente para validar una fecha pública y `freeSlots` para listar horarios.

Alternativas descartadas: poner la ventana dentro de `AvailabilityService` (C-05 heredaría una política que no le corresponde) y duplicar las constantes 24/30 en cada controlador (la UI y C-07 volverían a hardcodearlas).

### D7 — Contrato HTTP de configuración, disponibilidad y bloqueos

`GET /api/schedule` devuelve:

```json
{
  "timezone": "America/Argentina/Buenos_Aires",
  "slotDurationMinutes": 30,
  "publicWindow": {
    "minNoticeHours": 24,
    "maxDays": 30,
    "timezone": "America/Argentina/Buenos_Aires"
  },
  "days": [
    {
      "day": "MONDAY",
      "windows": [
        { "start": "09:00", "end": "13:00" },
        { "start": "14:00", "end": "19:00" }
      ]
    }
  ]
}
```

`PATCH /api/schedule` recibe `timezone` opcional y `days` obligatorio. Es un reemplazo total: deben venir exactamente los cinco días `MONDAY` a `FRIDAY`, cada uno con su lista de franjas (vacía significa cerrado). Un día faltante, repetido, sábado/domingo o una timezone distinta a la configurada produce 422. Las franjas invertidas o solapadas dentro del mismo día también producen 422. La respuesta es 200 con la misma forma que el GET. No se aceptan `effectiveFrom`/`effectiveTo`.

`GET /api/schedule/availability?from=...&to=...` recibe dos `OffsetDateTime` ISO-8601 obligatorios y devuelve:

```json
{
  "timezone": "America/Argentina/Buenos_Aires",
  "durationMinutes": 30,
  "window": {
    "from": "2026-11-25T00:00:00-03:00",
    "to": "2026-11-29T00:00:00-03:00",
    "minNoticeHours": 24,
    "maxDays": 30,
    "timezone": "America/Argentina/Buenos_Aires"
  },
  "slots": [
    { "start": "2026-11-25T09:00:00-03:00", "end": "2026-11-25T09:30:00-03:00" }
  ]
}
```

`slots` es una lista plana (sin agrupar por día) de objetos `start`/`end`; no se agrega un flag `busy`. El eco de `window.from`/`window.to` se normaliza a `-03:00`. Un rango fuera de la ventana, vacío, invertido, faltante o mal formado devuelve 400 con mensaje explícito; no se recorta ni se devuelve una lista vacía.

`POST /api/schedule/blocks` recibe `startAt`, `endAt` (`OffsetDateTime` ISO-8601) y `reason` opcional (máximo 255); responde 201 con `id`, `startAt`, `endAt` y `reason`. Solo se valida `endAt > startAt`; el bloqueo puede estar fuera de la ventana pública o en el pasado. `DELETE /api/schedule/blocks/{id}` responde 204 y aplica `markDeleted(clock.instant())`; si el bloqueo ya estaba eliminado, responde 204 sin reescribir `deleted_at`. Un bloqueo de otro odontólogo o inexistente responde 404.

### D8 — Errores acotados al paquete de agenda

Se agrega un `@RestControllerAdvice(basePackages = "com.turnosodontologia.schedule")` que traduce excepciones de negocio a:

- 400: rango de disponibilidad inválido, parámetro faltante o formato ISO incorrecto.
- 422: configuración inválida (franjas, días, timezone) o bloqueo con rango inválido.
- 404: bloqueo inexistente o ajeno.

El cuerpo de error conserva el formato de la API (`{"error": "..."}`) y agrega `details` solo en 422. El `basePackages` es deliberado: no existe un advice global y no se quiere alterar el JSON de autenticación de C-03. Todos los controladores de agenda deben vivir bajo `com.turnosodontologia.schedule` para que el advice los alcance; las pruebas cubren 400, 404 y 422 por HTTP.

Alternativa descartada: un `@RestControllerAdvice` global. Cambiaría respuestas de auth y de futuros módulos sin necesidad.

### D9 — Seguridad por sesión, sin `permitAll()`

No se modifica `AuthSecurityConfiguration`: `/api/schedule/**` sigue cayendo en `anyRequest().authenticated()`. El odontólogo se obtiene siempre de `DentistPrincipal.id()` con `@AuthenticationPrincipal`; ningún DTO acepta `dentistId`. La ruta pública será `/api/public/**` en C-07, no una excepción nueva en C-04.

Los controladores de agenda reciben 401 del entry point JSON existente cuando no hay sesión. Las pruebas MockMvc verifican 401 en GET, PATCH, availability y bloqueos sin sesión, y que el JSON de error no cambió.

Alternativa descartada: abrir `/api/schedule/availability` como pública. La reserva pública necesita otra forma de acceso y C-07 la definirá bajo `/api/public/**`; abrirla ahora filtraría la agenda sin decisión explícita.

### D10 — Esquema `local` sin migraciones

`local` corre con `ddl-auto: validate`, así que las tablas deben existir antes de arrancar. El change entrega `database/schedule-rules.sql` con DDL idempotente (`CREATE TABLE IF NOT EXISTS`, constraints e índice) y documenta en `database/README.md` el comando para aplicarlo con `psql` antes del arranque. No se agrega Flyway/Liquibase ni se cambia `ddl-auto`.

Forma esperada (los tipos exactos se confirman contra el esquema que Hibernate genera en `test`):

```sql
CREATE TABLE IF NOT EXISTS schedule_configurations (
  id uuid PRIMARY KEY,
  dentist_id uuid NOT NULL,
  created_at timestamp(6) with time zone NOT NULL,
  updated_at timestamp(6) with time zone NOT NULL,
  deleted_at timestamp(6) with time zone,
  CONSTRAINT uk_schedule_configurations_dentist UNIQUE (dentist_id),
  CONSTRAINT fk_schedule_configurations_dentist
    FOREIGN KEY (dentist_id) REFERENCES dentists (id)
);

CREATE TABLE IF NOT EXISTS schedule_windows (
  schedule_configuration_id uuid NOT NULL,
  day_of_week varchar(10) NOT NULL,
  start_time time(6) NOT NULL,
  end_time time(6) NOT NULL,
  CONSTRAINT fk_schedule_windows_configuration
    FOREIGN KEY (schedule_configuration_id) REFERENCES schedule_configurations (id)
);

CREATE INDEX IF NOT EXISTS idx_schedule_windows_configuration
  ON schedule_windows (schedule_configuration_id);
```

El perfil `test` sigue creando todo con `create-drop`; el script es solo para `local`. La verificación incluye arrancar `local` con `validate` y comprobar que Hibernate no reporta diferencias.

Alternativas descartadas: `ddl-auto: update` (deja el esquema a merced de Hibernate y contradice la configuración vigente), `schema.sql` automático (Spring lo ejecutaría también donde no corresponde y no resuelve el orden con el esquema de C-02) e introducir Flyway (fuera del stack aprobado).

### D11 — Paquetes y componentes

Se conserva `com.turnosodontologia.schedule` y se organiza sin mover `ScheduleBlock`:

- `schedule/domain`: `ScheduleWindow`, `WeeklySchedule`, `TimeRange`, `ScheduleCalendar`.
- `schedule/ScheduleConfiguration.java` y `ScheduleConfigurationRepository.java`.
- `schedule/config`: `ScheduleProperties`, `ScheduleClockConfiguration`, beans de zona/reloj.
- `schedule/service`: `ScheduleConfigurationService`, `AvailabilityService`, `PublicBookingWindow`, `ScheduleBlockService`.
- `schedule/api`: controladores de configuración, disponibilidad y bloqueos.
- `schedule/dto`: requests/responses y `ScheduleErrorResponse`.
- `schedule/error`: excepciones de negocio y `ScheduleExceptionHandler`.

`TimeRange` encapsula el intervalo half-open y la intersección; `WeeklySchedule` valida días, orden y solapamientos; `ScheduleCalendar` genera slots. Así la lógica pura se prueba sin Spring y los servicios solo orquestan persistencia.

Alternativa descartada: poner todo en `schedule/` plano. Con siete clases de cálculo, DTOs y tres controladores, el paquete se vuelve difícil de revisar.

### D12 — Estrategia TDD y determinismo

Cada unidad arranca con su prueba roja:

1. `TimeRange` (intersección, half-open, rangos inválidos).
2. `WeeklySchedule` (días, orden, solapamiento, franjas invertidas, cierre de día).
3. `ScheduleCalendar` (18 slots/día, 90/semana, bordes 09:00/12:30/14:00/18:30, sin 19:00, fin de semana, franja exacta de 30 minutos y franja desalineada de 45 minutos).
4. `ScheduleConfiguration` con `@DataJpaTest` + `@ActiveProfiles("test")` (mapeo, unique por odontólogo, orden de franjas, soft delete).
5. `AvailabilityService` con `@DataJpaTest` (bloqueo total/parcial/no alineado, bloqueo eliminado, turnos PENDING/CONFIRMED/CANCELLED, aislamiento por odontólogo).
6. `PublicBookingWindow` con `Clock.fixed` (límites exactos, un segundo antes/después, DST argentino inexistente).
7. `ScheduleService`/`ScheduleBlockService` con pruebas de servicio.
8. Controladores con `@SpringBootTest` + MockMvc (200/201/204/400/401/404/422, JSON y offset).
9. Regresión `./gradlew test` y verificación de paridad H2/PostgreSQL.

Todas las fechas de prueba son fijas y ficticias. No se usa `Thread.sleep`, ni el reloj real, ni la zona del sistema. Los fixtures viven en `backend/src/test/java/com/turnosodontologia/support/` y usan emails `@odontologia.invalid`.

Alternativa descartada: probar solo por HTTP. Los bordes de grilla y solapamiento se diagnostican mejor en pruebas unitarias, y MockMvc se reserva para el contrato.

### D13 — Supuestos registrados

- `PATCH /api/schedule` exige exactamente los cinco días hábiles; sábado y domingo se rechazan con 422 porque la v1 configura solo lunes a viernes.
- Una franja menor a 30 minutos se rechaza con 422 porque el turno tiene duración fija de 30 minutos; una franja de exactamente 30 minutos es válida y produce un slot. Las franjas desalineadas que duran al menos 30 minutos siguen siendo válidas y solo generan los slots completos que entren.
- Un bloqueo puede crearse en el pasado o fuera de la ventana pública; solo se exige `endAt > startAt`.
- La ocupación de un turno se evalúa por intersección de su intervalo fijo de 30 minutos, no solo por igualdad de inicio; es más conservador y evita liberar un slot parcialmente cubierto.
- La configuración no usa optimistic locking: un solo odontólogo y un reemplazo total idempotente hacen aceptable “la última escritura gana”. Se documenta para C-12.
- La baja repetida de un bloqueo propio responde 204; un bloqueo ajeno se trata como inexistente (404) para no filtrar su existencia.

## Risks / Trade-offs

- [El DDL manual puede no coincidir con lo que Hibernate valida en `local`] → Generar el esquema en `test`, copiar los tipos exactos, aplicar el script y arrancar `local` con `validate` como verificación obligatoria.
- [La disponibilidad es advisory y dos clientes pueden ver el mismo slot libre] → C-05 conserva `UNIQUE(active_slot)` y 409; las pruebas de C-04 no prometen exclusividad.
- [La siembra perezosa escribe en un GET] → Ocurre una sola vez por odontólogo, está documentada y la unique de `dentist_id` evita duplicados.
- [Carrera en la primera lectura de dos pestañas] → Reintento único de lectura tras violación de unicidad; el caso normal no paga ese costo.
- [Bloqueos no alineados pueden sorprender al odontólogo] → Se documenta la semántica half-open y se prueba el caso 10:10–10:20.
- [El reloj del sistema en Windows cambia resultados] → `Clock` y zona inyectables; pruebas con `Clock.fixed` y sin `Instant.now()` en el dominio.
- [El advice acotado no cubre controladores fuera del paquete] → Todos los controladores de agenda viven en `schedule/api`; una prueba HTTP por código de error lo verifica.
- [Sin versionado de configuración, dos pestañas pueden pisarse] → Aceptado para un único odontólogo; el reemplazo total evita mezclas parciales y queda anotado para C-12.
- [`time(6)`/`timestamp with time zone` pueden variar de precisión entre H2 y PostgreSQL] → Se validan ambos perfiles y el script usa los tipos que Hibernate acepta sin cambios.

## Migration Plan

1. Agregar la entidad, el repositorio y la lógica de cálculo; el perfil `test` crea el esquema automáticamente con `create-drop`.
2. Crear `database/schedule-rules.sql` con el DDL validado y documentar en `database/README.md` el paso previo al arranque local.
3. Aplicar el script en PostgreSQL local y arrancar con `ddl-auto: validate`; corregir el script si Hibernate reporta una diferencia.
4. Ejecutar `./gradlew test` y `./gradlew build`; la suite de C-01 a C-03 debe seguir verde.
5. Actualizar `knowledge-base/04_modelo_de_datos.md` (ERD + `ScheduleConfiguration`) en la misma entrega.
6. Rollback: revertir el código y eliminar solo las dos tablas nuevas si no hay datos que conservar; no hay migración de datos existentes. `CHANGES.md` se actualiza únicamente al cerrar y verificar el change.

## Open Questions

Ninguna que afecte specs, enfoque o desglose de tareas. Los supuestos de D13 quedan registrados y pueden revisarse en el apply sin cambiar el contrato externo.

## UI Mockups

- Design system revision: no aplica (el proyecto no tiene `design-system/` y este change no produce UI).
- Decision: no (no UI), 2026-11-23.
- Mockups: ninguno.
- Approval: no aplica.
- Preview: no aplica.
- Visual verify: no aplica.
