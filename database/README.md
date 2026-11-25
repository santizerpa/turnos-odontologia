# Base de datos

El entorno local usa PostgreSQL mediante `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. Las pruebas usan H2 en modo compatibilidad PostgreSQL desde `application-test.yml` (`create-drop`), por lo que no requieren una instancia PostgreSQL local.

## Cómo se crea el esquema

El proyecto **no usa Flyway, Liquibase ni `schema.sql`**. Hay dos configuraciones:

| Perfil | `ddl-auto` | Qué hace |
|---|---|---|
| `test` | `create-drop` | Crea todas las tablas en H2 al iniciar cada contexto y las borra al terminar |
| `local` | `validate` | No crea ni modifica nada: si una tabla o columna no coincide con las entidades JPA, la aplicación se detiene |

Como `local` solo valida, una base PostgreSQL vacía necesita **un primer arranque** con `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, que deja que Hibernate cree todas las tablas. Después se quita la variable y los arranques siguientes validan. El procedimiento paso a paso, con los comandos de PowerShell y de shell Unix, está en la sección 3.4 del [README](../README.md).

Tablas que debe tener la base al terminar el primer arranque: `dentists`, `patients`, `appointments`, `appointment_tokens`, `appointment_events`, `schedule_configurations`, `schedule_windows`, `schedule_blocks`, `waiting_list_entries` y `outbox_messages`.

Comprobación rápida con `psql` (te pide la contraseña):

```powershell
psql -h localhost -p 5432 -U odontologia -d odontologia -c "\dt"
```

## `schedule-rules.sql` (C-04)

`database/schedule-rules.sql` contiene el DDL de las dos tablas de agenda (`schedule_configurations` y `schedule_windows`), obtenido del esquema que Hibernate genera con `application-test.yml`. Usa los tipos reales `uuid`, `timestamp(6) with time zone`, `time(6)` y `varchar(10)`.

Con el procedimiento de primer arranque ya **no hace falta aplicarlo**: Hibernate crea esas tablas junto con las demás. Queda como referencia para comparar tipos y restricciones contra la base real. Solo cubre estas dos tablas; no crea `dentists` ni el resto del esquema, por lo que no alcanza por sí solo para arrancar el perfil `local`.

## Estado de la verificación en PostgreSQL real

La suite automática se ejecuta únicamente sobre H2. La evidencia contra un PostgreSQL real se registra en `docs/qa/` (ver `docs/qa/verification-index.md`) con el motor, la versión, los comandos y el resultado, sin credenciales ni datos reales.

Lista de comprobación (marcar solo con resultado observado):

- [X] Primer arranque en una base vacía con `SPRING_JPA_HIBERNATE_DDL_AUTO=update`: `Started TurnosOdontologiaApplication` y las diez tablas presentes (`\dt`).
- [X] Segundo arranque con la variable quitada: arranca con `ddl-auto: validate` sin diferencias.
- [X] `schedule_configurations` tiene `dentist_id` único y clave foránea a `dentists`; `schedule_windows` guarda `time` y `varchar` como indica `schedule-rules.sql`.
- [X] Login del odontólogo ficticio, `GET /api/auth/me`, logout y 401 posterior.
- [X] `GET /api/schedule` responde 200 y la agenda por defecto existe desde el arranque.
- [X] Reserva pública, y segunda reserva de la misma franja con 409 (unicidad de `active_slot`).
- [ ] Cancelación (`active_slot` queda `NULL`) y nueva reserva de la misma franja.
- [ ] Cancelación con una entrada `WAITING` elegible: la entrada pasa a `NOTIFIED` y aparece un mensaje en la bandeja.
- [ ] `POST /api/outbox/reminders/run` y `POST /api/outbox/dispatch` sobre turnos de 24 a 48 horas.
- [ ] Carga de demo (`APP_DEMO_SEED=true`) sobre una base limpia y segundo arranque el mismo día.

Mientras haya casillas sin marcar, no se afirma paridad H2/PostgreSQL en la documentación ni en el video.

Todos los usuarios, pacientes y datos de demostración deben ser ficticios. Nunca guardar datos reales de pacientes ni credenciales reales en este directorio o en el repositorio.