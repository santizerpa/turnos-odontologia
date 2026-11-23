# gestion-pacientes Specification

## Purpose

Definir el comportamiento observable de la gestión de pacientes del odontólogo: alta, reutilización por DNI, listado paginado y búsqueda, apoyándose en las reglas de validación ya vigentes.

## Requirements

### Requirement: El alta de pacientes SHALL crear o reutilizar una identidad por DNI normalizado
El sistema SHALL crear un paciente cuando el DNI normalizado no existe y SHALL reutilizar la identidad existente cuando ya existe, sin generar duplicados.

#### Scenario: Alta de un paciente nuevo
- **GIVEN** un DNI ficticio `27123456`, nombre y apellido completos y un email válido `paciente.ficticio@example.com`
- **WHEN** no existe ningún paciente con ese DNI normalizado y se solicita su alta
- **THEN** se crea una única identidad con el DNI normalizado `27123456` y la operación responde `201` con los datos del paciente

#### Scenario: Reutilización con formatos de DNI distintos
- **GIVEN** ya existe el paciente ficticio con DNI normalizado `27123456`
- **WHEN** se solicita el alta con `27.123.456` y luego con `27 123 456`
- **THEN** ambos valores se normalizan a `27123456` con la misma regla vigente y se reutiliza la misma identidad, sin crear un segundo paciente

#### Scenario: Upsert con datos actualizados
- **GIVEN** ya existe el paciente ficticio con DNI `27123456`, nombre `Paciente`, apellido `Ficticio` y email `paciente.ficticio@example.com`
- **WHEN** se envía el mismo DNI normalizado con nombre, apellido o email distintos
- **THEN** se actualizan esos campos sobre la misma identidad `27123456`, se normaliza el email con la regla vigente y la operación responde `200` sin crear un segundo paciente

#### Scenario: Upsert con email inválido no modifica al paciente existente
- **GIVEN** ya existe el paciente ficticio con DNI `27123456` y email `paciente.ficticio@example.com`
- **WHEN** se envía el mismo DNI con el email inválido `paciente.ficticio@ejemplo`
- **THEN** se responde `400` con un error de negocio y el paciente existente conserva su nombre, apellido y email sin modificación

#### Scenario: Upsert con nombre o apellido inválidos no modifica al paciente existente
- **GIVEN** ya existe el paciente ficticio con DNI `27123456`, nombre `Paciente` y apellido `Ficticio`
- **WHEN** se envía el mismo DNI con el nombre vacío o con el apellido solo espacios
- **THEN** se responde `400` con un error consistente y el paciente existente conserva sus datos sin modificación

### Requirement: Nombre y apellido SHALL ser obligatorios y no vacíos
El sistema SHALL exigir `nombre` y `apellido` en el alta y en la actualización: valores no nulos, no vacíos y no compuestos solo por espacios, con una longitud máxima de 100 caracteres. Los valores inválidos SHALL rechazarse con `400` y un error consistente, sin persistir un paciente nuevo ni modificar uno existente. La validación SHALL ocurrir en el DTO de request mediante Bean Validation (`@NotBlank`, `@Size(max=100)`), sin duplicar la lógica de la entidad.

#### Scenario: Alta sin nombre o apellido
- **GIVEN** un DNI ficticio `27123456` y un email válido `paciente.ficticio@example.com`, sin el campo `nombre` o sin el campo `apellido`
- **WHEN** se solicita el alta del paciente
- **THEN** se responde `400` con un error consistente y no se persiste ningún paciente

#### Scenario: Nombre o apellido vacío o solo espacios
- **GIVEN** un DNI ficticio `27123456` y un email válido, con `nombre` `""` o `"   "` y con `apellido` `""` o `"   "`
- **WHEN** se solicita el alta del paciente
- **THEN** se responde `400` con un error consistente, no se persiste ningún paciente y el mensaje no incluye datos personales

#### Scenario: Nombre o apellido con más de 100 caracteres
- **GIVEN** un DNI ficticio `27123456`, un email válido y un `nombre` o `apellido` de 101 caracteres
- **WHEN** se solicita el alta del paciente
- **THEN** se responde `400` con un error consistente y no se persiste ningún paciente

### Requirement: El email SHALL normalizarse en Patient antes de persistirse
El sistema SHALL quitar los espacios circundantes, convertir el email a minúsculas y exigir que el dominio contenga al menos un punto, según la regla vigente en `Patient.normalizeEmail`. Esta normalización SHALL ejecutarse únicamente en `Patient`: el DTO no define el formato y el service no reemplaza ni convierte valores. Un email inválido SHALL rechazarse con `400` sin persistirse.

#### Scenario: Email con espacios y mayúsculas se normaliza
- **GIVEN** el email `  Paciente.Ficticio@Example.COM  ` y un DNI ficticio `27123456`
- **WHEN** se solicita el alta del paciente
- **THEN** se persiste el email `paciente.ficticio@example.com` y la operación responde `201`

#### Scenario: Email con dominio que contiene un punto se acepta
- **GIVEN** el email `paciente.ficticio@consultorio.example.com` y un DNI ficticio `27123456`
- **WHEN** se solicita el alta del paciente
- **THEN** se persiste el email normalizado y la operación responde `201`

#### Scenario: Dominio sin punto se rechaza
- **GIVEN** el email `paciente.ficticio@ejemplo` y un DNI ficticio `27123456`
- **WHEN** se solicita el alta del paciente
- **THEN** se responde `400` con un error de negocio, no se persiste ningún paciente y el mensaje no incluye el email

### Requirement: El listado de pacientes SHALL ser paginado
El sistema SHALL devolver pacientes en páginas acotadas, con metadatos de paginación, y SHALL evitar listas sin límite.

#### Scenario: Listado paginado
- **GIVEN** existen varios pacientes ficticios persistidos
- **WHEN** se solicita `GET /api/patients` con `page=0` y `size=2`
- **THEN** se responde `200` con como máximo dos pacientes y metadatos de página, sin exponer detalles internos de persistencia

#### Scenario: Tamaño de página inválido
- **GIVEN** un cliente solicita un `size` fuera del rango permitido
- **WHEN** se procesa la solicitud de listado
- **THEN** se responde `400` con un error consistente y no se ejecuta una consulta sin límite

### Requirement: La búsqueda por DNI SHALL aplicar la misma normalización que el alta
El sistema SHALL interpretar cualquier representación válida del DNI como la misma clave de búsqueda, reutilizando la regla de normalización definida en `validacion-datos`.

#### Scenario: Búsqueda con DNI formateado
- **GIVEN** existe un paciente ficticio con DNI `27123456`
- **WHEN** se solicita el listado con el filtro `dni=27.123.456`
- **THEN** se normaliza a `27123456` y se devuelve ese único paciente en la respuesta

#### Scenario: Búsqueda sin coincidencias
- **GIVEN** no existe ningún paciente con el DNI normalizado `27123456`
- **WHEN** se solicita el listado con el filtro `dni=27123456`
- **THEN** se responde `200` con una página vacía y metadatos de paginación

#### Scenario: Búsqueda con DNI inválido
- **GIVEN** un valor de filtro con letras o con longitud fuera de 7 u 8 dígitos
- **WHEN** se solicita la búsqueda por DNI
- **THEN** se responde `400` con un error de negocio y no se consultan pacientes

### Requirement: Los errores de negocio SHALL ser consistentes y no exponer información sensible
El sistema SHALL rechazar entradas inválidas con una respuesta de error estructurada y SHALL evitar la exposición de DNI o email en mensajes y registros.

#### Scenario: Email inválido en el alta
- **GIVEN** un DNI válido y un email inválido `paciente.ficticio@ejemplo`
- **WHEN** se solicita el alta del paciente
- **THEN** se responde `400` con un error de negocio, no se persiste ningún paciente y el mensaje no incluye el email ni detalles internos

#### Scenario: DNI inválido en el alta
- **GIVEN** un DNI con letras como `27123A56`
- **WHEN** se solicita el alta del paciente
- **THEN** se responde `400` con un error de negocio y no se persiste ningún paciente
