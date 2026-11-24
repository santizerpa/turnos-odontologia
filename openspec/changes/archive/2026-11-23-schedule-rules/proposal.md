# Proposal

## Why

C-02 dejó solo el dato de `ScheduleBlock` y ninguna regla de agenda: hoy no existe una fuente persistente del horario semanal, ni un cálculo reproducible de slots, ni una ventana pública verificable. Sin eso, C-05 no puede validar si un turno cae en un horario abierto y C-07 no puede ofrecer reservas públicas dentro de las 24 horas y los 30 días que exige la KB.

C-04 cierra esa brecha con una configuración semanal por odontólogo, un motor de disponibilidad determinista y el contrato de ventana pública que reutilizarán los changes siguientes.

## What Changes

- Incorporar `ScheduleConfiguration` como entidad persistente: una fila por odontólogo (`dentist_id` único) con sus franjas semanales como `@ElementCollection`. Es una entidad nueva fuera del ERD actual; el change actualiza el modelo documentado y emite el delta correspondiente.
- Sembrar la configuración de forma perezosa desde los valores por defecto de `app.schedule.*`: lunes a viernes, 09:00–13:00 y 14:00–19:00, timezone `America/Argentina/Buenos_Aires`.
- Calcular slots de 30 minutos anclados al inicio de cada franja, sin persistirlos, excluyendo bloqueos vigentes y turnos activos.
- Incorporar `GET /api/schedule` con la configuración y los límites de la ventana pública, y `PATCH /api/schedule` con reemplazo total del horario semanal.
- Incorporar `GET /api/schedule/availability?from=&to=` con la ventana pública de 24 horas a 30 días, rechazando con 400 los rangos que la exceden.
- Incorporar `POST /api/schedule/blocks` y `DELETE /api/schedule/blocks/{id}` con baja lógica e idempotencia en la eliminación.
- Introducir `Clock` y `ZoneId` inyectables para que el cálculo y las pruebas no dependan del reloj ni de la zona del sistema operativo.
- Dejar contratos reutilizables para C-05 (`isOpenSlot`/`freeSlots`) y C-07 (`PublicBookingWindow`), sin política pública dentro del cálculo interno.
- Documentar el DDL de las tablas nuevas para el perfil `local` con `ddl-auto: validate` y actualizar `knowledge-base/04_modelo_de_datos.md`; no se incorpora Flyway ni Liquibase.

Fuera de alcance: UI administrativa o pública (C-12/C-08), creación de turnos y transiciones de estado (C-05), reserva pública (C-07), feriados automáticos, duración variable, reprogramación y cualquier cambio en autenticación o en las entidades existentes.

## Capabilities

### New Capabilities

- `disponibilidad-agenda`: configuración semanal persistida por odontólogo, generación de slots de 30 minutos, consulta de disponibilidad dentro de la ventana pública, bloqueos manuales con baja lógica y contrato de ventana reutilizable por la reserva pública.

### Modified Capabilities

- `modelo-persistente`: el modelo pasa de ocho a nueve entidades persistentes con la incorporación de `ScheduleConfiguration` y su colección de franjas; se agrega su repositorio Spring Data y se actualiza el requisito que enumera las entidades.

## Impact

- Nuevos componentes en `com.turnosodontologia.schedule`: entidad de configuración, value objects de horario, cálculo de slots, servicio de disponibilidad, ventana pública, controladores, DTOs y manejo acotado de errores.
- Dos tablas nuevas (`schedule_configurations` y sus franjas) que deben crearse en el perfil `local` antes de arrancar con `ddl-auto: validate`; el perfil `test` las genera con `create-drop`.
- Nuevas propiedades `app.schedule.*` y un `Clock`/`ZoneId` inyectables; sin dependencias nuevas en `build.gradle` y sin cambios de versión de Java o Spring Boot.
- Contratos nuevos para C-05 (evaluación de un slot y slots libres sin política de ventana) y C-07 (ventana pública y disponibilidad por fecha), sin agregar `permitAll()` ni tocar `/api/public/**`.
- Documentación afectada: `knowledge-base/04_modelo_de_datos.md` (ERD y entidad) y `database/README.md` (DDL y forma de aplicarlo en local).
- Sin cambios en frontend ni en el esquema de autenticación existente.
