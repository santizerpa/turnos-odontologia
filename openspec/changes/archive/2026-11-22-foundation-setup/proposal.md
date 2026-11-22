# Proposal

## Why

El proyecto no dispone todavía de una estructura ejecutable ni de perfiles de ejecución y prueba coherentes. C-01 establece los cimientos necesarios para iniciar el desarrollo posterior sin depender de PostgreSQL en tests ni improvisar la estructura del monorepo.

## What Changes

- Crear la estructura del monorepo: `backend/`, `frontend/`, `database/` y `knowledge-base/`.
- Configurar el backend Spring Boot 3.3.13 con Gradle Wrapper y Java release 17, sin toolchain estricta, incluyendo las dependencias web, JPA, validación, seguridad, H2, PostgreSQL y test.
- Incorporar perfiles `local` con PostgreSQL y `test` con H2 en modo compatibilidad PostgreSQL.
- Exponer `GET /api/health` sin autenticación y verificar una carga mínima del contexto Spring con perfil `test`.
- Crear el frontend TypeScript vanilla con `package.json`, dependencia TypeScript, script `build`, compilado en `frontend/dist/` y páginas públicas y administrativas mínimas servidas por Spring Boot como recursos estáticos.
- Agregar `.env.example` con las variables locales declaradas por la arquitectura y un README con carga de variables en PowerShell, perfil `local` por defecto para ejecución y `test` para pruebas.
- Mantener datos ficticios y variables locales; no incluir secretos reales ni datos reales de pacientes.

## Capabilities

### New Capabilities

- `fundacion-monorepo`: Cimientos ejecutables, configurables y comprobables del backend, frontend, perfiles de base de datos y documentación del proyecto.
- `salud-api`: Disponibilidad pública y observable del servicio mediante un endpoint de salud.

### Modified Capabilities

Ninguna.

## Impact

- Nuevo backend Spring Boot y configuración Gradle en `backend/`.
- Nuevo frontend TypeScript vanilla en `frontend/`.
- Nuevos perfiles de ejecución y variables de entorno en el backend y `.env.example`.
- Nueva prueba de carga del contexto Spring con H2.
- Nueva API pública `GET /api/health`.
- Dependencia inicial de PostgreSQL solo para ejecución local; H2 para pruebas automáticas.
