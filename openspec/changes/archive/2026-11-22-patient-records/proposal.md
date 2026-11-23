# Proposal

## Why

C-06 incorpora la alta y reutilización de pacientes: hoy el paquete `patient` solo tiene la entidad y el repositorio, sin servicio, endpoints ni búsqueda, y C-07 depende de esta capacidad. La normalización de DNI y email ya vive en `Patient`, por lo que el nuevo flujo debe apoyarse en ese único punto de verdad en lugar de repetir reglas.

## What Changes

- Incorporar un servicio de upsert de `Patient` por DNI normalizado que delegue la normalización en la entidad existente, sin duplicar regex ni reemplazos. Si el DNI ya existe y llegan nombre, apellido o email distintos, se actualizan sobre la misma identidad y se responde `200`; si algún dato es inválido, se responde `400` sin modificar el paciente existente.
- Exponer `GET /api/patients` con paginación y `POST /api/patients` con DTOs de request/response, sin exponer entidades. El DTO exige nombre y apellido no vacíos ni solo espacios, con `@NotBlank` y `@Size(max=100)`, y rechaza los valores inválidos con `400` sin persistir.
- Hacer explícito el comportamiento del email ya vigente en `Patient.normalizeEmail`: quita espacios circundantes, pasa a minúsculas y exige dominio con al menos un punto. La normalización sigue siendo responsabilidad de `Patient`, sin `@Email`/`@Pattern` en el DTO ni conversiones en el service.
- Implementar la búsqueda por DNI como filtro del listado, normalizando la entrada con el mismo punto de verdad.
- Mapear errores de negocio y de validación (DNI inválido, nombre o apellido inválidos, email inválido) a respuestas `ProblemDetail` consistentes, sin exponer datos internos ni PII en logs.
- Cubrir con pruebas TDD el alta, la reutilización por DNI con formatos distintos (`27.123.456` / `27 123 456` → `27123456`), la normalización del email, la actualización ante DNI existente y los casos borde de DNI, nombre y apellido.
- Mantener Java 17, Spring Boot 3.3.13 y el Gradle Wrapper existentes; no se agregan dependencias.

## Capabilities

### New Capabilities
- `gestion-pacientes`: alta, reutilización, listado paginado y búsqueda por DNI de pacientes para el odontólogo.

### Modified Capabilities
- Ninguna. `validacion-datos` se reutiliza por referencia; sus requirements no cambian y su spec main no se modifica.

## Impact

- Paquete `backend/src/main/java/com/turnosodontologia/patient`: nuevos DTO, service, controller, mapper y manejo centralizado de errores; `Patient` y `PatientRepository` se reutilizan y solo se ajustan si el upsert lo requiere. El esquema no cambia: `email` no pasa a ser único y no se incorporan teléfono ni fecha de nacimiento.
- API REST: `GET /api/patients` (paginado, filtro opcional `dni`) y `POST /api/patients`.
- Tests: JUnit 5 + Mockito, `@WebMvcTest`, `@DataJpaTest` y `@SpringBootTest` con H2 en modo PostgreSQL; verificación con `./gradlew build`.
- Fuera de alcance: C-03 `dentist-auth`, C-05 `schedule-rules`, C-07 `public-booking-api`, UI y cambios de versión de dependencias.
