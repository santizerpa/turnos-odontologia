# Tasks

## 1. Inscripción de pacientes

- [x] 1.1 Escribir primero pruebas de servicio/persistencia para crear una entrada asociada a un paciente nuevo y reutilizar un DNI normalizado existente sin modificar su nombre ni email; verificar rojo, implementar la resolución segura de paciente y verificar verde con H2 en modo PostgreSQL.
- [x] 1.2 Escribir primero pruebas para validar DNI, nombre, apellido, email y rango opcional; comprobar que `from > to` y los datos inválidos responden 400 sin crear entrada ni alterar un paciente; implementar la validación y dejar verde las pruebas.
- [x] 1.3 Cubrir la carrera de dos inscripciones simultáneas con el mismo DNI: verificar que la unicidad existente evita duplicar pacientes y que el reintento reutiliza el registro sin actualizar perfil.

## 2. API de lista de espera

- [x] 2.1 Escribir primero pruebas MockMvc de `POST /api/public/waiting-list` sin sesión: inscripción válida responde 201, no revela si el DNI existía y no devuelve el DNI; implementar DTO, controlador y respuesta y verificar el contrato HTTP.
- [x] 2.2 Escribir primero pruebas MockMvc de `GET /api/waiting-list`: con sesión devuelve entradas ordenadas y sin DNI; sin sesión responde 401; implementar consulta, proyección y controlador y verificar ambos casos.
- [x] 2.3 Actualizar `knowledge-base/02_descripcion_general.md` para distinguir `POST /api/public/waiting-list` de `GET /api/waiting-list`; verificar que el contrato documentado coincide con rutas y seguridad implementadas.

## 3. FIFO y elegibilidad

- [x] 3.1 Escribir primero pruebas de selección FIFO: ignorar entradas no `WAITING`, saltar la entrada más antigua si su rango no incluye la fecha liberada, aceptar límites inclusivos y límites nulos, y desempatar `created_at` con `id`; implementar la consulta ordenada y verificar verde.
- [x] 3.2 Escribir primero pruebas de transición: solo la entrada elegible pasa de `WAITING` a `NOTIFIED`; entradas no elegibles y estados terminales permanecen iguales; implementar la operación de dominio y verificar persistencia con H2.
- [x] 3.3 Escribir primero pruebas de conversión de `event.startAt` a fecha en `America/Argentina/Buenos_Aires`, incluyendo los límites de fecha; implementar la elegibilidad con la zona configurada y verificar que no depende de la zona del sistema.

## 4. Listener y aviso Outbox

- [x] 4.1 Antes de integrar Outbox, verificar la dependencia C-10 en la rama objetivo: confirmar que el constructor de cinco parámetros sigue disponible, que deja `type` nulo y que existe la unicidad `(appointment_id, type)`; si C-10 no está presente, detener la integración de este punto y coordinar la rama base, sin inventar ni usar `REMINDER`.
- [x] 4.2 Escribir primero pruebas del listener publicando `AppointmentCancelledEvent`: procesa solo después del commit, usa `REQUIRES_NEW`, elige una entrada y crea un único `OutboxMessage` PENDING asociado al turno cancelado y al email guardado; implementar listener y servicio sin modificar clases de C-05 y verificar verde.
- [x] 4.3 Escribir primero pruebas de atomicidad y casos vacíos: si falla la persistencia del mensaje se revierte `NOTIFIED`; si no hay elegible no se crean mensajes ni cambian estados; implementar el manejo transaccional y verificar ambos resultados.
- [x] 4.4 Escribir primero una prueba de integración con un `REMINDER` preexistente para el mismo turno; verificar que el aviso de lista de espera se persiste con tipo nulo sin alterar el recordatorio ni violar la unicidad de C-10.

## 5. Regresión

- [ ] 5.1 Ejecutar `./gradlew test` y `./gradlew build` con el wrapper; verificar que la suite relevante está verde, que no se omitieron pruebas y que H2 en modo PostgreSQL alcanza para las pruebas.
- [x] 5.2 Revisar el diff de C-11: verificar ausencia de cambios en clases de C-05, asignación automática, datos reales o secretos; registrar la evidencia de las pruebas y dejar la rama lista para revisión.

## Workflow follow-up

- Actualizar el estado y la ruta de inscripción de C-11 en `CHANGES.md` únicamente al cerrar y verificar el change, conservando el control de git del usuario.
- Archivar el change solo después de completar las tareas y obtener la revisión correspondiente.
