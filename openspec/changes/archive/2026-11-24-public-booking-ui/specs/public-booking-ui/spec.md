# Delta de especificación: public-booking-ui

## ADDED Requirements

### Requirement: La UI pública permite consultar disponibilidad y reservar dentro de la ventana permitida
La interfaz pública SHALL permitir seleccionar una fecha dentro de la ventana permitida por la API (desde 24 horas hasta 30 días), consultar los slots libres del endpoint público existente y enviar una reserva con los datos mínimos del paciente. Tras una reserva exitosa (201 con `startAt`, `durationMinutes: 30` y `status: PENDING`), SHALL mostrar una confirmación visible sin exponer datos internos ni requerir cambios de backend.

#### Scenario: Reserva exitosa desde la interfaz pública
- **DADO (GIVEN)** la API pública responde 200 en `GET /api/public/slots?date=...` con al menos una franja libre para una fecha dentro de la ventana permitida
- **CUANDO (WHEN)** el paciente selecciona esa fecha, elige una franja libre, completa nombre, apellido, DNI y email válidos y envía la reserva
- **ENTONCES (THEN)** la UI llama a `POST /api/public/appointments`, muestra una confirmación visible con el inicio, la duración de 30 minutos y el estado `PENDING`, y no muestra errores

### Requirement: La UI pública muestra errores 409, 422 y otros fallos de forma visible y recuperable
Ante errores de la API, la UI SHALL leer el campo `error` del JSON (único campo; sin `ProblemDetail`, `field` ni `details`) y mostrar un mensaje visible en español. El 409 (conflicto de negocio sobre franja/estado) SHALL permitir elegir otra franja o contactar al consultorio. El 422 (datos inválidos o fecha fuera de ventana) SHALL conservar los valores para corregirlos. Otros errores SHALL usar el `error` o el fallback `No se pudo completar la solicitud. Intente nuevamente más tarde.`

#### Scenario: Conflicto de negocio al reservar una franja ocupada
- **DADO (GIVEN)** el paciente completó el formulario con datos válidos y seleccionó una franja que la API rechaza por conflicto
- **CUANDO (WHEN)** `POST /api/public/appointments` responde 409 con un error de negocio
- **ENTONCES (THEN)** la UI muestra un mensaje visible que indica que la franja ya no está disponible, mantiene visible el flujo de reserva para elegir otra franja o reintentar, y no presenta el fallo como éxito

#### Scenario: Datos inválidos o fecha fuera de ventana al reservar
- **DADO (GIVEN)** el paciente completó el formulario con datos inválidos o seleccionó una fecha fuera de la ventana permitida
- **CUANDO (WHEN)** `POST /api/public/appointments` responde 422 con `{"error": "Datos de reserva inválidos"}` o `{"error": "La fecha solicitada está fuera de la ventana pública"}`
- **ENTONCES (THEN)** la UI muestra el mensaje visible del campo `error`, conserva los valores ingresados del formulario y permite al paciente corregir los datos o la fecha, sin presentar el fallo como éxito

### Requirement: La UI pública permite gestionar un turno existente mediante token sin mutar en GET
La interfaz SHALL ofrecer un acceso "ya tengo un turno" que solicite el token/enlace y consulte el estado mediante `GET /api/public/appointments/{token}/confirm` o `/cancel`. Esos GET devuelven JSON con `startAt` y `status`; la UI renderiza su propio HTML y el backend no sirve una página. La acción SHALL ejecutarse solo mediante `POST`. Ante borde inválido/vencido o error de la acción, SHALL mostrar estado o error visible y aplicar el mismo fallback genérico, sin cambiar el turno desde GET.

#### Scenario: Borde de token vencido en la consulta informativa
- **DADO (GIVEN)** el paciente ingresa un token en el acceso "ya tengo un turno"
- **CUANDO (WHEN)** la UI consulta `GET /api/public/appointments/{token}/cancel` y la API responde 410 por token vencido
- **ENTONCES (THEN)** la UI muestra un mensaje visible de enlace vencido, no ejecuta ningún `POST` automático y no presenta el turno como cancelado ni confirmado
