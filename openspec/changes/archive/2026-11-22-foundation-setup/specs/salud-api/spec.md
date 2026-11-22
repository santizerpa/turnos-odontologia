# Spec Delta

## Purpose

Permitir comprobar de forma simple y pública si el servicio backend quedó iniciado y disponible después del scaffolding inicial.

## ADDED Requirements

### Requirement: El servicio debe ofrecer una salud pública
El backend SHALL responder públicamente desde `GET /api/health` sin requerir autenticación ni sesión.

#### Scenario: Respuesta exacta del servicio disponible
- **WHEN** un cliente sin sesión envía `GET /api/health` al backend iniciado
- **THEN** recibe HTTP 200
- **AND** recibe exactamente el JSON `{"status":"UP"}`
- **AND** no se redirige a autenticación

#### Scenario: Recursos estáticos públicos
- **WHEN** un cliente sin sesión solicita `index.html`, `admin.html` o JavaScript estático
- **THEN** los recursos estáticos son accesibles sin autenticación

#### Scenario: Ruta protegida sin sesión
- **WHEN** un cliente sin sesión solicita una ruta no pública
- **THEN** el sistema responde HTTP 401
- **AND** no exige una redirección a autenticación

### Requirement: El arranque local debe reportar configuración incompleta
El backend SHALL fallar de forma clara al arrancar localmente sin las variables obligatorias de conexión.

#### Scenario: Arranque local sin variables
- **WHEN** se inicia con el perfil `local` sin variables de conexión configuradas
- **THEN** el arranque falla con un mensaje claro que indica qué variables faltan

### Requirement: La suite de pruebas no debe depender de PostgreSQL
La suite SHALL ejecutarse con el perfil `test` y H2 en modo compatibilidad PostgreSQL sin una instalación local de PostgreSQL.

#### Scenario: Prueba ejecutada sin PostgreSQL
- **WHEN** se ejecuta la suite con el perfil `test` en un entorno sin PostgreSQL
- **THEN** las pruebas pasan
- **AND** no se establece una conexión con PostgreSQL
