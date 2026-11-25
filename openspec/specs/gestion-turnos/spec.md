# gestion-turnos Specification

## Purpose

Definir el comportamiento observable del núcleo de turnos: creación fija de 30 minutos, consulta de agenda, cambios de estado y cancelación idempotente para el odontólogo autenticado.

## Requirements

### Requirement: Crear turnos con paciente y duración fija
El sistema SHALL crear turnos asociados a un paciente existente, fijar internamente la duración en 30 minutos y no aceptar duración como entrada.

#### Scenario: Creación válida
- **WHEN** se solicita crear un turno con odontólogo, paciente y hora de inicio válidos
- **THEN** el turno queda PENDING
- **AND** duration_minutes es 30
- **AND** active_slot es igual a start_at

### Requirement: Prevenir solapamientos de turnos activos
La creación SHALL impedir dos turnos activos con el mismo inicio y SHALL responder conflicto ante una colisión de persistencia.

#### Scenario: Segunda reserva activa en la misma franja
- **WHEN** existe o se intenta persistir simultáneamente otro turno activo con el mismo active_slot
- **THEN** el sistema rechaza la operación con HTTP 409
- **AND** solo persiste una reserva activa

#### Scenario: Reserva luego de cancelación
- **WHEN** el turno previo fue cancelado y dejó active_slot en NULL
- **THEN** puede crearse otro turno activo en esa franja

### Requirement: Validar transiciones de estado
El sistema SHALL usar como matriz autoritativa `knowledge-base/05_reglas_de_negocio.md` §Estados: PENDING → CONFIRMED | CANCELLED | ABSENT y CONFIRMED → CANCELLED | ABSENT | ATTENDED.

#### Scenario: Transición válida
- **WHEN** se solicita una transición incluida en la matriz autoritativa
- **THEN** el turno adopta el estado destino
- **AND** se registra el cambio

#### Scenario: Transición inválida
- **WHEN** se solicita una transición fuera de la matriz, como volver a PENDING desde CANCELLED, ABSENT o ATTENDED
- **THEN** el sistema rechaza la transición usando el handler/código de error existente
- **AND** conserva el estado actual

### Requirement: Consultar la agenda por rango
El sistema SHALL exponer la agenda autenticada del odontólogo para fechas incluidas en los parámetros from y to.

#### Scenario: Consulta diaria o semanal
- **WHEN** el odontólogo autenticado solicita GET /api/appointments con from y to válidos
- **THEN** recibe los turnos del rango ordenados por hora de inicio

#### Scenario: Rango inválido
- **WHEN** from es posterior a to o los parámetros son inválidos
- **THEN** el sistema rechaza la consulta con un error de validación

#### Scenario: GET sin sesión
- **WHEN** se solicita GET /api/appointments sin sesión
- **THEN** C-03 responde HTTP 401

### Requirement: Cancelar desde el punto único del servicio
Toda cancelación SHALL pasar por AppointmentService.cancel y SHALL aplicar la regla de 24 horas para el actor PATIENT.

#### Scenario: Cancelación válida del paciente
- **WHEN** un actor PATIENT cancela un turno activo con al menos 24 horas de anticipación
- **THEN** el turno pasa a CANCELLED
- **AND** active_slot queda NULL
- **AND** se registra un evento de estado
- **AND** se publica un evento de cancelación

#### Scenario: Cancelación fuera de plazo del paciente
- **WHEN** un actor PATIENT cancela con menos de 24 horas de anticipación
- **THEN** el sistema rechaza la operación
- **AND** no cambia el turno ni publica eventos

#### Scenario: Segunda cancelación
- **WHEN** se cancela nuevamente un turno ya CANCELLED
- **THEN** la respuesta indica "ya cancelado"
- **AND** no registra otro AppointmentEvent
- **AND** no publica otro AppointmentCancelledEvent

### Requirement: Delegar cambios de estado desde la API
PATCH /api/appointments/{id}/status SHALL delegar el cambio al servicio de turnos sin mutar el estado directamente.

#### Scenario: Cambio de estado por el odontólogo
- **WHEN** un odontólogo autenticado solicita cambiar el estado de un turno
- **THEN** el controlador delega la operación al servicio
- **AND** el resultado refleja la transición validada

#### Scenario: Cancelación por PATCH
- **WHEN** un odontólogo autenticado envía PATCH status=CANCELLED con motivo
- **THEN** el controlador invoca AppointmentService.cancel(..., DENTIST, reason)
- **AND** no cambia directamente estado ni active_slot

#### Scenario: Repetición de cancelación por PATCH
- **WHEN** el odontólogo envía nuevamente status=CANCELLED para un turno ya CANCELLED
- **THEN** la respuesta indica "ya cancelado"
- **AND** no se crea AppointmentEvent ni AppointmentCancelledEvent nuevo

#### Scenario: Transición inválida por PATCH
- **WHEN** PATCH solicita una transición fuera de la matriz autoritativa
- **THEN** el servicio rechaza la operación mediante el handler/código de error existente
- **AND** el estado persistido no cambia

#### Scenario: Turno inexistente por PATCH
- **WHEN** PATCH identifica un appointmentId inexistente
- **THEN** el sistema responde HTTP 404
- **AND** no registra ni publica efectos

#### Scenario: PATCH sin sesión
- **WHEN** se solicita PATCH /api/appointments/{id}/status sin sesión
- **THEN** C-03 responde HTTP 401

### Requirement: El odontólogo no confirma turnos por la API administrativa
`PATCH /api/appointments/{id}/status` SHALL rechazar `status: CONFIRMED` con 422 y `{"error":"La confirmación del turno la realiza el paciente"}`, sin cambiar el estado, `active_slot`, eventos ni bandeja. La transición `PENDING → CONFIRMED` sigue siendo válida en la matriz de estados, pero solo se ejecuta mediante el token público del paciente.

#### Scenario: Odontólogo intenta confirmar
- **GIVEN** un turno ficticio `PENDING` y un odontólogo autenticado
- **WHEN** envía `PATCH /api/appointments/{id}/status` con `{"status":"CONFIRMED"}`
- **THEN** la API responde 422 con `{"error":"La confirmación del turno la realiza el paciente"}`
- **AND** el turno sigue `PENDING` y no se registra ningún `AppointmentEvent`

#### Scenario: Paciente confirma con su token
- **GIVEN** el mismo turno `PENDING` con un token vigente
- **WHEN** se envía `POST /api/public/appointments/{token}/confirm`
- **THEN** el turno pasa a `CONFIRMED` como antes de este cambio

#### Scenario: Otras acciones del odontólogo sin cambios
- **GIVEN** un turno ficticio `CONFIRMED`
- **WHEN** el odontólogo envía `PATCH` con `ATTENDED`, `ABSENT` o `CANCELLED` con motivo
- **THEN** la API aplica la transición como antes de este cambio
