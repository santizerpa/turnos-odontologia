# lista-espera Specification

## Purpose

Permite a pacientes anotarse en una lista de espera y al odontólogo consultarla. Cuando se libera un turno, notifica de forma simulada a una única entrada elegible en orden FIFO sin asignar el turno automáticamente.

## Requirements

### Requirement: El paciente puede inscribirse públicamente en la lista de espera
El sistema SHALL aceptar inscripciones sin sesión y SHALL asociarlas a un paciente identificado por DNI normalizado, sin modificar los datos de un paciente existente.

#### Scenario: Inscripción de un paciente nuevo
- **WHEN** se envía `POST /api/public/waiting-list` con DNI, nombre, apellido, email válidos y fechas opcionales válidas
- **THEN** se crea el paciente y una entrada `WAITING`
- **AND** se responde `201` sin revelar si el DNI existía previamente

#### Scenario: Inscripción de un paciente ya registrado
- **GIVEN** existe un paciente con el DNI normalizado recibido
- **WHEN** se envía una inscripción con ese DNI y datos de perfil diferentes
- **THEN** se crea una entrada `WAITING` asociada al paciente existente
- **AND** se conservan el nombre y el email ya guardados

#### Scenario: Datos de inscripción inválidos
- **WHEN** se envía un DNI, nombre, apellido, email o rango de fechas inválido
- **THEN** se responde `400`
- **AND** no se crea una entrada ni se modifica un paciente existente

#### Scenario: Inscripción sin sesión
- **WHEN** un paciente sin sesión válida envía `POST /api/public/waiting-list`
- **THEN** la solicitud puede procesarse según las validaciones de inscripción

### Requirement: Las fechas solicitadas determinan la elegibilidad
El sistema SHALL tratar `requested_date_from` y `requested_date_to` como límites inclusivos de fechas locales argentinas; cada límite nulo SHALL ser no restrictivo.

#### Scenario: Rango de fechas inclusivo
- **GIVEN** una entrada WAITING con rango desde el 10 hasta el 20 de diciembre
- **WHEN** se libera un turno en cualquiera de esas fechas locales
- **THEN** la entrada es elegible para recibir el aviso

#### Scenario: Un límite nulo no restringe
- **GIVEN** una entrada WAITING cuyo inicio o fin solicitado es nulo
- **WHEN** se libera un turno dentro del límite definido, si existe
- **THEN** la entrada es elegible sin restricción por el límite nulo

#### Scenario: Rango invertido
- **WHEN** se solicita una fecha inicial posterior a la fecha final
- **THEN** se responde `400` y no se crea la inscripción

### Requirement: El odontólogo autenticado puede consultar la lista FIFO
El sistema SHALL permitir al odontólogo autenticado consultar las entradas de la lista de espera ordenadas por `created_at` ascendente, mostrando estado y preferencias de fecha sin exponer el DNI.

#### Scenario: Consulta autenticada
- **WHEN** el odontólogo solicita `GET /api/waiting-list` con sesión válida
- **THEN** recibe las entradas ordenadas desde la más antigua y con sus estados actuales

#### Scenario: Consulta sin sesión
- **WHEN** se solicita `GET /api/waiting-list` sin sesión válida
- **THEN** se responde `401`

### Requirement: Una cancelación notifica a una sola entrada FIFO elegible
Al recibir un evento de cancelación confirmado, el sistema SHALL seleccionar la entrada `WAITING` elegible más antigua, marcarla `NOTIFIED` y crear un aviso simulado para el email guardado del paciente, dentro de una misma transacción nueva. Solo SHALL notificar si el inicio del turno está dentro de la ventana pública inclusiva de 24 horas a 30 días desde el reloj de agenda, y SHALL excluir al paciente del turno cancelado.

#### Scenario: Selección de la primera entrada elegible
- **GIVEN** hay varias entradas WAITING y la más antigua no incluye la fecha liberada, pero una posterior sí
- **WHEN** se procesa el evento del turno cancelado
- **THEN** se marca `NOTIFIED` solo la primera entrada elegible por FIFO
- **AND** se crea un único aviso para esa entrada

#### Scenario: La cancelación no avisa al propio paciente
- **GIVEN** el paciente del turno cancelado también tiene una entrada `WAITING`
- **AND** existe otra entrada `WAITING` elegible
- **WHEN** se procesa el evento del turno cancelado
- **THEN** se omite la entrada del paciente que canceló y se notifica la siguiente elegible

#### Scenario: Turno fuera de la ventana pública
- **GIVEN** el turno liberado está antes de 24 horas o después de 30 días desde ahora
- **WHEN** se procesa el evento de cancelación
- **THEN** no se cambia ninguna entrada ni se crea un aviso

#### Scenario: No hay entradas elegibles
- **GIVEN** no existe ninguna entrada WAITING cuyo rango incluya la fecha local del turno liberado
- **WHEN** se procesa el evento de cancelación
- **THEN** no cambia el estado de ninguna entrada
- **AND** no se crea un aviso

#### Scenario: No hay duplicados activos por paciente
- **GIVEN** el paciente ya tiene una entrada `WAITING`
- **WHEN** se registra nuevamente con el mismo DNI
- **THEN** se reutiliza la entrada activa y no se crea otra `WAITING`

#### Scenario: Aviso junto a recordatorio existente
- **GIVEN** ya existe un recordatorio `REMINDER` asociado al turno cancelado
- **WHEN** se procesa el evento para una entrada elegible
- **THEN** se persiste el aviso de lista de espera sin duplicar, reemplazar ni alterar el recordatorio

### Requirement: El aviso no reserva ni confirma el turno liberado
El sistema SHALL limitar el efecto de la lista de espera a notificar al paciente seleccionado, sin asignar el turno ni cambiar el estado del turno cancelado.

#### Scenario: Aviso sin asignación automática
- **WHEN** se procesa una cancelación con una entrada elegible
- **THEN** el paciente recibe un aviso simulado de disponibilidad
- **AND** el turno continúa cancelado y la franja permanece disponible para una reserva posterior
