package com.turnosodontologia;

import com.turnosodontologia.appointment.*;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
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
class CoreModelsIntegrityTests {
    @Autowired EntityManager em;
    @Autowired PatientRepository patients;

    @Test
    void dniNormalizesAndEmailValidates() {
        Patient patient = new Patient("12.345.678", "Pat", "Ejemplo", "  PAT@example.invalid  ");
        assertThat(patient.getDni()).isEqualTo("12345678");
        assertThat(patient.getEmail()).isEqualTo("pat@example.invalid");
        assertThatThrownBy(() -> new Patient("1234A678", "Pat", "Ejemplo", "pat@example.invalid"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Patient("12345678", "Pat", "Ejemplo", "not-an-email"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activeSlotUniqueRejectsSecondActiveTurnAndAllowsCancelledNulls() {
        Dentist dentist = new Dentist("admin@demo.invalid", "hash", "Ana", "Demo");
        Patient one = new Patient("12345678", "Uno", "Demo", "uno@example.invalid");
        Patient two = new Patient("87654321", "Dos", "Demo", "dos@example.invalid");
        em.persist(dentist); em.persist(one); em.persist(two);

        Instant slot = Instant.parse("2030-01-01T12:00:00Z");
        Appointment a = Appointment.create(dentist, one, slot, 30, CreatedBy.PATIENT);
        Appointment b = Appointment.create(dentist, two, slot, 30, CreatedBy.PATIENT);
        em.persist(a); em.flush();
        em.persist(b);
        assertThatThrownBy(() -> em.flush()).isInstanceOfAny(
            DataIntegrityViolationException.class,
            jakarta.persistence.PersistenceException.class,
            org.hibernate.exception.ConstraintViolationException.class);
        em.clear();

        Appointment c = em.find(Appointment.class, a.getId());
        c.cancelForFutureDomainService(Instant.parse("2029-12-01T12:00:00Z"));
        em.flush();
        assertThat(c.getActiveSlot()).isNull();

        Appointment d = Appointment.create(dentist, two, slot, 30, CreatedBy.PATIENT);
        em.persist(d); em.flush();
        d.cancelForFutureDomainService(Instant.parse("2029-12-02T12:00:00Z"));
        em.flush();
        assertThat(c.getActiveSlot()).isNull();
        assertThat(d.getActiveSlot()).isNull();
    }

    @Test
    void tokenHashIsSha256AndConfirmationDoesNotConsumeIt() {
        String plain = "opaque-token-with-enough-entropy";
        String hash = AppointmentToken.sha256(plain);
        assertThat(hash).hasSize(64).doesNotContain(plain);

        Dentist dentist = new Dentist("admin2@demo.invalid", "hash", "Ana", "Demo");
        Patient patient = new Patient("11223344", "Token", "Demo", "token@example.invalid");
        em.persist(dentist); em.persist(patient);
        Appointment appointment = Appointment.create(dentist, patient, Instant.parse("2030-01-01T12:00:00Z"), 30, CreatedBy.PATIENT);
        em.persist(appointment);
        AppointmentToken token = new AppointmentToken(appointment, hash, Instant.parse("2030-01-02T12:00:00Z"));
        em.persist(token); em.flush();

        assertThat(appointment.confirmForFutureDomainService()).isTrue();
        assertThat(appointment.confirmForFutureDomainService()).isFalse();
        em.flush();
        assertThat(token.getTokenHash()).isEqualTo(hash);
        assertThat(token.getExpiresAt()).isEqualTo(Instant.parse("2030-01-02T12:00:00Z"));

        appointment.cancelForFutureDomainService(Instant.parse("2029-12-01T12:00:00Z"));
        assertThat(appointment.confirmForFutureDomainService()).isFalse();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

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

    @Test
    void patientRepositoryPersistsNormalizedUniqueDni() {
        Patient patient = new Patient("12.345.678", "Repo", "Demo", "repo@example.invalid");
        Patient saved = patients.saveAndFlush(patient);
        assertThat(patients.findByDni("12345678")).contains(saved);
    }
}
