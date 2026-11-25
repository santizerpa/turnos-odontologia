# Tareas

## 1. Auditar cobertura y hacer deterministas los casos temporales

- [ ] 1.1 Contrastar cada regla C-14 con la prueba existente y completar una matriz breve de trazabilidad en `docs/qa/c14-demo-checklist.md`; marcar si se reutiliza evidencia existente o si falta un caso, verificando que cada fila apunte a un test concreto y no proponga duplicados.
- [ ] 1.2 Sustituir en las pruebas de token, notificación de lista de espera y seguridad de agenda los usos pertinentes de `Instant.now()`/`LocalDate.now()` por `Clock.fixed` e instantes explícitos; verificar los tests afectados con `.\gradlew.bat test --tests <clase>` (Windows) o `./gradlew test --tests <clase>`.
- [ ] 1.3 Completar solo los bordes de ventana pública y validación DNI que no queden ya demostrados por `PublicBookingWindowTests`, `PublicBookingApiTests` y `PatientControllerTest`; verificar límites exactos y rechazo adyacente con los tests focalizados y sin cambiar reglas existentes.

## 2. Verificar invariantes de turnos, tokens y lista de espera

- [ ] 2.1 Asegurar pruebas de persistencia H2 PostgreSQL-mode para unicidad de `active_slot`, colisión sin segundo turno y reutilización de franja cancelada, comprobando filas/estado después de cada operación; verificar `CoreModelsIntegrityTests` y `AppointmentPersistenceTest`.
- [ ] 2.2 Revisar la matriz completa de transiciones y efectos de confirmación/cancelación (incluidos token vencido, repetición y rechazo de cancelación con menos de 24 horas); corregir o agregar únicamente casos faltantes y verificar `AppointmentServiceTest`, `PublicAppointmentTokenHttpTest` y `AppointmentTokenEndToEndTest`.
- [ ] 2.3 Confirmar selección FIFO entre varias entradas elegibles, exclusión de no elegibles, una transición/aviso por evento y ausencia de duplicados en la integración; verificar `WaitingListSelectionServiceTest` y `WaitingListNotificationIntegrationTest`.
- [ ] 2.4 Cuando haya una instancia PostgreSQL local autorizada, repetir con datos ficticios la unicidad de `active_slot` y la re-reserva posterior a cancelación, validar el esquema según `database/README.md` y registrar motor, comandos y resultado (sin credenciales) en la sección de evidencia del checklist; si no está disponible, dejar el resultado explícitamente pendiente y no afirmar paridad real.

## 3. Endurecer límites HTTP y documentar la demo simulada

- [ ] 3.1 Revisar pruebas y handlers de reserva pública, turnos y tokens; agregar aserciones para status/mensaje contractual y ausencia de mensaje de excepción, clase/stacktrace o detalle SQL/driver; corregir producción solo ante una fuga reproducible y verificar `PublicBookingApiTests`, `PublicAppointmentTokenHttpTest` y `AppointmentConflictHttpTest`.
- [ ] 3.2 Revisar pruebas y handlers de pacientes, agenda, lista de espera, outbox y seguridad; agregar aserciones representativas contra exposición de datos personales, tokens o detalles internos; corregir producción solo ante una fuga reproducible y verificar los tests HTTP focalizados preservando el contrato.
- [ ] 3.3 Completar `docs/qa/c14-demo-checklist.md` con precondiciones ficticias, arranque documentado, inspección de `GET /api/outbox/messages`, uso del enlace de confirmación/cancelación, despacho explícito con `POST /api/outbox/dispatch` y comprobación de idempotencia; verificar rutas y nombres contra `README.md`, `OutboxController` y los tests, dejando claro que no se envía correo real.

## 4. Verificación integral y evidencia de cierre

- [ ] 4.1 Ejecutar toda la suite mediante Gradle Wrapper (`.\gradlew.bat test` en Windows o `./gradlew test` en macOS/Linux), guardar salida e informe HTML y confirmar `BUILD SUCCESSFUL` sin flags que omitan tests.
- [ ] 4.2 Ejecutar el build completo con el wrapper (`.\gradlew.bat build` o `./gradlew build`), revisar el diff de código/tests/documentación y registrar resultados H2, PostgreSQL (o pendiente), errores HTTP y demo; solicitar la revisión/sign-off correspondiente a gobernanza HIGH sin modificar `CHANGES.md`.

## Workflow follow-up

- Archivar el change solo después de revisión de la evidencia y de que el responsable acepte explícitamente cualquier validación PostgreSQL que permanezca pendiente.
