# Tasks

## 1. Controlador público — GET /api/public/slots

*Sección aplicable sin C-09.*

- [ ] 1.1 [red] `PublicBookingApiTests` con Clock fijo: `GET ?date=hoy` → 422; fecha parcialmente válida mañana → 200 solo con franjas válidas; primer día con límite now+24h → 200 con bordes correctos; último día con límite now+30d → 200 con bordes correctos; fecha fuera → 422; sin slots → 200 lista vacía.
- [ ] 1.2 [green mínimo] Implementar `PublicBookingController.slots(date)`: date requerido ISO; `from = max(inicioDelDia(date), publicWindow.earliest(clock))`; `to = min(finDelDia(date), publicWindow.latest(clock))`; si el día completo queda fuera, lanzar un error público fijo 422; llamar `AvailabilityService.freeSlots(dentistId, from, to)`.
- [ ] 1.3 [triangulación] Añadir casos: mañana a las 15:00 con ahora 10:00 devuelve 200 solo con franjas válidas; hoy 422; plus31d 422; date exacta en now+30d acepta solo hasta el límite exacto.
- [ ] 1.4 [refactor] Extraer acotación por día a métodos privados de C-07 y mantener verde con H2 PostgreSQL-mode.

## 2. Handlers públicos y validación de reserva previa

*Sección aplicable sin C-09. Esta sección debe completarse antes de POST.*

- [ ] 2.1 [red] Mover el caso `GET /api/public/slots` sin date a esta sección y escribirlo como 422 esperado; añadir date sin parsear → 422. Sin handler nuevo, conservar la baseline actual 400/errores internos.
- [ ] 2.2 [green mínimo] Crear `PublicBookingExceptionHandler` en `com.turnosodontologia.appointment` con `@RestControllerAdvice(assignableTypes = PublicBookingController.class)` y `@Order(Ordered.HIGHEST_PRECEDENCE)`. Mapear Bean Validation y parámetros faltantes/type mismatch a 422 con mensajes fijos; nunca usar `ex.getMessage()`.
- [ ] 2.3 [triangulación] Agregar 422 fijo para ventana, 422 fijo para franja fuera de grilla/bloqueada, 409 fijo para solapamiento, 503 fijo para cero o más de un Dentist, y 404 reservado para C-09 sin uso en C-07.
- [ ] 2.4 [green/refactor] Implementar la resolución previa de `publicDentist()` y la validación previa de `startAt`: resolver exactamente un Dentist; validar ventana; llamar `AvailabilityService.isOpenSlot(dentistId, startAt)` antes del upsert. Mensajes fijos en español y verde.
- [ ] 2.5 [riesgo] Documentar que el handler C-05 existente traduce toda `DataIntegrityViolationException` a franja ocupada, aunque el constraint sea otro; corregirlo quedaría fuera de C-07.

## 3. Flujo de reserva, token y Outbox

*Sección dependiente del merge de C-09 (`AppointmentTokenService.issue` en feat/augusto).* 

- [ ] 3.1 [red] POST válido → 201 con `PublicBookingResponse(startAt, durationMinutes, status)` sin ids internos ni token. Sin endpoint, baseline 404/405.
- [ ] 3.2 [green mínimo] Implementar `PublicBookingController.book()` con `@Valid PublicBookingRequest`; delegar a `PublicBookingService.book()`; responder 201 con el DTO mínimo.
- [ ] 3.3 [green mínimo] Implementar `PublicBookingService.book()` **en el paso green**, no en refactor: antes de transacción resolver Dentist, validar ventana y franja; dentro de transacción upsert Patient → `AppointmentService.create(..., CreatedBy.PATIENT)` → esperar id → `AppointmentTokenService.issue(appointment)` → crear Outbox PENDING → devolver `PublicBookingResponse`.
- [ ] 3.4 [triangulación] Reenviar el mismo startAt devuelto por GET /slots al POST y verificar 201 PENDING duration 30.
- [ ] 3.5 [triangulación] Verificar reutilización por DNI normalizado, sin crear una segunda identidad para el mismo DNI.
- [ ] 3.6 [triangulación] Verificar respuesta sin token, sin dentistId, patientId ni activeSlot.
- [ ] 3.7 [triangulación] DNI inválido, email inválido, nombre/apellido vacíos y startAt fuera de ventana → 422 sin persistencia.
- [ ] 3.8 [triangulación] startAt fuera de la grilla y startAt bloqueado por ScheduleBlock → 422 `La franja solicitada no está disponible` antes del upsert.
- [ ] 3.9 [triangulación] Solapamiento → 409 `La franja solicitada ya está ocupada`; verificar rollback sin paciente nuevo, turno nuevo ni OutboxMessage nuevo.
- [ ] 3.10 [triangulación] Después del merge de C-09, verificar que existe AppointmentToken asociado al turno emitido por `AppointmentTokenService.issue(appointment)`, sin testear ni calcular el hash; extraer del link un token base64url de exactamente 43 caracteres.
- [ ] 3.11 [triangulación] Verificar OutboxMessage PENDING con recipient_email normalizado y body que contiene `/api/public/appointments/{token}/confirm` y `/api/public/appointments/{token}/cancel`.
- [ ] 3.12 [refactor] Extraer construcción de links y respuesta mínima a métodos privados; mantener transacción sin catch interno y sin persistir/cálcular hash manualmente. Verde con `./gradlew test --tests com.turnosodontologia.appointment.PublicBookingApiTests`.

## 4. Verificación de integración

*Sección aplicable sin C-09 para las partes GET y validación previa; requiere C-09 para la reserva completa.*

- [ ] 4.1 [red/green] Ejecutar tests del paquete public-booking con Clock fijo y datos ficticios; cubrir bordes del primer y último día de ventana.
- [ ] 4.2 [green] Ejecutar `./gradlew test --tests com.turnosodontologia.appointment.PublicBookingApiTests` con H2 PostgreSQL-mode.
- [ ] 4.3 [integración] En la rama con C-09 mergeado, verificar reserva feliz, 409, DNI inválido, Outbox PENDING, rollback ante conflicto y ausencia de token en PublicBookingResponse.
- [ ] 4.4 [wrapper] Ejecutar `./gradlew test` y registrar el resultado. No ejecutar ni modificar C-09/C-10.

## Workflow follow-up

- Aplicables sin C-09: secciones 1, 2 y 4 solo en sus partes GET/handlers/validación previa.
- Dependen del merge de C-09: sección 3 completa e integración POST de sección 4.
- Continuar C-07 después del merge de C-09 mediante la integración de `AppointmentTokenService.issue(Appointment)` y el OutboxMessage.
- Archivar el change al cerrar y verificarlo, usando `openspec-archive-change`; C-10 no forma parte de este change ni debe quedar como sustituto del archivo C-07.
