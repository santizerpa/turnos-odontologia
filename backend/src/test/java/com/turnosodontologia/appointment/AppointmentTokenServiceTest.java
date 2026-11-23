package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(AppointmentTokenServiceTest.TestClock.class)
class AppointmentTokenServiceTest {
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentTokenRepository tokens;
    @Autowired Clock scheduleClock;

    @Test
    void issuePersistsOnlyHashAndReplacesExistingToken() {
        Dentist dentist = dentists.save(new Dentist("token.issue@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("76543210", "Paciente", "Ficticio", "token.paciente@odontologia.invalid"));
        Appointment appointment = appointments.save(Appointment.create(
            dentist, patient, NOW.plusSeconds(10 * 24 * 60 * 60), 30, CreatedBy.PATIENT));

        AppointmentTokenService service = new AppointmentTokenService(tokens, scheduleClock);

        String firstPlain = service.issue(appointment);
        AppointmentToken firstRecord = tokens.findByAppointmentId(appointment.getId()).orElseThrow();

        assertThat(firstPlain).matches("^[A-Za-z0-9_-]+$");
        assertThat(Base64.getUrlDecoder().decode(firstPlain)).hasSize(32);
        assertThat(firstRecord.getTokenHash()).matches("^[0-9a-f]{64}$");
        assertThat(firstRecord.getTokenHash()).isEqualTo(AppointmentToken.sha256(firstPlain));
        assertThat(firstRecord.getExpiresAt()).isEqualTo(NOW.plusSeconds(7 * 24 * 60 * 60));

        String secondPlain = service.issue(appointment);
        assertThat(secondPlain).isNotEqualTo(firstPlain);

        assertThat(tokens.count()).isEqualTo(1);
        assertThat(tokens.findByTokenHash(AppointmentToken.sha256(firstPlain))).isEmpty();
        assertThat(tokens.findByTokenHash(AppointmentToken.sha256(secondPlain))).containsSame(tokens.findByAppointmentId(appointment.getId()).orElseThrow());
    }

    @Test
    void issueExpiresAtIsStartWhenStartIsBeforeSevenDays() {
        Dentist dentist = dentists.save(new Dentist("token.ttl.before@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("76543211", "Paciente", "Ficticio", "token.ttl.before@odontologia.invalid"));
        Appointment appointment = appointments.save(Appointment.create(
            dentist, patient, NOW.plusSeconds(2 * 24 * 60 * 60), 30, CreatedBy.PATIENT));

        AppointmentTokenService service = new AppointmentTokenService(tokens, scheduleClock);
        service.issue(appointment);

        AppointmentToken record = tokens.findByAppointmentId(appointment.getId()).orElseThrow();
        assertThat(record.getExpiresAt()).isEqualTo(appointment.getStartAt());
    }

    @Test
    void repositoryFindsAppointmentAssociatedWithPlainTokenHash() {
        Dentist dentist = dentists.save(new Dentist("token.lookup@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("76543212", "Paciente", "Ficticio", "token.lookup@odontologia.invalid"));
        Appointment appointment = appointments.save(Appointment.create(
            dentist, patient, NOW.plusSeconds(4 * 24 * 60 * 60), 30, CreatedBy.PATIENT));

        AppointmentTokenService service = new AppointmentTokenService(tokens, scheduleClock);
        String plainToken = service.issue(appointment);

        AppointmentToken found = tokens.findByTokenHash(AppointmentToken.sha256(plainToken)).orElseThrow();
        assertThat(found.getAppointment().getId()).isEqualTo(appointment.getId());
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestClock {
        @Bean
        Clock scheduleClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
