package com.turnosodontologia.seed;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.patient.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DemoDataLoaderRollbackTests extends DemoLoaderTestSupport {

    @Autowired DemoDataLoader loader;

    @TestConfiguration
    static class FixedScheduleClock {
        @Bean
        @Primary
        Clock fixedScheduleClock() {
            return Clock.fixed(NOW, ZONE);
        }
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        outbox.deleteAll();
        waitingEntries.deleteAll();
        appointmentEvents.deleteAll();
        tokens.deleteAll();
        appointments.deleteAll();
        blocks.deleteAll();
        configurations.deleteAll();
        patients.deleteAll();
        dentists.deleteAll();
    }

    @Test
    void aConflictingActiveSlotRollsBackTheWholeDemoBatch() {
        Dentist dentist = createDentist();
        Patient foreignPatient = patients.saveAndFlush(new Patient(
            "98000902", "Ajena", "Ficticia", "conflicto@example.invalid"));
        Instant conflictingStart = plan().firstConfirmedStart();
        Appointment foreign = appointments.saveAndFlush(Appointment.create(
            dentist, foreignPatient, conflictingStart, 30, CreatedBy.PATIENT));

        assertThatThrownBy(() -> loader.loadOrReuse(dentist))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("base local de demo limpia");

        assertThat(patients.count()).isEqualTo(1);
        assertThat(patients.findAll()).extracting(Patient::getDni).containsExactly("98000902");
        assertThat(appointments.count()).isEqualTo(1);
        assertThat(blocks.count()).isZero();
        assertThat(waitingEntries.count()).isZero();
        assertThat(outbox.count()).isZero();
        assertThat(appointments.findById(foreign.getId()).orElseThrow())
            .extracting(Appointment::getStartAt, Appointment::getStatus, Appointment::getActiveSlot)
            .containsExactly(conflictingStart, AppointmentStatus.PENDING, conflictingStart);
    }
}
