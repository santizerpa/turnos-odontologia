# Arquitectura Propuesta

## Patrones aplicados

| Patrón | Uso | Por qué |
|---|---|---|
| Modular monolith | Backend único con paquetes por dominio | Entrega rápida y operación local simple |
| Repository/JPA | Acceso a datos | Menos SQL repetido y buen testeo |
| Service/Use case | Casos de reserva y confirmación | Reglas centralizadas |
| State machine explícita | Estados de turno | Evita transiciones inválidas |
| Servicio único de cancelación | `AppointmentService.cancel(...)` | Centraliza transición, `active_slot`, auditoría y evento |
| Domain event + listener transaccional | `AppointmentCancelledEvent` → lista de espera | Desacopla C-11 sin modificar C-05 |
| Outbox local | Mensajes simulados | Mantiene demo sin integración externa |
| Optimistic locking | Turnos | Evita conflictos y overwrite |
| Unique constraint | `active_slot` nullable | Defensa final contra doble reserva, portable a H2 y PostgreSQL |

## Eventos de dominio

- `AppointmentCancelledEvent` — payload `appointmentId`, `startAt`, `actor`.
- Se publica desde `AppointmentService.cancel(...)` solo si la transición a `CANCELLED` ocurrió.
- La lista de espera lo consume con `@TransactionalEventListener(AFTER_COMMIT)` y `@Transactional(REQUIRES_NEW)`, en una transacción nueva y sin modificar las clases de C-05.

## Estructura de directorios

```text
turnos-odontologia/
├── backend/
│   └── src/main/java/com/.../
│       ├── auth/
│       ├── patient/
│       ├── appointment/
│       ├── waitinglist/
│       ├── messaging/
│       ├── api/
│       └── config/
├── frontend/
│   └── src/
│       ├── admin/
│       ├── public-reservation/
│       ├── shared/
│       └── main.ts
├── database/
└── knowledge-base/
```

## Seguridad

- Autenticación: email y contraseña con contraseña hash BCrypt.
- Sesión: expira a los 30 minutos de inactividad y vive en memoria del servidor.
- Autorización: rutas administrativas autenticadas.
- Tokens: al menos 128 bits de entropía (32 bytes en base64url), hash SHA-256 persistido, vigencia hasta el inicio del turno o 7 días. El token no se consume al confirmar; confirmar y cancelar son idempotentes y el estado del turno impide efectos duplicados.
- Validación: Bean Validation en API y validación de dominio.
- SQL: JPA con parámetros, no concatenación manual.
- Secretos: variables de entorno y perfil local.
- Datos: seed ficticio; no datos reales en demo.
- No se implementa cifrado externo ni cumplimiento argentino completo en v1.

## Variables de entorno

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Conexión PostgreSQL | jdbc:postgresql://localhost:5432/odontologia | Sí |
| `SPRING_DATASOURCE_USERNAME` | Usuario DB | odontologia | Sí |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña DB | local-only | Sí |
| `APP_ADMIN_EMAIL` | Email del odontólogo inicial | demo@odontologia.local | Sí |
| `APP_ADMIN_PASSWORD` | Contraseña inicial ficticia (BCrypt) | demo-segura-123 | Sí |
| `APP_ADMIN_SEED` | Ejecuta el seed del odontólogo ficticio solo si es `true` | true | No |
| `APP_PUBLIC_URL` | URL local para links | http://localhost:8080 | No |
| `APP_TOKEN_MAX_DAYS` | Límite máximo del token: 7 días | 7 | No |

## Perfiles

- `local`: PostgreSQL.
- `test`: H2 PostgreSQL compatibility mode.
- `seed`: datos ficticios de demo.
