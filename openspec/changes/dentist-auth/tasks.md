# Tasks

## 1. Preparación y red de seguridad

- [x] 1.1 Ejecutar `./gradlew test` en `backend/` antes de tocar código y registrar la línea base de pruebas verdes de C-01/C-02; verificar que la suite termina en `BUILD SUCCESSFUL` sin fallos previos.
- [x] 1.2 Crear un soporte de pruebas de autenticación que persista un `Dentist` ficticio con contraseña BCrypt usando el `PasswordEncoder` del contexto y un `MockMvc` con perfil `test`; verificar que compila y que el contexto de prueba carga sin depender de `APP_ADMIN_*`.

## 2. Login del odontólogo

- [x] 2.1 Escribir primero las pruebas de `POST /api/auth/login`: login correcto (200 con id, email, nombre y apellido), contraseña incorrecta (401 con mensaje genérico) y email inexistente (401 con el mismo mensaje); verificar que quedan rojas.
- [x] 2.2 Implementar el `PasswordEncoder` BCrypt de `auth/`, el `DentistUserDetailsService` sobre `DentistRepository`, el principal de sesión, el `AuthenticationManager` y el controlador de login; verificar que 2.1 pasa y que la respuesta no contiene `password_hash`.
- [x] 2.3 Escribir primero las pruebas de identidad dada de baja y hash inválido: `Dentist` con `deleted_at` informado responde 401 genérico y conserva su `deleted_at`, y un `password_hash` en texto plano no autentica; verificar que quedan rojas.
- [x] 2.4 Implementar el rechazo de `deleted_at` y la normalización de email de entrada (trim + lowercase) sin modificar entidades ni repositorios de C-02; verificar que 2.3 pasa.
- [x] 2.5 Triangular con un caso adicional: email ingresado con mayúsculas y espacios y contraseña correcta; verificar 200 y que el `Dentist` persistido no se modifica.

## 3. Sesión: consulta, cierre y expiración

- [x] 3.1 Escribir primero las pruebas de `GET /api/auth/me` con sesión (200 con perfil, sin `password_hash`) y de `POST /api/auth/logout` (sesión invalidada y `me` posterior responde 401); verificar que quedan rojas.
- [x] 3.2 Implementar `me` y `logout` guardando el contexto con `HttpSessionSecurityContextRepository.saveContext(...)` y limpiando la sesión al cerrar; verificar que 3.1 pasa.
- [x] 3.3 Escribir primero las pruebas deterministas de expiración con `MockMvc`: la configuración resuelve `server.servlet.session.timeout` en `30m`, y una sesión que dejó de ser válida (después de logout o con un `JSESSIONID` que no corresponde a ninguna sesión viva) responde 401 en `/api/auth/me`. No se simula el paso de 30 minutos ni se usa `Thread.sleep`; verificar que quedan rojas.
- [x] 3.4 Configurar `server.servlet.session.timeout=30m` y verificar que 3.3 pasa.
- [x] 3.5 Escribir primero la prueba de atributos reales de la cookie con `@SpringBootTest(webEnvironment = RANDOM_PORT)`: login contra el servidor embebido y lectura de la cabecera `Set-Cookie` real, exigiendo `HttpOnly` y `SameSite=Lax`. `MockMvc` no emite esa cabecera, por eso este escenario necesita puerto aleatorio; verificar que queda roja.
- [x] 3.6 Configurar `server.servlet.session.cookie.http-only=true` y `same-site=lax` hasta que 3.5 pase; verificar que 3.3 y 3.5 quedan verdes y que no se agregó ninguna espera por reloj.

## 4. Rutas protegidas, rutas públicas y CSRF

- [x] 4.1 Escribir primero las pruebas de rutas: `GET /api/auth/me` y `GET /api/appointments` sin sesión responden 401 JSON sin redirección; `/`, `/index.html`, `/admin.html`, `/js/**`, `/api/health` y `POST /api/auth/login` responden sin sesión; una ruta bajo `/api/public/` no responde 401 por falta de sesión; verificar que quedan rojas.
- [x] 4.2 Implementar la cadena de seguridad en `auth/config` con el `AuthenticationEntryPoint` JSON, los matchers de `design.md` D11 (`/api/auth/login` incluido como `permitAll`) y retirar `common/SecurityConfiguration` en el mismo paso; verificar que 4.1 pasa y que la suite de C-01/C-02 sigue verde.
- [x] 4.3 Escribir primero la prueba del trade-off CSRF: `POST /api/auth/logout` con sesión válida y sin token CSRF se procesa; verificar que queda roja.
- [x] 4.4 Deshabilitar CSRF de forma explícita con comentario que referencia `design.md` D2; verificar que 4.3 pasa.

## 5. Verificación de integración

- [x] 5.1 Ejecutar la suite completa con `./gradlew test` y verificar que todas las pruebas nuevas y previas pasan.
- [x] 5.2 Ejecutar `./gradlew build` y verificar compilación, empaquetado y suite en verde.
- [ ] 5.3 Verificar manualmente en perfil `local` con PostgreSQL y `APP_ADMIN_SEED=true`: login con las credenciales ficticias, `GET /api/auth/me` con la cookie, logout y posterior 401; registrar el resultado o el bloqueo por esquema faltante en la evidencia del change.
- [x] 5.4 Revisar el diff y verificar que no se modificó código de C-02 (`seed/`, `dentist/`, entidades y repositorios), ni `ddl-auto`, ni `CHANGES.md`, y que ninguna respuesta expone `password_hash`.

## 6. Documentación mínima (última tarea del change)

- [x] 6.1 Agregar una única fila de `APP_ADMIN_SEED` en la tabla de variables de `README.md` y una línea sobre la sesión en memoria; verificar que la fila coincide con la de `knowledge-base/08_arquitectura_propuesta.md`.
- [x] 6.2 Agregar una única fila de `APP_ADMIN_SEED` en §Variables de entorno y una línea del mecanismo de sesión de 30 minutos en §Seguridad de `knowledge-base/08_arquitectura_propuesta.md`; verificar que coincide con `README.md` y con `design.md`.

## Workflow follow-up

- Ejecutar `openspec status --change dentist-auth --json` y `openspec validate dentist-auth --strict` hasta obtener resultado válido.
- Archivar el change cuando la revisión del proyecto lo apruebe.
- Actualizar el checkbox de C-03 en `CHANGES.md` solo si el líder lo autoriza.
