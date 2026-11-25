# Turnos Odontología

Sistema de turnos para un consultorio odontológico de un solo profesional. Reduce ausencias y cancelaciones tardías con una agenda, una reserva pública para el paciente, un ciclo de confirmación por mail simulado y una lista de espera.

Trabajo global de Desarrollo de Software, construido con desarrollo guiado por especificaciones (SDD): OpenSpec y Active Stack.

> Todos los datos de ejemplo son **ficticios**. No cargues datos reales de pacientes ni credenciales en el repositorio.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 17 o superior, Spring Boot 3.x, API REST |
| Build | Gradle (wrapper incluido) |
| Persistencia | Hibernate/JPA. PostgreSQL para ejecutar, H2 en memoria para los tests |
| Frontend | TypeScript vanilla, sin frameworks |
| Tests | JUnit 5 (backend) y `node:test` (frontend) |

## Requisitos previos

| Para qué | Qué necesitás |
|---|---|
| Tests del backend | Git y JDK 17 o superior. **No hace falta PostgreSQL.** |
| Tests y compilación del frontend | Node.js 22 o superior (incluye npm) |
| Ejecutar la aplicación | Todo lo anterior, más PostgreSQL |

Comprobá tus versiones:

```powershell
java -version
```

```powershell
node --version
```

Cualquier Java 17 o superior y cualquier Node 22 o superior sirven. Gradle no hay que instalarlo: el proyecto trae su propio wrapper, que descarga la versión necesaria la primera vez (requiere internet).

## 1. Clonar el repositorio

```powershell
git clone https://github.com/santizerpa/turnos-odontologia.git
cd turnos-odontologia
```

Todos los comandos siguientes parten de esta carpeta, la **raíz del proyecto**.

## 2. Ejecutar los tests

No hace falta ninguna base de datos ni compilar nada antes.

### 2.1 Backend

Los tests usan el perfil `test` con H2 en memoria (modo compatible con PostgreSQL).

Windows (PowerShell):

```powershell
cd backend
.\gradlew.bat test
cd ..
```

macOS y Linux:

```bash
cd backend
./gradlew test
cd ..
```

Si todo está bien, la salida termina con `BUILD SUCCESSFUL`. El reporte queda en `backend/build/reports/tests/test/index.html`. Para compilar y correr todo junto, usá `.\gradlew.bat build` (o `./gradlew build`) en lugar de `test`.

### 2.2 Frontend

`npm test` compila el TypeScript y corre los tests.

Windows (PowerShell):

```powershell
cd frontend
npm.cmd ci
npm.cmd test
cd ..
```

macOS y Linux:

```bash
cd frontend
npm ci
npm test
cd ..
```

Si todo está bien, la salida termina con `ℹ fail 0`. En Windows usá `npm.cmd` si PowerShell bloquea la ejecución de scripts.

## 3. Ejecutar la aplicación (perfil local)

Solo hace falta si querés probar la aplicación a mano o hacer la demo. Los comandos están en PowerShell; en macOS y Linux usá `export VARIABLE="valor"` en lugar de `$env:VARIABLE = "valor"`, `unset VARIABLE` en lugar de `Remove-Item Env:VARIABLE` y `./gradlew` en lugar de `.\gradlew.bat`.

### 3.1 Preparar PostgreSQL

Instalá PostgreSQL y, con un usuario administrador (por ejemplo `psql -U postgres`), creá una base y un usuario **de desarrollo** (valores ficticios):

```sql
CREATE USER odontologia WITH PASSWORD 'local-only';
CREATE DATABASE odontologia OWNER odontologia;
```

### 3.2 Compilar el frontend

El backend sirve las páginas desde `frontend/dist`. Si no está compilado, las páginas muestran "Frontend sin compilar". Desde la raíz del proyecto:

```powershell
cd frontend
npm.cmd ci
npm.cmd run build
cd ..
```

Si ya corriste `npm.cmd test` (sección 2.2), el frontend ya quedó compilado. Repetí `npm.cmd run build` cada vez que cambies código del frontend.

### 3.3 Variables de entorno

Spring Boot no lee el archivo `.env` por sí solo. Usá `.env.example` como referencia y cargá las variables en la **misma terminal** donde vayas a arrancar la app.

| Variable | Descripción | Ejemplo ficticio |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/odontologia` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `odontologia` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base | `local-only` |
| `APP_ADMIN_EMAIL` | Email del odontólogo inicial (login de `admin.html`) | `demo@odontologia.local` |
| `APP_ADMIN_PASSWORD` | Contraseña inicial (ficticia, solo demo) | `demo-segura-123` |
| `APP_ADMIN_SEED` | Crea el odontólogo ficticio al arrancar solo si es `true`; la demo lo requiere | `true` |
| `APP_DEMO_SEED` | Carga el conjunto completo de demo solo si es `true` (ver 3.7); ausente equivale a `false` | `false` |
| `APP_PUBLIC_URL` | Reservada: la versión actual **no la lee** (los enlaces de la bandeja son relativos) | `http://localhost:8080` |
| `APP_TOKEN_MAX_DAYS` | Reservada: la versión actual **no la lee** (el token vence al inicio del turno o a los 7 días) | `7` |

Entrá a la carpeta `backend` y cargá las variables:

```powershell
cd backend
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/odontologia"
$env:SPRING_DATASOURCE_USERNAME = "odontologia"
$env:SPRING_DATASOURCE_PASSWORD = "local-only"
$env:APP_ADMIN_SEED = "true"
$env:APP_ADMIN_EMAIL = "demo@odontologia.local"
$env:APP_ADMIN_PASSWORD = "demo-segura-123"
$env:APP_DEMO_SEED = "false"
```

### 3.4 Primer arranque en una base vacía

El proyecto no usa Flyway ni `schema.sql`: Hibernate crea las tablas a partir de las entidades. El perfil `local` usa `ddl-auto: validate`, que solo verifica que las tablas coincidan con las entidades y no crea nada; sobre una base vacía **no arranca**. Por eso, **solo la primera vez**, se arranca con `update` para que Hibernate cree las tablas.

En la misma terminal de 3.3:

```powershell
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "update"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Cuando veas `Started TurnosOdontologiaApplication`, detené el servidor con `Ctrl+C` (y `S` si pregunta si terminar el trabajo por lotes) y quitá la variable:

```powershell
Remove-Item Env:SPRING_JPA_HIBERNATE_DDL_AUTO
```

**¿Por qué quitarla?** `update` modifica la base sin avisar y nunca corrige columnas viejas. `validate`, en cambio, detiene el arranque si el código y la base dejaron de coincidir, y así el problema se ve enseguida.

### 3.5 Arrancar

Con las variables de 3.3 cargadas y **sin** `SPRING_JPA_HIBERNATE_DDL_AUTO`:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Tiene que terminar en `Started TurnosOdontologiaApplication`. Abrí en el navegador:

- `http://localhost:8080/`: página pública de reservas y bandeja de entrada simulada del paciente.
- `http://localhost:8080/admin.html`: consola del odontólogo. Ingresá con `APP_ADMIN_EMAIL` y `APP_ADMIN_PASSWORD`.

Para detener el servidor, `Ctrl+C`.

### 3.6 Comprobar que está vivo

En otra terminal:

```powershell
curl.exe http://localhost:8080/api/health
```

Debe responder con éxito sin pedir autenticación. En PowerShell usá `curl.exe`: `curl` a secas es un alias de `Invoke-WebRequest`.

### 3.7 Demo completa (opcional)

La carga de demo requiere una base local **limpia** (sin un conjunto de demo previo). Lo más simple es crear una base aparte, con un usuario administrador de PostgreSQL:

```sql
CREATE DATABASE odontologia_demo OWNER odontologia;
```

Después, en la carpeta `backend`, con las variables de 3.3, apuntá a esa base, activá la demo y hacé el primer arranque como en 3.4:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/odontologia_demo"
$env:APP_DEMO_SEED = "true"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "update"
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

En ese mismo arranque, Hibernate crea las tablas y el seed carga los datos. Después, detené el servidor, quitá `SPRING_JPA_HIBERNATE_DDL_AUTO` y volvé a arrancar como en 3.5.

El seed:

- crea o reutiliza el odontólogo administrativo y carga 7 pacientes ficticios;
- deja 2 turnos `CONFIRMED`, 1 `PENDING` y 1 `CANCELLED`, 3 entradas `WAITING`, un bloqueo de agenda y 3 mensajes `BOOKING` visibles en la bandeja, con el mismo asunto ("Confirma tu turno") y los mismos enlaces de confirmar y cancelar que una reserva real;
- ubica los turnos activos entre 24 horas y 30 días desde el arranque, en los días y horarios de la agenda configurada, sin solaparse con el bloqueo ni entre sí;
- deja los mensajes de outbox en `PENDING` con destinatarios `@example.invalid` (por ejemplo `demo.paciente3@example.invalid`, que podés ingresar en la bandeja de entrada simulada). Son locales: el seeder nunca envía correos, WhatsApp ni invoca el despacho.

**Repetir el seed:** si volvés a arrancar el mismo día con `APP_DEMO_SEED=true`, el seed reutiliza los mismos registros sin duplicarlos. Si la fecha cambió (por ejemplo, de lunes a martes), el seed **no** mueve los datos existentes: falla con un mensaje claro y deja los datos intactos. En ese caso, arrancá con `APP_DEMO_SEED = "false"` para seguir usando la base, o preparala de nuevo (`DROP DATABASE odontologia_demo;` y otra vez `CREATE DATABASE`).

Los tests del seed usan H2 y no requieren PostgreSQL; alcanza con la sección 2.

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `backend/` | API Spring Boot. Código en `backend/src/main`, tests en `backend/src/test` |
| `frontend/` | Código TypeScript vanilla, páginas HTML, estilos y sus tests |
| `database/` | Scripts y notas de base de datos |
| `design-system/` | Identidad visual, tokens y componentes de referencia |
| `knowledge-base/` | Base de conocimiento: visión, reglas de negocio, modelo de datos, arquitectura y decisiones |
| `openspec/` | Especificaciones y changes (activos y archivados) |
| `docs/` | Investigación (`discovery/`) y evidencia de verificación (`qa/`) |
| `CHANGES.md` | Roadmap con los changes y sus dependencias |
| `AGENTS.md` / `CLAUDE.md` | Reglas del agente de IA para este proyecto |
| `.atl/skill-registry.md` | Registro de skills del agente |
| `.engram/` | Configuración de la memoria persistente del agente |

## Cómo trabajamos (SDD)

1. Leer `CHANGES.md` y los documentos de `knowledge-base/` que indica el change.
2. Crear una rama desde `main`: `feat/<nombre>-<change>`.
3. Recorrer el ciclo de OpenSpec: explorar, proponer, aplicar, verificar y archivar. Leer cada artefacto generado y corregir lo que sobre, falte o esté inventado.
4. Escribir primero el test que falla y después el código mínimo (TDD).
5. Dejar el change archivado en `openspec/changes/archive/AAAA-MM-DD-nombre/`.
6. Abrir un Pull Request hacia `main`. El líder lo revisa y hace el merge.

Reglas del equipo:
- Solo el líder edita `CHANGES.md`.
- Cada integrante commitea con su propio usuario de GitHub.
- No se sube nada con claves, contraseñas reales ni datos reales de pacientes.

## Problemas frecuentes

| Síntoma | Qué hacer |
|---|---|
| `java` no se reconoce o la versión es menor a 17 | Instalá un JDK 17 o superior y abrí una terminal nueva |
| `node` no se reconoce o la versión es menor a 22 | Instalá Node.js 22 o superior y abrí una terminal nueva |
| `./gradlew: Permission denied` (macOS/Linux) | Ejecutá `chmod +x gradlew` dentro de `backend` |
| PowerShell dice que la ejecución de scripts está deshabilitada | Usá `npm.cmd`, o ejecutá `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| La app no arranca por la base de datos | Revisá que PostgreSQL esté corriendo y que las variables `SPRING_DATASOURCE_*` estén cargadas en esa misma terminal |
| `FATAL: no existe la base de datos` | Creá la base (sección 3.1 o 3.7); `update` crea las tablas, no la base |
| La app no arranca y menciona tablas o columnas faltantes | Es una base vacía: hacé el primer arranque de la sección 3.4 |
| La página dice "Frontend sin compilar" | Compilá el frontend (sección 3.2) y recargá con `Ctrl+F5` |
| El seed falla con "Ya existen datos de demo para la fecha…" | Arrancá con `APP_DEMO_SEED = "false"` o recreá la base de demo (sección 3.7) |
| El puerto 8080 está ocupado | Cerrá el otro programa que lo usa o cambiá el puerto de la aplicación |
| El primer `gradlew` tarda mucho | Es normal: descarga Gradle y las dependencias |

## Equipo

| Integrante | Rol | Change |
|---|---|---|
| Santiago (líder) | Fundación, revisión de PR y cierre | C-01, C-02 y C-14 |
| Pia | Cadena backend | C-03, C-05 y C-07 |
| Augusto | Tokens y outbox | C-09 y C-10 |
| Santos | Agenda, lista de espera y seed | C-04, C-11 y C-13 |
| Valentina | Pacientes y frontend | C-06, C-08 y C-12 |
