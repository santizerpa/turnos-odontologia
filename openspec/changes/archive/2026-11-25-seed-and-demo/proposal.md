# Proposal

## Why

El seed actual crea únicamente al odontólogo inicial y no deja la aplicación lista para recorrer una demo completa de agenda, turnos, lista de espera y bandeja. Se necesita un conjunto ficticio reproducible, relativo a la fecha de ejecución e idempotente, que permita levantar y revisar la demo local sin preparar registros a mano ni introducir datos reales. El alcance considera que C-10 y C-11 ya están integrados: los fixtures no deben interferir con recordatorios ni activar el flujo de notificación por cancelación.

## What Changes

- Agregar una carga de demo optativa con `APP_DEMO_SEED` (deshabilitada por defecto), compatible con el seed administrativo existente: `APP_ADMIN_SEED`, `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`.
- Ejecutar explícitamente `AdminDentistSeeder` antes de `DemoDataSeeder`; si la demo está habilitada y falta la configuración/odontólogo administrativo requerido, fallar con un mensaje operativo antes de persistir datos parciales.
- Crear de forma determinista 5–10 pacientes ficticios, turnos en los estados requeridos, tres entradas de lista de espera, un bloqueo de agenda de ejemplo y mensajes visibles en la bandeja de salida.
- Derivar las fechas del reloj de agenda y de la zona horaria configurada. Todos los turnos activos deben estar al menos 24 horas y como máximo 30 días adelante, en días y franjas laborales configurados, sin solaparse con el bloqueo ni entre sí.
- Hacer la carga idempotente para la misma fecha efectiva local: reutilizar los fixtures sin modificarlos. Si ya existe el conjunto de demo y la fecha efectiva cambia (por ejemplo, reinicio de lunes a martes), no mover, sobrescribir ni regenerar sus registros; exigir al operador una base local de demo limpia.
- Mantener los turnos cancelados como fixtures en estado inicial, con `activeSlot` nulo, sin publicar `AppointmentCancelledEvent` ni consumir entradas `WAITING`. Los turnos activos conservan `activeSlot` único.
- Crear mensajes de outbox `PENDING` de tipo `BOOKING` (o tipo no `REMINDER` con compatibilidad explícita), con destinatarios reservados bajo `.invalid`; no invocar el despacho ni SMTP, WhatsApp u otro servicio externo.
- Excluir el runner de demo del perfil `test`, aun cuando `APP_DEMO_SEED=true`. Agregar pruebas deterministas del seed con H2 en modo PostgreSQL y documentar en el README su opt-in, variables y comandos de PowerShell y shell Unix.
- No incorporar envío real de correo, endpoints nuevos, datos reales ni cambios en la interfaz. `CHANGES.md` queda fuera de alcance: ninguna tarea de este change lo edita; solo el líder del proyecto lo modifica.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `semilla-inicial`: ampliar el contrato del seed para cubrir el conjunto completo de demo, su activación explícita, determinismo temporal, seguridad frente a C-10/C-11 e idempotencia entre reinicios.

## Impact

- Backend: configuración y componentes de `seed`, usando los repositorios existentes de Dentist, Patient, Appointment, ScheduleBlock, WaitingListEntry y OutboxMessage.
- Tests: pruebas del seed con H2 en modo PostgreSQL, reloj controlado, aislamiento del perfil `test` y ausencia de efectos laterales C-10/C-11; H2 es solo para tests, no para el runtime local.
- Documentación: actualizar en `README.md` la tabla de variables y ejemplos de arranque PowerShell y shell Unix, indicando `APP_DEMO_SEED=false` por defecto y el opt-in explícito.
- Sin cambios de API, esquema de persistencia, versiones del stack ni integración de correo real. Sin cambios a `CHANGES.md`.
