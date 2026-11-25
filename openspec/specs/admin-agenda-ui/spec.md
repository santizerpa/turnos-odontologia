# admin-agenda-ui Specification

## Purpose

Define el comportamiento observable de la consola web del odontólogo: acceso con sesión, agenda diaria y semanal con acciones de estado, consulta de pacientes, bandeja simulada con enlaces de prueba y lista de espera, incluida la presentación de los errores reales del backend.

## Requirements

### Requirement: La consola permite iniciar sesión con email y contraseña

La interfaz administrativa SHALL enviar email y contraseña a `POST /api/auth/login` y SHALL mostrar el perfil devuelto (`id`, `email`, `firstName`, `lastName`) sin exponer contraseñas ni hashes.

#### Scenario: Inicio de sesión correcto
- **DADO (GIVEN)** un odontólogo ficticio con credenciales válidas
- **CUANDO (WHEN)** la consola envía `POST /api/auth/login` con ese email y contraseña
- **ENTONCES (THEN)** la API responde 200 con `id`, `email`, `firstName` y `lastName`, sin hash de contraseña
- **Y** la consola muestra la agenda administrativa sin revelar la contraseña ingresada

#### Scenario: Credenciales inválidas
- **DADO (GIVEN)** un email o una contraseña incorrectos
- **CUANDO (WHEN)** `POST /api/auth/login` responde 401 con `{"error":"Credenciales inválidas"}`
- **ENTONCES (THEN)** la consola muestra ese mensaje sin indicar qué dato falló y no muestra la agenda

#### Scenario: Formulario incompleto
- **DADO (GIVEN)** el formulario de acceso sin email o sin contraseña
- **CUANDO (WHEN)** se intenta iniciar sesión y la API responde 401 con `{"error":"Credenciales inválidas"}`
- **ENTONCES (THEN)** la consola muestra el mismo mensaje genérico y no inicia ninguna sesión

### Requirement: La consola detecta la sesión vencida sin redirigir

Ante un 401 con `{"error":"No autenticado"}` en cualquier llamada autenticada, la consola SHALL mostrar el acceso de login en el mismo documento, SHALL descartar la acción que falló y SHALL NOT redirigir ni reenviar la solicitud automáticamente.

#### Scenario: Sesión vencida al cargar la agenda
- **DADO (GIVEN)** una sesión inexistente, cerrada o vencida por inactividad
- **CUANDO (WHEN)** `GET /api/appointments` responde 401 con `{"error":"No autenticado"}`
- **ENTONCES (THEN)** la consola muestra el formulario de login en la misma página, sin redirección ni presentación de error de agenda

#### Scenario: Sesión vencida al cambiar el estado de un turno
- **DADO (GIVEN)** una sesión que dejó de ser válida
- **CUANDO (WHEN)** `PATCH /api/appointments/{id}/status` responde 401 con `{"error":"No autenticado"}`
- **ENTONCES (THEN)** la consola no aplica el cambio localmente, muestra el acceso de login y no reintenta la acción automáticamente

#### Scenario: Sesión vencida en consultas auxiliares
- **DADO (GIVEN)** una sesión inválida
- **CUANDO (WHEN)** `GET /api/patients`, `GET /api/outbox/messages` o `GET /api/waiting-list` responde 401 con `{"error":"No autenticado"}`
- **ENTONCES (THEN)** la consola aplica el mismo manejo de sesión vencida, sin redirigir y sin mostrar datos de la consulta

#### Scenario: Cierre de sesión
- **DADO (GIVEN)** una sesión vigente
- **CUANDO (WHEN)** `POST /api/auth/logout` responde 204 sin cuerpo
- **ENTONCES (THEN)** la consola muestra el acceso de login y una llamada posterior a `GET /api/auth/me` con esa sesión responde 401

### Requirement: La consola muestra la agenda diaria y semanal con el estado de cada turno

La consola SHALL consultar la agenda diaria y semanal mediante `GET /api/appointments?from=&to=`, representar estados vacío y error, y excluir turnos cancelados de los activos. Para nombres SHALL hacer una sola llamada `GET /api/patients?page=0&size=100` (100 es el máximo), sin paginar ni reutilizar otra vista, y usar `patientId` si no hay nombre; un fallo distinto de 401 no invalida agenda y 401 aplica sesión vencida.

#### Scenario: Agenda diaria con turnos
- **DADO (GIVEN)** un rango `from`/`to` válido en formato `Instant` ISO-8601 y turnos ficticios activos
- **CUANDO (WHEN)** `GET /api/appointments?from=&to=` responde 200 con turnos ordenados por `startAt`
- **ENTONCES (THEN)** la consola presenta los turnos del rango diario en orden de inicio y conserva los datos recibidos para su presentación y acciones

#### Scenario: Agenda semanal
- **DADO (GIVEN)** un rango de siete días con turnos ficticios
- **CUANDO (WHEN)** la consola solicita ese rango a `GET /api/appointments`
- **ENTONCES (THEN)** presenta los turnos del rango semanal en orden de inicio y distingue la vista semanal de la diaria

#### Scenario: Nombre del paciente en la agenda
- **DADO (GIVEN)** una agenda con turnos ficticios y un listado de hasta 100 pacientes disponible
- **CUANDO (WHEN)** la consola carga la agenda y realiza exactamente una llamada `GET /api/patients?page=0&size=100`
- **ENTONCES (THEN)** construye un mapa `patientId → nombre y apellido` con esos pacientes, no solicita más páginas, no reutiliza lo cargado en la vista de pacientes y no invoca `GET /api/patients/{id}`

#### Scenario: Paciente fuera del mapa
- **DADO (GIVEN)** un turno cuyo `patientId` no está entre los primeros 100 pacientes devueltos por `GET /api/patients?page=0&size=100` (por ejemplo, hay más de 100 pacientes)
- **CUANDO (WHEN)** la consola resuelve los nombres de la agenda
- **ENTONCES (THEN)** usa `patientId` como fallback, sin solicitar páginas adicionales ni inventar datos

#### Scenario: Fallo del listado de pacientes (distinto de 401)
- **DADO (GIVEN)** un fallo en `GET /api/patients?page=0&size=100` distinto de 401
- **CUANDO (WHEN)** la consola carga la agenda con turnos ficticios
- **ENTONCES (THEN)** la agenda se renderiza igual y no presenta el fallo como error de agenda; si la llamada responde 401, se aplica el manejo de sesión vencida especificado, sin redirigir

#### Scenario: Agenda vacía
- **DADO (GIVEN)** un rango válido sin turnos activos
- **CUANDO (WHEN)** `GET /api/appointments` responde 200 con un array vacío
- **ENTONCES (THEN)** la consola muestra un estado "sin turnos" y no lo presenta como error

#### Scenario: Rango inválido o parámetros ausentes
- **DADO (GIVEN)** un rango con `from >= to` o parámetros `from`/`to` ausentes o no parseables
- **CUANDO (WHEN)** la API responde 422 con `{"error":"Rango de agenda inválido"}` o 400 con `{"error":"Parámetros from y to inválidos o faltantes"}`
- **ENTONCES (THEN)** la consola muestra el mensaje exacto del campo `error` y no reenvía la misma consulta

#### Scenario: Turno cancelado fuera de la agenda
- **DADO (GIVEN)** un turno `CANCELLED` cuyo `activeSlot` pasó a nulo
- **CUANDO (WHEN)** la consola carga un rango que incluye su inicio original
- **ENTONCES (THEN)** el turno no aparece como activo en la agenda, en coherencia con el filtro existente del backend

### Requirement: La consola cambia el estado de un turno con los contratos existentes

La consola SHALL usar `PATCH /api/appointments/{id}/status` con `{"status":"ABSENT|ATTENDED|CANCELLED","reason":string|null}` para marcar ausente, marcar atendido y cancelar, sin crear turnos ni franjas. La consola SHALL NOT ofrecer la acción de confirmar: la confirmación corresponde al paciente mediante su enlace.

#### Scenario: Confirmar, marcar ausente o atendido
- **DADO (GIVEN)** un turno visible en la agenda y una transición válida
- **CUANDO (WHEN)** la consola renderiza sus acciones y el odontólogo envía `PATCH` con `status` `ABSENT` o `ATTENDED`, y la API responde 200 con `{"status":"<ESTADO>","message":"estado actualizado"}`
- **ENTONCES (THEN)** la fila muestra exactamente "Marcar ausente", "Marcar atendido" y "Cancelar", sin ninguna acción "Confirmar", y tras la respuesta la consola muestra el nuevo estado y refresca la agenda sin presentar la operación como cancelación

#### Scenario: Cancelar un turno
- **DADO (GIVEN)** un turno cancelable desde la agenda
- **CUANDO (WHEN)** la consola envía `PATCH` con `status` `CANCELLED` y un motivo, y la API responde 200 con `{"cancelled":true,"message":"cancelado"}`
- **ENTONCES (THEN)** la consola informa la cancelación y el turno deja de mostrarse como activo en la agenda

#### Scenario: Cancelación repetida
- **DADO (GIVEN)** un turno ya cancelado
- **CUANDO (WHEN)** la API responde 200 con `{"cancelled":false,"message":"ya cancelado"}`
- **ENTONCES (THEN)** la consola muestra "ya cancelado" sin duplicar efectos ni cambiar otros turnos

#### Scenario: Transición inválida
- **DADO (GIVEN)** un turno en un estado que no permite la acción solicitada
- **CUANDO (WHEN)** la API responde 422 con `{"error":"Transición de estado inválida"}`
- **ENTONCES (THEN)** la consola muestra ese mensaje, conserva el estado previamente visible y no marca la acción como exitosa

#### Scenario: Solapamiento al cambiar el estado
- **DADO (GIVEN)** una franja ya ocupada
- **CUANDO (WHEN)** la API responde 409 con `{"error":"La franja solicitada ya está ocupada"}`
- **ENTONCES (THEN)** la consola muestra el mensaje de conflicto y no actualiza el estado local del turno

#### Scenario: Turno inexistente
- **DADO (GIVEN)** un `appointmentId` que no corresponde a ningún turno
- **CUANDO (WHEN)** la API responde 404 con `{"error":"Turno no encontrado: <uuid>"}`
- **ENTONCES (THEN)** la consola muestra el mensaje exacto y retira la acción sobre ese turno sin reintentarla

### Requirement: La consola lista pacientes y muestra el detalle mínimo disponible

La consola SHALL obtener pacientes solo desde `GET /api/patients` con `page`, `size` y, como máximo, uno de los filtros `dni` o `nombre`, elegido por el odontólogo, y SHALL mostrar como detalle mínimo los campos devueltos por ese listado, sin inventar datos ni consumir rutas inexistentes de alta o detalle.

#### Scenario: Listado paginado
- **DADO (GIVEN)** pacientes ficticios persistidos
- **CUANDO (WHEN)** la consola solicita `GET /api/patients` con `page=0` y `size=10` y la API responde 200
- **ENTONCES (THEN)** muestra `content` con `id`, `dni`, `nombre`, `apellido` y `email`, además de `page`, `size`, `totalElements` y `totalPages`

#### Scenario: Elegir el criterio de búsqueda
- **DADO (GIVEN)** la sección de pacientes con un selector de criterio "DNI" o "Nombre" y un único campo de búsqueda
- **CUANDO (WHEN)** el odontólogo elige "Nombre", escribe `ana` y busca
- **ENTONCES (THEN)** la consola envía `nombre=ana` y no envía `dni`; con "DNI" envía solo `dni`; con el campo vacío no envía ningún filtro

#### Scenario: Detalle mínimo de un paciente
- **DADO (GIVEN)** un paciente presente en `content`
- **CUANDO (WHEN)** la consola abre su detalle
- **ENTONCES (THEN)** muestra exclusivamente `id`, `dni`, `nombre`, `apellido` y `email`, sin invocar una ruta de detalle inexistente ni solicitar alta de pacientes

#### Scenario: Paginación vacía
- **DADO (GIVEN)** un filtro `dni` o `nombre` sin coincidencias
- **CUANDO (WHEN)** la API responde 200 con `content: []` y metadatos de paginación
- **ENTONCES (THEN)** la consola muestra un estado "sin resultados" y conserva el criterio, el filtro y la página visibles

#### Scenario: Parámetros de paciente inválidos
- **DADO (GIVEN)** un `dni` inválido o `page`/`size` fuera de rango
- **CUANDO (WHEN)** la API responde 400 con `{"type":"about:blank","title":"Bad Request","status":400,"detail":"Datos de paciente inválidos."}`, con o sin `instance`
- **ENTONCES (THEN)** la consola muestra `detail`, con o sin `instance`, y no interpreta el cuerpo como un objeto con campo `error`

### Requirement: La consola muestra la bandeja simulada y permite probar sus enlaces

La consola SHALL listar mensajes con `GET /api/outbox/messages` como historial de mensajes enviados, del más reciente al más antiguo, mostrando por mensaje asunto, email, nombre y DNI del paciente y fecha de envío local, sin el estado del mensaje. SHALL ofrecer por mensaje con enlace tokenizado una única acción "Ver estado del turno", que usa solo el `GET` público informativo y es donde se ve el estado del turno. La consola SHALL NOT confirmar ni cancelar turnos desde la bandeja de salida.

#### Scenario: Bandeja con mensajes
- **DADO (GIVEN)** mensajes ficticios en la bandeja simulada
- **CUANDO (WHEN)** `GET /api/outbox/messages` responde 200 ordenado del más reciente al más antiguo, con `patientName` y `patientDni` por mensaje
- **ENTONCES (THEN)** la consola muestra cada mensaje como asunto · email · nombre del paciente · DNI · fecha local, en ese orden de llegada, sin el estado `PENDING` ni `SIMULATED_SENT`, y ofrece exactamente un botón "Ver estado del turno" por mensaje con enlace, sin botones de confirmar ni cancelar

#### Scenario: Mensaje sin paciente asociado
- **DADO (GIVEN)** un mensaje sin turno, con `patientName` y `patientDni` nulos
- **CUANDO (WHEN)** la consola lo muestra
- **ENTONCES (THEN)** omite esos datos sin mostrar `null` ni texto de reemplazo

#### Scenario: Bandeja vacía
- **DADO (GIVEN)** ningún mensaje simulado
- **CUANDO (WHEN)** `GET /api/outbox/messages` responde 200 con un array vacío
- **ENTONCES (THEN)** la consola muestra un estado "sin mensajes" y no ofrece acciones

#### Scenario: Consulta informativa de un enlace
- **DADO (GIVEN)** un mensaje con enlace `/api/public/appointments/{token}/confirm` o `/cancel` en `body`
- **CUANDO (WHEN)** el odontólogo pulsa "Ver estado del turno" y el `GET` público responde 200 con `{"startAt":"<Instant>","status":"<ESTADO>"}`
- **ENTONCES (THEN)** la consola muestra el inicio en fecha y hora locales de Buenos Aires y el estado, sin ejecutar ningún `POST`

#### Scenario: Acción correcta mediante un enlace
- **DADO (GIVEN)** un mensaje con un enlace vigente de confirmación o cancelación
- **CUANDO (WHEN)** el odontólogo revisa ese mensaje en la bandeja de salida
- **ENTONCES (THEN)** la consola no ofrece confirmar ni cancelar y no envía ningún `POST`; esas acciones las realiza el paciente desde su bandeja de entrada simulada

#### Scenario: Acción repetida mediante un enlace
- **DADO (GIVEN)** un turno ya confirmado o cancelado por el paciente
- **CUANDO (WHEN)** el odontólogo pulsa "Ver estado del turno" en su mensaje
- **ENTONCES (THEN)** la consola muestra el estado `CONFIRMED` o `CANCELLED` informado por el `GET`, sin repetir ni ofrecer la acción

#### Scenario: Conflicto en la acción por enlace
- **DADO (GIVEN)** un token vigente cuyo turno ya no admite confirmación o cancelación
- **CUANDO (WHEN)** el odontólogo consulta su estado desde la bandeja de salida
- **ENTONCES (THEN)** la consola muestra solo el estado informado, no intenta la acción y no puede producir los errores 409 de confirmación o cancelación

#### Scenario: Enlace inválido o vencido
- **DADO (GIVEN)** un token inexistente, inválido o vencido en un enlace de la bandeja
- **CUANDO (WHEN)** el `GET` público responde 404 con `{"error":"Token inexistente o inválido"}` o 410 con `{"error":"Token vencido"}`
- **ENTONCES (THEN)** la consola muestra el mensaje exacto y no presenta el turno como confirmado o cancelado

#### Scenario: Error interno de la bandeja
- **DADO (GIVEN)** un fallo interno al consultar los mensajes
- **CUANDO (WHEN)** la consola recibe una respuesta 500 de `GET /api/outbox/messages` con `{"type":"about:blank","title":"Error interno","status":500,"detail":"Error interno al procesar la bandeja"}`, con o sin `instance` (respuesta controlada en navegador, no generada por el backend en la verificación)
- **ENTONCES (THEN)** la consola muestra `detail`, sin mensajes internos ni trazas de excepción

### Requirement: La consola interpreta ambos formatos de error del backend

La consola SHALL reconocer los cuerpos `{"error":"<mensaje>"}` y `ProblemDetail` con `detail`, SHALL mostrar el mensaje visible correspondiente y SHALL usar un mensaje genérico cuando el cuerpo no sea legible.

#### Scenario: Errores con campo `error`
- **DADO (GIVEN)** una respuesta de auth, turnos, lista de espera o bandeja con `{"error":"<mensaje>"}`
- **CUANDO (WHEN)** la consola procesa la respuesta con estado 401, 404, 409, 410 o 422
- **ENTONCES (THEN)** muestra el valor exacto de `error` y no busca un campo `detail`

#### Scenario: Errores `ProblemDetail`
- **DADO (GIVEN)** el cuerpo `{"type":"about:blank","title":"Bad Request","status":400,"detail":"Datos de paciente inválidos."}` o `{"type":"about:blank","title":"Error interno","status":500,"detail":"Error interno al procesar la bandeja"}`, con o sin `instance`
- **CUANDO (WHEN)** la consola procesa la respuesta
- **ENTONCES (THEN)** muestra el valor exacto de `detail`, con o sin `instance`, y no muestra campos internos

#### Scenario: Cuerpo de error no legible
- **DADO (GIVEN)** una respuesta sin JSON o sin `error` ni `detail` aprovechables
- **CUANDO (WHEN)** la consola procesa la respuesta
- **ENTONCES (THEN)** muestra `No se pudo completar la solicitud. Intente nuevamente más tarde.` y no presenta el fallo como éxito

### Requirement: Cada fila de agenda identifica semánticamente el estado del turno

Cada fila SHALL exponer en `data-status` exactamente el estado recibido y conservar el identificador de turno requerido por sus acciones.

#### Scenario: Turno pendiente
- **DADO (GIVEN)** un turno ficticio con estado `PENDING`
- **CUANDO (WHEN)** la consola renderiza su fila
- **ENTONCES (THEN)** la fila contiene `data-status="PENDING"`

#### Scenario: Turno confirmado o atendido
- **DADO (GIVEN)** turnos ficticios con estados `CONFIRMED` y `ATTENDED`
- **CUANDO (WHEN)** la consola renderiza ambas filas
- **ENTONCES (THEN)** cada atributo `data-status` coincide exactamente con el estado de su turno

#### Scenario: Acciones conservan el identificador
- **DADO (GIVEN)** una fila de agenda con acciones disponibles
- **CUANDO (WHEN)** la consola renderiza el turno
- **ENTONCES (THEN)** mantiene el id de turno usado por las acciones además del atributo `data-status`

### Requirement: Cada fila presenta los datos del turno en formato legible

La fila SHALL mostrar el inicio corto de Buenos Aires, la duración y el nombre del paciente sin UUID redundante; `activeSlot` nulo se presenta como “franja libre” y uno no nulo como el rango horario local de Buenos Aires desde su inicio hasta el fin según la duración del turno, sin exponer el `Instant` UTC.

#### Scenario: Inicio local del turno
- **DADO (GIVEN)** un turno con `startAt` ISO-8601
- **CUANDO (WHEN)** la consola presenta su fila
- **ENTONCES (THEN)** muestra fecha y hora cortas en español de Argentina para `America/Argentina/Buenos_Aires`, sin exponer el `Instant` UTC como texto técnico

#### Scenario: Nombre de paciente disponible
- **DADO (GIVEN)** un turno asociado a un paciente con nombre y apellido conocidos
- **CUANDO (WHEN)** la consola presenta su fila
- **ENTONCES (THEN)** muestra nombre, apellido y duración, sin añadir el `patientId` entre paréntesis

#### Scenario: Fallback visible sin duplicación
- **DADO (GIVEN)** un turno cuyo paciente no tiene nombre disponible
- **CUANDO (WHEN)** la consola presenta su fila
- **ENTONCES (THEN)** muestra `patientId` como texto de reemplazo, sin repetirlo entre paréntesis

#### Scenario: Franja activa nula o presente
- **DADO (GIVEN)** turnos con `activeSlot` nulo y no nulo
- **CUANDO (WHEN)** la consola presenta ambas filas
- **ENTONCES (THEN)** muestra “franja libre” para el nulo y, para el no nulo, el rango local (por ejemplo `14:30–15:00` para `activeSlot` `2026-11-26T17:30:00Z` y 30 minutos), sin el texto `2026-11-26T17:30:00Z`

### Requirement: Los rangos diarios y semanales de agenda usan la hora local de Buenos Aires

La consola SHALL representar los límites de los rangos diarios y semanales seleccionados como medianoches locales de Buenos Aires, con offset `-03:00`, manteniendo rangos ISO-8601 válidos y exclusivos en su límite superior.

#### Scenario: Rango diario
- **DADO (GIVEN)** una fecha válida elegida para consultar la agenda
- **CUANDO (WHEN)** la consola genera el rango diario
- **ENTONCES (THEN)** `from` corresponde a la medianoche local de esa fecha con `-03:00` y `to` a la medianoche local del día siguiente con `-03:00`, sin sufijo `Z`

#### Scenario: Rango semanal
- **DADO (GIVEN)** una fecha válida elegida para consultar la agenda
- **CUANDO (WHEN)** la consola genera el rango semanal
- **ENTONCES (THEN)** `from` corresponde a la medianoche local de esa fecha y `to` a la medianoche local siete días después, ambos con `-03:00` y sin sufijo `Z`

#### Scenario: Rango que cruza el fin de año
- **DADO (GIVEN)** una fecha válida cercana al fin de año
- **CUANDO (WHEN)** la consola genera un rango diario o semanal que cruza al año siguiente
- **ENTONCES (THEN)** conserva el número de días correspondiente, el cambio de año correcto y el offset `-03:00` en ambos límites

### Requirement: Las páginas frontend cargan la hoja de estilos local y declaran el viewport

Las páginas pública y administrativa SHALL enlazar el stylesheet local `/css/styles.css` y declarar un viewport adaptable; el build frontend SHALL publicar el directorio CSS bajo `dist/css/`.

#### Scenario: Entrada pública
- **DADO (GIVEN)** el HTML de entrada pública del frontend
- **CUANDO (WHEN)** se sirve `frontend/index.html`
- **ENTONCES (THEN)** incluye `<link rel="stylesheet" href="/css/styles.css">` y `<meta name="viewport" content="width=device-width, initial-scale=1">`

#### Scenario: Entrada administrativa
- **DADO (GIVEN)** el HTML de entrada administrativa del frontend
- **CUANDO (WHEN)** se sirve `frontend/admin.html`
- **ENTONCES (THEN)** incluye `<link rel="stylesheet" href="/css/styles.css">` y `<meta name="viewport" content="width=device-width, initial-scale=1">`

#### Scenario: Publicación del recurso CSS
- **DADO (GIVEN)** que existe la fuente local `frontend/css/styles.css`
- **CUANDO (WHEN)** se ejecuta el build frontend existente
- **ENTONCES (THEN)** el contenido de `frontend/css/` se copia a `frontend/dist/css/` junto a las páginas y bundles compilados
