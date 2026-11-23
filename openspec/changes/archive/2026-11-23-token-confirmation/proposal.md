# Proposal

## Why

Confirmar y cancelar por enlace forma parte del MVP imprescindible del Discovery, porque permite resolver las acciones públicas centrales sin obligar al paciente a acceder a una cuenta. La elección es acotada pero verificable: las reglas de vigencia, el límite de 24 horas y la idempotencia pueden probarse con escenarios deterministas. Es una sola funcionalidad de C-09 integrada con la reserva de C-07 y el dominio central de C-05.

## What Changes

- Incorporar `AppointmentTokenService.issue(Appointment)` como propietario de la emisión: genera 32 bytes aleatorios en base64url, calcula SHA-256, persiste solo el hash y devuelve el token plano una única vez.
- Sumar consulta por hash en `AppointmentTokenRepository`, búsqueda segura por el token recibido y cálculo de vencimiento como `min(startAt, now + 7 días)`.
- Incorporar los cuatro endpoints públicos GET/POST de confirmación y cancelación, con GET informativo y sin mutaciones.
- Delegar toda cancelación a `AppointmentService.cancel(appointmentId, Actor.PATIENT, reason)` y conservar su idempotencia y publicación de eventos.
- Definir respuestas JSON informativas para páginas GET y acciones POST.

Fuera de alcance: envío de correo, scheduler de recordatorios (C-10), UI de administrador (C-12), autenticación de odontólogos y cambios directos de estado fuera de C-05.

## Capabilities

### New Capabilities

- `token-confirmation`: validación de tokens públicos, confirmación/cancelación reusable e idempotente, reglas de vigencia y contrato entre C-07 y C-09.

### Modified Capabilities

- Ninguna especificación principal existente requiere modificación: C-05 conserva la cancelación centralizada y C-07 conserva la creación de la reserva llamando al emisor de C-09.

## Impact

- Backend público Spring: controladores de confirmación/cancelación, utilidad de generación/hash, servicio de token, consulta por hash y manejo acotado de errores 4xx.
- Persistencia: `AppointmentToken` existente; C-09 persiste solo el hash y no almacena el token plano.
- Integración: C-07 crea la reserva y llama a `AppointmentTokenService.issue(Appointment)`, entrega una vez el token plano retornado y C-09 lo busca posteriormente por su hash; C-09 consume C-05 para cancelar.
- Pruebas JUnit con reloj fijo y datos ficticios para validación de TTL, estados, doble acción y regla de 24 horas.
