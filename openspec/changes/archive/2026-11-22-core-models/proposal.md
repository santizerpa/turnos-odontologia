# Proposal

## Why

C-02 debe convertir el modelo de dominio aprobado en una base persistente verificable antes de implementar autenticación, agenda y reservas. Sin entidades JPA, restricciones portables y repositorios, los changes posteriores duplicarían reglas y perderían garantías de concurrencia y auditoría.

## What Changes

- Incorporar entidades JPA y enums para odontólogos, pacientes, turnos, tokens, eventos, lista de espera, mensajería, bloqueos y su persistencia.
- Definir claves UUID, relaciones, campos de auditoría/soft delete y repositorios Spring Data.
- Crear el esquema inicial, el `Dentist` administrador desde `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD` con contraseña BCrypt ficticia, y datos seed ficticios.
- Garantizar que `Appointment.active_slot` sea una única reserva activa por franja y que solo el servicio de dominio pueda mantener su valor al crear/cancelar/rereservar.
- Validar DNI normalizado y email; persistir solo el hash SHA-256 de `AppointmentToken` y conservarlo al confirmar.
- Agregar pruebas JPA, restricciones H2/PostgreSQL, cancelación/rereservación, bloqueos y paridad portable.

## Capabilities

### New Capabilities
- `modelo-persistente`: Mapeo JPA completo, claves UUID, relaciones, auditoría, soft delete y repositorios del dominio.
- `integridad-turnos`: Restricciones y reglas de `active_slot`, estados, tokens hash y comportamiento cancelado/rereservable.
- `semilla-inicial`: Creación portable del odontólogo administrador y datos seed ficticios desde configuración.
- `validacion-datos`: Normalización y validación de DNI y formato de email en el modelo.

### Modified Capabilities
- Ninguna.

## Impact

- Nuevos paquetes de dominio bajo `com.turnosodontologia`, entidades JPA, enums y repositorios.
- Nueva migración/esquema inicial compatible con PostgreSQL y H2 en modo PostgreSQL.
- Nuevas dependencias de configuración y seed para `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`.
- Ningún endpoint REST, UI ni implementación de cálculo de slots se incorpora en este change.
