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
6. Crea AppointmentToken de al menos 128 bits con vencimiento y persiste solo su hash.
7. Guarda OutboxMessage simulado.
8. Si la franja estaba tomada, devuelve 409 y no crea entidades.

## Flujo 3: Confirmación/cancelación por link

**Disparador:** el paciente abre el link generado en la bandeja simulada.

```text
Paciente → Página pública → API REST → Servicio de turno → JPA → PostgreSQL
                                  ← estado actualizado / error
```

1. API obtiene el hash del token y valida vigencia, turno y estado; el token no se consume al confirmar.
2. El `GET` solo muestra la página con el estado y las acciones disponibles; el cambio de estado requiere `POST`.
3. Confirma o cancela según estado y fecha. Confirmar es idempotente; cancelar se permite hasta 24hs antes y es idempotente.
4. Toda cancelación invoca `AppointmentService.cancel(appointmentId, actor, reason)`, que valida la transición y la regla de 24hs si el actor es `PATIENT`.
5. El servicio pone `CANCELLED`, deja `active_slot` en `NULL`, registra `AppointmentEvent` y publica `AppointmentCancelledEvent` solo si la transición ocurrió.
6. Si ya estaba `CANCELLED`, responde "ya cancelado" sin publicar el evento.
7. Si la cancelación llega con menos de 24hs, responde fuera de plazo y no cambia el estado.
8. La lista de espera se notifica por el listener de `AppointmentCancelledEvent` (Flujo 4).
9. Devuelve una página con el estado final.

## Flujo 4: Lista de espera

**Disparador:** `AppointmentCancelledEvent` publicado por `AppointmentService.cancel(...)`.

1. El listener `@TransactionalEventListener(AFTER_COMMIT)` corre en una transacción nueva (`REQUIRES_NEW`).
2. Busca entradas WAITING elegibles.
3. Toma la primera por `created_at`.
4. Cambia estado a NOTIFIED.
5. Persiste OutboxMessage dirigido al paciente.
6. No asigna ni confirma el turno automáticamente.
7. No modifica clases de C-05; solo consume el evento.

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
