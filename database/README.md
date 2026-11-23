# Base de datos

El entorno local usa PostgreSQL mediante `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.

Las pruebas usan H2 en modo compatibilidad PostgreSQL desde `application-test.yml`, por lo que no requieren una instancia PostgreSQL local.

## Schedule rules (`C-04`)

Antes de arrancar el perfil `local`, el esquema de agenda debe existir porque `application-local.yml` mantiene `ddl-auto: validate`.

Precondiciones:

- PostgreSQL local disponible y la base indicada por `SPRING_DATASOURCE_URL` ya contiene las tablas base creadas por C-01/C-02 (incluido `dentists`).
- Variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` configuradas en el entorno local.
- `psql` instalado y accesible.

Aplicación idempotente:

```powershell
psql "$env:SPRING_DATASOURCE_URL" -U "$env:SPRING_DATASOURCE_USERNAME" -f database/schedule-rules.sql
```

Si la URL JDBC es la forma `jdbc:postgresql://host:puerto/base`, convertirla a una URL/DSN aceptada por `psql` o ejecutar con el host, puerto y base explícitos:

```powershell
psql -h HOST -p PUERTO -d BASE -U USUARIO -f database/schedule-rules.sql
```

El script crea únicamente `schedule_configurations` y `schedule_windows`. Después arrancar con `SPRING_PROFILES_ACTIVE=local`; Hibernate debe pasar `ddl-auto: validate` sin reportar diferencias.

El DDL fue obtenido del esquema que Hibernate genera con `application-test.yml` (`H2 MODE=PostgreSQL`, `create-drop`) y usa los tipos reales `uuid`, `timestamp(6) with time zone`, `time(6)` y `varchar(10)`. No incorpora Flyway, Liquibase ni `schema.sql`.

**Estado de integración/Handoff:** no se pudo verificar la aplicación ni el arranque `local` en PostgreSQL en esta sesión: no se encontró `psql`/`pg_isready` en el PATH ni una instancia comprobada. Esta comprobación queda explícitamente pendiente para el líder, que posee PostgreSQL. No publicar esta verificación como aprobada hasta que el líder aplique el script y arranque el perfil `local` con las credenciales locales.

Checklist de aceptación para el PR del líder:

- [ ] Ejecutar `psql` sobre una base que contenga las tablas de C-01/C-02.
- [ ] Confirmar `schedule_configurations` y `schedule_windows`, UNIQUE de `dentist_id` y FK a `dentists`.
- [ ] Arrancar con `SPRING_PROFILES_ACTIVE=local` y `ddl-auto: validate` sin diferencias reportadas.
- [ ] Ejecutar una comprobación de paridad H2/PostgreSQL con datos ficticios y registrar el resultado sin credenciales ni datos reales.
- [ ] Marcar C-04 `[x]` en `CHANGES.md` y archivar el change solo después de completar este checklist.

Todos los usuarios, pacientes y datos de demostración deben ser ficticios. Nunca guardar datos reales de pacientes ni credenciales reales en este directorio o en el repositorio.
