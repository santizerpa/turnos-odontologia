# outbox-reminders Specification

## Purpose

Permitir que el odontólogo genere y revise recordatorios simulados con enlaces vigentes para turnos próximos, sin entrega real por SMTP y con operaciones protegidas e idempotentes.

## Requirements

### Requirement: El sistema genera un recordatorio elegible con enlaces vigentes
El sistema SHALL generar un único mensaje `REMINDER` para turnos `PENDING` o `CONFIRMED`, con `activeSlot` no nulo y cuyo inicio cumple `now + 24h < startAt <= now + 48h`. Para cada creación SHALL emitir el token mediante `AppointmentTokenService.issue(Appointment)` y armar enlaces relativos de confirmación y cancelación con el formato `/api/public/appointments/{token}/confirm` y `/api/public/appointments/{token}/cancel`.

#### Scenario: Turno a 36 horas recibe recordatorio feliz
- **GIVEN** un turno ficticio `PENDING` con `activeSlot` no nulo y `startAt = now + 36h`
- **WHEN** el odontólogo autentica y ejecuta `POST /api/outbox/reminders/run`
- **THEN** el sistema crea un mensaje `REMINDER` con la dirección ficticia del paciente, links relativos válidos con el nuevo token y devuelve `created = 1`

#### Scenario: Turno cancelado no recibe recordatorio
- **GIVEN** un turno ficticio `CANCELLED` con `activeSlot` nulo y `startAt = now + 36h`
- **WHEN** se ejecuta `POST /api/outbox/reminders/run`
- **THEN** el sistema no llama a emisión de token y no crea mensaje

#### Scenario: Turno confirmado a menos de 24 horas no recibe recordatorio
- **GIVEN** un turno ficticio `CONFIRMED` con `activeSlot` no nulo y `startAt = now + 23h`
- **WHEN** se ejecuta `POST /api/outbox/reminders/run`
- **THEN** el sistema no crea un recordatorio ni llama a emisión de token

#### Scenario: Exactamente ahora más 24 horas no recibe
- **GIVEN** un turno ficticio elegible cuyo `startAt` es exactamente `now + 24h`
- **WHEN** se ejecuta `POST /api/outbox/reminders/run`
- **THEN** el sistema no crea el recordatorio por no cumplir la ventana estricta inferior

#### Scenario: Exactamente ahora más 48 horas sí recibe
- **GIVEN** un turno ficticio elegible cuyo `startAt` es exactamente `now + 48h`
- **WHEN** se ejecuta `POST /api/outbox/reminders/run`
- **THEN** el sistema crea un único recordatorio por cumplir la ventana superior inclusiva

### Requirement: La generación de recordatorios no se duplica
El sistema SHALL identificar los recordatorios por la clave lógica `(appointment_id, type)` y SHALL NOT llamar a emisión de token ni crear otro mensaje si ya existe un `REMINDER` para el turno. Para cada turno elegible, el sistema SHALL ejecutar `AppointmentTokenService.issue(Appointment)` y el guardado del `REMINDER` en una única transacción; si cualquiera falla, SHALL revertir la reemisión y el guardado de ese turno sin afectar los demás turnos elegibles.

#### Scenario: Ejecutar dos veces no duplica
- **GIVEN** un turno ficticio elegible sin recordatorio previo
- **WHEN** `POST /api/outbox/reminders/run` se ejecuta una vez y luego una segunda vez
- **THEN** la primera ejecución crea uno y la segunda devuelve `created = 0` sin emitir token nuevo ni duplicar el mensaje

#### Scenario: Restricción persistente rechaza duplicado concurrente
- **GIVEN** dos ejecuciones que encuentran simultáneamente el mismo turno elegible sin recordatorio
- **WHEN** ambas intentan persistir un `REMINDER` para el mismo turno
- **THEN** la restricción única impide el segundo registro y solo puede existir un recordatorio por turno

#### Scenario: Dos turnos elegibles y issue falla para uno
- **GIVEN** dos turnos ficticios elegibles y `issue()` falla para uno de ellos
- **WHEN** se ejecuta `POST /api/outbox/reminders/run`
- **THEN** responde `200 OK` con `created = 1` y `failed = 1`; no queda `REMINDER` para el turno que falló, su token anterior sigue válido y el otro turno recibe su recordatorio
- **AND** la respuesta no incluye mensajes internos de excepción ni stack traces

#### Scenario: Error inesperado fuera del procesamiento por turno
- **GIVEN** una petición autenticada y una falla inesperada al consultar los turnos elegibles
- **WHEN** se ejecuta `POST /api/outbox/reminders/run`
- **THEN** responde `500 Internal Server Error` con un mensaje general fijo, sin exponer detalles internos ni stack trace

### Requirement: Reemitir el token invalida el enlace anterior de reserva
El sistema SHALL usar el token plano devuelto por `issue(Appointment)` para el nuevo recordatorio. Como DD-08 reemplaza el hash persistido, el link anterior generado por C-07 SHALL dejar de resolverse después de que C-10 reemita.

#### Scenario: El link anterior deja de funcionar tras el recordatorio
- **GIVEN** un turno ficticio con un token de reserva emitido por C-07 y su link anterior
- **WHEN** C-10 genera el recordatorio y reemite el token
- **THEN** el nuevo link contiene el nuevo token válido y el token anterior no resuelve ni confirma ni cancela el turno

### Requirement: El despacho simulado es idempotente
El sistema SHALL marcar como `SIMULATED_SENT` y fijar `sentAt` solo sobre mensajes `PENDING`; SHALL no modificar ni volver a fijar `sentAt` en mensajes que ya estén enviados.

#### Scenario: Despacho feliz
- **GIVEN** dos mensajes ficticios `PENDING` y un reloj fijado a `2026-11-24T10:00:00Z`
- **WHEN** el odontólogo ejecuta `POST /api/outbox/dispatch`
- **THEN** ambos pasan a `SIMULATED_SENT`, quedan con `sentAt` igual al instante del reloj y la respuesta devuelve `dispatched = 2`

#### Scenario: Segundo despacho no repite cambios
- **GIVEN** mensajes previamente marcados `SIMULATED_SENT` por el primer despacho
- **WHEN** se ejecuta nuevamente `POST /api/outbox/dispatch`
- **THEN** el sistema devuelve `dispatched = 0`, conserva los estados enviados y no cambia ningún `sentAt`

### Requirement: La bandeja expone datos mínimos solo a sesión autenticada
El sistema SHALL proteger `GET /api/outbox/messages`, `POST /api/outbox/reminders/run` y `POST /api/outbox/dispatch` con la sesión de odontólogo y SHALL devolver solo datos mínimos de mensajería, más el nombre y el DNI del paciente del turno, ordenados del mensaje más reciente al más antiguo.

#### Scenario: GET protegido sin sesión da 401
- **GIVEN** una petición sin sesión al endpoint `GET /api/outbox/messages`
- **WHEN** el cliente realiza la petición
- **THEN** el sistema responde `401 Unauthorized` y no devuelve mensajes ni datos internos

#### Scenario: GET autenticado lista el formato permitido
- **GIVEN** una sesión válida de odontólogo y mensajes ficticios en la bandeja
- **WHEN** ejecuta `GET /api/outbox/messages`
- **THEN** cada elemento incluye `id`, `appointmentId`, `recipientEmail`, `subject`, `body`, `status`, `type`, `createdAt`, `sentAt`, `patientName` (nombre y apellido) y `patientDni`, estos dos nulos si el mensaje no tiene turno, sin hash, token persistido ni datos de sesión
- **AND** los elementos están ordenados por `createdAt` descendente

#### Scenario: Ejecución exitosa siempre responde 200 con conteos
- **GIVEN** una petición autenticada a `POST /api/outbox/reminders/run` con procesamiento por turno completado
- **WHEN** todos los turnos elegibles se procesan individualmente
- **THEN** la respuesta es `200 OK` con el objeto `{ "created": <cantidad>, "failed": <cantidad> }`, sin detalles internos

#### Scenario: Fallo de issue se registra como fallo por turno
- **GIVEN** una petición autenticada a `POST /api/outbox/reminders/run` y `issue()` que falla para un turno elegible
- **WHEN** el procesador de ese turno revierte su transacción y continúa
- **THEN** responde `200 OK` con `failed = 1`, incrementado para ese turno, y no expone el mensaje interno de la excepción ni stack trace

### Requirement: La bandeja de entrada simulada del paciente se lee por email solo en modo demo
El sistema SHALL exponer `GET /api/public/inbox?email=` sin sesión solo si `app.demo.inbox.enabled` es `true` (desactivada por defecto, activa en el perfil `local`). Devuelve, del más reciente al más antiguo, los mensajes cuyo `recipientEmail` coincide con el email normalizado, con solo `id`, `subject`, `recipientEmail`, `patientDni`, `appointmentStartAt`, `appointmentDurationMinutes`, `createdAt` y `body`. Es una simulación de demo, no autenticación.

#### Scenario: Mensajes del email consultado
- **GIVEN** la bandeja simulada habilitada y mensajes ficticios para `paciente@demo.invalid` y `otro@demo.invalid`
- **WHEN** se solicita `GET /api/public/inbox?email=%20Paciente@Demo.invalid%20` sin sesión
- **THEN** se responde `200` solo con los mensajes de `paciente@demo.invalid`, ordenados del más reciente al más antiguo, con los campos permitidos y sin hash de token ni datos de sesión

#### Scenario: Mensaje sin turno asociado
- **GIVEN** la bandeja simulada habilitada y un mensaje ficticio sin turno para el email consultado
- **WHEN** se lista la bandeja de ese email
- **THEN** el mensaje se devuelve con `patientDni`, `appointmentStartAt` y `appointmentDurationMinutes` nulos

#### Scenario: Bandeja simulada deshabilitada
- **GIVEN** `app.demo.inbox.enabled` ausente o `false`
- **WHEN** se solicita `GET /api/public/inbox?email=paciente@demo.invalid`
- **THEN** se responde `404` con `{"error":"Bandeja simulada deshabilitada"}` y no se consultan mensajes

#### Scenario: Email ausente o inválido
- **GIVEN** la bandeja simulada habilitada
- **WHEN** se solicita sin `email`, con un valor vacío o sin formato de email
- **THEN** se responde `422` con `{"error":"Email inválido"}` y no se consultan mensajes

#### Scenario: Email sin mensajes
- **GIVEN** la bandeja simulada habilitada y ningún mensaje para `nadie@demo.invalid`
- **WHEN** se solicita `GET /api/public/inbox?email=nadie@demo.invalid`
- **THEN** se responde `200` con un array vacío
