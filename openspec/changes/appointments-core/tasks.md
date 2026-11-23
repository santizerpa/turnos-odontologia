# Tasks

## 1. Contrato del dominio de turnos

- [ ] 1.1 Escribir tests unitarios rojos para creación con paciente, duración fija 30 y `active_slot=startAt`; verificar que pasan con `Appointment.create` reutilizado y datos ficticios.
- [ ] 1.2 Escribir tests rojos para la matriz de transiciones PENDING/CONFIRMED y estados terminales; verificar que las transiciones válidas pasan y las inválidas quedan rechazadas sin mutación.
- [ ] 1.3 Escribir tests rojos de cancelación para `PATIENT`, límite de 24 horas, `DENTIST`, segunda cancelación, liberación de `active_slot`, `AppointmentEvent` y ausencia de efectos repetidos; verificar el mínimo verde del servicio.
- [ ] 1.4 Agregar casos de triangulación: cancelación exactamente 24 horas, fuera de plazo, estados ya confirmados y cancelados; verificar que Clock.fixed determina el resultado y no existen esperas reales.

## 2. Servicio transaccional y persistencia

- [ ] 2.1 Escribir tests rojos de creación transaccional usando `DentistRepository`, `PatientRepository` y `AppointmentRepository`; verificar paciente existente, duración fija y creación PENDING con `active_slot` activo.
- [ ] 2.2 Implementar el mínimo de servicio transaccional para consulta y creación; verificar que dos persistencias concurrentes no crean dos turnos activos en H2 PostgreSQL mode y que uno recibe 409 en el boundary.
- [ ] 2.3 Escribir y pasar tests de reutilización de franja cancelada y unicidad nullable; verificar que cancelar deja NULL y que una nueva reserva persiste sin violación.
- [ ] 2.4 Implementar el evento `AppointmentCancelledEvent` con `appointmentId`, `startAt` y `actor`; verificar publicación una sola vez por transición real y ausencia en segunda cancelación.

## 3. API de agenda y estado

- [ ] 3.1 Escribir tests rojos HTTP protegidos para `GET /api/appointments?from=&to=`; verificar rango válido, agenda ordenada, rango inválido y reutilización de la sesión/autenticación existente.
- [ ] 3.2 Escribir tests rojos HTTP para `PATCH /api/appointments/{id}/status`; verificar delegación al servicio y rechazo de transiciones inválidas sin mutación directa del controlador.
- [ ] 3.3 Implementar y verificar 409 para conflicto de franja y respuestas 200/422/409 consistentes con la seguridad existente.
- [ ] 3.4 Reutilizar `AuthFixtures` en tests del endpoint protegido; verificar que no se crean datos reales de pacientes ni secretos y que la suite pasa con H2 PostgreSQL mode.

## 4. Integración y verificación

- [ ] 4.1 Ejecutar los tests específicos de appointment y, a continuación, el task relevante de Gradle; verificar que pasan junto con los tests existentes de C-02/C-03/C-04 sin modificar CHANGES.md.
- [ ] 4.2 Ejecutar `./gradlew build` desde el backend y verificar compilación y suite relevante; registrar cualquier fallo preexistente sin corregir fuera del change.
- [ ] 4.3 Confirmar por revisión estática que ningún endpoint o servicio muta `active_slot` directamente fuera de la creación/cancelación mantenida por el servicio y que PATCH no cambia estado directamente.
