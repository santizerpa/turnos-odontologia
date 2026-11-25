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

La consola SHALL construir la agenda diaria y semanal solo con `GET /api/appointments?from=&to=`, con estados vacío o de error. Para los nombres SHALL hacer una sola llamada `GET /api/patients?page=0&size=100` al cargar la agenda (`100` es el máximo), construir un mapa `patientId → nombre y apellido` sin más páginas ni reutilizar la vista de pacientes, y usar el `patientId` si el id no está. Un fallo distinto de 401 no es error de agenda; el 401 aplica el manejo de sesión vencida.

#### Scenario: Agenda diaria con turnos
- **DADO (GIVEN)** un rango `from`/`to` válido en formato `Instant` ISO-8601 y turnos ficticios activos
- **CUANDO (WHEN)** `GET /api/appointments?from=&to=` responde 200 con turnos ordenados por `startAt`
- **ENTONCES (THEN)** la consola muestra `id`, `patientId`, `startAt`, `durationMinutes`, `status` y `activeSlot` de cada turno, sin inventar datos adicionales

#### Scenario: Nombre del paciente en la agenda
- **DADO (GIVEN)** una agenda con turnos ficticios y un listado de hasta 100 pacientes disponible
- **CUANDO (WHEN)** la consola carga la agenda y realiza exactamente una llamada `GET /api/patients?page=0&size=100`
- **ENTONCES (THEN)** construye un mapa `patientId → nombre y apellido` con esos pacientes, muestra `nombre` y `apellido` en cada turno, no solicita más páginas, no reutiliza lo cargado en la vista de pacientes y no invoca `GET /api/patients/{id}`

#### Scenario: Paciente fuera del mapa
- **DADO (GIVEN)** un turno cuyo `patientId` no está entre los primeros 100 pacientes devueltos por `GET /api/patients?page=0&size=100` (por ejemplo, hay más de 100 pacientes)
- **CUANDO (WHEN)** la consola renderiza ese turno con el mapa `patientId → nombre y apellido`
- **ENTONCES (THEN)** muestra el `patientId` en lugar de un nombre, sin solicitar páginas adicionales ni inventar datos

#### Scenario: Fallo del listado de pacientes (distinto de 401)
- **DADO (GIVEN)** un fallo en `GET /api/patients?page=0&size=100` distinto de 401
- **CUANDO (WHEN)** la consola carga la agenda con turnos ficticios
- **ENTONCES (THEN)** la agenda se renderiza igual, mostrando el `patientId` de cada turno y sin presentar el fallo como error de la agenda; si esa llamada responde 401, se aplica el manejo de sesión vencida ya especificado, sin redirigir

#### Scenario: Agenda semanal
- **DADO (GIVEN)** un rango de siete días con turnos ficticios
- **CUANDO (WHEN)** la consola solicita ese rango a `GET /api/appointments`
- **ENTONCES (THEN)** muestra los mismos turnos del rango ordenados por inicio y distingue la vista semanal de la diaria

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

La consola SHALL usar `PATCH /api/appointments/{id}/status` con `{"status":"CONFIRMED|ABSENT|ATTENDED|CANCELLED","reason":string|null}` para confirmar, marcar ausente, marcar atendido y cancelar, sin crear turnos ni franjas.

#### Scenario: Confirmar, marcar ausente o atendido
- **DADO (GIVEN)** un turno visible en la agenda y una transición válida
- **CUANDO (WHEN)** la consola envía `PATCH` con `status` `CONFIRMED`, `ABSENT` o `ATTENDED` y la API responde 200 con `{"status":"<ESTADO>","message":"estado actualizado"}`
- **ENTONCES (THEN)** la consola muestra el nuevo estado y refresca la agenda sin presentar la operación como cancelación

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

La consola SHALL obtener pacientes solo desde `GET /api/patients?dni=&page=&size=` y SHALL mostrar como detalle mínimo los campos devueltos por ese listado, sin inventar datos ni consumir rutas inexistentes de alta o detalle.

#### Scenario: Listado paginado
- **DADO (GIVEN)** pacientes ficticios persistidos
- **CUANDO (WHEN)** la consola solicita `GET /api/patients` con `page=0` y `size=10` y la API responde 200
- **ENTONCES (THEN)** muestra `content` con `id`, `dni`, `nombre`, `apellido` y `email`, además de `page`, `size`, `totalElements` y `totalPages`

#### Scenario: Detalle mínimo de un paciente
- **DADO (GIVEN)** un paciente presente en `content`
- **CUANDO (WHEN)** la consola abre su detalle
- **ENTONCES (THEN)** muestra exclusivamente `id`, `dni`, `nombre`, `apellido` y `email`, sin invocar una ruta de detalle inexistente ni solicitar alta de pacientes

#### Scenario: Paginación vacía
- **DADO (GIVEN)** un filtro `dni` sin coincidencias
- **CUANDO (WHEN)** la API responde 200 con `content: []` y metadatos de paginación
- **ENTONCES (THEN)** la consola muestra un estado "sin resultados" y conserva el filtro y la página visibles

#### Scenario: Parámetros de paciente inválidos
- **DADO (GIVEN)** un `dni` inválido o `page`/`size` fuera de rango
- **CUANDO (WHEN)** la API responde 400 con `{"type":"about:blank","title":"Bad Request","status":400,"detail":"Datos de paciente inválidos."}`, con o sin `instance`
- **ENTONCES (THEN)** la consola muestra `detail`, con o sin `instance`, y no interpreta el cuerpo como un objeto con campo `error`

### Requirement: La consola muestra la bandeja simulada y permite probar sus enlaces

La consola SHALL listar mensajes con `GET /api/outbox/messages`, SHALL extraer los enlaces tokenizados del texto de `body` y SHALL usar `GET` solo para información y `POST` para confirmar o cancelar mediante las rutas públicas ya existentes.

#### Scenario: Bandeja con mensajes
- **DADO (GIVEN)** mensajes ficticios en la bandeja simulada
- **CUANDO (WHEN)** `GET /api/outbox/messages` responde 200 con `id`, `appointmentId`, `recipientEmail`, `subject`, `body`, `status`, `type`, `createdAt` y `sentAt`
- **ENTONCES (THEN)** la consola muestra esos campos, admite `appointmentId` y `sentAt` nulos y presenta los enlaces contenidos en `body` sin exponer DNI, hash ni token persistido

#### Scenario: Bandeja vacía
- **DADO (GIVEN)** ningún mensaje simulado
- **CUANDO (WHEN)** `GET /api/outbox/messages` responde 200 con un array vacío
- **ENTONCES (THEN)** la consola muestra un estado "sin mensajes" y no ofrece enlaces de prueba

#### Scenario: Consulta informativa de un enlace
- **DADO (GIVEN)** un enlace `/api/public/appointments/{token}/confirm` o `/cancel` extraído de `body`
- **CUANDO (WHEN)** la consola realiza el `GET` público y la API responde 200 con `{"startAt":"<Instant>","status":"<ESTADO>"}`
- **ENTONCES (THEN)** muestra inicio y estado sin ejecutar ningún `POST` automático ni mutar el turno

#### Scenario: Acción correcta mediante un enlace
- **DADO (GIVEN)** un token vigente y un enlace de la bandeja
- **CUANDO (WHEN)** la consola envía el `POST` público correspondiente y la API responde 200 con `{"message":"confirmación exitosa","status":"CONFIRMED"}` o `{"message":"cancelación exitosa","status":"CANCELLED"}`
- **ENTONCES (THEN)** la consola muestra el mensaje devuelto y el estado informado, sin inventar una pantalla servida por el backend

#### Scenario: Acción repetida mediante un enlace
- **DADO (GIVEN)** un turno ya confirmado o cancelado
- **CUANDO (WHEN)** el `POST` público responde 200 con `{"message":"ya confirmado","status":"CONFIRMED"}` o `{"message":"ya cancelado","status":"CANCELLED"}`
- **ENTONCES (THEN)** la consola informa la repetición sin presentarla como una nueva operación

#### Scenario: Enlace inválido o vencido
- **DADO (GIVEN)** un token inexistente, inválido o vencido en un enlace de la bandeja
- **CUANDO (WHEN)** el `GET` o `POST` público responde 404 con `{"error":"Token inexistente o inválido"}` o 410 con `{"error":"Token vencido"}`
- **ENTONCES (THEN)** la consola muestra el mensaje exacto, no reintenta el `POST` y no presenta el turno como confirmado o cancelado

#### Scenario: Conflicto en la acción por enlace
- **DADO (GIVEN)** un token vigente pero un estado o plazo que impide la acción
- **CUANDO (WHEN)** el `POST` responde 409 con `{"error":"El turno no puede confirmarse"}`, `{"error":"No se pudo confirmar el turno"}`, `{"error":"Debe contactarse al consultorio para cancelar el turno"}` o `{"error":"No se pudo cancelar el turno"}`
- **ENTONCES (THEN)** la consola muestra el mensaje recibido y no cambia el estado local del turno

#### Scenario: Error interno de la bandeja
- **DADO (GIVEN)** un fallo interno al consultar los mensajes
- **CUANDO (WHEN)** la consola recibe una respuesta 500 de `GET /api/outbox/messages` con `{"type":"about:blank","title":"Error interno","status":500,"detail":"Error interno al procesar la bandeja"}`, con o sin `instance` (respuesta controlada en navegador, no generada por el backend en la verificación)
- **ENTONCES (THEN)** la consola muestra `detail`, sin mensajes internos ni trazas de excepción

### Requirement: La consola muestra la lista de espera visible

La consola SHALL consultar `GET /api/waiting-list` con sesión y SHALL mostrar las entradas en el orden FIFO devuelto, con estado y preferencias de fecha pero sin DNI. El alta en `POST /api/public/waiting-list` es un endpoint público y queda fuera del alcance de esta consola.

#### Scenario: Lista de espera con entradas
- **DADO (GIVEN)** entradas ficticias ordenadas por `created_at` ascendente y por `id` en empate
- **CUANDO (WHEN)** `GET /api/waiting-list` responde 200 con `id`, `nombre`, `apellido`, `email`, `requestedDateFrom`, `requestedDateTo` y `status`
- **ENTONCES (THEN)** la consola muestra las entradas en ese orden, sin DNI y sin asignar turnos automáticamente

#### Scenario: Lista de espera vacía
- **DADO (GIVEN)** ninguna entrada en la cola
- **CUANDO (WHEN)** `GET /api/waiting-list` responde 200 con un array vacío
- **ENTONCES (THEN)** la consola muestra un estado "sin entradas" y no ofrece acciones de asignación

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
