# modelo-persistente Specification

## Purpose

Definir el modelo persistente JPA compartido por los módulos de turno, paciente, agenda y mensajería, con UUID, relaciones, auditoría y soft delete consistentes.

## Requirements

### Requirement: El modelo debe persistir todas las entidades JPA de C-02

El sistema SHALL proveer entidades persistentes para Dentist, Patient, Appointment, AppointmentToken, AppointmentEvent, WaitingListEntry, OutboxMessage, ScheduleBlock y ScheduleConfiguration, con sus claves primarias UUID.

#### Scenario: Se inspecciona el esquema generado

- **WHEN** Hibernate genera o valida el esquema inicial
- **THEN** existen las nueve entidades y sus columnas, relaciones y claves primarias
- **AND** no se usa una clave primaria distinta de UUID sin una evidencia de diseño explícita

### Requirement: Las relaciones entre entidades deben ser consistentes

El modelo SHALL representar la relación Dentist 1:* Appointment, la relación Dentist 1:1 ScheduleConfiguration y la demás cardinalidad definida por el ERD.

#### Scenario: Se persiste un turno asociado

- **WHEN** se crea un Appointment con un Patient y un Dentist existentes
- **THEN** se persisten las dos claves foráneas y las asociaciones JPA son navegables según el ERD

#### Scenario: Se persiste el token de un turno

- **WHEN** se registra el token asociado a un Appointment
- **THEN** existe a lo sumo un AppointmentToken para ese turno

#### Scenario: Se persiste la configuración de horario de un odontólogo

- **WHEN** se crea una ScheduleConfiguration para un Dentist existente
- **THEN** la clave foránea `dentist_id` queda informada y la configuración es navegable desde el odontólogo
- **AND** un segundo ScheduleConfiguration para el mismo Dentist es rechazado por la restricción única

### Requirement: Los registros deben conservar convenciones de auditoría y soft delete
Las entidades con created_at, updated_at y deleted_at SHALL aplicar columnas de auditoría; deleted_at SHALL representar baja lógica sin borrado físico automático.

#### Scenario: Se marca una entidad soft-deletable
- **WHEN** una entidad compatible recibe una baja lógica
- **THEN** deleted_at queda informado
- **AND** created_at y updated_at conservan su semántica de auditoría

### Requirement: Dentist debe identificar al odontólogo administrador
El modelo SHALL admitir que el Dentist inicial tenga relación de administrador o sea la identidad administrativa única gestionada por la configuración.

#### Scenario: Se inicia el seed administrativo
- **WHEN** se crea el Dentist inicial
- **THEN** queda disponible como la identidad administrativa de la aplicación

### Requirement: Debe existir un repositorio Spring Data por entidad

El sistema SHALL definir un repositorio Spring Data para Dentist, Patient, Appointment, AppointmentToken, AppointmentEvent, WaitingListEntry, OutboxMessage, ScheduleBlock y ScheduleConfiguration.

#### Scenario: Se carga el contexto con los repositorios

- **WHEN** Spring crea los componentes de persistencia
- **THEN** los nueve repositorios quedan disponibles sin requerir PostgreSQL en las pruebas

### Requirement: La configuración de horario debe persistirse por odontólogo

ScheduleConfiguration SHALL persistir una fila por odontólogo con `dentist_id` único y una colección de franjas semanales con día, hora de inicio y hora de fin, usando tipos portables entre H2 en modo PostgreSQL y PostgreSQL.

#### Scenario: Se persisten y recuperan las franjas semanales

- **WHEN** se guarda una configuración con dos franjas del mismo día
- **THEN** las filas de franjas quedan asociadas a la configuración
- **AND** al recuperarla, las franjas se ordenan de forma determinista por día y hora de inicio

#### Scenario: La colección de franjas no requiere una entidad independiente

- **WHEN** se inspecciona el mapeo de la configuración
- **THEN** las franjas se persisten como colección de valores dependiente de la configuración
- **AND** no existe una clave primaria propia para cada franja

#### Scenario: El esquema es portable entre H2 y PostgreSQL

- **WHEN** el perfil `test` crea el esquema y el perfil `local` lo valida
- **THEN** ambas bases aceptan la tabla de configuración y su tabla de franjas
- **AND** no se usan funciones ni tipos específicos de un solo motor

#### Scenario: La configuración admite baja lógica sin borrado físico

- **WHEN** una configuración recibe `deleted_at`
- **THEN** el registro y sus franjas se conservan
- **AND** `created_at` y `updated_at` mantienen su semántica de auditoría
