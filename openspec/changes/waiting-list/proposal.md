# Proposal

## Why

El sistema ya registra turnos cancelados mediante `AppointmentCancelledEvent` y persiste `WaitingListEntry`, pero todavía no permite administrar inscripciones ni avisar al primer paciente elegible cuando se libera una franja. C-11 completa ese flujo con una cola FIFO y un aviso simulado, sin reservar ni confirmar el turno por el paciente.

## What Changes

- Incorporar la API de lista de espera: `POST /api/public/waiting-list` para que un paciente se inscriba sin sesión y `GET /api/waiting-list` para que el odontólogo autenticado consulte las entradas.
- Reutilizar un paciente existente por DNI normalizado sin modificar sus datos; crear el paciente solo cuando no exista. Las fechas solicitadas son opcionales.
- Al cancelar un turno, consumir `AppointmentCancelledEvent` después del commit y en una transacción nueva. Seleccionar en FIFO la primera entrada `WAITING` cuyo rango inclusivo contenga la fecha local argentina del turno; un límite nulo no restringe ese lado.
- Marcar la entrada elegida `NOTIFIED` y crear atómicamente un `OutboxMessage` dirigido al email guardado del paciente. El mensaje queda asociado al turno cancelado y usa el constructor de cinco parámetros, con `type` nulo cuando esté integrado C-10; no usar `REMINDER` ni modificar las clases de C-05.
- Si no hay entradas elegibles, no cambiar estados ni crear mensajes. No asignar ni confirmar automáticamente el turno liberado.
- Agregar pruebas de FIFO, elegibilidad, transición de estados, atomicidad del aviso, seguridad HTTP y consumo del evento publicado manualmente.

## Capabilities

### New Capabilities
- `lista-espera`: inscripción pública y consulta autenticada de la cola, selección FIFO por elegibilidad y aviso simulado al liberarse un turno.

### Modified Capabilities
- Ninguna.

## Impact

- Backend `waiting`, integración de `AppointmentCancelledEvent`, API y persistencia de `OutboxMessage`.
- La ruta de inscripción queda bajo `/api/public/**`, que ya es pública en la configuración de seguridad; el listado permanece protegido por la sesión existente.
- En el contrato de C-10, los avisos de lista de espera no son recordatorios: el tipo debe quedar nulo mediante el constructor compatible de cinco parámetros para evitar colisiones con la unicidad `(appointment_id, type)`.
- Pruebas con JUnit y H2 en modo PostgreSQL; no se requieren SMTP, servicios externos ni datos reales de pacientes.
