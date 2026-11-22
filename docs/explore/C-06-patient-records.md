# Relevamiento — C-06 `patient-records`

> **Naturaleza del documento:** exploración de solo lectura y análisis. No contiene propuestas de diseño, decisiones de implementación ni código nuevo.
> **Fecha:** 2026-11-22 · **Rama:** `feat/vale`
> **Estado:** C-06 pendiente en el roadmap; no existe change OpenSpec activo asociado.

---

## 1. Resumen ejecutivo

- C-06 figura **pendiente** (`[ ]`) en `CHANGES.md:224`, con governance **MEDIO**, dependencia única **C-02** y tres lecturas obligatorias: KB §Patient, RN-RS-05/06 y KB §Integridad de datos (`CHANGES.md:232-237`).
- No integra el camino crítico de 9 changes (`CHANGES.md:92-96`), pero es prerrequisito de **C-07** (`CHANGES.md:37`, `:74`) y del **GATE 7** que habilita C-12/C-13 (`CHANGES.md:84-86`). En GATE 2 se ejecuta en paralelo, asignado a **Persona E** (`CHANGES.md:65-68`, `:105`).
- La entidad `Patient` y `PatientRepository` **ya existen** en código, con DNI normalizado y único, formato de email y campos de auditoría heredados. En cambio, los ítems propios del scope de C-06 (upsert, endpoints, búsqueda, tests) no tienen archivos en el paquete `patient`.
- La spec main vigente `validacion-datos` cubre normalización/unicidad de DNI y formato de email; no menciona nombre, apellido, teléfono, fecha de nacimiento ni unicidad de email.
- `openspec list --json` devuelve `{"changes":[]}`. Solo hay dos changes archivados (`2026-11-22-foundation-setup`, `2026-11-22-core-models`); C-06 existe únicamente como ítem de `CHANGES.md`.

---

## 2. Ficha del change en el roadmap

Fuente: `CHANGES.md:223-237`.

| Atributo | Valor |
|---|---|
| Identificador | C-06 `patient-records` |
| Estado | `[ ]` pendiente (`:224`) |
| Scope | "Alta y reutilización de pacientes" (`:225`) |
| Normalización de DNI | Quitar puntos y espacios; solo 7 u 8 dígitos; rechazar letras (`:226`) |
| Email | Validación con formato estándar (`:227`) |
| Upsert | `Patient` por DNI (`:228`) |
| Endpoints | `GET /api/patients` y `POST /api/patients` para el odontólogo (`:229`) |
| Búsqueda | Por DNI (`:230`) |
| Tests | DNI válido e inválido, email inválido, reutilización por DNI (`:231`) |
| Dependencias | C-02 (`:232`) |
| Governance | MEDIO (`:233`) |
| Leer antes | `knowledge-base/04_modelo_de_datos.md §Patient`, `knowledge-base/05_reglas_de_negocio.md §RN-RS-05 / §RN-RS-06`, `knowledge-base/10_preguntas_abiertas.md §Integridad de datos` (`:234-237`) |

Contexto de planificación:

- Árbol de dependencias: `C-02 core-models → C-06 patient-records → C-07 public-booking-api → C-08 public-booking-ui` (`CHANGES.md:31-38`).
- C-06 también forma parte del conjunto `C-03 + C-05 + C-06 + C-10 + C-11` del que dependen C-12 y C-13 (`CHANGES.md:46-48`).
- GATE 2: con `C-02 ✓` se habilitan C-03, C-04 y C-06 en paralelo (`CHANGES.md:65-68`).
- GATE 4: `C-07 ... requiere C-06 ✓` (`CHANGES.md:73-75`).
- GATE 7: exige `C-05 + C-06 + C-10 + C-11 ✓` (`CHANGES.md:84-86`).
- Fase 2 "Agenda y turnos", junto a C-04 y C-05 (`CHANGES.md:408`).
- Camino crítico textual (sin C-06): `C-01 → C-02 → C-03 → C-05 → C-09 → C-10 → C-11 → C-12 → C-14` (`CHANGES.md:92-96`).

---

## 3. Hallazgos por fuente

### 3.1 `CHANGES.md`

Índice canónico de 14 changes atómicos; exige leer el archivo antes de iniciar un ítem (`:1-5`). Entregable objetivo 2026-11-24 (`:21-23`). El resto de la ficha C-06 está en la sección 2.

### 3.2 `knowledge-base/04_modelo_de_datos.md` — §Patient

- Dominio: "Pacientes: datos mínimos y duplicados por DNI" (`:6`).
- Relaciones en el ERD: `Patient 1 ─── * Appointment` (`:17`) y `Patient 1 ─── * WaitingListEntry` (`:20`).
- §Patient (`:36-42`):
  - `id: UUID`, clave primaria (`:37`).
  - `dni: varchar(8)`, único; solo 7 u 8 dígitos; puntos y espacios se eliminan; letras se rechazan (`:38`).
  - `first_name: varchar(100)`, requerido (`:39`).
  - `last_name: varchar(100)`, requerido (`:40`).
  - `email: varchar(255)`, requerido y validado (`:41`).
  - `created_at`, `updated_at`, `deleted_at` (`:42`).
- Seed inicial: "5-10 pacientes ficticios" (`:100`), dentro del bloque de datos ficticios (`:97-105`). La sección no detalla valores de campo.
- No figuran teléfono ni fecha de nacimiento en la entidad.

### 3.3 `knowledge-base/05_reglas_de_negocio.md` — RN-RS-05 / RN-RS-06

- RN-RS-05 (`:9`): "paciente necesita nombre, apellido, DNI y email válido; el formato de email se valida estándar, sin dominio restringido."
- RN-RS-06 (`:10`): "DNI solo acepta 7 u 8 dígitos; se eliminan puntos y espacios antes de guardar y se rechazan letras."
- Contexto del dominio Datos: no se usa borrado físico para turnos ni pacientes en v1 (`:43`); los datos seed son ficticios (`:44`).
- Excepciones globales relevantes: errores concurrentes sin duplicados (`:49`), cambios de estado generan evento de auditoría mínimo (`:50`), validaciones inválidas devuelven errores consistentes sin exponer detalles internos (`:51`).
- Las reglas no mencionan teléfono, fecha de nacimiento ni unicidad de email.

### 3.4 `knowledge-base/10_preguntas_abiertas.md` — §Integridad de datos

Sección `:11-16`, en forma declarativa:

- Email: solo formato estándar; sin restricción de dominio (`:13`).
- DNI: solo números de 7 u 8 dígitos; se eliminan puntos y espacios; letras y otros caracteres se rechazan (`:14`).
- Cancelación desde dos pestañas: la segunda solicitud ve `CANCELLED`, responde "ya cancelado" y no repite efectos (`:15`).
- No se hace anonimización ni borrado en v1; la demo usa solo datos ficticios (`:16`).

Otros contextos: "Anonimización y política de retención" figura en alcance posterior (`:37`); los pacientes se identifican con datos mínimos y el token de su turno/aviso, sin login propio (`:44`). El §Integridad de datos **no registra decisión pendiente** sobre Patient.

### 3.5 `backend/src/main/java/com/turnosodontologia/patient/Patient.java`

- `@Entity` (`:8`) y `@Table(name = "patients", indexes = @Index(name = "idx_patient_dni", columnList = "dni"))` (`:9`); extiende `AuditableEntity` (`:10`).
- Patrones estáticos: `DNI = \d{7,8}` (`:11`) y `EMAIL` con local parte permitida y dominio con al menos un punto (`:12`).
- `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id` (`:14-15`).
- `dni`: `@Column(nullable = false, unique = true, length = 8)` (`:17-18`).
- `firstName`: `@Column(name = "first_name", nullable = false, length = 100)` (`:20-21`); `lastName`: ídem con `last_name` (`:23-24`).
- `email`: `@Column(nullable = false, length = 255)` (`:26-27`), **sin `unique`**.
- Constructor protegido (`:29`) y constructor público `(dni, firstName, lastName, email)` que aplica `normalizeDni` y `normalizeEmail` (`:31-36`).
- `normalizeDni` (`:38-43`): `null → IllegalArgumentException("DNI requerido")`; reemplaza `"."` y `" "`; valida con regex y lanza `IllegalArgumentException("DNI inválido")`.
- `normalizeEmail` (`:45-50`): `null → IllegalArgumentException("Email requerido")`; aplica `trim().toLowerCase()`; valida con regex y lanza `IllegalArgumentException("Email inválido")`.
- Solo getters (`:52-56`). No hay setters, Bean Validation, teléfono, fecha de nacimiento ni métodos de upsert.

### 3.6 `backend/src/main/java/com/turnosodontologia/patient/PatientRepository.java`

- `public interface PatientRepository extends JpaRepository<Patient, UUID>` (`:5`).
- Único método declarado: `Optional<Patient> findByDni(String dni)` (`:6`).
- No hay `@Query`, `existsByDni`, bloqueos, `@Transactional` ni operación de upsert. La normalización no ocurre en el repositorio: está en `Patient.normalizeDni` (`Patient.java:38-43`).

### 3.7 `openspec/specs/validacion-datos/spec.md`

- Purpose: "Definir validaciones portables y deterministas para identidad mínima de pacientes, evitando duplicados por representaciones diferentes de DNI" (`:3-5`).
- Requirement 1 — normalizar DNI antes de validar o persistir: eliminar puntos y espacios, rechazar letras y otros caracteres no numéricos, conservar 7 u 8 dígitos (`:9-10`). Escenarios: `27.123.456` y `27 123 456 → 27123456` (`:12-14`); letras/símbolos rechazados (`:16-18`); longitud <7 o >8 rechazada (`:20-22`).
- Requirement 2 — DNI único tras normalización: el modelo almacena el DNI normalizado y aplica unicidad sobre su representación persistida (`:24-25`). Escenario: dos representaciones equivalentes → una sola identidad persistente (`:27-29`).
- Requirement 3 — email con formato estándar antes de persistirse (`:31-32`). Escenarios: local `@` dominio válidos → puede persistirse (`:34-36`); falta `@`, falta dominio o formato inválido → se rechaza (`:38-40`).
- La spec no menciona nombre, apellido, teléfono, fecha de nacimiento, unicidad de email ni normalización case/trim del email.

**Contexto adicional:** `backend/src/main/java/com/turnosodontologia/common/AuditableEntity.java` — `@MappedSuperclass` (`:10`), `created_at` y `updated_at` `nullable=false`, `deleted_at` nullable (`:12-19`), `@PrePersist`/`@PreUpdate` (`:21-31`), `markDeleted(Instant)` e `isDeleted()` (`:33-37`).

---

## 4. Cuadro comparativo Patient

| Concepto | KB (`04`) | Reglas RN-RS-05/06 | Código actual | Spec `validacion-datos` | Estado |
|---|---|---|---|---|---|
| `id` | `UUID`, clave primaria (`:37`) | Sin mención | `@Id @GeneratedValue(UUID)` (`Patient.java:14-15`) | Sin mención | Coherente |
| `dni` — tipo/tamaño | `varchar(8)` (`:38`) | 7 u 8 dígitos (`:10`) | `String`, `@Column(length = 8)` (`:17-18`), regex `\d{7,8}` (`:11`) | 7 u 8 dígitos (`:10`, `:20-22`) | Coherente |
| `dni` — normalización | Puntos y espacios se eliminan (`:38`) | Antes de guardar (`:10`) | `replace(".","").replace(" ","")` en constructor y método (`:32`, `:38-43`) | Eliminar puntos y espacios (`:10`, `:12-14`) | Coherente en los caracteres nombrados; solo espacio U+0020 |
| `dni` — rechazo de letras | Letras se rechazan (`:38`) | Letras se rechazan (`:10`) | `matches()` → excepción (`:41`) | No numéricos rechazados (`:10`, `:16-18`) | Coherente |
| `dni` — unicidad | Único (`:38`) | Sin mención directa | `unique = true` (`:17`); `findByDni` (Repository `:6`) | Unicidad sobre representación persistida (`:24-29`) | Coherente |
| `dni` — índice | Sin mención | Sin mención | `@Index("idx_patient_dni")` (`:9`) | Sin mención | Solo en código |
| `first_name` / `last_name` | Requeridos, `varchar(100)` (`:39-40`) | Requeridos (`:9`) | `nullable = false, length = 100` (`:20-24`); constructor sin validar valor (`:33-34`) | Sin mención | Requerimiento en KB/reglas; sin regla en spec ni validación de valor |
| `email` — requerido y formato | Requerido y validado, `varchar(255)` (`:41`) | Válido, formato estándar, sin dominio restringido (`:9`) | `nullable = false, length = 255` (`:26-27`), regex con dominio con punto (`:12`, `:45-50`) | Formato estándar (`:31-40`) | Coherente en lo documentado; punto en dominio y `trim().toLowerCase()` no explicitados |
| `email` — unicidad | Sin mención | Sin mención | Sin `unique` (`:26`) | Sin mención | No documentado |
| Teléfono | No figura | No figura | No existe | No figura | No documentado |
| Fecha de nacimiento | No figura | No figura | No existe | No figura | No documentado |
| `created_at` / `updated_at` / `deleted_at` | Presentes (`:42`) | Sin borrado físico en v1 (`:43`) | Heredados de `AuditableEntity` (`Patient.java:10`; `AuditableEntity.java:12-19`, `:33-37`) | Sin mención | Coherente |
| Upsert por DNI / búsqueda | Sin mención | Sin mención | Solo `findByDni` (Repository `:6`) | Sin mecanismo; solo el resultado (`:29`) | Ítem del scope (`CHANGES.md:228`, `:230`) sin método en el repositorio |
| `GET/POST /api/patients` | Sin mención | Sin mención | Sin controlador en el paquete `patient` | Sin mención | Ítem del scope (`CHANGES.md:229`) sin archivo correspondiente |
| Validación de entrada | Sin mención | Errores consistentes, sin detalles internos (`:51`) | `IllegalArgumentException` (`:39`, `:41`, `:46`, `:48`); sin Bean Validation | Rechazo del dato inválido (`:18`, `:22`, `:40`), sin prescribir mecanismo | Mecanismo en código, sin equivalente normativo |
| Seed ficticio | 5-10 pacientes ficticios (`:100`) | Datos seed ficticios (`:44`) | Fuera del alcance de este relevamiento | Sin mención | Cantidad documentada; valores no detallados |

---

## 5. Observaciones de coherencia

Hechos encontrados, descriptos sin proponer soluciones:

1. `knowledge-base/04_modelo_de_datos.md:6` describe "Pacientes: datos mínimos y **duplicados** por DNI", mientras que `:38`, RN-RS-06 (`05:10`) y la spec (`:24-29`) exigen DNI único y una sola identidad persistente por DNI normalizado.
2. C-06 figura pendiente (`CHANGES.md:224`), pero `Patient` y `PatientRepository` ya existen y la spec main `validacion-datos` ya está presente. Bajo `openspec/changes/` solo hay la carpeta `archive/` con dos entradas; no hay changes activos.
3. El paquete `patient` contiene únicamente `Patient.java` y `PatientRepository.java`. No hay service, controlador ni DTO, aunque el scope incluye upsert, dos endpoints y búsqueda por DNI (`CHANGES.md:228-230`).
4. KB, reglas, spec y roadmap dicen "puntos y espacios" (`04:38`, `05:10`, `spec:10`, `CHANGES.md:226`); la implementación reemplaza solo `"."` y `" "` (`Patient.java:40`), sin otra clase de whitespace.
5. El regex de `Patient.java:12` exige un punto en el dominio y `normalizeEmail` aplica `trim().toLowerCase()` (`:47`). Ninguno de los dos comportamientos está explicitado en KB, reglas ni spec, que coinciden en "formato estándar, sin dominio restringido" (`05:9`, `10:13`, `spec:31-32`).
6. Nombre y apellido son requeridos en KB (`04:39-40`) y RN-RS-05 (`05:9`), y están como `nullable = false` en código (`Patient.java:20-24`), pero el constructor los asigna sin validar nulidad o vacío (`:33-34`). La spec `validacion-datos` no los cubre.
7. Teléfono y fecha de nacimiento no aparecen en KB §Patient, en RN-RS-05/06, en la spec ni en la entidad.
8. El §Integridad de datos (`10:11-16`) está redactado en forma declarativa y no registra decisión pendiente sobre Patient. Anonimización/retención figura en §Alcance posterior (`10:37`) y su no aplicación en v1 en `10:16`.
9. C-06 no integra el camino crítico (`CHANGES.md:92-96`), pero es requisito de C-07 (`CHANGES.md:37`, `:74`) y del GATE 7 (`CHANGES.md:84`), y está asignado en paralelo a Persona E en GATE 2 (`CHANGES.md:65-68`, `:105`).

---

## 6. Ejemplos ficticios

> **Todos los valores de esta sección son ficticios.** No corresponden a ningún paciente real. Se usan exclusivamente para ilustrar las reglas relevadas.

Normalización de DNI (según `spec.md:12-14` y `Patient.java:38-43`):

| Entrada ficticia | Resultado esperado |
|---|---|
| `27.123.456` | `27123456` |
| `27 123 456` | `27123456` |
| `2712345` | válido (7 dígitos, dentro del rango permitido) |
| `271234` | rechazado (6 dígitos, fuera de 7-8) |
| `271234567` | rechazado (9 dígitos, fuera de 7-8) |
| `27123A56` | rechazado (contiene letra) |

Correo electrónico ficticio:

| Entrada ficticia | Resultado según reglas relevadas |
|---|---|
| `paciente.ficticio@example.com` | formato estándar válido; `example.com` es dominio reservado para ejemplos |
| `paciente.ficticio@ejemplo` | rechazado por el regex vigente (dominio sin punto) |
| `paciente.ficticio` | rechazado (falta `@`) |
| `paciente.ficticio@` | rechazado (falta dominio) |

---

## 7. Estado y verificación

- No se creó ni modificó código fuente, configuración ni artefactos OpenSpec durante el relevamiento.
- Fuentes leídas: `CHANGES.md`, `knowledge-base/04_modelo_de_datos.md`, `knowledge-base/05_reglas_de_negocio.md`, `knowledge-base/10_preguntas_abiertas.md`, `backend/src/main/java/com/turnosodontologia/patient/Patient.java`, `backend/src/main/java/com/turnosodontologia/patient/PatientRepository.java`, `openspec/specs/validacion-datos/spec.md`.
- Contexto adicional leído: `.atl/skill-registry.md`, `backend/src/main/java/com/turnosodontologia/common/AuditableEntity.java`.
- Memoria persistida en Engram: observación `opsx/C-06-patient-records/explore` y resumen de sesión del proyecto `turnos-odontologia`.
- Único archivo incorporado al repositorio como resultado de esta tarea: este documento.
