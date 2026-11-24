# Design

## Context

Estado actual (de `backend/src/main`):
- v1 es single-dentist: `AdminDentistSeeder` crea el `Dentist` base desde `APP_ADMIN_EMAIL` (KB 04 `modelo-persistente`).
- C-04 (`AvailabilityService.freeSlots`, `PublicBookingWindow`, `ScheduleProperties` `app.schedule`, `ScheduleSlot`): disponibilidad libre en rango, franjas de 30 min fijas, `America/Argentina/Buenos_Aires` (POA-01, KB Flujo 2).
- C-05 (`AppointmentService.create`): crea el turno PENDING con active_slot = start_at; turno ocupado → `AppointmentConflictException` (integridad-turnos, KB 07 Flujo 2).
- C-06 (`PatientService.upsert`): upsert por DNI normalizado, validación estándar de DNI/email (validacion-datos).
- C-02 (`DentistRepository`, `OutboxMessage`: `PENDING`/`SIMULATED_SENT`, `AppointmentToken`: hash SHA-256, repositorios).
- C-03 (`AuthSecurityConfiguration`): `/api/public/**` ya `permitAll` — sin sesión pública necesaria (KB Flujo 2).
- C-09 (`AppointmentTokenService.issue(Appointment)` en `com.turnosodontologia.appointment`): pendiente merge en feat/augusto; C-07 planifica su integración, sin implementarla (ver D2).

## Goals / Non-Goals

Goals:
- Consulta de disponibilidad pública dentro de la ventana de 24hs–30d (RN-RS-01, KB Flujo 2).
- Reserva completa: upsert de `Patient` (C-06) + `Appointment` PENDING (C-05) + token emitido por C-09 + `OutboxMessage` con links de confirmación/cancelación.
- Códigos de error consistentes con el canal autenticado: 409 solapamiento, 422 validación.

Non-Goals:
- Endpoints de confirmación/cancelación por token (`GET/POST /api/public/appointments/{token}/confirm|cancel`): son C-09.
- UI pública (C-08), confirmación automática desde lista de espera (C-11), mail real (C-10).
- Rate limit (KB 10 Seguridad: no se implementa en v1).

## Decisions

### D1 — GET /api/public/slots
- Parámetro: `date` requerido (LocalDate, ISO `yyyy-MM-dd`, `@DateTimeFormat(iso = ISO.DATE)`).
- Zona fija: `America/Argentina/Buenos_Aires`, con Clock fijo en tests.
- Cálculo acotado al día: `earliest = max(inicioDelDia(date), publicWindow.earliest(clock))`; `latest = min(finDelDia(date), publicWindow.latest(clock))`.
- Se obtiene el rango local de consulta como `[earliest, latest]` solo si `earliest < latest`. Si el día completo queda antes de 24 horas o después de 30 días, C-07 responde 422 con mensaje fijo `La fecha solicitada está fuera de la ventana pública`.
- Ejemplo hoy a las 10:00 con ahora fijo: la fecha de hoy queda completamente antes de ahora+24h, por lo que 422. Mañana a las 15:00 es parcialmente válida: `from = max(inicioDelDia(mañana), now+24h)`, `to = finDelDia(mañana)` y solo se devuelven las franjas que quedan dentro de `[from, to]`.
- Último día parcialmente válido: `to = min(finDelDia(date), now+30d)` y se excluyen las franjas posteriores.
- Slots: `AvailabilityService.freeSlots(dentistId, from, to)` (C-04); response `PublicSlotsResponse(List<AvailabilitySlot>)` con start/end en offset UTC-3.
- No se modifica ni se amplía `PublicBookingWindow` de C-04. C-07 usa sus límites `earliest(clock)` y `latest(clock)` solo para acotar el rango local.
- Dentist: `publicDentist()` — `dentists.findAll()` debe devolver exactamente 1. Cero o más de uno es error interno de configuración, no 422; responde 503 con mensaje fijo `Servicio temporalmente no disponible` y no filtra detalles internos.

### D2 — Flujo de reserva y transacción (C-07)
`PublicBookingService.book(PublicBookingRequest)` — el límite transaccional debe comenzar después de todas las validaciones previas de ventana/franja y del diagnóstico de configuración:

```java
public PublicBookingResponse book(PublicBookingRequest request) {
    Dentist dentist = publicDentist();       // 503 si 0 o >1; antes de persistir
    Instant startAt = request.startAt();
    validatePublicWindow(startAt, clock);    // 422, antes del upsert
    if (!availability.isOpenSlot(dentist.getId(), startAt)) {
        throw new IllegalArgumentException("La franja solicitada no está disponible"); // 422
    }

    return inTransaction(() -> {
        PatientUpsertResult upsert = patients.upsert(...); // C-06
        Appointment appointment = appointments.create(
            dentist.getId(), upsert.patient().getId(), startAt, CreatedBy.PATIENT); // C-05
        String token = tokenService.issue(appointment); // C-09, después de saveAndFlush
        outbox.save(new OutboxMessage(..., bodyWithConfirmAndCancelLinks(token), PENDING));
        return new PublicBookingResponse(appointment.getStartAt(), appointment.getDurationMinutes(), appointment.getStatus());
    });
}
```

- Validación previa obligatoria: resolver el único Dentist, validar `startAt` dentro de la ventana y validar con `AvailabilityService.freeSlots`/`isOpenSlot` que coincide con una franja exacta, libre y no bloqueada. Esto ocurre antes del upsert y antes de crear el turno.
- C-05 no valida grilla, horario ni ScheduleBlock: C-07 aplica esa comprobación sin modificar C-05.
- Código de franja inválida/no libre: el dato no corresponde a una franja elegible del cliente. Es `422` con mensaje fijo `La franja solicitada no está disponible`. La KB no define un código específico para desalineación o bloqueos; por eso se usa un 422 derivado de la regla 409/422 sin ampliar la KB.
- DTO: `PublicBookingRequest(nombre @NotBlank @Size(max=100), apellido @NotBlank @Size(max=100), dni, email, startAt @NotNull Instant)`.
- Respuesta: `PublicBookingResponse(startAt, durationMinutes, status)`, HTTP 201; no incluye IDs internos, activeSlot ni token.
- `startAt`: requerido y enviado desde una franja devuelta por GET /slots. Se valida en ventana y con disponibilidad antes del upsert.
- Transacción: únicamente Patient + Appointment + token + Outbox deben compartir el límite; C-07 no atrapa excepciones dentro de ese límite para evitar rollback-only. Captura de HTTP se realiza fuera de la transacción en el handler.
- "Appointment must have id before issue()": `AppointmentService.create` persiste con `saveAndFlush` → el id queda asignado antes del retorno.
- C-07 no crea ni calcula hash manualmente: solo invoca `AppointmentTokenService.issue(appointment)`; el token plano se usa solo para construir el link del OutboxMessage.
- Excepciones: sin catch dentro de `book()`; se materializan en los handlers (D3).

### D3 — Materialización de errores (handlers)

- **409 solapamiento:** `AppointmentConflictException` (C-05) o `DataIntegrityViolationException` materializan 409 con mensaje fijo `La franja solicitada ya está ocupada`. C-07 usa un `PublicBookingExceptionHandler` con `@RestControllerAdvice(assignableTypes = PublicBookingController.class)` y `@Order(Ordered.HIGHEST_PRECEDENCE)`.
- **422 validación:** Bean Validation, DNI, email, nombre/apellido, ventana, date faltante/type mismatch y franja no libre se materializan 422 con mensajes fijos en español. Ningún handler público usa `ex.getMessage()`.
- **404:** reservado para C-09. C-07 no busca turnos ni define un 404 de reserva; el POST no debería producirlo.
- **503 configuración del odontólogo:** cero o más de un Dentist produce 503 con `Servicio temporalmente no disponible`. No es un 422 de validación del cliente y no se expone el detalle interno.
- **Mensajes públicos fijos:**
  - ventana/fecha: `La fecha solicitada está fuera de la ventana pública`
  - franja: `La franja solicitada no está disponible`
  - solapamiento: `La franja solicitada ya está ocupada`
  - validación: `Datos de reserva inválidos`
  - configuración: `Servicio temporalmente no disponible`

**Regla sobre handlers existentes:**
- C-07 necesita el handler público dedicado y específico para garantizar 422, no 400 ni mensajes internos.
- No se modifica `PatientExceptionHandler` ni `AppointmentExceptionHandler`.
- Punto abierto: si C-07 necesita resolver la precedencia global de estos handlers en el runtime, se mantiene fuera de esta propuesta hasta aplicar C-07.
- Riesgo: `AppointmentExceptionHandler` captura toda `DataIntegrityViolationException` y la traduce a conflicto de franja. Una DataIntegrityViolationException causada por otro dato persistente se mostraría también como `La franja solicitada ya está ocupada`; no existe hoy clasificación por constraint. Corregir esa clasificación exigiría cambios fuera de C-07 y queda abierto.

### D4 — KB tension (ambigüedad registrada)
KB 04: AppointmentToken persiste "solo el hash SHA-256; nunca el token plano" (integridad-turnos: "el valor plano no existe como columna ni atributo persistido"). Flujo 2: "Guarda OutboxMessage simulado" con links a los endpoints de confirmación/cancelación.
- Resolución de C-07: el `OutboxMessage.body` contiene el link clickeable con el token plano (Flujo 2 asume que el link se usa por el paciente; los endpoints de confirmación/cancelación son C-09). C-07 no viola RN-DA-03: solo invoca `issue()` y usa el token plano para el link; no persiste ni calcula hash.
- Ambigüedad (para C-09): si `OutboxMessage.body` debe contener el token plano, la regla RN-DA-03 ("nunca se guarda como texto plano") queda tensionada con los links simulados. Alternativas: (a) mantener el token plano en el body del OutboxMessage (decisión actual); (b) C-09 construye los links consultando el turno por hash al servir GET confirm/cancel. **Open: resolver en C-09.**

### D5 — Resolución del odontólogo público
v1 es single-dentist (`AdminDentistSeeder`). `publicDentist()` debe encontrar exactamente 1 Dentist; cero o más de uno constituye configuración inválida del servidor y responde 503 con `Servicio temporalmente no disponible`. Nunca se usa el detalle interno ni se mapea a 422. **Open:** selección multi-dentist — C-12 define selección explícita; v1 requiere un único Dentist registrado.

## Risks / Trade-offs

[Risk] La consulta por día puede calcular rangos parciales y necesita reloj fijo para ser determinista.
[Mitigation] C-07 acota el rango a `max(inicio del día, now+24h)` y `min(fin del día, now+30d)`; 422 solo si el día completo queda fuera.
[Risk] `startAt` en el POST: el cliente puede enviar una hora fuera de grilla o bloqueada.
[Mitigation] C-07 valida primero con `AvailabilityService.freeSlots`/`isOpenSlot`; fuera de grilla/bloqueada devuelve 422 fijo sin upsert.
[Risk] DTO usa `nombre`/`apellido` (español, consistente con `PatientRequest` de C-06); `dni`/`email` sin cambios.
[Mitigation] None — simpleza del contrato público.

## Open Questions

- OQ-1 resuelta: C-07 acota por día con `from = max(inicio del día, now+24h)` y `to = min(fin del día, now+30d)`; usa 422 solo si el día completo queda fuera.
- OQ-2: Tensión KB (D4) — resolver en C-09 si el body de Outbox puede contener el token plano mientras AppointmentToken persiste solo hash.
- OQ-3 resuelta: cero o más de un Dentist responde 503 fijo; no es 422.
- OQ-4 resuelta: POST responde HTTP 201 con `PublicBookingResponse(startAt, durationMinutes, status)`, sin ids internos ni token.
- OQ-5: El código específico del KB no define subject; se propone "Confirma tu turno" como texto no normativo de C-07, pendiente de revisión.
