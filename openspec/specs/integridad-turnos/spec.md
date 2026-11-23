# integridad-turnos Specification

## Purpose

Establecer las invariantes persistentes de turnos, estados, tokens y franja activa que protejan reservas concurrentes y cancelaciones.

## Requirements

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

### Requirement: active_slot no debe aceptarse mediante escritura directa externa
El modelo SHALL impedir que controladores, repositorios o código ajeno al servicio de dominio mantengan active_slot mediante setters públicos o escrituras directas.

#### Scenario: Se intenta alterar active_slot fuera del servicio
- **WHEN** código distinto del servicio de dominio intenta cambiar active_slot
- **THEN** el diseño no expone una operación pública equivalente
- **AND** active_slot solo cambia al crear o cancelar mediante la operación mantenida por el servicio

### Requirement: AppointmentToken debe almacenar exclusivamente el hash de un token opaco
AppointmentToken SHALL persistir token_hash como SHA-256 y SHALL no incluir un campo para el token plano.

#### Scenario: Se crea un token válido
- **WHEN** se calcula el hash SHA-256 del token de al menos 128 bits de entropía
- **THEN** solo el hash hexadecimal o textual de longitud definida se persiste
- **AND** el valor plano no existe como columna ni atributo persistido

### Requirement: El token no se consume al confirmar
La entidad AppointmentToken SHALL seguir vigente hasta expires_at después de una confirmación exitosa.

#### Scenario: Se confirma un turno
- **WHEN** C-09 registra CONFIRMED o ejecuta una confirmación idempotente
- **THEN** AppointmentToken no cambia su vigencia por consumo

#### Scenario: Se confirma desde un estado CANCELLED
- **WHEN** el turno ya está CANCELLED y se intenta confirmar con el token
- **THEN** el modelo conserva el estado y la entidad no habilita una confirmación posterior

### Requirement: El modelo debe permitir idempotencia sin implementar su orquestación
Los estados y relaciones SHALL conservar suficiente información para que C-05 y C-09 puedan implementar confirmación y cancelación idempotentes, sin consumir el token.

#### Scenario: Se repite una cancelación
- **WHEN** C-05 recibe una segunda cancelación del mismo turno
- **THEN** el estado CANCELLED ya existe y active_slot sigue NULL

#### Scenario: Se repite una confirmación
- **WHEN** C-09 recibe una confirmación repetida del mismo turno aún confirmado
- **THEN** el modelo permite reconocer la operación como reiterada sin alterar el token ni crear otro turno
