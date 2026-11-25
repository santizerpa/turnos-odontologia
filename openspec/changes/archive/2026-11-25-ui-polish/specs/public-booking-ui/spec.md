# Spec Delta

## MODIFIED Requirements

### Requirement: La UI pública permite consultar disponibilidad y reservar dentro de la ventana permitida
La interfaz pública SHALL permitir seleccionar una fecha dentro de la ventana permitida por la API (desde 24 horas hasta 30 días), consultar los slots libres del endpoint público existente y enviar una reserva con los datos mínimos del paciente. Tras una reserva exitosa (201 con `startAt`, `durationMinutes: 30` y `status: PENDING`), SHALL mostrar una confirmación visible con el inicio en fecha y hora locales de Buenos Aires, sin exponer el `Instant` UTC ni datos internos.

#### Scenario: Reserva exitosa desde la interfaz pública
- **DADO (GIVEN)** la API pública responde 200 en `GET /api/public/slots?date=...` con al menos una franja libre para una fecha dentro de la ventana permitida
- **CUANDO (WHEN)** el paciente selecciona esa fecha, elige una franja libre, completa nombre, apellido, DNI y email válidos y envía la reserva
- **ENTONCES (THEN)** la UI llama a `POST /api/public/appointments`, muestra una confirmación visible con el inicio, la duración de 30 minutos y el estado `PENDING`, y no muestra errores

#### Scenario: Hora local en la confirmación
- **DADO (GIVEN)** el paciente reservó la franja de las 14:30 de Buenos Aires
- **CUANDO (WHEN)** la API responde 201 con `startAt: "2026-11-26T17:30:00Z"`
- **ENTONCES (THEN)** la confirmación muestra la fecha 26/11/26 y la hora 14:30 de Buenos Aires, y no muestra el texto `2026-11-26T17:30:00Z` ni la hora 17:30

### Requirement: La UI pública permite gestionar un turno existente mediante token sin mutar en GET
El paciente SHALL gestionar su turno solo desde los mensajes de la bandeja de entrada simulada, que usan el token contenido en cada mensaje: "Ver estado del turno" usa el `GET` informativo; "Confirmar turno" y "Cancelar turno" usan solo `POST`. La página SHALL NOT pedir al paciente que escriba un token o enlace. Ante token inválido, vencido o error, SHALL mostrar el error visible junto al mensaje sin cambiar el turno desde `GET`.

#### Scenario: Borde de token vencido en la consulta informativa
- **DADO (GIVEN)** un mensaje de la bandeja de entrada cuyo enlace está vencido
- **CUANDO (WHEN)** el paciente pulsa "Ver estado del turno" y la API responde 410 por token vencido
- **ENTONCES (THEN)** la UI muestra un mensaje visible de enlace vencido junto a ese mensaje, no ejecuta ningún `POST` automático y no presenta el turno como cancelado ni confirmado

#### Scenario: Sin acceso manual por token
- **DADO (GIVEN)** la página pública de reservas
- **CUANDO (WHEN)** se renderiza
- **ENTONCES (THEN)** no existe ningún campo para escribir un token o enlace ni una sección "Ya tengo un enlace"; solo las secciones de reserva y de bandeja de entrada simulada

#### Scenario: Confirmar o cancelar cambia el estado
- **DADO (GIVEN)** un mensaje con enlace vigente de un turno `PENDING`
- **CUANDO (WHEN)** el paciente pulsa "Confirmar turno" o "Cancelar turno" y la API responde 200
- **ENTONCES (THEN)** la UI muestra el `message` y el nuevo `status` (`CONFIRMED` o `CANCELLED`) junto al mensaje

## ADDED Requirements

### Requirement: La UI pública ofrece una bandeja de entrada simulada del correo del paciente
La página pública SHALL incluir una sección rotulada como simulación del correo del paciente que, para un email ingresado, lista los mensajes de `GET /api/public/inbox?email=`. Cada mensaje SHALL mostrar asunto, email, DNI y fecha de envío local, el inicio local del turno y, una sola vez, los botones "Ver estado del turno", "Confirmar turno" y "Cancelar turno", que usan el token del mensaje.

#### Scenario: Mensajes del email ingresado
- **DADO (GIVEN)** dos mensajes ficticios dirigidos a `paciente@demo.invalid` y la bandeja simulada habilitada
- **CUANDO (WHEN)** el usuario ingresa ese email y consulta la bandeja
- **ENTONCES (THEN)** la UI muestra ambos mensajes, cada uno con asunto, email, DNI, fecha de envío local e inicio local del turno, y exactamente un botón de cada acción por mensaje

#### Scenario: Confirmar desde la bandeja de entrada
- **DADO (GIVEN)** un mensaje con enlace vigente para un turno `PENDING`
- **CUANDO (WHEN)** el usuario pulsa "Confirmar turno"
- **ENTONCES (THEN)** la UI envía un único `POST /api/public/appointments/{token}/confirm` y muestra junto al mensaje el `message` y el `status` devueltos

#### Scenario: Ver estado sin mutar
- **DADO (GIVEN)** un mensaje con enlace
- **CUANDO (WHEN)** el usuario pulsa "Ver estado del turno"
- **ENTONCES (THEN)** la UI usa solo el `GET` informativo, muestra inicio local y estado, y no envía ningún `POST`

#### Scenario: Bandeja vacía o email sin mensajes
- **DADO (GIVEN)** un email válido sin mensajes
- **CUANDO (WHEN)** la API responde 200 con un array vacío
- **ENTONCES (THEN)** la UI muestra "No hay mensajes para este email." y no ofrece acciones

#### Scenario: Bandeja simulada deshabilitada o email inválido
- **DADO (GIVEN)** la bandeja simulada deshabilitada o un email mal formado
- **CUANDO (WHEN)** la API responde 404 con `{"error":"Bandeja simulada deshabilitada"}` o 422 con `{"error":"Email inválido"}`
- **ENTONCES (THEN)** la UI muestra el mensaje exacto del campo `error`, sin pistas sobre enlaces o tokens, y no ofrece acciones

#### Scenario: Servidor sin la bandeja simulada
- **DADO (GIVEN)** un backend que no expone la ruta de bandeja y responde 404 genérico (por ejemplo `{"error":"Not Found","status":404}`)
- **CUANDO (WHEN)** el usuario consulta la bandeja
- **ENTONCES (THEN)** la UI muestra "La bandeja simulada no está disponible en este servidor." y no menciona enlaces ni tokens
