# Propuesta

## Why

Los pacientes no tienen canal público para ver ni reservar turnos: no existe ningún endpoint público, y toda la agenda se gestiona por el odontólogo autenticado. Flujo 2 de `knowledge-base/07_flujos_principales.md` (Reserva pública) no está implementado y C-08 (public-booking-ui) depende de esta API de reserva pública.

## What Changes

- Nuevo endpoint público `GET /api/public/slots?date=`: consulta de disponibilidad libre dentro de la ventana pública de 24hs–30d (RN-RS-01, `PublicBookingWindow` de C-04).
- Nuevo endpoint público `POST /api/public/appointments`: reserva de turno (DTO `nombre`, `apellido`, `dni`, `email`, `startAt` — la franja seleccionada). Reusa C-06 (`PatientService.upsert`), C-05 (`AppointmentService.create`) y C-04 (`AvailabilityService.freeSlots`); emite el token mediante `AppointmentTokenService.issue` (C-09) y crea un `OutboxMessage` con los links de confirmación/cancelación (`GET /api/public/appointments/{token}/confirm` y `/cancel`).
- Validación: 422 ante validación inválida (DNI, email, nombre vacío, franja fuera de la ventana) y 409 ante solapamiento (C-05). Sin rate limit en v1 (KB 10 Seguridad: no se implementa en v1).

## Capabilities

### New Capabilities
- `public-booking`: comportamiento observable de la disponibilidad y reserva públicas (endpoints `/api/public/slots` y `/api/public/appointments`).

### Modified Capabilities
- Sin modificaciones de capacidades existentes.

## Impact

- Nuevos archivos en `com.turnosodontologia.appointment`: `PublicBookingController`, `PublicBookingService`, `PublicBookingRequest`, `PublicBookingResponse` mínimo (`startAt`, `durationMinutes`, `status`), `PublicSlotsResponse`, `PublicBookingExceptionHandler`. La respuesta HTTP 201 nunca incluye el token ni ids internos (`dentistId`, `patientId`, `activeSlot`).
- Security: `/api/public/**` ya permitido por `AuthSecurityConfiguration` (C-03).
- No se modifican handlers existentes: `PatientExceptionHandler` queda sin cambios (open); `AppointmentExceptionHandler` (paquete) cubre el controlador público sin modificaciones.
- Verificación: wrapper de Gradle (`./gradlew test`) con H2 en modo PostgreSQL (perfil `test`).

---
*Planning only. Sin implementación de código, sin commits. Tareas dependientes de merge de C-09 (issue de AppointmentTokenService + outbox link) marcadas en tasks.md.*
