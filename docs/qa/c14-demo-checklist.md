# C-14 — Matriz de trazabilidad de pruebas

Alcance de esta sección: auditoría de la tarea 1.1. “Reutilizada” indica evidencia existente inspeccionada; no implica que todos los entornos estén verificados. Las brechas se conservan para las tareas indicadas y no se proponen tests paralelos. Los datos de los tests enlazados son ficticios.

| Regla C-14 / fuente | Test concreto existente | Evidencia y estado |
|---|---|---|
| Colisión de franja: una reserva gana, la otra recibe 409 y no deja efectos parciales ([public-booking](../../openspec/specs/public-booking/spec.md), [integridad-turnos](../../openspec/specs/integridad-turnos/spec.md)). | [`PublicBookingApiTests`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicBookingApiTests.java): `bookOverlapReturns409AndRollsBackWithoutNewRecords`, `concurrentReservationsSameSlotOne201One409`, `concurrentReservationsSameSlotDifferentDniOne201One409`; [`AppointmentPersistenceTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentPersistenceTest.java): `concurrentPersistencesCreateOnlyOneActiveAppointmentInSameSlot`. | **Evidencia reutilizada en H2**: HTTP 409, una sola reserva y ausencia de paciente/turno/outbox/token adicional en el conflicto simple; la prueba concurrente verifica una persistencia. No repetir estos escenarios. La equivalencia PostgreSQL real se registra aparte abajo. |
| Ventana pública de 24 horas a 30 días y rechazo de fechas fuera de rango ([public-booking](../../openspec/specs/public-booking/spec.md)). | [`PublicBookingWindowTests`](../../backend/src/test/java/com/turnosodontologia/schedule/PublicBookingWindowTests.java): `exactLimitsAndValuesFromProperties`; [`PublicBookingApiTests`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicBookingApiTests.java): `slotsOnMañanaReturnsOnlySlotsWithinWindowBorders`, `slotsTodayReturns422`, `slotsDaysOutsideWindowReturn422`, `slotsOnLastDayIsBoundedBy30DaysLimit`, `bookRejectsStartAtOutsideWindowWith422`. | **Parcial; reutilizar lo existente**: el test unitario comprueba el límite inferior exacto y el superior, además de los adyacentes; la API verifica el recorte del día y rechazos fuera de ventana, pero faltan los casos adyacentes exactos a nivel de endpoints. Completar solo esa brecha en la tarea 1.3. |
| DNI normalizado: aceptar solo 7 u 8 dígitos, rechazar letras/longitudes inválidas y reutilizar identidad equivalente ([validacion-datos](../../openspec/specs/validacion-datos/spec.md)). | [`PatientServiceTest`](../../backend/src/test/java/com/turnosodontologia/patient/PatientServiceTest.java): `rejectsInvalidDniWithoutPersisting`, `reusesSameIdentityForFormattedDnis`; [`CoreModelsIntegrityTests`](../../backend/src/test/java/com/turnosodontologia/CoreModelsIntegrityTests.java): `dniNormalizesAndEmailValidates`, `patientRepositoryPersistsNormalizedUniqueDni`; [`PatientControllerTest`](../../backend/src/test/java/com/turnosodontologia/patient/PatientControllerTest.java): `rejectsInvalidDniWithBusinessError`; [`PublicBookingApiTests`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicBookingApiTests.java): `bookRejectsInvalidPatientDataWith422`. | **Parcial; reutilizar lo existente**: están demostradas normalización, formato inválido, duplicado normalizado y rechazo HTTP. Falta evidenciar aceptación de DNI de 7 dígitos y los bordes de longitud en la ruta pública; verificar/completar solo esos bordes en la tarea 1.3. |
| Expiración del token (incluido `expiresAt == now`) precede al estado del turno ([token-confirmation](../../openspec/specs/token-confirmation/spec.md)). | [`AppointmentTokenServiceValidationTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentTokenServiceValidationTest.java): `expiredTokenWinsOverConfirmedAppointmentWithoutStatusAccess`, `exactlyExpiryIsExpiredBeforeCheckingAppointment`; [`PublicAppointmentTokenHttpTest`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicAppointmentTokenHttpTest.java): `expiredConfirmedTokenReturns410BeforeStatusChange`. | **Evidencia reutilizada** para vencimiento exacto, precedencia y respuesta HTTP 410. La capa de servicio usa `Clock.fixed`; el test end-to-end con token también usa `Instant.now()` y queda dentro de la brecha de determinismo de la tarea 1.2. |
| Confirmación/cancelación idempotentes, turno cancelado no confirmable y cancelación de paciente con menos de 24 horas ([token-confirmation](../../openspec/specs/token-confirmation/spec.md), [gestion-turnos](../../openspec/specs/gestion-turnos/spec.md)). | [`PublicAppointmentTokenHttpTest`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicAppointmentTokenHttpTest.java): `confirmedTokenReturns200AndDoesNotCallDomainService`, `cancelledTokenReturns409AndDoesNotCallDomainService`, `cancellationWithin24HoursReturnsSafeConflict`, `repeatedCancellationDelegatesAndReturnsAlreadyCancelled`; [`AppointmentTokenEndToEndTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentTokenEndToEndTest.java): `realEndpointCancelOnceThenIdempotentAndConfirmTracksOnePatientEvent`, `repeatedCancelledTokenWithinTwentyFourHoursReturnsAlreadyCancelledWithoutNewEvents`. | **Evidencia reutilizada, con brecha de reloj**: hay comprobaciones HTTP y end-to-end de estado/eventos y repetición. El test de servicio [`AppointmentServiceTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentServiceTest.java), `patientCancelAtExactlyTwentyFourHoursIsAllowed` y `patientCancelBeforeTwentyFourHoursIsRejectedWithoutMutation`, cubre el borde exacto y un segundo antes. Los flujos end-to-end usan tiempo de pared; fijarlo corresponde a 1.2, no a crear tests equivalentes. |
| Matriz de transiciones de estado y conservación de estado ante transición inválida ([gestion-turnos](../../openspec/specs/gestion-turnos/spec.md)). | [`AppointmentServiceTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentServiceTest.java): `validStateMatrixTransitionsWithoutInvalidMutation`; [`PublicAppointmentTokenHttpTest`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicAppointmentTokenHttpTest.java): `absentAndAttendedTokensAreRejectedWithoutDomainService`. | **Parcial**: se prueban todas las transiciones válidas declaradas desde PENDING/CONFIRMED y el rechazo CANCELLED→PENDING; no queda demostrada la matriz completa de transiciones inválidas desde cada estado terminal ni todas las consecuencias HTTP. Revisar/completar solo los huecos en la tarea 2.2. |
| FIFO de lista de espera, exclusión de no elegibles, una notificación persistida y ausencia de asignación automática ([lista-espera](../../openspec/specs/lista-espera/spec.md)). | [`WaitingListSelectionServiceTest`](../../backend/src/test/java/com/turnosodontologia/waiting/WaitingListSelectionServiceTest.java): `selectsOldestEligibleWaitingEntrySkippingOlderIneligibleAndNotifiedEntries`, `transitionsOnlyTheSelectedWaitingEntryAndPersistsTheState`; [`WaitingListNotificationIntegrationTest`](../../backend/src/test/java/com/turnosodontologia/waiting/WaitingListNotificationIntegrationTest.java): `marksOneFifoEntryAndCreatesUntypedPendingOutboxMessage`, `doesNotNotifyPatientWhoCancelledAndChoosesNextEligiblePatient`, `listenerConsumesManuallyPublishedEventAfterCommit`, `doesNotNotifyWhenNoEntryIsEligible`. | **Evidencia reutilizada para selección y efecto integrado**: se comprueban orden, exclusión, transición y mensaje simulado. Falta cerrar en la integración la garantía de una sola transición/aviso al repetir el evento y añadir una aserción explícita de que el turno liberado no se asigna ni confirma; la tarea 2.3 revisa esos huecos sin duplicar la selección ya probada. `WaitingListNotificationIntegrationTest` también usa `Instant.now()` (tarea 1.2). |
| Unicidad persistida de `active_slot`, colisión y reutilización después de cancelar ([integridad-turnos](../../openspec/specs/integridad-turnos/spec.md)). | [`CoreModelsIntegrityTests`](../../backend/src/test/java/com/turnosodontologia/CoreModelsIntegrityTests.java): `activeSlotUniqueRejectsSecondActiveTurnAndAllowsCancelledNulls`; [`AppointmentPersistenceTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentPersistenceTest.java): `rejectsSecondActiveAppointmentInSameSlot`, `cancelledSlotCanBeReusedAndNullSlotsAreUniqueNullable`. | **Evidencia reutilizada solo en H2 PostgreSQL-mode**: se verifica constraint, slots nulos tras cancelar y nueva reserva. **PostgreSQL real pendiente**: estos tests corren con perfil `test`/H2; no constituyen prueba del motor real. La repetición autorizada y su registro corresponden a la tarea 2.4; `database/README.md` mantiene pendiente cancelación y re-reserva. |
| Respuestas 4xx/5xx conservan status/mensaje público y no filtran detalles de excepciones ([public-booking](../../openspec/specs/public-booking/spec.md), [token-confirmation](../../openspec/specs/token-confirmation/spec.md), [outbox-reminders](../../openspec/specs/outbox-reminders/spec.md)). | [`PublicBookingApiTests`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicBookingApiTests.java): `bookRejectsInvalidPatientDataWith422`, `unrelatedDataIntegrityViolationDoesNotLeakDriverDetails`; [`AppointmentConflictHttpTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentConflictHttpTest.java): `unrelatedIntegrityViolationMapsToGeneric409WithoutDriverDetails`, `unexpectedIllegalArgumentAndIllegalStateUseFixed422Body`; [`PublicAppointmentTokenHttpTest`](../../backend/src/test/java/com/turnosodontologia/appointment/PublicAppointmentTokenHttpTest.java): `malformedTokenReturnsFixedNotFoundWithoutEchoingToken`; [`FinalErrorHandlerHttpTest`](../../backend/src/test/java/com/turnosodontologia/common/error/FinalErrorHandlerHttpTest.java): `unexpectedRuntimeReturnsFixed500BodyAndLogsSafeExceptionAndRootCauseTypes`, `responseStatusExceptionKeepsItsStatus`, `nonexistentRouteRemains404`, `unsupportedMethodRemains405`. | **Evidencia reutilizada**: status, errores fijos y exclusión de sentinels/clases/SQL se verifican en tests HTTP. `verify.md` documenta además las ejecuciones focalizadas y de suite de 3.1/3.2; no hace falta una batería genérica duplicada. |
| Recordatorios, consulta de bandeja y despacho explícito/idempotente simulado ([outbox-reminders](../../openspec/specs/outbox-reminders/spec.md)). | [`C10ReminderGenerationTests`](../../backend/src/test/java/com/turnosodontologia/outbox/C10ReminderGenerationTests.java): `eligiblePendingAndConfirmedAndStrictBoundsUseFixedClock`, `secondRunIsIdempotentAndDoesNotDuplicate`; [`C10OutboxHttpTests`](../../backend/src/test/java/com/turnosodontologia/outbox/C10OutboxHttpTests.java): `runDispatchAndGetWithRealSessionAndVerifyDatabase`, `dtoToleratesMessageWithoutAppointmentAndDispatchIsIdempotent`; [`C10ReminderReissueEndToEndTests`](../../backend/src/test/java/com/turnosodontologia/outbox/C10ReminderReissueEndToEndTests.java): `oldReservationTokenStopsResolvingAndReminderTokenResolvesPublicly`. | **Evidencia reutilizada para componentes del flujo**: generación, consulta, dispatch `SIMULATED_SENT` e idempotencia sí tienen test. **Brecha de demo manual**: no hay una prueba única ni checklist que demuestre el ciclo completo reserva→bandeja→confirmación/cancelación→lista de espera→recordatorio→dispatch. La documentación/precondiciones y su validación manual pertenecen a la tarea 3.3; los tests parciales no la sustituyen. No se envía correo real. |
| Tests sensibles al tiempo deben ser deterministas con `Clock.fixed` e instantes explícitos (diseño C-14). | [`PublicBookingWindowTests`](../../backend/src/test/java/com/turnosodontologia/schedule/PublicBookingWindowTests.java): `exactLimitsAndValuesFromProperties`; [`AppointmentTokenServiceValidationTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentTokenServiceValidationTest.java): `exactlyExpiryIsExpiredBeforeCheckingAppointment`; [`AppointmentServiceTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentServiceTest.java): tests de cancelación con reloj inyectado. | **Parcial**: hay ejemplos deterministas reutilizables, pero permanecen usos del reloj global en [`AppointmentTokenEndToEndTest`](../../backend/src/test/java/com/turnosodontologia/appointment/AppointmentTokenEndToEndTest.java) (`NOW = Instant.now()`), [`WaitingListNotificationIntegrationTest`](../../backend/src/test/java/com/turnosodontologia/waiting/WaitingListNotificationIntegrationTest.java) y [`ScheduleHttpSecurityTests`](../../backend/src/test/java/com/turnosodontologia/schedule/ScheduleHttpSecurityTests.java): `availabilityAndAdviceForBadRanges`. La tarea 1.2 es la brecha; no agregar variantes duplicadas. |
| Suite completa de backend y frontend reproducible (C-14, incluyendo npm test limpio). | [`FoundationApplicationTests`](../../backend/src/test/java/com/turnosodontologia/FoundationApplicationTests.java): `contextLoadsWithTestProfile`; [`build.test.ts`](../../frontend/src/admin/build.test.ts): `test script compiles both projects and keeps the public test suite in the run`. Las ejecuciones completas están en [`verify.md`](../../openspec/changes/test-suite-and-hardening/verify.md): `gradlew.bat test` y `npm.cmd ci` + `npm.cmd test` sin `dist/`/`node_modules`. | **Evidencia histórica reutilizada**: `verify.md` registra suite backend verde (230 tests en la verificación posterior) y frontend limpio (94/94). El gate final de suite/build y revisión de diff sigue pendiente en las tareas 4.1/4.2; volver a ejecutar allí, no crear otra prueba para el comando. |

## Resultado de la auditoría

Se reutilizan los tests existentes indicados arriba. Las brechas abiertas están acotadas a las tareas 1.2, 1.3, 2.2, 2.3, 2.4, 3.3, 4.1 y 4.2; la ejecución contra PostgreSQL real permanece separada de H2. Esta matriz no modifica producción ni amplía esas tareas.

## Checklist manual de demo end-to-end (tarea 3.3)

Este procedimiento complementa la matriz anterior: describe una demo manual de la bandeja local, no una prueba automatizada ni evidencia de PostgreSQL. Las rutas y cuerpos se contrastaron con `README.md` §§3.4–3.6, `PublicBookingController`, `PublicAppointmentTokenController`, `WaitingListController`, `OutboxController` y los tests HTTP de reserva, lista de espera y outbox.

### Precondiciones y datos

- [ ] Usar una base local de desarrollo autorizada y descartable, preparada según [database/README.md](../../database/README.md); no apuntar a una base desconocida ni a producción.
- [ ] Configurar localmente el perfil `local`, PostgreSQL y el usuario inicial ficticio según [README.md](../../README.md) §§3.1–3.3. Usar `APP_ADMIN_SEED=true` y `APP_DEMO_SEED=false` para este recorrido manual. No copiar credenciales a esta documentación, al repositorio ni a capturas.
- [ ] Usar exclusivamente personas/DNI ficticios y correos `@example.invalid`; no reutilizar datos reales. Registrar dos turnos públicos: uno para confirmar y luego cancelar, y otro que quede `PENDING` para el recordatorio.
- [ ] Para el turno de recordatorio, elegir un slot disponible cuyo inicio sea **posterior a 24 horas y no posterior a 48 horas** desde el momento de la prueba. Si no hay slot disponible en esa ventana, no afirmar que se generó un recordatorio.
- [ ] Abrir dos consolas PowerShell. En la primera, desde `backend`, arrancar como indica README §3.4:

  ```powershell
  cd backend
  .\gradlew.bat bootRun --args="--spring.profiles.active=local"
  ```

  Para una base vacía, seguir además la nota de primer arranque de [database/README.md](../../database/README.md) y README §3.5; quitar la configuración temporal de creación del esquema después del primer inicio.

### Salud y sesión de consola

- [ ] En la segunda consola, comprobar el endpoint sin autenticación documentado en README §3.5:

  ```powershell
  $base = 'http://localhost:8080'
  (Invoke-WebRequest -Uri "$base/api/health").StatusCode
  ```

  Debe responder con éxito. README no fija un cuerpo JSON contractual para esta comprobación.
- [ ] Iniciar sesión con las variables ficticias configuradas localmente para el seed. La sesión se conserva en `$sesion` para las rutas protegidas de outbox y lista de espera:

  ```powershell
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ email = $env:APP_ADMIN_EMAIL; password = $env:APP_ADMIN_PASSWORD } | ConvertTo-Json) `
    -SessionVariable sesion
  ```

### Reserva, confirmación y cancelación con aviso de lista de espera

- [ ] Consultar disponibilidad pública para una fecha dentro de la ventana permitida. `GET /api/public/slots?date=AAAA-MM-DD` devuelve `slots[]` con `start` y `end`; elegir un `start` realmente devuelto, no inventar un horario:

  ```powershell
  $fechaCancelacion = Read-Host 'Fecha local disponible (AAAA-MM-DD)'
  $slotsCancelacion = Invoke-RestMethod -Uri "$base/api/public/slots?date=$fechaCancelacion"
  $slotsCancelacion.slots | Select-Object start, end
  ```

- [ ] Inscribir primero una persona ficticia distinta en la lista pública para la fecha elegida. El cuerpo sigue `WaitingListRegistrationRequest`; el rango inclusivo debe cubrir el día local del turno:

  ```powershell
  $correoEspera = 'espera-c14@example.invalid'
  $espera = Invoke-RestMethod -Method Post -Uri "$base/api/public/waiting-list" `
    -ContentType 'application/json' `
    -Body (@{ dni = '90123457'; nombre = 'Persona'; apellido = 'Espera Ficticia'; email = $correoEspera; requestedDateFrom = $fechaCancelacion; requestedDateTo = $fechaCancelacion } | ConvertTo-Json)
  ```

- [ ] Crear el turno público usando otro DNI y correo ficticios, y un `startAt` copiado de la respuesta de disponibilidad. La ruta exacta es `POST /api/public/appointments`; la respuesta es mínima y **no** incluye el token:

  ```powershell
  $correoTurno = 'turno-c14@example.invalid'
  $inicioCancelacion = Read-Host 'Copiar start de un slot disponible'
  Invoke-RestMethod -Method Post -Uri "$base/api/public/appointments" `
    -ContentType 'application/json' `
    -Body (@{ nombre = 'Paciente'; apellido = 'Demo Ficticia'; dni = '90123456'; email = $correoTurno; startAt = $inicioCancelacion } | ConvertTo-Json)
  ```

- [ ] Consultar la bandeja autenticada con `GET /api/outbox/messages` y localizar el mensaje `BOOKING` dirigido a `$correoTurno`. Extraer el token de su cuerpo local; el test `PublicBookingApiTests.bookCreatesPendingAppointmentAndReturnsMinimalResponse` confirma que ahí están las rutas de confirmación y cancelación:

  ```powershell
  $mensajes = Invoke-RestMethod -Uri "$base/api/outbox/messages" -WebSession $sesion
  $mensajeTurno = $mensajes | Where-Object { $_.recipientEmail -eq $correoTurno -and $_.type -eq 'BOOKING' } | Sort-Object createdAt -Descending | Select-Object -First 1
  if ($mensajeTurno.body -notmatch '/api/public/appointments/([A-Za-z0-9_-]+)/confirm') { throw 'No se encontró el token ficticio en el mensaje BOOKING.' }
  $tokenTurno = $Matches[1]
  ```

- [ ] Confirmar explícitamente el turno por `POST /api/public/appointments/{token}/confirm` y comprobar respuesta satisfactoria/estado `CONFIRMED`:

  ```powershell
  Invoke-RestMethod -Method Post -Uri "$base/api/public/appointments/$tokenTurno/confirm"
  ```

- [ ] Cancelar ese mismo turno por `POST /api/public/appointments/{token}/cancel`. Comprobar respuesta satisfactoria, que el turno deja de estar activo y que el aviso queda en la bandeja `PENDING` para `$correoEspera`:

  ```powershell
  Invoke-RestMethod -Method Post -Uri "$base/api/public/appointments/$tokenTurno/cancel"
  $avisos = Invoke-RestMethod -Uri "$base/api/outbox/messages" -WebSession $sesion
  $avisos | Where-Object { $_.recipientEmail -eq $correoEspera } | Select-Object subject, status, body
  ```

  Confirmar también en `GET /api/waiting-list` (con la misma sesión) que la entrada de `$espera.id` pasó de `WAITING` a `NOTIFIED`:

  ```powershell
  $listaEspera = Invoke-RestMethod -Uri "$base/api/waiting-list" -WebSession $sesion
  $listaEspera | Where-Object { $_.id -eq $espera.id } | Select-Object id, status, requestedDateFrom, requestedDateTo
  ```

  El aviso solo informa la disponibilidad: no reasigna ni confirma el turno automáticamente.

### Recordatorio y despacho explícito e idempotente

- [ ] Reservar un **segundo** slot disponible para otra persona ficticia, verificando antes que su inicio sea `> ahora + 24 h` y `<= ahora + 48 h`; dejarlo activo en `PENDING` o `CONFIRMED` (no cancelarlo). Para consultar slots, usar `GET /api/public/slots?date=AAAA-MM-DD`; elegir un `start` devuelto y reservarlo con el mismo `POST /api/public/appointments`:

  ```powershell
  $fechaRecordatorio = Read-Host 'Fecha con un slot entre más de 24 h y hasta 48 h desde ahora (AAAA-MM-DD)'
  $slotsRecordatorio = Invoke-RestMethod -Uri "$base/api/public/slots?date=$fechaRecordatorio"
  $slotsRecordatorio.slots | Select-Object start, end
  $inicioRecordatorio = Read-Host 'Copiar start de un slot disponible dentro de la ventana de recordatorio'
  Invoke-RestMethod -Method Post -Uri "$base/api/public/appointments" `
    -ContentType 'application/json' `
    -Body (@{ nombre = 'Paciente'; apellido = 'Recordatorio Ficticio'; dni = '90123458'; email = 'recordatorio-c14@example.invalid'; startAt = $inicioRecordatorio } | ConvertTo-Json)
  ```

  La ventana y estados elegibles corresponden a `ReminderGenerationService.generate` y `C10ReminderGenerationTests`.
- [ ] Ejecutar generación explícita y repetirla para comprobar idempotencia. La ruta es `POST /api/outbox/reminders/run`; la respuesta contiene `created` y `failed`. La primera ejecución debe registrar el recordatorio del turno elegible y la segunda no debe duplicarlo (`created: 0`):

  ```powershell
  Invoke-RestMethod -Method Post -Uri "$base/api/outbox/reminders/run" -WebSession $sesion
  Invoke-RestMethod -Method Post -Uri "$base/api/outbox/reminders/run" -WebSession $sesion
  ```

- [ ] Revisar `GET /api/outbox/messages` y comprobar que el recordatorio y el aviso de lista de espera siguen `PENDING` antes del despacho. El despacho es exclusivamente una acción manual `POST /api/outbox/dispatch`; no se ejecuta en el arranque ni al generar recordatorios:

  ```powershell
  Invoke-RestMethod -Method Post -Uri "$base/api/outbox/dispatch" -WebSession $sesion
  Invoke-RestMethod -Method Post -Uri "$base/api/outbox/dispatch" -WebSession $sesion
  ```

  El primer despacho devuelve el número procesado; el segundo debe devolver `dispatched: 0`. Consultar de nuevo la bandeja y verificar que los mensajes procesados figuran como `SIMULATED_SENT` y no se duplican.
- [ ] Cerrar la sesión con `POST /api/auth/logout` si se mantiene la consola de demo. Detener la aplicación con `Ctrl+C`.

**Límite importante:** toda la bandeja es simulada. `SIMULATED_SENT` solo registra el despacho local explícito; no se envía correo, SMS ni WhatsApp y no se contacta ningún servicio externo. No presentar esta demo como entrega real ni como evidencia de PostgreSQL si se ejecutó sobre otro motor.
