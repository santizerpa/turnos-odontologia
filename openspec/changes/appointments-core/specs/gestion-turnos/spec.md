# Spec Delta

## Purpose

Definir el comportamiento observable del núcleo de turnos: creación, consulta de agenda, cambios de estado y cancelación idempotente para el odontólogo autenticado.

## ADDED Requirements

### Requirement: Crear turnos con paciente y duración fija
El sistema SHALL crear turnos asociados a un paciente existente y fijar su duración en 30 minutos.

#### Scenario: Creación válida
- **WHEN** se solicita crear un turno con odontólogo, paciente y hora de inicio válidos
- **THEN** el turno queda PENDING
- **AND** duration_minutes es 30
- **AND** active_slot es igual a start_at

#### Scenario: Duración distinta de 30 minutos
- **WHEN** se solicita crear un turno con otra duración
- **THEN** el sistema rechaza la creación
- **AND** no persiste el turno

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
El sistema SHALL aceptar solo PENDING → CONFIRMED | CANCELLED | ABSENT y CONFIRMED → CANCELLED | ABSENT | ATTENDED.

#### Scenario: Transición válida
- **WHEN** se solicita una transición incluida en la máquina de estados
- **THEN** el turno adopta el estado destino
- **AND** se registra el cambio

#### Scenario: Transición inválida
- **WHEN** se solicita volver de CANCELLED, ABSENT o ATTENDED a PENDING
- **THEN** el sistema rechaza la transición
- **AND** conserva el estado actual

### Requirement: Consultar la agenda por rango
El sistema SHALL exponer la agenda autenticada del odontólogo para fechas incluidas en los parámetros from y to.

#### Scenario: Consulta diaria o semanal
- **WHEN** el odontólogo solicita GET /api/appointments con from y to válidos
- **THEN** recibe los turnos del rango ordenados por hora de inicio

#### Scenario: Rango inválido
- **WHEN** from es posterior a to o los parámetros son inválidos
- **THEN** el sistema rechaza la consulta con un error de validación

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
