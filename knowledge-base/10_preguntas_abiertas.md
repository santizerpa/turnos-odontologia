# Preguntas Abiertas

## Estado resuelto — prioridad alta

1. **[POA-01] Zona horaria.** Se usa hora de Argentina (UTC-3), sin horario de verano. Los feriados no se automatizan en v1; el odontólogo bloquea el día manualmente.
2. **[POA-02] Disponibilidad por defecto.** Lunes a viernes, 9:00-13:00 y 14:00-19:00, salvo bloqueos manuales del odontólogo.
3. **[POA-03] Login inicial.** El perfil local/demo se crea por seed al arrancar. El odontólogo inicia con email y contraseña ficticia; la contraseña se guarda con BCrypt y solo se documenta para demo.
4. **[POA-04] Confirmación.** Los recordatorios se generan 24-48hs antes del turno.
5. **[POA-05] Concurrencia.** La reserva única se garantiza por constraint único/clave de franja y prueba JUnit; conflicto devuelve 409.

## Integridad de datos

- **Email:** solo formato estándar; no se restringe dominio.
- **DNI:** solo números de 7 u 8 dígitos. Se eliminan puntos y espacios; letras y otros caracteres se rechazan.
- **Cancelación desde dos pestañas:** la segunda solicitud ve el turno ya `CANCELLED`, responde “ya cancelado”, no repite efectos y no vuelve a avisar a la lista.
- **Datos antes de anonimizar:** no se hace anonimización ni borrado en v1. Este tema pasa a alcance posterior. La demo usa solo datos ficticios.

## Seguridad

- **TTL del token:** vence al inicio del turno o a los 7 días, lo que ocurra primero.
- **Sesión:** 30 minutos de inactividad.
- **Rate limit:** no se implementa en v1.
- **Contraseña inicial:** ficticia, mínima de 8 caracteres, hash BCrypt y documentada solo para demo.
- **Demo fuera de localhost:** no está habilitada; corre local sin hosting externo.
- **Datos reales:** prohibidos en v1.

## Alcance posterior

- Reprogramación directa por el paciente.
- Envío real por SMTP/WhatsApp.
- Duración variable por prestación y feriados automáticos.
- Odontograma y periodontograma.
- Cobros, señas, Mercado Pago y obras sociales.
- ARCA/facturación.
- Reportes de ausentismo y score de riesgo.
- Multiusuario/multisede.
- Anonimización y política de retención.

## Discrepancias resueltas

- El odontólogo ya no carga turnos manualmente en v1; los pacientes reservan desde la interfaz pública.
- El login usa email, no un usuario genérico.
- Java mínimo es 17 o superior.
- Los pacientes se identifican por datos mínimos y el token de su turno/aviso; no usan login propio.
