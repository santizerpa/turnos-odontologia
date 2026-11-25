# Decisiones y Supuestos

## Decisiones documentadas

### DD-01 — Aplicación web monolítica
**Decisión:** backend Spring Boot REST + frontend TypeScript vanilla.
**Contexto:** stack obligatorio y entrega en 3 días (22-24/11).
**Alternativas:** React/Angular; microservicios; backend-only.
**Justificación:** reduce setup y complejidad sin ocultar la API.
**Trade-offs:** menos componente reutilizable, UI manual y menor ecosistema frontend.

### DD-02 — PostgreSQL local y H2 en tests
**Decisión:** PostgreSQL para ejecutar; H2 para test sin instalación externa.
**Contexto:** demo local y restricción sin hosting pago.
**Alternativas:** solo H2; SQLite; PostgreSQL obligatorio local.
**Justificación:** compatibilidad JPA y pruebas rápidas.
**Trade-offs:** diferencias SQL que deben verificarse antes de entrega.

### DD-03 — Mail simulado
**Decisión:** persistir mensajes en bandeja de salida; sin SMTP real.
**Contexto:** el usuario aprobó demo local sin credenciales ni red externas.
**Alternativas:** SMTP real, sandbox, WhatsApp.
**Justificación:** permite probar el ciclo sin secretos externos.
**Trade-offs:** no valida entregabilidad ni respuesta real de pacientes.

### DD-04 — Reserva pública sin login
**Decisión:** paciente reserva sin cuenta y usa token por turno.
**Contexto:** consultorio unipersonal y fricción mínima.
**Alternativas:** cuenta, magic link, OTP.
**Justificación:** menor complejidad y demo rápido.
**Trade-offs:** seguridad limitada a posesión del token y datos mínimos.

### DD-05 — Soft delete e historial
**Decisión:** turnos y pacientes conservan registros/estado.
**Contexto:** auditoría mínima y recuperación de demo.
**Alternativas:** borrado físico, archive externo.
**Trade-offs:** crecimient de datos y reglas de retención pendientes.

### DD-06 — Token de confirmación y cancelación
**Decisión:** un único token opaco por turno, de al menos 128 bits (32 bytes en base64url), con hash SHA-256 persistido y vencimiento al inicio del turno o a los 7 días. No se consume al confirmar: confirmar y cancelar son idempotentes y el token sigue válido hasta su vencimiento.
**Contexto:** links públicos de confirmación y cancelación enviados por la bandeja simulada.
**Alternativas:** JWT firmado; dos tokens separados (confirmar/cancelar); token de un solo uso.
**Justificación:** permite cancelar después de confirmar sin generar un segundo link, y el estado del turno evita efectos duplicados.
**Trade-offs:** el link es reutilizable dentro de su ventana; se mitiga con vencimiento corto, token de alta entropía y validación de estado. No hay cifrado extremo ni revocación individual.

### DD-07 — Punto único de cancelación y evento de dominio
**Decisión:** toda cancelación pasa por `AppointmentService.cancel(appointmentId, actor, reason)`, que valida la transición (y la regla de 24hs si el actor es `PATIENT`), pone `CANCELLED`, deja `active_slot` en `NULL`, registra `AppointmentEvent` y publica `AppointmentCancelledEvent` solo si la transición ocurrió. La lista de espera escucha con `@TransactionalEventListener(AFTER_COMMIT)` y `@Transactional(REQUIRES_NEW)`.
**Contexto:** cancelan el odontólogo desde el PATCH y el paciente desde el token; ambos deben aplicar las mismas reglas y no duplicar avisos.
**Alternativas:** lógica duplicada en cada endpoint; llamada directa de C-09 a C-11; evento publicado siempre, incluso sin transición.
**Justificación:** un solo lugar concentra la regla de negocio, la auditoría y el evento; el listener desacopla la lista de espera sin que C-11 modifique C-05.
**Trade-offs:** el aviso es eventual: si el listener falla después del commit, la cancelación ya quedó firme y el aviso puede requerir reintento o registro. El evento solo se procesa si la cancelación corre dentro de una transacción; una segunda cancelación no publica nada.

### DD-08 — Token reutilizable emitido por C-09
**Contexto:** C-09 debe emitir el enlace público que C-07 entrega durante la reserva y permitir confirmar o cancelar el mismo turno sin consumir el token.
**Decisión:** C-09 emite el token mediante `AppointmentTokenService.issue(Appointment)`; una reemisión reemplaza el registro anterior y hace inválido el token anterior.
**Consecuencias:** solo se conserva el hash del token vigente, el valor plano se devuelve una sola vez y cada reemisión invalida el enlace previo.

### DD-09 — Reemisión del token para recordatorios
**Contexto:** C-10 debe generar un recordatorio por turno sin depender del contenido del mail anterior ni leer el token del body del mail de C-07.
**Decisión:** el recordatorio reemite el token mediante `issue()` y ejecuta la generación en una transacción por turno, con resultado `{ created, failed }`.
**Alternativa descartada:** leer el token del body del mail generado por C-07, porque acopla el recordatorio a un formato de texto y no constituye una fuente segura ni estructurada del token vigente.
**Consecuencias:** el nuevo mail deja vigente únicamente el token reemitido y invalida el link de reserva anterior; la transacción garantiza la consistencia individual de cada turno y permite informar cantidades creadas y fallidas.

## Supuestos inferidos

### SU-01 — Fusiones por DNI
**Supuesto:** el DNI normalizado identifica al paciente existente.
**Origen:** decisión de datos aprobada.
**Riesgo si es falso:** pacientes reales pueden compartir o equivocarse el DNI.
**Cómo validar:** obtener dato real en demo e implementar validación externa posterior.

### SU-02 — Ventana en zona Argentina
**Supuesto:** todos los horarios usan la hora de Argentina (UTC-3), sin horario de verano.
**Origen:** decisión aprobada para eliminar ambigüedad del modelo horario.
**Riesgo si es falso:** turnos visualizados o reservados en otra hora.
**Cómo validar:** configurar America/Argentina/Buenos_Aires o un offset fijo UTC-3 en el modelo.

### SU-03 — Lista de espera avisa pero no reasigna
**Supuesto:** el mensaje simulado espera acción manual del paciente o odontólogo.
**Origen:** la ronda resolvió aviso automático, no autoasignación.
**Riesgo si es falso:** el paciente puede reservar el turno esperado en simultáneo.
**Cómo validar:** decidir reprogramación automática en próxima versión.

### SU-04 — Datos seed deterministas
**Supuesto:** los horarios seed cambian según fecha de ejecución.
**Origen:** demo local.
**Riesgo si es falso:** enlaces vencidos o agenda vacía.
**Cómo validar:** script seed relativo a fecha/hora local.

### SU-05 — Bandeja de entrada simulada accesible por email
**Supuesto:** en la demo, el correo del paciente se simula en la página pública: quien ingresa un email ve sus mensajes y puede confirmar o cancelar con los enlaces que contienen. Se acepta porque no hay correo real ni cuentas de paciente.
**Origen:** decisión del usuario en la revisión de `ui-polish` (2026-11-25).
**Riesgo si es falso:** fuera de la demo, cualquiera que conozca un email podría confirmar o cancelar turnos ajenos y ver su DNI.
**Cómo validar:** `app.demo.inbox.enabled` está en `false` por defecto y solo se activa en el perfil `local`; desactivada, `GET /api/public/inbox` responde 404.
