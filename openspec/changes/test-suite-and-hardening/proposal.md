# Propuesta: suite de pruebas y hardening final

## Why

C-01…C-13 ya establecen las reglas funcionales y cuentan con pruebas repartidas por dominio; C-14 debe cerrar los huecos de evidencia, comprobar que el manejo de errores no filtre información interna y dejar una demo repetible antes de la entrega. La revisión es necesaria ahora porque el roadmap identifica este change como el gate final de calidad y su gobernanza es **ALTO**.

## What Changes

- Auditar la cobertura existente y completar únicamente los casos faltantes para colisión de slots/HTTP 409, ventana pública de 24 horas–30 días, DNI, token vencido, confirmación/cancelación idempotentes, cancelación dentro de las 24 horas, transiciones de estado y selección FIFO.
- Verificar con H2 en modo PostgreSQL la unicidad de `active_slot` y la reserva posterior a una cancelación; registrar por separado la evidencia de compatibilidad real con PostgreSQL, sin presentar H2 como sustituto de esa comprobación.
- Revisar las respuestas 4xx/5xx de los límites HTTP afectados y añadir aserciones contra mensajes de excepción, SQL, stack traces u otros detalles internos, preservando los códigos y mensajes públicos establecidos.
- Documentar un checklist de demo end-to-end que parte de la bandeja simulada y distingue claramente el despacho simulado de una entrega de correo real.
- Ejecutar la suite completa con el Gradle Wrapper y conservar salidas/informes reproducibles como evidencia de cierre.
- Mantener fixtures ficticios, pruebas deterministas con `Clock` cuando intervenga el tiempo y los patrones actuales de JUnit 5/Spring Boot.

El inventario inicial confirma cobertura existente para gran parte de los casos: reservas concurrentes y HTTP 409, límites de ventana, DNI, token vencido, repetición de operaciones, FIFO, estados y sanitización de algunos errores de reserva/outbox. C-14 consolidará y corregirá huecos reales, no duplicará pruebas equivalentes ni agregará pruebas sin aserciones útiles.

**Fuera de alcance:** cambios funcionales de producto o de contrato API, rediseño de seguridad, integración de correo real, datos reales de pacientes o credenciales reales, nuevas dependencias/infraestructura de test, upgrades implícitos de Java/Spring/JUnit, omitir tests, y cualquier edición de `CHANGES.md` (reservado al líder).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna. Las reglas que se van a verificar ya están definidas en las capacidades existentes (`gestion-turnos`, `integridad-turnos`, `disponibilidad-agenda`, `validacion-datos`, `token-confirmation`, `lista-espera`, `public-booking` y `outbox-reminders`), además de `knowledge-base/05_reglas_de_negocio.md`. El trabajo busca evidencia y conformidad, no un contrato de producto nuevo.

Este change declara `skip_specs: true` porque es una auditoría de pruebas, hardening de implementación para cumplir requisitos existentes y documentación de demo; no debe inventarse una especificación delta solo para satisfacer el esquema.

## Impact

- **Backend y tests:** `backend/src/main/java` solo si la auditoría demuestra una fuga o incumplimiento concreto; `backend/src/test/java` para completar casos faltantes con las capas unit, `@WebMvcTest`, `@DataJpaTest` o integración apropiadas.
- **Persistencia:** perfil de pruebas actual H2 PostgreSQL-mode; una comprobación contra PostgreSQL real requerirá una instancia local disponible y evidencia aparte. No se presupone que Docker esté configurado ni se añade Testcontainers.
- **Documentación:** checklist nuevo de demo end-to-end, limitado al flujo local de outbox simulado.
- **Build/API:** conservar Gradle Wrapper, Java 17 y Spring Boot 3.3.13 actuales; sin endpoints ni cambios intencionales de status codes o mensajes públicos.
- **Gobernanza y evidencia:** HIGH. Antes de cerrar, revisar diff, salidas de wrapper e informe de tests; registrar claramente cualquier validación PostgreSQL que quede pendiente, sin alterar `CHANGES.md`.
