# Design

## Context

C-01 dejó Spring Boot 3.3.13, Java release 17, Gradle Wrapper, perfil `test` con H2 PostgreSQL compatibility mode y perfil `local` con PostgreSQL. C-02 añade el modelo persistente compartido; C-03+ implementarán autenticación y casos de uso sobre él. Véase proposal.md y las specs relacionadas.

## Goals / Non-Goals

**Goals:**
- Entidades JPA, enums, relaciones, repositorios y esquema inicial coherentes con el ERD.
- Invariantes persistentes suficientes para concurrencia y soft delete.
- Validaciones y seed seguros y reproducibles.
- Base demostrable con H2, sin que PostgreSQL sea requisito de test.

**Non-Goals:**
- Controladores REST, login, reservas públicas o cálculo de slots.
- Confirmación/cancelación idempotente como servicios completos; C-05/C-09 los implementarán usando este modelo.
- Generación o envío de mensajes; solo persistencia de OutboxMessage.
- Cálculo de disponibilidad desde ScheduleBlock; C-04 conservará solo el dato de bloqueo.

## Decisions

### Modelo con UUID y relaciones JPA
Se usarán UUID generados por la aplicación/JPA para todas las claves primarias, manteniendo el requisito portable. Para todas las columnas foreign key de Appointment, ScheduleBlock y demás relaciones se configurará `@JoinColumn` explícito y se crearán índices compatibles con PostgreSQL y H2. Se conservará el paquete base `com.turnosodontologia` y paquetes por dominio definidos en C-01.

Alternativa considerada: BIGINT identity. Se descarta porque la KB exige UUID para entidades opacas y C-01 no dejó evidencia que justifique una clave distinta.

### Auditoría y soft delete
Las entidades requeridas tendrán columnas físicas `created_at`, `updated_at` y, cuando corresponda, `deleted_at`. Se usarán timestamps locales UTC-3 de forma consistente con el supuesto horario argentino; una entidad no visible con soft delete podrá conservar su historial. No se añadirá una interfaz genérica que revele borrado físico.

Alternativa considerada: borrado físico. Se descarta por DD-05 y el objetivo de conservar registros y auditoría.

### Appointment.active_slot como defensa persistente
`active_slot` será nullable y único. Al crear una entidad activa, el servicio de creación futuro deberá asignar el mismo valor que `start_at`; al cancelar deberá poner NULL. La columna tendrá una restricción única portable (`UNIQUE (active_slot)`) y `active_slot` no tendrá setter público ni endpoint/repository operation que permita mantenimiento fuera de los métodos de dominio.

Se normaliza el concepto de “activo” con estados PENDING, CONFIRMED, ABSENT, ATTENDED. CANCELLED siempre usa NULL. En H2 PostgreSQL compatibility mode y PostgreSQL, múltiples NULL no violan la unicidad. El servicio necesitará transacción y optimistic locking en C-05 para convertir violaciones concurrentes en resultado controlado.

Alternativa considerada: tablas por slot o índice parcial PostgreSQL. Se descarta porque una restricción nullable única es portable a ambos motores y el índice parcial puede diferir en H2.

### Estados y enums
Se persistirán enums como strings (`EnumType.STRING`) para legibilidad y estabilidad: AppointmentStatus, CreatedBy, Actor, WaitingListStatus y OutboxStatus. Ningún cambio implícito de ordinal podrá afectar datos existentes.

### AppointmentToken
Se almacenará únicamente `token_hash`, con longitud suficiente para SHA-256. No existirá `plain_token`. `expires_at` se calcula en C-07/C-09 con el límite definido por el sistema. Confirmar no cambia ni consume el token; una segunda confirmación o cancelación queda gobernada por estados y por C-05/C-09.

Alternativa considerada: JWT o dos tokens. Se descarta por DD-06 y porque un único hash opaco habilita confirmar y cancelar durante su ventana.

### Dentist, seed y soft delete
El seed administrativo buscará/creará el Dentist desde `APP_ADMIN_EMAIL`. `APP_ADMIN_PASSWORD` se transforma con BCrypt antes de persistir. `password_hash` no admite texto plano. Los datos seed son ficticios y el seed solo opera según perfil/configuración definida; no se almacena ninguna credencial real.

Se considera que el Dentist inicial tiene la relación administrativa de la aplicación sin introducir otra entidad de rol en C-02.

### Validación de datos
`Patient.dni` se normaliza quitando puntos y espacios, se rechazan letras y caracteres no numéricos y solo se admiten 7-8 dígitos. Se define un único CHECK portable o validación JPA/Bean Validation que preserve esa norma. `Patient.email` se valida con formato estándar y se almacena normalizado según decisión de C-06. El valor normalizado es la clave de unicidad de DNI.

Alternativa considerada: permitir formatos de DNI arbitrarios. Se descarta por 04_modelo_de_datos y RN-RS-05/RN-RS-06.

### Esquema y paridad H2/PostgreSQL
Se generará el esquema inicial mediante DDL controlado o migraciones compatibles: tipos JPA estándar, varchar definido, timestamp local documentado, UUID, enums como varchar, claves foráneas e índices. Se evitarán sintaxis específicas y funciones fuera de SQL portable. Las pruebas usarán H2 PostgreSQL compatibility mode y harán una verificación documentada de las restricciones equivalentes en PostgreSQL local.

### Estrategia TDD
Cada repositorio y mapeo tendrá una prueba roja antes del mínimo verde. El orden será: prueba de contexto/mapeo, repositories y validaciones, restricción UNIQUE, cancelación/rereservación, ScheduleBlock y paridad H2. Los casos posteriores solo se añadirán después de pasar el mínimo previo.

## Risks / Trade-offs

- [H2 no descubre todas las diferencias de PostgreSQL] → Mantener DDL portable, `@DataJpaTest`, y una prueba/manual PostgreSQL para un esquema local.
- [Escritura directa accidental de active_slot] → Definir campo no expuesto públicamente y pruebas de arquitectura/diseño; C-05 deberá mantener el único servicio autorizado.
- [Soft delete puede acumular datos] → Registrar deleted_at sin política de retención; C-14 revisará hardening posterior.
- [Variables administrativas ausentes] → El seed fallará explícitamente y no realizará fallback con secretos de ejemplo.
- [El seed puede contener datos personales] → Todos los valores serán ficticios y los tests deben usar datos sintéticos únicamente.

## Migration Plan

C-02 es un cambio inicial sobre el repositorio de C-01; no existe esquema de dominio previo que conservar. Se crea el esquema en ambos perfiles, se ejecutan migraciones/creación y se valida el seed. Rollback: eliminar únicamente las tablas nuevas del esquema de prueba o la base local; no existe dato productivo previo. No se modifica CHANGES.md hasta verificar el change completo.

## Open Questions

Ninguna: el alcance y las reglas materiales están resueltos por CHANGES.md y la KB vigente.
