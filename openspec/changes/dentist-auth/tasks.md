# Tasks

## 1. Preparación y red de seguridad

- [ ] 1.1 Ejecutar `./gradlew test` en `backend/` antes de tocar código y registrar la línea base de pruebas verdes de C-01/C-02; verificar que la suite termina en `BUILD SUCCESSFUL` sin fallos previos.
- [ ] 1.2 Crear un soporte de pruebas de autenticación que persista un `Dentist` ficticio con contraseña BCrypt usando el `PasswordEncoder` del contexto y un `MockMvc` con perfil `test`; verificar que compila y que el contexto de prueba carga sin depender de `APP_ADMIN_*`.

## 2. Login del odontólogo

- [ ] 2.1 Escribir primero las pruebas de `POST /api/auth/login`: login correcto (200 con id, email, nombre y apellido), contraseña incorrecta (401 con mensaje genérico) y email inexistente (401 con el mismo mensaje); verificar que quedan rojas.
- [ ] 2.2 Implementar el `PasswordEncoder` BCrypt de `auth/`, el `DentistUserDetailsService` sobre `DentistRepository`, el principal de sesión, el `AuthenticationManager` y el controlador de login; verificar que 2.1 pasa y que la respuesta entrega cookie `HttpOnly` sin `password_hash`.
- [ ] 2.3 Escribir primero las pruebas de identidad dada de baja y hash inválido: `Dentist` con `deleted_at` informado responde 401 genérico y conserva su `deleted_at`, y un `password_hash` en texto plano no autentica; verificar que quedan rojas.
- [ ] 2.4 Implementar el rechazo de `deleted_at` y la normalización de email de entrada (trim + lowercase) sin modificar entidades ni repositorios de C-02; verificar que 2.3 pasa.
- [ ] 2.5 Triangular con un caso adicional: email ingresado con mayúsculas y espacios y contraseña correcta; verificar 200 y que el `Dentist` persistido no se modifica.

## 3. Sesión: consulta, cierre y expiración

- [ ] 3.1 Escribir primero las pruebas de `GET /api/auth/me` con sesión (200 con perfil, sin `password_hash`) y de `POST /api/auth/logout` (sesión invalidada y `me` posterior responde 401); verificar que quedan rojas.
- [ ] 3.2 Implementar `me` y `logout` guardando y limpiando el `SecurityContext` en la `HttpSession`; verificar que 3.1 pasa.
- [ ] 3.3 Escribir primero las pruebas deterministas de expiración: cookie `JSESSIONID` sin sesión viva responde 401, y la configuración resuelve `server.servlet.session.timeout` en `30m` con cookie `HttpOnly` y `SameSite=Lax`; verificar que quedan rojas.
- [ ] 3.4 Configurar timeout de sesión y atributos de cookie según `design.md` D5/D9; verificar que 3.3 pasa sin usar `Thread.sleep` ni esperas reales.

## 4. Rutas protegidas, rutas públicas y CSRF

- [ ] 4.1 Escribir primero las pruebas de rutas: `GET /api/auth/me` y `GET /api/appointments` sin sesión responden 401 JSON sin redirección, `/`, `/index.html`, `/admin.html`, `/js/**` y `/api/health` responden sin sesión, y una ruta bajo `/api/public/` no responde 401 por falta de sesión; verificar que quedan rojas.
- [ ] 4.2 Implementar la cadena de seguridad en `auth/config` con el `AuthenticationEntryPoint` JSON, la lista de matchers de `design.md` D11 y `/api/auth/login` accesible; retirar `common/SecurityConfiguration` en el mismo paso y verificar que 4.1 pasa y que la suite de C-01/C-02 sigue verde.
- [ ] 4.3 Escribir primero las pruebas del trade-off CSRF: `POST /api/auth/logout` con sesión válida y sin token CSRF se procesa, y la cookie de sesión declara `SameSite=Lax`; verificar que quedan rojas.
- [ ] 4.4 Deshabilitar CSRF de forma explícita con comentario que referencia `design.md` D2 y ajustar los atributos de cookie; verificar que 4.3 pasa.

## 5. Documentación operativa

- [ ] 5.1 Actualizar `README.md` con `APP_ADMIN_SEED` en la tabla de variables, la aclaración de que sin esa variable no existe odontólogo y que la sesión vive en memoria; verificar que los comandos documentados coinciden con los que se ejecutan en la verificación manual.
- [ ] 5.2 Actualizar `knowledge-base/08_arquitectura_propuesta.md` (§Variables de entorno y §Seguridad) con `APP_ADMIN_SEED`, el mecanismo de sesión de 30 minutos y la decisión de CSRF; verificar que la tabla coincide con `README.md` y con `design.md`.

## 6. Verificación de integración

- [ ] 6.1 Ejecutar la suite completa con `./gradlew test` y verificar que todas las pruebas nuevas y previas pasan.
- [ ] 6.2 Ejecutar `./gradlew build` y verificar compilación, empaquetado y suite en verde.
- [ ] 6.3 Verificar manualmente en perfil `local` con PostgreSQL y `APP_ADMIN_SEED=true`: login con las credenciales ficticias, `GET /api/auth/me` con la cookie, logout y posterior 401; registrar el resultado o el bloqueo por esquema faltante en la evidencia del change.
- [ ] 6.4 Revisar el diff y verificar que no se modificó código de C-02 (`seed/`, `dentist/`, entidades y repositorios) ni se expone `password_hash` en ninguna respuesta.

## Workflow follow-up

- Ejecutar `openspec status --change dentist-auth --json` y `openspec validate dentist-auth --strict` hasta obtener resultado válido.
- Archivar el change cuando la revisión del proyecto lo apruebe.
- Actualizar el checkbox de C-03 en `CHANGES.md` solo si el líder lo autoriza.
