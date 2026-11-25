# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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

La fila SHALL mostrar el inicio corto de Buenos Aires, la duración y el nombre del paciente sin UUID redundante; `activeSlot` nulo se presenta como “franja libre” y uno no nulo conserva su valor.

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
- **ENTONCES (THEN)** muestra “franja libre” para el nulo y conserva el valor actual del no nulo

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
