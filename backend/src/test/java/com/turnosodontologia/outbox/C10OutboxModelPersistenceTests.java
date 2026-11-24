package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class C10OutboxModelPersistenceTests {

    @Autowired EntityManager entityManager;
    @Autowired OutboxMessageRepository outbox;

    @Test
    void bookingConstructorRemainsCompatibleAndTypeIsNullable() {
        Appointment appointment = persistAppointment();

        OutboxMessage booking = new OutboxMessage(appointment, "paciente@demo.invalid", "Reserva", "Cuerpo ficticio", OutboxStatus.PENDING);
        outbox.saveAndFlush(booking);

        assertThat(booking.getType()).isNull();
        assertThat(outbox.findById(booking.getId())).isPresent();
    }

    @Test
    void simulatedSentTransitionOnlyChangesPendingOnce() {
        Appointment appointment = persistAppointment();
        OutboxMessage reminder = new OutboxMessage(appointment, "paciente@demo.invalid", "Recordatorio", "Cuerpo ficticio", OutboxStatus.PENDING, OutboxMessageType.REMINDER);
        Instant first = Instant.parse("2026-11-24T10:00:00Z");
        Instant second = Instant.parse("2026-11-24T11:00:00Z");

        reminder.markSimulatedSent(first);
        assertThat(reminder.getStatus()).isEqualTo(OutboxStatus.SIMULATED_SENT);
        assertThat(reminder.getSentAt()).isEqualTo(first);

        reminder.markSimulatedSent(second);
        assertThat(reminder.getStatus()).isEqualTo(OutboxStatus.SIMULATED_SENT);
        assertThat(reminder.getSentAt()).isEqualTo(first);
    }

    @Test
    void reminderHasPersistentUniqueAppointmentTypeAndExistsQuery() {
        Appointment appointment = persistAppointment();
        OutboxMessage reminder = new OutboxMessage(appointment, "paciente@demo.invalid", "Recordatorio", "Cuerpo ficticio", OutboxStatus.PENDING, OutboxMessageType.REMINDER);
        outbox.saveAndFlush(reminder);

        assertThat(outbox.existsByAppointmentIdAndType(appointment.getId(), OutboxMessageType.REMINDER)).isTrue();

        OutboxMessage duplicate = new OutboxMessage(appointment, "paciente@demo.invalid", "Duplicado", "Cuerpo ficticio", OutboxStatus.PENDING, OutboxMessageType.REMINDER);
        assertThatThrownBy(() -> {
            outbox.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Appointment persistAppointment() {
        Dentist dentist = new Dentist("odontologo@demo.invalid", "$2a$10$fictitious", "Ana", "Demo");
        Patient patient = new Patient("12345678", "Pat", "Ejemplo", "paciente@demo.invalid");
        Appointment appointment = Appointment.create(dentist, patient, Instant.parse("2030-01-01T12:00:00Z"), 30, CreatedBy.PATIENT);
        entityManager.persist(dentist);
        entityManager.persist(patient);
        entityManager.persist(appointment);
        entityManager.flush();
        return appointment;
    }
}
