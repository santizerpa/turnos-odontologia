# Funcionalidades

## Épica 1: Acceso del odontólogo

### US-001 — Login
**Como** odontólogo
**Quiero** iniciar sesión con email y contraseña
**Para** administrar la agenda de forma privada

**Criterios de aceptación:**
- [ ] Email incorrecto devuelve error controlado.
- [ ] Contraseña se valida contra hash seguro.
- [ ] Sesión expira o se invalida al cerrar.
- [ ] Rutas administrativas exigen autenticación.

**Reglas relacionadas:** RN-ES, RN-DA

## Épica 2: Pacientes

### US-002 — Crear paciente desde reserva
**Como** paciente
**Quiero** cargar mis datos mínimos
**Para** reservar sin crear cuenta

**Criterios:** nombre, apellido, DNI y email válidos; DNI normalizado; reutilizar paciente existente si coincide.

## Épica 3: Agenda y turnos

### US-010 — Ver agenda
El odontólogo ve turnos diarios/semanales y estados.

### US-011 — Turno creado por reserva
No existe carga manual por el odontólogo. El paciente crea el turno desde la página pública y el sistema rechaza solapamientos.

### US-012 — Marcar estado
El odontólogo marca confirmado, ausente o atendido, conservando historial.

## Épica 4: Reserva pública

### US-020 — Consultar horarios
El paciente ve horarios libres desde 24hs hasta 30 días.

### US-021 — Reservar turno
El paciente elige un slot libre y queda PENDING; conflicto devuelve 409.

### US-022 — Cancelar turno público
El paciente cancela hasta 24hs antes; el turno conserva historial y libera el slot.

### US-023 — Reservar nuevamente
Después de cancelar, el paciente vuelve a consultar disponibilidad y crea otro turno.

## Épica 5: Validación de turno por mail simulado

### US-030 — Generar recordatorio
El sistema genera un mensaje y un link seguro con token.

### US-031 — Confirmar por link
El paciente abre la página del link y confirma con un `POST`; el estado pasa `PENDING → CONFIRMED`. Si ya estaba `CONFIRMED`, responde "ya confirmado" sin repetir efectos. El token no se consume y sigue válido para cancelar hasta su vencimiento.

### US-032 — Cancelar desde el link
**Como** paciente
**Quiero** cancelar el turno desde el mail simulado
**Para** liberar el horario sin crear cuenta

**Criterios:** el estado pasa `PENDING → CANCELLED` o `CONFIRMED → CANCELLED` hasta 24hs antes; se conserva el historial y se dispara la lista de espera una sola vez. Si ya estaba `CANCELLED`, responde "ya cancelado" sin repetir efectos. Con menos de 24hs se rechaza y se deriva al odontólogo.

### US-033 — Visualizar bandeja
El odontólogo ve los mensajes simulados y puede abrir el link público de prueba.

## Épica 6: Lista de espera

### US-040 — Registrar paciente
El paciente se inscribe sin sesión mediante `POST /api/public/waiting-list`; el odontólogo consulta la cola privada mediante `GET /api/waiting-list`. El DNI identifica al paciente, no se modifica un perfil existente y no se permiten entradas `WAITING` duplicadas para el mismo paciente.

### US-041 — Avisar al liberar horario
Al cancelar o liberar una reserva, el sistema genera un mensaje al primer paciente elegible cuyo rango inclusivo contenga la fecha local argentina del turno. Solo se notifica si el turno está entre 24 horas y 30 días desde ahora, se excluye al paciente que canceló, y el aviso incluye fecha y hora locales. La entrada pasa a `NOTIFIED`; no se asigna el turno automáticamente.

## Épica 7: Demo y calidad

### US-050 — Datos seed
La demo arranca con un odontólogo ficticio, pacientes y turnos ficticios. El odontólogo usa email y contraseña BCrypt ficticia documentada solo para demo.

### US-051 — Tests deterministas
Las reglas se prueban con JUnit y H2 sin PostgreSQL instalado.
