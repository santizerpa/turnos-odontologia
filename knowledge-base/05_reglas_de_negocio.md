# Reglas de Negocio

## Dominio: Reserva (RN-RS)

- **RN-RS-01:** la reserva pública muestra turnos desde 24hs y hasta 30 días desde el momento de consulta.
- **RN-RS-02:** la duración fija es de 30 minutos.
- **RN-RS-03:** no se puede crear ni mover un turno a una franja ocupada.
- **RN-RS-04:** ante conflicto concurrente el servidor rechaza con HTTP 409 y no crea un segundo turno.
- **RN-RS-05:** paciente necesita nombre, apellido, DNI y email válido.
- **RN-RS-06:** DNI normalizado identifica el paciente existente; no valida identidad oficial.

## Dominio: Estados (RN-ES)

- `PENDING → CONFIRMED | CANCELLED | ABSENT`
- `CONFIRMED → CANCELLED | ABSENT | ATTENDED`
- No se permite volver de `CANCELLED`, `ABSENT` o `ATTENDED` a `PENDING`.
- `CANCELLED` conserva el registro con soft delete/lógica de estado.

## Dominio: Cancelación (RN-CA)

- **RN-CA-01:** el paciente puede cancelar hasta 24hs antes.
- **RN-CA-02:** una solicitud posterior a 24hs queda como ABSENT o requiere decisión del odontólogo según el estado actual.
- **RN-CA-03:** cancelar libera el horario y dispara el proceso de lista de espera.
- **RN-CA-04:** cancelar no elimina el turno ni el historial.

## Dominio: Confirmación (RN-CF)

- **RN-CF-01:** el token es único por turno y solo se acepta una vez.
- **RN-CF-02:** el token vence según configuración y no funciona en turnos pasados.
- **RN-CF-03:** confirmar/cancelar en simultáneo aplica un estado final antes que el otro evento.
- **RN-CF-04:** el recordatorio simulado queda visible en la bandeja de salida.

## Dominio: Lista de espera (RN-LE)

- **RN-LE-01:** el orden es FIFO por `created_at`.
- **RN-LE-02:** al liberarse una franja se produce un mensaje simulado al primer paciente elegible.
- **RN-LE-03:** el paciente puede reservar otra disponibilidad después de cancelar.
- **RN-LE-04:** no se puede confirmar automáticamente un turno desde la lista de espera en v1.

## Dominio: Datos (RN-DA)

- **RN-DA-01:** borrado físico no se usa para turnos ni pacientes en v1.
- **RN-DA-02:** los datos seed son ficticios y no representan pacientes reales.
- **RN-DA-03:** el token nunca se guarda como texto plano.

## Excepciones globales

- Los errores concurrentes nunca crean duplicados.
- Los cambios de estado generan un evento de auditoría mínimo.
- Las validaciones inválidas devuelven errores consistentes, sin exponer detalles internos.
