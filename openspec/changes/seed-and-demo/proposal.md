# Proposal

## Why

El seed actual crea únicamente al odontólogo inicial y no deja la aplicación lista para recorrer una demo completa de agenda, turnos, lista de espera y bandeja. Se necesita un conjunto ficticio reproducible, relativo a la fecha de ejecución e idempotente, que permita levantar y revisar la demo local sin preparar registros a mano ni introducir datos reales.

## What Changes

- Extender el seed existente con una carga de demo explícita y segura, reutilizando las credenciales del odontólogo tomadas de `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`; no incluir credenciales reales ni valores secretos en el repositorio.
- Crear de forma determinista 5–10 pacientes ficticios, turnos en los estados requeridos, tres entradas de lista de espera, un bloqueo de agenda de ejemplo y mensajes visibles en la bandeja de salida.
- Calcular las fechas de los datos de demo relativas al reloj de agenda y a la zona horaria configurada, para que el escenario siga siendo útil al ejecutarlo en días distintos.
- Hacer la carga idempotente: repetir el arranque no duplica datos ni modifica registros ajenos al seed; limitar su ejecución al modo de demo/local explícitamente habilitado.
- Agregar pruebas deterministas del seed con H2 en modo PostgreSQL y documentar en el README cómo configurarlo y arrancarlo.
- No incorporar envío real de correo, endpoints nuevos, datos reales ni cambios en la interfaz.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `semilla-inicial`: ampliar el contrato del seed para cubrir el conjunto completo de demo, su activación explícita, determinismo temporal e idempotencia.

## Impact

- Backend: configuración y componentes de `seed`, usando los repositorios existentes de Dentist, Patient, Appointment, ScheduleBlock, WaitingListEntry y OutboxMessage.
- Tests: pruebas del seed de demo con H2 y reloj controlado, sin requerir PostgreSQL.
- Documentación: pasos de configuración y arranque reproducible en `README.md`.
- Sin cambios de API, esquema de persistencia, versiones del stack ni integración de correo real.
