# Explore C-09 — token-confirmation

## 1. Reutilización de C-05

- C-05 aporta `AppointmentService.cancel(UUID appointmentId, Actor actor, String reason)`: C-09 debe pasar `Actor.PATIENT` y un motivo, sin cancelar directamente ni publicar eventos por su cuenta.
- El servicio ya es idempotente ante `CANCELLED`, aplica 24 h para pacientes, pone `CANCELLED` y `activeSlot = null`, registra `AppointmentEvent` y publica `AppointmentCancelledEvent` una sola vez. El test cubre exactamente 24 h, menos de 24 h, cancelación de un turno confirmado y repetición.
- `Appointment.confirmForFutureDomainService()` permite confirmar PENDING y rechazar CONFIRMED/CANCELLED; C-09 aún debe rodearlo con validación de token y vigencia.

## 2. Falta y riesgos para C-09

- **Generación y consumo del token:** C-07 es responsable de crear `AppointmentToken` junto con la reserva pública: generar 32 bytes aleatorios/base64url, calcular el hash SHA-256, calcular `expiresAt = min(inicio, ahora + 7 días)` y entregar el token plano al enlace/salida simulada. C-09 debe buscar por hash, validar vigencia/turno/estado, confirmar/cancelar y mantener el registro reusable. Falta una consulta por `tokenHash` en el repositorio.
- **Ambigüedad C-07/C-09:** CHANGES asigna explícitamente la creación a C-07, mientras C-09 pide persistir solo el hash. Si C-07 no se implementó aún, C-09 no puede recibir ni proveer un token real. Hay que definir en el spec el contrato exacto entre ambos o dejar C-07 como creador único del registro y C-09 como consumidor de su hash.
- **Vencimiento:** rechazar vencido y turno pasado; el límite inclusivo/exclusivo de `expiresAt` debe quedar definido. El token no se consume al confirmar.
- **Idempotencia:** confirmar dos veces devuelve “ya confirmado” sin efectos; cancelar dos veces delega a C-05 y evita eventos; cancelar un turno CANCELLED sigue resultando idempotente. Confirmar CANCELLED es inválido.
- **24 h:** C-05 permite exactamente 24 h y rechaza cuando falta menos tiempo (`startAt < now + 24h`). Para C-09, una cancelación fuera de plazo debe rechazarse sin mutar el turno ni emitir evento ni mensaje; una repetición ya cancelada conserva la idempotencia incluso si ya pasó el plazo.
- **CSRF y rutas públicas:** `AuthSecurityConfiguration` deshabilita CSRF globalmente (`.csrf(...disable())`), no solo `/api/public/**`. `/api/public/**` y los POST de confirmación/cancelación están permitidos sin autenticación. La sesión usa `JSESSIONID` con timeout de 30 minutos, HttpOnly y `SameSite=Lax`; esto reduce envíos entre sitios cross-site, pero no sustituye CSRF ni protege por sí solo enlaces POST iniciados por el mismo sitio. No hay un beneficio adicional de autenticación en estos endpoints.

## 3. Contradicciones o ambigüedades

- No hay contradicción entre TTL, reutilización, 24 h y cancelación centralizada en CHANGES/KB/código.
- Sí hay una ambigüedad de ownership: C-07 crea `AppointmentToken`; C-09 debe resolver el hash de una forma reutilizable para confirmar/cancelar. El registro y la prueba deben acordar cuál change genera, entrega y persiste el secreto.
- Confirmar todavía no tiene método de servicio público; solo existe `confirmForFutureDomainService()`. C-09 debe decidir si añade un método único o usa esa primitiva manteniendo auditoría y validación fuera de los endpoints.

## Preguntas abiertas para el spec

- Precedencia: ¿qué gana si el token está vencido y el turno ya está CONFIRMED/CANCELLED?
- `expiresAt`: ¿es válido hasta el instante exacto (`<=`) o deja de ser válido a partir de él (`<`)?
- ¿El GET devuelve una página HTML, un JSON, o ambos según los headers/contenido? Debe incluirse el contrato y el tratamiento de errores.
