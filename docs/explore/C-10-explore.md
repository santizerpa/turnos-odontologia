# Explore C-10 — outbox-and-reminders

## Hallazgos principales

1. **Outbox actual.** `OutboxMessage` tiene `id`, `appointment` opcional, `recipientEmail`, `subject`, `body`, `status`, `createdAt` y `sentAt` ([04 modelo, OutboxMessage](../../knowledge-base/04_modelo_de_datos.md); `OutboxMessage.java:12-36`). Estados reales: `PENDING` y `SIMULATED_SENT` (`OutboxStatus.java:1-2`). Falta diferenciar explícitamente reserva y recordatorio (`type/messageType`/key por tipo) y exponer un mutador para pasar a `SIMULATED_SENT` asignando `sentAt`. La entidad tiene `sentAt`, pero solo se diferencia por `subject/body` para hoy; el índice solo cubre `appointment_id` (`OutboxMessage.java:10`, `OutboxMessageRepository.java:1-4`).

2. **Link y token.** C-07 llama `issue()` una vez y guarda en el body de reserva links relativos con el token plano devuelto; C-10 no puede obtener ese plano porque solo persiste el hash (`PublicBookingService.java:76-78,86-90`; `AppointmentTokenService.java:35-56`; DD-08). Llamar `issue()` nuevamente reemplaza el hash y invalida el link de reserva. Por eso C-10 debe reutilizar el mail/cuerpo de C-07 o la referencia ya existente, sin reemitir: no hay hoy una forma segura de C-10 de recuperar el valor plano. Esto es coherente con DD-08, DD-06 y RN-CF-01; no es un fallo de C-09.

3. **Elegibilidad y ventana.** Son candidatos los turnos con `activeSlot != null`, inicio futuro y estado `PENDING` o `CONFIRMED`; excluir `CANCELLED`, `ABSENT`, `ATTENDED` y pasados (`Appointment.java:28-43,61-64,70-74`). La ventana resuelta es 24-48 h antes: `now` debe cumplir `startAt - 48h <= now <= startAt - 24h` (POA-04). Los límites inclusivos/exclusivos y si se genera una sola vez no están definidos.

4. **Duplicados.** Sin modificar C-07/C-09, se puede buscar antes de guardar un mensaje de recordatorio asociado al turno (convenio por `subject/type`) y saltarlo. Es débil ante concurrencia y reinicios. Una extensión propia de C-10 con `messageType` y/o clave única `(appointment_id, message_type)` es más robusta y aceptable: no modifica entidades de C-07/C-09, pero sí puede ampliar `OutboxMessage`; si no se autoriza, `subject` puede servir solo como heurística no garantizada.

5. **Disparador.** Un endpoint protegido `POST`/`PUT` para procesar/generar recordatorios es el más fácil de testear y mostrar en demo. `/api/outbox/**` no aparece en `permitAll`, por lo que queda protegido por `.anyRequest().authenticated()` (`AuthSecurityConfiguration.java:60-66`). `@Scheduled` es viable como automatización, pero más difícil de fijar en reloj/tests y requiere consideración de ejecución concurrente.

6. **Simulación de envío.** `PENDING` es “pendiente de procesamiento”; C-10 puede marcarlo `SIMULATED_SENT` y fijar `sentAt = now` al procesarlo. No hay SMTP ni entrega real (DD-03). El procesador/servicio C-10 debe ser quien haga esa transición; no es una transición automática de JPA.

7. **GET protegido.** `GET /api/outbox/messages` debe devolver datos mínimos para la bandeja: `id`, `appointmentId`, `recipientEmail`, `subject`, `body`, `status`, `createdAt`, `sentAt`. Si `body` contiene el link de reserva C-07, ese link ya es parte del mensaje y no necesita exponerlo como campo extra. No incluir DNI, nombre completo, teléfono, hash/tokens, datos de sesión ni detalles internos. El DTO exacto no está definido en la KB.

## Contradicciones o ambigüedades

- CHANGES pide link tokenizado, pero C-10 solo conoce entidades sin token plano: reemitir con `issue()` invalida la reserva C-07, por lo que el link correcto debe provenir de C-07 o requerir decisión/artefacto nuevo.
- El alcance de CHANGES no decide si el disparador es endpoint, scheduler o ambos, ni define límites inclusivos de la ventana.
- No existe tipo/categoría ni unicidad en persistencia para garantizar un único recordatorio por turno.
- No hay getters/setters de estado en `OutboxMessage` ni método repository específico; C-10 tendrá que usar una API propia sin alterar C-07/C-09.
- No hay contradicción con DD-03/DD-08: el correo es simulación y el token se emite una vez/reemite reemplazando el anterior.

**Fuentes:** `CHANGES.md:312-327`, KB 04/07/05/10/09 y código indicado arriba.
