# Reglas de Negocio

## Dominio: Reserva (RN-RS)

- **RN-RS-01:** la reserva pública muestra turnos desde 24hs y hasta 30 días desde el momento de consulta.
- **RN-RS-02:** la duración fija es de 30 minutos.
- **RN-RS-03:** no se puede crear ni mover un turno a una franja ocupada.
- **RN-RS-04:** ante conflicto concurrente el servidor rechaza con HTTP 409 y no crea un segundo turno.
- **RN-RS-05:** paciente necesita nombre, apellido, DNI y email válido; el formato de email se valida estándar, sin dominio restringido.
- **RN-RS-06:** DNI solo acepta 7 u 8 dígitos; se eliminan puntos y espacios antes de guardar y se rechazan letras.

## Dominio: Estados (RN-ES)

- `PENDING → CONFIRMED | CANCELLED | ABSENT`
- `CONFIRMED → CANCELLED | ABSENT | ATTENDED`
- No se permite volver de `CANCELLED`, `ABSENT` o `ATTENDED` a `PENDING`.
- `CANCELLED` conserva el registro con soft delete/lógica de estado.

## Dominio: Cancelación (RN-CA)

- **RN-CA-01:** el paciente puede cancelar hasta 24hs antes del inicio, incluso si el turno ya está `CONFIRMED`.
- **RN-CA-02:** una solicitud con menos de 24hs se rechaza en el canal público y se deriva al odontólogo; el odontólogo decide y puede marcar `ABSENT`.
- **RN-CA-03:** cancelar libera el horario y dispara el proceso de lista de espera una sola vez; una segunda cancelación devuelve "ya cancelado" sin repetir efectos.
- **RN-CA-04:** cancelar no elimina el turno ni el historial.

## Dominio: Confirmación (RN-CF)

- **RN-CF-01:** el token es reutilizable: no se consume al confirmar y sirve tanto para confirmar como para cancelar. Solo se guarda su hash y vence al inicio del turno o a los 7 días, lo que ocurra primero.
- **RN-CF-02:** el token vence al inicio del turno o a los 7 días, lo que ocurra primero, y no funciona en turnos pasados.
- **RN-CF-03:** confirmar y cancelar aplican el primer cambio válido; si la operación llega a un turno ya `CONFIRMED` responde "ya confirmado" y si llega a `CANCELLED` responde "ya cancelado", sin repetir efectos ni avisos.
- **RN-CF-04:** un turno `CANCELLED` no se puede confirmar.
- **RN-CF-05:** el recordatorio simulado queda visible en la bandeja de salida.

## Dominio: Lista de espera (RN-LE)

- **RN-LE-01:** el orden es FIFO por `created_at`.
- **RN-LE-02:** al liberarse una franja se produce un mensaje simulado al primer paciente elegible.
- **RN-LE-03:** el paciente puede reservar otra disponibilidad después de cancelar.
- **RN-LE-04:** no se puede confirmar automáticamente un turno desde la lista de espera en v1.

## Dominio: Datos (RN-DA)

- **RN-DA-01:** borrado físico no se usa para turnos ni pacientes en v1.
- **RN-DA-02:** los datos seed son ficticios y no representan pacientes reales.
- **RN-DA-03:** el token se genera con al menos 128 bits de entropía y nunca se guarda como texto plano; se persiste su hash SHA-256.

## Excepciones globales

- Los errores concurrentes nunca crean duplicados.
- Los cambios de estado generan un evento de auditoría mínimo.
- Las validaciones inválidas devuelven errores consistentes, sin exponer detalles internos.
