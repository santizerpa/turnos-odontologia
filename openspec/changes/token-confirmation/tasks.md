# Tasks

## 1. Emisión y persistencia de token (TDD)

- [x] 1.1 Test rojo y mínimo verde: probar `AppointmentTokenService.issue(Appointment)` y persistir solo el hash. Añadir el test que verifica generación opaca de 32 bytes en base64url sin relleno, SHA-256 hexadecimal de 64 caracteres, persistencia de `AppointmentToken` y devolución única del token plano sin persistirlo. Añadir un caso donde el turno ya tenga token y comprobar que se reemplazan el hash y `expiresAt` en el mismo registro, sin crear otro; verificar que el token anterior deja de resolver y el nuevo devuelve el plano una vez. Implementar `issue(Appointment)` como propietario C-09.
- [x] 1.2 Test rojo y mínimo verde: probar el vencimiento emitido con turnos que comienzan antes y después de siete días desde `now`. Implementar `expiresAt = min(startAt, now + 7 días)` para ambos casos.
- [x] 1.3 Test rojo y mínimo verde: probar `AppointmentTokenRepository.findByTokenHash(String tokenHash)` y la búsqueda del turno asociado desde el hash SHA-256 del valor plano. Implementar la consulta y mantener el hash único si la restricción aún no existe.

## 2. Validación de token y precedencia (TDD)

- [x] 2.1 Test rojo y mínimo verde: probar presencia/formato y hash SHA-256 del token recibido. Implementar el cálculo y la búsqueda antes de inspeccionar estado o vigencia del turno.
- [x] 2.2 Test rojo y mínimo verde: probar token inexistente o inválido para una acción POST. Implementar `404 Not Found` sin modificar turnos.
- [x] 2.3 Test rojo y mínimo verde: probar token vencido exactamente en `expiresAt` y turno pasado cuyo `expiresAt` vence al inicio. Implementar la regla exclusiva `now < expiresAt` y `410 Gone` antes de inspeccionar estado.
- [x] 2.4 Test rojo y mínimo verde: probar que un token vencido gana sobre un turno `CONFIRMED` e implementar la precedencia para que no se inspeccione ni cambie su estado.

## 3. Confirmación por enlace (TDD)

- [ ] 3.1 Test rojo y mínimo verde: probar el escenario de turno `PENDING` con token válido llamando a `POST /confirm`. Implementar que C-09 revise el estado y solo PENDING delegue en `AppointmentService.changeStatus(id, CONFIRMED, Actor.PATIENT, reason)`; verificar que el método ya registra el `AppointmentEvent` y C-09 no registra otro evento.
- [ ] 3.2 Test rojo y mínimo verde: probar una segunda confirmación con turno `CONFIRMED` y token vigente. Implementar la respuesta `ya confirmado` sin llamar a `changeStatus`, sin cambios ni eventos.
- [ ] 3.3 Test rojo y mínimo verde: probar turno `CANCELLED` con token vigente en `POST /confirm`. Implementar el rechazo sin llamar a `changeStatus` y mantener el turno cancelado.

## 4. Cancelación por enlace (TDD)

- [ ] 4.1 Test rojo y mínimo verde: probar cancelación de turno `CONFIRMED` con token vigente y más de 24 horas hasta el inicio. Implementar la delegación siempre en `AppointmentService.cancel(id, Actor.PATIENT, reason)` y verificar la transición centralizada una sola vez.
- [ ] 4.2 Test rojo y mínimo verde: probar cancelación de turno `PENDING` o `CONFIRMED` a menos de 24 horas. Implementar que el servicio central rechace sin mutación ni publicación, respondiendo que debe contactarse al consultorio.
- [ ] 4.3 Test rojo y mínimo verde: probar la doble cancelación de un turno `CANCELLED` con token vigente. Implementar la delegación en `AppointmentService.cancel(..., Actor.PATIENT, reason)` y la respuesta `ya cancelado` sin efectos repetidos.
- [ ] 4.4 Test rojo y mínimo verde: probar repetición cancelada fuera de plazo a menos de 24 horas. Implementar la delegación siempre y aceptar `alreadyCancelled` antes de la regla de 24 horas, sin mutar ni aplicar nuevamente el plazo.

## 5. API pública GET y respuestas (TDD)

- [ ] 5.1 Test rojo y mínimo verde: probar `GET /api/public/appointments/{token}/confirm` con token válido asociado a un turno. Implementar una respuesta informativa sin cambiar estado ni delegar a servicios de dominio.
- [ ] 5.2 Test rojo y mínimo verde: probar `GET /api/public/appointments/{token}/cancel` con token válido asociado a un turno. Implementar una respuesta informativa sin cancelar, mutar ni llamar a `AppointmentService.cancel`.
- [ ] 5.3 Test rojo y mínimo verde: probar `GET /confirm` y `GET /cancel` con token inexistente o vencido. Implementar `404 Not Found` o `410 Gone` sin efectos ni delegación.
- [ ] 5.4 Test rojo y mínimo verde: probar respuestas JSON seguras para éxito, idempotencia y errores. Implementar cuerpos JSON sin exponer detalles internos.

