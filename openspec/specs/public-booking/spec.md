# public-booking Specification

## Purpose

Definir el comportamiento observable de la disponibilidad y la reserva públicas: consulta de slots dentro de la ventana efectiva por día, creación de un turno PENDING y bandeja de salida simulada.

## Requirements

### Requirement: Consultar disponibilidad pública
GET /api/public/slots SHALL devolver únicamente slots libres dentro de la parte del día comprendida entre 24 horas y 30 días desde el momento de consulta (RN-RS-01). C-07 SHALL usar el Clock, el timezone y los límites de `PublicBookingWindow` de C-04, sin modificar C-04.

#### Scenario: Fecha parcialmente dentro de la ventana
- **WHEN** el Clock fijo indica ahora y se consulta una fecha cuyo día comienza antes de ahora+24 horas pero contiene franjas posteriores
- **THEN** responde 200
- **AND** solo incluye franjas desde `max(inicio del día, now+24h)` hasta `min(fin del día, now+30d)`

#### Scenario: Hoy queda fuera de la ventana
- **WHEN** el Clock fijo indica ahora y se consulta la fecha de hoy con todas sus franjas antes de ahora+24 horas
- **THEN** responde 422 con el mensaje fijo `La fecha solicitada está fuera de la ventana pública`

#### Scenario: Último día parcialmente dentro de la ventana
- **WHEN** el Clock fijo indica ahora y se consulta una fecha posterior a ahora+30 días pero dentro de ese mismo día
- **THEN** responde 200
- **AND** solo incluye franjas hasta `min(fin del día, now+30d)`

#### Scenario: Fecha completa fuera de la ventana
- **WHEN** se consulta una fecha cuyo día completo termina antes de ahora+24 horas o comienza después de ahora+30 días
- **THEN** responde 422 con el mensaje fijo `La fecha solicitada está fuera de la ventana pública`

#### Scenario: Date faltante o sin parsear
- **WHEN** se consulta GET /api/public/slots sin date o con un date que no usa formato ISO
- **THEN** responde 422 con un mensaje fijo

#### Scenario: No hay slots libres
- **WHEN** se consulta una fecha parcialmente válida pero todas sus franjas están bloqueadas u ocupadas
- **THEN** responde 200 con una lista vacía

### Requirement: Reservar un turno público
POST /api/public/appointments SHALL validar primero el único Dentist, la ventana y que `startAt` sea una franja libre exacta de la grilla; SHALL reutilizar `PatientService.upsert` de C-06 y `AppointmentService.create` de C-05; SHALL crear PENDING y SHALL responder 201 con `PublicBookingResponse` mínimo.

#### Scenario: Reserva exitosa
- **WHEN** se envía nombre, apellido, DNI y email válidos junto con un startAt devuelto por GET /api/public/slots
- **THEN** responde 201 con `PublicBookingResponse(startAt, durationMinutes=30, status=PENDING)`
- **AND** la respuesta no incluye dentistId, patientId, activeSlot ni token
- **AND** el paciente se reutiliza o crea por DNI normalizado
- **AND** el turno persistido queda PENDING con duration 30 y active_slot igual a startAt

#### Scenario: Franja reenviada desde disponibilidad
- **WHEN** el cliente toma un startAt de GET /api/public/slots y lo reenvía a POST /api/public/appointments
- **THEN** responde 201 si la franja sigue libre

#### Scenario: StartAt fuera de la grilla o bloqueado
- **WHEN** se recibe startAt que no coincide con una franja libre exacta, está fuera de horario o está bloqueado por ScheduleBlock
- **THEN** responde 422 con el mensaje fijo `La franja solicitada no está disponible`
- **AND** no se ejecuta el upsert ni se crea el turno

#### Scenario: Solapamiento
- **WHEN** una franja activa PENDING o CONFIRMED ocupa el mismo startAt solicitado
- **THEN** responde 409 con el mensaje fijo `La franja solicitada ya está ocupada`
- **AND** no queda un paciente nuevo creado, turno nuevo ni OutboxMessage nuevo

#### Scenario: Validación inválida
- **WHEN** se reciben DNI inválido, email inválido, nombre o apellido vacío, o startAt fuera de la ventana pública
- **THEN** responde 422 con mensajes fijos en español
- **AND** no persiste paciente ni turno

#### Scenario: Ventana inválida en el POST
- **WHEN** se recibe una startAt cuya fecha completa queda fuera de la ventana pública de 24 horas–30 días
- **THEN** responde 422 con el mensaje fijo `La fecha solicitada está fuera de la ventana pública`
- **AND** no se ejecuta el upsert ni se crea el turno

#### Scenario: Odontólogo público no disponible
- **WHEN** no existe exactamente un Dentist habilitado para la reserva pública
- **THEN** responde 503 con el mensaje fijo `Servicio temporalmente no disponible`
- **AND** no filtra la configuración interna

### Requirement: Integrar emisión de token por C-09
C-07 SHALL invocar exclusivamente `AppointmentTokenService.issue(appointment)` después de guardar el Appointment con id. SHALL usar el token plano devuelto solo para construir los links del OutboxMessage y SHALL no crear ni calcular AppointmentToken.

#### Scenario: Token asociado al turno creado
- **WHEN** se completa una reserva exitosa después del merge de C-09
- **THEN** existe un AppointmentToken asociado al Appointment creado mediante la emisión de `AppointmentTokenService.issue(appointment)`
- **AND** el body del OutboxMessage contiene un token plano de exactamente 43 caracteres base64url
- **AND** el turno ya tiene id antes de invocar issue()

#### Scenario: Bandeja de salida de reserva
- **WHEN** se completa una reserva exitosa
- **THEN** existe un OutboxMessage PENDING asociado al Appointment
- **AND** recipient_email coincide con el email normalizado del paciente
- **AND** el body contiene `/api/public/appointments/{token}/confirm` y `/api/public/appointments/{token}/cancel`
