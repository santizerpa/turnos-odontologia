# Proposal

## Why

C-05 debe convertir el modelo persistente de turnos existente en el núcleo operativo de la agenda: crear turnos válidos con duración fija, controlar estados y cancelaciones, y exponerlos al odontólogo sin duplicar reglas entre endpoints. Sin este servicio central, C-07, C-09 y C-12 podrían divergir en la reserva y el manejo de conflictos.

La máquina de estados de C-05 se restringe a la matriz autoritativa de `knowledge-base/05_reglas_de_negocio.md` §Estados: `PENDING → CONFIRMED | CANCELLED | ABSENT` y `CONFIRMED → CANCELLED | ABSENT | ATTENDED`; no existen transiciones de regreso a `PENDING`. La repetición de una cancelación es idempotencia de efecto, no una transición de estado.

## What Changes

- Agregar el servicio de aplicación de turnos que crea una cita de 30 minutos asociada a un paciente; C-05 no acepta duración como entrada y siempre fija `durationMinutes=30`.
- Incorporar prevención transaccional de solapamientos mediante validación y la restricción única existente sobre `active_slot`, resolviendo el conflicto como excepción de servicio o integridad persistida y traduciéndolo al código HTTP 409 existente.
- Centralizar cambios de estado y cancelaciones en `AppointmentService` según la matriz de estados de la KB.
- Exponer `PATCH /api/appointments/{id}/status` y `GET /api/appointments?from=&to=` a la agenda protegida del odontólogo. C-05 no agrega `POST /api/appointments`.
- Publicar `AppointmentCancelledEvent(appointmentId, startAt, actor)` solamente después de una transición real a `CANCELLED` dentro de la cancelación centralizada.
- Mantener comportamiento idempotente: una segunda cancelación responde "ya cancelado" y no publica ni registra efectos nuevos.

## Capabilities

### New Capabilities
- `gestion-turnos`: crear, consultar y cambiar el estado de turnos desde el servicio y la API de agenda del odontólogo.

### Modified Capabilities
- `integridad-turnos`: ampliar las invariantes existentes para estados reales, eventos, cancelación idempotente y bloqueo único transaccional con respuesta 409, limitado a los requisitos `active_slot`.

## Impact

- Backend: reutiliza `Appointment`, `AppointmentRepository`, `AppointmentEvent`, `AppointmentStatus`, `Actor`, `CreatedBy`, `Patient`, `PatientRepository`, `DentistRepository`, `ScheduleProperties` y el `Clock` compartido de agenda.
- API: agrega `GET` y `PATCH` protegidos bajo `/api/appointments`; mantiene `/api/auth/**` y la configuración de seguridad existente de C-03.
- Persistencia: reutiliza la tabla y la restricción única `appointments.active_slot` de C-02; no introduce migraciones estructurales nuevas ni modifica C-04/schedule-rules.
- Eventos: publica el evento de dominio ya definido para que C-11 pueda consumirlo en una fase posterior, sin modificar C-11 ni C-09.
- Pruebas: usa H2 PostgreSQL mode, datos ficticios, `AuthFixtures`, `Clock.fixed` y repositorios existentes; no usa reloj real ni esperas activas.
- Fuera de alcance: reserva pública C-07, AppointmentToken y orquestación de token C-09, outbox C-10, lista de espera C-11, UI y cambios en C-02/C-03/C-04/C-06.
