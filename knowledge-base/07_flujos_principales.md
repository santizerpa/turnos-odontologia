# Flujos Principales

## Flujo 1: Login del odontólogo

**Disparador:** el odontólogo abre la aplicación.
**Actores:** odontólogo, backend, PostgreSQL.

1. Ingresa email y contraseña.
2. Backend valida credenciales y genera sesión/cookie.
3. Frontend carga agenda y turnos.
4. Si falla, muestra error sin revelar qué dato es incorrecto.

## Flujo 2: Reserva pública

**Disparador:** el paciente abre el enlace público.
**Actores:** paciente, frontend público, API, repositorio de pacientes, repositorio de turnos.

1. Consulta `GET /api/public/slots` dentro de 24hs–30 días.
2. API calcula slots libres con turnos activos y duración 30 minutos.
3. Carga nombre, apellido, DNI y email.
4. API reutiliza o crea Patient por DNI.
5. Crea Appointment en PENDING dentro de transacción.
6. Llama a `AppointmentTokenService.issue(appointment)` para obtener el token plano del enlace.
7. C-09 persiste solo el hash del token con vencimiento y devuelve el valor plano una sola vez.
8. Guarda OutboxMessage simulado con el enlace.
9. Si la franja estaba tomada, devuelve 409 y no crea entidades.

## Flujo 3: Confirmación/cancelación por link

**Disparador:** el paciente abre el link generado en la bandeja simulada.

```text
Paciente → Página pública → API REST → Servicio de turno → JPA → PostgreSQL
                                  ← estado actualizado / error
```

1. API calcula el hash SHA-256 del token, busca el registro y valida vigencia antes que el estado del turno. El token es reutilizable y no se consume al confirmar.
2. `GET /confirm` y `GET /cancel` solo muestran información y las acciones disponibles; no cambian el turno ni invocan servicios de dominio.
3. `POST /confirm` y `POST /cancel` ejecutan la operación; confirmar es idempotente y cancelar se permite hasta 24hs antes y es idempotente.
4. Toda cancelación invoca `AppointmentService.cancel(appointmentId, actor, reason)`, que valida la transición y la regla de 24hs si el actor es `PATIENT`.
5. El servicio pone `CANCELLED`, deja `active_slot` en `NULL`, registra `AppointmentEvent` y publica `AppointmentCancelledEvent` solo si la transición ocurrió.
6. Si ya estaba `CANCELLED`, responde "ya cancelado" sin publicar el evento.
7. Si el token no existe, responde `404`; si venció al inicio o a los 7 días, responde `410`; si el estado impide la operación, responde `409`.
8. Si la cancelación llega con menos de 24hs, responde fuera de plazo sin cambiar el estado.
9. La lista de espera se notifica por el listener de `AppointmentCancelledEvent` (Flujo 4).
10. Al ejecutar el recordatorio, el token se reemite con `issue()` y el enlace de reserva anterior queda invalidado.
11. El dispatch marca los mensajes como `SIMULATED_SENT`.
12. Devuelve una página con el estado final.

## Flujo 4: Lista de espera

**Disparador:** `AppointmentCancelledEvent` publicado por `AppointmentService.cancel(...)`.

1. El listener `@TransactionalEventListener(AFTER_COMMIT)` corre en una transacción nueva (`REQUIRES_NEW`).
2. Si el turno liberado no está dentro de la ventana pública inclusiva de 24 horas a 30 días, termina sin notificar.
3. Busca entradas `WAITING` elegibles por fecha local argentina, excluyendo al paciente del turno cancelado.
4. Toma la primera por `created_at` y, en empate, por `id`; una inscripción activa duplicada del mismo paciente se reutiliza.
5. Cambia estado a `NOTIFIED` y persiste un `OutboxMessage` dirigido al email guardado, con fecha y hora locales en el texto.
6. El dispatch procesa también mensajes cuyo tipo sea nulo.
7. No asigna ni confirma el turno automáticamente.
8. No modifica clases de C-05; solo consume el evento.

> Si el listener falla después del commit, la cancelación ya quedó firme. El aviso puede requerir reintento o registro para no perderlo.

## Flujo 5: Cancelación manual del odontólogo

**Disparador:** el odontólogo cancela desde la agenda.

1. Valida autenticación y turno.
2. Invoca `AppointmentService.cancel(appointmentId, DENTIST, reason)`; no cambia el estado directamente.
3. El servicio pone `CANCELLED`, deja `active_slot` en `NULL`, registra `AppointmentEvent` y publica `AppointmentCancelledEvent`.
4. El listener de C-11 notifica a la lista de espera.

## Casos de error

- Slot ocupado → 409 Conflict.
- DNI no válido → 422 con campo a corregir.
- Token vencido/usado → 410 Gone o 400 según mensaje del API.
- Turno pasado → 409/422, sin cambios.
- Base no disponible → 503 sin exponer credenciales.
