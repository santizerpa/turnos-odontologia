# Verificación — public-booking-ui (C-08)

## Arranque local

1. Tener PostgreSQL en ejecución y las variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` del perfil `local` (valores ficticios de demo; nunca credenciales reales en el repositorio).
2. Compilar el frontend: `cd frontend && npm install && npm run build`.
3. Arrancar la API: `cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'` (en Windows: `gradlew.bat bootRun --args="--spring.profiles.active=local"`).
4. El perfil `local` declara `classpath:/static/,file:../frontend/dist/`. Como `classpath:/static/index.html` existe, el `/` del backend sirve esa página mínima. Para probar la UI compilada sin tocar el backend, levantar un servidor estático temporal que sirva `frontend/dist/` y reenvíe `/api/**` al puerto 8080:

   ```bash
   cd frontend
   node -e "const http=require('http'),fs=require('fs'),path=require('path');http.createServer((req,res)=>{if(req.url.startsWith('/api/')){const p=http.request({host:'localhost',port:8080,path:req.url,method:req.method,headers:req.headers},r=>{res.writeHead(r.statusCode,r.headers);r.pipe(res)});req.pipe(p);return}const f=path.join(process.cwd(),'dist',decodeURIComponent(req.url.split('?')[0]));fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.writeHead(200,{'Content-Type':f.endsWith('.js')?'text/javascript':'text/html'});res.end(d)})}).listen(5500)"
   ```

5. Abrir `http://localhost:5500/index.html`. No se modifica `backend/`, `CHANGES.md` ni la configuración de recursos estáticos.

## Regla de datos ficticios

Usar **solo** datos de prueba: nombre `Ana Ficticia`, apellido `Pérez Prueba`, DNI `12345678`, email `ana.ficticia@example.invalid` (o `Bruno Demo`, `Gómez Ejemplo`, DNI `98765432`, `bruno.demo@example.invalid`). Está prohibido usar nombres, DNI, emails reales o secreto alguno en pasos, capturas o ejemplos.

## Checklist manual

| # | Caso | Pasos y resultado esperado | Escenario del delta spec |
|---|---|---|---|
| 1 | Reserva feliz | Elegir una fecha dentro de `min`/`max`, Consultar horarios, seleccionar una franja, completar los datos ficticios y Reservar. Se ve "Turno reservado" con inicio, `30 minutos` y `PENDING`; no hay error. | `Reserva exitosa desde la interfaz pública` |
| 2 | 409 al reservar | Repetir con una franja que la API rechace por ocupada. Se ve `La franja solicitada ya está ocupada` + "Elija otra franja o contacte al consultorio."; no aparece confirmación y el formulario sigue visible. | `Conflicto de negocio al reservar una franja ocupada` |
| 3 | 422 por datos/fecha | Enviar DNI/email inválidos con `{"error":"Datos de reserva inválidos"}` (o forzar fecha fuera de ventana). Se ve el mensaje exacto del campo `error`, "Revise los datos ingresados" y los valores introducidos permanecen en el formulario. | `Datos inválidos o fecha fuera de ventana al reservar` |
| 4 | 503 / error no mapeado | Forzar 503 con `{"error":"Servicio temporalmente no disponible"}`; se ve ese texto exacto. Repetir sin campo `error`: se ve `No se pudo completar la solicitud. Intente nuevamente más tarde.` Lo mismo para 500 y estados no mapeados. | Requisito "Muestra errores 409, 422 y otros fallos"; respaldo de los escenarios 1–3 |
| 5 | Token inválido / vencido | En "Ya tengo un turno" pegar un enlace con token inexistente → 404 visible; usar un token vencido con la acción `Cancelar turno` → `Token vencido` y "El enlace venció: solicite uno nuevo al consultorio." No se ejecuta POST automático. | `Borde de token vencido en la consulta informativa` |
| 6 | GET no muta | Repetir "Consultar estado" varias veces con el mismo token válido; el estado mostrado no cambia y la bitácora de red solo registra `GET /api/public/appointments/{token}/confirm` (o `/cancel`). El botón "Ejecutar acción" sigue oculto hasta la consulta y el `POST` ocurre solo al hacer clic. | `Borde de token vencido en la consulta informativa` + requisito "gestionar un turno existente mediante token sin mutar en GET" |
| 7 | 409 en acción de token | Con un turno ya cancelado, consultar estado y hacer clic en "Ejecutar acción" con `Confirmar turno`: se ve el `error` del backend 409 y "No se pudo completar la acción."; sin resultado falso. | Requisito "gestionar un turno existente mediante token" |

Cobertura: los 4 escenarios del delta spec están cubiertos por los casos 1, 2, 3 y 5/6. La UI solo llama a los 6 endpoints públicos existentes; los casos 4 y 7 verifican el fallback y los errores de acción.

## Verificación automatizada (task 4.2)

Setup ESM sin dependencias nuevas: `frontend/tsconfig.json` mantiene `module: ES2020` y agrega `moduleResolution: node`; los imports relativos usan extensión `.js`; el script `build` escribe `frontend/dist/package.json` con `{"type":"module"}` para que `node --test` trate los `.js` compilados como ESM; `frontend/src/node-test-shims.d.ts` declara localmente `node:test` y `node:assert/strict` (no se instala `@types/node`). El navegador sigue cargando `/main.js` como módulo.

Línea base antes de tocar archivos: `npm install` OK, `npm run build` OK, 0 tests preexistentes.

Comandos y resultados finales (ejecutados en `frontend/`):

```text
$ npm run build
> build
> tsc -p tsconfig.json && node -e "const fs=require('fs'); fs.copyFileSync('index.html','dist/index.html'); fs.copyFileSync('admin.html','dist/admin.html'); fs.writeFileSync('dist/package.json', JSON.stringify({type:'module'}))"
(sin salida de error)

$ node --test "dist/**/*.test.js"
ℹ tests 32
ℹ suites 0
ℹ pass 32
ℹ fail 0
ℹ cancelled 0
ℹ skipped 0
ℹ todo 0
```

Los 32 tests son ejecución real de lógica pura (`node:test` + `node:assert/strict`) sobre los módulos compilados: ventana de fechas, rutas de los 6 endpoints, extracción de token, mapeo de errores 409/422/404/410/503/500 y fallback, construcción de payloads y view-models. DOM/fetch se cubre con el checklist manual anterior.

## Verificación manual ejecutada (2026-11-24)

Ejecutada con Playwright sobre `http://c08.test/index.html`, sirviendo los archivos compilados de `frontend/dist/` mediante interceptación de red. **La API fue simulada**: no había PostgreSQL ni backend en ejecución, por lo que no es una prueba end-to-end contra el servicio real. Los datos son ficticios (`Ana Ficticia`, `Pérez Prueba`, DNI `12345678`, `ana.ficticia@example.invalid`) y no se usaron secretos ni datos reales.

| # | Caso | Resultado | Evidencia observable |
|---|---|---|---|
| 1 | Reserva feliz | PASS | `Turno reservado` · `Inicio: 2026-11-25T09:00:00Z · Duración: 30 minutos · Estado: PENDING`; sin error. Red: `GET /api/public/slots`, `POST /api/public/appointments`. |
| 2 | Conflicto 409 | PASS | `No se pudo reservar el turno` + `La franja solicitada ya está ocupada` + `Elija otra franja o contacte al consultorio.`; sin confirmación; formulario conserva `Ana Ficticia`. |
| 3 | Validación 422 | PASS | `Revise los datos ingresados` + `Datos de reserva inválidos`; formulario conserva los valores introducidos. |
| 4 | 503 y fallback | PASS | Con `error`: `Servicio temporalmente no disponible`. Sin campo `error`: `No se pudo completar la solicitud. Intente nuevamente más tarde.` |
| 5 | Token vencido 410 | PASS | `No se pudo consultar el enlace` + `Token vencido` + `El enlace venció: solicite uno nuevo al consultorio.`; `POST` count = 0. |
| 6 | GET no muta / POST explícito | PASS | Botón de acción oculto antes de consultar; dos `GET` y cero `POST` tras dos consultas; botón visible después; un `POST` tras el clic; `Turno confirmado` · `confirmación exitosa · Estado: CONFIRMED`. |
| 7 | Error 409 en acción de token | PASS | `No se pudo completar la acción.` + `El turno no puede confirmarse`; sin resultado falso. |

Resultado final: **7/7 casos manuales PASS**. En la primera corrida los casos 1 y 2 fallaron porque el harness no sirvió los submódulos `/public-reservation/*.js` (404) y los selectores nunca aparecieron; al corregir el served estático y re-ejecutar, ambos pasaron sin cambiar código de producción. La consola solo registró los códigos HTTP esperados de las respuestas simuladas (422, 503, 410, 409), sin errores JS inesperados.

Cobertura de los 4 escenarios del delta spec:

- `Reserva exitosa desde la interfaz pública` → tests unitarios de payload/view-model + caso manual 1.
- `Conflicto de negocio al reservar una franja ocupada` → test unitario de mapeo 409 + caso manual 2.
- `Datos inválidos o fecha fuera de ventana al reservar` → tests unitarios de fecha/validación/mapeo 422 + caso manual 3.
- `Borde de token vencido en la consulta informativa` → test unitario de mapeo 410 + casos manuales 5 y 6.

Cobertura automatizada estricta: **0 escenarios `full`, 4 `partial`**; los 32 tests ejercitan lógica pura pero ninguno ejercita DOM/fetch. La verificación manual cubre esa brecha, pero si se exige cobertura `full` automatizada quedan pendientes tests de interacción DOM/fetch. Riesgo de integración fuera de alcance C-08: `classpath:/static/index.html` podría opacar `frontend/dist/index.html` cuando se sirve desde Spring Boot.
