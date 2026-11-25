# Verificación — C-14 tarea 1.4

Fecha: 2026-11-25. Datos de pruebas ficticios. El backend usa el perfil `test` y H2 en modo de compatibilidad PostgreSQL; no se afirma una verificación en PostgreSQL real.

## Línea base

| Comando | Directorio | Resultado |
|---|---|---|
| `node --version` | raíz | `v26.13.0` (cumple `>=22`) |
| `npm.cmd test` | `frontend/` | 94 tests, 94 passed, 0 failed; línea base con `dist/` y `node_modules/` existentes |
| `.\gradlew.bat test --tests "com.turnosodontologia.schedule.ScheduleLazySeedTests" --tests "com.turnosodontologia.schedule.ScheduleApplicationApiTests" --tests "com.turnosodontologia.schedule.ScheduleHttpSecurityTests" --tests "com.turnosodontologia.appointment.PublicBookingApiTests" --tests "com.turnosodontologia.seed.DemoDataSeederActivationTests" --tests "com.turnosodontologia.seed.DemoDataSeederOrderTests"` | `backend/` | `BUILD SUCCESSFUL`; pruebas backend focalizadas existentes en verde antes de modificar código |

## Evidencia RED → GREEN

- **Backend RED:** las pruebas de regresión añadidas fallaron antes de la corrección: 15 tests, 3 fallidos. Fallaron la comprobación reflectiva de `@Transactional(readOnly = false)`, la idempotencia del seed y la precondición del caso seed+disponibilidad porque el schedule esperado no existía; esa primera ejecución llegó a fallar antes de invocar el GET público. El GET autenticado de agenda pasa en H2, lo que confirma por qué se añadió la comprobación de metadatos para el defecto específico de PostgreSQL.
- **Backend GREEN focalizado:** `.\gradlew.bat test --tests "com.turnosodontologia.schedule.ScheduleLazySeedTests" --tests "com.turnosodontologia.schedule.ScheduleHttpSecurityTests" --tests "com.turnosodontologia.seed.DemoDataSeederActivationTests" --tests "com.turnosodontologia.seed.DemoDataSeederOrderTests" --tests "com.turnosodontologia.seed.AdminSeedScheduleIntegrationTests"` desde `backend/`: `BUILD SUCCESSFUL`.
- **Frontend RED limpio:** se confirmó que `frontend/dist/` y `frontend/node_modules/` son directorios generados e ignorados por Git. Desde la raíz se eliminaron únicamente esos dos directorios con `Remove-Item -LiteralPath frontend/dist,frontend/node_modules -Recurse -Force`; después, desde `frontend/`, `npm.cmd ci` terminó correctamente (`added 1 package`, auditoría de 2 paquetes, 0 vulnerabilidades) y `npm.cmd test` dio 93/94: el test `shell.test` falló con `ENOENT` para `dist/admin.html`, que no se copiaba sin ejecutar `build`.
- **Frontend GREEN limpio:** repetidos los mismos pasos de eliminación, instalación y test después de corregir el script. `npm.cmd ci` terminó correctamente (`added 1 package`, auditoría de 2 paquetes, 0 vulnerabilidades); `npm.cmd test` ejecutó `npm run build && node --test "dist/**/*.test.js"`: 94 tests, 94 passed, 0 failed, 0 skipped.

## Casos funcionales verificados

- `ScheduleConfigurationService.get(Dentist)` no tiene `readOnly=true`; el test inspecciona el método mediante `AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class)`.
- Un GET autenticado a `/api/schedule` sin configuración previa responde 200 y crea una fila para ese odontólogo.
- Con `APP_ADMIN_SEED=true`, el arranque integra un schedule por defecto para el odontólogo seed; el GET público `/api/public/slots?date=<día hábil dentro de la ventana>` responde 200 sin consultar antes `/api/schedule`. Una segunda ejecución del seed conserva exactamente una fila.
- El seeder solo crea agenda tras habilitarse `APP_ADMIN_SEED`; la prueba de activación conserva la separación entre el seed administrativo y los fixtures de demo.
- No se modificaron contratos HTTP ni dependencias. La expectativa existente de fixtures de demo no suponía que faltara agenda y no se cambió.

## Revisión de métodos `readOnly`

- `ScheduleBlockService.find`: solo consulta el repositorio y devuelve la entidad encontrada.
- `PatientService.list`: solo consulta pacientes y transforma resultados/paginación en respuestas.
- `WaitingListQueryService.list`: solo consulta y transforma las entradas ordenadas.
- `ReminderGenerationService.generate`: selecciona turnos candidatos y comprueba si ya existe un recordatorio. La creación se delega a `ReminderPerAppointmentTransaction.create()`, con `REQUIRES_NEW`, que posee la transacción de escritura; se dejó sin cambios.

## Suite backend completa

| Comando | Directorio | Resultado |
|---|---|---|
| `.\gradlew.bat test` | `backend/` | `BUILD SUCCESSFUL in 2m 23s`; H2 (`application-test.yml`, `jdbc:h2:mem:turnos;MODE=PostgreSQL`, perfil `test`); 59 XML de suite, 220 tests, 0 failures, 0 errors, 0 skipped |

Informe HTML: `backend/build/reports/tests/test/index.html`.

## PostgreSQL real

Se detectó el servicio local `postgresql-x64-19` en estado `Running` y el puerto `127.0.0.1:5432` abierto. No había variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` ni `SPRING_DATASOURCE_PASSWORD`, y `psql`/`pg_isready` no están disponibles en `PATH`. Además, el perfil `local` requiere un esquema preexistente y `ddl-auto: validate` (ver `database/README.md`). No se seleccionó ni modificó una base local sin configuración/autorización verificable; por tanto, la suite se ejecutó únicamente en H2 y el comportamiento en PostgreSQL real queda **NO VERIFICADO**.

## Alcance de cambios

- Solo se completó C-14 tarea 1.4. No se modificó `CHANGES.md`, no se agregaron dependencias y no se hicieron commits.

---

# Verificación — C-14 tareas 3.1 y 3.2 (Block 2)

Fecha: 2026-11-25. Se conservaron intactas las evidencias anteriores de la tarea 1.4. Fixtures, DNI y tokens usados abajo son ficticios. Todas las pruebas backend corrieron con el perfil `test` y H2 en modo PostgreSQL (`jdbc:h2:mem:turnos;MODE=PostgreSQL`); no se afirma una prueba contra PostgreSQL real.

## TDD: evidencia RED → GREEN

| Etapa | Comando / cobertura | Resultado |
|---|---|---|
| Línea base antes de cambios de código | `& .\gradlew.bat test --tests "com.turnosodontologia.appointment.PublicBookingApiTests" --tests "com.turnosodontologia.appointment.PublicAppointmentTokenHttpTest" --tests "com.turnosodontologia.appointment.AppointmentConflictHttpTest" --tests "com.turnosodontologia.appointment.AppointmentApiTests" --tests "com.turnosodontologia.patient.PatientControllerTest" --tests "com.turnosodontologia.schedule.ScheduleApplicationApiTests" --tests "com.turnosodontologia.waiting.WaitingListApiTest" --tests "com.turnosodontologia.outbox.C10OutboxErrorHttpTests"` (`backend/`) | `BUILD SUCCESSFUL` en 1m 48s; los tests existentes afectados estaban verdes antes de tocar producción. |
| RED | Se añadieron primero los casos HTTP/domino y luego se ejecutó el foco de 6 clases (`PublicBookingApiTests`, `PublicAppointmentTokenHttpTest`, `AppointmentConflictHttpTest`, `AppointmentApiTests`, `AppointmentServiceTest`, `FinalErrorHandlerHttpTest`). | `53 tests completed, 9 failed`: fallaron el mensaje de agenda inexistente, el mapeo de integridad no relacionada, el body genérico para argumentos/estado inesperados y los tipos de excepción esperados de rango/transición/odontólogo/paciente. Tres casos del advice final dieron 404 porque el controlador de prueba anidado no quedaba registrado por `@WebMvcTest`; se movió a un controlador de prueba top-level antes de la implementación, sin cambiar producción por ese motivo. |
| GREEN focalizado | `& .\gradlew.bat test` con `--tests` para las diez clases de la línea focal detalladas debajo (incluye pacientes, seguridad/agenda, lista de espera y outbox), desde `backend/`. | `BUILD SUCCESSFUL`; 87 tests focalizados, sin fallos. Incluye los casos nuevos de status/body, redacción, Advice, `ErrorResponse`, 404/405 y log sanitizado. |
| Suite completa | `& .\gradlew.bat test` (`backend/`), sin flags de exclusión. | `BUILD SUCCESSFUL in 2m 58s`; 60 XML de suite, 230 tests, 0 fallos, 0 errores, 0 omitidos. Informe HTML: `backend/build/reports/tests/test/index.html`. |

Comando del GREEN focalizado:

```powershell
& .\gradlew.bat test --tests "com.turnosodontologia.appointment.PublicBookingApiTests" --tests "com.turnosodontologia.appointment.PublicAppointmentTokenHttpTest" --tests "com.turnosodontologia.appointment.AppointmentConflictHttpTest" --tests "com.turnosodontologia.appointment.AppointmentApiTests" --tests "com.turnosodontologia.appointment.AppointmentServiceTest" --tests "com.turnosodontologia.common.error.FinalErrorHandlerHttpTest" --tests "com.turnosodontologia.patient.PatientControllerTest" --tests "com.turnosodontologia.schedule.ScheduleHttpSecurityTests" --tests "com.turnosodontologia.waiting.WaitingListApiTest" --tests "com.turnosodontologia.outbox.C10OutboxErrorHttpTests"
```

## Contratos HTTP comprobados

- Agenda pública sin configuración: `GET /api/public/slots` conserva HTTP 422 y ahora devuelve `{"error":"Solicitud inválida"}`, sin reenviar `Configuración de agenda inexistente`.
- Rango de agenda inválido conserva 422 y el mensaje controlado `Rango de agenda inválido`; transición inválida desde `PATCH /api/appointments/{id}/status` conserva 422 y el mensaje controlado `Transición de estado inválida`.
- Token malformado conserva 404 y `Token inexistente o inválido`; DNI inválido conserva 422 y `Datos de reserva inválidos` en reserva pública. Las pruebas comprueban que los bodies no contienen `Exception`, `org.springframework`, `org.hibernate`, `SQL`, `at com.` ni el token/DNI ficticio enviado.
- `AppointmentConflictException` conserva 409 `La franja solicitada ya está ocupada`. Una `DataIntegrityViolationException` ajena a ese conflicto conserva 409 y devuelve `No se pudo completar la operación`, sin mensajes de driver/SQL ni sentinels de DNI/token.
- `IllegalArgumentException`/`IllegalStateException` inesperadas dentro del límite de turnos responden 422 `Solicitud inválida`; RuntimeException no manejada responde 500 `Error interno`. Ambas respuestas descartan los sentinels sensibles y detalles internos.
- El advice final vuelve a lanzar excepciones `ErrorResponse`: prueba de `ResponseStatusException` conserva 418. Ruta inexistente conserva 404 y método no soportado conserva 405.
- `OutboxExceptionHandler` no se modificó: el 500 inesperado de outbox conserva su `ProblemDetail` (`detail: Error interno al procesar la bandeja`), como única excepción de formato **dentro del fallback genérico de errores inesperados**. La validación esperada de pacientes conserva su `ProblemDetail` 400 por el contrato explícito de `admin-agenda-ui`; es una ruta de error distinta, no una excepción del fallback. No se afirma que Outbox sea el único handler que devuelve `ProblemDetail`.

## Logging y precedencia de handlers

`FinalRuntimeExceptionHandler` usa `@RestControllerAdvice` con `@Order(Ordered.LOWEST_PRECEDENCE)`. Registra el nombre de clase de la excepción y el nombre de clase de la causa raíz; no registra objeto `Throwable`, mensajes, causas, stacktrace, cuerpo, URL/parámetros, valores SQL ni datos de sesión/PII/token. El test captura el log e inyecta sentinels ficticios SQL/token/DNI, comprueba que no aparecen y verifica ambos tipos de clase.

Los advice de dominio existentes para turnos, pacientes, agenda y lista de espera fueron ordenados antes del fallback; los advice públicos/outbox conservan su precedencia más específica. Esta precedencia es necesaria porque el valor predeterminado de otros advice coincidía con `LOWEST_PRECEDENCE` y el fallback podía adelantarse, cambiando respuestas de validación existentes a 500. Los textos y status establecidos se conservaron.

## Alcance de esta verificación

- Se completaron únicamente las tareas 3.1 y 3.2 en esta ejecución; OpenSpec queda en 3/14 tareas completas. No se modificaron specs, `.openspec.yaml`, `CHANGES.md`, dependencias ni la evidencia de la tarea 1.4. No se hicieron commits.
- PostgreSQL real permanece **NO VERIFICADO**; la verificación completa fue exclusivamente sobre H2 en modo PostgreSQL.

## Seguimiento — diagnóstico seguro en logs (2026-11-25)

Este seguimiento modifica únicamente la información técnica del log de errores inesperados: conserva el nombre de clase de la excepción y agrega el nombre de clase de la causa raíz. No modifica responses, status, mensajes, handlers de pacientes, specs ni tareas.

| Etapa | Comando (`backend/`) | Resultado |
|---|---|---|
| Línea base | `& .\gradlew.bat test --tests "com.turnosodontologia.common.error.FinalErrorHandlerHttpTest" --tests "com.turnosodontologia.outbox.C10OutboxErrorHttpTests"` | `BUILD SUCCESSFUL` en 1m; comportamiento de respuesta y 404/405 preexistente verde. |
| RED | `& .\gradlew.bat test --tests "com.turnosodontologia.common.error.FinalErrorHandlerHttpTest"` | 4 tests, 1 fallo: el log tenía el tipo de excepción pero faltaba `rootCauseType=java.sql.SQLException`. |
| GREEN focalizado | El mismo comando de línea base. | `BUILD SUCCESSFUL` en 54s; 5 tests pasaron (4 del advice final y 1 de outbox). La primera aserción verde también comprobó que la clase permitida `java.sql.SQLException` contiene `SQL`; por eso se acotó la prohibición del test al sentinel de valor SQL, no al nombre técnico de la clase. |
| Suite backend completa | `& .\gradlew.bat test` | `BUILD SUCCESSFUL` en 2m 26s; 60 XML de suite, 230 tests, 0 fallos, 0 errores, 0 omitidos. Perfil `test`, H2 `jdbc:h2:mem:turnos;MODE=PostgreSQL`; informe `backend/build/reports/tests/test/index.html`. |

El test HTTP usa una causa raíz ficticia `SQLException` e incluye sentinels SQL-value/DNI/token en mensajes de excepción y causa; el log capturado incluye ambos nombres de clase y excluye dichos sentinels, mensajes y ubicación de stacktrace. La respuesta continúa siendo HTTP 500 `{"error":"Error interno"}`. `OutboxExceptionHandler` y el `ProblemDetail` 500 de outbox siguen intactos. La validación esperada de pacientes sigue devolviendo su `ProblemDetail` HTTP 400, tal como exige `openspec/specs/admin-agenda-ui/spec.md`; no se altera ni se presenta como excepción al fallback genérico.

No se modificaron `PatientExceptionHandler`, formatos de respuesta, status, textos, specs, tareas, `CHANGES.md`, dependencias ni se hicieron commits.

---

# Verificación — C-14 tarea 3.4 (Block 3, C9)

Fecha: 2026-11-25. Se modificó únicamente el texto fallback de los dos HTML y se agregaron assertions del mismo texto en la prueba estática existente. Las pruebas backend corrieron con el perfil `test` y H2 en modo PostgreSQL (`jdbc:h2:mem:turnos;MODE=PostgreSQL`); no se cambió configuración ni se afirma verificación adicional contra PostgreSQL real.

## Referencias de tests inspeccionadas antes de editar

- La búsqueda bajo `backend/src/test` de `static/index.html`, `admin.html` y `static/js/main.js` no encontró referencias literales a esas rutas de archivos.
- `AuthRouteSecurityTests.publicRoutesDoNotRequireSession` solicita `/`, `/index.html`, `/admin.html` y `/js/app.js`; sus assertions son de status/ruta pública (incluido 404 para `/js/app.js`), no de contenido HTML ni de JavaScript.
- `FoundationApplicationTests.publicStaticResourcesAreAllowed` solicita `/index.html`, `/admin.html` y `/js/main.js`; antes de esta tarea solo comprobaba status 200. No había assertions de copy fallback ni de contenido del script. Se agregaron únicamente assertions del copy en las respuestas HTML; la assertion de status 200 de `/js/main.js` permanece intacta.

## TDD: evidencia línea base → RED → GREEN

| Etapa | Comando (`backend/`) | Resultado |
|---|---|---|
| Línea base, antes de editar archivos | `& .\gradlew.bat test --tests "com.turnosodontologia.AuthRouteSecurityTests" --tests "com.turnosodontologia.FoundationApplicationTests"` | `BUILD SUCCESSFUL in 41s`; ambas clases existentes pasaron antes del cambio. |
| RED, assertions nuevas contra los HTML sin cambiar | `& .\gradlew.bat test --tests "com.turnosodontologia.FoundationApplicationTests"` | `BUILD FAILED`; 5 tests, 1 fallo. `publicStaticResourcesAreAllowed` falló en la assertion de `/index.html` porque la copia solicitada aún no existía. |
| GREEN focalizado | `& .\gradlew.bat test --tests "com.turnosodontologia.FoundationApplicationTests" --tests "com.turnosodontologia.AuthRouteSecurityTests"` | `BUILD SUCCESSFUL in 43s`; pasaron las pruebas focalizadas con las assertions de copy y las comprobaciones de rutas/script existentes. |
| Suite backend completa | `& .\gradlew.bat test` | `BUILD SUCCESSFUL in 2m 44s`; 60 suites XML, 230 tests, 0 failures, 0 errors, 0 skipped. Sin flags de exclusión. |

Durante el primer intento GREEN, `content().string(...)` comparó el texto UTF-8 contra la decodificación por defecto de MockMvc y mostró mojibake (`EjecutÃ¡`) porque el response HTML no declara charset. Se mantuvo el body/headers de producción sin cambios y se ajustó solamente la assertion de texto para decodificar los bytes como UTF-8; después el foco y la suite completa pasaron.

Informe HTML: `backend/build/reports/tests/test/index.html`.

## Alcance e invariantes preservadas

- `index.html` y `admin.html` muestran literalmente `Frontend sin compilar. Ejecutá npm run build en la carpeta frontend y recargá.` en el fallback.
- Los tags/atributos `<script src="/js/main.js"></script>` permanecen sin cambios en ambos HTML. `backend/src/main/resources/static/js/main.js` no se modificó: SHA-256 antes y después `C18F92DC4C9F87AD668A1693CDD44FFCE922C9F910DDE33935B138A7BB8395AE`.
- `backend/src/main/resources/application-local.yml:8` conserva `static-locations: file:../frontend/dist/,classpath:/static/`; el frontend compilado mantiene precedencia sobre el fallback de classpath. La configuración no se modificó y no se eliminó `frontend/dist/`.
- Solo se marcó como completa la tarea 3.4. No se editaron `CHANGES.md`, dependencias ni configuración; no se hicieron commits ni se modificaron las assertions de seguridad/scripts.

---

# Integración final — C-14 (2026-11-25)

Esta sección consolida la evidencia de los grupos de código y documentación y la verificación final. Todas las pruebas backend usan el perfil `test`, H2 (`jdbc:h2:mem:turnos;MODE=PostgreSQL`, `create-drop`); no representan una ejecución contra PostgreSQL real. Los datos son ficticios.

## Tareas 1.2, 1.3 y 2.1–2.3 — evidencia focalizada

La ejecución focalizada combinada de 11 clases terminó con **99 tests, 0 fallos, 0 errores y 0 omitidos**. Cubrió `AppointmentTokenEndToEndTest`, `WaitingListNotificationIntegrationTest`, `ScheduleHttpSecurityTests`, `PublicBookingWindowTests`, `PublicBookingApiTests`, `PatientControllerTest`, `CoreModelsIntegrityTests`, `AppointmentPersistenceTest`, `AppointmentServiceTest`, `PublicAppointmentTokenHttpTest` y `WaitingListSelectionServiceTest`.

- **1.2, reloj determinista:** se sustituyeron los usos pertinentes del reloj de host por instantes explícitos/`Clock.fixed` en los tests de token end-to-end, notificación de lista de espera y seguridad de agenda. La búsqueda final en esas tres clases no encontró `Instant.now()` ni `LocalDate.now()`. Los tests focalizados quedaron verdes; la primera expectativa exacta hizo visible que el fixture también debía fijar su reloj.
- **1.3, ventana pública y DNI:** se añadieron los límites exactos y los instantes adyacentes para reserva, aceptación de DNI de 7 dígitos y rechazo de longitudes 6 y 9. Un primer caso usó un slot que no devolvía disponibilidad; se ajustó el fixture a un slot efectivamente disponible. No cambió ninguna regla de producción.
- **2.1, persistencia:** en H2 PostgreSQL-mode se reforzaron las comprobaciones de filas y estado para colisión `active_slot`, ausencia de segundo turno y reutilización posterior a cancelar, en `CoreModelsIntegrityTests` y `AppointmentPersistenceTest`.
- **2.2, transiciones y efectos:** se amplió la matriz de transiciones inválidas y las aserciones de estado, franja, eventos y efectos; se verificaron token vencido, repetición y cancelación dentro de 24 horas sin efectos. El helper inicial no construía correctamente `ABSENT`/`ATTENDED`; quedó corregido antes de que el foco pasara.
- **2.3, lista de espera:** la regresión de repetición de evento se observó primero en rojo y luego pasó con la integración: selección FIFO, segunda entrada aún `WAITING`, un único aviso y turno liberado `CANCELLED` sin asignación automática. La protección de repetición se verificó secuencialmente; **la entrega concurrente duplicada no se probó**.

## Tarea 2.4 — disposición PostgreSQL real

La rama condicional de la tarea se cerró como **pendiente explícito**, no como verificación del motor: el entorno no proporcionó ajustes de conexión autorizados ni `psql`/`pg_isready`; no se eligió ni modificó una base desconocida. `database/README.md` contiene ahora la sección exacta `Estado de la verificación en PostgreSQL real`, y `docs/qa/verification-index.md` registra **PENDIENTE / NO VERIFICADO**, sin credenciales. La comprobación de cancelación y re-reserva sigue sin evidencia real. La casilla 2.4 indica que se atendió la disposición condicional; **no afirma paridad H2/PostgreSQL**.

## Tarea 3.3 — checklist de demo

`docs/qa/c14-demo-checklist.md` documenta precondiciones con datos ficticios, arranque y health check según `README.md` §§3.4–3.5, comandos PowerShell, reserva, bandeja, confirmación, cancelación que activa lista de espera, recordatorios y despacho explícito/idempotente. Los pasos se contrastaron con rutas/controladores y tests existentes. Es un checklist para ejecución manual posterior, no evidencia de que se haya realizado la demo; se aclara que no se envía correo real.

## Verificación integral

| Comando | Directorio | Resultado |
|---|---|---|
| `& .\gradlew.bat test` | `backend/` | `BUILD SUCCESSFUL in 4h 43m 15s`; 60 suites XML, **234 tests, 0 failures, 0 errors, 0 skipped**; 1 tarea ejecutada y 3 up-to-date. Sin flags que excluyan pruebas. Perfil `test`, H2 en modo PostgreSQL. |
| `& .\gradlew.bat build` | `backend/` | `BUILD SUCCESSFUL in 14s`; 7 tareas accionables, 3 ejecutadas y 4 up-to-date. La tarea `test` constó como up-to-date tras la suite completa verde recién ejecutada; no se usó ningún flag para omitirla. |

Informe HTML de la suite: [backend/build/reports/tests/test/index.html](../../../backend/build/reports/tests/test/index.html).

## Revisión de alcance y cierre de gobernanza

- Se revisó el diff de C-14 para tests de tiempo, bordes HTTP/DNI, persistencia, transiciones, deduplicación de avisos y documentación de PostgreSQL/demo. En las tareas 1.2, 1.3 y 2.1–2.3, la única modificación de producción fue añadir una guarda que consulta si ya existe un aviso sin tipo para ese `appointmentId`, haciendo idempotente el replay secuencial; no se cambió contrato HTTP. Se preservó el `ProblemDetail` de validación de pacientes exigido por `admin-agenda-ui`; el `ProblemDetail` de Outbox sigue siendo el formato de su fallback específico.
- `git diff --check` terminó sin errores de whitespace; Git mostró únicamente avisos de conversión LF/CRLF en Windows.
- No se modificó `CHANGES.md`; no se hicieron commits ni se stagearon cambios durante esta integración. Se preservaron cambios staged/preexistentes fuera de este alcance.
- La revisión técnica y el build están completos, pero gobernanza es HIGH. **La tarea 4.2 queda sin marcar** hasta que el responsable revise esta evidencia/diff y dé su sign-off explícito. PostgreSQL real y la ejecución manual de la demo también siguen pendientes como evidencia separada.

## Estado consolidado

Quedan completadas **13/14 tareas**. La única tarea de implementación/aceptación aún abierta es **4.2**, pendiente del sign-off humano HIGH; la verificación PostgreSQL real no se reporta como aprobada y permanece pendiente según 2.4.
