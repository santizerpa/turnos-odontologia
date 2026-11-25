# Verificación — `seed-and-demo` (C-13)

Fecha de ejecución: 2026-11-24/25. Entorno: backend Spring Boot con perfil `test`, H2 en memoria con modo PostgreSQL. No se probó PostgreSQL ni un arranque real de `bootRun` con el perfil `local` y datos persistidos.

## Comandos ejecutados y resultados exactos

| Comando | Directorio | Exit code | Resultado |
|---|---|---:|---|
| `.\gradlew.bat test --rerun-tasks` | `backend/` | 0 | `BUILD SUCCESSFUL in 2m 39s`; 58 XML, 216 tests, 0 failures, 0 errors, 0 skipped |
| `.\gradlew.bat test --tests 'com.turnosodontologia.seed.*'` | `backend/` | 0 | `BUILD SUCCESSFUL in 51s`; 35 tests del paquete seed, 0 failures, 0 errors, 0 skipped |
| `.\gradlew.bat build` | `backend/` | 0 | `BUILD SUCCESSFUL in 2m 20s`; 7 tareas accionables, suite test verde |

## Relación escenarios / tests

| Escenario de la spec | Test que lo cubre |
|---|---|
| Opt-in habilitado con configuración administrativa | `DemoDataSeederActivationTests#enabledDemoWithValidConfigurationLoadsForTheConfiguredDentist` y `DemoDataSeederOrderTests#springOrderCreatesTheDentistBeforeTheDemoDatasetLoads` |
| Opt-in ausente o deshabilitado | `DemoSeedConfigurationTests#absentOptInIsDisabled`, `DemoSeedConfigurationTests#falseOptInIsDisabled`, `DemoDataSeederActivationTests#disabledDemoKeepsAdminSeedBehaviorAndDoesNotLoadDemoData` |
| Demo habilitada sin credenciales o seed administrativo | `DemoSeedConfigurationTests#enabledDemoWithoutAdminSeedFailsWithActionableMessage`, `DemoSeedConfigurationTests#enabledDemoWithoutAdminCredentialsFailsWithActionableMessage`, `DemoDataSeederActivationTests#enabledDemoWithoutAdminSeedFailsWithoutDemoData`, `#enabledDemoWithoutAdminCredentialsFailsWithoutDemoData`, `#enabledDemoWithoutResolvableDentistFailsWithoutDemoData` |
| Perfil test no carga demo | `DemoDataSeederProfileGuardTests#testProfileDoesNotRegisterTheDemoSeeder`, `#testProfileCreatesNoDemoFixturesEvenWhenTheOptInIsActive`, `DemoDataSeederActivationTests#testProfileGuardSkipsDemoEvenWhenEnabled` |
| Carga inicial del dataset | `DemoDataLoaderDatasetTests#loadsRelatedFictitiousDataset` |
| Datos de demo sin información real | `DemoDataLoaderDatasetTests#loadsRelatedFictitiousDataset` verifica 7 pacientes ficticios y emails `.invalid`; el fixture no incluye datos reales por revisión estática |
| Turnos activos respetan ventana y agenda | `DemoDataLoaderDatasetTests#activeAppointmentsRespectTheWindowTheAgendaAndTheBlock`, `DemoSchedulePlannerTests#weekChangeSelectsConfiguredWeekdaysInsideThePublicWindow`, `#activeAppointmentsDoNotOverlapTheBlockOrEachOther`, `#inclusiveWindowBoundsAcceptExactlyTwentyFourHoursAndThirtyDays`, `#configuredDaysAndTimeWindowsAreRespected` |
| No hay slots válidos suficientes | `DemoDataLoaderDatasetTests#insufficientConfiguredAgendaFailsWithoutPartialDemoData`, `DemoSchedulePlannerTests#insufficientAgendaFailsClearly` |
| Carga del turno cancelado sin efectos de C-11 | `DemoDataLoaderCancelledFixtureTests#cancelledFixtureIsAnInitialStateWithoutOperationalCancellation` |
| No se simulan transiciones imposibles | `DemoDataLoaderCancelledFixtureTests#cancelledFixtureIsAnInitialStateWithoutOperationalCancellation`, `#loaderDoesNotDependOnOperationalCancellationDispatchOrNotificationServices` |
| Mensajes demo aislados de recordatorios | `DemoDataLoaderOutboxTests#demoMessagesArePendingBookingMessagesWithInvalidRecipients`, `#eachAppointmentHasAtMostOneBookingFixture`, `#bookingFixtureCoexistsWithAReminderGeneratedForTheSameAppointment` |
| No hay entrega ni despacho automático | `DemoDataLoaderOutboxTests#demoMessagesArePendingBookingMessagesWithInvalidRecipients`, `#bookingFixtureCoexistsWithAReminderGeneratedForTheSameAppointment`; la revisión estática confirma ausencia de dispatch/SMTP/WhatsApp |
| Carga con reloj fijado | `DemoSchedulePlannerTests#sameEffectiveDateProducesTheSamePlanRegardlessOfTimeOfDay`, `#effectiveDateComesFromTheConfiguredZoneAndNotFromUtc` |
| Primera carga en otra fecha y base limpia | `DemoSchedulePlannerTests#weekChangeSelectsConfiguredWeekdaysInsideThePublicWindow`; una arranque real del cambio de fecha NO se ejecutó |
| Segunda ejecución en la misma fecha efectiva | `DemoDataLoaderIdempotencyTests#secondRunOnTheSameEffectiveDateReusesTheSameRecords` |
| Reinicio lunes → martes con datos existentes | `DemoDataLoaderIdempotencyTests#mondayToTuesdayRestartFailsWithoutMovingOrOverwritingDemoData` |
| Existen registros ajenos a los fixtures | `DemoDataLoaderIdempotencyTests#recordsOutsideTheReservedFixturesRemainUntouched` |
| Operador sigue el procedimiento documentado | README revisado: tabla, PowerShell, shell Unix, base limpia y opt-ins. No se hizo recorrida manual real de instalación + arranque local |

## Cobertura adicional verificada

- `AdminDentistSeeder` y `DemoDataSeeder` declaran `@Order` explícito y el admin corre primero: `DemoDataSeederOrderTests#bothRunnersDeclareExplicitOrderWithAdminFirst` y `#springOrderCreatesTheDentistBeforeTheDemoDatasetLoads`.
- Conflicto de `active_slot` produce rollback de todo el lote: `DemoDataLoaderRollbackTests#aConflictingActiveSlotRollsBackTheWholeDemoBatch`.
- Los fixtures tienen estados y asociaciones esperados: 7 pacientes, 4 turnos, 3 entradas WAITING, 1 bloqueo y 3 mensajes BOOKING en `DemoDataLoaderDatasetTests`.
- El turno CANCELLED tiene `activeSlot=null`; no se crea `AppointmentEvent`, `AppointmentToken` ni se modifica WAITING: `DemoDataLoaderCancelledFixtureTests#cancelledFixtureIsAnInitialStateWithoutOperationalCancellation`.

## Revisión del diff y reglas duras

- `CHANGES.md`: sin entrada en `git diff --stat` y sin entrada en `git status --short -- CHANGES.md`. No fue modificado.
- `backend/build.gradle` y archivos de esquema/versiones del stack: sin diff verificado para estos archivos.
- El paquete `backend/src/main/java/com/turnosodontologia/seed/` no contiene `Instant.now()`, `LocalDate.now()`, setters/reflexión de `activeSlot`, llamadas a `OutboxDispatchService`, publicación de `AppointmentCancelledEvent`, llamada a `AppointmentService.cancel`, SMTP ni WhatsApp.
- Los 7 pacientes seed usan DNIs reservados `99.000.101`–`99.000.107` y emails `@example.invalid`.
- Los mensajes demo usan `OutboxMessageType.BOOKING`, `PENDING` y destinatarios `.invalid`.
- No se encontraron secretos ni datos reales de pacientes en el diff revisado.
- `.env.example` contiene variables ficticias, pero no forma parte del diff git versionado por las reglas `.gitignore` existentes.

## README

- `APP_DEMO_SEED` está en la tabla de variables con ejemplo `false` y descripción de opt-in.
- PowerShell y shell Unix incluyen `APP_ADMIN_SEED`, `APP_DEMO_SEED`, email y contraseña ficticios.
- Se exige base limpia, `APP_ADMIN_SEED=true` y `APP_DEMO_SEED=true` para la demo completa.
- Se documenta el reinicio lunes→martes: no desplaza fixtures y requiere base local limpia.
- Se documenta que los mensajes son locales y no se despachan ni envían externamente por el seed.

## Discrepancias y observaciones

- **No encontrada una discrepancia bloqueante** entre spec, implementación y tests ejecutados.
- La spec contiene un escenario de operador siguiendo el procedimiento; se verificó estáticamente, pero no se ejecutó la demostración manual completa.
- La verificación automática del arranque real con perfil `local` no se realizó. Por lo tanto, la creación real persistida de dataset con PostgreSQL o H2 vía `bootRun` queda como `NO VERIFICADO`.

## Limitaciones y lo NO probado

- **No se probó PostgreSQL real.** Toda la ejecución fue H2 con `MODE=PostgreSQL` y perfil `test`.
- **No se probó `bootRun` con `APP_DEMO_SEED=true`** ni dos arranques consecutivos sobre una base persistida real.
- **No se probó end-to-end el reinicio lunes→martes** con proceso real; sí está cubierto por test unitario/persistencia H2.
- **No se probó manualmente el README** siguiendo todos los comandos, aunque el texto y las variables fueron revisados.
- No se probó envío externo real: no existe en el cambio y la revisión confirma que el seed no lo invoca.

## Resultado

C-13 queda **técnicamente verificado con cobertura automatizada H2**:

- Suite completa: **216 tests, 0 failures, 0 errors, 0 skipped**.
- Suite específica seed: **35 tests, 0 failures, 0 errors, 0 skipped**.
- Build: **BUILD SUCCESSFUL**.
- `CHANGES.md`: **no modificado**.
- PostgreSQL real y arranque persistido: **NO VERIFICADO**.
