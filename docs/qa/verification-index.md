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
