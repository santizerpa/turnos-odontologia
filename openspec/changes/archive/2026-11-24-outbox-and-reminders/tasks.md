# Tasks

## 1. Modelo aditivo y persistencia (TDD)

- [x] 1.1 Escribir tests rojos para `OutboxMessageType` con `BOOKING` y `REMINDER`, el constructor existente de cinco parámetros y el campo nullable; implementar la columna/enum aditiva sin cambiar ni eliminar ese constructor; verificar los tests JPA y mapeo H2.
- [x] 1.2 Escribir tests rojos para `markSimulatedSent(Instant)` en `PENDING` y `SIMULATED_SENT`; implementar la transición mínima que fija `sentAt` solo al primer envío y no modifica un mensaje ya enviado; verificar ambos casos con JUnit.
- [x] 1.3 Escribir pruebas de esquema para la restricción única `(appointment_id, type)` y consulta de existencia de un `REMINDER`; implementar consulta/repositorio y restricción sin alterar C-07; verificar duplicado persistido rechazado y mensajes de reserva existentes conservados.

## 2. Generación de recordatorios (TDD)

- [x] 2.1 Escribir tests rojos con reloj fijo para estados elegibles, `activeSlot`, ventana estricta inferior e inclusiva superior; implementar la selección `now + 24h < startAt <= now + 48h` usando `Clock`; verificar PENDING/CONFIRMED feliz y límites con H2.
- [x] 2.2 Escribir tests rojos de exclusión para `CANCELLED`, menos de 24 horas, `activeSlot` nulo y fuera de 48 horas; implementar el filtro mínimo; verificar que no se llama a `issue()` ni se crea ningún mensaje en esos casos.
- [x] 2.3 Escribir tests rojos para turno elegible a 36 horas: reservar token con `AppointmentTokenService.issue(appointment)`, construir links relativos confirm/cancel y persistir `REMINDER`; ejecutar ambos pasos en una transacción por turno y verificar que body con datos ficticios y nuevo token solo quedan persistidos si la transacción completa tiene éxito.
- [x] 2.4 Escribir tests rojos para segunda ejecución y restricción única; implementar comprobación previa por turno/tipo y persistencia protegida; verificar `created = 0` en la segunda corrida y que no se llama a `issue()` ni se duplica.
- [x] 2.5 Escribir tests rojos para dos turnos elegibles con `issue()` fallando para uno: implementar procesamiento por turno que revierta la reemisión y el `REMINDER` fallido sin detener el procesamiento; verificar `200`, `created = 1`, `failed = 1`, ausencia de REMINDER y validez del token anterior en el turno fallido, y recordatorio creado en el otro turno.
- [x] 2.6 Escribir test rojo de reemisión que comprueba que el token anterior de reserva deja de resolver y el nuevo sí; integrar solo con C-09 sin modificar C-07/C-09; verificar el resultado observable con tests end-to-end reales del servicio público.

## 3. Bandeja y dispatch protegidos (TDD)

- [x] 3.1 Escribir tests rojos para GET de mensajes con sesión y sin sesión; implementar DTO mínimo con los nueve campos solicitados y protección 401 sin sesión; verificar que no aparecen DNI, hashes, tokens persistidos ni datos internos.
- [x] 3.2 Escribir tests rojos para `POST /api/outbox/reminders/run`; implementar procesamiento por turno con `TransactionTemplate` o bean separado con `@Transactional(REQUIRES_NEW)` y respuesta siempre 200 con `{ created, failed }` durante fallos aislados; manejar errores C-10 con `@RestControllerAdvice(assignableTypes = <controladores C-10>)` y `@Order(Ordered.HIGHEST_PRECEDENCE)` para precedencia sobre `PatientExceptionHandler`; verificar 200 con conteos y 500 con mensaje general fijo solo para errores inesperados fuera del loop, sin mensajes internos ni stack traces.
- [x] 3.3 Escribir tests rojos para dispatch con PENDING y SIMULATED_SENT; implementar recorrido por PENDING, llamada a `markSimulatedSent(now)` y conteo observado; verificar primero cuenta enviada y segundo cuenta cero sin cambios de `sentAt`.
- [x] 3.4 Escribir tests de seguridad C-10 sin sesión para los tres endpoints; verificar 401 uniforme y ausencia de cuerpo con datos de mensajes o excepciones internas.

## 4. Integración y hardening

- [x] 4.1 Ya cubierto por tests C-10 existentes: ventana estricta en `C10ReminderGenerationTests#eligiblePendingAndConfirmedAndStrictBoundsUseFixedClock` y exclusiones en `C10ReminderGenerationTests#excludesCancelledAndInactiveOrOutsideWindow`; unicidad en `C10OutboxModelPersistenceTests#reminderHasPersistentUniqueAppointmentTypeAndExistsQuery` y ejecución idempotente en `C10ReminderGenerationTests#secondRunIsIdempotentAndDoesNotDuplicate`; reemisión en `C10ReminderReissueEndToEndTests#oldReservationTokenStopsResolvingAndReminderTokenResolvesPublicly` e aislamiento de emisión en `C10ReminderIsolationTests#isolatedIssueFailureKeepsOldTokenAndProcessesOtherAppointment`; dispatch en `C10OutboxHttpTests#runGenerateAndDispatchFlowIsProtectedAndIdempotent` y `C10OutboxHttpTests#dtoToleratesMessageWithoutAppointmentAndDispatchIsIdempotent`. Se agregó el test faltante sin `@Transactional` `C10ReminderIsolationTests#outboxSaveFailureRevertsReissueAndLeavesNoReminderWithoutTransactionalTest`: falla el guardado de un `REMINDER` con spy, el token anterior sigue resolviendo y no queda recordatorio.
- [x] 4.2 Suite completa ejecutada desde `backend/` con `./gradlew.bat test --rerun-tasks`; finalizó `BUILD SUCCESSFUL` en 1m 5s. No se reportaron fallos preexistentes ni fallos introducidos por C-10.
- [x] 4.3 Revisado con `git diff --stat origin/main` y `git diff --name-only origin/main`: C-10 modificó solo el paquete `outbox`, tests C-10 y el arreglo autorizado de `PublicBookingApiTests` fuera del paquete. No modificó C-07, C-09, C-11 ni `AuthSecurityConfiguration`. `OutboxMessageDto` expone exactamente los nueve campos permitidos (`id`, `appointmentId`, `recipientEmail`, `subject`, `body`, `status`, `type`, `createdAt`, `sentAt`) y no expone DNI, hashes, tokens ni datos de sesión; los tests HTTP C-10 verifican ausencia de esos campos.
- [x] 4.4 Revisada coherencia entre proposal, design, spec y tasks. `openspec validate outbox-and-reminders --strict` finalizó válido: `Change 'outbox-and-reminders' is valid`.

## Workflow follow-up

- Archivar el change después de que la revisión del proyecto apruebe los artefactos y la implementación posterior quede completada.
