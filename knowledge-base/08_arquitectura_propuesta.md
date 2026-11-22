# Arquitectura Propuesta

## Patrones aplicados

| Patrón | Uso | Por qué |
|---|---|---|
| Modular monolith | Backend único con paquetes por dominio | Entrega rápida y operación local simple |
| Repository/JPA | Acceso a datos | Menos SQL repetido y buen testeo |
| Service/Use case | Casos de reserva y confirmación | Reglas centralizadas |
| State machine explícita | Estados de turno | Evita transiciones inválidas |
| Outbox local | Mensajes simulados | Mantiene demo sin integración externa |
| Optimistic locking | Turnos | Evita conflictos y overwrite |
| Unique constraint | Slot activo | Defensa final contra doble reserva |

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
- Sesión: expira a los 30 minutos de inactividad.
- Autorización: rutas administrativas autenticadas.
- Tokens: aleatorios, entre 5 y 10 numeros, hash persistido, vigencia y uso único.
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
| `APP_PUBLIC_URL` | URL local para links | http://localhost:8080 | No |
| `APP_TOKEN_MAX_DAYS` | Límite máximo del token: 7 días | 7 | No |

## Perfiles

- `local`: PostgreSQL.
- `test`: H2 PostgreSQL compatibility mode.
- `seed`: datos ficticios de demo.
