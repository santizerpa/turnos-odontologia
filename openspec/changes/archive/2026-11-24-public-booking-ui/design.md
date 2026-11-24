# Diseño

## Contexto

La reserva pública y la gestión por token ya están implementadas en backend (C-07, C-09). Los contratos HTTP existentes devuelven JSON y cubren disponibilidad, reserva y acciones por token. El frontend actual es mínimo (`frontend/index.html`, `frontend/src/main.ts`, build con `tsc` y copia de HTML a `dist/`), se sirve como recurso estático desde Spring Boot en perfiles `local`/`test` con `classpath:/static/,file:../frontend/dist/`, y no tiene framework ni runner de tests. Ver `proposal.md` para motivación y `specs/public-booking-ui/spec.md` para el comportamiento observable.

Restricciones: no modificar backend, handlers, specs de backend ni configuración de runtime; usar solo endpoints existentes; TypeScript vanilla sin framework; datos ficticios en ejemplos, pruebas y checklist manual; política visual del proyecto en `off`.

### Contrato HTTP verificado

Fuentes: `backend/src/main/java/com/turnosodontologia/appointment/PublicBookingExceptionHandler.java`, `PublicAppointmentTokenExceptionHandler.java` y `PublicAppointmentTokenController.java`.

**Todo cuerpo de error es JSON con un único campo `error`** (`Map.of("error", "<mensaje>")`), incluido el 409 directo de `PublicAppointmentTokenController.java:52`. No hay `ProblemDetail`, ni campos `field` ni arreglos `details`.

| Código | Mensaje exacto del campo `error` | Origen |
|---|---|---|
| 422 | `La fecha solicitada está fuera de la ventana pública` | `PublicBookingExceptionHandler.java:23-27` |
| 503 | `Servicio temporalmente no disponible` | `PublicBookingExceptionHandler.java:29-33` |
| 422 | `La franja solicitada no está disponible` | `PublicBookingExceptionHandler.java:35-39` |
| 422 | `Datos de reserva inválidos` | `PublicBookingExceptionHandler.java:41-45` y `:59-64` |
| 409 | `La franja solicitada ya está ocupada` | `PublicBookingExceptionHandler.java:47-51` |
| 500 | `No se pudo procesar la reserva` | `PublicBookingExceptionHandler.java:53-57` |
| 404 | `Token inexistente o inválido` | `PublicAppointmentTokenExceptionHandler.java:15-18` |
| 410 | `Token vencido` | `PublicAppointmentTokenExceptionHandler.java:20-23` |
| 409 | `Debe contactarse al consultorio para cancelar el turno` | `PublicAppointmentTokenExceptionHandler.java:25-29` |
| 409 | `No se pudo cancelar el turno` | `PublicAppointmentTokenExceptionHandler.java:31-35` |
| 409 | `No se pudo confirmar el turno` | `PublicAppointmentTokenExceptionHandler.java:37-41` |
| 409 | `El turno no puede confirmarse` | `PublicAppointmentTokenController.java:49-53` |

**Cuerpos de éxito relevantes para la UI:**
- `POST /api/public/appointments` → 201 `{"startAt","durationMinutes":30,"status":"PENDING"}`.
- `GET /api/public/appointments/{token}/confirm` y `/cancel` → 200 `{"startAt","status"}`.
- `POST /api/public/appointments/{token}/confirm` y `/cancel` → 200 `{"message","status"}`, con mensajes idempotentes `ya confirmado` / `ya cancelado` además de los de éxito de la acción.

Los `GET` de confirmación y cancelación son `@GetMapping` que devuelven `ResponseEntity<Map<String,Object>>` desde `informative()` (`PublicAppointmentTokenController.java:23-39`): **devuelven JSON, no HTML**. La UI debe renderizar su propio HTML en el cliente; el backend no sirve una página de confirmación/cancelación.

## Objetivos / No objetivos

**Objetivos:**
- Implementar una UI pública que cubra consulta de slots, reserva, confirmación visible y gestión "ya tengo un turno".
- Mapear el campo `error` de 409/422 a mensajes visibles y recuperables, y usar un fallback genérico para otros errores.
- Mantener la verificación alineada con el estado actual del frontend: automatizar lógica pura con el runner nativo de Node; documentar verificación manual como respaldo obligatorio.

**No objetivos:**
- No cambiar contratos HTTP, controladores, excepciones, seguridad, perfiles ni estáticos del backend.
- No agregar frameworks, bundlers, librerías de UI, rate limit, lista de espera, autenticación admin, mails reales ni flujos de otros changes.
- No crear mockups ni base visual nueva; la política visual está en `off`.

## Decisiones

### 1. Estructura TypeScript vanilla modular
- **Decisión:** Separar la lógica en módulos pequeños bajo `frontend/src/public-reservation/` o equivalente simple (estado/UI, cliente API, validaciones/formateo de fecha, render de mensajes) manteniendo `main.ts` como punto de entrada.
- **Rationale:** Permite aislar lógica pura testeable sin introducir framework ni bundler. El build actual (`tsc`) ya compila `src/**/*.ts`.
- **Alternativa considerada:** Todo en `main.ts`. Se descarta por dificultar pruebas y mantenimiento.

### 2. Uso exclusivo de endpoints existentes
- **Decisión:** La UI solo llamará a:
  - `GET /api/public/slots?date=yyyy-MM-dd`
  - `POST /api/public/appointments`
  - `GET /api/public/appointments/{token}/confirm`
  - `POST /api/public/appointments/{token}/confirm`
  - `GET /api/public/appointments/{token}/cancel`
  - `POST /api/public/appointments/{token}/cancel`
- **Rationale:** Cumple el alcance de C-08 y evita dependencias de backend no existentes. Los GET informativos devuelven JSON `{"startAt","status"}`; la UI construye el HTML client-side.
- **Alternativa considerada:** Agregar endpoints de conveniencia o HTML server-side. Se descarta por modificar backend.

### 3. Manejo visible de errores y recuperación
- **Decisión:** Centralizar el mapeo de respuestas no exitosas a mensajes visibles en español, leyendo siempre el campo `error` del JSON. No buscar `ProblemDetail`, `field` ni `details`.
- **Distinción de recuperación:**
  - **409 = conflicto de negocio sobre la franja/estado → el paciente elige otra franja o contacta al consultorio.** Incluye franja ocupada, estado que impide confirmar/cancelar y ventana de cancelación cerrada.
  - **422 = datos inválidos o fuera de ventana → el paciente corrige los datos/la fecha.** El backend también usa 422 para `La franja solicitada no está disponible`; en ese caso la UI lo trata como validación de la selección y ofrece elegir otra franja, sin confundirlo con el 409 por ocupación/estado.
- **Fallback genérico:** `No se pudo completar la solicitud. Intente nuevamente más tarde.` Se usa para estados no mapeados o cuando el campo `error` no es legible. Para 503 y 500 se muestra preferentemente el mensaje exacto del backend (`Servicio temporalmente no disponible`, `No se pudo procesar la reserva`) y el fallback solo como respaldo.
- **Rationale:** El usuario necesita recuperar la acción con una instrucción clara según el código; los handlers actuales ya devuelven mensajes fijos en español.
- **Alternativa considerada:** Mostrar solo errores de consola o texto técnico HTTP. Se descarta por no cumplir el criterio visible.

### 4. Flujo "ya tengo un turno"
- **Decisión:** Presentar un acceso dedicado donde el usuario pega token/URL, la UI extrae el token si corresponde, ejecuta GET informativo para mostrar estado y solo ejecuta POST al confirmar/cancelar explícitamente. El HTML de ese flujo se renderiza en el cliente a partir del JSON recibido.
- **Rationale:** Respeta la semántica existente: GET no muta; POST ejecuta acción. Cubre 404/410/409 y aplica el mismo fallback genérico ante otros errores.
- **Alternativa considerada:** Ejecutar acción directamente al abrir enlace o esperar una página HTML del backend. Se descarta porque GET es informativo y el backend no sirve HTML.

### 5. Verificación
- **Decisión:** Verificación en dos capas, ambas obligatorias en la planificación.
  - **Automatizada (viable y comprometida):** Node está disponible en el entorno (`node v24.21.0`, `npm 11.19.0`) e incluye el runner nativo `node:test` y `node:assert/strict`; no se agregan dependencias ni se deja deuda técnica. La lógica pura se extrae a módulos TypeScript sin DOM (validación de fecha en ventana, extracción de token, mapeo de errores HTTP 409/422/404/410/503/500 y fallback genérico a mensajes visibles, formateo/construcción de payloads). Los tests fuente viven bajo `frontend/src/` (por ejemplo `frontend/src/**/*.test.ts`). La verificación compila con el `tsc` existente mediante `npm run build` y ejecuta `node --test` sobre los módulos compilados en `frontend/dist/` (por ejemplo `node --test "frontend/dist/**/*.test.js"`).
  - **Manual documentada:** checklist con arranque local y **solo datos ficticios** (nombre, apellido, DNI y email de prueba; nunca datos reales de pacientes), casos felices, 409, 422, 503/otro error con fallback, token vencido/inválido y verificación de que GET no muta y POST solo ocurre por acción explícita.
- **Rationale:** Hay toolchain suficiente sin dependencias nuevas; la automatización cubre lógica determinista y la verificación manual cubre integración UI/API y comportamiento visible.
- **Alternativa considerada:** No automatizar lógica pura. Se descarta porque el entorno ya provee `node:test` y el roadmap pide evaluar viabilidad.

## Política visual

- `Decision: no (project policy)`
- La política visual project-wide está marcada en `design-system/README.md` y `.active-orchestrator-state.json` (`off`). Esa mención es solo el marcador de política: no se crea ni modifica `design-system/`, no se construye una base visual y no se generan mockups.

## Riesgos / Trade-offs

- [Cobertura parcial de UI por tests de lógica pura] → Automatizar lógica pura con `node:test` y mantener checklist manual detallado para DOM, fetch e integración visual.
- [Doble fuente de HTML estático (`classpath:/static/` y `file:../frontend/dist/`)] → Durante implementación verificar qué HTML sirve el perfil local; no cambiar configuración en C-08, documentar el comportamiento observado.
- [Contrato JSON sin campo detallado por input en 422] → Mostrar el mensaje general del campo `error`, conservar valores del formulario para corrección y aplicar el fallback genérico solo si el cuerpo no es legible.
- [Respuestas de error fuera del mapa conocido (503/500/estados inesperados)] → Mostrar el `error` exacto cuando exista y, si falta, el fallback `No se pudo completar la solicitud. Intente nuevamente más tarde.`
- [Token plano no devuelto por POST de reserva] → La confirmación visible usa la respuesta de reserva; el flujo "ya tengo un turno" depende del token que el usuario obtiene por fuera de la UI (bandeja simulada/outbox), según comportamiento actual.

## Plan de migración

No aplica migración de datos ni despliegue backend. Implementación limitada a frontend estático compilado. Rollback: revertir cambios de `frontend/`.

## Preguntas abiertas

- Ninguna que bloquee la planificación. La ubicación final de la UI de token (misma página o sección) se resolverá en implementación dentro del alcance definido, sin cambiar contratos.
