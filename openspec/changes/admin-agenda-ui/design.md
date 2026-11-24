# Design

## Context

Ver `proposal.md` para la motivación y `specs/admin-agenda-ui/spec.md` para el contrato observable. El estado actual es un frontend vanilla con `frontend/src/main.ts` dedicado a la reserva pública y un `frontend/admin.html` sin funcionalidad; el backend ya expone los 14 endpoints documentados en `docs/explore/C-12-admin-agenda-ui.md` §4 y no se modificará. La cookie `JSESSIONID` es `HttpOnly`/`SameSite=Lax`, CSRF está deshabilitado y la revisión de esa decisión pertenece a C-14. El script raíz `/main.js` no está permitido sin sesión, mientras que `/js/**` sí lo está (`AuthSecurityConfiguration.java:60-66`).

## Goals / Non-Goals

**Goals:**

- Entregar una consola administrativa navegable desde `/admin.html` sin sesión previa para poder mostrar el login.
- Separar lógica pura (rangos, normalización de errores, enlaces, paginación, estado de sesión) del DOM para poder probarla con `node:test` sin dependencias nuevas.
- Consumir exactamente los contratos HTTP existentes, con mensajes y códigos reales, sin reescribir reglas de negocio en el cliente.

**Non-Goals:**

- No crear ni modificar endpoints, DTO, reglas de negocio ni configuración de seguridad del backend.
- No alta de pacientes, no carga manual de turnos ni creación manual de franjas.
- No diseño visual ni mockups: la política visual del proyecto es `off`.
- No credenciales reales, no datos reales de pacientes y no secretos versionados.

## Decisions

1. **Punto de entrada administrativo bajo `/js/`.** `frontend/admin.html` cargará `/js/admin/main.js`, emitido por una configuración de TypeScript adicional (`frontend/tsconfig.admin.json`, `rootDir: src/admin`, `outDir: dist/js/admin`). El bundle público y `frontend/index.html` quedan intactos. Alternativas descartadas: mantener `/main.js` (responde 401 sin sesión) o ampliar la lista de recursos permitidos del backend (fuera de alcance y prohibido para C-12).

2. **Módulos por responsabilidad, con lógica pura primero.** Bajo `frontend/src/admin/`: `api.ts` (único cliente HTTP con `credentials: 'same-origin'`), `errors.ts` (normalización de los dos formatos), `session.ts` (estado de acceso y vencimiento), `agenda.ts` (rangos diario/semanal y vista modelo), `appointments.ts` (cuerpos y resultados de `PATCH`), `patients.ts` (paginación y detalle mínimo), `outbox.ts` (enlaces del `body` y acciones por token), `waiting-list.ts` (FIFO) y `ui.ts` (render vanilla). Cada módulo puro lleva un `*.test.ts` con `node:test`; se reutiliza el patrón ya presente en `frontend/src/public-reservation/`. Alternativa descartada: añadir un framework de test o DOM simulado, que introduciría dependencias nuevas.

3. **Sesión gobernada por el servidor, nunca por el cliente.** La UI solo conoce el perfil devuelto por `/api/auth/login` y delega la cookie al navegador. Toda llamada autenticada pasa por el cliente HTTP común; si responde 401 con `{"error":"No autenticado"}`, se cambia a estado de login dentro del mismo documento, se descarta la acción en curso y no se redirige ni se reintenta. Alternativa descartada: guardar un token o la sesión en `localStorage`, que no existe en el backend y expondría datos.

4. **Normalizador único de errores.** `errors.ts` distingue `{"error":"<mensaje>"}` de `ProblemDetail` (`detail`) y usa el fallback `No se pudo completar la solicitud. Intente nuevamente más tarde.` cuando no hay cuerpo legible. `instance` es opcional y su ausencia no cambia el resultado; nunca se muestra `title`, `type` ni trazas internas. Se respetan los mensajes exactos de exploración §4.4. Alternativa descartada: leer solo `error`, que ocultaría el 400 de pacientes y el 500 de la bandeja.

5. **Render seguro y sin inventar datos.** Se usa `textContent` para todos los valores del servidor (`subject`, `body`, emails, nombres); los enlaces se extraen del texto de `body` y solo se renderizan como anclas cuando empiezan con `/api/public/appointments/`. El "detalle mínimo" del paciente son los cinco campos ya presentes en `content` de `GET /api/patients`: no existe `GET /api/patients/{id}` y no se agregará. Alternativa descartada: `innerHTML` con datos del mail, que permitiría inyección de HTML.

6. **Enlaces tokenizados sin caché de larga duración.** Los `GET` públicos son informativos (200 con `startAt` y `status`) y las acciones se ejecutan solo con `POST`. Ante 404/410 o 409 se muestra el mensaje exacto sin reintentos automáticamente; si un recordatorio reemite el token, el enlace anterior deja de resolverse, por lo que la consola debe volver a leer la bandeja antes de una nueva tentativa manual.

7. **Sin base visual ni mockups.** La política visual del proyecto es `off`, confirmada por el usuario para este change. No se crean archivos de mockup.

## Risks / Trade-offs

- [El script raíz `/main.js` responde 401 sin sesión] → publicar el entry administrativo en `/js/admin/main.js` y verificar `GET /js/admin/main.js` anónimo con 200.
- [Doble fuente de HTML estático (`classpath:/static/` y `file:../frontend/dist/`)] → el build copia `admin.html` a `dist/` y la verificación manual comprueba la ruta servida con el perfil `test`.
- [Los turnos `CANCELLED` no aparecen en `GET /api/appointments` porque `activeSlot` es nulo] → la UI muestra el estado "sin turnos" del rango y no afirma que la agenda contiene el historial completo.
- [Formatos de error mixtos] → normalizador único con casos de prueba para ambos formatos y mensajes exactos.
- [Cookie de sesión con CSRF deshabilitado] → no almacenar credenciales ni exponer tokens en la UI; `SameSite=Lax` vigente y revisión diferida a C-14.
- [Reemisión de tokens al generar recordatorios] → recargar la bandeja antes de reintentar; nunca repetir un `POST` automáticamente.
- [Listas de bandeja y espera sin paginación] → renderizar el array completo devuelto sin asumir límites ni solicitar parámetros inexistentes.
- [Un 401 no distingue sesión vencida de cookie ausente] → aplicar el mismo estado de "sesión no disponible" en ambos casos, sin redirección.
- [El 500 de la bandeja no es reproducible de forma determinista sin forzar una falla local] → verificar el contrato HTTP con el cuerpo exacto documentado y comprobar su render en navegador con una respuesta controlada, dejando el resto de los casos contra el backend con el perfil `test` e H2 en memoria.
- [Política visual `off`] → verificación visual manual sin mockups aprobados ni capturas de referencia.

## Migration Plan

No hay migración de datos ni cambios de backend. La implementación se publica regenerando `frontend/dist/` con el build de frontend y comprobando que `/admin.html` y `/js/admin/main.js` se sirven sin sesión. Rollback: restaurar `frontend/admin.html`, `frontend/package.json` y la configuración de TypeScript a su estado previo; el backend y la base no se tocan.

## UI / Mockups

`Decision: no (project policy)` — la política visual del proyecto es `off`, confirmada por el usuario para este change el 2026-11-24. No se crean mockups ni se solicita aprobación visual; la verificación de pantallas es manual y se documenta en `tasks.md`.
