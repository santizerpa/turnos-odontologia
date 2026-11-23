package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(AppointmentPersistenceTest.TestClock.class)
@Transactional
class AppointmentPersistenceTest {
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentEventRepository appointmentEvents;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired Clock scheduleClock;

    @Test
    void persistsThirtyMinutePendingAppointmentWithActiveSlot() {
        Dentist dentist = dentists.save(new Dentist("persist.pendiente@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("12345671", "Paciente", "Ficticio", "persist.paciente@odontologia.invalid"));

        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Appointment created = service.create(dentist.getId(), patient.getId(), NOW.plusSeconds(3600), CreatedBy.DENTIST);

        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(created.getDurationMinutes()).isEqualTo(30);
        assertThat(created.getActiveSlot()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(appointments.findById(created.getId())).isPresent();
    }

    @Test
    void rejectsSecondActiveAppointmentInSameSlot() {
        Dentist dentist = dentists.save(new Dentist("persist.conflicto@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient one = patients.save(new Patient("87654321", "Uno", "Ficticio", "persist.uno@odontologia.invalid"));
        Patient two = patients.save(new Patient("87654322", "Dos", "Ficticio", "persist.dos@odontologia.invalid"));
        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Instant slot = NOW.plusSeconds(7200);

        service.create(dentist.getId(), one.getId(), slot, CreatedBy.DENTIST);

        assertThatThrownBy(() -> service.create(dentist.getId(), two.getId(), slot, CreatedBy.DENTIST))
            .isInstanceOf(AppointmentConflictException.class);
        assertThat(appointments.count()).isEqualTo(1);
    }

    @Test
    void cancelledSlotCanBeReusedAndNullSlotsAreUniqueNullable() {
        Dentist dentist = dentists.save(new Dentist("persist.reuse@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient one = patients.save(new Patient("77654321", "Uno", "Ficticio", "persist.reuse1@odontologia.invalid"));
        Patient two = patients.save(new Patient("77654322", "Dos", "Ficticio", "persist.reuse2@odontologia.invalid"));
        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Instant slot = NOW.plusSeconds(10800);

        Appointment first = service.create(dentist.getId(), one.getId(), slot, CreatedBy.DENTIST);
        service.cancel(first.getId(), Actor.DENTIST, "franja ficticia");
        Appointment replacement = service.create(dentist.getId(), two.getId(), slot, CreatedBy.DENTIST);

        assertThat(first.getActiveSlot()).isNull();
        assertThat(replacement.getActiveSlot()).isEqualTo(slot);
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestClock {
        @Bean
        Clock scheduleClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
