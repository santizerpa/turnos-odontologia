# disponibilidad-agenda Specification

## Purpose

Definir la agenda semanal del odontólogo, el cálculo determinista de slots de 30 minutos, la consulta de disponibilidad dentro de la ventana pública de reserva y los bloqueos manuales con baja lógica.

## Requirements

### Requirement: Cada odontólogo debe tener una configuración semanal persistida

El sistema SHALL mantener una única configuración de horario por odontólogo y SHALL crearla con los valores por defecto la primera vez que se consulta, sin exigir una carga manual previa.

#### Scenario: Primer acceso crea la configuración por defecto

- **GIVEN** un odontólogo autenticado sin configuración de horario
- **WHEN** solicita `GET /api/schedule`
- **THEN** la respuesta es 200 con lunes a viernes de 09:00 a 13:00 y de 14:00 a 19:00
- **AND** la timezone informada es `America/Argentina/Buenos_Aires`
- **AND** una segunda consulta devuelve la misma configuración sin duplicar filas

#### Scenario: La configuración de un odontólogo no afecta a otro

- **GIVEN** dos odontólogos con configuraciones distintas
- **WHEN** cada uno consulta su propio `GET /api/schedule`
- **THEN** cada respuesta contiene únicamente las franjas de su odontólogo

### Requirement: El horario semanal debe poder consultarse con los límites de la ventana pública

`GET /api/schedule` SHALL devolver la timezone fija, la duración de slot, los días con sus franjas y los límites de la ventana pública, para que la UI y la reserva pública no dupliquen constantes.

#### Scenario: Respuesta completa de configuración

- **GIVEN** un odontólogo autenticado
- **WHEN** solicita `GET /api/schedule`
- **THEN** la respuesta incluye `timezone`, `slotDurationMinutes: 30` y `publicWindow`
- **AND** `publicWindow` incluye `minNoticeHours: 24`, `maxDays: 30` y `timezone`
- **AND** cada día incluye su nombre y la lista de franjas con hora de inicio y fin
- **AND** la respuesta no incluye `effectiveFrom` ni `effectiveTo`

### Requirement: El reemplazo del horario semanal debe validar franjas y días

`PATCH /api/schedule` SHALL reemplazar por completo el horario semanal del odontólogo autenticado, SHALL exigir los cinco días hábiles una sola vez y SHALL rechazar con 422 las franjas invertidas, de duración menor a 30 minutos o solapadas dentro de un mismo día.

#### Scenario: Reemplazo válido del horario

- **GIVEN** un odontólogo autenticado con el horario por defecto
- **WHEN** envía `PATCH /api/schedule` con lunes a viernes y una sola franja de 10:00 a 12:00 por día
- **THEN** la respuesta es 200 con el horario nuevo
- **AND** una consulta posterior devuelve únicamente la franja de 10:00 a 12:00
- **AND** las franjas anteriores ya no están presentes

#### Scenario: Un día sin franjas queda cerrado

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `PATCH /api/schedule` con lunes a jueves con franjas y el viernes con lista vacía
- **THEN** la respuesta es 200
- **AND** el viernes queda configurado sin franjas y no genera slots

#### Scenario: Franja invertida o de duración nula

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `PATCH /api/schedule` con una franja cuyo fin no es posterior al inicio
- **THEN** la respuesta es 422 con un mensaje que identifica el día y la franja inválida
- **AND** la configuración anterior no se modifica

#### Scenario: Franjas solapadas en el mismo día

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `PATCH /api/schedule` con dos franjas del mismo día que comparten al menos un minuto
- **THEN** la respuesta es 422 con un mensaje que identifica el día
- **AND** la configuración anterior no se modifica

#### Scenario: Día hábil faltante o día no permitido

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `PATCH /api/schedule` sin alguno de los días lunes a viernes o incluye sábado o domingo
- **THEN** la respuesta es 422
- **AND** la configuración anterior no se modifica

#### Scenario: Intento de cambiar la timezone fija

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `PATCH /api/schedule` con una timezone distinta de `America/Argentina/Buenos_Aires`
- **THEN** la respuesta es 422
- **AND** la configuración anterior no se modifica

### Requirement: Los slots deben generarse en una grilla de 30 minutos por franja

El cálculo SHALL generar slots de 30 minutos anclados al inicio de cada franja, avanzando de 30 en 30 minutos e incluyendo solo los slots cuyo fin no supera el fin de la franja.

#### Scenario: Un día hábil completo genera 18 slots

- **GIVEN** la configuración por defecto de lunes a viernes
- **WHEN** se calculan los slots de un día hábil
- **THEN** se obtienen 18 slots de 30 minutos
- **AND** están presentes los inicios 09:00, 12:30, 14:00 y 18:30
- **AND** no existe un slot que comience a las 13:00 ni a las 19:00
- **AND** no hay slots durante la pausa de 13:00 a 14:00

#### Scenario: Una semana hábil completa genera 90 slots

- **GIVEN** la configuración por defecto
- **WHEN** se calculan los slots de lunes a domingo
- **THEN** se obtienen 90 slots
- **AND** sábado y domingo no aportan slots

#### Scenario: Un día sin franjas no genera slots

- **GIVEN** una configuración con un día sin franjas
- **WHEN** se calculan los slots de ese día
- **THEN** el día devuelve una lista vacía

#### Scenario: Una franja menor a 30 minutos es inválida

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `PATCH /api/schedule` con una franja de 09:00 a 09:20
- **THEN** la respuesta es 422 con un mensaje que identifica el día y la duración inválida
- **AND** la configuración anterior no se modifica

### Requirement: La consulta pública de disponibilidad debe respetar la ventana de 24 horas a 30 días

`GET /api/schedule/availability` SHALL aceptar un rango `from`/`to` en ISO-8601 con offset y SHALL rechazar con 400 todo rango que no esté completamente contenido entre 24 horas y 30 días desde el instante de consulta, sin recortar el rango ni devolver una lista vacía.

#### Scenario: Rango dentro de la ventana pública

- **GIVEN** un odontólogo autenticado y un reloj fijo
- **WHEN** solicita disponibilidad desde 48 horas hasta 5 días después del instante actual
- **THEN** la respuesta es 200 con los slots libres de ese rango
- **AND** cada slot se informa con offset `-03:00`

#### Scenario: Inicio anterior a las 24 horas

- **GIVEN** un odontólogo autenticado y un reloj fijo
- **WHEN** solicita disponibilidad con `from` anterior a 24 horas desde el instante actual
- **THEN** la respuesta es 400 con un mensaje que indica el mínimo de 24 horas
- **AND** no se devuelve una lista de slots

#### Scenario: Fin posterior a los 30 días

- **GIVEN** un odontólogo autenticado y un reloj fijo
- **WHEN** solicita disponibilidad con `to` posterior a 30 días desde el instante actual
- **THEN** la respuesta es 400 con un mensaje que indica el máximo de 30 días
- **AND** no se devuelve una lista de slots

#### Scenario: Rango vacío o invertido

- **GIVEN** un odontólogo autenticado
- **WHEN** solicita disponibilidad con `from` igual o posterior a `to`
- **THEN** la respuesta es 400
- **AND** no se ejecuta el cálculo de slots

#### Scenario: Parámetros faltantes o con formato inválido

- **GIVEN** un odontólogo autenticado
- **WHEN** solicita disponibilidad sin `from`, sin `to` o con una fecha no ISO-8601
- **THEN** la respuesta es 400 con un mensaje de parámetros inválidos

#### Scenario: Los límites se calculan con el reloj inyectado

- **GIVEN** un reloj fijo en un instante conocido
- **WHEN** se solicitan exactamente los límites de 24 horas y 30 días
- **THEN** ambos límites se aceptan
- **AND** un segundo antes del mínimo o un segundo después del máximo se rechaza con 400

#### Scenario: Metadatos de la respuesta de disponibilidad

- **GIVEN** una consulta válida de disponibilidad
- **WHEN** se inspecciona la respuesta
- **THEN** incluye `timezone`, `durationMinutes: 30`, `window` con `from`, `to`, `minNoticeHours`, `maxDays` y `timezone`
- **AND** `slots` es una lista plana de objetos con `start` y `end`
- **AND** ningún slot incluye un indicador de ocupado

### Requirement: La disponibilidad debe excluir bloqueos vigentes y turnos activos

La lista de slots libres SHALL excluir todo slot que intersecte un bloqueo no eliminado o un turno activo del mismo odontólogo, y SHALL ignorar los bloqueos con baja lógica y los turnos cancelados.

#### Scenario: Bloqueo de día completo

- **GIVEN** un odontólogo con un bloqueo de 00:00 a 23:59 de un día hábil
- **WHEN** se consulta la disponibilidad de ese día
- **THEN** no se devuelve ningún slot de ese día

#### Scenario: Bloqueo parcial alineado

- **GIVEN** un odontólogo con un bloqueo de 09:00 a 10:00
- **WHEN** se consulta la disponibilidad de ese día
- **THEN** los slots de 09:00 y 09:30 no se devuelven
- **AND** el slot de 10:00 sí se devuelve

#### Scenario: Bloqueo no alineado a la grilla

- **GIVEN** un odontólogo con un bloqueo de 10:10 a 10:20
- **WHEN** se consulta la disponibilidad de ese día
- **THEN** el slot de 10:00 a 10:30 no se devuelve
- **AND** el slot de 09:30 a 10:00 sí se devuelve
- **AND** el slot de 10:30 a 11:00 sí se devuelve

#### Scenario: Bloqueo con baja lógica

- **GIVEN** un bloqueo de día completo marcado como eliminado
- **WHEN** se consulta la disponibilidad de ese día
- **THEN** los slots del día se devuelven con normalidad

#### Scenario: Un turno pendiente o confirmado ocupa su franja

- **GIVEN** turnos PENDING y CONFIRMED en distintas franjas del mismo día
- **WHEN** se consulta la disponibilidad
- **THEN** ninguna de las dos franjas ocupadas se devuelve como libre

#### Scenario: Un turno cancelado libera su franja

- **GIVEN** un turno CANCELLED en una franja del día
- **WHEN** se consulta la disponibilidad
- **THEN** esa franja se devuelve como libre

#### Scenario: Un bloqueo de otro odontólogo no afecta la disponibilidad

- **GIVEN** un bloqueo de día completo perteneciente a otro odontólogo
- **WHEN** se consulta la disponibilidad del odontólogo autenticado
- **THEN** los slots del día se devuelven con normalidad

### Requirement: Los bloqueos manuales deben poder crearse y eliminarse con baja lógica

`POST /api/schedule/blocks` SHALL crear un bloqueo del odontólogo autenticado y `DELETE /api/schedule/blocks/{id}` SHALL aplicar baja lógica con respuesta 204 idempotente, sin revelar la existencia de bloqueos ajenos.

#### Scenario: Alta de un bloqueo válido

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `POST /api/schedule/blocks` con un rango válido y un motivo
- **THEN** la respuesta es 201 con id, inicio, fin y motivo
- **AND** el bloqueo queda asociado al odontólogo de la sesión

#### Scenario: Alta de un bloqueo con rango inválido

- **GIVEN** un odontólogo autenticado
- **WHEN** envía `POST /api/schedule/blocks` con fin anterior o igual al inicio
- **THEN** la respuesta es 422
- **AND** no se persiste ningún bloqueo

#### Scenario: Un bloqueo fuera de la ventana pública es válido

- **GIVEN** un odontólogo autenticado
- **WHEN** crea un bloqueo a 60 días de distancia
- **THEN** la respuesta es 201
- **AND** el bloqueo se conserva aunque no afecte la disponibilidad pública actual

#### Scenario: Baja lógica de un bloqueo propio

- **GIVEN** un bloqueo propio vigente
- **WHEN** el odontólogo envía `DELETE /api/schedule/blocks/{id}`
- **THEN** la respuesta es 204
- **AND** el bloqueo deja de excluir slots en la disponibilidad

#### Scenario: Baja repetida del mismo bloqueo

- **GIVEN** un bloqueo propio ya eliminado
- **WHEN** el odontólogo repite `DELETE /api/schedule/blocks/{id}`
- **THEN** la respuesta vuelve a ser 204
- **AND** no se produce ningún otro efecto

#### Scenario: Baja de un bloqueo ajeno o inexistente

- **GIVEN** un bloqueo perteneciente a otro odontólogo y un identificador inexistente
- **WHEN** el odontólogo autenticado intenta eliminarlos
- **THEN** ambas respuestas son 404
- **AND** el bloqueo ajeno permanece vigente

### Requirement: Las rutas de agenda deben exigir sesión y tomar al odontólogo del contexto autenticado

Todas las rutas bajo `/api/schedule/**` SHALL exigir una sesión válida y SHALL obtener el odontólogo de la sesión, nunca de un identificador enviado en el cuerpo o en parámetros.

#### Scenario: Acceso sin sesión

- **WHEN** un cliente sin sesión solicita `GET /api/schedule`, `PATCH /api/schedule`, `GET /api/schedule/availability` o las rutas de bloqueos
- **THEN** la respuesta es 401 con el formato JSON de error de la API
- **AND** no se ejecuta lógica de agenda

#### Scenario: El cuerpo no puede elegir al odontólogo

- **GIVEN** una sesión de un odontólogo
- **WHEN** envía una configuración o un bloqueo con un identificador de otro odontólogo en el cuerpo
- **THEN** la operación se aplica al odontólogo de la sesión
- **AND** el identificador del cuerpo se ignora o se rechaza sin afectar a otro odontólogo

### Requirement: La disponibilidad interna debe ser reutilizable sin la política pública

El cálculo interno de slots libres SHALL aceptar cualquier rango y SHALL no aplicar la ventana de 24 horas a 30 días; la política pública SHALL vivir en un componente separado reutilizable por la reserva pública.

#### Scenario: Cálculo interno fuera de la ventana pública

- **GIVEN** un odontólogo con disponibilidad en las próximas 2 horas
- **WHEN** un servicio interno consulta sus slots libres en ese rango
- **THEN** el cálculo devuelve los slots correspondientes
- **AND** no aplica el mínimo de 24 horas

#### Scenario: La reserva pública reutiliza la política sin duplicar constantes

- **GIVEN** la configuración `app.schedule.public-window`
- **WHEN** otro módulo necesita validar un rango público
- **THEN** obtiene los límites desde el mismo componente de ventana pública
- **AND** no define sus propios valores de 24 horas o 30 días

### Requirement: La disponibilidad debe ser informativa y no reservar slots

La consulta de disponibilidad SHALL ser de solo lectura sobre turnos y bloqueos, SHALL no crear reservas y SHALL no reemplazar la garantía de unicidad de franja que aplica al crear un turno.

#### Scenario: Consultas repetidas no alteran la agenda

- **GIVEN** una agenda sin cambios
- **WHEN** se consulta la disponibilidad dos veces con el mismo rango
- **THEN** ambas respuestas contienen los mismos slots
- **AND** no se crean turnos ni bloqueos

#### Scenario: Un slot libre puede dejar de estarlo antes de reservar

- **GIVEN** un slot informado como libre
- **WHEN** otro proceso ocupa esa franja antes de la reserva
- **THEN** la próxima consulta de disponibilidad ya no lo informa como libre
- **AND** la garantía definitiva contra la doble reserva queda en la restricción única de franja aplicada al crear el turno

### Requirement: El cálculo debe usar una timezone fija inyectable

Los slots y los límites SHALL calcularse con `America/Argentina/Buenos_Aires` y un reloj inyectable, y SHALL no depender de la zona horaria del sistema operativo ni del reloj global del proceso.

#### Scenario: Verano e invierno conservan el offset argentino

- **GIVEN** fechas de enero y de julio
- **WHEN** se calculan slots en ambas fechas
- **THEN** todas las horas se informan con offset `-03:00`
- **AND** no aparecen huecos ni duplicados por horario de verano

#### Scenario: Pruebas deterministas con reloj fijo

- **GIVEN** un reloj fijo en un instante conocido
- **WHEN** se ejecutan dos veces el cálculo de ventana y disponibilidad
- **THEN** ambos resultados son idénticos
- **AND** no se consulta el reloj del sistema operativo
