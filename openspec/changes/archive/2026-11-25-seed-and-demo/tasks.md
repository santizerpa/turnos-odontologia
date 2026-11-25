# Tasks

## 1. Activación segura del seed de demo

- [x] 1.1 Escribir primero pruebas para `APP_DEMO_SEED` ausente y `false`: no crear datos completos de demo y conservar el comportamiento actual de `APP_ADMIN_SEED`; verificar el resultado con H2.
- [x] 1.2 Escribir primero pruebas para el runner: `AdminDentistSeeder` tiene orden explícito anterior a `DemoDataSeeder`; con opt-in y configuración válida el odontólogo existe antes de cargar el dataset. Implementar `@Order` explícito en ambos runners y dejar verdes las pruebas.
- [x] 1.3 Escribir primero pruebas para `APP_DEMO_SEED=true` sin `APP_ADMIN_SEED=true`, sin `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD`, o sin odontólogo resoluble: el arranque falla con mensaje accionable y no deja datos parciales de demo. Implementar validación/guardas y verificar rollback con H2.
- [x] 1.4 Escribir primero un escenario de contexto con perfil `test` y `APP_DEMO_SEED=true`; demostrar que `DemoDataSeeder` no corre y no crea fixtures. Implementar la exclusión explícita del perfil `test` y dejar verde el escenario.

## 2. Dataset ficticio determinista e idempotente

- [x] 2.1 Escribir primero pruebas unitarias con `Clock.fixed` para la fecha efectiva local y selección determinista de días/horarios en la zona configurada, incluyendo cambio de semana; verificar límites inclusivos `now + 24h` y `now + 30d`, días y franjas configurados, duración de dominio, bloqueo, ausencia de solapamiento y `active_slot` únicos. Implementar el cálculo mínimo y verificar verde.
- [x] 2.2 Escribir primero pruebas de persistencia con H2 para la carga relacionada: un odontólogo configurado, entre cinco y diez pacientes, dos turnos `CONFIRMED`, uno `PENDING`, uno `CANCELLED`, tres entradas `WAITING`, un bloqueo y mensajes ficticios de outbox. Comprobar asociaciones, estados y coherencia de `active_slot`; implementar el seeder transaccional usando construcción/fábricas de dominio, sin escribir `active_slot` directamente.
- [x] 2.3 Escribir primero pruebas de efectos laterales: cargar un fixture `CANCELLED` deja `activeSlot=null`, no publica `AppointmentCancelledEvent` ni consume entradas `WAITING`; no invocar `AppointmentService.cancel` ni la cancelación de paciente con menos de 24 horas. Implementar la construcción de estado inicial coherente y dejar verdes las pruebas.
- [x] 2.4 Escribir primero pruebas de outbox: los mensajes son `PENDING`, usan destinatarios `.invalid` y tipo `BOOKING` (o tipo compatible no `REMINDER`), no invocan dispatch/servicios externos, no duplican `(appointment_id, type)` y coexisten para el mismo turno con un `REMINDER` de C-10. Implementar los fixtures y verificar que no interfieren con `ReminderGenerationService` ni C-11.
- [x] 2.5 Escribir primero pruebas de idempotencia para dos arranques con la misma fecha efectiva: conservar mismas identidades, relaciones, estados y horarios, no duplicar ni modificar registros ajenos. Escribir además prueba lunes → martes con fixtures previos: fallar claramente y sin cambios, nunca mover/sobrescribir/regenerar datos. Implementar detección de fecha existente y dejar verdes las pruebas.

## 3. Procedimiento reproducible de demo

- [x] 3.1 Actualizar la tabla de variables del `README.md` y ambos ejemplos (PowerShell y shell Unix) con `APP_ADMIN_SEED`, `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` y `APP_DEMO_SEED`; mostrar `APP_DEMO_SEED=false` como valor por defecto y el cambio explícito a `true` para la demo, junto con requisitos, base local limpia y credenciales ficticias. Comprobar que coincida con los runners reales, que la demo requiera ambas habilitaciones y que no se documenten secretos.
- [x] 3.2 Documentar en `README.md` que un restart con cambio de fecha efectiva (por ejemplo, lunes a martes) no desplaza fixtures existentes y requiere que el operador prepare una base local de demo limpia; indicar que los mensajes de outbox son locales y nunca se envían externamente por el seeder.

## 4. Regresión y cierre técnico

- [x] 4.1 Ejecutar las pruebas específicas del seed y luego `./gradlew test` desde `backend`; confirmar suite verde con H2 en modo PostgreSQL, incluyendo el guard del perfil `test`, sin requerir PostgreSQL instalado.
- [x] 4.2 Ejecutar `./gradlew build` desde `backend` y revisar el diff: sin datos reales, secretos, envío externo, cambios de esquema/versiones ni escrituras directas de `active_slot`; registrar la evidencia.

## Workflow follow-up

- Antes de iniciar apply, verificar que la base de implementación contiene C-10 y C-11 integrados; ambos forman parte del contrato de compatibilidad de este change.
- Archivar el change solo después de completar las tareas, revisar la implementación y validar el resultado.
- `CHANGES.md` está fuera de alcance: ninguna tarea de este change debe editarlo; solo el líder del proyecto lo modifica.
