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
| Tests | JUnit 5 |

## Requisitos previos

| Para qué | Qué necesitás |
|---|---|
| Correr los tests | Git y JDK 17 o superior. **No hace falta PostgreSQL.** |
| Ejecutar la aplicación | Lo anterior, más PostgreSQL |
| Compilar el frontend | Node.js (versión LTS) y npm |

Comprobá tu versión de Java:

```powershell
java -version
```

Cualquier versión 17 o superior sirve. Gradle no hay que instalarlo: el proyecto trae su propio wrapper, que descarga la versión necesaria la primera vez (requiere internet).

## 1. Clonar el repositorio

```powershell
git clone https://github.com/santizerpa/turnos-odontologia.git
cd turnos-odontologia
```

## 2. Ejecutar los tests

Los tests usan el perfil `test` con H2 en memoria (modo compatible con PostgreSQL), así que corren sin ninguna base de datos instalada.

Windows (PowerShell):

```powershell
cd backend
.\gradlew.bat test
```

macOS y Linux:

```bash
cd backend
./gradlew test
```

Si todo está bien, la salida termina con `BUILD SUCCESSFUL`. El reporte queda en `backend/build/reports/tests/test/index.html`.

Para compilar y correr todo junto:

```powershell
.\gradlew.bat build
```

## 3. Ejecutar la aplicación (perfil local)

Solo hace falta si querés probar la API a mano o hacer la demo.

### 3.1 Preparar PostgreSQL

Instalá PostgreSQL y creá una base y un usuario **de desarrollo** (valores ficticios):

```sql
CREATE USER odontologia WITH PASSWORD 'local-only';
CREATE DATABASE odontologia OWNER odontologia;
```

### 3.2 Variables de entorno

Spring Boot no lee el archivo `.env` por sí solo. Copiá `.env.example` como referencia y cargá las variables en la terminal donde vayas a arrancar la app.

| Variable | Descripción | Ejemplo ficticio |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/odontologia` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la base | `odontologia` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la base | `local-only` |
| `APP_ADMIN_EMAIL` | Email del odontólogo inicial | `demo@odontologia.local` |
| `APP_ADMIN_PASSWORD` | Contraseña inicial (ficticia, solo demo) | `demo-segura-123` |
| `APP_ADMIN_SEED` | Ejecuta el seed del odontólogo ficticio solo si es `true` | `true` |
| `APP_PUBLIC_URL` | URL base para los links de la bandeja | `http://localhost:8080` |
| `APP_TOKEN_MAX_DAYS` | Vigencia máxima del token, en días | `7` |

En PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/odontologia"
$env:SPRING_DATASOURCE_USERNAME = "odontologia"
$env:SPRING_DATASOURCE_PASSWORD = "local-only"
$env:APP_ADMIN_EMAIL = "demo@odontologia.local"
$env:APP_ADMIN_PASSWORD = "demo-segura-123"
$env:APP_PUBLIC_URL = "http://localhost:8080"
$env:APP_TOKEN_MAX_DAYS = "7"
```

### 3.3 Arrancar

```powershell
cd backend
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

<!-- VERIFICAR tras aplicar C-01: que este comando y el perfil por defecto coincidan con lo implementado. -->

### 3.4 Comprobar que está vivo

```powershell
curl http://localhost:8080/api/health
```

Debe responder con éxito sin pedir autenticación.

<!-- VERIFICAR tras aplicar C-01: formato exacto de la respuesta (por ejemplo {"status":"UP"}). -->

## 4. Frontend

```powershell
cd frontend
npm.cmd install
npm.cmd run build
```

En Windows usá `npm.cmd` si PowerShell bloquea la ejecución de scripts. En macOS y Linux alcanza con `npm`.

<!-- VERIFICAR tras aplicar C-01: nombre del script de build y carpeta donde queda el resultado compilado. -->

Páginas base: `index.html` (reserva pública) y `admin.html` (odontólogo).

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `backend/` | API Spring Boot. Código en `backend/src/main`, tests en `backend/src/test` |
| `frontend/` | Código TypeScript vanilla y páginas HTML |
| `database/` | Scripts y notas de base de datos |
| `knowledge-base/` | Base de conocimiento: visión, reglas de negocio, modelo de datos, arquitectura y decisiones |
| `openspec/` | Especificaciones y changes (activos y archivados) |
| `docs/discovery/` | Investigación de mercado que justifica el alcance |
| `CHANGES.md` | Roadmap con los changes y sus dependencias |
| `AGENTS.md` / `CLAUDE.md` | Reglas del agente de IA para este proyecto |
| `.atl/skill-registry.md` | Registro de skills del agente |
| `.engram/` | Configuración de la memoria persistente del agente |

## Cómo trabajamos (SDD)

1. Leer `CHANGES.md` y los documentos de `knowledge-base/` que indica el change.
2. Crear una rama desde `main`: `feat/<nombre>-<change>`.
3. Recorrer el ciclo de OpenSpec: explorar, proponer, aplicar, verificar y archivar. Leer cada artefacto generado y corregir con Ajustar lo que sobre, falte o esté inventado.
4. Escribir primero el test que falla y después el código mínimo (TDD).
5. Dejar el change archivado en `openspec/changes/archive/AAAA-MM-DD-nombre/`.
6. Abrir un Pull Request hacia `main`. El líder lo revisa y hace el merge.

Reglas del equipo:
- Solo el líder edita `CHANGES.md`.
- Cada integrante commitea con su propio usuario de GitHub.
- No se sube nada con claves, contraseñas reales ni datos reales de pacientes.

## Entorno del agente de IA (opcional, para trabajar con SDD)

Para ejecutar el flujo de cada change con el agente necesitás:
- Active Stack instalado en modo Full, con `opencode` como agente.
- La herramienta `openspec` (la instala Active Stack).
- Engram para la memoria persistente. Después de clonar, podés traer la memoria compartida del proyecto con `engram sync --import`.

Abrí OpenCode parado en la carpeta del repositorio.

<!-- VERIFICAR: si las skills de OpenSpec no aparecen al abrir el agente, probá `openspec init --tools opencode` dentro del repo. Confirmalo en la documentación de OpenSpec. -->

## Problemas frecuentes

| Síntoma | Qué hacer |
|---|---|
| `java` no se reconoce o la versión es menor a 17 | Instalá un JDK 17 o superior y abrí una terminal nueva |
| PowerShell dice que la ejecución de scripts está deshabilitada | Usá `npm.cmd`, o ejecutá `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| La app no arranca por la base de datos | Revisá que PostgreSQL esté corriendo y que las variables `SPRING_DATASOURCE_*` estén cargadas en esa misma terminal |
| El puerto 8080 está ocupado | Cerrá el otro programa que lo usa o cambiá el puerto de la aplicación |
| El primer `gradlew` tarda mucho | Es normal: descarga Gradle y las dependencias |

## Equipo

| Integrante | Rol | Change |
|---|---|---|
| Santiago (líder) | Fundación, revisión de PR | C-01, C-02 |
| (completar) | | |
| (completar) | | |
| (completar) | | |
| (completar) | | |
