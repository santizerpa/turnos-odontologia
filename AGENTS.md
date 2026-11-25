<!-- active-stack:agent-instruction -->
# turnos-odontologia — Instrucciones para Agentes

> Este archivo es el primer punto de entrada del proyecto.
> Las reglas globales ya definidas en el archivo global de instrucciones del cliente activo (orquestador, governance, TDD, Engram) se heredan y no se duplican aquí.

## Stack Tecnológico

| Capa | Tecnología | Observación |
|---|---|---|
| Backend | Java 17 o superior + Spring Boot | API REST |
| Build | Gradle | Ejecutable local con Gradle Wrapper |
| Persistencia | Hibernate/JPA | PostgreSQL en ejecución, H2 en tests |
| Base local | PostgreSQL | Sin hosting pago |
| Tests | JUnit 5 + H2 | Perfil de test con modo PostgreSQL |
| Frontend | TypeScript vanilla | Sin frameworks |
| Plantillas/UI | HTML/CSS/TypeScript compilado | Diseño visual diferido |
| Integración mail | Bandeja simulada | Persistencia, no SMTP real |

Detalle completo: [knowledge-base/02_descripcion_general.md](knowledge-base/02_descripcion_general.md)

## Base de Conocimiento

La fuente de verdad del dominio vive en `knowledge-base/`. Leer el archivo relevante antes de implementar.

| Archivo | Cuándo leerlo |
|---|---|
| [01. Visión y objetivos](knowledge-base/01_vision_y_objetivos.md) | Propósito, alcance y métricas |
| [02. Descripción general](knowledge-base/02_descripcion_general.md) | Stack, arquitectura e integraciones |
| [03. Actores y roles](knowledge-base/03_actores_y_roles.md) | Odontólogo, paciente y permisos |
| [04. Modelo de datos](knowledge-base/04_modelo_de_datos.md) | Entidades, ERD y seed |
| [05. Reglas de negocio](knowledge-base/05_reglas_de_negocio.md) | Reglas RN-XX |
| [06. Funcionalidades](knowledge-base/06_funcionalidades.md) | Funcionalidades e historias de usuario |
| [07. Flujos principales](knowledge-base/07_flujos_principales.md) | Reserva, confirmación y lista de espera |
| [08. Arquitectura propuesta](knowledge-base/08_arquitectura_propuesta.md) | Patrones, seguridad y estructura |
| [09. Decisiones y supuestos](knowledge-base/09_decisiones_y_supuestos.md) | ADR y trade-offs |
| [10. Preguntas abiertas](knowledge-base/10_preguntas_abiertas.md) | Integridad, seguridad y decisiones pendientes |

## Skills Disponibles

| Agente | Rol | Skills que carga |
|---|---|---|
| Backend Core | Spring Boot, REST, JPA, Gradle, dominio y persistencia | `java-springboot`, `springboot-patterns`, `postgresql-table-design` |
| Backend Seguridad | Login, sesiones, BCrypt, tokens y autorización | `springboot-security` |
| Testing | JUnit 5, Mockito, slices, TDD e integración H2 | `java-junit`, `springboot-tdd`, `spring-boot-test-patterns` |
| Fundación y OPSX | KB, roadmap, OpenSpec, registry e instrucciones | `kb-creator`, `roadmap-generator`, `openspec-*`, `agent-instruction`, `skill-registry` |

Los compact rules se resuelven desde [`.atl/skill-registry.md`](.atl/skill-registry.md); esta tabla solo mapea skill→rol.

## Roadmap de Changes

El plan completo está en [CHANGES.md](CHANGES.md).

- **Total:** 14 changes, C-01 a C-14, distribuidos en 7 fases, más el paso extra `ui-polish`, surgido al cierre y aceptado por el equipo.
- **Camino crítico:** `C-01 → C-02 → C-03 → C-05 → C-09 → C-10 → C-11 → C-12 → C-14`.
- **Primer change:** `C-01 foundation-setup`.
- **Plan confirmado:** 5 personas en paralelo, según el paso 1–9 documentado en `CHANGES.md`.
- **Entregable objetivo:** 2026-11-24.

Antes de iniciar un change: leer `CHANGES.md`, verificar sus dependencias y leer los archivos indicados en `Leer antes`.

## Reglas Duras (específicas del proyecto)

- **NUNCA usar datos reales de pacientes → usar siempre datos ficticios en código, tests, seeds y capturas.**
- **NUNCA guardar secretos, credenciales reales o claves privadas en el repositorio → usar variables locales, perfiles no versionados o valores ficticios de demo.**
- **NUNCA introducir upgrades implícitos de Java o Spring Boot → mantener Java 17+ y las versiones existentes hasta que haya una decisión explícita.**
- **NUNCA omitir el wrapper de Gradle ni evitar deliberadamente tests → usar `./gradlew build` o el task de test correspondiente y ejecutar la suite relevante.**
- **NUNCA requerir PostgreSQL para probar lógica portable del modelo → usar H2 en PostgreSQL mode en tests; PostgreSQL es el runtime local.**
- **NUNCA saltarse el ciclo TDD definido por el proyecto → test rojo, mínimo verde, caso/edge adicional, refactor con tests verdes.**
- **NUNCA modificar artefactos en español sin actualizar fuentes relacionadas → conservar KB, OpenSpec y `CHANGES.md` coherentes.**
- **NUNCA hacer commits sin solicitud explícita del usuario → el usuario mantiene el control de git.**

## Flujo de Trabajo

```text
1. Leer la KB relevante y las reglas de este archivo.
2. Leer el change de CHANGES.md y verificar dependencias.
3. Resolver alcance y criterios de aceptación antes de escribir.
4. Aplicar TDD estricto con H2 en el perfil de test.
5. Implementar con Java/Spring/Gradle y mantener PostgreSQL para runtime.
6. Ejecutar la verificación relevante y registrar la evidencia.
7. Actualizar CHANGES.md solo al cerrar y verificar un change.
```

Ante conflicto entre la documentación, este archivo y el código, prevalecen las reglas duras confirmadas y la KB vigente.
<!-- /active-stack:agent-instruction -->
