# Descripción General

## Stack tecnológico

| Capa | Tecnología | Observación |
|---|---|---|
| Backend | Java 17 o superior + Spring Boot | API REST |
| Build | Gradle | Ejecutable local |
| Persistencia | Hibernate/JPA | PostgreSQL en ejecución, H2 en tests |
| Base local | PostgreSQL | Sin hosting pago |
| Tests | JUnit 5 + H2 | Perfil de test con modo PostgreSQL |
| Frontend | TypeScript vanilla | Sin frameworks |
| Plantillas/UI | HTML/CSS/TypeScript compilado | Diseño visual diferido |
| Integración mail | Bandeja simulada | Persistencia, no SMTP real |

## Arquitectura general

```text
[Navegador odontólogo]        [Navegador paciente público]
         |                              |
         +---------- HTTP REST --------+
                        |
                 Spring Boot API
                        |
          +-------------+-------------+
          |                           |
    Servicios de dominio        Security/Login
          |                           |
          +-------------+-------------+
                        |
                  JPA Repositories
                        |
               PostgreSQL local

Tests → H2 en memoria → JPA mismo modelo → JUnit
```

Se propone una arquitectura modular en Spring Boot. La prioridad es entregar rápido sin sacrificar separación básica: controladores REST, servicios de aplicación/dominio, repositorios JPA y componentes de seguridad.

## Integraciones externas

| Servicio | Propósito | Tipo |
|---|---|---|
| PostgreSQL | Persistencia principal | JDBC/JPA |
| H2 | Tests sin instalación externa | JDBC/JPA |
| Mailbox simulada | Mensajes de recordatorio/confirmación | Interna, persistida |
| WhatsApp | Post-v1 | No integrado |
| Mercado Pago | Post-v1 | No integrado |
| ARCA/AFIP | Post-v1 | No integrado |

## API REST

- `POST /api/auth/login`, `POST /api/auth/logout`
- `GET/PATCH /api/schedule`
- `GET/POST/PATCH /api/appointments`
- `GET/POST/PATCH /api/patients`
- `GET /api/public/slots?date=...`
- `POST /api/public/appointments`
- `POST /api/public/appointments/{token}/confirm`
- `POST /api/public/appointments/{token}/cancel`
- `GET/POST /api/waiting-list`
- `GET /api/outbox/messages`
