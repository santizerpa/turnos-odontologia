# Design

## Context

C-02 ya aporta `Appointment`, `AppointmentRepository`, `AppointmentEventRepository`, `AppointmentEvent`, enums y la restricción única nullable de `active_slot`. C-03 aporta seguridad de sesiones y C-04 aporta configuración de agenda y `Clock scheduleClock`. C-05 debe reutilizar esas APIs sin alterar sus cambios ni crear entidades duplicadas. Véase proposal.md para el alcance.

## Goals / Non-Goals

**Goals:**
- Centralizar creación, estados y cancelación en `AppointmentService`.
- Mantener `active_slot` coherente y proteger el doble uso de una franja.
- Exponer agenda y cambio de estado a la API autenticada.
- Persistir auditoría y publicar una única cancelación transaccional.

**Non-Goals:**
- Implementar reserva pública, AppointmentToken, orquestación de tokens C-09, outbox o lista de espera.
- Modificar C-02/C-03/C-04/C-06 o `CHANGES.md`.
- Agregar UI o cambiar la configuración de schedule-rules.
- Exponer `POST /api/appointments`; C-05 solo contiene GET de agenda y PATCH de estado.

## Decisions

1. **Reutilizar entidades y repositorios existentes.**
   - `Appointment.create(...)` crea `PENDING` con duración fija 30 y `active_slot=startAt`; no recibe duración como entrada.
   - El repositorio existente aporta consulta por odontólogo/rango. Se puede agregar una consulta derivada o `@Query` para incluir turnos activos según agenda, sin reemplazar la entidad.

2. **Creación en transacción con doble defensa.**
   - `@Transactional` valida paciente, odontólogo y fija internamente 30 minutos, y verifica solapamiento mediante consulta de turnos activos antes de guardar.
   - La restricción única de `active_slot` es defensa final ante concurrencia. El conflicto puede materializarse como excepción de servicio propia o como `DataIntegrityViolationException`/violación JPA de la restricción única; el boundary HTTP traduce ambos a 409 mediante el handler existente, sin exponer detalles internos.

3. **Máquina de estados explícita en el servicio.**
   - La matriz de `knowledge-base/05_reglas_de_negocio.md` §Estados es autoritativa: `PENDING → CONFIRMED | CANCELLED | ABSENT` y `CONFIRMED → CANCELLED | ABSENT | ATTENDED`; no hay transición de regreso a `PENDING`.
   - El controlador no llama setters de estado. Cada transición real crea un `AppointmentEvent` con estados, actor y razón. Una solicitud inválida no persiste evento. Repetir una cancelación no es una transición a `CANCELLED`, sino una respuesta idempotente.

4. **Cancelación única e idempotente.**
   - `AppointmentService.cancel(UUID, Actor, String)` es el único camino de cancelación.
   - Si el estado ya es `CANCELLED`, devuelve el dominio específico con mensaje "ya cancelado" sin mutar ni publicar.
   - Para actor `PATIENT`, compara `startAt` con `Clock scheduleClock`; exige al menos 24 horas. El odontólogo no aplica esa restricción.
   - Al cancelar, el servicio pone estado `CANCELLED` y `active_slot=null`, conserva `startAt`, registra evento y llama al `ApplicationEventPublisher` dentro de la misma transacción.

5. **Evento transaccional compatible con C-11.**
   - `AppointmentCancelledEvent` conserva el payload acordado. Se publica solo al haber transición real.
   - El diseño no agrega listener ni outbox aquí; C-11 podrá consumirlo después sin modificar C-05.

6. **API protegida y respuestas consistentes.**
   - `/api/appointments` usa la `SecurityFilterChain` existente de C-03. GET y PATCH sin sesión reciben 401 por esa cadena de seguridad.
   - GET valida rango y devuelve agenda. PATCH recibe únicamente el estado destino y, si es `CANCELLED`, invoca `AppointmentService.cancel(..., DENTIST, reason)`; nunca cambia el estado directamente.
   - Las operaciones inexistentes usan 404, las transiciones inválidas usan el handler/código existente y el conflicto 409 se maneja sin exponer detalles internos.

7. **Prueba determinista.**
   - Reutilizar `AuthFixtures` y datos ficticios. H2 test profile en PostgreSQL mode.
   - Inyectar `Clock scheduleClock` en tests usando `Clock.fixed`; no usar `Instant.now()` ni Thread.sleep.

## Risks / Trade-offs

- [Concurrencia] → Validación más restricción única; traducción de integridad o conflicto de servicio a 409 en el boundary HTTP.
- [Estado inválido] → Guardas centralizadas según la KB y tests de matriz completa; el controlador no muta directamente.
- [Doble cancelación] → Retorno idempotente con mensaje específico y ausencia de evento adicional.
- [24 h dependiente del reloj] → Usar exclusivamente el bean `Clock scheduleClock`, configurable en tests.
- [Publicación de evento antes del commit] → Aplicar un patrón de publicación transaccional disponible en Spring; C-11 conserva listener después de commit.

## Migration Plan

- No se requiere migración DDL de C-02 para C-05: el modelo ya contiene columnas y restricción única previstas.
- Implementación incremental compatible con C-04 y C-03; despliegue local con perfil test o local vigente.
- Rollback: desactivar los nuevos endpoints/componentes sin revertir entidades C-02; no hay cambios destructivos de datos.
