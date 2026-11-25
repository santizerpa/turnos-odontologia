# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
