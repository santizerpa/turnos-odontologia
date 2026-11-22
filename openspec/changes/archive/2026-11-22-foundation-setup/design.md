# Design

## Context

C-01 es el primer change y no tiene dependencias previas. El repositorio ya contiene conocimiento de dominio y configuración de OpenSpec, pero todavía no dispone de la estructura ejecutable del monorepo. El diseño debe fijar Java 17+ con release 17, Spring Boot 3.3.13, Gradle, PostgreSQL local y H2 compatible con PostgreSQL en tests.

## Goals / Non-Goals

**Goals:**
- Crear una base reproducible y verificable para backend, frontend y configuración local.
- Separar claramente la ejecución local de las pruebas automáticas.
- Exponer una ruta pública mínima sin implementar autenticación ni dominio funcional aún.
- Documentar variables y comandos sin almacenar secretos reales.

**Non-Goals:**
- Implementar modelos JPA de dominio, autenticación, agenda, reserva o interfaz funcional.
- Integrar SMTP real, WhatsApp, pagos o servicios externos.
- Diseñar una UI visual compleja ni generar mockups.
- Crear datos reales de pacientes o credenciales sensibles.

## Decisions

### Monorepo modular
Se conservarán las áreas `backend/`, `frontend/`, `database/` y `knowledge-base/`. El backend se organizará en paquetes futuros por dominio y configuración, siguiendo la arquitectura propuesta.

Alternativa considerada: repositorios separados. Se descarta para simplificar ejecución local y coordinación de este MVP.

### Gradle con wrapper y compatibilidad Java
El backend usará Gradle Wrapper y se configurará con `JavaCompile.options.release = 17`, sin fijar una toolchain Java estricta. Esto permite compilar con JDK 17 o superior y ejecutar el bytecode compatible con Java 17.

Alternativa considerada: Maven. Se descarta porque el roadmap y la documentación del proyecto identifican Gradle como build existente previsto.

Alternativa considerada: fijar una toolchain Java 17 exacta. Se descarta porque impediría compilar de forma natural con JDK 21 u otros JDK compatibles superiores; el requisito es release 17 como mínimo.

### Perfiles de ejecución separados
- `local`: perfil por defecto para ejecución, usando PostgreSQL mediante `SPRING_DATASOURCE_*` y variables locales. En PowerShell se fija con `$env:SPRING_PROFILES_ACTIVE='local'`.
- `test`: perfil activo para pruebas, usando H2 en modo compatibilidad PostgreSQL con base en memoria. En PowerShell se fija con `$env:SPRING_PROFILES_ACTIVE='test'`.

El arranque local sin las variables obligatorias deberá fallar con un mensaje claro en lugar de iniciar con una fuente de datos incompleta.

Alternativa considerada: usar H2 también en local. Se descarta porque la arquitectura exige PostgreSQL como persistencia principal local y mantiene H2 reservado para pruebas.

### Spring Security con PermitList explícita
El backend usará Spring Boot 3.3.13, compatible con Java 17 y dentro de la línea Spring Boot 3 estable. La versión exacta se fija para evitar cambios implícitos de arranque, seguridad y dependencias; la migración a otra línea requiere decisión explícita.

El paquete base Java exacto será `com.turnosodontologia`. La configuración inicial declarará una PermitList pública: `GET /api/health`, `index.html`, `admin.html` y recursos JavaScript estáticos. Toda otra ruta quedará protegida por defecto. Sin sesión, `/api/health` y los estáticos permitidos seguirán accesibles; las rutas protegidas responderán 401. Futuros endpoints públicos deberán declararse explícitamente en sus specs y agregarse a la lista permitida.

Alternativa considerada: desactivar Security en el cimiento. Se descarta porque dejaría una base menos alineada con la arquitectura futura y podría inducir a exponer rutas administrativas inadvertidamente.

### Frontend sin framework
Se utilizará TypeScript vanilla con `package.json`, dependencia `typescript` y script `build`. El compilado quedará en `frontend/dist/`; Spring Boot lo servirá como recursos estáticos en C-01, junto con `index.html` y `admin.html`. Si se requiriera un servidor local independiente, deberá quedar definido explícitamente en C-01 y no se asumirá como integrado actualmente.

Alternativa considerada: introducir un framework. Se descarta por el stack confirmado y por la ausencia de requisitos visuales complejos en este change.

### Organización de paquetes Java y KB
El código Java pertenecerá al paquete base `com.turnosodontologia` y se organizará por dominios. El registro de paquetes de código en el conocimiento del proyecto incluirá `auth`, `patient`, `appointment`, `schedule`, `waitinglist`, `outbox` y `common`.

## Risks / Trade-offs

- [PostgreSQL no disponible] → La suite de tests no debe depender de él; el perfil `test` usa H2 en modo PostgreSQL.
- [Variables locales mal configuradas] → `.env.example` y el README definirán nombres y ejemplos ficticios; los valores reales permanecen fuera del repositorio.
- [Spring Boot 3.3.13 y Java 17+ deben permanecer compatibles] → Validar el wrapper, `release=17` y la aplicación con JDK 17 o superior antes de integrar cambios posteriores.
- [H2 puede no revelar todos los comportamientos PostgreSQL] → C-02 y siguientes deberán incluir verificaciones específicas de paridad donde corresponda.

## Migration Plan

C-01 introduce una nueva base verde; no existe una aplicación previa que migrar. La puesta en marcha es:

1. Configurar el perfil `local` con variables ficticias o locales.
2. Arrancar PostgreSQL solo para ejecutar el perfil local.
3. Ejecutar `./gradlew test` o el equivalente documentado para validar el perfil `test`.
4. Verificar `GET /api/health` en la instancia local.

No se requiere rollback de datos ni migración de esquema en este change.
