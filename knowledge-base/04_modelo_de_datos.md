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
Patient 1 ─────────── * Appointment
Appointment 1 ─────── 1 AppointmentToken
Appointment 1 ─────── * AppointmentEvent
Patient 1 ─────────── * WaitingListEntry
OutboxMessage * ───── 1 Appointment (opcional)
```

Los slots no se persisten: se calculan a partir de la configuración de horario, los `ScheduleBlock` y los turnos activos.

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

### OutboxMessage
- `id: UUID`.
- `appointment_id: UUID nullable`.
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
