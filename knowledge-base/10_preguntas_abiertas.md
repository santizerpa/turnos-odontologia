# Preguntas Abiertas

## Prioridad Alta

1. **[POA-01] Ventana y timezone.** Confirmar timezone local, feriados y horarios laborales del odontólogo.
2. **[POA-02] Configuración de disponibilidad.** ¿Cuál es el rango horario diario? Ejemplo 9-13 y 14-19.
3. **[POA-03] Login inicial.** Confirmar si el usuario inicial viene por seed, env o migración.
4. **[POA-04] Recomendación de confirmación:** usar solo 24-48hs antes o configurable.
5. **[POA-05] Concurrencia:** validar el único índice de turno activo bajo PostgreSQL y pruebas JUnit.

## Integridad de datos

- ¿Email debe validar dominio/formato o solo formato?
- ¿DNI admite letras/puntos/espacios y cómo se normaliza?
- ¿Qué sucede si el paciente cancela desde dos pestañas simultáneamente?
- ¿Qué datos deben conservarse antes de anonimizar? (No definidas en v1.)

## Seguridad

- Definir TTL de token y sesiones.
- Definir rate limits para el endpoint público.
- Definir política de contraseñas iniciales.
- Confirmar si la demo puede abrirse fuera de localhost.
- Datos reales no deben cargarse hasta definir privacidad y retención.

## Alcance posterior

- Reprogramación directa por el paciente.
- Envío real por SMTP/WhatsApp.
- Duración variable por prestación.
- Odontograma y periodontograma.
- Cobros y señas.
- ARCA/obras sociales.
- Reportes de ausentismo y score de riesgo.
- Multiusuario/multisede.

## Discrepancias detectadas

- Discovery decía “sin roles/permisos” y luego se aprobó login usuario/contraseña: se interpreta como un único rol autenticado, no RBAC complejo.
- Discovery decía H2 sin instalar; PostgreSQL sigue siendo obligatorio solo para ejecución local.
- La lista de espera se aprobó con aviso automático; no se aprobó asignación automática ni respuesta por parte del paciente en v1.
