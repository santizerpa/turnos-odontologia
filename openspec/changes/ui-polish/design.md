# Design

## Context

Ver `proposal.md` para la motivación y `specs/admin-agenda-ui/spec.md` para el contrato separado por comportamiento. La agenda se arma en `frontend/src/admin/agenda.ts` y sus filas/textos en `frontend/src/admin/ui.ts`; hoy se muestran `startAt`, paciente con `patientId` entre paréntesis y `activeSlot` crudos. `dailyRange`/`weeklyRange` parten de medianoche UTC. Las entradas son `frontend/index.html` y `frontend/admin.html`; el build de `frontend/package.json` copia esas páginas, pero no recursos CSS. El matcher de recursos estáticos comunes en `AuthSecurityConfiguration` ya deja público CSS, por lo que este diseño no cambia backend.

El usuario proporcionó `C:\Users\santi\Downloads\styles.css` (448 líneas), disponible en el entorno actual. Es una hoja local sin dependencias, con paleta (primario `#0f6f8f`, oscuro `#0b5a75`), neutrales, colores semánticos, radio de 8 px, selectores para el DOM público/administrativo existente, estados `data-status`, reglas responsive y fuentes del sistema. Indica que no requiere fuentes externas ni JavaScript. Durante apply se copiará exactamente a `frontend/css/styles.css`; la ruta de descarga es solo procedencia de la entrada, no una dependencia de runtime. Este update no la copia ni modifica.

## Goals / Non-Goals

**Goals:**
- Hacer legible la agenda con fecha/hora local de Buenos Aires, nombre sin UUID redundante, franja nula explícita y estado semántico por fila.
- Expresar rangos de fecha como límites locales con `-03:00` y añadir la carga/copia del stylesheet suministrado en las dos entradas frontend.
- Mantener compatibilidad con endpoints y payloads actuales. Copiar exactamente el CSS suministrado sin dependencias nuevas.

**Non-Goals:**
- Cambiar contratos o comportamiento backend, autenticación, seguridad, frameworks o tipografías externas.
- Editar el CSS suministrado, crear mockups o alterar el alcance de C-14 o `CHANGES.md`.

## Decisions

1. **Localizar solo la presentación del inicio.** Formatear `startAt` mediante `Intl.DateTimeFormat('es-AR', { timeZone: 'America/Argentina/Buenos_Aires', dateStyle: 'short', timeStyle: 'short' })`. El API y `AgendaEntry` conservan valores ISO; el formato es de vista. La alternativa de dejar visible el `Instant` sin convertir mantiene el problema actual.
2. **Separar consulta y presentación de agenda.** Mantener el contrato de una consulta de agenda y una consulta auxiliar de pacientes; especificar la semántica del estado por fila y la lectura de sus datos en requisitos propios. El mapa de pacientes usa una sola página de hasta 100 registros; el fallback es `patientId`.
3. **No renderizar el UUID del paciente como dato secundario.** La fila usa `patientName` como representación visible, que ya contiene el nombre o el fallback `patientId`; no añade además `(${patientId})`. Mantener el `id` en el elemento/estado que necesita las acciones. Colocar exactamente el estado recibido en `data-status` de cada fila para identificarla sin introducir una API nueva.
4. **Tratar `activeSlot` de forma conservadora.** Cuando es `null`, mostrar “franja libre”; si no es nulo, mantener el valor actual sin una nueva normalización no solicitada. No cambiar el filtrado de turnos cancelados.
5. **Construir límites locales con offset explícito.** Interpretar la fecha seleccionada como medianoche de Buenos Aires y sumar un día o siete días de calendario para el límite superior; serializar ambos límites con `-03:00`. No usar `toISOString()` para construir esos límites, porque vuelve a expresarlos como `Z`.
6. **Copiar la fuente CSS exacta y mantener el bundle actual.** En apply, copiar sin modificaciones el archivo suministrado a `frontend/css/styles.css`, enlazar la URL estable `/css/styles.css` en ambas páginas y extender el comando Node existente del build para copiar recursivamente `css/` a `dist/css/`. No agregar herramientas, dependencias, fuentes externas ni JavaScript; no tocar allowlists backend.
7. **No crear mockups por política.** El usuario seleccionó `system-only`: no hay mockups por cambio. No se encontró un design system aprobado, así que su creación y aprobación quedan como dependencia obligatoria antes de apply; este documento no afirma que exista ni que esté aprobado.

## Risks / Trade-offs

- [El runtime puede producir una representación localizada distinta en puntuación/espaciado] → Probar con `Intl.DateTimeFormat` usando exactamente locale, zona y opciones acordadas; no comparar contra el `Instant` UTC.
- [Una copia incompleta del CSS suministrado afectaría estilos o estados] → Copiar exactamente la fuente disponible a `frontend/css/styles.css` durante apply y verificar ruta de salida y carga del recurso.
- [Un `data-status` incompleto dejaría filas sin identificación semántica] → Verificar todas las filas renderizadas para estados diversos, no solo el primer turno.

## Migration Plan

No hay migración de datos ni despliegue backend. Al aplicar, copiar primero la fuente CSS suministrada sin modificarla; actualizar páginas y build junto con los tests, ejecutar `npm test` en `frontend/` y verificar que ambas entradas resuelven `/css/styles.css` desde el bundle publicado. Rollback: revertir solo los cambios del frontend de este change y retirar el enlace/copia; no afecta API ni datos.

## Open Questions

- Crear y aprobar el design system del proyecto antes de implementar cambios UI, según política `system-only`.

## UI Mockups

- **Decision:** no (project policy).
- **Mockups:** ninguno; la política `system-only` no requiere mockups por change.
- **Design system:** no existe todavía una revisión aprobada; su aprobación es prerrequisito de apply.
