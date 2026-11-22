# semilla-inicial Specification

## Purpose

Definir la creación segura y reproducible del odontólogo administrador inicial y los datos ficticios necesarios para la demo.

## Requirements

### Requirement: El Dentist inicial debe tomar credenciales de variables de entorno
El sistema SHALL crear o reutilizar el Dentist administrador usando APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD sin incluir credenciales reales en el repositorio.

#### Scenario: Las variables administrativas están presentes
- **WHEN** arranca el perfil configurado para ejecutar el seed
- **THEN** el email proviene de APP_ADMIN_EMAIL
- **AND** password_hash contiene el BCrypt de APP_ADMIN_PASSWORD

#### Scenario: Faltan variables administrativas
- **WHEN** se solicita ejecutar el seed sin una variable obligatoria
- **THEN** el seed falla de forma explícita
- **AND** no se crea un Dentist con contraseña plana ni credenciales predeterminadas reales

### Requirement: El seed debe contener únicamente datos ficticios
Los datos iniciales SHALL usar nombres, DNIs, emails y horarios ficticios claramente no atribuibles a pacientes reales.

#### Scenario: Se ejecuta el seed en demo
- **WHEN** se crean pacientes, turnos, lista de espera, bloqueos y outbox
- **THEN** todos los datos son ficticios
- **AND** no se persisten datos personales reales

### Requirement: El esquema inicial debe ser reproducible en perfiles compatibles
El modelo SHALL disponer de un esquema inicial que aplique las entidades y restricciones sobre PostgreSQL local y H2 PostgreSQL compatibility mode.

#### Scenario: Se ejecutan las migraciones en H2
- **WHEN** arranca una base de pruebas en H2 modo PostgreSQL
- **THEN** el esquema crea claves, índices y restricciones sin requerir PostgreSQL instalado

#### Scenario: Se ejecutan las migraciones en PostgreSQL
- **WHEN** arranca el perfil local con PostgreSQL
- **THEN** el esquema refleja las mismas entidades y restricciones verificadas en H2
