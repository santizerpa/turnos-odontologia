# Spec Delta

## ADDED Requirements

### Requirement: El seed de demo se ejecuta solo con opt-in explícito
La carga del conjunto completo de demo SHALL ejecutarse únicamente cuando el operador habilite explícitamente el modo de seed local/demo; el arranque normal y el perfil de pruebas no SHALL crear datos de demo incidentalmente.

#### Scenario: Opt-in habilitado
- **GIVEN** la configuración requerida del odontólogo está presente y el operador habilita el seed de demo
- **WHEN** la aplicación inicia en el modo local/demo configurado
- **THEN** el sistema crea o reutiliza el odontólogo ficticio y carga los datos de demo

#### Scenario: Opt-in ausente o deshabilitado
- **GIVEN** el seed de demo no está explícitamente habilitado
- **WHEN** la aplicación inicia
- **THEN** el sistema no crea pacientes, turnos, bloqueos, entradas de lista de espera ni mensajes de demo
- **AND** el comportamiento existente del seed administrativo conserva su configuración

#### Scenario: Faltan credenciales administrativas
- **GIVEN** el seed de demo está habilitado pero falta `APP_ADMIN_EMAIL` o `APP_ADMIN_PASSWORD`
- **WHEN** se ejecuta el seed
- **THEN** el arranque falla de forma explícita sin crear una contraseña plana ni credenciales predeterminadas

### Requirement: El conjunto de demo incluye datos ficticios relacionados
Cuando el seed de demo se ejecuta en una base local, el sistema SHALL crear o reutilizar un odontólogo y cargar entre cinco y diez pacientes, turnos de demostración, tres entradas `WAITING`, al menos un bloqueo de agenda y mensajes de outbox visibles.

#### Scenario: Carga inicial del dataset
- **GIVEN** una base local preparada y el seed de demo habilitado
- **WHEN** se completa la carga
- **THEN** existe un odontólogo ficticio autenticable con las credenciales configuradas
- **AND** existen entre cinco y diez pacientes ficticios
- **AND** existen dos turnos `CONFIRMED`, uno `PENDING` y uno `CANCELLED`
- **AND** existen tres entradas de lista de espera en estado `WAITING`
- **AND** existe al menos un bloqueo de agenda y un mensaje visible en outbox

#### Scenario: Datos de demo sin información real
- **GIVEN** el seed de demo genera o reutiliza registros
- **WHEN** se inspeccionan los datos persistidos
- **THEN** los nombres, identificadores y emails pertenecen a fixtures ficticios
- **AND** no se incluyen datos reales de pacientes ni secretos en el repositorio

#### Scenario: Los mensajes de demo no producen entrega externa
- **GIVEN** el seed de demo crea mensajes de outbox para revisar la bandeja
- **WHEN** finaliza la carga
- **THEN** los mensajes son solo datos simulados visibles en la aplicación
- **AND** el seed no invoca SMTP, WhatsApp ni otro servicio de entrega externa

### Requirement: Las fechas de demo son reproducibles y relativas al reloj de agenda
El sistema SHALL derivar los horarios de demo de un reloj inyectable y de la zona `America/Argentina/Buenos_Aires`, ubicándolos en días hábiles y franjas configuradas para que la agenda pueda recorrerse al ejecutar la demo.

#### Scenario: Carga con reloj fijado
- **GIVEN** dos ejecuciones aisladas usan la misma fecha, hora y zona del reloj de agenda
- **WHEN** se carga el seed de demo
- **THEN** ambas producen los mismos días y horarios de demo
- **AND** los turnos activos no se solapan entre sí ni con el bloqueo de agenda

#### Scenario: Carga en otra fecha
- **GIVEN** cambia la fecha del reloj de agenda
- **WHEN** se ejecuta el seed en una base preparada para una nueva demo
- **THEN** los horarios se recalculan respecto de esa fecha
- **AND** los turnos activos de demostración permanecen en días hábiles y dentro de las franjas configuradas

### Requirement: Repetir el seed no duplica ni altera datos existentes
El seed SHALL ser idempotente para una misma base y fecha efectiva de demo: una segunda ejecución SHALL conservar el mismo conjunto sin duplicar registros ni sobrescribir datos ajenos a los fixtures.

#### Scenario: Segunda ejecución en la misma fecha efectiva
- **GIVEN** el conjunto de demo ya fue cargado en la base
- **WHEN** se ejecuta nuevamente el seed con la misma fecha efectiva
- **THEN** se conservan las cantidades y relaciones del primer conjunto
- **AND** no se duplican pacientes, turnos, bloqueos, entradas de lista de espera ni mensajes

#### Scenario: Existen registros ajenos a los fixtures
- **GIVEN** la base contiene registros que no pertenecen a los identificadores ficticios reservados para el seed
- **WHEN** se ejecuta el seed de demo
- **THEN** esos registros permanecen sin cambios ni borrado

### Requirement: El procedimiento de demo está documentado
El README SHALL documentar los requisitos, variables ficticias, opt-in explícito y comandos para preparar y arrancar la demo local sin incluir credenciales reales.

#### Scenario: Operador sigue el procedimiento documentado
- **GIVEN** el operador cuenta con Java y PostgreSQL local
- **WHEN** sigue los pasos de demo del README con variables ficticias configuradas
- **THEN** puede iniciar la aplicación con el seed habilitado y revisar los datos de demo
- **AND** puede ejecutar los tests del seed con H2 sin instalar PostgreSQL
