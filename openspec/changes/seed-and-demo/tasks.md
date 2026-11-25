# Tasks

## 1. Activación segura del seed de demo

- [ ] 1.1 Escribir primero pruebas para `APP_DEMO_SEED` ausente y deshabilitado: no crear datos completos de demo y conservar el comportamiento actual de `APP_ADMIN_SEED`; verificar el resultado con H2.
- [ ] 1.2 Escribir primero pruebas de configuración: con `APP_DEMO_SEED=true` y credenciales administrativas válidas, el odontólogo existe antes de cargar el dataset; si faltan `APP_ADMIN_EMAIL` o `APP_ADMIN_PASSWORD`, el arranque falla explícitamente y no crea datos parciales. Implementar el opt-in, el orden del runner y dejar verdes las pruebas.

## 2. Dataset ficticio determinista e idempotente

- [ ] 2.1 Escribir primero pruebas unitarias con `Clock.fixed` para la selección determinista de días hábiles y horas de agenda en `America/Argentina/Buenos_Aires`, incluyendo cambio de semana, sin solapamientos ni horarios fuera de las franjas; implementar el cálculo mínimo y verificar verde.
- [ ] 2.2 Escribir primero pruebas de persistencia con H2 para la carga relacionada: un odontólogo configurado, entre cinco y diez pacientes, dos turnos `CONFIRMED`, uno `PENDING`, uno `CANCELLED`, tres entradas `WAITING`, un bloqueo y mensajes ficticios de outbox. Comprobar asociaciones, estados y coherencia de `active_slot`; implementar el seeder transaccional usando fábricas/métodos de dominio, sin escribir `active_slot` directamente.
- [ ] 2.3 Escribir primero pruebas de efectos laterales e idempotencia: la cancelación de fixture no publica `AppointmentCancelledEvent` ni consume entradas de espera; una segunda ejecución con la misma fecha no duplica datos y los registros ajenos a los fixtures no cambian. Implementar las búsquedas conservadoras por claves ficticias y dejar verdes las pruebas.
- [ ] 2.4 Verificar con pruebas que los mensajes de muestra son visibles, usan destinatarios ficticios, no invocan dispatch ni servicios externos y no interfieren con la unicidad de recordatorios C-10.

## 3. Procedimiento reproducible de demo

- [ ] 3.1 Actualizar `README.md` con requisitos, preparación de una base local limpia, variables `APP_ADMIN_SEED` y `APP_DEMO_SEED`, credenciales de ejemplo ficticias y comandos de arranque en PowerShell y shell Unix; comprobar que los pasos documentados corresponden al runner real y no incorporan secretos.

## 4. Regresión y cierre técnico

- [ ] 4.1 Ejecutar las pruebas específicas del seed y luego `./gradlew test` desde `backend`; confirmar suite verde con H2 en modo PostgreSQL y sin requerir PostgreSQL instalado.
- [ ] 4.2 Ejecutar `./gradlew build` desde `backend` y revisar el diff: sin datos reales, secretos, envío externo, cambios de esquema/versiones ni escrituras directas de `active_slot`; registrar la evidencia.

## Workflow follow-up

- Antes de iniciar apply, actualizar `feat/santosc13` con la integración del líder que contiene C-10 y C-11 archivado; no aplicar sobre la base antigua observada durante el propose.
- Archivar el change solo después de completar las tareas, revisar la implementación y validar el resultado.
