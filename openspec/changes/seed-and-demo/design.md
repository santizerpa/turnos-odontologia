# Design

## Context

El backend ya tiene un `AdminDentistSeeder` optativo por `APP_ADMIN_SEED`, configuración de agenda con `Clock` inyectable y repositorios para pacientes, turnos, bloqueos, lista de espera y outbox. En `upstream/main`, C-10 y C-11 están integrados; la rama local `feat/santosc13` aún no contiene ese avance, por lo que la aplicación del change deberá hacerse sobre una rama actualizada antes de implementar.

La carga es local/de demo, no una migración de esquema ni un servicio de envío. Las fechas deben seguir la zona fija `America/Argentina/Buenos_Aires`; los datos y credenciales siguen siendo ficticios.

## Goals / Non-Goals

**Goals:**
- Reutilizar la configuración existente del odontólogo y cargar un dataset relacionado, coherente con las entidades y reglas actuales.
- Mantener la carga aislada, transaccional, repetible para una misma fecha efectiva y activada solo por opt-in.
- Hacer que turnos, bloqueo, lista de espera y bandeja queden visibles sin disparar efectos externos ni notificaciones accidentales.

**Non-Goals:**
- Migrar o alterar el esquema, cambiar APIs o poblar la base de tests por defecto.
- Enviar emails reales, ejecutar el dispatch de outbox durante el seed o añadir datos reales.
- Mantener actualizados automáticamente los turnos de una demo ya cargada al cambiar de día; para una demo nueva se prepara una base local limpia según el README.

## Decisions

1. **Separar el dataset de demo del seed administrativo.** Mantener `APP_ADMIN_SEED` y sus credenciales tal como están; agregar un opt-in `APP_DEMO_SEED=true` para los demás datos. Ordenar explícitamente los runners para que `AdminDentistSeeder` termine antes de `DemoDataSeeder`; este último exige que el odontólogo configurado exista (y falla con un mensaje operativo si no está), en vez de crear credenciales alternativas. El README configura ambos opt-ins para la demo completa. Alternativa descartada: ejecutar todos los datos en cada arranque local, porque poblaría bases de desarrollo sin consentimiento.

2. **Usar fixtures estables y upsert conservador.** Identificar pacientes por sus DNIs sintéticos y emails ficticios reservados; asociar turnos/entradas/mensajes a esas identidades y consultar antes de insertar. Si un registro fixture ya existe, reutilizarlo sin sobrescribir datos; no borrar ni modificar registros ajenos. La idempotencia se define para la misma base y fecha efectiva, y el README pide preparar una base limpia para refrescar una demo en otra fecha. Alternativa descartada: truncar tablas o volver a insertar en cada arranque, por riesgo de borrar trabajo local y violar unicidad.

3. **Calcular las fechas desde el `Clock` de agenda.** Convertir el instante actual a fecha local en `America/Argentina/Buenos_Aires` y elegir, de forma determinista, días hábiles y horas de las franjas existentes. Ubicar los turnos activos dentro de la ventana demostrable y no superponerlos con el bloqueo. No usar `LocalDate.now()` ni la zona del sistema. Esto permite probar el calendario con `Clock.fixed` y conserva un escenario futuro útil.

4. **Crear estados iniciales sin simular una cancelación operativa.** Construir los turnos con la fábrica de dominio `Appointment.create`; pasar los confirmados y cancelado por los métodos de dominio existentes para conservar la relación entre estado y `active_slot`. El fixture cancelado se persiste como estado inicial y no publica `AppointmentCancelledEvent`: el seed no representa una cancelación en tiempo real y no debe cambiar una entrada `WAITING` ni producir un aviso de C-11. No escribir `active_slot` directamente ni llamar al dispatch.

5. **Mantener mensajes de muestra en la bandeja local.** Insertar mensajes ficticios `PENDING` con destinatario `.invalid` y claves de fixture estables, sin invocar el servicio de despacho. El endpoint existente solo simula el envío al marcar `SIMULATED_SENT`; no se agrega SMTP ni se llama a proveedores externos. Se evitan tipos de mensaje asociados a recordatorios para no interferir con la unicidad `(appointment_id, type)` ni con C-10.

6. **Cargar el conjunto dentro de una transacción.** Si falla una inserción necesaria, revertir el lote completo y dejar un error de arranque accionable. Las pruebas usan el perfil `test` con H2 en modo PostgreSQL y un reloj controlado; no requieren PostgreSQL.

## Risks / Trade-offs

- [El opt-in podría habilitarse por error en una base local persistente] → Mantenerlo deshabilitado por defecto, documentar claramente ambas variables y usar solo fixtures ficticios.
- [Un arranque repetido en otra fecha podría dejar una agenda de demo vencida] → La idempotencia se limita a la misma fecha efectiva; documentar que una demo nueva se prepara con una base local limpia, sin truncar datos automáticamente.
- [Un cambio en las reglas de turnos, lista de espera u outbox podría romper fixtures] → Construir mediante fábricas/métodos de dominio existentes y cubrir invariantes y relaciones en pruebas de integración H2.
- [Aplicar sobre la rama C-13 actual, que está detrás de `upstream/main`, podría omitir C-10/C-11] → Actualizar la rama con la integración del líder antes de iniciar `apply`; no integrar ni rebasar ramas como parte de este propose.

## Migration Plan

No hay migración de base de datos. Para la demo, el operador prepara una base local limpia, configura `APP_ADMIN_SEED=true`, `APP_DEMO_SEED=true` y las credenciales ficticias en variables de entorno, y arranca con el perfil local siguiendo el README. Para volver a generar fechas relativas en otro día, se reinicia la base local de demo siguiendo esos pasos; el seed no elimina datos automáticamente.

## UI Mockups

Decision: no (no UI)
