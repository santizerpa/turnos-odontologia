# Proposal

## Why

La consola administrativa muestra horarios y datos de agenda en formatos técnicos poco legibles, mientras que los rangos diarios/semanales se calculan como medianoches UTC en vez de reflejar la hora local de Buenos Aires. Además, las dos páginas frontend carecen de un contrato de carga de estilos y viewport responsive.

La revisión manual del usuario sobre la primera aplicación (2026-11-25) detectó problemas adicionales que se incorporan a este mismo change por decisión explícita del usuario:

1. La confirmación de reserva pública muestra el `Instant` UTC (`2026-11-26T17:30:00Z`) en lugar de la hora local reservada (14:30).
2. El acceso "ya tengo un turno" pide un token o enlace que un paciente no puede conocer. La demo necesita una **bandeja de entrada simulada** del correo del paciente que muestre los mismos mensajes de la bandeja de salida, con acciones directas.
3. En ese acceso, el selector "Acción" no tiene un botón que ejecute confirmar o cancelar: solo existe "Consultar estado".
4. El odontólogo no debe confirmar turnos (la confirmación es del paciente) y la franja de la agenda sigue mostrándose como `Instant` UTC.
5. La búsqueda de pacientes solo admite DNI; debe poder elegirse entre DNI o nombre, nunca ambos a la vez.
6. La bandeja de salida del admin debe quedar como historial de mensajes enviados, con una sola acción informativa "Ver estado del turno".

## What Changes

**Aplicado (primera iteración):**
- Agenda legible: fecha/hora de Buenos Aires, nombre sin UUID redundante, `activeSlot` nulo como "franja libre", `data-status` por fila.
- Rangos diarios y semanales con offset `-03:00`.
- `/css/styles.css` y viewport en `frontend/index.html` y `frontend/admin.html`; build copia `css/` a `dist/css/`. La hoja suministrada por el usuario (`C:\Users\santi\Downloads\styles.css`) se copió exactamente a `frontend/css/styles.css`.

**Ampliación (segunda iteración):**
- **Horas locales en la UI pública:** la confirmación de reserva y las consultas de estado muestran fecha y hora de Buenos Aires, nunca el `Instant` UTC.
- **Bandeja de entrada simulada del paciente:** la segunda sección de la página pública pasa a ser una simulación del correo del paciente. Se ingresa un email y se listan los mensajes dirigidos a ese email, cada uno con asunto, email, DNI, fecha de envío y la fecha/hora local del turno, más tres botones: **Ver estado del turno**, **Confirmar turno** y **Cancelar turno**. Para eso se agrega un endpoint público de solo lectura `GET /api/public/inbox?email=`, habilitado únicamente con la bandera de demo `app.demo.inbox.enabled` (activa en el perfil `local`, inactiva por defecto).
- **Acceso manual por enlace:** se conserva como alternativa secundaria, con botones explícitos **Confirmar turno** y **Cancelar turno** junto al detalle y **Consultar estado** debajo; se elimina el selector "Acción" sin botón.
- **El odontólogo no confirma turnos:** la agenda ofrece solo **Marcar ausente**, **Marcar atendido** y **Cancelar**. El backend rechaza `PATCH /api/appointments/{id}/status` con `CONFIRMED` (422); la confirmación queda solo por token del paciente. Se actualiza la KB.
- **Franja legible en la agenda:** `activeSlot` no nulo se muestra como rango local `HH:mm–HH:mm` de Buenos Aires.
- **Búsqueda de pacientes por DNI o nombre:** `GET /api/patients` acepta `nombre` (coincidencia parcial sin distinguir mayúsculas sobre nombre o apellido) como alternativa exclusiva a `dni`; enviar ambos responde 400. La consola ofrece elegir el criterio.
- **Bandeja de salida como historial:** cada mensaje conserva un único botón **Ver estado del turno** (solo `GET`); se quitan los botones de confirmar/cancelar del admin.

Fuera de alcance: frameworks, fuentes externas, correo SMTP real, autenticación de pacientes, edición del CSS suministrado, cambios a C-14 o a `CHANGES.md`.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `admin-agenda-ui`: agenda legible y rangos locales (primera iteración); acciones de estado sin confirmar, franja local, búsqueda de pacientes por DNI o nombre y bandeja de salida como historial.
- `public-booking-ui`: horas locales en confirmación y estado; bandeja de entrada simulada con acciones directas; acceso manual por enlace con botones explícitos.
- `gestion-turnos`: el odontólogo no puede confirmar turnos por la API administrativa.
- `gestion-pacientes`: búsqueda por nombre exclusiva respecto de la búsqueda por DNI.
- `outbox-reminders`: lectura pública simulada de mensajes por email, solo con la bandera de demo activa.

## Impact

- Frontend vanilla TypeScript: `frontend/src/admin/*` y `frontend/src/public-reservation/*`, con sus pruebas; páginas de entrada y build.
- Backend: `AppointmentController`/`AppointmentService` (rechazo de `CONFIRMED` por odontólogo), `PatientController`/`PatientService`/`PatientRepository` (filtro `nombre`), nuevo controlador público de bandeja simulada en `outbox`, configuración `app.demo.inbox.enabled` en `application.yml`/`application-local.yml`/`application-test.yml` y la regla de seguridad que permite su ruta pública.
- KB: `05_reglas_de_negocio.md` (quién confirma), `03_actores_y_roles.md` (bandeja simulada del paciente), `06_funcionalidades.md` (búsqueda por nombre, bandeja de entrada simulada), `02_descripcion_general.md` (endpoint nuevo).
- **Gobernanza:** la bandeja de entrada simulada expone, sin sesión, enlaces con token que permiten confirmar o cancelar turnos de quien conozca un email. El usuario aceptó este riesgo explícitamente por tratarse de una simulación de correo; se mitiga con la bandera de demo desactivada por defecto. Es un punto de gobernanza ALTA que requiere aprobación de este artefacto antes de apply.
- Diseño visual: design system Revision 1 aprobado; política `system-only`, sin mockups por change.
