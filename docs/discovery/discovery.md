# Discovery — Sistema de turnos para odontología

**Fecha:** 2026-11-21
**Estado:** cerrado para v1 (entrega 2026-11-24)
**Fuentes:** 17 sistemas relevados en `docs/discovery/competitors.md`; evidencia y accesos en `docs/discovery/evidence-log.md`; bibliografía en `docs/discovery/sources.md`.
**Regla de evidencia:** todo dato de competidor lleva URL oficial y fecha de consulta. Lo no demostrado públicamente figura como `No evidenciado`. Se distingue funcionalidad comprobada de afirmación comercial del proveedor.

---

## 1. Problema que resuelve

Un consultorio odontológico de un solo profesional pierde horas de sillón por ausencias y cancelaciones tardías. Hoy la confirmación existe, pero es manual y por correo: se le avisa al paciente y se le pregunta si asistirá, sin registro sistematizado de quién confirmó, quién no respondió y quién no vino.

El sistema resuelve dos cosas concretas:

1. **Ordenar la agenda** del odontólogo (turnos, huecos, solapamientos, estados).
2. **Reducir el ausentismo** con un ciclo explícito: recordatorio → confirmación o cancelación del paciente → liberación del turno → aviso automático a la lista de espera.

No resuelve gestión de clínica, facturación ni historia clínica completa. Esa es una decisión de alcance, no una omisión.

## 2. Usuarios / roles

| Rol | Verbo principal | Alcance en v1 |
|---|---|---|
| **Odontólogo** | Administrar | Único usuario con login. Carga turnos, define disponibilidad, ve agenda diaria/semanal, gestiona lista de espera, marca asistencia o ausencia. |
| **Paciente** | Reservar y confirmar | Sin cuenta ni contraseña. Reserva su turno desde un enlace público y confirma o cancela desde el link del correo. |

**Decisión:** el paciente es un usuario de primera clase para reservar, pero no tiene login. Su identidad se compone de los datos que carga al reservar (nombre, apellido, DNI, teléfono, email) más el token del turno.

## 3. Casos de uso

1. **Como odontólogo**, quiero ver mi agenda diaria y semanal para saber qué turnos tengo, cuáles están confirmados y dónde quedan huecos.
2. **Como odontólogo**, quiero cargar un turno manualmente (paciente, fecha, hora, duración) sin riesgo de solapamiento, para los pacientes que llaman o escriben.
3. **Como paciente**, quiero reservar mi propio turno desde un enlace público viendo los horarios libres reales, para no tener que llamar.
4. **Como odontólogo**, quiero que el sistema genere el recordatorio 24-48hs antes preguntando si el paciente asistirá.
5. **Como paciente**, quiero confirmar o cancelar mi turno con un click desde el correo, sin crear cuenta.
6. **Como odontólogo**, quiero que al liberarse un turno el sistema avise automáticamente al primero de la lista de espera.

## 4. Competidores / soluciones existentes

Resumen; el detalle con URLs, precios, operación, seguridad y puntajes está en `docs/discovery/competitors.md`.

**Ranking ponderado (0-5)** con los pesos acordados — turnos y automatización 25%, clínica 20%, integraciones AR/WhatsApp 15%, administración 15%, experiencia del paciente 10%, seguridad 10%, precio 5%:

| # | Sistema | Ponderado | Presencia AR |
|---|---|---:|---|
| 1 | Dentalink | 4,20 | Sí (sitio /ar) |
| 2 | DentalSoft | 4,15 | Sí (ARS, equipo local) |
| 3 | Doctocliq | 3,70 | No evidenciada |
| 4 | AgendaPro | 3,65 | Sí (ARS, CABA) |
| 5 | OdontoApp | 3,45 | Sí (Sovil) |

**Qué ya es estándar de mercado:** agenda multi-profesional, reserva online 24/7, recordatorios por WhatsApp, ficha clínica con odontograma, caja y cobros.

**Qué es diferenciador real:** el ciclo completo del no-show (detectar riesgo, confirmar, ofrecer el hueco a una cola, medir), la integración local argentina (Mercado Pago, ARCA, obras sociales) y una experiencia de reserva sin app para el paciente.

**Vacío frecuente en Argentina:** los líderes verticales esconden el precio (Dentalink, Reservo, Dentidesk) y están diseñados para clínica, no para el odontólogo solo.

**Consecuencia para este proyecto:** no competimos por cantidad de funcionalidades. Competimos por foco: un consultorio, un profesional, un problema (no-show) y una entrega en 4 días.

## 5. Funcionalidades necesarias (MVP)

| # | Funcionalidad | Por qué es MVP |
|---|---|---|
| 1 | Login simple del odontólogo | Único usuario con acceso a la agenda. |
| 2 | Agenda diaria y semanal | Es la pantalla central de trabajo. |
| 3 | Alta y edición de pacientes (mínima) | Requisito para asociar turnos. |
| 4 | Turno con estado y control de solapamiento | Evita el error más caro de una agenda. |
| 5 | Reserva online del paciente por enlace público | Pedido explícito: el paciente también agenda. |
| 6 | Recordatorio por correo (simulado en local) | Dispara el ciclo de confirmación. |
| 7 | Link de confirmación/cancelación con token | Convierte la respuesta del paciente en estado. |
| 8 | Lista de espera con aviso automático al liberarse un turno | Ataca el no-show directamente. |
| 9 | Vista de turnos por estado | Permite ver confirmados, sin respuesta, cancelados y ausentes. |
| 10 | Datos seed simples | Demo reproducible sin carga manual. |
| 11 | Tests JUnit con H2 en memoria | Verificación sin instalar PostgreSQL. |

## 6. Funcionalidades opcionales (post-v1)

- WhatsApp real (Business API) y envío real por SMTP.
- Odontograma y periodontograma.
- Cobros, señas y Mercado Pago.
- Multi-profesional y multi-sede.
- Reprogramación automática del hueco liberado.
- Reportes de ausentismo y score de riesgo por paciente.
- Recuperación de pacientes inactivos y campañas.

## 7. Reglas de negocio

1. **Cancelación:** el paciente puede cancelar hasta **24hs antes** del turno. Después, la cancelación queda registrada como ausencia.
2. **Solapamiento:** no puede existir más de un turno activo en la misma franja horaria.
3. **Duración:** fija en **30 minutos** para la v1, configurable en el modelo. Duración variable queda para después.
4. **Estados del turno:** `PENDIENTE` → `CONFIRMADO` | `CANCELADO` | `AUSENTE` | `ATENDIDO`. Las transiciones inválidas se rechazan.
5. **Token de confirmación:** único por turno, de un solo uso y con vencimiento. Un turno ya cancelado o pasado no puede confirmarse.
6. **Lista de espera:** orden FIFO. Al liberarse un turno, el sistema **avisa automáticamente** al primero de la lista.
7. **Reserva online:** solo muestra horarios realmente libres, según la agenda y la duración fija.
8. **Un turno pasado no se confirma ni se cancela** desde el link del paciente.

## 8. Integraciones

| Integración | v1 | Detalle |
|---|---|---|
| Email | **Simulada** | El mensaje se persiste y se muestra en una bandeja de salida dentro de la app. No hay SMTP ni envío real. |
| WhatsApp | No | Post-v1. |
| Mercado Pago / ARCA | No | Fuera de alcance. |
| Google Calendar | No | Fuera de alcance. |
| Base de datos | PostgreSQL en ejecución | Local, sin hosting pago. |
| Base de datos en tests | **H2 en memoria** | Modo compatible con PostgreSQL, para correr tests sin instalar nada. |

**Decisión:** la demo end-to-end se hace desde la bandeja de salida simulada: se genera el correo, se abre el link y se confirma o cancela el turno.

## 9. Restricciones

| Tipo | Restricción |
|---|---|
| Backend | Java + Spring Boot + API REST + Gradle + Hibernate (JPA) |
| Base de datos | PostgreSQL para ejecutar la app; **H2 en memoria (modo PostgreSQL) para tests** |
| Frontend | TypeScript vanilla, sin frameworks, lo mínimo posible |
| Tests | JUnit |
| Ejecución | Local, sin hosting pago |
| Datos | Ficticios en ejemplos, tests y capturas; sin datos reales de pacientes |
| Plazo | **Martes 24/11/2026** (4 días contando el 21/11) |
| Presupuesto | Mínimo |
| Visual | Política `deferred`: se decide en el primer change visual |

## 10. Riesgos

| # | Riesgo | Severidad | Mitigación propuesta |
|---|---|---|---|
| R1 | 4 días para Discovery + KB + roadmap + implementación | Crítico | Recortar a los 11 ítems del MVP; nada más entra. |
| R2 | El correo simulado no valida entregabilidad ni comportamiento real del paciente | Alto | Declararlo explícitamente; la demo se hace desde la bandeja de salida. |
| R3 | El canal elegido (email) es el más débil frente a WhatsApp para reducir no-show | Alto | Documentar como decisión consciente y candidato #1 post-v1. |
| R4 | Datos sensibles de salud sin roles ni cifrado | Alto en producción, aceptable en demo local | Un solo usuario, base local, sin exposición a internet. |
| R5 | PostgreSQL local agrega fricción de setup para la demo | Medio | H2 en tests + script de arranque + datos seed. |
| R6 | Sin reserva online el producto no compite en adquisición | Medio | Ya corregido: la reserva online entra al MVP. |
| R7 | **Sumar reserva online del paciente + lista de espera automática + login en 4 días** | **Crítico** | Priorizar el camino: agenda → turno → reserva → recordatorio → confirmación → lista de espera. Si el tiempo aprieta, la lista de espera automática es lo primero que se simplifica. |
| R8 | El flujo del paciente depende de un correo que en local no sale | Medio | Bandeja de salida simulada como parte de la demo. |

## 11. Preguntas abiertas

**Resueltas con el usuario (2026-11-21):**

- ¿Alcanza con bandeja de salida simulada? **Sí, alcanza.**
- ¿La entrega incluye datos seed? **Sí, simples.**
- ¿Login simple del odontólogo o app abierta? **Login simple.**
- ¿Duración fija de 30 o 60 minutos? **30 minutos.**
- ¿La lista de espera avisa automáticamente o el odontólogo decide? **Avisa automáticamente.**

**Pendientes reales (a resolver durante la implementación):**

- ¿Qué datos mínimos pide la reserva online del paciente? Propuesta: nombre, apellido, DNI, teléfono, email.
- ¿La reserva online requiere validación de email o se acepta el dato tal cual?
- ¿Ventana de reserva? Propuesta: desde el día siguiente hasta 30 días vista.
- ¿El paciente puede reprogramar solo o solo cancelar y volver a reservar?
- ¿Qué pasa si dos pacientes reservan el mismo horario a la vez? Propuesta: constraint único + respuesta 409.
- ¿La lista de espera avisa por correo simulado y espera respuesta, o reasigna sola el turno?

---

## Supuestos declarados

| # | Supuesto |
|---|---|
| S1 | El paciente no tiene cuenta ni login; el token del turno es su identidad. |
| S2 | "Correo simulado" = persistir el mensaje, verlo en bandeja de salida y abrir el link desde ahí. |
| S3 | Un solo odontólogo, sin roles ni permisos. |
| S4 | La reserva online es pública (sin login) y crea el paciente si no existe (por DNI). |
| S5 | El MVP se recorta a los 11 ítems de la sección 5; el resto va al backlog. |
| S6 | La demo end-to-end se ejecuta desde la bandeja de salida, no desde una casilla real. |
