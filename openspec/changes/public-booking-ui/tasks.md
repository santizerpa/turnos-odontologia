# Tareas

## 1. Estructura y cliente público

- [ ] 1.1 Crear la estructura TypeScript vanilla para la reserva pública (entrada, módulos de API/estado/render) y verificar que `npm run build` en `frontend/` genera `dist/` sin errores.
- [ ] 1.2 Implementar el cliente de endpoints públicos existentes (`/api/public/slots`, `/api/public/appointments`, `/api/public/appointments/{token}/confirm`, `/api/public/appointments/{token}/cancel`) y verificar con revisión de código que no usa endpoints nuevos ni modifica backend.
- [ ] 1.3 Implementar utilidades puras para ventana de fechas, extracción de token y mapeo del campo `error` a mensajes visibles para 409, 422, 404, 410, 503 y 500, más el fallback genérico `No se pudo completar la solicitud. Intente nuevamente más tarde.`; verificar con tests fuente en `frontend/src/**/*.test.ts`, compilando con `npm run build` y ejecutando `node --test` sobre `frontend/dist/**/*.test.js` usando `node:test` y `node:assert/strict`, sin agregar dependencias.

## 2. Flujo de reserva pública

- [ ] 2.1 Implementar selector de fecha dentro de la ventana permitida (24 h–30 d), consulta de slots y render de estados vacío/cargando/error; verificar manualmente que solo se consultan fechas válidas y que la lista de slots se actualiza.
- [ ] 2.2 Implementar formulario de datos del paciente y envío de reserva a `POST /api/public/appointments`; verificar con caso manual feliz que la UI muestra confirmación visible con inicio, duración 30 y estado `PENDING`, leyendo `startAt`, `durationMinutes` y `status` de la respuesta 201.
- [ ] 2.3 Implementar manejo visible de 409 (conflicto de negocio sobre franja/estado) en reserva; verificar manualmente que el paciente puede elegir otra franja o contactar al consultorio y que no hay éxito falso.
- [ ] 2.4 Implementar manejo visible de 422 (datos inválidos o fecha fuera de ventana) en reserva, mostrando el mensaje del campo `error` y conservando los valores del formulario; verificar manualmente que el paciente puede corregir datos/fecha sin perder lo ingresado.
- [ ] 2.5 Implementar mensaje genérico `No se pudo completar la solicitud. Intente nuevamente más tarde.` para 503, 500 y errores no mapeados, priorizando el `error` del backend cuando exista; verificar manualmente con una respuesta forzada 503 y confirmar que se ve el mensaje exacto del backend y que el fallback aparece si falta el campo.

## 3. Flujo "ya tengo un turno"

- [ ] 3.1 Implementar acceso "ya tengo un turno" que acepte token/enlace, consulte GET informativo y muestre estado a partir del JSON `{"startAt","status"}` con HTML renderizado en el cliente; verificar manualmente que la consulta GET no ejecuta cambios y que 404/410 se muestran visiblemente.
- [ ] 3.2 Implementar acciones de confirmar/cancelar por POST desde la UI de token, mostrando resultado o error 409 visible; verificar manualmente que POST solo ocurre por acción explícita del usuario.
- [ ] 3.3 Aplicar el mismo fallback genérico a errores no mapeados del flujo de token; verificar manualmente con una respuesta forzada sin campo `error` que se muestra `No se pudo completar la solicitud. Intente nuevamente más tarde.` y que el turno no cambia.

## 4. Verificación y documentación

- [ ] 4.1 Documentar verificación manual en el cambio (pasos de arranque, **solo datos ficticios**: nombre, apellido, DNI y email de prueba; nunca datos reales de pacientes; casos feliz/409/422/503-otro error con fallback/borde de token) y verificar que el checklist cubre todos los escenarios del delta spec.
- [ ] 4.2 Ejecutar la verificación automatizada de lógica frontend: `npm run build` en `frontend/` y `node --test` sobre `frontend/dist/**/*.test.js`; verificar que todos los tests pasan y asentar el resultado en notas de la tarea/PR interna del cambio.
- [ ] 4.3 Realizar revisión final de alcance: confirmar que no hay cambios en `backend/`, `CHANGES.md`, specs main, `.active-orchestrator-state.json`, `design-system/` ni configuración runtime; verificar con `git status`/diff de archivos del change antes de entregar.

**Checklist manual:** usa únicamente datos ficticios (nombre, apellido, DNI y email de prueba). Está prohibido usar datos reales de pacientes en pasos, capturas o ejemplos.
