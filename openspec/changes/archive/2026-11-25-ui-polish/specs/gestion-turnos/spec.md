# Spec Delta

## ADDED Requirements

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
