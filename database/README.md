# Base de datos

El entorno local usa PostgreSQL mediante `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.

Las pruebas usan H2 en modo compatibilidad PostgreSQL desde `application-test.yml`, por lo que no requieren una instancia PostgreSQL local.

Todos los usuarios, pacientes y datos de demostración deben ser ficticios. Nunca guardar datos reales de pacientes ni credenciales reales en este directorio o en el repositorio.
