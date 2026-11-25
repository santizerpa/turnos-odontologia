# Design

## Context

El backend tiene un `AdminDentistSeeder` optativo por `APP_ADMIN_SEED`; su configuración lee `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`. C-10 y C-11 están integrados y archivados. C-10 define `OutboxMessageType.BOOKING` y `REMINDER`, la unicidad `(appointment_id, type)`, y `ReminderGenerationService` crea solo `REMINDER` para turnos `PENDING`/`CONFIRMED` entre más de 24 y hasta 48 horas, comprobando `existsByAppointmentIdAndType`. `OutboxDispatchService` marca como `SIMULATED_SENT` todos los mensajes `PENDING` cuando se invoca. C-11 consume `AppointmentCancelledEvent` con listener `AFTER_COMMIT` y puede notificar una entrada `WAITING`.

La carga es local/de demo, no una migración de esquema ni un servicio de envío. Las fechas deben seguir el reloj de agenda y la zona horaria configurada (`America/Argentina/Buenos_Aires`); los datos y credenciales siguen siendo ficticios. PostgreSQL es el runtime local; H2 en modo PostgreSQL se usa exclusivamente en tests.

## Goals / Non-Goals

**Goals:**
- Reutilizar la configuración existente del odontólogo y cargar un dataset relacionado, coherente con las entidades y reglas actuales.
- Mantener la carga aislada, transaccional, repetible para una misma fecha efectiva y activada solo por opt-in.
- Hacer que turnos, bloqueo, lista de espera y bandeja queden visibles sin disparar eventos de cancelación, el despacho ni efectos externos.
- Mantener la compatibilidad con C-10/C-11 y prevenir una regeneración destructiva al cambiar de fecha.

**Non-Goals:**
- Migrar o alterar el esquema, cambiar APIs o poblar la base de tests por defecto.
- Enviar emails reales, ejecutar el dispatch de outbox durante el seed o añadir datos reales.
- Mantener actualizados automáticamente los turnos de una demo ya cargada al cambiar de día; el operador prepara una base local de demo limpia para cargar un nuevo día efectivo.
- Modificar `CHANGES.md`: queda fuera de alcance y ninguna tarea de este change debe editarlo; solo el líder del proyecto lo modifica.

## Decisions

1. **Separar y ordenar los seeders.** Mantener `APP_ADMIN_SEED` y sus credenciales tal como están; agregar `APP_DEMO_SEED=false` por defecto. Ambos runners tendrán `@Order` explícito, con un valor de orden anterior para `AdminDentistSeeder` que para `DemoDataSeeder`. La demo completa requiere `APP_ADMIN_SEED=true`, `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` válidos y que el odontólogo resuelto exista. Si falta un requisito, el arranque falla con un mensaje operativo antes de escribir datos de demo; el lote de demo es transaccional para no dejar registros parciales. Alternativa descartada: ejecutar todos los datos en cada arranque local, porque poblaría bases de desarrollo sin consentimiento.

2. **Proteger el perfil de pruebas.** `DemoDataSeeder` no se ejecuta en el perfil `test`, incluso cuando `APP_DEMO_SEED=true`. El opt-in no prevalece sobre la exclusión del perfil. H2 en modo PostgreSQL se reserva a pruebas unitarias/de integración del seed; la documentación de ejecución local usa PostgreSQL.

3. **Usar fixtures estables y upsert conservador.** Identificar los registros con claves ficticias reservadas y consultar antes de insertar. Si un fixture ya existe en la misma fecha efectiva local, reutilizarlo sin sobrescribirlo; nunca borrar ni modificar registros ajenos. No se usa truncado ni un upsert que actualice los fixtures existentes.

4. **Fijar la fecha efectiva y proteger el cambio de día.** La fecha efectiva es la fecha local del reloj de agenda en la zona configurada. Una segunda ejecución el mismo día reusa el mismo conjunto sin duplicarlo ni moverlo. Si el reloj pasa, por ejemplo, de lunes a martes y ya hay datos de demo, el seeder debe detectarlos y fallar claramente antes de escribir: no recalcula, mueve, sobrescribe ni regenera el conjunto existente. El operador debe preparar una base local de demo limpia antes de optar por cargar datos relativos a la nueva fecha. No borrar datos automáticamente.

5. **Calcular turnos válidos desde el reloj y la agenda configurada.** Elegir determinísticamente días y horas a partir del `Clock` de agenda y de la zona configurada; cada turno activo debe satisfacer `now + 24h <= startAt <= now + 30d`, en días y ventanas configuradas y con la duración que acepta el dominio. Ningún turno activo se superpone con el bloqueo ni con otro fixture; los `activeSlot` son distintos y compatibles con la unicidad persistente. Si no existen slots suficientes dentro de las reglas, fallar sin persistencia parcial en vez de fabricar horarios inválidos. No usar `LocalDate.now()` ni la zona del sistema.

6. **Crear el turno cancelado como estado de fixture, no como transición operativa.** Los fixtures cancelados se inicializan coherentemente con `status=CANCELLED` y `activeSlot=null` usando una construcción de dominio compatible; nunca se llama a `AppointmentService.cancel`, no se publica `AppointmentCancelledEvent` y no se modifica una entrada `WAITING`. No invocar la cancelación de paciente con menos de 24 horas ni usar escrituras directas a columnas para fingir una transición. Los fixtures activos mantienen `activeSlot=startAt`.

7. **Aislar los mensajes de demo del ciclo de recordatorios.** Crear mensajes `PENDING` visibles con `OutboxMessageType.BOOKING` (o, solo si se justifica una alternativa compatible, un tipo no `REMINDER`) y destinatarios `@example.invalid`. La unicidad `(appointment_id, type)` exige como máximo un fixture `BOOKING` por turno; no crear `REMINDER` ni usar un tipo que active `existsByAppointmentIdAndType(..., REMINDER)`. Por consiguiente, C-10 no considera esos mensajes existentes como recordatorios y puede generar sus propios `REMINDER` elegibles sin colisión. El seeder no invoca `OutboxDispatchService` ni proveedores. El endpoint de despacho existente puede marcar todo `PENDING` como `SIMULATED_SENT` si el operador lo invoca después; tampoco envía por SMTP/WhatsApp.

8. **Cargar el conjunto dentro de una transacción.** Si falla una inserción necesaria o la validación del conjunto, revertir el lote completo y dejar un error de arranque accionable. Las pruebas usan el perfil `test` con H2 en modo PostgreSQL y un reloj controlado; no requieren PostgreSQL.

## Risks / Trade-offs

- [El opt-in podría habilitarse por error en una base local persistente] → Mantenerlo deshabilitado por defecto, documentar claramente `APP_ADMIN_SEED` y `APP_DEMO_SEED`, y usar solo fixtures ficticios.
- [Un arranque repetido en otra fecha podría dejar una agenda de demo vencida o mezclar dos datasets] → Si ya existen fixtures de demo con otra fecha efectiva, fallar sin cambiarlos y documentar que el operador debe preparar una base local limpia.
- [Un turno inválido podría violar reglas de agenda, aviso previo o unicidad] → Probar límites de 24 horas/30 días, días y horarios configurados, bloqueos y `active_slot` sin colisiones con `Clock.fixed`.
- [Una cancelación de fixture podría disparar C-11 o un turno cercano podría entrar en el ciclo C-10] → Persistir el cancelado como estado inicial sin evento, y probar explícitamente la ausencia de evento, consumo de `WAITING` y conflicto de `REMINDER`.
- [El despachador actual procesa cualquier mensaje PENDING al ser llamado] → No invocar dispatch desde seed; documentar que `BOOKING` es una muestra local y que dispatch solo cambia estado a `SIMULATED_SENT`, sin entrega externa.
- [Las reglas de turnos, lista de espera u outbox podrían cambiar] → Construir mediante fábricas/métodos de dominio compatibles y cubrir invariantes y relaciones en pruebas con H2.

## Migration Plan

No hay migración de base de datos. Para la demo, el operador prepara una base local limpia, configura `APP_ADMIN_SEED=true`, `APP_DEMO_SEED=true` y credenciales ficticias en variables de entorno, y arranca con el perfil local siguiendo el README. Sin opt-in explícito, `APP_DEMO_SEED` se considera `false`. Para volver a generar fechas relativas en otro día, se reinicia la base local de demo según esos pasos; el seed no elimina ni modifica datos automáticamente. La tabla de variables y ejemplos PowerShell/Unix del README deben mostrar la variable y su opt-in explícito.

## UI Mockups

Decision: no (no UI)
