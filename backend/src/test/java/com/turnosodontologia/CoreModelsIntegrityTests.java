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

        assertThat(em.createQuery("select count(a) from Appointment a", Long.class).getSingleResult()).isEqualTo(1L);
        Appointment persisted = em.createQuery("select a from Appointment a", Appointment.class)
            .getSingleResult();
        assertThat(persisted.getId()).isEqualTo(a.getId());
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(persisted.getActiveSlot()).isEqualTo(slot);

        Appointment c = em.find(Appointment.class, a.getId());
        c.cancelForFutureDomainService(Instant.parse("2029-12-01T12:00:00Z"));
        em.flush();
        assertThat(em.find(Appointment.class, c.getId()).getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(c.getActiveSlot()).isNull();

        Appointment d = Appointment.create(dentist, two, slot, 30, CreatedBy.PATIENT);
        em.persist(d); em.flush();
        assertThat(em.createQuery("select count(a) from Appointment a", Long.class).getSingleResult()).isEqualTo(2L);
        Appointment replacement = em.find(Appointment.class, d.getId());
        assertThat(replacement.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(replacement.getActiveSlot()).isEqualTo(slot);
        d.cancelForFutureDomainService(Instant.parse("2029-12-02T12:00:00Z"));
        em.flush();
        assertThat(c.getActiveSlot()).isNull();
        assertThat(d.getActiveSlot()).isNull();
        assertThat(em.createQuery("select count(a) from Appointment a", Long.class).getSingleResult()).isEqualTo(2L);
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
    void patientRepositoryPersistsNormalizedUniqueDni() {
        Patient patient = new Patient("12.345.678", "Repo", "Demo", "repo@example.invalid");
        Patient saved = patients.saveAndFlush(patient);
        assertThat(patients.findByDni("12345678")).contains(saved);
    }
}
