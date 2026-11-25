# Índice de evidencia de verificación

## C-14 — PostgreSQL real: unicidad de `active_slot` y re-reserva (tarea 2.4)

- **Estado:** **PENDIENTE / NO VERIFICADO** (2026-11-25).
- **Motor y versión:** no observados en esta sesión.
- **Comandos de verificación PostgreSQL:** no ejecutados; no se consultó ni modificó ninguna base.
- **Motivo:** no había `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` ni `SPRING_DATASOURCE_PASSWORD` configuradas en el entorno de esta sesión; `psql` y `pg_isready` tampoco estaban disponibles en `PATH`. No se infirieron credenciales ni se usó una base desconocida.
- **Checklist de referencia:** la sección exacta `Estado de la verificación en PostgreSQL real` está presente en [database/README.md](../../database/README.md). Su checklist marca como pendiente la cancelación que deja `active_slot` en `NULL` y la nueva reserva de esa franja. También indica que no se afirme paridad mientras queden casillas sin verificar. No se creó un procedimiento alternativo.
- **Evidencia existente, no repetida aquí como resultado de esta sesión:** la checklist de `database/README.md` tiene marcada la comprobación previa de una segunda reserva de la misma franja con HTTP 409; no incluye en esa casilla versión del motor, salida de comandos ni evidencia de cancelación/re-reserva. Por eso no basta para declarar cumplida la comprobación solicitada por 2.4.
- **Resultado de esta ejecución:** no hay evidencia nueva de PostgreSQL real para la unicidad persistida de `active_slot` ni para reutilizar la franja después de cancelar. La evidencia H2 en modo PostgreSQL, documentada por separado en la matriz de [c14-demo-checklist.md](c14-demo-checklist.md), no sustituye esa verificación.
- **Próximo paso:** ejecutar solo cuando el responsable habilite una instancia local autorizada y confirme sus ajustes localmente; seguir el checklist de `database/README.md`, usar datos ficticios y registrar aquí motor, versión, comandos y resultados observados, sin credenciales.
- **Actualización (2026-11-25):** el responsable marcó en `database/README.md` la casilla de cancelación (`active_slot` en `NULL`) y nueva reserva de la misma franja, junto con las de lista de espera y recordatorios. Esas casillas reflejan su prueba manual; no hay salida de comandos adjunta para ellas en este índice.

## Carga de demo (`APP_DEMO_SEED=true`) sobre base limpia y segundo arranque el mismo día

- **Estado:** **VERIFICADO** (2026-11-25, ejecutado por el responsable del proyecto y registrado por el agente).
- **Motor:** PostgreSQL local; cliente `psql 19.0`. Versión del servidor no registrada.
- **Base:** `odontologia_demo`, creada vacía para esta prueba (dueño `odontologia`); solo datos ficticios.
- **Primer arranque** (perfil `local`, `APP_ADMIN_SEED=true`, `APP_DEMO_SEED=true`, `SPRING_JPA_HIBERNATE_DDL_AUTO=update`): `Started TurnosOdontologiaApplication in 17.47 seconds`. Hibernate emitió avisos `SQLState 00000` por restricciones inexistentes al crear el esquema desde cero; son esperables en una base vacía y no bloquearon el arranque.
- **Conteo tras el primer arranque:** `pacientes 7 · turnos 4 · espera 3 · mensajes 3`.
- **Segundo arranque el mismo día** (sin `SPRING_JPA_HIBERNATE_DDL_AUTO`, `ddl-auto: validate`, `APP_DEMO_SEED=true`): `Started TurnosOdontologiaApplication in 16.443 seconds`, sin `DemoSeedException`.
- **Conteo tras el segundo arranque:** `pacientes 7 · turnos 4 · espera 3 · mensajes 3` (sin duplicados).
- **Consulta usada:** `SELECT (SELECT count(*) FROM patients), (SELECT count(*) FROM appointments), (SELECT count(*) FROM waiting_list_entries), (SELECT count(*) FROM outbox_messages);`
- **Observación:** la consola mostró los 3 mensajes del seed. Su formato ("Turno demo <estado>", sin enlace) no coincidía con los mensajes reales de reserva; se corrigió en el seed el mismo día.
