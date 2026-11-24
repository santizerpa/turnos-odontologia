# Design

## Context

C-05 creó turnos y estados; C-09 ofrece emisión y reemplazo de tokens; C-07 puede crear mensajes de reserva. C-10 debe sumar una operación local controlada por el odontólogo y persistir recordatorios simulados sin conectar SMTP. Ver `proposal.md` para la motivación.

## Goals / Non-Goals

**Goals:**
- Generar como máximo un recordatorio por turno.
- Emitir un enlace vigente al crear el recordatorio.
- Usar un reloj inyectado para ventana y marcas temporales.
- Proveer listado y ejecución protegidos, sin exposición de datos sensibles.
- Mantener idempotencia en ejecución y despacho.

**Non-Goals:**
- SMTP, cola asíncrona, scheduler automático o reintentos.
- Modificar C-07, C-09 o C-11.
- Exponer DNI, hashes, tokens internos, datos de sesión o trazas de errores.

## Decisions

### Emisión de token propia para recordatorios

Cada recordatorio elegible llamará a `AppointmentTokenService.issue(appointment)` antes de construir su body. Los links serán relativos:

- `/api/public/appointments/{token}/confirm`
- `/api/public/appointments/{token}/cancel`

Se usará el mismo formato relativo definido por C-07. Esta decisión hace que el registro de C-09 sea reemplazado y invalida el link anterior de reserva, porque DD-08 establece que una reemisión reemplaza el hash vigente. Si ya existe un recordatorio, C-10 no llamará a `issue()`.

Para cada turno elegible, `AppointmentTokenService.issue(appointment)` y el guardado del `REMINDER` deberán ejecutarse dentro de la misma transacción, una transacción por turno. Si falla el guardado o `issue()`, la transacción se revierte completa: no queda `REMINDER` para ese turno y el token anterior conserva su validez. El procesamiento continúa con los demás turnos elegibles, de modo que un fallo aislado no cancela la ejecución completa.

La transacción por turno se implementará con `TransactionTemplate` o con un bean separado cuyo método declare `@Transactional(REQUIRES_NEW)`. Un `@Transactional` llamado desde la misma clase no abre una transacción nueva por el bypass de la auto-proxy, por lo que no se utilizará ese patrón.

**Alternativa descartada:** reutilizar el body o extraer el token del body del mail de C-07.
El body es texto frágil, no expone un token plano persistente por diseño y no sirve para turnos cargados por el odontólogo, que pueden no tener mail de reserva. Releer el body tampoco permitiría distinguir enlaces válidos ni compartir el token vigente de forma segura.

### Ventana de elegibilidad

El candidato debe cumplir simultáneamente:

- estado `PENDING` o `CONFIRMED`;
- `activeSlot` no nulo;
- `now + 24h < startAt <= now + 48h`.

Con límites exclusivos e inclusivos definidos así, exactamente `now + 24h` no recibe; exactamente `now + 48h` sí recibe. `Clock` será la fuente de `now` para selección, creación y despacho.

### Tipo y unicidad aditiva

`OutboxMessage` recibirá `OutboxMessageType type`, nullable en Java para compatibilidad con mensajes históricos. Los valores serán `BOOKING` y `REMINDER`: `BOOKING` queda definido para representar mensajes de C-07, pero no tendrá uso nuevo por ahora. C-07 seguirá usando su constructor existente de cinco parámetros, por lo que los mensajes creados por ese change conservarán `type = null`. C-10 agregará construcciones aditivas necesarias para REMINDER y no alterará el comportamiento de C-07.

La persistencia tendrá restricción única `(appointment_id, type)`. Antes de emitir token se comprobará la existencia de un `REMINDER` por turno. La consulta previa evita la creación normal y la restricción protege también concurrencia.

### Estado enviado simuladamente

Se agregará `markSimulatedSent(Instant sentAt)` a `OutboxMessage`. El método cambiará `PENDING` a `SIMULATED_SENT`, fijará `sentAt`, y no hará nada si ya estaba enviado. El endpoint de despacho leerá únicamente los PENDING y ejecutará ese mutador, asegurando repetir la operación sin duplicar cambios.

### API y privacidad

- `GET /api/outbox/messages` devuelve `id`, `appointmentId`, `recipientEmail`, `subject`, `body`, `status`, `type`, `createdAt`, `sentAt`.
- `POST /api/outbox/reminders/run` procesa cada turno elegible en su propia transacción y siempre devuelve `200 OK` con `{ "created": <cantidad>, "failed": <cantidad> }` durante el procesamiento normal. `failed` cuenta exclusivamente fallos aislados de `issue()` o guardado por turno. La respuesta no incluye mensajes internos ni stack traces.
- `POST /api/outbox/dispatch` devuelve la cantidad marcada como enviada.

Los tres quedan protegidos por sesión de odontólogo. Un acceso sin sesión debe devolver 401. Los controladores C-10 tendrán su propio manejador limitado de errores, declarado con `@RestControllerAdvice(assignableTypes = <controladores C-10>)` y `@Order(Ordered.HIGHEST_PRECEDENCE)`, para que solo intervenga en esos controladores y tenga precedencia sobre el manejador global `PatientExceptionHandler`. Devolverán mensajes generales; no delegarán ni extenderán manejadores existentes.

El manejador usará `500` solo para errores inesperados fuera del procesamiento por turno, por ejemplo una falla al consultar los turnos elegibles. Los fallos de `issue()` o guardado dentro de una transacción por turno se contabilizan en `failed`, revierten ese turno y no producen `500` ni detalles internos.

## Risks / Trade-offs

- Reemitir el token invalida el enlace anterior → Mitigación: documentarlo y entregar siempre el nuevo enlace en el recordatorio; C-09 conserva la misma API e idempotencia pública.
- Dos ejecuciones simultáneas pueden llegar al mismo turno → Mitigación: verificación previa más restricción única persistente y no llamar a `issue()` si el recordatorio ya existe.
- La generación deja el turno elegible repetidas veces → Mitigación: existencia de REMINDER por turno es el criterio único y precede a la emisión.
- No hay entrega real → Mitigación: estados simulados y endpoints locales explícitos según DD-03.
- Datos sensibles en el body o listado → Mitigación: DTO mínimo sin DNI, nombre completo, hash ni token persistido; el body contiene solo el texto y los enlaces relativos necesarios.

## Migration Plan

La implementación deberá agregar la columna/enum persistente y la restricción única, reutilizando el constructor existente de C-07. No requiere migración de datos compleja: los mensajes existentes deben recibir `type = null`. El despliegue local debe validar H2 y PostgreSQL. El rollback puede retirar C-10 sin modificar ni eliminar mensajes de reserva; no se conserva otro formato de tipo que pueda romper C-07.
