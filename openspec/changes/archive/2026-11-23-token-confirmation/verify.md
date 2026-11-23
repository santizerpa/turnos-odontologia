# Verificación — token-confirmation

## Resultado de la suite

Comando ejecutado desde `backend/`:

```text
./gradlew.bat test --rerun-tasks
```

- Estado: **BUILD SUCCESSFUL**
- Tests ejecutados: **124**
- Fallos: **0**
- Errores: **0**
- Omitidos: **0**

## Relación escenarios → tests

| Escenario de `specs/token-confirmation/spec.md` | Test que lo cubre |
|---|---|
| **Token válido confirmado una sola vez** | `PublicAppointmentTokenHttpTest.pendingTokenDelegatesToDomainChangeStatusOnce`; `AppointmentTokenEndToEndTest.realEndpointCancelOnceThenIdempotentAndConfirmTracksOnePatientEvent` |
| **Segunda confirmación sin efectos repetidos** | `PublicAppointmentTokenHttpTest.confirmedTokenReturns200AndDoesNotCallDomainService` |
| **Token vencido tiene precedencia sobre turno confirmado** | `AppointmentTokenServiceValidationTest.expiredTokenWinsOverConfirmedAppointmentWithoutStatusAccess`; `PublicAppointmentTokenHttpTest.expiredConfirmedTokenReturns410BeforeStatusChange` |
| **Token vencido por turno pasado** | `AppointmentTokenServiceValidationTest.exactlyExpiryIsExpiredBeforeCheckingAppointment` |
| **Token inexistente se rechaza** | `AppointmentTokenServiceValidationTest.hashAndLookupRejectInvalidOrMissingTokenBeforeAppointmentState`; `PublicAppointmentTokenHttpTest.missingTokenReturns404WithoutAppointmentInteraction` |
| **Cancelación de turno confirmado** | `PublicAppointmentTokenHttpTest.confirmedTokenCancelsThroughDomainServiceOnce`; `AppointmentTokenEndToEndTest.realEndpointCancelOnceThenIdempotentAndConfirmTracksOnePatientEvent` |
| **Menos de 24 horas rechaza sin mutación y deriva al consultorio** | `AppointmentServiceTest.patientCancelBeforeTwentyFourHoursIsRejectedWithoutMutation`; `PublicAppointmentTokenHttpTest.cancellationWithin24HoursReturnsSafeConflict` |
| **Doble cancelación delega y es idempotente** | `PublicAppointmentTokenHttpTest.repeatedCancellationDelegatesAndReturnsAlreadyCancelled`; `AppointmentTokenEndToEndTest.realEndpointCancelOnceThenIdempotentAndConfirmTracksOnePatientEvent`; `AppointmentServiceTest.dentistCancelAndRepeatedCancelAreIdempotent` |
| **Repetición cancelada conserva idempotencia aun fuera de plazo** | `AppointmentTokenEndToEndTest.repeatedCancelledTokenWithinTwentyFourHoursReturnsAlreadyCancelledWithoutNewEvents` |
| **Turno cancelado no se puede confirmar** | `PublicAppointmentTokenHttpTest.cancelledTokenReturns409AndDoesNotCallDomainService` |
| **GET confirm solo informa** | `PublicAppointmentTokenHttpTest.getConfirmAndCancelReturnMinimumAppointmentInfoWithoutDomainCalls`; `AppointmentTokenEndToEndTest.getConfirmAndCancelAreInformativeAndDoNotMutateStatusEventsOrServices` |
| **GET cancel solo informa** | `PublicAppointmentTokenHttpTest.getConfirmAndCancelReturnMinimumAppointmentInfoWithoutDomainCalls`; `AppointmentTokenEndToEndTest.getConfirmAndCancelAreInformativeAndDoNotMutateStatusEventsOrServices` |
| **C-09 persiste solo el hash y devuelve el token plano** | `AppointmentTokenServiceTest.issuePersistsOnlyHashAndReplacesExistingToken` |
| **Reemitir reemplaza el token de un turno** | `AppointmentTokenServiceTest.issuePersistsOnlyHashAndReplacesExistingToken` |
| **El vencimiento emitido usa el menor de inicio y siete días** | `AppointmentTokenServiceTest.issueExpiresAtIsStartWhenStartIsBeforeSevenDays`; `AppointmentTokenServiceTest.issuePersistsOnlyHashAndReplacesExistingToken` |
| **C-09 encuentra el turno por hash** | `AppointmentTokenServiceTest.repositoryFindsAppointmentAssociatedWithPlainTokenHash`; `AppointmentTokenServiceValidationTest.hashAndLookupRejectInvalidOrMissingTokenBeforeAppointmentState` |

## Cobertura directa

Todos los escenarios de `specs/token-confirmation/spec.md` tienen al menos una referencia de test directa. La repetición cancelada fuera de plazo está cubierta por el test end-to-end real sobre MockMvc.

No quedan escenarios marcados como `Sin test específico` ni `Escenario no cubierto`. Estado: **BUILD SUCCESSFUL**.
