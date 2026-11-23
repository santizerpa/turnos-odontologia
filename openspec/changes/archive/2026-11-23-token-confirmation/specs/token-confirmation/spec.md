# Token confirmation

## ADDED Requirements

### Requirement: El sistema valida un token reutilizable antes del estado del turno
El sistema SHALL validar la presencia, existencia y vigencia de un token calculando el hash SHA-256 del valor recibido y buscándolo por hash. El token SHALL ser válido solo si `now < expiresAt`. La inexistencia o el vencimiento SHALL rechazarse antes de inspeccionar el estado del turno.

#### Scenario: Token válido confirmado una sola vez
- **DADO** un turno ficticio con estado `PENDING` y un token válido cuyo `expiresAt` es posterior a `now`
- **CUANDO** se llama a `POST /api/public/appointments/{token}/confirm`
- **ENTONCES** el turno pasa a `CONFIRMED` y el sistema responde confirmación exitosa

#### Scenario: Segunda confirmación sin efectos repetidos
- **DADO** un turno que ya está `CONFIRMED` y cuyo token sigue vigente
- **CUANDO** se llama nuevamente a `POST .../confirm`
- **ENTONCES** el sistema responde `ya confirmado` sin repetir cambios ni eventos

#### Scenario: Token vencido tiene precedencia sobre turno confirmado
- **DADO** un turno `CONFIRMED` cuyo token tiene `expiresAt` igual a `now`
- **CUANDO** se llama a `POST .../confirm`
- **ENTONCES** el sistema rechaza el token por vencimiento y no inspecciona ni cambia el estado confirmado

#### Scenario: Token vencido por turno pasado
- **DADO** un turno ficticio cuyo inicio ya pasó y cuyo `expiresAt` vence al inicio
- **CUANDO** se llama a `GET` o `POST` con el token después del inicio
- **ENTONCES** el sistema rechaza el token por vencimiento antes de inspeccionar el estado del turno

#### Scenario: Token inexistente se rechaza
- **DADO** un valor opaco que no tiene registro asociado por su hash SHA-256
- **CUANDO** se llama a cualquier acción POST con ese valor
- **ENTONCES** el sistema responde token inexistente o inválido sin modificar un turno

### Requirement: El sistema permite cancelar mediante el servicio centralizado
Toda cancelación solicitada por token SHALL delegar en `AppointmentService.cancel(appointmentId, Actor.PATIENT, reason)`. El endpoint SHALL NOT cambiar directamente `Appointment.status`, `activeSlot`, publicar eventos ni registrar eventos por su cuenta.

#### Scenario: Cancelación de turno confirmado
- **DADO** un turno `CONFIRMED` y un token vigente con más de 24 horas hasta el inicio
- **CUANDO** se llama a `POST /api/public/appointments/{token}/cancel`
- **ENTONCES** el sistema delega a `AppointmentService.cancel` y el turno queda `CANCELLED` una sola vez

#### Scenario: Menos de 24 horas rechaza sin mutación y deriva al consultorio
- **DADO** un turno `PENDING` o `CONFIRMED` cuyo inicio está a menos de 24 horas
- **CUANDO** se llama a `POST .../cancel`
- **ENTONCES** el servicio centralizado rechaza la cancelación, informa que debe contactarse al consultorio y el turno conserva su estado, `activeSlot` y eventos sin publicación nueva

#### Scenario: Doble cancelación delega y es idempotente
- **DADO** un turno ya `CANCELLED` cuyo token sigue vigente
- **CUANDO** se llama nuevamente a `POST .../cancel`
- **ENTONCES** el sistema delega a `AppointmentService.cancel(..., Actor.PATIENT, reason)`, recibe su resultado idempotente y responde `ya cancelado` sin repetir publicación de eventos

#### Scenario: Repetición cancelada conserva idempotencia aun fuera de plazo
- **DADO** un turno ya `CANCELLED` cuyo inicio está a menos de 24 horas
- **CUANDO** se llama a `POST .../cancel`
- **ENTONCES** el sistema delega a `AppointmentService.cancel` y recibe `alreadyCancelled`, por lo que responde `ya cancelado` sin aplicar nuevamente la regla de 24 horas ni mutar nada

### Requirement: El sistema protege confirmaciones imposibles y muestra estados sin modificar
Un turno `CANCELLED` SHALL NOT poder pasar a `CONFIRMED`. Las operaciones GET confirm/cancel SHALL mostrar información del turno o del error sin cambiar estado ni delegar a servicios de dominio.

#### Scenario: Turno cancelado no se puede confirmar
- **DADO** un turno `CANCELLED` y un token vigente
- **CUANDO** se llama a `POST /api/public/appointments/{token}/confirm`
- **ENTONCES** el sistema rechaza la transición y mantiene el turno `CANCELLED`

#### Scenario: GET confirm solo informa
- **DADO** un token válido asociado a un turno
- **CUANDO** se llama a `GET /api/public/appointments/{token}/confirm`
- **ENTONCES** el sistema devuelve información del turno sin confirmarlo ni cambiar otro dato

#### Scenario: GET cancel solo informa
- **DADO** un token válido asociado a un turno
- **CUANDO** se llama a `GET /api/public/appointments/{token}/cancel`
- **ENTONCES** el sistema devuelve información del turno sin cancelarlo ni delegar a `AppointmentService.cancel`

### Requirement: C-09 emite el token y C-07 lo consume al reservar
C-09 SHALL ser el dueño de emitir tokens mediante `AppointmentTokenService.issue(Appointment)`: genera 32 bytes aleatorios en base64url, calcula SHA-256, persiste solo el hash y devuelve el token plano una única vez. Como `AppointmentToken.appointment` tiene una FK única, si el turno ya tiene un token el servicio SHALL reemplazar en el registro existente el hash y `expiresAt`, invalidando el token plano anterior; SHALL devolver el nuevo token plano una única vez y no SHALL conservarlo. C-07 SHALL llamar a este servicio durante la reserva y entregar una vez el token plano recibido, sin crear ni persistir por su cuenta otro registro.

#### Scenario: C-09 persiste solo el hash y devuelve el token plano
- **DADO** un turno ficticio creado por C-07 sin un token previo
- **CUANDO** C-09 ejecuta `issue(appointment)`
- **ENTONCES** persiste un `AppointmentToken` con un hash SHA-256 de 64 caracteres y no persiste el token plano, mientras devuelve una sola vez el valor aleatorio de 32 bytes codificado en base64url

#### Scenario: Reemitir reemplaza el token de un turno
- **DADO** un turno que ya tiene un registro `AppointmentToken` asociado
- **CUANDO** C-09 ejecuta nuevamente `issue(appointment)`
- **ENTONCES** reemplaza en el registro existente el hash y `expiresAt`, devuelve un nuevo token plano y hace inválido el token plano anterior

#### Scenario: El vencimiento emitido usa el menor de inicio y siete días
- **DADO** un turno ficticio cuyo inicio puede ocurrir antes o después de siete días desde `now`
- **CUANDO** C-09 ejecuta `issue(appointment)`
- **ENTONCES** persiste `expiresAt = min(startAt, now + 7 días)`

#### Scenario: C-09 encuentra el turno por hash
- **DADO** un token plano recibido en una acción pública y un registro emitido por C-09
- **CUANDO** C-09 calcula `SHA-256(tokenPlano)` y consulta `AppointmentTokenRepository`
- **ENTONCES** obtiene el turno asociado y continúa con las reglas de vencimiento y estado
