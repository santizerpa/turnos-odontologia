# Spec Delta

## Purpose

Definir el acceso del odontólogo al sistema mediante email y contraseña, con una sesión de 30 minutos de inactividad que protege las rutas administrativas y deja explícitas las rutas públicas.

## ADDED Requirements

### Requirement: El odontólogo debe poder iniciar sesión con email y contraseña

`POST /api/auth/login` SHALL aceptar email y contraseña, verificar la contraseña contra el hash BCrypt persistido del `Dentist` y, si coincide, iniciar una sesión y devolver el perfil del odontólogo autenticado.

#### Scenario: Login correcto

- **GIVEN** un odontólogo activo con email ficticio y contraseña almacenada como hash BCrypt
- **WHEN** envía `POST /api/auth/login` con ese email y la contraseña correcta
- **THEN** la respuesta es 200 con id, email, nombre y apellido del odontólogo
- **AND** la respuesta entrega una cookie de sesión `HttpOnly`
- **AND** la respuesta no contiene el hash de la contraseña

#### Scenario: Credenciales inválidas con mensaje genérico

- **GIVEN** un odontólogo activo con email ficticio
- **WHEN** envía `POST /api/auth/login` con ese email y una contraseña incorrecta
- **THEN** la respuesta es 401 con un mensaje genérico de credenciales inválidas
- **AND** no se entrega cookie de sesión
- **AND** el mensaje no permite distinguir si el email existe o la contraseña es incorrecta

#### Scenario: Email inexistente responde igual que una contraseña incorrecta

- **GIVEN** un email que no corresponde a ningún odontólogo
- **WHEN** envía `POST /api/auth/login` con ese email y cualquier contraseña
- **THEN** la respuesta es 401 con el mismo mensaje genérico del caso anterior
- **AND** no se entrega cookie de sesión

### Requirement: La sesión debe expirar tras 30 minutos de inactividad

Una sesión autenticada SHALL mantenerse mientras haya actividad y SHALL quedar inválida tras 30 minutos sin solicitudes, obligando a un nuevo login.

#### Scenario: Sesión expirada

- **GIVEN** una sesión iniciada sin actividad durante 30 minutos
- **WHEN** el cliente solicita una ruta administrativa con la cookie vencida
- **THEN** la respuesta es 401
- **AND** el cliente debe autenticarse de nuevo

#### Scenario: La actividad dentro de la ventana mantiene la sesión

- **GIVEN** una sesión iniciada
- **WHEN** el cliente realiza solicitudes autenticadas antes de cumplirse 30 minutos de inactividad
- **THEN** cada solicitud se acepta
- **AND** la cookie de sesión continúa siendo `HttpOnly`

### Requirement: Las rutas administrativas deben exigir sesión y las públicas no

La seguridad SHALL dejar abiertas `/api/health`, las páginas públicas, los recursos estáticos y el prefijo `/api/public/**`, y SHALL responder 401 sin redirección a cualquier otra ruta solicitada sin sesión válida.

#### Scenario: Ruta protegida sin sesión

- **WHEN** un cliente sin sesión solicita `GET /api/auth/me`
- **THEN** la respuesta es 401
- **AND** el cuerpo es una respuesta de error de la API, no una redirección a una página de login

#### Scenario: Ruta protegida de negocio sin sesión

- **WHEN** un cliente sin sesión solicita `GET /api/appointments`
- **THEN** la respuesta es 401
- **AND** no se ejecuta ninguna lógica de agenda

#### Scenario: Páginas y recursos públicos sin sesión

- **WHEN** un cliente sin sesión solicita `/`, `/index.html`, `/admin.html`, `/api/health` o un archivo bajo `/js/`
- **THEN** la solicitud se acepta sin pedir autenticación

#### Scenario: Prefijo público reservado para la reserva de pacientes

- **WHEN** un cliente sin sesión solicita una ruta bajo `/api/public/`
- **THEN** la seguridad no responde 401 por falta de sesión

### Requirement: El odontólogo debe poder consultar y cerrar su sesión

`GET /api/auth/me` SHALL devolver el perfil de la sesión activa y `POST /api/auth/logout` SHALL invalidar la sesión y la cookie asociada.

#### Scenario: Consultar la sesión activa

- **GIVEN** una sesión iniciada
- **WHEN** el cliente solicita `GET /api/auth/me`
- **THEN** la respuesta es 200 con id, email, nombre y apellido
- **AND** la respuesta no contiene `password_hash`

#### Scenario: Cerrar sesión

- **GIVEN** una sesión iniciada
- **WHEN** el cliente envía `POST /api/auth/logout`
- **THEN** la sesión queda invalidada y la cookie de sesión deja de ser válida
- **AND** una solicitud posterior a `GET /api/auth/me` con esa cookie responde 401

### Requirement: Un odontólogo dado de baja no debe autenticarse

Un `Dentist` con `deleted_at` informado SHALL ser rechazado en el login con el mismo mensaje genérico que credenciales inválidas, sin reactivarlo ni modificar su baja lógica.

#### Scenario: Odontólogo con deleted_at intenta iniciar sesión

- **GIVEN** un odontólogo con `deleted_at` informado y credenciales correctas
- **WHEN** envía `POST /api/auth/login`
- **THEN** la respuesta es 401 con el mensaje genérico de credenciales inválidas
- **AND** no se entrega cookie de sesión
- **AND** el registro conserva su `deleted_at` original

### Requirement: La verificación de contraseña debe usar BCrypt

El sistema SHALL verificar contraseñas únicamente con BCrypt y SHALL rechazar cualquier credencial cuyo hash almacenado no sea un hash BCrypt válido.

#### Scenario: Hash no BCrypt no autentica

- **GIVEN** un odontólogo cuyo `password_hash` contiene texto plano en lugar de un hash BCrypt
- **WHEN** envía `POST /api/auth/login` con esa misma cadena como contraseña
- **THEN** la respuesta es 401
- **AND** no se entrega cookie de sesión

### Requirement: La sesión debe usar cookie HttpOnly y SameSite Lax sin token CSRF

La cookie de sesión SHALL ser `HttpOnly` y `SameSite=Lax`, y las solicitudes autenticadas SHALL procesarse sin token CSRF, aceptando el trade-off documentado para la v1.

#### Scenario: Cookie con atributos restrictivos

- **GIVEN** un login correcto
- **WHEN** se inspecciona la cookie de sesión entregada
- **THEN** la cookie es `HttpOnly`
- **AND** la cookie declara `SameSite=Lax`

#### Scenario: Logout sin token CSRF con sesión válida

- **GIVEN** una sesión iniciada
- **WHEN** el cliente envía `POST /api/auth/logout` sin ningún token CSRF
- **THEN** la solicitud se procesa y la sesión queda invalidada
