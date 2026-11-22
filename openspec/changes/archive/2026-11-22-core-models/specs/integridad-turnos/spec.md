# Spec Delta

## Purpose

Establecer las invariantes persistentes de turnos, estados, tokens y franja activa que protejan reservas concurrentes y cancelaciones.

## ADDED Requirements

### Requirement: active_slot debe conservar la franja solo mientras el turno está activo
Appointment SHALL usar active_slot igual a start_at para estados distintos de CANCELLED y SHALL dejarlo NULL al cancelar.

#### Scenario: Se crea un turno activo
- **WHEN** se persiste un Appointment con estado PENDING, CONFIRMED, ABSENT o ATTENDED
- **THEN** active_slot es igual a start_at

#### Scenario: Se cancela un turno
- **WHEN** el servicio de dominio aplica la transición a CANCELLED
- **THEN** el turno conserva start_at
- **AND** active_slot pasa a NULL

### Requirement: active_slot debe ser único para turnos activos
El modelo SHALL definir una restricción UNIQUE nullable sobre active_slot, portable a PostgreSQL y H2 en modo PostgreSQL.

#### Scenario: Dos turnos activos usan la misma franja
- **WHEN** se intenta persistir un segundo turno activo con el mismo active_slot
- **THEN** la base rechaza el registro por la restricción única

#### Scenario: Dos turnos cancelados usan la misma franja
- **WHEN** existen turnos CANCELLED que usan active_slot NULL
- **THEN** ambos registros pueden coexistir sin violar la unicidad

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
