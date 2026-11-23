# Proposal

## Why

C-05 debe convertir el modelo persistente de turnos existente en el núcleo operativo de la agenda: crear turnos válidos, controlar estados y cancelaciones, y exponerlos al odontólogo sin duplicar reglas entre endpoints. Sin este servicio central, C-07, C-09 y C-12 podrían divergir en la reserva, la idempotencia y el manejo de conflictos.

## What Changes

- Agregar el servicio de aplicación de turnos que crea una cita de 30 minutos asociada a un paciente y mantiene `active_slot` solo para turnos activos.
- Incorporar prevención transaccional de solapamientos mediante validación y la restricción única existente sobre `active_slot`, traduciendo conflictos de persistencia a HTTP 409.
- Centralizar cambios de estado y cancelaciones en `AppointmentService`, con una máquina de estados explícita y registro de `AppointmentEvent` en cada transición real.
- Exponer `PATCH /api/appointments/{id}/status` y `GET /api/appointments?from=&to=` para cambios protegidos y consulta de agenda por rango.
- Publicar `AppointmentCancelledEvent(appointmentId, startAt, actor)` solamente después de una transición real a `CANCELLED` dentro de la cancelación centralizada.
- Mantener comportamiento idempotente: una segunda cancelación responde "ya cancelado" y no publica ni registra efectos nuevos.

## Capabilities

### New Capabilities
- `gestion-turnos`: crear, consultar y cambiar el estado de turnos desde el servicio y la API de agenda del odontólogo.

### Modified Capabilities
- `integridad-turnos`: ampliar las invariantes existentes para incluir estados, eventos, cancelación idempotente y bloqueo único transaccional con respuesta 409.

## Impact

- Backend: reutiliza `Appointment`, `AppointmentRepository`, `AppointmentEvent`, `AppointmentStatus`, `Actor`, `CreatedBy`, `Patient`, `PatientRepository`, `DentistRepository`, `ScheduleProperties` y el `Clock` compartido de agenda.
- API: agrega endpoints protegidos de turno bajo `/api/appointments`; mantiene `/api/auth/**` y la configuración de seguridad existente.
- Persistencia: reutiliza la tabla y la restricción única `appointments.active_slot` de C-02; no introduce migraciones estructurales nuevas ni modifica C-04/schedule-rules.
- Eventos: publica el evento de dominio ya definido para que C-11 pueda consumirlo en una fase posterior, sin modificar C-11 ni C-09.
- Pruebas: usa H2 PostgreSQL mode, datos ficticios, `AuthFixtures`, `Clock.fixed` y repositorios existentes; no usa reloj real ni esperas activas.
- Fuera de alcance: reserva pública C-07, token C-09, outbox C-10, lista de espera C-11, UI y cambios en C-02/C-03/C-04/C-06.
