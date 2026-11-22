# Design

## Context

Véase `proposal.md` — Why. El punto de partida técnico condiciona el diseño:

- `common/SecurityConfiguration` usa `httpBasic` con un `AuthenticationEntryPoint` que responde 401 y `csrf` deshabilitado. No hay `UserDetailsService`, por lo que Spring Boot autoconfigura un usuario en memoria y el login nunca consulta `Dentist`.
- C-02 dejó `Dentist` (`email` único, `password_hash`, `first_name`, `last_name`, auditoría con `deleted_at`), `DentistRepository.findByEmail(...)` y `AuditableEntity.isDeleted()`.
- El seed administrativo existe en `seed/AdminDentistSeeder` y corre solo con `APP_ADMIN_SEED=true`; **no se modifica en C-03**.
- El perfil `test` usa H2 y no define `APP_ADMIN_*`, así que las pruebas no pueden depender del seed: cada prueba crea su propio `Dentist`.
- `spring-security-test` ya está en `build.gradle`; no se agregan dependencias.

## Goals / Non-Goals

**Goals:**

- Autenticación por sesión de servidor con cookie, consumible por el frontend TypeScript vanilla de C-12.
- Un único punto de verificación de credenciales que consulte `Dentist` y respete la baja lógica.
- Cobertura determinista de los cuatro casos exigidos por el roadmap más el caso `deleted_at`, sin `Thread.sleep`.

**Non-Goals:**

- UI de login, manejo de sesión expirada en pantalla y fetch con cookies: C-12.
- Rate limiting, bloqueo por intentos fallidos, remember-me, recuperación de contraseña y RBAC multirol.
- Sesiones distribuidas o persistidas (Redis/JDBC): la sesión vive en memoria del servidor.
- Cualquier cambio en entidades, repositorios, seed o configuración de C-02.

## Decisions

### D1 — Login como API JSON con sesión de servidor

`POST /api/auth/login` recibe JSON, delega en un `AuthenticationManager` con `DaoAuthenticationProvider` y guarda el `SecurityContext` en la `HttpSession` mediante `HttpContextRepository.saveContext(...)` (Spring Security 6 no lo hace implícitamente con `requireExplicitSave`). `GET /api/auth/me` lee el contexto; `POST /api/auth/logout` invalida la sesión y borra la cookie.

Alternativa descartada: `formLogin` de Spring. Arrastra redirects, parámetros de formulario y un login HTML que el frontend vanilla de C-12 no necesita; además impediría devolver 401 JSON homogéneo en `/api/**`.

### D2 — CSRF deshabilitado de forma explícita, con cookie `SameSite=Lax`

Se mantiene `csrf.disable()` con un comentario que referencia este diseño. Mitigación: cookie `SameSite=Lax` (bloquea POST cross-site), API sin cookies de terceros y despliegue local de un solo usuario. Riesgo residual aceptado y registrado para revisión en C-14.

Alternativas descartadas: `CookieCsrfTokenRepository` (obliga a que C-03 y C-12 manejen el token antes de tener UI, y agrega ruido a todas las pruebas) y CSRF solo en rutas administrativas (mismo costo que habilitarlo completo, con reglas más difíciles de auditar).

### D3 — `/admin.html` queda abierto como shell vacío

La página administrativa se sirve sin sesión porque no contiene datos: todo lo que muestra llega por `/api/**` protegido. Esto conserva el comportamiento ya declarado en `fundacion-monorepo` y evita un redirect que C-12 no está listo para manejar.

Alternativa descartada: proteger `/admin.html` y redirigir a un login inexistente hasta C-12, lo que rompería la prueba de rutas públicas de C-01.

### D4 — 401 uniforme, sin redirects

Un `AuthenticationEntryPoint` propio escribe 401 con cuerpo JSON para cualquier ruta protegida, incluidas las de negocio como `/api/appointments`. No hay redirect a login en ningún caso: el frontend decide qué hacer con el 401. También se deja `/error` accesible para evitar bucles de dispatch de error.

Alternativa descartada: entry point que redirige según `Accept`/ruta; agrega ramas y C-12 puede resolverlo en cliente con el mismo 401.

### D5 — Sesión de 30 minutos de inactividad con cookie `HttpOnly`

Configuración: `server.servlet.session.timeout=30m`, `server.servlet.session.cookie.http-only=true`, `same-site=lax`, `secure=false` en local (documentado para producción). El timeout de Spring se cuenta desde el último acceso, que es exactamente "inactividad". Se conserva la protección de fijación de sesión por defecto (`changeSessionId`).

Alternativa descartada: expiración absoluta propia con `SessionRegistry` y un scheduler; más código y no es lo que pide la KB.

### D6 — Identidad de autenticación mapeada desde `Dentist`, sin tocar C-02

`DentistUserDetailsService` busca por email normalizado (trim + lowercase, igual que persiste el constructor de `Dentist`), y si `isDeleted()` es verdadero lanza `UsernameNotFoundException` para que `DaoAuthenticationProvider` lo oculte como `BadCredentials`. El `UserDetails` es un record propio con id, email, nombre y apellido; no se agregan setters ni campos a `Dentist`.

Alternativa descartada: hacer que `Dentist` implemente `UserDetails`; acopla el modelo de dominio a Spring Security y obliga a modificar una clase de C-02.

### D7 — `PasswordEncoder` BCrypt propio de C-03

C-03 declara su `@Bean BCryptPasswordEncoder` en la configuración de `auth/` y lo usa el `DaoAuthenticationProvider`. No se modifica `AdminSeedConfiguration`, que sigue instanciando el suyo. Ambos usan la fuerza por defecto (10), por lo que los hashes del seed verifican sin cambios.

Alternativa descartada: refactorizar el seed para inyectar el bean (prohibido por la decisión del usuario de no tocar C-02). Riesgo derivado: si alguien cambia la fuerza en un solo lado, los hashes siguen verificando (BCrypt guarda el costo en el propio hash), así que el acoplamiento es débil.

### D8 — Paquetes: `com.turnosodontologia.auth` sobre la estructura existente

Se agrega `auth/` con `config/` (seguridad, encoder, entry point), `controller/` (login, logout, me), `principal/` (`DentistUserDetailsService`, `DentistPrincipal`) y `service/` (login/logout). `common/SecurityConfiguration` se reemplaza por la configuración de `auth/config`; el resto de `common/` (health, auditoría, validador de perfil) no se mueve.

Alternativa descartada: crear `api/` y `config/` globales como sugiere la KB 08; el código real ya usa paquetes por dominio y mover `common/` excede C-03.

### D9 — Prueba determinista de expiración, sin `Thread.sleep`

Se cubre en dos niveles complementarios:

1. **Contrato de configuración**: prueba que el entorno resuelve `server.servlet.session.timeout` en `30m` y que la cookie declara `HttpOnly` y `SameSite=Lax`.
2. **Sesión inválida/vencida en la práctica**: se envía una cookie `JSESSIONID` que no corresponde a ninguna sesión viva y se exige 401 en `/api/auth/me`. Para el contenedor, una sesión expirada es indistinguible de una inexistente, así que la prueba ejercita el mismo camino sin esperar reloj.
3. **Refuerzo opcional** si se habilita control de concurrencia de sesiones: `SessionRegistry.expireNow()` sobre la `SessionInformation` del login y verificación de 401 en la siguiente solicitud.

Alternativas descartadas: `server.servlet.session.timeout=1s` con espera real (flaky y lenta) y `MockHttpSession` mutada a mano (no pasa por la cadena de seguridad real, no prueba nada útil).

### D10 — `APP_ADMIN_SEED` se documenta, no se cambia

Se documenta en `README.md` y en `knowledge-base/08_arquitectura_propuesta.md` que el seed de C-02 corre solo con `APP_ADMIN_SEED=true`, junto a `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`. Sin esa variable no hay odontólogo y el login no puede funcionar; queda explícito para el operador de la demo y para C-13.

Alternativa descartada: quitar el flag o sembrar siempre que existan credenciales; cambia comportamiento de C-02, que el usuario pidió no tocar.

### D11 — Orden de matchers y rutas públicas

El orden será: `/api/public/**`, `/api/health`, `/`, `/index.html`, `/admin.html`, `/js/**`, recursos estáticos de Spring, `/error` → `permitAll`; `anyRequest().authenticated()`. `/api/auth/login` queda dentro de `authenticated()` a nivel de matcher y se exceptúa con `permitAll` explícito para evitar ambigüedad.

Alternativa descartada: depender del orden implícito de los recursos estáticos; deja `/api/auth/login` inalcanzable y produce 401 confusos.

## Risks / Trade-offs

- [CSRF deshabilitado con cookie de sesión] → `SameSite=Lax`, API solo local, decisión registrada; revisar en C-14.
- [Sesión en memoria: reiniciar el backend desloguea] → Aceptado para demo de instancia única; documentado en README.
- [Una sesión abierta de un `Dentist` dado de baja sigue válida hasta 30 minutos] → Riesgo acotado por la ventana de inactividad; se documenta y se deja como mejora futura (invalidación por evento de baja).
- [Dos `PasswordEncoder` (seed de C-02 y bean de C-03)] → Ambos BCrypt con costo embebido en el hash; verificar en la prueba de login contra el hash del seed.
- [Las pruebas no pueden usar el seed porque `test` no define `APP_ADMIN_*`] → Cada prueba crea su `Dentist` ficticio con el encoder del contexto.
- [Perfil `local` usa `ddl-auto: validate` sin migraciones ni `schema.sql`] → La verificación manual de login contra PostgreSQL depende de un esquema preexistente; riesgo heredado de C-02, fuera de alcance de C-03 y a registrar en la evidencia de verificación.
- [401 uniforme puede ocultar un problema de configuración] → Las pruebas cubren rutas públicas y protegidas por separado para distinguir "sin sesión" de "ruta mal declarada".

## Migration Plan

1. Agregar `auth/` y su configuración de seguridad; retirar `common/SecurityConfiguration` en el mismo commit para que no convivan dos cadenas.
2. Ejecutar `./gradlew test`: la suite de C-01/C-02 debe seguir verde (la prueba de 401 sin sesión sigue siendo válida).
3. Verificación manual en perfil `local` con PostgreSQL y `APP_ADMIN_SEED=true` para comprobar login real contra el hash del seed.
4. Rollback: revertir el commit; no hay cambios de esquema ni datos que migrar. Las sesiones se pierden al reiniciar en cualquier caso.

## Open Questions

Ninguna que afecte specs, enfoque o desglose de tareas. La invalidación de sesiones de un odontólogo dado de baja y el endurecimiento de CSRF quedan como mejoras posteriores documentadas.
