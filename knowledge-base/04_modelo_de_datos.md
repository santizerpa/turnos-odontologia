# Modelo de Datos

## Dominios

- Identidad: Odontólogo y sesión.
- Pacientes: datos mínimos y duplicados por DNI.
- Agenda: disponibilidad y slots.
- Turnos: estado, token y versión de concurrencia.
- Lista de espera: cola FIFO.
- Mensajería: salida simulada y registro de eventos.

## ERD

```text
Dentist 1 ─────────── * Appointment
Dentist 1 ─────────── * ScheduleBlock
Dentist 1 ─────────── 1 ScheduleConfiguration
ScheduleConfiguration 1 ─────────── * ScheduleWindow (colección embebida)
Patient 1 ─────────── * Appointment
Appointment 1 ─────── 1 AppointmentToken
Appointment 1 ─────── * AppointmentEvent
Patient 1 ─────────── * WaitingListEntry
OutboxMessage * ───── 1 Appointment (opcional)
```

Los slots no se persisten: se calculan a partir de la configuración de horario, los `ScheduleBlock` y los turnos activos. La ventana pública es una política de consulta de 24 horas hasta 30 días, no una entidad persistida.

## Entidades

### Dentist
- `id: UUID` — clave primaria.
- `email: varchar(255)` — único y usado para iniciar sesión.
- `password_hash: varchar(255)` — BCrypt; nunca guardar contraseña plana.
- `first_name: varchar(100)`.
- `last_name: varchar(100)`.
- `created_at`, `updated_at`, `deleted_at`.

### Patient
- `id: UUID` — clave primaria.
- `dni: varchar(8)` — único; solo 7 u 8 dígitos; puntos y espacios se eliminan; letras se rechazan.
- `first_name: varchar(100)` — requerido.
- `last_name: varchar(100)` — requerido.
- `email: varchar(255)` — requerido y validado.
- `created_at`, `updated_at`, `deleted_at`.

### Appointment
- `id: UUID` — clave primaria.
- `dentist_id: UUID` — FK a Dentist.
- `patient_id: UUID` — FK a Patient.
- `start_at: timestamp` — hora local de Argentina (UTC-3); sin horario de verano.
- `duration_minutes: integer` — fijo 30 en v1.
- `status: enum` — PENDING, CONFIRMED, CANCELLED, ABSENT, ATTENDED.
- `created_by: enum` — PATIENT, DENTIST.
- `active_slot: timestamp nullable` — igual a `start_at` mientras el estado no sea `CANCELLED`; se pone en `NULL` al cancelar. Tiene constraint único y los `NULL` no chocan. Lo mantiene el servicio al crear y al cancelar; nunca se carga a mano.
- `cancelled_at`, `version`, `created_at`, `updated_at`, `deleted_at`.

### AppointmentToken
- `id: UUID`.
- `appointment_id: UUID` — FK única.
- `token_hash: varchar(128)` — SHA-256 del token; nunca guardar el token plano.
- `expires_at: timestamp` — vence al inicio del turno o a los 7 días, lo que ocurra primero.
- `created_at`.
- El token se genera con al menos 128 bits de entropía (32 bytes en base64url). No se consume al confirmar: confirmar y cancelar son idempotentes y el token sigue válido hasta su vencimiento.

### WaitingListEntry
- `id: UUID`.
- `patient_id: UUID`.
- `requested_date_from/to: date nullable`.
- `position: integer` o orden por `created_at`.
- `status: enum` — WAITING, NOTIFIED, EXPIRED, CONSUMED.
- `deleted_at`.

### ScheduleBlock
- `id: UUID`.
- `dentist_id: UUID` — FK a Dentist.
- `start_at: timestamp`.
- `end_at: timestamp`.
- `reason: varchar(255)` nullable.
- `created_at`, `updated_at`, `deleted_at`.
- Bloquea total o parcialmente un día; el cálculo de slots lo excluye.

### ScheduleConfiguration
- `id: UUID` — clave primaria.
- `dentist_id: UUID` — FK única a Dentist; una sola configuración por odontólogo.
- `windows` — colección embebida `@ElementCollection` persistida en `schedule_windows`; no posee identidad propia.
- `day_of_week: enum textual` — lunes a viernes en la v1; la colección también admite el cierre mediante una lista sin ventanas.
- `start_time: time(6)` y `end_time: time(6)` — horas locales de Argentina, sin timezone en la tabla.
- `created_at`, `updated_at`, `deleted_at` — auditoría y baja lógica heredadas.
- La lectura determina las ventanas por día y hora de inicio. La timezone fija `America/Argentina/Buenos_Aires`, la duración de 30 minutos y la ventana pública 24 horas/30 días viven en configuración de despliegue y no se duplican por fila.
- Los slots son derivados y no se almacenan. Se generan desde las ventanas, se excluyen por bloqueos vigentes y turnos PENDING/CONFIRMED, y se sirven dentro de la política pública solo en las consultas públicas; el cálculo interno puede recibir otros rangos.

### ScheduleWindow
- Es un value object embebido, sin clave primaria ni repositorio propio.
- Persistido como filas dependientes de `ScheduleConfiguration` en `schedule_windows`.
- Requiere día, hora de inicio y hora de fin; una franja dura al menos 30 minutos y no debe solaparse con otra del mismo día.

### OutboxMessage
- `id: UUID`.
- `appointment_id: UUID nullable`.
- `type: enum` — BOOKING, REMINDER; nullable para mantener los mensajes de C-07 en `null`.
- Restricción única: `(appointment_id, type)`.
- `recipient_email: varchar(255)`.
- `subject: varchar(255)`.
- `body: text`.
- `status: enum` — PENDING, SIMULATED_SENT.
- `created_at`, `sent_at`.

### AppointmentEvent
- `id: UUID`.
- `appointment_id: UUID`.
- `from_status`, `to_status`.
- `actor: enum`.
- `reason: varchar(255)`.
- `created_at`.

## Seed data inicial

- 1 odontólogo ficticio con email, nombre y contraseña BCrypt ficticia documentada solo para demo.
- 5-10 pacientes ficticios.
- Agenda con turnos ocupados y disponibles.
- 2 turnos confirmados, 1 pendiente y 1 cancelado.
- 3 entradas simples en lista de espera.
- 1 bloqueo de agenda de ejemplo.
- Mensajes existentes en outbox para revisar la demo.
