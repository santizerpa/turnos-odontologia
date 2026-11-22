package com.turnosodontologia;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.appointment.AppointmentEvent;
import com.turnosodontologia.appointment.Actor;
import com.turnosodontologia.appointment.AppointmentToken;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.schedule.ScheduleBlock;
import com.turnosodontologia.waiting.WaitingListEntry;
import com.turnosodontologia.waiting.WaitingListStatus;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CoreModelsMappingTests {

    @Autowired
    EntityManager entityManager;

    @Test
    void mappingContextHasTheEightDomainEntities() {
        Dentist dentist = new Dentist("admin@demo.invalid", "$2a$10$fictitious", "Ana", "Demo");
        Patient patient = new Patient("12345678", "Pat", "Ejemplo", "pat@example.invalid");
        Appointment appointment = Appointment.create(dentist, patient, Instant.parse("2030-01-01T12:00:00Z"), 30, CreatedBy.PATIENT);

        entityManager.persist(dentist);
        entityManager.persist(patient);
        entityManager.persist(appointment);

        AppointmentToken token = new AppointmentToken(appointment, "hash", Instant.parse("2030-01-01T12:00:00Z"));
        AppointmentEvent event = new AppointmentEvent(appointment, null, AppointmentStatus.PENDING, Actor.PATIENT, "creado");
        WaitingListEntry waiting = new WaitingListEntry(patient, LocalDate.of(2030, 1, 1), null, WaitingListStatus.WAITING);
        OutboxMessage outbox = new OutboxMessage(appointment, "pat@example.invalid", "Asunto", "Cuerpo", OutboxStatus.PENDING);
        ScheduleBlock block = new ScheduleBlock(dentist, Instant.parse("2030-01-01T12:00:00Z"), Instant.parse("2030-01-01T13:00:00Z"), "Bloqueo demo");

        entityManager.persist(token);
        entityManager.persist(event);
        entityManager.persist(waiting);
        entityManager.persist(outbox);
        entityManager.persist(block);
        entityManager.flush();

        assertThat(appointment.getDentist().getId()).isEqualTo(dentist.getId());
        assertThat(appointment.getPatient().getId()).isEqualTo(patient.getId());
        assertThat(token.getAppointment().getId()).isEqualTo(appointment.getId());
        assertThat(block.getDentist().getId()).isEqualTo(dentist.getId());
        assertThat(waiting.getPatient().getId()).isEqualTo(patient.getId());
        assertThat(outbox.getAppointment().getId()).isEqualTo(appointment.getId());
    }
}
