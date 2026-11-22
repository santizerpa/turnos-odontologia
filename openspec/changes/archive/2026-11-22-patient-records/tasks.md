# Tasks

## 1. Upsert de pacientes por DNI (TDD)

- [x] 1.1 Escribir primero la prueba unitaria roja de `PatientService` con JUnit 5 y Mockito para el alta con DNI `27123456`, nombre `Paciente`, apellido `Ficticio` y email `paciente.ficticio@example.com`; verificar que falla por ausencia del servicio y que la aserción comprueba el DNI normalizado y el id devuelto.
- [x] 1.2 Agregar la triangulación con `27.123.456` y `27 123 456` sobre el mismo DNI existente; verificar que convergen en `27123456`, que `findByDni` recibe el valor normalizado y que no se crea un segundo paciente.
- [x] 1.3 Agregar los casos borde de DNI inválido (`27123A56`, `271234`, `271234567`) y de email inválido (`paciente.ficticio@ejemplo`); verificar que se rechazan sin persistir y como error de negocio.
- [x] 1.4 Escribir primero la prueba roja de upsert sobre el DNI existente `27123456` con nombre, apellido o email distintos; verificar que se actualizan esos campos sobre la misma identidad, que el email se normaliza y que la operación responde `200` sin crear un segundo paciente.
- [x] 1.5 Escribir primero la prueba roja de upsert sobre el DNI existente con email inválido `paciente.ficticio@ejemplo`; verificar que se responde `400`, que no se invoca el guardado y que el paciente existente conserva sus datos sin modificación. La cobertura de nombre o apellido vacíos/solo espacios en el upsert se completa en la tarea 3.4, en el boundary.
- [x] 1.6 Implementar el servicio con inyección por constructor y `@Transactional`, pasando el DNI recibido al constructor de `Patient` y usando `Patient.normalizeDni` solo para la consulta previa; verificar que las pruebas 1.1–1.5 pasan en verde, sin regex ni reemplazos en service, DTO ni helpers.
- [x] 1.7 Implementar el método de dominio en `Patient` que actualiza nombre, apellido y email reutilizando `normalizeEmail`, validando el email antes de asignar cualquier campo y sin setters genéricos; verificar que el upsert con datos corregidos conserva la misma identidad y responde `200`, y que el upsert inválido deja el registro intacto.
- [x] 1.8 Refactorizar con las pruebas en verde y ejecutar `./gradlew test --tests '*PatientService*'` desde `backend/`; verificar que pasan sin omisiones.

## 2. Búsqueda por DNI (TDD)

- [x] 2.1 Escribir primero pruebas rojas de búsqueda con `dni=27.123.456` y `dni=27 123 456`; verificar que encuentran al paciente `27123456` y que `findByDni` recibe el valor normalizado.
- [x] 2.2 Agregar los casos de sin coincidencia (página vacía) y DNI inválido (error `400` sin consultar repositorio); verificar la triangulación happy + edge.
- [x] 2.3 Implementar el filtro `dni` en el servicio de listado apoyándose en `Patient.normalizeDni`; verificar que las pruebas 2.1–2.2 pasan sin agregar un endpoint nuevo.
- [x] 2.4 Refactorizar y ejecutar `./gradlew test --tests '*Patient*'` desde `backend/`; verificar que siguen pasando las pruebas de upsert.

## 3. Capa web y validación del boundary (TDD)

- [x] 3.1 Escribir primero pruebas `@WebMvcTest` + MockMvc para `GET /api/patients?page=0&size=2`; verificar que responden `200` con dos pacientes como máximo y metadatos de página (rojo).
- [x] 3.2 Agregar pruebas rojas de `POST /api/patients`: alta `201`, reutilización `200` y errores `400` por DNI inválido y email inválido; verificar que el cuerpo no expone entidades ni incluye el email o el DNI en mensajes.
- [x] 3.3 Agregar el caso borde rojo de `size` fuera de rango; verificar `400` consistente y que no se ejecuta una consulta sin límite.
- [x] 3.4 Agregar pruebas rojas de nombre y apellido requeridos en el alta y en el upsert: campos ausentes, vacíos, solo espacios y con más de 100 caracteres; verificar `400` consistente, sin persistir ni modificar al paciente existente.
- [x] 3.5 Agregar pruebas rojas del comportamiento del email: `  Paciente.Ficticio@Example.COM  ` se persiste como `paciente.ficticio@example.com`, `paciente.ficticio@consultorio.example.com` se acepta y `paciente.ficticio@ejemplo` se rechaza con `400` sin persistir; verificar que el DTO ni el service normalizan.
- [x] 3.6 Implementar los DTOs de request/response con `@NotBlank` y `@Size(max=100)` en nombre y apellido, y `@Valid`, sin `@Pattern` ni `@Email`; verificar que las pruebas 3.1–3.5 pasan en verde y que el formato del email sigue definido solo en `Patient`.
- [x] 3.7 Implementar el controller sin lógica de negocio, con inyección por constructor y mapeo a DTO; verificar que no se exponen entidades.
- [x] 3.8 Implementar `@RestControllerAdvice` con `ProblemDetail` para errores de negocio y de validación de Bean Validation; verificar que las pruebas 3.1–3.5 pasan y que no se loguean DNI ni email.
- [x] 3.9 Refactorizar con las pruebas en verde y ejecutar `./gradlew test --tests '*PatientController*'` desde `backend/`; verificar que el error `400` mantiene el mismo formato para DNI, nombre, apellido y email.

## 4. Persistencia e integración

- [x] 4.1 Escribir primero pruebas `@DataJpaTest` de `findByDni` con DNI normalizado y de unicidad de `dni`; verificar rojo-verde y que `27.123.456` y `27123456` convergen en una sola fila.
- [x] 4.2 Implementar los ajustes mínimos de repositorio o entidad que las pruebas 4.1 exijan; verificar que pasan sin cambiar el esquema existente.
- [x] 4.3 Escribir y ejecutar una prueba `@SpringBootTest` con H2 en modo PostgreSQL que recorra POST y GET con filtro `dni` usando datos ficticios; verificar que el email se persiste normalizado, que el upsert con datos corregidos conserva la identidad y que un upsert inválido no modifica el registro, sin requerir PostgreSQL local.

## 5. Verificación final

- [x] 5.1 Ejecutar `./gradlew test` desde `backend/`; verificar que toda la suite pasa sin `-DskipTests`, sin omisiones y sin datos reales de pacientes.
- [x] 5.2 Ejecutar `./gradlew build` desde `backend/`; verificar que compila y empaqueta con Java 17 y Spring Boot 3.3.13 sin cambios de versión.
- [x] 5.3 Revisar el diff; verificar que no hay regex ni reemplazos de DNI o email fuera de `Patient`, que no se loguea PII, que no se agregaron teléfono, fecha de nacimiento ni unicidad de email, y que no se crearon requirements, specs ni tareas de C-03, C-05 o C-07.

## Workflow follow-up

- Ejecutar `openspec status --change patient-records --json` y `openspec validate patient-records --strict` hasta obtener resultado válido.
- Mantener todas las tareas en `[ ]` hasta que el change sea aplicado y verificado por una solicitud posterior.
- Actualizar `CHANGES.md` solo al cerrar y verificar el change; no hacer commits sin solicitud explícita.
