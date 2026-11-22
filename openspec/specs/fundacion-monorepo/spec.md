# fundacion-monorepo Specification

## Purpose

Establecer una base técnica reproducible para desarrollar el monorepo con Java 17, Spring Boot, Gradle, perfiles PostgreSQL/H2 y frontend TypeScript sin incluir datos reales ni secretos.

## Requirements

### Requirement: La estructura monorepo debe quedar disponible
El proyecto SHALL disponer de las áreas `backend/`, `frontend/`, `database/` y `knowledge-base/`, con documentación suficiente para distinguir su responsabilidad.

#### Scenario: El repositorio se inspecciona después del cambio
- **WHEN** el cambio de cimientos se ha aplicado
- **THEN** las cuatro áreas principales existen en la raíz del proyecto
- **AND** el backend contiene el código fuente y el wrapper de Gradle
- **AND** el frontend contiene su configuración TypeScript y sus puntos de entrada HTML

### Requirement: El backend debe cargar en el perfil de prueba
El backend SHALL cargarse en el perfil `test` usando H2 en modo compatibilidad PostgreSQL sin requerir PostgreSQL instalado.

#### Scenario: Se ejecuta la prueba de contexto con perfil test
- **WHEN** se inicia el contexto Spring con `test` activo
- **THEN** la aplicación termina de cargar sin errores
- **AND** la fuente de datos configurada es H2 PostgreSQL compatibility mode

### Requirement: El perfil local debe usar PostgreSQL
El backend SHALL admitir un perfil `local` cuya configuración utiliza PostgreSQL mediante variables de entorno configurables.

#### Scenario: Se arranca el perfil local
- **WHEN** se selecciona el perfil `local` y existen las variables de conexión válidas
- **THEN** Spring inicia con la configuración PostgreSQL indicada
- **AND** no se requiere guardar credenciales reales en el repositorio

#### Scenario: Se arranca el perfil local sin variables
- **WHEN** se inicia con el perfil `local` sin las variables obligatorias
- **THEN** el arranque falla con un mensaje claro que identifica las variables faltantes

### Requirement: Las rutas públicas y privadas deben aplicar seguridad explícita
El backend SHALL aplicar una lista permitida para `/api/health` y los recursos estáticos públicos, y SHALL proteger todas las demás rutas salvo rutas públicas futuras declaradas explícitamente en sus propias specs.

#### Scenario: Acceso público sin sesión
- **WHEN** un cliente sin sesión solicita `/api/health`, `index.html`, `admin.html` o JavaScript estático
- **THEN** la solicitud se acepta según corresponda al recurso

#### Scenario: Acceso privado sin sesión
- **WHEN** un cliente sin sesión solicita cualquier ruta no declarada pública
- **THEN** el sistema responde HTTP 401

### Requirement: El frontend compilado debe tener destino y servidor definidos
El frontend SHALL compilar JavaScript estático hacia `frontend/dist/`, y dicho resultado SHALL ser servido por Spring Boot como recursos estáticos en C-01.

#### Scenario: Compilación y publicación del frontend
- **WHEN** se ejecuta el build de TypeScript
- **THEN** el resultado queda bajo `frontend/dist/`
- **AND** el backend puede servir ese directorio como recursos estáticos

### Requirement: La configuración local debe estar documentada
El proyecto SHALL incluir una plantilla de variables de entorno y documentación de arranque y pruebas.

#### Scenario: Un desarrollador prepara el entorno local
- **WHEN** consulta `.env.example` y el README
- **THEN** puede identificar las variables PostgreSQL y administrativas necesarias
- **AND** puede ejecutar el wrapper de Gradle y la suite de pruebas mediante comandos documentados

### Requirement: El frontend base debe ser compilable sin framework
El frontend SHALL usar TypeScript vanilla y SHALL proveer una página pública y una página administrativa HTML.

#### Scenario: Se verifica la configuración frontend
- **WHEN** el repositorio contiene la configuración TypeScript y los archivos de entrada
- **THEN** existen `index.html` y `admin.html`
- **AND** existe una configuración `tsconfig.json` para compilar el código TypeScript vanilla
