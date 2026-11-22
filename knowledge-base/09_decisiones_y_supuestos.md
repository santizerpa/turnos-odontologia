# Decisiones y Supuestos

## Decisiones documentadas

### DD-01 — Aplicación web monolítica
**Decisión:** backend Spring Boot REST + frontend TypeScript vanilla.
**Contexto:** stack obligatorio y entrega en 4 días.
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
**Contexto:** usuario aprobó demo local sin credenciales ni red.
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

### DD-06 — Token de confirmación
**Decisión:** hash persistido, único y con vencimiento.
**Contexto:** links públicos.
**Alternativas:** JWT firmado o token opaco en table.
**Trade-offs:** requiere limpieza/índice y no hay cifrado extremo.

## Supuestos inferidos

### SU-01 — Fusiones por DNI
**Supuesto:** el DNI normalizado identifica al paciente existente.
**Origen:** decisión de datos aprobada.
**Riesgo si es falso:** pacientes reales pueden compartir o equivocarse el DNI.
**Cómo validar:** obtener dato real en demo e implementar validación externa posterior.

### SU-02 — Ventana en zona local
**Supuesto:** todos los horarios usan la zona horaria local del consultorio.
**Origen:** no hay zonas horarias ni operación remota especificadas.
**Riesgo si es falso:** huecos incorrectos para reservas remotas.
**Cómo validar:** definir timezone en configuración.

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
