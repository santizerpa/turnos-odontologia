# Tasks

## 1. Preparación de pruebas de persistencia

- [x] 1.1 Escribir primero pruebas @DataJpaTest de mapeo y relaciones para las ocho entidades; verificar que quedan rojas y luego verdes con el esquema implementado.
- [x] 1.2 Escribir pruebas de repositorios Spring Data por entidad y verificar operaciones CRUD básicas con datos ficticios en H2.
- [x] 1.3 Escribir pruebas de auditoría y soft delete; verificar que deleted_at y updated_at quedan consistentes sin borrado físico.

## 2. Modelo JPA y enums

- [x] 2.1 Implementar UUID, enums string y entidades JPA base; verificar que el mínimo de las pruebas 1.1–1.3 pasa.
- [x] 2.2 Implementar relaciones Dentist→Appointments, Dentist→ScheduleBlocks y las relaciones del ERD; verificar mapeo JPA y claves foráneas en test.
- [x] 2.3 Implementar los ocho repositorios Spring Data; verificar carga del contexto y operaciones de 1.2.

## 3. Integridad de slots y turnos

- [x] 3.1 Escribir primero prueba de restricción UNIQUE sobre Appointment.active_slot; verificar falla roja y luego que dos turnos activos en la misma franja sean rechazados.
- [x] 3.2 Implementar nullable unique sin índices parciales dependientes de PostgreSQL; verificar que múltiples CANCELLED con NULL coexisten en H2.
- [x] 3.3 Escribir prueba de modelo para cancelación/rereservación y token; verificar que CANCELLED deja active_slot NULL y la nueva reserva puede ocupar la franja.
- [x] 3.4 Implementar la representación de estados y el mantenimiento de active_slot con API no pública para escritura directa; verificar que solo el método de dominio futuro puede cambiarlo.
- [x] 3.5 Escribir pruebas de AppointmentToken con SHA-256, sin campo plano y vigencia persistida; verificar hash y ausencia de consumo al confirmar.
- [x] 3.6 Escribir casos de confirmación repetida y CANCELLED→confirmación rechazada como soporte de C-09; verificar que el modelo conserva estado y token.

## 4. Validación de DNI y email

- [x] 4.1 Escribir primero pruebas unitarias de DNI válido, puntos/espacios, letras, longitud y normalización; verificar rojo-verde-refactor.
- [x] 4.2 Implementar normalización y unicidad de DNI; verificar que formatos equivalentes convergen al mismo valor persistido.
- [x] 4.3 Escribir pruebas de email válido e inválido; verificar rechazo con formato estándar y persistencia normalizada.

## 5. ScheduleBlock y datos auxiliares

- [x] 5.1 Escribir primero pruebas de ScheduleBlock con rango, motivo, Dentist FK y soft delete; verificar persistencia y asociación.
- [x] 5.2 Implementar ScheduleBlock sin calcular slots ni disponibilidad; verificar que solo persiste start_at/end_at y sus auditorías.
- [x] 5.3 Escribir pruebas de WaitingListEntry, OutboxMessage y AppointmentEvent con enums string y relaciones; verificar persistencia mínima.

## 6. Esquema y seed inicial

- [x] 6.1 Escribir primero prueba de carga del esquema y variables APP_ADMIN ausentes/presentes; verificar fallo explícito sin secretos reales.
- [x] 6.2 Crear el esquema inicial portable para H2 PostgreSQL mode y PostgreSQL; verificar claves foráneas, índices y restricciones básicas en ambos perfiles.
- [x] 6.3 Implementar el seed del Dentist desde APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD con BCrypt; verificar que password_hash nunca contiene la contraseña plana.
- [x] 6.4 Implementar datos seed ficticios opcionales para pacientes, turnos, espera, bloqueos y outbox; verificar que no aparecen datos reales.

## 7. Paridad y verificación final

- [x] 7.1 Ejecutar ./gradlew test con perfil test y corregir fallos sin omitir pruebas ni usar PostgreSQL como requisito.
- [x] 7.2 Ejecutar ./gradlew build y registrar que compile, empaquete y pase el ciclo relevante.
- [x] 7.3 Verificar manual o automáticamente el esquema PostgreSQL local y comparar UNIQUE, FKs, enums, timestamps y soft delete con H2.
- [x] 7.4 Revisar que no existe código externo que escriba active_slot ni campos planos de token; verificar con pruebas y revisión del diff.

## Workflow follow-up

- Ejecutar `openspec status --change core-models --json` y `openspec validate core-models --strict` hasta obtener resultado válido.
- Mantener todas las tareas en [ ] hasta que el change sea aplicado y verificado por una solicitud posterior.
