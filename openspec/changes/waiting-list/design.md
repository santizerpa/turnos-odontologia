# Design

## Context

Véase `proposal.md` — Why y `specs/lista-espera/spec.md` para el contrato observable.

- `WaitingListEntry` ya existe en `waiting/`, extiende `AuditableEntity` y tiene paciente, límites de fecha, estado y posición; el repositorio está vacío y no hay controlador ni servicio de lista de espera.
- `AppointmentCancelledEvent` ya transporta `appointmentId`, `startAt` y `actor`. `AppointmentService.cancel(...)` lo publica solo en la primera transición y no modifica las clases de C-05 desde el consumidor.
- `AuthSecurityConfiguration` permite `/api/public/**`; otras rutas, incluyendo `/api/waiting-list`, requieren la sesión del odontólogo.
- `PatientService.upsert(...)` actualiza datos si el DNI existe. La inscripción pública debe reutilizar al paciente existente sin invocar ese upsert para no permitir cambios anónimos de perfil.
- C-10 está integrado en la rama objetivo: `OutboxMessage` conserva el constructor de cinco parámetros, agrega el tipo nullable y la unicidad `(appointment_id, type)`. C-11 usa el constructor de cinco parámetros para dejar el tipo nulo.
- La zona horaria del producto es `America/Argentina/Buenos_Aires`; las fechas de elegibilidad son fechas locales, no instantes comparados en UTC.

## Goals / Non-Goals

**Goals:**

- Implementar inscripción pública y consulta autenticada sin modificar perfiles existentes.
- Seleccionar una sola entrada `WAITING` elegible en FIFO y persistir conjuntamente `NOTIFIED` y el aviso simulado.
- Consumir el evento solo después del commit de la cancelación, en una transacción independiente.
- Mantener la compatibilidad con C-05 y con la unicidad de `OutboxMessage` introducida por C-10.

**Non-Goals:**

- Asignar o confirmar automáticamente el turno, confirmar la disponibilidad o reservarla para el paciente avisado.
- Reintentos durables del evento, envío SMTP, rate limiting, UI o cambios a las clases y al contrato de cancelación de C-05.
- Cambiar las columnas de `WaitingListEntry` o introducir una migración de base de datos en C-11.

## Decisions

### D1 — Endpoint público separado de la consulta administrativa

`POST /api/public/waiting-list` recibe DNI, nombre, apellido, email y límites de fecha opcionales. Se crea una entrada `WAITING`; `GET /api/waiting-list` permanece bajo la política autenticada ya existente. Se aprovecha el matcher `/api/public/**` y no se agrega una excepción específica en seguridad.

Se normaliza el DNI con la regla de `Patient`. Si ya existe, se asocia la entrada al paciente persistido y se ignoran los datos de perfil enviados; si no existe, se crea. No se usa `PatientService.upsert`, porque actualizaría un perfil existente en una solicitud anónima. La respuesta pública no indica si el paciente ya existía ni incluye el DNI.

Alternativa descartada: publicar `POST /api/waiting-list` manteniéndolo autenticado. Impediría la inscripción autónoma del paciente y contradice la decisión de alcance confirmada.

### D2 — FIFO y elegibilidad por fecha local

Las entradas se recorren por `created_at` ascendente y se elige la primera con estado `WAITING` cuyo `requested_date_from` sea nulo o no posterior a la fecha local del turno y cuyo `requested_date_to` sea nulo o no anterior a esa fecha. Los límites son inclusivos. Para empates de `created_at`, se usa `id` como orden estable. La columna `position` existente no es la fuente de orden en C-11.

La zona se toma de la configuración de agenda ya vigente; la comparación usa `event.startAt().atZone(zoneId).toLocalDate()`. Un rango inicial posterior al final se rechaza al inscribir. Si no hay candidato, el listener termina sin escrituras.

Alternativa descartada: elegir siempre la primera entrada WAITING sin considerar sus preferencias, porque ignoraría los límites que ya forman parte del modelo.

### D3 — Listener post-commit y servicio transaccional separado

Un bean listener recibe `AppointmentCancelledEvent` con `@TransactionalEventListener(phase = AFTER_COMMIT)` y delega a un servicio separado con `@Transactional(propagation = REQUIRES_NEW)`. El servicio resuelve el turno por `appointmentId`, determina el candidato y actualiza su estado junto con la creación del mensaje en la misma transacción. Si falla la persistencia del mensaje, la transición `NOTIFIED` revierte también.

La selección de la entrada se hará con bloqueo de escritura durante la transacción para que dos cancelaciones concurrentes no notifiquen accidentalmente a la misma entrada. La consulta debe conservar el orden FIFO y filtrar por estado y rango elegible.

Alternativa descartada: procesar dentro de la transacción de cancelación o modificar el publisher de C-05. El límite `AFTER_COMMIT` evita acoplar la confirmación de cancelación al módulo de espera y `REQUIRES_NEW` cumple el contrato del change.

### D4 — Mensaje enlazado al turno cancelado, sin tipo de recordatorio

El servicio carga el `Appointment` del evento y asocia ese turno al `OutboxMessage`; el destinatario es el email guardado del paciente elegido. Usa el constructor compatible de cinco parámetros, con estado inicial `PENDING`. No asigna `REMINDER`. Cuando C-10 esté integrado, el tipo queda nulo y la restricción `(appointment_id, type)` no colisiona con el recordatorio ya existente. No se amplía el enum en C-11.

Alternativa descartada: etiquetar el aviso como `REMINDER`, que mezclaría dos propósitos y puede violar la unicidad si ya existe un recordatorio para el turno.

### D5 — Contratos HTTP mínimos y errores

La inscripción valida los datos de identidad, email y rango de fechas antes de persistir; devuelve `201` para una entrada creada. `GET /api/waiting-list` devuelve entradas ordenadas por antigüedad con estado y fechas solicitadas, sin DNI. Las entradas inválidas se traducen a `400`; el controlador y el handler se mantienen en el paquete `waiting` y no se modifica el tratamiento global de errores ni autenticación.

### D6 — Pruebas y compatibilidad

Se usa TDD con datos ficticios. Las pruebas de persistencia/servicio corren con H2 en modo PostgreSQL; las HTTP verifican inscripción anónima, consulta `401` sin sesión y consulta autenticada. El listener se prueba publicando manualmente `AppointmentCancelledEvent` y verificando estado, destinatario y un solo `OutboxMessage`.

La rama objetivo contiene la ampliación de C-10. La verificación de C-11 confirma que el constructor de cinco parámetros deja `type` nulo y que un `REMINDER` previo no impide crear el aviso.

## Risks / Trade-offs

- [El listener corre después de que la cancelación ya quedó confirmada; un fallo no revierte la cancelación ni tiene reintento durable] → Mantener estado y Outbox atómicos; registrar y probar el fallo. Reintentos persistentes quedan fuera de C-11.
- [La inscripción pública permite abuso o envío de datos personales falsos] → Validar campos, no modificar un perfil existente, limitar la respuesta y no exponer DNI; rate limiting permanece fuera del alcance local.
- [Las inscripciones concurrentes para un DNI nuevo pueden competir por la unicidad] → Mantener la restricción única existente y, ante colisión, volver a resolver el paciente antes de crear la entrada, sin actualizar su perfil.
- [La unicidad de C-10 separa recordatorios tipados de avisos de lista de espera nullables] → Usar exclusivamente el constructor de cinco parámetros para C-11 y cubrir un recordatorio previo en una prueba de integración.

## Migration Plan

1. No se agregan columnas ni se requiere DDL nuevo para C-11; reutilizar el esquema de `WaitingListEntry` y `OutboxMessage`.
2. Implementar entidades/repository existentes, servicios, listener y controladores con sus pruebas; conservar intactas las clases de C-05.
3. Tras integrar C-10, ejecutar la suite relevante con `./gradlew test` y `./gradlew build`, incluyendo un caso con un recordatorio previo para el mismo turno.
4. Para rollback, retirar el módulo de API y listener de C-11 sin borrar entradas ni mensajes existentes; no hacer borrados físicos.

## Open Questions

Ninguna que cambie el contrato acordado. La entrega duradera/reintento del listener queda explícitamente fuera de este change.
