# Proposal

## Why

La consola administrativa muestra horarios y datos de agenda en formatos técnicos poco legibles, mientras que los rangos diarios/semanales se calculan como medianoches UTC en vez de reflejar la hora local de Buenos Aires. Además, las dos páginas frontend carecen de un contrato de carga de estilos y viewport responsive; este cambio mejora la presentación sin modificar API ni agregar frameworks.

## What Changes

- Separar el contrato de consulta de agenda (incluida la resolución de nombres) de la semántica por fila y la lectura legible: fecha/hora de Buenos Aires, nombre sin UUID redundante y `activeSlot` nulo como “franja libre”. Los valores no nulos conservan su representación actual y el id de turno sigue disponible para acciones.
- Generar rangos diarios y semanales con offset `-03:00` para Buenos Aires en vez de `Z`.
- Enlazar `/css/styles.css` y declarar el viewport responsive en `frontend/index.html` y `frontend/admin.html`; actualizar el build existente para copiar `css/` a `dist/css/`.
- La hoja suministrada por el usuario, `C:\Users\santi\Downloads\styles.css` (448 líneas, disponible en el entorno actual), es la fuente para copiar exactamente a `frontend/css/styles.css` durante apply. Define estilos locales sin dependencias, paleta y espaciado, estados semánticos, radio de 8 px, diseño responsive y tipografía del sistema; no requiere fuentes externas ni JavaScript. Esa ruta identifica el origen de esta sesión, no es una dependencia de runtime.

Fuera de alcance: frameworks, fuentes externas, cambios de API o seguridad/backend, edición del CSS suministrado, y cambios a C-14 o a `CHANGES.md`.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `admin-agenda-ui`: dividir el comportamiento de consulta y presentación legible de agenda en requisitos verificables, además de agregar la carga del stylesheet y el viewport responsive, sin cambiar payloads ni endpoints.

## Impact

- Frontend vanilla TypeScript: `frontend/src/admin/ui.ts`, `frontend/src/admin/agenda.ts`, sus pruebas y `frontend/package.json`/pruebas de build.
- Páginas de entrada `frontend/index.html` y `frontend/admin.html`, más la futura copia exacta del CSS suministrado en `frontend/css/styles.css`.
- La configuración existente de recursos estáticos permite CSS por el matcher común de recursos estáticos; no se plantea cambio backend ni API. La conversión de `startAt` usa `Intl.DateTimeFormat` integrado, sin dependencias nuevas.
- El cambio requiere que exista un design system aprobado antes de apply bajo la política `system-only`; esta propuesta no genera mockups ni afirma que esa aprobación ya exista.
