# Proposal

## Why

El odontólogo es el único operador administrativo del sistema y hoy no puede iniciar sesión: la configuración actual usa `httpBasic` contra el usuario genérico que genera Spring Boot, no contra la entidad `Dentist` que C-02 ya persiste. Sin autenticación real, la agenda, los pacientes, la lista de espera y la bandeja de salida no tienen dueño ni protección, y los changes C-05 y posteriores no pueden distinguir al actor `DENTIST` de un paciente público.

C-02 dejó el modelo, el `Dentist` administrador desde `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`, y la regla de rutas protegidas. C-03 cierra el acceso con sesión y habilita el resto del roadmap.

## What Changes

- Incorporar `POST /api/auth/login` con email y contraseña, validación BCrypt contra `Dentist.password_hash` y mensaje de error genérico ante credenciales inválidas.
- Incorporar `POST /api/auth/logout` con invalidación de sesión y borrado de la cookie.
- Incorporar `GET /api/auth/me` para que el frontend administrativo conozca la sesión activa.
- Reemplazar la cadena de seguridad actual por una basada en sesión: cookie `HttpOnly` de 30 minutos de inactividad, `/api/public/**` y páginas públicas abiertas, el resto autenticado.
- Rechazar la autenticación de un `Dentist` con `deleted_at` informado.
- Definir en C-03 un `PasswordEncoder` BCrypt propio y una `UserDetailsService` respaldada por `DentistRepository`.
- Mantener CSRF deshabilitado de forma explícita y documentada, con cookie `SameSite=Lax`, registrando el trade-off en `design.md`.
- Documentar la variable `APP_ADMIN_SEED` que hoy controla el seed de C-02, sin modificar el código de C-02.
- Agregar pruebas deterministas de login, credenciales inválidas, ruta protegida sin sesión, expiración de sesión y odontólogo con baja lógica.

Fuera de alcance: interfaz de login (C-12), rate limiting o bloqueo de cuenta, remember-me, RBAC multirol, recuperación de contraseña, cambios en el seed de C-02 y cualquier endpoint de agenda o reserva.

## Capabilities

### New Capabilities

- `autenticacion-odontologo`: sesión del odontólogo con email y contraseña, cookie de 30 minutos de inactividad, rutas públicas declaradas, rutas administrativas protegidas con 401 y rechazo de identidades dadas de baja.

### Modified Capabilities

- Ninguna. `fundacion-monorepo` ya declara las rutas públicas y el 401 para rutas no públicas; C-03 las implementa sin cambiar su texto. `semilla-inicial` no cambia: el seed de C-02 queda intacto y solo se documenta su variable de activación.

## Impact

- Nuevo paquete `com.turnosodontologia.auth` con controlador, servicio de sesión, `UserDetailsService` y configuración de seguridad.
- `common/SecurityConfiguration` se reemplaza por la configuración de seguridad de C-03; no se modifica ninguna clase de C-02 (`seed/`, `dentist/`, entidades ni repositorios).
- Sin cambios de esquema ni de dependencias: la sesión vive en memoria del servidor y `spring-security-test` ya está declarado.
- Documentación afectada: `README.md` (variables de entorno y arranque) y `knowledge-base/08_arquitectura_propuesta.md` (§Variables de entorno y §Seguridad) para incluir `APP_ADMIN_SEED` y el mecanismo de sesión.
- Contrato nuevo para el frontend: los tres endpoints de autenticación y el formato de error 401 que consumirá C-12.
