# Tasks

## 1. Lógica pura de horarios

- [x] 1.1 Escribir primero pruebas unitarias de `TimeRange`: rechazo de fin anterior o igual al inicio, intersección half-open, límites que se tocan sin solapar y contención de un instante; verificar rojo, implementar el mínimo y dejar la prueba verde.
- [x] 1.2 Escribir primero pruebas de `WeeklySchedule`: normalización del orden por día y hora, día sin franjas, franjas invertidas, duración menor a 30 minutos, solapamiento dentro del mismo día y días repetidos; verificar rojo, implementar y dejar verde con refactor.
- [x] 1.3 Escribir primero pruebas de `ScheduleCalendar` con la configuración por defecto: 18 slots por día hábil, 90 por semana, presencia de 09:00, 12:30, 14:00 y 18:30, ausencia de 13:00 y 19:00, pausa de almuerzo, sábado y domingo sin slots, franja exacta de 30 minutos con un slot y franja desalineada de 45 minutos con un solo slot completo; verificar rojo, implementar la grilla anclada al inicio y dejar verde.
- [x] 1.4 Escribir primero pruebas de timezone: slots de enero y julio con offset `-03:00`, sin huecos ni duplicados por horario de verano, usando una zona fija y sin `ZoneId.systemDefault()`; verificar rojo y verde.
- [x] 1.5 Ejecutar `./gradlew test --tests "*TimeRange*" --tests "*WeeklySchedule*" --tests "*ScheduleCalendar*"` y registrar la salida verde de la lógica pura.

## 2. Configuración persistente y propiedades

- [x] 2.1 Escribir primero pruebas `@DataJpaTest` con `@ActiveProfiles("test")` para `ScheduleConfiguration`: mapeo de la entidad, colección de franjas, una configuración por odontólogo con rechazo de duplicados, orden determinista al leer y soft delete sin borrado físico; usar fixtures ficticios en `support/` y verificar rojo antes de implementar entidad, embeddable y repositorio.
- [x] 2.2 Escribir primero una prueba de carga de contexto que valide el binding de `app.schedule.*` (timezone, duración de 30 y ventanas por defecto) y la existencia de un `Clock` inyectable; implementar `ScheduleProperties`, la configuración de reloj/zona y los valores en `application.yml`; verificar verde y que ninguna clase de agenda use `Instant.now()` ni la zona del sistema.
- [x] 2.3 Escribir primero pruebas de siembra perezosa: la primera lectura crea una única fila con lunes a viernes de 09:00–13:00 y 14:00–19:00, la segunda no duplica y dos odontólogos no comparten configuración; implementar el servicio transaccional de configuración y el reintento único ante violación de unicidad; verificar verde.
- [x] 2.4 Verificar que la configuración sembrada genera 18 slots por día hábil y 90 por semana, y que el perfil `test` crea el esquema sin PostgreSQL; registrar la evidencia.

## 3. Motor de disponibilidad

- [x] 3.1 Escribir primero pruebas `@DataJpaTest` de `AvailabilityService.freeSlots` con datos ficticios: día hábil completo, día cerrado, fin de semana y rango interno sin política pública; verificar rojo, implementar el cálculo sobre configuración y dejar verde.
- [x] 3.2 Escribir primero pruebas de bloqueos: bloqueo de día completo, bloqueo alineado 09:00–10:00, bloqueo no alineado 10:10–10:20 que excluye el slot 10:00–10:30 y conserva 09:30–10:00 y 10:30–11:00, bloqueo con `deleted_at` ignorado y bloqueo de otro odontólogo sin efecto; implementar la consulta de solapamiento half-open y verificar verde.
- [x] 3.3 Escribir primero pruebas de turnos: PENDING ocupa, CONFIRMED ocupa, CANCELLED libera, un turno no alineado que intersecta el slot lo ocupa, un turno eliminado no ocupa y el turno de otro odontólogo no afecta; implementar la consulta con lookback de 30 minutos y la intersección en Java; verificar verde.
- [x] 3.4 Escribir primero pruebas de `isOpenSlot`: verdadero solo para un inicio exacto de la grilla, libre y de un día con franja; falso para inicio no alineado, día cerrado, slot bloqueado, slot ocupado y odontólogo distinto; implementar el contrato reutilizable por C-05 y verificar verde.
- [x] 3.5 Escribir primero una prueba de que dos consultas de disponibilidad con el mismo rango devuelven los mismos slots y no crean turnos ni bloqueos; verificar verde y que la consulta no aplique la ventana de 24 horas a 30 días.

## 4. Ventana pública

- [x] 4.1 Escribir primero pruebas unitarias de `PublicBookingWindow` con `Clock.fixed`: límites exactos de 24 horas y 30 días aceptados, un segundo antes del mínimo y un segundo después del máximo rechazados, rango vacío o invertido rechazado y valores tomados de `app.schedule.public-window`; verificar rojo, implementar y dejar verde.
- [x] 4.2 Verificar que la ventana no vive dentro del cálculo interno: invocar `freeSlots` con un rango de las próximas 2 horas y comprobar que devuelve slots, mientras el componente público rechaza ese mismo rango; registrar la prueba.

## 5. Servicios de aplicación

- [x] 5.1 Escribir primero pruebas de `ScheduleConfigurationService`: representación de GET con timezone, `slotDurationMinutes` y `publicWindow`; PATCH que reemplaza por completo, cierre con lista vacía y conservación de la configuración anterior ante error; implementar y verificar verde.
- [x] 5.2 Escribir primero pruebas de validación de PATCH: 422 para franja invertida, duración menor a 30 minutos, franja solapada, día hábil faltante, día repetido, sábado o domingo y timezone distinta a la fija; implementar las excepciones de negocio y verificar que la configuración previa no cambia.
- [x] 5.3 Escribir primero pruebas de `ScheduleBlockService`: alta válida con motivo, 422 por rango inválido, baja lógica propia, segunda baja idempotente sin reescribir `deleted_at`, 404 para bloqueo ajeno o inexistente y aceptación de un bloqueo fuera de la ventana pública; implementar y verificar verde.
- [x] 5.4 Escribir primero una prueba de propiedad: el odontólogo se toma de `DentistPrincipal.id()` y un `dentistId` enviado en el cuerpo no permite operar sobre otro odontólogo; implementar DTOs y servicios sin ese campo y verificar verde.

## 6. API HTTP y seguridad

- [x] 6.1 Escribir primero pruebas MockMvc de `GET /api/schedule`: 200 con `timezone`, `slotDurationMinutes: 30`, `publicWindow` de 24/30 y días con franjas; primera lectura siembra; 401 sin sesión con JSON de error; implementar el controlador y verificar verde.
- [x] 6.2 Escribir primero pruebas MockMvc de `PATCH /api/schedule`: 200 y persistencia del reemplazo, 422 por franja invertida, duración menor a 30 minutos, solapada, día faltante, día no permitido y timezone distinta, y 401 sin sesión; implementar el controlador y verificar verde.
- [x] 6.3 Escribir primero pruebas MockMvc de `GET /api/schedule/availability`: 200 con lista plana de slots, offset `-03:00`, `durationMinutes` y `window`; 400 por inicio antes de 24 horas, fin después de 30 días, rango vacío, parámetro faltante y formato inválido; 401 sin sesión; implementar y verificar verde.
- [x] 6.4 Escribir primero pruebas MockMvc de bloqueos: 201 con id e instantes, 204 en baja propia, 204 repetido, 404 en bloqueo ajeno, 422 por rango inválido y 401 sin sesión; implementar los controladores y verificar verde.
- [x] 6.5 Escribir primero pruebas del manejo de errores: 400, 404 y 422 de agenda con `{"error": ...}` y `details` en validación, y confirmar que `GET /api/auth/me` sin sesión conserva exactamente `{"error": "No autenticado"}`; implementar el advice acotado a `com.turnosodontologia.schedule` y verificar verde.
- [x] 6.6 Verificar que `/api/schedule/**` sigue protegido por `anyRequest().authenticated()`, que no se agregó `permitAll()` para agenda y que `/api/public/**` sigue reservado para C-07; registrar la revisión de `AuthSecurityConfiguration`.

## 7. Esquema local y documentación

- [ ] 7.1 **[Pendiente — entorno PostgreSQL del líder]** Aplicar `database/schedule-rules.sql` con `psql` en una base PostgreSQL que contenga las tablas base de C-01/C-02. Verificar que `schedule_configurations` y `schedule_windows` existen, que `dentist_id` es UNIQUE, que la FK apunta a `dentists` y que arranca `SPRING_PROFILES_ACTIVE=local` con `ddl-auto: validate` sin diferencias. Registrar fecha, host/instancia sin credenciales y salida de arranque; no marcar hasta tener evidencia real.
- [x] 7.2 Actualizar `knowledge-base/04_modelo_de_datos.md`: agregar `ScheduleConfiguration` al ERD y a las entidades, documentar `dentist_id` único, la colección de franjas, la ausencia de persistencia de slots y la relación con la ventana pública; verificar que el texto coincide con la spec y con el esquema real.
- [ ] 7.3 **[Pendiente — entorno PostgreSQL del líder]** Verificar en PostgreSQL: unique por odontólogo, claves foráneas, tipos `uuid`, `timestamp(6) with time zone`, `time(6)`, días textuales y lectura ordenada de franjas; comparar con H2 sin usar datos reales. Registrar consultas/evidencia en el PR o en `docs/verification/` sin credenciales ni datos de pacientes.
- [ ] 7.4 Actualizar `CHANGES.md` solo después de cerrar y verificar el change: cambiar `[~] implementado en rama; pendiente integración/revisión PostgreSQL del líder` a `[x]` cerrado, conservar el ID y no adelantar el estado de C-05 ni posteriores.

## 8. Regresión y cierre técnico

- [x] 8.1 Ejecutar `./gradlew test` con el perfil `test` y confirmar que toda la suite de C-01 a C-04 queda verde, sin pruebas omitidas ni PostgreSQL como requisito.
- [x] 8.2 Ejecutar `./gradlew build` y registrar que compila, empaqueta y pasa el ciclo relevante con el wrapper.
- [x] 8.3 Revisar el diff final: sin cambios de versión de Java o Spring Boot, sin secretos, sin datos reales, sin escrituras directas de `active_slot`, sin migraciones nuevas y sin commits no solicitados.

## Workflow follow-up

- Ejecutar `openspec status --change schedule-rules --json` y `openspec validate schedule-rules` hasta obtener un resultado válido antes de aplicar.
- **Handoff de integración:** el líder debe ejecutar las tareas 7.1 y 7.3 con PostgreSQL real, documentar las evidencias sin credenciales ni datos reales y revisar el diff completo.
- La tarea 7.4 solo puede cerrar C-04 después de que 7.1 y 7.3 estén verdes.
- Archivar el change solo después de completar y verificar todas las tareas; el usuario mantiene el control de git.
