# Tasks

## 1. Preparar recursos y política de UI

- [ ] 1.1 Durante apply, copiar exactamente la fuente CSS suministrada por el usuario (`C:\Users\santi\Downloads\styles.css`, disponible en el entorno actual) a `frontend/css/styles.css`; esta ruta es solo el origen de la copia, no una dependencia de runtime. No editar el contenido ni agregar dependencias externas.
- [ ] 1.2 Crear y obtener aprobación explícita del design system del proyecto según la política `system-only`; verificar `design-system/README.md` con revisión y `Approval: approved` antes de aplicar cambios visuales.

## 2. Presentación legible de agenda y rangos locales

- [ ] 2.1 Actualizar `agendaEntryText` para mostrar `startAt` con `Intl.DateTimeFormat('es-AR', { timeZone: 'America/Argentina/Buenos_Aires', dateStyle: 'short', timeStyle: 'short' })`, el nombre sin UUID redundante, `activeSlot: null` como “franja libre” y el valor no nulo sin alterarlo. Ampliar `ui.test.ts` con al menos tres casos de nombre, fallback, franjas y formato local, más la resolución de pacientes especificada.
- [ ] 2.2 Asignar `data-status` a cada fila renderizada sin perder el identificador usado por sus acciones; probar varios estados recibidos y comprobar que el nombre visible no añade el UUID entre paréntesis.
- [ ] 2.3 Cambiar `dailyRange` y `weeklyRange` a límites de calendario en Buenos Aires con `-03:00`; actualizar `agenda.test.ts` para verificar el rango diario, el semanal y un límite que cruza el año, confirmando que no se genera sufijo `Z`.

## 3. Carga de stylesheet y build

- [ ] 3.1 Añadir el enlace `/css/styles.css` y el viewport especificado en `frontend/index.html` y `frontend/admin.html`; ampliar una prueba automatizada que lea ambas entradas y verifique los dos elementos exactos en cada archivo.
- [ ] 3.2 Extender el build existente para copiar recursivamente `frontend/css/` a `frontend/dist/css/`; ampliar `build.test.ts` para comprobar el CSS copiado exactamente y verificar que ambas entradas compiladas conservan sus enlaces.

## 4. Verificación integrada

- [ ] 4.1 Ejecutar `npm test` desde `frontend/` después de completar las tareas anteriores y confirmar que compila las dos entradas, ejecuta las pruebas existentes y nuevas, y deja los recursos CSS publicados en `dist/css/`.
