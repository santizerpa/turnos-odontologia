# Spec Delta

## ADDED Requirements

### Requirement: La búsqueda por nombre SHALL ser una alternativa exclusiva a la búsqueda por DNI
`GET /api/patients` SHALL aceptar el filtro opcional `nombre`, que devuelve, paginados, los pacientes cuyo nombre, apellido o "nombre apellido" contienen el texto buscado sin distinguir mayúsculas ni minúsculas y descartando espacios en los extremos. Los filtros `dni` y `nombre` SHALL ser mutuamente excluyentes. Un `nombre` vacío o solo con espacios equivale a no filtrar.

#### Scenario: Búsqueda parcial por nombre o apellido
- **GIVEN** pacientes ficticios "Ana Ejemplo", "Bruno Demo" y "Mariana Prueba"
- **WHEN** se solicita `GET /api/patients?nombre=ANA&page=0&size=10`
- **THEN** se responde `200` con "Ana Ejemplo" y "Mariana Prueba" y metadatos de paginación

#### Scenario: Búsqueda por nombre y apellido juntos
- **GIVEN** el paciente ficticio "Bruno Demo"
- **WHEN** se solicita `GET /api/patients?nombre=bruno%20demo`
- **THEN** se responde `200` con ese paciente

#### Scenario: Búsqueda por nombre sin coincidencias
- **GIVEN** ningún paciente contiene `zzz`
- **WHEN** se solicita `GET /api/patients?nombre=zzz`
- **THEN** se responde `200` con una página vacía y metadatos de paginación

#### Scenario: DNI y nombre a la vez
- **GIVEN** un cliente que envía `dni` y `nombre` no vacíos en la misma solicitud
- **WHEN** se procesa el listado
- **THEN** se responde `400` con el mismo formato de error de datos de paciente inválidos y no se consultan pacientes
