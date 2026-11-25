# Spec Delta

## ADDED Requirements

### Requirement: El seed de demo se ejecuta solo con opt-in explícito y fuera de test
La carga del conjunto completo de demo SHALL ejecutarse únicamente cuando el operador habilite `APP_DEMO_SEED=true`; el valor ausente SHALL equivaler a `false`. El runner de demo SHALL estar excluido del perfil `test`, incluso si `APP_DEMO_SEED=true`, y SHALL ejecutarse después de `AdminDentistSeeder` cuando se habilite en un perfil local/demo.

#### Scenario: Opt-in habilitado con configuración administrativa
- **GIVEN** `APP_DEMO_SEED=true`, `APP_ADMIN_SEED=true` y credenciales administrativas válidas
- **WHEN** la aplicación inicia en un perfil local/demo
- **THEN** `AdminDentistSeeder` se ejecuta antes de `DemoDataSeeder`
- **AND** el conjunto de demo se crea o reutiliza para la fecha efectiva

#### Scenario: Opt-in ausente o deshabilitado
- **GIVEN** `APP_DEMO_SEED` está ausente o tiene valor `false`
- **WHEN** la aplicación inicia
- **THEN** no crea pacientes, turnos, bloqueos, entradas de lista de espera ni mensajes de demo
- **AND** el comportamiento existente de `APP_ADMIN_SEED` conserva su configuración

#### Scenario: Demo habilitada sin credenciales o seed administrativo
- **GIVEN** `APP_DEMO_SEED=true` y falta `APP_ADMIN_SEED=true`, `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` o el odontólogo resuelto
- **WHEN** se ejecuta el arranque
- **THEN** falla claramente indicando el requisito faltante
- **AND** no se persiste ningún dato parcial del conjunto de demo

#### Scenario: El perfil test no carga demo aunque el opt-in esté activo
- **GIVEN** el perfil `test` está activo y `APP_DEMO_SEED=true`
- **WHEN** se inicia el contexto de prueba
- **THEN** `DemoDataSeeder` no se ejecuta
- **AND** no aparecen registros de demo por efecto incidental del arranque

### Requirement: El conjunto de demo incluye datos ficticios relacionados y válidos
Cuando el seed de demo se ejecuta en una base local, el sistema SHALL crear o reutilizar un odontólogo y cargar entre cinco y diez pacientes, turnos de demostración, tres entradas `WAITING`, al menos un bloqueo de agenda y mensajes de outbox visibles.

#### Scenario: Carga inicial del dataset
- **GIVEN** una base local preparada, credenciales administrativas válidas y el seed de demo habilitado
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

#### Scenario: Turnos activos respetan ventana y agenda
- **GIVEN** el seed calcula turnos activos a partir del reloj de agenda y la configuración vigente
- **WHEN** persiste los turnos de demo
- **THEN** cada turno activo comienza al menos 24 horas y como máximo 30 días después del instante del reloj
- **AND** cada turno cae en un día y horario configurado y respeta la duración admitida por el dominio
- **AND** ningún turno activo se solapa con otro ni con el bloqueo de agenda
- **AND** los `activeSlot` activos son no nulos y únicos

#### Scenario: No hay slots válidos suficientes
- **GIVEN** la configuración de agenda y la ventana temporal no ofrecen slots suficientes para los fixtures activos
- **WHEN** el seeder intenta generar el conjunto
- **THEN** falla con un error claro
- **AND** no persiste un conjunto parcial ni crea turnos fuera de las reglas

### Requirement: Los fixtures cancelados no ejecutan la cancelación operativa
El turno ficticio `CANCELLED` SHALL inicializarse como estado de fixture coherente con `activeSlot=null`, sin publicar `AppointmentCancelledEvent` ni ejecutar la cancelación de paciente.

#### Scenario: Carga del turno cancelado sin efectos de C-11
- **GIVEN** el seeder construye el turno de demo `CANCELLED`
- **WHEN** persiste el fixture
- **THEN** queda en estado `CANCELLED` con `activeSlot` nulo
- **AND** no publica `AppointmentCancelledEvent`
- **AND** no cambia ninguna entrada `WAITING` ni la convierte en `NOTIFIED`

#### Scenario: No se simulan transiciones imposibles
- **GIVEN** los datos iniciales de demo se están construyendo
- **WHEN** se crean turnos activos o cancelados
- **THEN** el seeder utiliza construcción compatible con el dominio y no llama a la cancelación de paciente con menos de 24 horas de anticipación
- **AND** no escribe directamente un `activeSlot` incompatible con el estado del turno

### Requirement: Los mensajes de demo coexisten con C-10 y no se despachan durante el seed
Los mensajes de demo SHALL permanecer visibles como `PENDING`, usar tipo `BOOKING` (o un tipo explícitamente compatible distinto de `REMINDER`) y destinatarios reservados bajo `.invalid`. El seeder SHALL NOT invocar `OutboxDispatchService` ni servicios externos. Los fixtures SHALL evitar duplicar la clave `(appointment_id, type)`.

#### Scenario: Mensajes demo aislados de recordatorios
- **GIVEN** el seed de demo crea mensajes asociados a turnos
- **WHEN** finaliza la carga
- **THEN** los mensajes son `PENDING`, visibles y de tipo `BOOKING` (o tipo compatible no `REMINDER`)
- **AND** cada turno tiene como máximo un mensaje fixture de ese tipo
- **AND** no se crea ni se reserva un `REMINDER` para el turno
- **AND** un `REMINDER` que C-10 genere para un turno elegible puede coexistir sin colisión con el fixture `BOOKING`

#### Scenario: No hay entrega ni despacho automático
- **GIVEN** el seed de demo crea mensajes de outbox
- **WHEN** finaliza la carga
- **THEN** todos los destinatarios usan el dominio `.invalid`
- **AND** el seed no invoca dispatch, SMTP, WhatsApp ni otro servicio de entrega externa
- **AND** los mensajes siguen `PENDING` hasta que una acción explícita posterior los marque como `SIMULATED_SENT`

### Requirement: Las fechas de demo son reproducibles y válidas respecto del reloj
El sistema SHALL derivar los horarios de demo de un reloj inyectable y de la zona configurada (`America/Argentina/Buenos_Aires`), respetando días hábiles/ventanas configurados y la ventana temporal de 24 horas a 30 días.

#### Scenario: Carga con reloj fijado
- **GIVEN** dos ejecuciones aisladas usan el mismo reloj y la misma configuración de agenda
- **WHEN** se calcula el seed de demo
- **THEN** ambas producen los mismos días y horarios de demo
- **AND** los turnos activos no se solapan entre sí ni con el bloqueo de agenda

#### Scenario: Primera carga en otra fecha y base limpia
- **GIVEN** cambia la fecha efectiva local del reloj y no existe un conjunto de demo previo en la base limpia
- **WHEN** se ejecuta el seed
- **THEN** los horarios se calculan respecto de la nueva fecha
- **AND** todos los turnos activos respetan la ventana de 24 horas a 30 días y la agenda configurada

### Requirement: Repetir el seed no duplica ni altera datos existentes
El seed SHALL ser idempotente para una misma base y fecha efectiva local: una segunda ejecución SHALL reutilizar los fixtures sin duplicarlos ni sobrescribir datos ajenos o datos del conjunto ya cargado.

#### Scenario: Segunda ejecución en la misma fecha efectiva
- **GIVEN** el conjunto de demo ya fue cargado y el reloj conserva la misma fecha local efectiva
- **WHEN** se ejecuta nuevamente el seed
- **THEN** se reutilizan los mismos registros sin mover ni sobrescribirlos
- **AND** no se duplican pacientes, turnos, bloqueos, entradas de lista de espera ni mensajes

#### Scenario: Reinicio de lunes a martes con datos de demo existentes
- **GIVEN** el conjunto de demo fue cargado el lunes y permanece en la base al avanzar el reloj a martes
- **WHEN** se vuelve a iniciar la aplicación con `APP_DEMO_SEED=true`
- **THEN** el seeder no mueve, sobrescribe ni regenera el conjunto existente
- **AND** falla claramente e indica al operador que prepare una base local de demo limpia para cargar la nueva fecha efectiva
- **AND** los datos de demo y los registros ajenos existentes quedan sin cambios

#### Scenario: Existen registros ajenos a los fixtures
- **GIVEN** la base contiene registros que no pertenecen a los identificadores ficticios reservados para el seed
- **WHEN** se ejecuta el seed de demo
- **THEN** esos registros permanecen sin cambios ni borrado

### Requirement: El procedimiento de demo está documentado
El README SHALL documentar las variables administrativas y de demo, su comportamiento seguro por defecto, los requisitos, comandos PowerShell y shell Unix para habilitar explícitamente el seed, y la preparación de una base local limpia sin credenciales reales.

#### Scenario: Operador sigue el procedimiento documentado
- **GIVEN** el operador cuenta con Java y PostgreSQL local
- **WHEN** sigue los pasos de demo del README con variables ficticias y opt-in explícito
- **THEN** puede iniciar la aplicación y revisar los datos de demo
- **AND** los ejemplos muestran `APP_DEMO_SEED=false` por defecto y `APP_DEMO_SEED=true` solo para la demo
- **AND** puede ejecutar los tests del seed con H2 sin instalar PostgreSQL
