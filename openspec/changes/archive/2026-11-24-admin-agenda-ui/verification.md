# Verificación — `admin-agenda-ui` (C-12)

Fecha de ejecución: 2026-11-24/25. Entorno: backend real Spring Boot con perfil `test`, H2 en memoria y frontend compilado servido por el propio backend desde `../frontend/dist/`. **No se usó PostgreSQL ni dev-server.** **No se probó con PostgreSQL.**

## Comandos ejecutados y resultados exactos

| Comando | Directorio | Exit code | Resultado |
|---|---|---:|---|
| `npm run build` | `frontend/` | 0 | `tsc -p tsconfig.json && tsc -p tsconfig.admin.json && copia de HTML/package.json` sin errores |
| `npm test` | `frontend/` | 0 | **94 tests, 94 pass, 0 fail, 0 skipped, 0 todo** (`node --test "dist/**/*.test.js"`) |
| `gradlew.bat test` | `backend/` | 0 | Primera ejecución `UP-TO-DATE` |
| `gradlew.bat test --rerun-tasks` | `backend/` | 0 | `BUILD SUCCESSFUL`; **48 archivos `TEST-*.xml`, 181 tests, 0 failures, 0 errors, 0 skipped** |

No se agregaron dependencias, no se modificó `backend/build.gradle` y no se saltaron tests. Para arrancar `bootRun` con H2 (`testRuntimeOnly`) se usó un init script temporal **fuera del repo**: `C:\Users\valen\AppData\Local\Temp\opencode\admin-agenda-add-test-classpath.gradle`, que agrega `sourceSets.test.runtimeClasspath` al classpath de `bootRun`.

Arranque real (proceso temporal fuera del repo, detenido al final):

```text
APP_ADMIN_SEED=true ; APP_ADMIN_EMAIL=<APP_ADMIN_EMAIL> ; APP_ADMIN_PASSWORD=<APP_ADMIN_PASSWORD>
gradlew.bat -I <init script> bootRun --args="--spring.profiles.active=test --spring.web.resources.static-locations=file:../frontend/dist/"
GET http://127.0.0.1:8080/api/health -> 200 {"status":"UP"}
GET /admin.html (sin sesión) -> 200
GET /js/admin/main.js (sin sesión) -> 200
```

Las credenciales ficticias se configuraron solo en el proceso temporal; en este documento se identifican por los nombres de variables `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`.

## Tabla de estado 5.0–5.5

| Tarea | Estado | Evidencia concreta | Limitaciones |
|---|---|---|---|
| 5.0 | **PASS** | Datos creados solo con API real: `POST /api/patients` → 201; `GET /api/public/slots?date=2026-11-26` → 200; `POST /api/public/appointments` → 201; `POST /api/public/waiting-list` → 201; `POST /api/outbox/reminders/run` → 200 `{"failed":0,"created":1}` | Sin PostgreSQL: solo H2 en modo PostgreSQL |
| 5.1 | **PASS** | Estáticos 200 sin sesión; login 200 con perfil sin hash; agenda día/semana con el turno; `PATCH .../status` → 200; pacientes, bandeja y lista de espera con campos reales; enlace de bandeja informó con `GET` y ejecutó con `POST` | Lista de espera vacía en navegador no verificada (cobertura unitaria) |
| 5.2 | **PASS** | `POST /api/auth/logout` → 204; llamada autenticada posterior → 401 `{"error":"No autenticado"}` con `Content-Type: application/json;charset=UTF-8` y sin redirect; en navegador volvió al login en el mismo documento, sin reintento ni cambio local | — |
| 5.3 | **PASS** (redacción revisada) | Ambos formatos verificados contra backend real: 400 `ProblemDetail` de `GET /api/patients?size=0` (con `instance`), 401, 400/422 de agenda, 422 de `PATCH` y 404 de token con `{"error":...}`; la UI mostró solo `detail` o `error` | 500 de bandeja con respuesta controlada, no generado por backend; alta inválida de lista de espera fuera de alcance de C-12 |
| 5.4 | **PASS** (redacción revisada) | Vacío y `from >= to` por HTTP directo y UI, parámetros ausentes por HTTP directo (400 real) y bloqueo en el cliente, búsqueda sin coincidencias, 404 y 409 reales; 410 cubierto con tests unitarios | 410 no reproducible vía API; discrepancia 422 vs 409 de franja duplicada documentada |
| 5.5 | **PASS** | El diff de implementación contiene solo `frontend/`; datos y credenciales ficticios; sin secretos; sin cambios en backend, `CHANGES.md` ni estado del orquestador | — |

## Qué se probó de verdad

### 5.0 — Generación de datos por API real

1. `GET /api/schedule` autenticado → 200. Fue necesario para inicializar la configuración de agenda: antes, `GET /api/public/slots?date=2026-11-26` respondía 422 `{"error":"Configuración de agenda inexistente"}`.
2. `POST /api/patients` con `{"dni":"12345678","nombre":"Paciente Demo","apellido":"Ficticio","email":"paciente@example.invalid"}` → **201**: `{"id":"27349650-b7ad-497f-b297-95e1c210f9bc","dni":"12345678","nombre":"Paciente Demo","apellido":"Ficticio","email":"paciente@example.invalid"}`.
3. `GET /api/public/slots?date=2026-11-26` → **200** con franjas; se eligió `start=2026-11-26T09:00:00-03:00` (dentro de 24 h–30 días).
4. `POST /api/public/appointments` con `{"nombre":"Paciente Demo","apellido":"Ficticio","dni":"12345678","email":"paciente@example.invalid","startAt":"2026-11-26T09:00:00-03:00"}` → **201**: `{"startAt":"2026-11-26T12:00:00Z","durationMinutes":30,"status":"PENDING"}`. Generó el mensaje de confirmación en bandeja.
5. `POST /api/public/waiting-list` con `{"dni":"87654321","nombre":"Espera Demo","apellido":"Ficticia","email":"espera@example.invalid","requestedDateFrom":"2026-11-27","requestedDateTo":"2026-11-30"}` → **201**: `{"id":"6b14a752-a934-4117-8c02-e7e9aaee0785","status":"WAITING","requestedDateFrom":"2026-11-27","requestedDateTo":"2026-11-30"}`.
6. `POST /api/outbox/reminders/run` → **200**: `{"failed":0,"created":1}`.
7. `GET /api/appointments?from=2026-11-26T00:00:00-03:00&to=2026-11-27T00:00:00-03:00` → **200** con un turno `id=52acd162-8642-45a4-892e-0882076d8b74`, `patientId=27349650-b7ad-497f-b297-95e1c210f9bc`, `startAt=2026-11-26T12:00:00Z`, `durationMinutes=30`, `status=PENDING`, `activeSlot` igual al inicio.
8. `GET /api/outbox/messages` → **200** con 2 mensajes (`Confirma tu turno` y `Recordatorio de turno`) y sus enlaces relativos `/api/public/appointments/{token}/confirm|/cancel`.

Nota real de backend: `POST /api/outbox/reminders/run` emite un token nuevo y reemplaza el hash del token anterior. Por eso el enlace del primer mensaje quedó inválido (`404`) y el del recordatorio es el vigente. No se inventaron datos ni se insertó en BD directamente.

### 5.1 — Ruta feliz

- Tras login por UI con `$APP_ADMIN_EMAIL`/`$APP_ADMIN_PASSWORD`, el perfil mostró solo nombre y email (`Admin Demostración · admin@demo.invalid`), sin hash.
- `Ver día` y `Ver semana` cargaron el turno con `nombre`/`apellido` resueltos desde `GET /api/patients?page=0&size=100` (una llamada por carga de agenda, parámetros exactos). Red: `GET /api/appointments?from=...&to=...` → 200.
- `PATCH /api/appointments/52acd162-8642-45a4-892e-0882076d8b74/status` con `{"status":"CONFIRMED",...}` → **200**, UI mostró `estado actualizado`; el turno pasó a `CONFIRMED`.
- `Buscar pacientes` → 200, paginación `Página 1 de 1 · 2 pacientes`, con los datos ficticios creados.
- `Cargar bandeja` → 200, `2 mensajes`, con subject, email, estado y fecha.
- En el mensaje vigente: `GET /api/public/appointments/{token}/confirm` → **200** `{"startAt":"2026-11-26T12:00:00Z","status":"CONFIRMED"}`, UI: `Inicio: ... · Estado: CONFIRMED`; `POST` del mismo enlace → **200** `{"message":"ya confirmado","status":"CONFIRMED"}`, UI: `ya confirmado · Estado: CONFIRMED`. No hubo `POST` automático: solo el clic explícito.
- `Cargar lista de espera` → 200, `1 entradas · sin asignación`, fila `Espera Demo Ficticia · espera@example.invalid · 2026-11-27 a 2026-11-30 · WAITING`, sin DNI.
- Único error de consola nuevo durante la ruta feliz: `favicon.ico` 404 (no funcional).

### 5.2 — Sesión vencida / logout

- API: `POST /api/auth/logout` → **204** con cookie invalidada; luego `GET /api/patients?page=0&size=20` → **401**, `Content-Type: application/json;charset=UTF-8`, body `{"error":"No autenticado"}`, sin `Location` ni redirect.
- Navegador: tras logout la UI quedó en `admin.html` con el formulario de login. Para simular expiración con sesión ya iniciada se invalidó la sesión mediante `POST /api/auth/logout` real desde el propio documento (204) y se pulsó `Cargar bandeja`: la petición devolvió **401** una sola vez, la UI mostró `Su sesión no está disponible. Ingrese nuevamente.` en el mismo documento, sin reintento (`GET /api/outbox/messages`: 200 inicial y 401 posterior) ni cambios locales.

### 5.3 — Formatos de error

Ambos formatos de error (`{"error":...}` y `ProblemDetail` con `detail`) verificados contra el backend real:

| Petición | Respuesta real |
|---|---|
| `GET /api/patients?size=0` | **400**, `Content-Type: application/problem+json`, `{"type":"about:blank","title":"Bad Request","status":400,"detail":"Datos de paciente inválidos.","instance":"/api/patients"}` |
| `GET /api/appointments` sin parámetros | **400**, `{"error":"Parámetros from y to inválidos o faltantes"}` |
| `GET /api/appointments?from=...&to=...` con `from == to` | **422**, `{"error":"Rango de agenda inválido"}` |
| `PATCH /api/appointments/{id}/status` con transición inválida | **422**, `{"error":"Transición de estado inválida"}` |
 |
| `POST /api/public/appointments` repetido en la misma franja | **422**, `{"error":"La franja solicitada no está disponible"}` |
| `GET /api/public/appointments/{token_stale}/confirm` | **404**, `{"error":"Token inexistente o inválido"}` |
| `POST /api/public/appointments/{token_vigente}/confirm` sobre turno cancelado | **409**, `{"error":"El turno no puede confirmarse"}` |

En UI: `size=0` mostró únicamente `Datos de paciente inválidos.`; el 401 mostró `No autenticado` vía sesión; el 422 de agenda mostró `Rango de agenda inválido`; el 404 mostró `Token inexistente o inválido`; el 409 real mostró `El turno no puede confirmarse`. El backend real incluyó `instance` en el 400 de pacientes; la spec y la task 5.3 revisadas aceptan `ProblemDetail` con o sin `instance` y exigen mostrar solo `detail`.

Fuera de alcance de C-12: la validación del alta en `POST /api/public/waiting-list` (observada como 400 `ProblemDetail` con `detail="Datos de paciente inválidos."`) pertenece a un endpoint público, no a la consola; no se cuenta como verificación ni como fallo. La vista de lista de espera (`GET /api/waiting-list` autenticado) permanece en alcance, con las entradas, el orden FIFO, la ausencia de DNI y el estado vacío cubiertos por tests unitarios.

**500 controlado según task, NO generado por backend:** se interceptó `GET /api/outbox/messages` en el navegador con `{"type":"about:blank","title":"Error interno","status":500,"detail":"Error interno al procesar la bandeja"}` y `Content-Type: application/problem+json`. La UI mostró solo `Error interno al procesar la bandeja`, sin `title`, `type` ni `instance`. La interceptación se retiró después. Esto **no** es evidencia de que el backend produzca ese 500.

### 5.4 — Bordes

- Rango sin turnos: `2026-11-30` → `GET /api/appointments?from=2026-11-30T00:00:00Z&to=2026-12-01T00:00:00Z` → 200 `[]`; UI: `Sin turnos en el rango seleccionado.`
- `from >= to`: UI mostró `Rango de agenda inválido` y la red registró **422** real con `{"error":"Rango de agenda inválido"}`.
- Parámetros ausentes: verificados contra el backend por **HTTP directo**, con **400** real `{"error":"Parámetros from y to inválidos o faltantes"}`; en la UI, al vaciar ambos campos, **no se emitió petición HTTP** y se mostró el mismo texto por validación previa del cliente. El bloqueo del cliente queda documentado como límite, sin presentarlo como petición end-to-end.
- Página sin coincidencias: `GET /api/patients?dni=99999999&page=0&size=20` → 200 `{"content":[],...}`; UI: `Página 1 de 0 · 0 pacientes` y `Sin pacientes para los filtros seleccionados.`, conservando DNI y página.
- Enlace inexistente: 404 real ya documentado en 5.3/5.1, con mensaje exacto en UI.
- Enlace vencido (410): **NO VERIFICADO end-to-end**. No existe forma de generarlo mediante la API real en esta sesión: el token expira en el inicio del turno o a los 7 días, y la ventana pública impide turnos pasados. La cobertura de `Token vencido` es solo unitaria (`frontend/src/admin/outbox.test.ts`), no evidencia de backend.
- Se canceló el turno vía UI (motivo ficticio) para provocar el 409 real indicado en 5.3; la agenda pasó a `Sin turnos en el rango seleccionado.` y la UI mostró `cancelado`.

### 5.5 — Diff de implementación

- `git diff --numstat -- frontend`: `frontend/admin.html`, `frontend/package.json`, `frontend/src/node-test-shims.d.ts`, `frontend/tsconfig.json`; además, sin versionar: `frontend/src/admin/` y `frontend/tsconfig.admin.json`. Solo `frontend/`.
- `git diff --stat -- backend`: vacío. `git diff --stat -- CHANGES.md .active-orchestrator-state.json`: vacío.
- Búsqueda de secretos/DNI/emails en `frontend/src/admin/`: solo marcadores ficticios (`12345678`, `87654321`, `*@example.invalid`, `*@demo.invalid`, `DemoPasswordOnly`, `must-not-be-shown`, `tokenFicticio123`). No hay credenciales reales ni claves.
- Los cambios en `openspec/changes/admin-agenda-ui/{design.md,specs/.../spec.md,tasks.md}` y este `verification.md` son **artefactos del flujo OpenSpec**, no código de implementación. El working tree también contiene artefactos sin versionar ajenos a este change (`.playwright-mcp/`, `design-system/`, `openspec.zip`, `backend/package-lock.json`, `frontend/.playwright-mcp/`); no fueron modificados por esta verificación.

## Discrepancias con la spec / tasks

1. Resuelto por decisión del usuario: `GET /api/patients?size=0` devuelve `"instance":"/api/patients"`; la spec y la task 5.3 revisadas aceptan `ProblemDetail` con o sin `instance` y solo exigen mostrar `detail`.
2. Fuera de alcance de C-12: la validación de `POST /api/public/waiting-list` es un endpoint público, no de la consola. No se elimina la vista de lista de espera ni sus comportamientos probados.
3. Duplicar una franja: la spec esperaba **409** `La franja solicitada ya está ocupada`; la API devolvió **422** `La franja solicitada no está disponible` (validación de disponibilidad previa al conflicto de persistencia). El 409 real obtenido fue otro caso: confirmar por token un turno cancelado, con mensaje `El turno no puede confirmarse`. Queda documentada como limitación; no se modifica su expectativa en spec/tasks sin autorización nueva.
4. El 500 `ProblemDetail` de bandeja no fue reproducible por API real; se verificó con la respuesta controlada que la propia task 5.3 prescribe, etiquetada arriba como no backend.
5. `GET /api/schedule` fue necesario para inicializar la agenda; sin ese paso, los slots públicos responden 422 `Configuración de agenda inexistente`. La task 5.0 no lo menciona.

## Limitaciones y lo NO probado

- **No se probó con PostgreSQL.** Solo H2 en memoria con `MODE=PostgreSQL`.
- No se usó dev-server: el backend sirvió `frontend/dist/`.
- **410 no reproducible vía API ni verificado end-to-end** (token vencido); la cobertura es unitaria (`frontend/src/admin/outbox.test.ts`).
- El 500 de bandeja es simulado/controlado, no generado por el backend.
- La lista de espera vacía en navegador no se verificó: no hay endpoint de borrado y los datos generados deben permanecer; la cobertura es unitaria (`frontend/src/admin/waiting-list.test.ts`).
- Alta inválida de lista de espera fuera de alcance de C-12: `POST /api/public/waiting-list` es un endpoint público, no de la consola.
- Discrepancia documentada: duplicar una franja respondió **422** real frente al **409** previo de spec/tasks; no se cambió esa expectativa sin autorización nueva, y el mensaje `La franja solicitada ya está ocupada` no se verificó en navegador generado por backend.
- Parámetros de agenda ausentes: verificados por HTTP directo contra el backend; en navegador el cliente bloquea la petición, por lo que ese caso no es end-to-end.
- Fenómeno observable de backend: el recordatorio registra un token nuevo y deja obsoleto el enlace del mensaje original.
- Error de consola `favicon.ico` 404, no funcional.
- Sesión expiró naturalmente durante la ejecución (timeout configurado de 30 m) y se re-ingresó con credenciales ficticias; queda registrado como comportamiento esperado.

## Cierre del entorno

`taskkill /PID <root> /T /F` sobre el proceso temporal: se terminaron los procesos hijos y el root. Verificación final: `PORT_8080_CLOSED` y `HEALTH_UNREACHABLE`. **No se hizo commit ni Archive.**

5.3 y 5.4 quedan marcados como **PASS** únicamente bajo la redacción revisada: `ProblemDetail` con o sin `instance`, 500 verificado con respuesta controlada, 410 con cobertura unitaria y parámetros ausentes verificados por HTTP directo además del bloqueo del cliente. Permanecen documentadas las limitaciones: **No se probó con PostgreSQL**, 410 no reproducible vía API, 500 simulado/controlado y la discrepancia 422 vs 409 de franja duplicada. No se ejecutó Archive.
