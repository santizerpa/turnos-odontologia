# Actores y Roles

## Actores del sistema

| Actor | Descripción | Interacción principal |
|---|---|---|
| Odontólogo | Único operador administrativo de la demo | Login, agenda, pacientes, turnos, lista de espera y estados |
| Paciente | Usuario público sin cuenta | Reserva, confirma o cancela su turno mediante enlaces públicos |
| Sistema | Servicio automático | Genera recordatorios simulados y avisa al siguiente paciente |

## RBAC — Matriz de permisos

La v1 tiene un rol autenticado y rutas públicas. No se implementa RBAC multirol avanzado.

| Rol | Agenda | Pacientes | Turnos | Lista de espera | Bandeja salida |
|---|---|---|---|---|---|
| ODONTOLOGO | CRUD | CRUD | CRUD/estado | CRUD + avisar | Lectura |
| PACIENTE_PUBLICO | Solo disponibilidad | Solo crear/buscar por DNI | Crear/confirmar/cancelar propio token | Sin acceso directo | Sin acceso |
| ANONIMO | Sin acceso | Sin acceso | Sin acceso | Sin acceso | Sin acceso |

## Rutas públicas

- Página de reserva.
- Consulta de horarios disponibles.
- Confirmación/cancelación por token.
- Sin listado de pacientes.
- Sin agenda completa pública.

## Autenticación

- Email y contraseña para el odontólogo.
- Contraseña almacenada con BCrypt.
- Sesión/cookie expira después de 30 minutos de inactividad.
- Cookies públicas no identifican al paciente; el token del turno o del aviso es el capability link.
- Los pacientes nunca hacen login.
