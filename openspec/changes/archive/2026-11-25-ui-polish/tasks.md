# Tasks

## 1. Preparar recursos y política de UI

- [x] 1.1 Durante apply, copiar exactamente la fuente CSS suministrada por el usuario (`C:\Users\santi\Downloads\styles.css`, disponible en el entorno actual) a `frontend/css/styles.css`; esta ruta es solo el origen de la copia, no una dependencia de runtime. No editar el contenido ni agregar dependencias externas.
- [x] 1.2 Crear y obtener aprobación explícita del design system del proyecto según la política `system-only`; verificar `design-system/README.md` con revisión y `Approval: approved` antes de aplicar cambios visuales.

## 2. Presentación legible de agenda y rangos locales

- [x] 2.1 Actualizar `agendaEntryText` para mostrar `startAt` con `Intl.DateTimeFormat('es-AR', { timeZone: 'America/Argentina/Buenos_Aires', dateStyle: 'short', timeStyle: 'short' })`, el nombre sin UUID redundante, `activeSlot: null` como “franja libre” y el valor no nulo sin alterarlo. Ampliar `ui.test.ts` con al menos tres casos de nombre, fallback, franjas y formato local, más la resolución de pacientes especificada.
- [x] 2.2 Asignar `data-status` a cada fila renderizada sin perder el identificador usado por sus acciones; probar varios estados recibidos y comprobar que el nombre visible no añade el UUID entre paréntesis.
- [x] 2.3 Cambiar `dailyRange` y `weeklyRange` a límites de calendario en Buenos Aires con `-03:00`; actualizar `agenda.test.ts` para verificar el rango diario, el semanal y un límite que cruza el año, confirmando que no se genera sufijo `Z`.

## 3. Carga de stylesheet y build

- [x] 3.1 Añadir el enlace `/css/styles.css` y el viewport especificado en `frontend/index.html` y `frontend/admin.html`; ampliar una prueba automatizada que lea ambas entradas y verifique los dos elementos exactos en cada archivo.
- [x] 3.2 Extender el build existente para copiar recursivamente `frontend/css/` a `frontend/dist/css/`; ampliar `build.test.ts` para comprobar el CSS copiado exactamente y verificar que ambas entradas compiladas conservan sus enlaces.

## 4. Verificación integrada

- [x] 4.1 Ejecutar `npm test` desde `frontend/` después de completar las tareas anteriores y confirmar que compila las dos entradas, ejecuta las pruebas existentes y nuevas, y deja los recursos CSS publicados en `dist/css/`.

## 5. Horas locales y franja legible

- [x] 5.1 UI pública: mostrar `startAt` de la confirmación de reserva y de las consultas de estado en fecha y hora de Buenos Aires (`es-AR`, `America/Argentina/Buenos_Aires`); probar que `2026-11-26T17:30:00Z` se ve como 26/11/26 14:30 y nunca como texto UTC.
- [x] 5.2 Agenda: mostrar `activeSlot` no nulo como rango local `HH:mm–HH:mm` (inicio + duración); actualizar `ui.test.ts` (caso `17:30Z` + 30 min → `14:30–15:00`) sin perder "franja libre".

## 6. El odontólogo no confirma turnos

- [x] 6.1 Backend (TDD): `PATCH /api/appointments/{id}/status` con `CONFIRMED` responde 422 `{"error":"La confirmación del turno la realiza el paciente"}` sin cambiar estado ni registrar eventos; confirmar por token sigue funcionando; `ATTENDED`/`ABSENT`/`CANCELLED` sin cambios.
- [x] 6.2 Consola: acciones por fila exactamente "Marcar ausente", "Marcar atendido" y "Cancelar"; actualizar pruebas de `statusActionLabels`.

## 7. Búsqueda de pacientes por DNI o nombre

- [x] 7.1 Backend (TDD): filtro `nombre` en `GET /api/patients` (parcial, sin distinguir mayúsculas, sobre nombre, apellido y "nombre apellido", paginado); `dni` + `nombre` → 400; `nombre` vacío equivale a sin filtro. Probar en H2 modo PostgreSQL.
- [x] 7.2 Consola: selector "DNI | Nombre" con un único campo; la consulta envía solo el filtro elegido. Ampliar `patients.test.ts`.

## 8. Bandeja de entrada simulada y acceso por enlace

- [x] 8.1 Backend (TDD): `GET /api/public/inbox?email=` con bandera `app.demo.inbox.enabled` (default `false`; `true` en `application-local.yml`); 404 deshabilitada, 422 email inválido, 200 solo con mensajes del email normalizado, orden descendente por `createdAt`, campos permitidos; ruta pública en `AuthSecurityConfiguration`.
- [x] 8.2 UI pública: sección "Bandeja de entrada simulada (correo del paciente)" con campo email; cada mensaje muestra asunto · email · DNI · fecha de envío local y, debajo, el inicio local del turno, con "Ver estado del turno", "Confirmar turno" y "Cancelar turno" una sola vez por mensaje; estados vacío y error.
- [x] 8.3 UI pública: reemplazar el selector "Acción" del acceso manual por botones "Confirmar turno" y "Cancelar turno" junto al detalle, con "Consultar estado" debajo; token inválido muestra "Ingrese un token o enlace válido." sin llamar a la API.
- [x] 8.4 Consola: bandeja de salida como historial con un único botón "Ver estado del turno" por mensaje (solo `GET`, inicio en hora local); quitar los `POST` y sus pruebas.

## 9. KB y verificación

- [x] 9.1 Actualizar KB: `05_reglas_de_negocio.md` (la confirmación la realiza solo el paciente por token), `03_actores_y_roles.md` y `06_funcionalidades.md` (bandeja de entrada simulada, búsqueda por nombre), `02_descripcion_general.md` (`GET /api/public/inbox`) y `09_decisiones_y_supuestos.md` (supuesto de demo de la bandeja pública).
- [x] 9.2 Ejecutar `npm test` en `frontend/` y `gradlew.bat build` en `backend/`; verificar en navegador la página pública y la consola a 1440×900 y 375×812, y registrar el resultado en `design.md`.

## 10. Tercera iteración (revisión del usuario)

- [x] 10.1 Backend (TDD): `GET /api/outbox/messages` agrega `patientName` y `patientDni` (nulos sin turno) y ordena por `createdAt` descendente.
- [x] 10.2 Consola: la bandeja de salida muestra asunto · email · nombre · DNI · fecha local, sin el estado del mensaje, conservando un único "Ver estado del turno".
- [x] 10.3 Consola: quitar la sección de lista de espera del frontend (sección, render, módulo y llamadas de la consola); el backend no cambia.
- [x] 10.4 UI pública: quitar la sección "Ya tengo un enlace" y su lógica (reemplaza 8.3); la gestión queda solo en la bandeja de entrada simulada.
- [x] 10.5 UI pública: los errores de la bandeja no mencionan enlaces; un 404 sin el error propio de la bandeja muestra "La bandeja simulada no está disponible en este servidor.".
- [x] 10.6 KB (`03`, `06`), suites completas y verificación en navegador; registrar en `design.md`.
