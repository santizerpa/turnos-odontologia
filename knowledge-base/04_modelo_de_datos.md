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
Patient 1 ─────────── * Appointment
Appointment 1 ─────── 1 AppointmentToken
Appointment 1 ─────── * AppointmentEvent
AppointmentSlot 1 ─── 1 Appointment (opcional)
Patient 1 ─────────── * WaitingListEntry
WaitingListEntry * ─── 1 AppointmentSlotRelease
OutboxMessage * ───── 1 Appointment (opcional)
```

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
- `patient_id: UUID` — FK a Patient.
- `start_at: timestamp` — hora local de Argentina (UTC-3); sin horario de verano.
- `duration_minutes: integer` — fijo 30 en v1.
- `status: enum` — PENDING, CONFIRMED, CANCELLED, ABSENT, ATTENDED.
- `created_by: enum` — PATIENT, DENTIST.
- `cancelled_at`, `version`, `created_at`, `updated_at`, `deleted_at`.
- Constraint único sobre franja activa; el diseño debe impedir dobles turnos.

### AppointmentToken
- `id: UUID`.
- `appointment_id: UUID` — FK única.
- `token_hash: varchar(128)` — no guardar token plano.
- `expires_at: timestamp` — vence al inicio del turno o a los 7 días, lo que ocurra primero.
- `used_at: timestamp nullable`.

### WaitingListEntry
- `id: UUID`.
- `patient_id: UUID`.
- `requested_date_from/to: date nullable`.
- `position: integer` o orden por `created_at`.
- `status: enum` — WAITING, NOTIFIED, EXPIRED, CONSUMED.
- `deleted_at`.

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
- Mensajes existentes en outbox para revisar la demo.
