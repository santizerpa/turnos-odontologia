# Proposal

## Why

C-10 completa el MVP imprescindible de comunicación por correo simulado: permite avisar al paciente dentro de la ventana de 24 a 48 horas y permite al odontólogo revisar y despachar esos mensajes. El punto 5 del Discovery requiere recordatorio con link funcional sin introducir infraestructura SMTP real.

## What Changes

- Agregar generación de recordatorios para turnos elegibles entre 24 y 48 horas antes de su inicio.
- Emitir un nuevo token válido mediante C-09 por cada recordatorio creado y construir enlaces relativos de confirmación y cancelación con el mismo formato que C-07.
- Extender aditivamente `OutboxMessage` con tipo, unicidad por turno y tipo, y transición simulada a enviado.
- Exponer listado protegido y disparadores protegidos para generar y despachar mensajes.
- Definir respuestas JSON mínimas y manejo local de errores sin exponer datos internos.

## Capabilities

### New Capabilities
- `outbox-reminders`: gestión de mensajes simulados, elegibilidad de recordatorios, enlaces tokenizados, despacho y acceso protegido.

### Modified Capabilities

- Ninguna.

## Impact

- Backend: entidad y repositorio de bandeja de salida, servicio de recordatorios y controladores C-10.
- Persistencia: columna de tipo en `outbox_messages` y restricción única `(appointment_id, type)`; se conserva el constructor existente de cinco parámetros.
- Integración: reemisión de token mediante `AppointmentTokenService.issue(Appointment)`; no se modifica C-07 ni C-09.
- Seguridad: los endpoints C-10 quedan bajo sesión de odontólogo; el correo sigue siendo una simulación sin SMTP.
