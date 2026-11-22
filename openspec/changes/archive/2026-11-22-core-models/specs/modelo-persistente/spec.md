# Spec Delta

## Purpose

Definir el modelo persistente JPA compartido por los módulos de turno, paciente, agenda y mensajería, con UUID, relaciones, auditoría y soft delete consistentes.

## ADDED Requirements

### Requirement: El modelo debe persistir todas las entidades JPA de C-02
El sistema SHALL proveer entidades persistentes para Dentist, Patient, Appointment, AppointmentToken, AppointmentEvent, WaitingListEntry, OutboxMessage y ScheduleBlock, con sus claves primarias UUID.

#### Scenario: Se inspecciona el esquema generado
- **WHEN** Hibernate genera o valida el esquema inicial
- **THEN** existen las ocho entidades y sus columnas, relaciones y claves primarias
- **AND** no se usa una clave primaria distinta de UUID sin una evidencia de diseño explícita

### Requirement: Las relaciones entre entidades deben ser consistentes
El modelo SHALL representar la relación Dentist 1:* Appointment y la demás cardinalidad definida por el ERD.

#### Scenario: Se persiste un turno asociado
- **WHEN** se crea un Appointment con un Patient y un Dentist existentes
- **THEN** se persisten las dos claves foráneas y las asociaciones JPA son navegables según el ERD

#### Scenario: Se persiste el token de un turno
- **WHEN** se registra el token asociado a un Appointment
- **THEN** existe a lo sumo un AppointmentToken para ese turno

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
El sistema SHALL definir un repositorio Spring Data para Dentist, Patient, Appointment, AppointmentToken, AppointmentEvent, WaitingListEntry, OutboxMessage y ScheduleBlock.

#### Scenario: Se carga el contexto con los repositorios
- **WHEN** Spring crea los componentes de persistencia
- **THEN** los ocho repositorios quedan disponibles sin requerir PostgreSQL en las pruebas
