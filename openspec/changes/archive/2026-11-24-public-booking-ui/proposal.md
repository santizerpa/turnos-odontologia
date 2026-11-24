# Propuesta

## Por qué

El backend ya expone la reserva pública (C-07) y la confirmación/cancelación por token (C-09), pero no existe una interfaz pública para que un paciente consulte disponibilidad, reserve un turno y gestione el enlace que recibe. C-08 cubre esa brecha con una UI pública mínima, sin framework, consumiendo únicamente los endpoints existentes.

## Qué cambia

- Agrega una interfaz pública en TypeScript vanilla (sin framework) para consultar slots, reservar y ver la confirmación del turno creado.
- Agrega un selector de fecha limitado a la ventana permitida por la API (24 h a 30 días).
- Agrega formulario de datos del paciente y listado de slots libres.
- Muestra errores de negocio visibles para respuestas 409 y 422, con mensajes comprensibles y sin exponer detalles internos.
- Agrega el flujo "ya tengo un turno": abre la consulta informativa GET por token y ejecuta la acción mediante POST, sin mutar el turno desde GET.
- No modifica backend, contratos HTTP, handlers, specs de backend ni configuración de runtime.

## Capacidades

### Nuevas capacidades

- `public-booking-ui`: interfaz pública de reserva y gestión por token que consume los endpoints públicos existentes, presenta estados de éxito y errores visibles, y respeta la ventana y el comportamiento de los contratos actuales.

### Capacidades modificadas

- Ninguna. No hay cambios de comportamiento a nivel de spec del backend; la UI consume contratos ya implementados.

## Impacto

- Código afectado (al implementar): `frontend/` (`index.html`, `src/main.ts` y módulos TS vanilla necesarios), manteniendo la salida compilada en `frontend/dist/` servida por la configuración estática ya existente en perfiles `local`/`test`.
- APIs: usa exclusivamente `GET /api/public/slots`, `POST /api/public/appointments`, `GET/POST /api/public/appointments/{token}/confirm` y `GET/POST /api/public/appointments/{token}/cancel`.
- Dependencias: sin nuevas dependencias de runtime; `frontend/package.json` hoy solo tiene TypeScript. La viabilidad de tests automatizados de lógica frontend se evalúa en `design.md`.
- Fuera de alcance: cambios de backend/controllers/handlers/specs backend/runtime, frameworks, rate limit, lista de espera, autenticación administrativa, mails reales, C-10 a C-14 y modificaciones a `CHANGES.md`.
