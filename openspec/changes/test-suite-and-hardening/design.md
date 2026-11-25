# Diseño: suite de pruebas y hardening final

## Context

Ver `proposal.md` para la motivación y el alcance. El repositorio ya tiene pruebas por capas con el starter de Spring Boot 3.3.13, JUnit 5, AssertJ y Mockito; `backend/build.gradle` configura `useJUnitPlatform()` y el perfil `test` usa H2 en modo PostgreSQL. La cobertura existente incluye reserva concurrente/409, límites de ventana, DNI, vencimiento de token, operaciones repetidas, transiciones, FIFO y respuestas seguras seleccionadas. No se debe duplicar ese trabajo sin una brecha demostrable.

La evidencia de paridad con un PostgreSQL real no está cerrada: `database/README.md` deja pendiente la aplicación del DDL y el arranque `local` con `ddl-auto: validate`. Esta propuesta no ejecutó la suite ni una instancia PostgreSQL; la presencia de Docker en el entorno no acredita una base configurada ni disponible.

## Goals / Non-Goals

**Goals:**

- Hacer que la matriz C-14 sea trazable a pruebas existentes o a una tarea concreta de brecha, con aserciones de resultados y efectos persistidos.
- Usar reloj fijo en pruebas sensibles al tiempo y las capas de test más estrechas adecuadas.
- Auditar respuestas HTTP seguras en errores de validación, conflicto y fallos inesperados sin cambiar deliberadamente contrato/status existentes.
- Separar evidencia automática en H2 de evidencia de ejecución y validación contra PostgreSQL real.
- Dejar una secuencia manual breve para demostrar el ciclo de outbox simulado con fixtures ficticios.

**Non-Goals:**

- No se agregan capabilities ni cambia la especificación de producto; las fuentes contractuales existentes siguen siendo autoridad.
- No se añade Testcontainers, dependencia de seguridad, correo real, cobertura porcentual, nuevo endpoint ni infraestructura.
- No se cambia el modelo persistente ni se toca `CHANGES.md`.

## Decisions

1. **Cerrar brechas, no reescribir la suite.** Mapear cada punto C-14 a los tests actuales (`PublicBookingApiTests`, `PublicBookingWindowTests`, `PatientControllerTest`/`PatientServiceTest`, `AppointmentTokenServiceValidationTest`/`PublicAppointmentTokenHttpTest`/`AppointmentTokenEndToEndTest`, `AppointmentServiceTest`, `WaitingListSelectionServiceTest`/`WaitingListNotificationIntegrationTest`, y persistencia de turnos). Mantenerlos cuando ya sean representativos; añadir o ajustar casos solo donde falte una aserción observable, independencia o consistencia de resultados. Alternativa descartada: una nueva batería paralela por regla, que duplicaría y volvería frágil la evidencia.

2. **Mantener la pirámide de tests y las versiones actuales.** Usar pruebas Java/JUnit 5 sin contexto para lógica de dominio, `@WebMvcTest` para mapeo HTTP, `@DataJpaTest` para constraints y `@SpringBootTest` solo para integración de eventos/flujo completo. Continuar con el Gradle Wrapper y no actualizar Java 17, Spring Boot 3.3.13 ni JUnit. Cada caso debe comprobar valores/efectos relevantes y ser independiente; no se aceptan tests omitidos ni cobertura sin assertions.

3. **Eliminar dependencia del reloj global en tests pertinentes.** Inyectar/importar `Clock.fixed` en los casos de token, cancelación y notificación que hoy usan `Instant.now()`/`LocalDate.now()`; elegir instantes explícitos que hagan reproducibles límites de 24 horas, vencimiento y fecha local argentina. Alternativa descartada: tolerancias o fechas relativas al reloj del host, que mantienen flakiness y ocultan errores de borde.

4. **Tratar H2 y PostgreSQL como dos evidencias distintas.** La suite automatizada debe ejercitar en H2 PostgreSQL-mode el índice/constraint único `active_slot`, colisión y re-reserva tras liberar la franja. Para PostgreSQL real, seguir el procedimiento documentado en `database/README.md`: base local disponible, ejecutar los pasos de esquema que correspondan, validar con el perfil local/DDL configurado y repetir los dos invariantes con datos ficticios. Capturar comando, resultado y versión del motor sin credenciales. Si no hay PostgreSQL accesible, registrar el punto como pendiente; no simular paridad ni agregar infraestructura para forzarla.

5. **Auditar la salida por boundary, preservando el contrato existente.** Revisar los advice/handlers de `appointment`, reserva pública, tokens, pacientes, agenda, lista de espera, outbox y seguridad. Para fallos representativos, afirmar status y mensaje público fijo cuando la especificación ya lo exige, y que el body no contiene texto controlado por excepción, clase/stacktrace, SQL/driver, tokens o datos de identidad no permitidos. Corregir producción únicamente ante una fuga o incumplimiento reproducible, con prueba primero; no convertir todos los errores a un mismo status ni filtrar detalles al cliente.

6. **Documentar la demo como checklist local independiente.** Añadir un documento bajo `docs/` con precondiciones de demo ficticia, arranque usando el wrapper/README vigente, inspección de la bandeja, apertura de los enlaces de confirmación/cancelación y despacho explícito como `SIMULATED_SENT`; comprobar idempotencia donde aplique. Aclarar en cada paso que no hay SMTP ni envío real y no incluir valores reales de credenciales o pacientes.

7. **No cambiar contrato, y no crear delta specs.** Los requisitos ya están en `openspec/specs/` y la KB (incluido el manejo seguro de errores). Si se detectara una necesidad de cambiar un status, mensaje o regla de negocio para corregir la suite, detener esa parte y proponer una modificación funcional separada en vez de ampliar silenciosamente C-14. Por esta naturaleza se declara `skip_specs: true`.

## Risks / Trade-offs

- **[La cobertura actual puede tener casos equivalentes con evidencia débil o fechas no deterministas]** → Inspeccionar métodos existentes antes de agregar; reemplazar el reloj global por reloj fijo y comprobar estado/filas/eventos además de status cuando corresponda.
- **[No existe PostgreSQL disponible durante la futura verificación]** → Mantener el gate explícito y aportar salida de H2 por separado; reportar la paridad real pendiente al responsable, sin dar C-14 por completamente verificado.
- **[El hardening puede alterar por accidente mensajes o códigos consumidos por UI]** → Fijar primero el contrato esperado en tests contra las especificaciones existentes y limitar cambios al saneamiento de detalles internos.
- **[Las pruebas de integración pueden ser lentas o depender del orden]** → Preferir unit/slice cuando valide la misma regla, usar fixtures únicos y no compartir estado mutable ni depender del orden global.
- **[La demo podría confundirse con una entrega real]** → El checklist y los pasos deben nombrar “simulado”, requerir acción explícita de dispatch y verificar que no se invoca SMTP/servicio externo.

## Migration Plan

No hay migración de datos ni despliegue. El change se verifica en la rama con `backend/gradlew.bat test` (Windows) o `./gradlew test` (macOS/Linux), seguido de `build` si el cierre lo requiere; se guardan los resultados del wrapper y el informe HTML. La comprobación PostgreSQL se registra aparte según `database/README.md`. Si una corrección de código de hardening falla, revertir únicamente esa corrección con su test asociado, sin eliminar pruebas o cambios preexistentes; no hacer commits sin solicitud.

## Open Questions

La disponibilidad de una instancia PostgreSQL local al momento de aplicar C-14 depende del entorno del responsable; no altera el plan y se resolverá como evidencia aprobada o pendiente durante la verificación.

## UI Mockups

- Decision: no (no UI)
