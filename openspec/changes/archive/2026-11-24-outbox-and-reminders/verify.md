# Verificación — outbox-and-reminders

## Resultado de la suite

- Comando: `gradlew.bat test --rerun-tasks`
- Directorio de ejecución: `backend/`
- Resultado: `BUILD SUCCESSFUL`
- Duración: 1 m 14 s
- Pruebas C-10 relevantes: sin fallos ni errores.
- Corte aplicado: agregado únicamente el test C-10 para el escenario de error inesperado; no se modificó código de producción.

## Relación escenarios / tests

| Escenario de la spec | Test que lo cubre |
|---|---|
| Turno a 36 horas recibe recordatorio feliz | `com.turnosodontologia.outbox.C10ReminderGenerationTests#eligiblePendingAndConfirmedAndStrictBoundsUseFixedClock` |
| Turno cancelado no recibe recordatorio | `com.turnosodontologia.outbox.C10ReminderGenerationTests#excludesCancelledAndInactiveOrOutsideWindow` |
| Turno confirmado a menos de 24 horas no recibe recordatorio | `com.turnosodontologia.outbox.C10ReminderGenerationTests#excludesCancelledAndInactiveOrOutsideWindow` |
| Exactamente ahora más 24 horas no recibe | `com.turnosodontologia.outbox.C10ReminderGenerationTests#eligiblePendingAndConfirmedAndStrictBoundsUseFixedClock` |
| Exactamente ahora más 48 horas sí recibe | `com.turnosodontologia.outbox.C10ReminderGenerationTests#eligiblePendingAndConfirmedAndStrictBoundsUseFixedClock` |
| Ejecutar dos veces no duplica | `com.turnosodontologia.outbox.C10ReminderGenerationTests#secondRunIsIdempotentAndDoesNotDuplicate` |
| Restricción persistente rechaza duplicado concurrente | `com.turnosodontologia.outbox.C10OutboxModelPersistenceTests#reminderHasPersistentUniqueAppointmentTypeAndExistsQuery` |
| Dos turnos elegibles y issue falla para uno | `com.turnosodontologia.outbox.C10ReminderIsolationTests#isolatedIssueFailureKeepsOldTokenAndProcessesOtherAppointment` |
| Error inesperado fuera del procesamiento por turno | `com.turnosodontologia.outbox.C10OutboxErrorHttpTests#unexpectedReminderGenerationFailureUsesFixedSafeResponse` |
| El link anterior deja de funcionar tras el recordatorio | `com.turnosodontologia.outbox.C10ReminderReissueEndToEndTests#oldReservationTokenStopsResolvingAndReminderTokenResolvesPublicly` |
| Despacho feliz | `com.turnosodontologia.outbox.C10OutboxHttpTests#runDispatchAndGetWithRealSessionAndVerifyDatabase` y `com.turnosodontologia.outbox.C10OutboxHttpTests#dtoToleratesMessageWithoutAppointmentAndDispatchIsIdempotent` |
| Segundo despacho no repite cambios | `com.turnosodontologia.outbox.C10OutboxModelPersistenceTests#simulatedSentTransitionOnlyChangesPendingOnce` y `com.turnosodontologia.outbox.C10OutboxHttpTests#dtoToleratesMessageWithoutAppointmentAndDispatchIsIdempotent` |
| GET protegido sin sesión da 401 | `com.turnosodontologia.outbox.C10OutboxHttpTests#allOutboxEndpointsRejectRequestsWithoutSession` |
| GET autenticado lista el formato permitido | `com.turnosodontologia.outbox.C10OutboxHttpTests#runDispatchAndGetWithRealSessionAndVerifyDatabase` |
| Ejecución exitosa siempre responde 200 con conteos | `com.turnosodontologia.outbox.C10OutboxHttpTests#runDispatchAndGetWithRealSessionAndVerifyDatabase` |
| Fallo de issue se registra como fallo por turno | `com.turnosodontologia.outbox.C10ReminderIsolationTests#isolatedIssueFailureKeepsOldTokenAndProcessesOtherAppointment` |

## Cobertura completada

El escenario **Error inesperado fuera del procesamiento por turno** quedó cubierto por `C10OutboxErrorHttpTests`:

- crea una sesión real de odontólogo;
- reemplaza `ReminderGenerationService` con `@MockBean` y provoca `RuntimeException("detalle-interno-secreto")`;
- verifica `500 Internal Server Error`;
- verifica el mensaje general fijo `Error interno al procesar la bandeja`;
- verifica ausencia del detalle secreto y rastros `Exception` o `at com.`.

El test nació en **verde**: la implementación existente de `OutboxExceptionHandler#unexpected` ya satisfizo el contrato. No fue necesario modificar código de producción.

Todos los escenarios listados en esta tabla tienen al menos un test asociado.

## Cierre de este corte

Se agregó solo el test C-10 solicitado. No se modificó código de producción ni archivos de otros changes.
