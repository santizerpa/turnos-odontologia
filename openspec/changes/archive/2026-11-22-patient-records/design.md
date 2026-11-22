# Design

## Context

Ver `proposal.md` para la motivación y `specs/gestion-pacientes/spec.md` para el contrato observable. El código vigente tiene `Patient` con `dni` único normalizado y `PatientRepository.findByDni`, pero no hay service, controller, DTO ni búsqueda. El stack es Java 17, Spring Boot 3.3.13 y Gradle Wrapper (`backend/build.gradle:1-16`); los tests usan H2 en modo PostgreSQL y no requieren PostgreSQL local. La spec main `validacion-datos` ya define normalización de DNI, unicidad de DNI y formato de email: se reutiliza, no se modifica. En el código vigente, `Patient.normalizeEmail` (`Patient.java:45-50`) aplica `trim()`, `toLowerCase()` y una regex cuyo dominio debe contener al menos un punto. `first_name` y `last_name` son `varchar(100)` requeridos (`knowledge-base/04_modelo_de_datos.md` §Patient, RN-RS-05 en `knowledge-base/05_reglas_de_negocio.md`), pero el constructor de `Patient` no valida el valor de esos campos: `nullable=false` es solo una restricción de columna (`Patient.java:20-24`).

## Goals / Non-Goals

**Goals:**
- Un único punto de verdad para la normalización de DNI y email, compartido por alta, búsqueda y actualización.
- Upsert por DNI que preserve una sola identidad por paciente y actualice los datos cuando cambian.
- Nombre y apellido requeridos y no vacíos, validados en el DTO con un error `400` consistente y sin persistir.
- API REST con DTOs, paginación, errores `ProblemDetail` y sin PII en logs.
- Cobertura TDD con triangulación (happy + edge) en los cuatro niveles de test del proyecto.

**Non-Goals:**
- Autenticación/autorización del odontólogo (C-03), reglas de agenda (C-05) y API pública de reserva (C-07).
- UI, mockups, nuevos endpoints distintos de `GET/POST /api/patients`, teléfono, fecha de nacimiento ni unicidad de email: el email no es `unique` y el DNI sigue siendo la única clave de identidad.
- Cambios de versión de Java, Spring Boot o dependencias; inclusión de una capa de infraestructura nueva.

## Decisions

### Reutilización de la normalización sin duplicarla
En el código leído, `Patient.normalizeDni` y `Patient.normalizeEmail` son `public static` (`Patient.java:38,45`) y el constructor público `(dni, firstName, lastName, email)` ya los aplica (`:31-36`). La descripción del ítem los menciona como privados; se toma como fuente el código efectivamente presente. La regla de diseño es la misma en ambos casos: la entidad es el único punto de verdad.

- Alta: el service pasa el DNI tal como llegó a `new Patient(...)`, que normaliza DNI y email; el service no ejecuta reemplazos ni valida formatos.
- Upsert y búsqueda: para consultar `findByDni` se normaliza la entrada invocando `Patient.normalizeDni(dni)`, el punto único que la entidad expone. Si en otra revisión los métodos fueran privados, se expondría un único método público de normalización en `Patient` y el service delegaría en él, sin mover la regex ni los `replace` a DTOs, service o helpers.
- Actualización: un método de dominio en `Patient` (por ejemplo `updateProfile`) reutiliza internamente `normalizeEmail`; no se agregan setters genéricos.
- DTOs: solo `@NotBlank`/`@Size(max=100)` para nombre y apellido; no se usa `@Pattern` ni `@Email`, porque sería una segunda definición del formato. Un email inválido se detecta en la entidad y se traduce a error de negocio.
- Email: `Patient.normalizeEmail` (`Patient.java:45-50`) aplica `trim()`, `toLowerCase()` y una regex cuyo dominio debe contener al menos un punto. El DTO no normaliza ni define el formato y el service no hace `trim`, `toLowerCase` ni reemplazos: alta y upsert persisten el valor devuelto por `Patient` (por ejemplo `  Paciente.Ficticio@Example.COM  ` → `paciente.ficticio@example.com`).

Alternativa considerada: un helper de normalización compartido nuevo. Se descarta porque crea dos dueños posibles de la misma regla y permitiría que alta y búsqueda divergieran. Tradeoff aceptado: el service queda acoplado a la entidad para normalizar, a cambio de garantizar que el DNI persistido y el buscado usen exactamente la misma transformación.

### Validación de nombre y apellido en el DTO
`nombre` y `apellido` se validan en el DTO de request con `@NotBlank` y `@Size(max=100)`, conforme a `first_name`/`last_name` `varchar(100)` requeridos (`knowledge-base/04_modelo_de_datos.md` §Patient) y RN-RS-05 (`knowledge-base/05_reglas_de_negocio.md`). El `@RestControllerAdvice` traduce los errores de Bean Validation al mismo `ProblemDetail` que los errores de negocio (`400`), sin persistir ni modificar pacientes y sin incluir PII en el mensaje. `Patient` conserva sus columnas `nullable=false` y su constructor sin validación de valor: no se agrega una segunda capa de validación en la entidad ni en el service.

### Upsert por DNI
`@Transactional` en el service. Se busca por DNI normalizado; si no existe se crea y responde `201`; si existe se reutiliza la identidad y se actualizan nombre, apellido y email mediante el método de dominio, respondiendo `200`, sin crear un segundo paciente. El DNI no se modifica: es la clave de identidad. Si alguno de los datos enviados es inválido (nombre o apellido vacíos/solo espacios, o email inválido), se valida antes de cualquier mutación: se responde `400` y el paciente existente conserva sus valores, sin guardado parcial. La restricción `unique=true` de `dni` y el índice existentes son la última defensa; no se agrega una tabla ni constraint nueva, y el email sigue sin ser `unique`. Alternativa descartada: alta idempotente que ignore cambios, porque no permitiría corregir datos del paciente.

### API y errores
`GET /api/patients` recibe `Pageable` (`page`, `size`) y un filtro opcional `dni`; devuelve un `Page` de DTOs con metadatos. `POST /api/patients` recibe un DTO validado con `@Valid` y devuelve un DTO de respuesta. El controller no contiene lógica de negocio; usa inyección por constructor. Un `@RestControllerAdvice` centralizado convierte errores de negocio en `ProblemDetail` con mensajes genéricos; los errores de validación de Bean Validation se mapean al mismo formato. Nunca se loguean DNI ni email.

### Búsqueda por DNI
Se implementa como el filtro `dni` de `GET /api/patients`, no como un endpoint nuevo: mantiene la paginación y reduce la superficie expuesta. Sin coincidencia se devuelve una página vacía con `200`, coherente con un listado filtrado. Alternativa considerada: `GET /api/patients/{dni}` con `404`; se descarta por ampliar los endpoints pedidos. Para normalizar se usa el mismo punto de verdad del alta, de modo que `27.123.456`, `27 123 456` y `27123456` encuentren el mismo registro.

### Autenticación fuera de alcance
Los endpoints se describen como "para el odontólogo" en `CHANGES.md:229`, pero C-03 `dentist-auth` no forma parte de este change. Supuesto registrado: cuando C-03 esté disponible exigirá sesión/rol; aquí no se diseña ni se configura autorización, no se crean tareas de seguridad y no se modifica la configuración de Spring Security. Si la configuración existente protege las rutas, los tests web usarán el soporte de test ya presente (`spring-security-test`) solo como accesorio, sin implementar login.

### Estrategia TDD
Orden por comportamiento: prueba roja → implementación mínima verde → triangulación con al menos dos casos (happy + edge) → refactor con tests verdes. Unitarios con JUnit 5 y Mockito sin contexto Spring; web con `@WebMvcTest` + MockMvc; persistencia con `@DataJpaTest`; integración con `@SpringBootTest` y H2 en modo PostgreSQL. Verificación final con `./gradlew build`. Sin `-DskipTests` ni omisiones.

### UI Mockups
Decision: no (no UI). El change es backend/API y no produce pantallas, componentes ni archivos mockup.

## Risks / Trade-offs

- [Divergencia entre normalización del alta y de la búsqueda] → Un único punto de verdad en `Patient` y tests con `27.123.456` y `27 123 456` en ambos flujos.
- [Email inválido no detectado por Bean Validation] → `Patient.normalizeEmail` aplica `trim`, minúsculas y dominio con al menos un punto; el advice lo convierte en `400` consistente y se documenta que `@Email`/`@Pattern` no se usan para evitar duplicar el formato.
- [Actualización parcial ante datos inválidos] → Se valida antes de mutar el paciente existente y `@Transactional` evita guardados parciales; las pruebas verifican que un `400` deja el registro intacto.
- [Upsert concurrente sobre el mismo DNI] → `@Transactional` y `unique=true` vigente; no se diseña manejo de 409 en este change porque no está en el alcance.
- [Exposición de PII] → Prohibido loguear DNI/email; mensajes de error genéricos y datos ficticios en tests.
- [Diferencias H2/PostgreSQL] → H2 en modo PostgreSQL, DDL ya existente sin cambios y verificación final con el wrapper.

## Migration Plan

No se requiere migración de esquema: la tabla `patients`, su índice y su restricción de unicidad ya existen desde C-02. La implementación agrega service, DTOs, controller y mapper sobre ese modelo. Rollback: revertir los archivos nuevos y el único método de dominio agregado, sin tocar datos ni esquema. `CHANGES.md` se actualiza solo cuando el change esté aplicado y verificado.

## Open Questions

Ninguna dentro del alcance. Supuesto pendiente de C-03: la autenticación de estos endpoints se resolverá en ese change y no altera los requirements ni las tareas de este.
