# Flujos Principales

## Flujo 1: Login del odontólogo

**Disparador:** el odontólogo abre la aplicación.
**Actores:** odontólogo, backend, PostgreSQL.

1. Ingresa usuario y contraseña.
2. Backend valida credenciales y genera sesión/cookie.
3. Frontend carga agenda y turnos.
4. Si falla, muestra error sin revelar qué dato es incorrecto.

## Flujo 2: Reserva pública

**Disparador:** el paciente abre el enlace público.
**Actores:** paciente, frontend público, API, repositorio de pacientes, repositorio de turnos.

1. Consulta `GET /api/public/slots` dentro de 24hs–30 días.
2. API calcula slots libres con turnos activos y duración 30 minutos.
3. Carga nombre, apellido, DNI y email.
4. API reutiliza o crea Patient por DNI.
5. Crea Appointment en PENDING dentro de transacción.
6. Crea AppointmentToken con vencimiento.
7. Guarda OutboxMessage simulado.
8. Si la franja fue tomada, devuelve 409 y no crea entidades.

## Flujo 3: Confirmación/cancelación por link

**Disparador:** el paciente abre el link generado en la bandeja simulada.

```text
Paciente → Página pública → API REST → Servicio de turno → JPA → PostgreSQL
                                  ← estado actualizado / error
```

1. API obtiene el hash del token y valida vigencia, uso y turno.
2. Confirma o cancela según estado y fecha.
3. Genera AppointmentEvent.
4. Si cancela, libera el slot y despacha la cola FIFO.
5. Devuelve una página con el estado final.

## Flujo 4: Lista de espera

**Disparador:** se cancela o libera un turno.

1. El sistema busca entradas WAITING elegibles.
2. Toma la primera entrada por `created_at`.
3. Cambia estado a NOTIFIED.
4. Persiste OutboxMessage dirigido al paciente.
5. No asigna ni confirma el turno automáticamente.

## Flujo 5: Cancelación manual del odontólogo

**Disparador:** el odontólogo cancela desde la agenda.

1. Valida autenticación y turno.
2. Cambia estado a CANCELLED y conserva registro.
3. Genera evento con actor ODONTOLOGO.
4. Libera la franja y ejecuta la lista de espera.

## Casos de error

- Slot ocupado → 409 Conflict.
- DNI no válido → 422 con campo a corregir.
- Token vencido/usado → 410 Gone o 400 según mensaje del API.
- Turno pasado → 409/422, sin cambios.
- Base no disponible → 503 sin exponer credenciales.
