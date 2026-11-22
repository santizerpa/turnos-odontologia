# Funcionalidades

## Épica 1: Acceso del odontólogo

### US-001 — Login
**Como** odontólogo
**Quiero** iniciar sesión con usuario y contraseña
**Para** administrar la agenda de forma privada

**Criterios de aceptación:**
- [ ] Usuario incorrecto devuelve error controlado.
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

### US-011 — Crear turno manual
El odontólogo carga paciente, inicio y duración fija de 30 minutos; rechaza solapamientos.

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

## Épica 5: Confirmación por mail simulado

### US-030 — Generar recordatorio
El sistema genera un mensaje y un link seguro con token.

### US-031 — Confirmar por link
El paciente abre el link y cambia PENDING/CONFIRMED según reglas.

### US-032 — Visualizar bandeja
El odontólogo ve los mensajes simulados y puede abrir el link público de prueba.

## Épica 6: Lista de espera

### US-040 — Registrar paciente
El paciente o odontólogo añade una entrada FIFO.

### US-041 — Avisar al liberar horario
Al cancelar o liberar una reserva, el sistema genera un mensaje al primer paciente elegible.

## Épica 7: Demo y calidad

### US-050 — Datos seed
La demo arranca con usuarios, pacientes y turnos ficticios.

### US-051 — Tests deterministas
Las reglas se prueban con JUnit y H2 sin PostgreSQL instalado.
