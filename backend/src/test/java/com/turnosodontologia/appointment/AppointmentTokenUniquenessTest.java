package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentTokenUniquenessTest {
    @Autowired EntityManager em;

    @Test
    void duplicateAppointmentTokenHashIsRejectedByDatabase() {
        Dentist dentist = new Dentist("unique-token@demo.invalid", "hash", "Ana", "Demo");
        Patient first = new Patient("22334455", "Primero", "Ficticio", "first-token@example.invalid");
        Patient second = new Patient("22334456", "Segundo", "Ficticio", "second-token@example.invalid");
        em.persist(dentist); em.persist(first); em.persist(second);

        Appointment firstAppointment = Appointment.create(dentist, first, Instant.parse("2030-01-01T12:00:00Z"), 30, CreatedBy.PATIENT);
        Appointment secondAppointment = Appointment.create(dentist, second, Instant.parse("2030-01-01T12:30:00Z"), 30, CreatedBy.PATIENT);
        em.persist(firstAppointment); em.persist(secondAppointment); em.flush();

        String duplicateHash = AppointmentToken.sha256("mismo-token-ficticio");
        em.persist(new AppointmentToken(firstAppointment, duplicateHash, Instant.parse("2030-01-02T12:00:00Z")));
        em.persist(new AppointmentToken(secondAppointment, duplicateHash, Instant.parse("2030-01-02T12:00:00Z")));

        assertThatThrownBy(() -> em.flush()).isInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
}
