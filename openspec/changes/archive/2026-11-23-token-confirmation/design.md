# Design

## Context

C-05 ya aporta turnos, estados y `AppointmentService.cancel(UUID, Actor, String)`, con verificación de 24 horas y idempotencia de cancelación. C-09 es dueño de emitir `AppointmentToken`; C-07 creará la reserva pública y llamará a su servicio emisor. C-09 agrega el acceso por enlace reutilizable sin consumir el token.

La entidad `AppointmentToken` ya contiene `appointment`, `tokenHash`, `expiresAt` y `createdAt`. El repositorio aún no expone búsqueda por hash. No se necesita una tabla adicional.

## Goals / Non-Goals

**Goals:**
- Validar el token antes que el estado o la vigencia del turno.
- Confirmar `PENDING → CONFIRMED` una sola vez desde el plano de dominio.
- Cancelar exclusivamente mediante `AppointmentService.cancel(..., Actor.PATIENT, ...)`.
- Mantener confirmación/cancelación idempotentes según el estado registrado.
- Resolver únicamente errores seguros al cliente sin exponer internals.

**Non-Goals:**
- C-07 no se implementa ni modifica en este change.
- No se implementan envío de mail, scheduler ni bandeja de salida.
- No se introduce autenticación en los enlaces públicos ni CSRF para ellos.
- No se cambia la máquina de estados ni el servicio de cancelación de C-05.

## Decisions

### Contrato C-09 / C-07

C-09 es el único dueño de emitir tokens mediante `AppointmentTokenService.issue(Appointment)`. El servicio genera 32 bytes aleatorios con `SecureRandom`, los codifica en base64url sin relleno, calcula `SHA-256(UTF-8(tokenPlano))`, calcula `expiresAt = min(startAt, now + 7 días)` y persiste el registro de `AppointmentToken` con el hash hexadecimal de 64 caracteres, el vencimiento y el turno. Como la entidad tiene una FK única por turno, si el turno ya tiene token se reemplazan en ese registro existente el hash y `expiresAt`; no se crea un segundo registro. El token plano anterior deja de valer y se devuelve al llamador el nuevo token plano una única vez; tampoco se persiste ni vuelve a exponerse.

C-07 solo llama a `issue(Appointment)` como parte de la reserva pública y entrega una vez el token plano recibido, por ejemplo en el enlace simulado. C-09 usa `AppointmentTokenRepository.findByTokenHash(hash)` para las acciones posteriores y no expone ni consulta el valor plano.

### Precedencia y vigencia

El flujo valida primero:
1. formato y presencia del token; 2. existencia mediante hash; 3. vencimiento.

Si no existe o el hash no coincide, responde token inválido. Si `now >= expiresAt`, responde token vencido. Solo después se inspecciona el estado del turno. Por ello, un token vencido gana sobre `CONFIRMED` o `CANCELLED`. El límite es exclusivo: el token deja de ser válido exactamente en `expiresAt`.

### GET informativo

Los métodos GET nunca mutan ni delegan a servicios de dominio. Presentan como respuesta JSON el estado asociado al token o el error correspondiente. Si el token es inexistente o vencido, no producen efectos.

### Acciones POST

- Confirmar: C-09 revisa primero el estado del turno antes de llamarlo. Si está `CONFIRMED`, responde `ya confirmado` y no llama al servicio. Si está `CANCELLED`, rechaza la confirmación y no llama al servicio. Solo si está `PENDING`, llama al método público `AppointmentService.changeStatus(id, CONFIRMED, Actor.PATIENT, reason)`. Ese método lanza `IllegalStateException` si el turno ya está `CONFIRMED` o `CANCELLED`, por lo que C-09 evita esas llamadas y mantiene la validación defensiva de C-05. Para `PENDING`, `changeStatus` valida la transición, aplica el cambio, registra un `AppointmentEvent` con `PENDING → CONFIRMED`, actor `PATIENT`, razón y reloj de C-05, y devuelve el turno. C-09 no registra por su cuenta otro evento.
- Cancelar: C-09 delega siempre a `AppointmentService.cancel(id, Actor.PATIENT, reason)`, incluso si el turno ya está `CANCELLED`. `cancel` devuelve `alreadyCancelled` antes de aplicar la regla de 24 horas y sin efectos repetidos. Para una cancelación nueva aplica 24 horas, cambia el estado, registra el `AppointmentEvent` y publica `AppointmentCancelledEvent` una sola vez. La razón puede ser “Cancelación solicitada por el paciente mediante token”; C-09 nunca cancela directamente ni publica eventos.

### Respuestas y errores

GET y POST responden únicamente JSON, sin distinguir el secreto del cliente.

- `404 Not Found`: el hash no corresponde a ningún `AppointmentToken`.
- `410 Gone`: `now >= expiresAt`, también para un turno pasado cuyo vencimiento venció al alcanzar el inicio.
- `409 Conflict`: el turno fue cancelado y no puede confirmarse, o falta menos de 24 horas para cancelar. La respuesta de cancelación fuera de plazo indica además contactar al consultorio.
- `200 OK`: la acción terminó con éxito o fue idempotente (`ya confirmado` / `ya cancelado`) y no hubo cambios adicionales.

Los detalles internos no se filtran.

## Risks / Trade-offs

- [CSRF] Los POST públicos no requieren autenticación y el proyecto deshabilita CSRF globalmente. Mitigación: no realizar acciones por GET y mantener el secreto en el POST; no se añade autenticación falsa.
- [Hash timing] La comparación se delega a una consulta exacta por hash persistido. Mitigación: no devolver el valor plano ni usar el token directamente como clave.
- [Reloj] TTL y 24 horas dependen del instante. Mitigación: `Clock` inyectable y pruebas deterministas.
- [Doble acción concurrente] Confirmación/cancelación pueden competir. Mitigación: operación transaccional y control de versión/estado de la entidad.

## Migration Plan

No requiere migración si `appointment_tokens.token_hash` ya existe como string hexadecimal único. C-09 agrega emisión, consulta por hash y búsqueda del registro, sin consumir ni eliminarlo.

## Open Questions

Las decisiones abiertas quedaron resueltas:
- el token válido se valida antes de inspeccionar el estado del turno;
- `expiresAt` es exclusivo y el token deja de ser válido cuando `now >= expiresAt`;
- los GET solo informan y no mutan;
- C-09 emite el token y C-07 solo lo consume durante la reserva; una reemisión reemplaza hash y vencimiento en el único registro por turno;
- toda cancelación, incluida la repetida, delega en `AppointmentService.cancel` para que el servicio resuelva la idempotencia.
