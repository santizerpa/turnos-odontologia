# Visión y Objetivos

## Propósito del sistema

Reducir ausencias y cancelaciones tardías en un consultorio odontológico de un solo profesional mediante una agenda pública, un ciclo de confirmación por mail y una lista de espera que reaprovecha los horarios liberados.

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios |
|---|---|---|
| Odontólogo | Administrar la agenda sin solapamientos | Confirmar disponibilidad, marcar asistencia, cancelar turnos y atender pacientes |
| Paciente | Reservar y confirmar sin llamar | Elegir un horario libre, cancelar hasta 24hs antes y reservar otra disponibilidad |

## Alcance v1

- Login simple del odontólogo con email y contraseña.
- Agenda diaria y semanal con turnos de 30 minutos.
- Alta mínima de pacientes con nombre, apellido, DNI y email.
- Los pacientes crean sus propios turnos desde la reserva pública; no existe carga manual por el odontólogo.
- Los turnos solo se crean si no hay solapamiento con otro turno activo.
- Pacientes creados al reservar o al cargar un turno. Se identifica con un token único por turno o por aviso, con vencimiento y de un solo uso, sin login del paciente.
- Reserva pública desde 24hs hasta 30 días en el futuro.
- Confirmación/cancelación del paciente mediante link con token.
- Bandeja de salida simulada; no se manda correo real.
- Lista de espera FIFO para turnos liberados.
- Estados: pendiente, confirmado, cancelado, ausente y atendido.
- Datos seed simples y demo local.
- Tests JUnit con H2 en memoria compatible con PostgreSQL.

## Fuera de alcance

- Odontograma y periodontograma.
- Historia clínica dental completa.
- Cobros, señas, Mercado Pago, obras sociales y facturación.
- WhatsApp real y SMTP real.
- Duración variable por práctica.
- Multi-sucursal y múltiples odontólogos.
- Roles avanzados, SSO y OAuth.
- Score de riesgo, campañas y marketplace de pacientes.

## Métricas de éxito

- 100% de turnos v1 evita solapamientos.
- Reserva pública crea un turno confirmado sin intervención del odontólogo.
- Confirmación/cancelación cambia el estado una sola vez y deja auditoría mínima.
- Al cancelar, el siguiente paciente de la lista recibe un aviso simulado.
- La suite corre localmente y pasa los tests JUnit con H2.
