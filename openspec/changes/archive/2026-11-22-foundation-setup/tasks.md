# Tasks

## 1. Pruebas TDD de perfiles y seguridad

- [x] 1.1 Escribir primero pruebas de seguridad para `/api/health`, `index.html`, `admin.html` y JavaScript públicos sin sesión; verificar que las rutas protegidas respondan 401.
- [x] 1.2 Escribir primero pruebas del perfil `test` con H2 en modo compatibilidad PostgreSQL; verificar carga del contexto sin PostgreSQL instalado.
- [x] 1.3 Escribir primero la prueba exacta de `GET /api/health` con HTTP 200 y JSON exacto `{"status":"UP"}`.
- [x] 1.4 Escribir primero prueba de arranque local sin variables; verificar que falle con un mensaje claro que identifique las variables faltantes.

## 2. Estructura y backend base

- [x] 2.1 Crear las áreas `backend/`, `frontend/`, `database/` y `knowledge-base/`, preservando la documentación existente; verificar que las cuatro áreas existan.
- [x] 2.2 Crear el proyecto Spring Boot 3.3.13 en `backend/` con Gradle Wrapper y `JavaCompile.options.release = 17`; verificar que JDK 17+ pueda ejecutar tareas y compilación.
- [x] 2.3 Agregar las dependencias `web`, `data-jpa`, `validation`, `security`, `h2`, `postgresql` y test; verificar resolución y compilación con `./gradlew build`.
- [x] 2.4 Configurar el paquete base exacto `com.turnosodontologia` y registrar los paquetes `auth`, `patient`, `appointment`, `schedule`, `waitinglist`, `outbox` y `common`.

## 3. Perfiles y configuración de datos

- [x] 3.1 Crear `application-local.yml` con PostgreSQL mediante variables de entorno; verificar arranque válido y fallo claro sin variables obligatorias.
- [x] 3.2 Crear `application-test.yml` con H2 en modo compatibilidad PostgreSQL; verificar que las pruebas pasen sin PostgreSQL.
- [x] 3.3 Crear `.env.example` con las variables declaradas por la arquitectura y sin credenciales reales; verificar que `SPRING_PROFILES_ACTIVE` local/test quede explicado.

## 4. Implementación mínima verde de API y seguridad

- [x] 4.1 Implementar `GET /api/health` público con la respuesta exacta HTTP 200 y JSON `{"status":"UP"}`.
- [x] 4.2 Implementar la PermitList de Security para `/api/health`, `index.html`, `admin.html` y JavaScript estático; verificar 401 sin sesión en rutas protegidas.
- [x] 4.3 Agregar casos verdes adicionales y refactorizar sin alterar el comportamiento probado.

## 5. Frontend base y empaquetado

- [x] 5.1 Crear `frontend/tsconfig.json`, `frontend/package.json` con TypeScript como dependencia y script `build`, código TypeScript vanilla y `index.html`/`admin.html`; verificar el build.
- [x] 5.2 Configurar el resultado compilado bajo `frontend/dist/` y servirlo desde Spring Boot como recursos estáticos; verificar acceso público a los recursos.

## 6. Documentación e ignore

- [x] 6.1 Crear o actualizar README con carga de variables en PowerShell, perfil `local` por defecto para ejecución y `test` para pruebas; documentar backend y frontend sin inventar scripts alternativos.
- [x] 6.2 Agregar `.gitignore` conservando el existente y excluyendo `.gradle/`, `build/`, `frontend/dist/` y `node_modules/`.
- [x] 6.3 Crear un README corto en `database/` explicando PostgreSQL local, H2 de test y datos ficticios; verificar su coherencia con la configuración.
- [x] 6.4 Crear un README corto en `knowledge-base/` explicando su índice y la responsabilidad de las fuentes de dominio.

## 7. Verificación integrada

- [x] 7.1 Ejecutar `./gradlew test` con perfil `test` y verificar que no requiera PostgreSQL.
- [x] 7.2 Ejecutar `./gradlew build` con JDK 17+ y verificar compilación, pruebas y artefactos verdes.
- [x] 7.3 Revisar datos reales, secretos, credenciales, perfiles y rutas públicas/privadas; verificar que solo existan valores ficticios o variables locales.

## Workflow follow-up

- Cuando el usuario solicite implementación, iniciar con el workflow de apply de OpenSpec.
- Archivar el change únicamente después de aplicar y verificar C-01 según el proceso del proyecto.
