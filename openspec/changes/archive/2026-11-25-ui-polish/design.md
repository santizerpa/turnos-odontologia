# Design

## Context

Ver `proposal.md` para la motivación y `specs/admin-agenda-ui/spec.md` para el contrato separado por comportamiento. La agenda se arma en `frontend/src/admin/agenda.ts` y sus filas/textos en `frontend/src/admin/ui.ts`; hoy se muestran `startAt`, paciente con `patientId` entre paréntesis y `activeSlot` crudos. `dailyRange`/`weeklyRange` parten de medianoche UTC. Las entradas son `frontend/index.html` y `frontend/admin.html`; el build de `frontend/package.json` copia esas páginas, pero no recursos CSS. El matcher de recursos estáticos comunes en `AuthSecurityConfiguration` ya deja público CSS, por lo que este diseño no cambia backend.

El usuario proporcionó `C:\Users\santi\Downloads\styles.css` (448 líneas), disponible en el entorno actual. Es una hoja local sin dependencias, con paleta (primario `#0f6f8f`, oscuro `#0b5a75`), neutrales, colores semánticos, radio de 8 px, selectores para el DOM público/administrativo existente, estados `data-status`, reglas responsive y fuentes del sistema. Indica que no requiere fuentes externas ni JavaScript. Durante apply se copiará exactamente a `frontend/css/styles.css`; la ruta de descarga es solo procedencia de la entrada, no una dependencia de runtime. Este update no la copia ni modifica.

## Goals / Non-Goals

**Goals:**
- Hacer legible la agenda con fecha/hora local de Buenos Aires, nombre sin UUID redundante, franja nula explícita y estado semántico por fila.
- Expresar rangos de fecha como límites locales con `-03:00` y añadir la carga/copia del stylesheet suministrado en las dos entradas frontend.
- Mantener compatibilidad con endpoints y payloads actuales. Copiar exactamente el CSS suministrado sin dependencias nuevas.

**Non-Goals:**
- Cambiar contratos o comportamiento backend, autenticación, seguridad, frameworks o tipografías externas.
- Editar el CSS suministrado, crear mockups o alterar el alcance de C-14 o `CHANGES.md`.

## Decisions

1. **Localizar solo la presentación del inicio.** Formatear `startAt` mediante `Intl.DateTimeFormat('es-AR', { timeZone: 'America/Argentina/Buenos_Aires', dateStyle: 'short', timeStyle: 'short' })`. El API y `AgendaEntry` conservan valores ISO; el formato es de vista. La alternativa de dejar visible el `Instant` sin convertir mantiene el problema actual.
2. **Separar consulta y presentación de agenda.** Mantener el contrato de una consulta de agenda y una consulta auxiliar de pacientes; especificar la semántica del estado por fila y la lectura de sus datos en requisitos propios. El mapa de pacientes usa una sola página de hasta 100 registros; el fallback es `patientId`.
3. **No renderizar el UUID del paciente como dato secundario.** La fila usa `patientName` como representación visible, que ya contiene el nombre o el fallback `patientId`; no añade además `(${patientId})`. Mantener el `id` en el elemento/estado que necesita las acciones. Colocar exactamente el estado recibido en `data-status` de cada fila para identificarla sin introducir una API nueva.
4. **Tratar `activeSlot` de forma conservadora.** Cuando es `null`, mostrar “franja libre”; si no es nulo, mantener el valor actual sin una nueva normalización no solicitada. No cambiar el filtrado de turnos cancelados.
5. **Construir límites locales con offset explícito.** Interpretar la fecha seleccionada como medianoche de Buenos Aires y sumar un día o siete días de calendario para el límite superior; serializar ambos límites con `-03:00`. No usar `toISOString()` para construir esos límites, porque vuelve a expresarlos como `Z`.
6. **Copiar la fuente CSS exacta y mantener el bundle actual.** En apply, copiar sin modificaciones el archivo suministrado a `frontend/css/styles.css`, enlazar la URL estable `/css/styles.css` en ambas páginas y extender el comando Node existente del build para copiar recursivamente `css/` a `dist/css/`. No agregar herramientas, dependencias, fuentes externas ni JavaScript; no tocar allowlists backend.
7. **No crear mockups por política.** El usuario seleccionó `system-only`: no hay mockups por cambio. No se encontró un design system aprobado, así que su creación y aprobación quedan como dependencia obligatoria antes de apply; este documento no afirma que exista ni que esté aprobado.

## Risks / Trade-offs

- [El runtime puede producir una representación localizada distinta en puntuación/espaciado] → Probar con `Intl.DateTimeFormat` usando exactamente locale, zona y opciones acordadas; no comparar contra el `Instant` UTC.
- [Una copia incompleta del CSS suministrado afectaría estilos o estados] → Copiar exactamente la fuente disponible a `frontend/css/styles.css` durante apply y verificar ruta de salida y carga del recurso.
- [Un `data-status` incompleto dejaría filas sin identificación semántica] → Verificar todas las filas renderizadas para estados diversos, no solo el primer turno.

## Migration Plan

No hay migración de datos ni despliegue backend. Al aplicar, copiar primero la fuente CSS suministrada sin modificarla; actualizar páginas y build junto con los tests, ejecutar `npm test` en `frontend/` y verificar que ambas entradas resuelven `/css/styles.css` desde el bundle publicado. Rollback: revertir solo los cambios del frontend de este change y retirar el enlace/copia; no afecta API ni datos.

## Open Questions

- Crear y aprobar el design system del proyecto antes de implementar cambios UI, según política `system-only`.

## Segunda iteración (revisión del usuario, 2026-11-25)

### Context

Al probar la primera iteración contra PostgreSQL, el usuario pidió, dentro de este mismo change: horas locales en la UI pública, una bandeja de entrada simulada del paciente, botones explícitos en el acceso por enlace, quitar "Confirmar" al odontólogo (también en backend), franja legible en agenda, búsqueda de pacientes por nombre y una bandeja de salida solo informativa. El usuario aclaró que la bandeja de entrada representa el correo del paciente: es una simulación de demo, no una función pública real; identificar los mensajes solo por email es aceptable en ese contexto.

### Decisions

8. **Un formateador local compartido por superficie.** La UI pública y la consola usan `Intl.DateTimeFormat('es-AR', { timeZone: 'America/Argentina/Buenos_Aires', hourCycle: 'h23', ... })` armado por partes, con el formato visible `dd/MM/yy HH:mm` en ambas superficies. Reemplaza en la consola el `dateStyle/timeStyle: 'short'` de la primera iteración, que en Node/Chrome mostraba 12 horas ("2:00 p. m.") junto a la franja en 24 horas. La UI pública formatea `startAt` de la confirmación, las consultas de estado y la bandeja de entrada; la consola, el inicio de agenda, la fecha de la bandeja de salida y `activeSlot` no nulo como `HH:mm–HH:mm` (inicio + `durationMinutes`). Sin dependencias nuevas. La lista de espera conserva su formato actual (fuera de alcance).
9. **Endpoint público de solo lectura con bandera de demo.** `GET /api/public/inbox?email=` en un controlador nuevo del paquete `outbox`, permitido en `AuthSecurityConfiguration` junto a las rutas `/api/public/**` existentes. Lee la propiedad `app.demo.inbox.enabled` (por defecto `false` en `application.yml`; `true` en `application-local.yml` y en el perfil de test que lo verifica). Deshabilitado responde 404 con `{"error":"Bandeja simulada deshabilitada"}`. El email se normaliza igual que en `Patient` (trim + minúsculas) y se valida con la misma regla de formato de email del alta. La respuesta es un DTO propio (`PatientInboxMessageDto`) que agrega `patientDni`, `appointmentStartAt` y `appointmentDurationMinutes`; no se modifica `OutboxMessageDto` ni el contrato autenticado de `GET /api/outbox/messages`. Alternativa descartada: reutilizar el endpoint autenticado, porque obligaría a iniciar sesión de odontólogo para simular el correo del paciente.
10. **El token se obtiene del `body` en el cliente.** La UI pública reutiliza el patrón de extracción de enlaces ya usado por la consola (`/api/public/appointments/{token}/confirm|cancel`) para no exponer un campo `token` adicional en la API.
11. **Rechazo de `CONFIRMED` en el controlador administrativo.** `PATCH /api/appointments/{id}/status` responde 422 `{"error":"La confirmación del turno la realiza el paciente"}` antes de delegar al servicio, mediante una excepción propia mapeada en `AppointmentExceptionHandler`. La matriz de estados y el flujo por token no cambian. Se actualiza RN en la KB.
12. **Filtro `nombre` exclusivo.** `PatientController` recibe `nombre` opcional; si `dni` y `nombre` llegan no vacíos, se lanza la excepción de datos inválidos existente (400 ProblemDetail). `PatientRepository` agrega una consulta paginada con `lower(...) like lower(concat('%', :q, '%'))` sobre `firstName`, `lastName` y la concatenación `firstName || ' ' || lastName`, portable entre H2 (modo PostgreSQL) y PostgreSQL. No se normalizan acentos.
13. **Bandeja de salida como historial.** La consola conserva un único botón "Ver estado del turno" por mensaje, que usa el `GET` informativo; se eliminan sus `POST`.
14. **Acceso manual por enlace secundario.** El selector "Acción" se reemplaza por botones "Confirmar turno" y "Cancelar turno" junto al detalle y "Consultar estado" debajo; queda después de la bandeja de entrada simulada.

15. **Tercera iteración (revisión del usuario).** (a) La lista de espera deja de mostrarse en la consola: es un mecanismo interno de aviso; el backend no cambia. (b) `OutboxMessageDto` agrega `patientName` y `patientDni`, y `OutboxController` lista con `findAllByOrderByCreatedAtDesc`; la consola reemplaza el estado del mensaje (siempre `PENDING`, engañoso) por nombre y DNI. Mostrar el DNI al odontólogo autenticado no amplía exposición: ya lo ve en Pacientes. (c) Se elimina el acceso manual por token de la página pública (deja sin efecto la decisión 14): el paciente no conoce tokens. (d) Los errores de la bandeja de entrada usan mensajes propios, sin pistas de enlace; un 404 genérico (backend sin la ruta, por ejemplo uno iniciado antes de este cambio) se informa como bandeja no disponible.

### Risks / Trade-offs

- [Cualquiera que conozca un email puede, con la bandera activa, leer sus mensajes y confirmar o cancelar ese turno] → Aceptado explícitamente por el usuario como simulación de demo; bandera desactivada por defecto, activa solo en `local`; la sección se rotula como simulación de correo; registrado en la KB como supuesto de demo.
- [El endpoint público expone el DNI del paciente al destinatario consultado] → Solo datos ficticios en demo; el DNI solo se devuelve en mensajes del email consultado.
- [`like` sin normalizar acentos: "Maria" no encuentra "María"] → Aceptado para v1; documentado.

## UI Mockups

- **Design system revision:** 1 (`design-system/README.md`, Approval: approved 2026-11-25, by user).
- **Decision:** no (project policy, `system-only`).
- **Mockups:** ninguno; la política `system-only` no requiere mockups por change.
- **Visual verify:** passed (2026-11-25) — `frontend/dist` servido localmente con respuestas de API ficticias (sin backend ni PostgreSQL); consola admin a 1440×900 y 375×812: `/css/styles.css` cargado, filas con `data-status` y borde por estado, rangos `-03:00`, sin desbordamiento horizontal ni errores de consola. No verificado contra el backend real.
- **Visual verify (segunda iteración):** passed (2026-11-25) — backend real en `localhost:8081` (perfil `local`, PostgreSQL local con datos ficticios), solo lecturas: bandeja de entrada de `uno@ejemplo.com` con encabezado, turno `26/11/26 14:30`, tres botones y "Ver estado del turno" (`GET`); acceso manual con botones explícitos y sin selector; agenda `26/11/26 14:00 · … · franja: 14:00–14:30` con solo "Marcar ausente / Marcar atendido / Cancelar"; búsqueda por nombre `PEP`; bandeja de salida con fecha local y un único botón. API: inbox 200/422, `dni`+`nombre` 400, `CONFIRMED` del odontólogo 422. Viewports 1440×900 y 375×812 sin desbordamiento ni errores de consola. **No ejecutado en navegador:** los `POST` de confirmar/cancelar (para no modificar la base del usuario); cubiertos por tests. Suites: frontend 109/109, backend 246/246 (`gradlew.bat build`).
- **Visual verify (tercera iteración):** passed (2026-11-25) — backend real en `localhost:8081`, solo lecturas. Consola: secciones Agenda, Pacientes y Bandeja de salida (sin lista de espera); bandeja con "asunto · email · nombre · DNI · fecha local", del más reciente (25/11/26 13:55) al más antiguo (12:57), un botón por mensaje. Página pública: solo "Reservar un turno" y "Bandeja de entrada simulada", sin campos de token; email inválido muestra "Email inválido" sin pistas de enlace. Sin desbordamiento a 1440×900. El "Not Found" que vio el usuario venía de un backend iniciado antes de que existiera la ruta; ahora se informa como bandeja no disponible. Suites: frontend 107/107, backend 247/247.
