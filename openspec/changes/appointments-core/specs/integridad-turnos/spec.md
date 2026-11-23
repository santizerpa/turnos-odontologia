# Spec Delta

## MODIFIED Requirements

### Requirement: active_slot debe conservar la franja solo mientras el turno está activo
Appointment SHALL usar active_slot igual a start_at para estados activos PENDING, CONFIRMED, ABSENT o ATTENDED y SHALL dejarlo NULL al aplicar CANCELLED. El servicio de dominio SHALL ser la única vía de modificación de estado y active_slot durante creación y cancelación.

#### Scenario: Se crea un turno activo
- **WHEN** el servicio persiste un Appointment PENDING con duración 30
- **THEN** active_slot es igual a start_at

#### Scenario: Se confirma un turno
- **WHEN** el servicio aplica PENDING → CONFIRMED
- **THEN** active_slot permanece igual a start_at

#### Scenario: Se cancela un turno
- **WHEN** el servicio aplica la primera transición válida a CANCELLED
- **THEN** el turno conserva start_at
- **AND** active_slot pasa a NULL

#### Scenario: Se repite la cancelación
- **WHEN** el servicio recibe una nueva cancelación para un turno ya CANCELLED
- **THEN** active_slot sigue NULL
- **AND** no se crea otro cambio de estado ni evento de dominio

### Requirement: active_slot debe ser único para turnos activos
La restricción UNIQUE sobre active_slot SHALL proteger turnos activos y SHALL impedir persistir simultáneamente dos reservas para el mismo inicio. La operación transaccional SHALL materializar el conflicto como excepción de servicio o integridad persistida y traducirla a HTTP 409 en el boundary HTTP.

#### Scenario: Dos turnos activos usan la misma franja
- **WHEN** dos operaciones concurrentes intentan persistir turnos activos con el mismo active_slot
- **THEN** una operación persiste
- **AND** la otra recibe HTTP 409 sin crear el segundo turno

#### Scenario: Dos turnos cancelados usan la misma franja
- **WHEN** existen turnos CANCELLED que usan active_slot NULL
- **THEN** ambos registros pueden coexistir sin violar la unicidad

#### Scenario: Se reutiliza una franja cancelada
- **WHEN** se crea un turno nuevo después de liberar el active_slot de un turno cancelado
- **THEN** la persistencia acepta el nuevo turno activo
