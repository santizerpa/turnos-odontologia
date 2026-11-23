# Tasks

## 1. Contrato del dominio de turnos

- [x] 1.1 **Test rojo — creación y estados:** escribir tests unitarios para creación con duración fija 30, `active_slot=startAt`, matriz autoritativa de `knowledge-base/05_reglas_de_negocio.md` §Estados y rechazo de transición sin mutación.
- [x] 1.2 **Test rojo — cancelación:** escribir tests de cancelación por `PATIENT` con límite de 24 horas, por `DENTIST`, segunda cancelación, liberación de `active_slot`, `AppointmentEvent` y ausencia de efectos repetidos.
- [x] 1.3 **Implementación mínima — contrato:** implementar solo lo necesario para pasar el rojo de creación y estados; cancelar red/green debe llevar una implementación explícita de `AppointmentService.cancel(...)` que determine transición real, escriba `CANCELLED`, libere `active_slot`, registre evento y responda "ya cancelado" sin efectos repetidos.
- [x] 1.4 **Caso extra y triangulación:** agregar cancelación exactamente a 24 horas, fuera de plazo, estado ya cancelado y estados ya confirmados; usar `Clock.fixed` sin esperas reales.
- [x] 1.5 **Refactor verde:** extraer constantes/reglas de transición y cancelar solo tras tests verdes; ejecutar tests de la sección y mantenerlos verdes.

## 2. Servicio transaccional y persistencia

- [ ] 2.1 **Test rojo — persistencia:** escribir tests transaccionales con `DentistRepository`, `PatientRepository` y `AppointmentRepository` para paciente existente, duración fija 30 y creación PENDING con `active_slot` activo.
- [ ] 2.2 **Implementación mínima — 409 sin POST:** implementar el mínimo de servicio transaccional para consulta y creación, sin requerir `POST /api/appointments`; probar que dos persistencias concurrentes no crean dos turnos activos en H2 PostgreSQL mode y que el conflicto materializado como excepción de servicio o `DataIntegrityViolationException`/restricción única llega a HTTP 409 en el boundary.
- [ ] 2.3 **Caso extra — franja reutilizable:** probar reutilización de franja cancelada y unicidad nullable; verificar que cancelar deja `active_slot=NULL` y una nueva creación persiste.
- [ ] 2.4 **Implementación explícita — evento:** implementar `AppointmentCancelledEvent` con `appointmentId`, `startAt` y `actor`; publicar una sola vez por transición real y no publicar al repetir la cancelación.
- [ ] 2.5 **Refactor verde:** simplificar el flujo transaccional sin cambiar contratos; ejecutar tests y mantenerlos verdes.

## 3. API de agenda y estado

- [ ] 3.1 **Test rojo — GET protegido:** escribir tests HTTP para `GET /api/appointments?from=&to=` con rango válido, agenda ordenada, rango inválido y 401 sin sesión según C-03.
- [ ] 3.2 **Test rojo — PATCH protegido:** escribir tests HTTP para `PATCH /api/appointments/{id}/status`: status=CANCELLED invoca `AppointmentService.cancel(..., DENTIST, reason)`, repetición devuelve "ya cancelado" sin eventos, transición inválida usa el handler/código existente y inexistente devuelve 404.
- [ ] 3.3 **Implementación mínima — boundary 409:** implementar el mínimo HTTP sin `POST /api/appointments`; verificar el conflicto de franja en el boundary 409, además de las respuestas 200/422/404/401 consistentes con C-03 y el handler existente.
- [ ] 3.4 **Caso extra — seguridad y datos:** agregar pruebas de 401 para GET/PATCH sin sesión y reutilizar `AuthFixtures`; verificar datos ficticios, sin secretos reales y H2 PostgreSQL mode.
- [ ] 3.5 **Refactor verde:** reducir duplicación de pruebas/DTO/controlador sin alterar HTTP; ejecutar tests y mantenerlos verdes.

## 4. Integración y verificación

- [ ] 4.1 Ejecutar los tests específicos de appointment y el test relevante de Gradle; incluir pero no asumir verde para `ScheduleHttpSecurityTests.availabilityAndAdviceForBadRanges`, que es un único test rojo preexistente de C-04, quedó sin modificar y fuera de C-05.
- [ ] 4.2 Ejecutar `./gradlew build` desde el backend; no afirmar que pasan todos los tests C-04: registrar explícitamente que `ScheduleHttpSecurityTests.availabilityAndAdviceForBadRanges` es un único test rojo preexistente de C-04, no modificado ni perteneciente a C-05.
- [ ] 4.3 **Refactor y revisión verde:** realizar solo ajustes de estructura dentro de C-05 que mantengan la suite verde, y confirmar por revisión estática que ningún endpoint o servicio muta `active_slot` fuera de la creación/cancelación mantenida por el servicio y que PATCH no cambia estado directamente.
