# Proposal

## Why

El odontólogo no dispone hoy de una interfaz administrativa: `frontend/admin.html` es un shell de cinco líneas que carga el mismo bundle que la reserva pública y `frontend/src/main.ts` solo monta esa reserva. Las dependencias C-03/C-05/C-06/C-10/C-11 ya están completas y los contratos HTTP que necesita la consola están documentados y verificados en `docs/explore/C-12-admin-agenda-ui.md`; C-12 debe convertirlos en un flujo usable sin modificar el backend.

## What Changes

- Pantalla de login con email y contraseña que consume `POST /api/auth/login`, guarda solo el perfil devuelto (`id`, `email`, `firstName`, `lastName`) y muestra un error genérico ante el 401 `{"error":"Credenciales inválidas"}`.
- Vista de agenda diaria y semanal construida desde `GET /api/appointments?from=&to=`, con el estado informado por cada turno y estados vacíos o inválidos explícitos.
- Acciones de confirmar, marcar ausente, marcar atendido y cancelar mediante `PATCH /api/appointments/{id}/status`, con el cuerpo y las respuestas exactas ya existentes, sin crear turnos ni franjas.
- Listado paginado de pacientes y detalle mínimo tomado de la misma respuesta de `GET /api/patients` (no existe `GET /api/patients/{id}`), sin alta de pacientes.
- Bandeja de salida simulada desde `GET /api/outbox/messages`: lectura de los enlaces tokenizados dentro de `body`, consulta informativa con `GET` y acciones con `POST` en `/api/public/appointments/{token}/confirm` y `/cancel`.
- Lista de espera visible desde `GET /api/waiting-list`, en orden FIFO y sin exponer DNI.
- Manejo centralizado de respuestas: 401 `{"error":"No autenticado"}` en cualquier llamada autenticada sin redirección, y lectura explícita de los dos formatos de error del backend (`{"error":"<mensaje>"}` y `ProblemDetail`).
- Pruebas automáticas de lógica pura con `node:test` y verificación manual contra el backend en ejecución, con datos ficticios y sin secretos.

## Capabilities

### New Capabilities

- `admin-agenda-ui`: consola web del odontólogo para autenticarse, consultar y operar la agenda, consultar pacientes, bandeja simulada y lista de espera, y presentar los errores reales del backend de forma recuperable.

### Modified Capabilities

- Ninguna. `autenticacion-odontologo`, `gestion-turnos`, `gestion-pacientes`, `outbox-reminders`, `lista-espera` y `public-booking-ui` conservan sus requisitos: no se agregan rutas ni se cambian contratos o reglas de negocio.

## Impact

- Frontend: `frontend/admin.html`, nuevos módulos bajo `frontend/src/admin/`, configuración de compilación en `frontend/package.json` y `frontend/tsconfig*.json`; artefactos generados en `frontend/dist/`.
- API: solo consumo de los 14 endpoints documentados en `docs/explore/C-12-admin-agenda-ui.md` §4; sin cambios en `backend/`, sin dependencias de ejecución nuevas y sin migraciones.
- Restricción operativa: `/main.js` no está permitido sin sesión (`AuthSecurityConfiguration.java:60-66`), mientras que `/js/**` sí; el bundle administrativo debe servirse bajo `/js/` para que la pantalla de login sea alcanzable sin sesión. El shell público y el backend quedan intactos.
- Datos: exclusivamente odontólogo, pacientes y turnos ficticios; credenciales de demo documentadas, sin secretos en el repositorio.
