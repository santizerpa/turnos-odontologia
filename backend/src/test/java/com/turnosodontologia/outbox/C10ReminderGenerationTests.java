package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.*;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class C10ReminderGenerationTests {
    static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");

    ReminderGenerationService reminders;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired OutboxMessageRepository outbox;
    @Autowired AppointmentTokenRepository tokens;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AppointmentTokenService tokenService = new AppointmentTokenService(tokens, clock);
        ReminderPerAppointmentTransaction perAppointment = new ReminderPerAppointmentTransaction(tokenService, outbox);
        reminders = new ReminderGenerationService(appointments, outbox, perAppointment, clock);
    }

    @Test
    void eligiblePendingAndConfirmedAndStrictBoundsUseFixedClock() {
        Dentist dentist = dentists.save(new Dentist("reminder.bounds@demo.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("81001001", "Paciente", "Ficticio", "paciente.bounds@demo.invalid"));

        persist(dentist, patient, NOW.plusSeconds(24 * 3600), AppointmentStatus.PENDING);
        persist(dentist, patient, NOW.plusSeconds(36 * 3600), AppointmentStatus.CONFIRMED);
        persist(dentist, patient, NOW.plusSeconds(48 * 3600), AppointmentStatus.PENDING);

        ReminderGenerationResult result = reminders.generate();

        assertThat(result.created()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(outbox.findAll()).hasSize(2);
        assertThat(outbox.findAll()).allSatisfy(message -> {
            assertThat(message.getType()).isEqualTo(OutboxMessageType.REMINDER);
            assertThat(message.getBody()).contains("/api/public/appointments/").contains("/confirm").contains("/cancel");
        });
    }

    @Test
    void excludesCancelledAndInactiveOrOutsideWindow() {
        Dentist dentist = dentists.save(new Dentist("reminder.exclusions@demo.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("81001002", "Paciente", "Ficticio", "paciente.exclusions@demo.invalid"));

        persist(dentist, patient, NOW.plusSeconds(23 * 3600), AppointmentStatus.CONFIRMED);
        persist(dentist, patient, NOW.plusSeconds(49 * 3600), AppointmentStatus.PENDING);
        persist(dentist, patient, NOW.plusSeconds(36 * 3600), AppointmentStatus.CANCELLED);

        ReminderGenerationResult result = reminders.generate();

        assertThat(result.created()).isZero();
        assertThat(result.failed()).isZero();
        assertThat(outbox.findAll()).isEmpty();
    }

    @Test
    void secondRunIsIdempotentAndDoesNotDuplicate() {
        Dentist dentist = dentists.save(new Dentist("reminder.idempotent@demo.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("81001003", "Paciente", "Ficticio", "paciente.idempotent@demo.invalid"));
        persist(dentist, patient, NOW.plusSeconds(36 * 3600), AppointmentStatus.PENDING);

        ReminderGenerationResult first = reminders.generate();
        ReminderGenerationResult second = reminders.generate();

        assertThat(first.created()).isEqualTo(1);
        assertThat(second.created()).isZero();
        assertThat(outbox.findAll()).hasSize(1);
    }

    private Appointment persist(Dentist dentist, Patient patient, Instant startAt, AppointmentStatus status) {
        Appointment appointment = Appointment.create(dentist, patient, startAt, 30, CreatedBy.DENTIST);
        if (status == AppointmentStatus.CONFIRMED) appointment.confirmForFutureDomainService();
        if (status == AppointmentStatus.CANCELLED) appointment.cancelForFutureDomainService(NOW);
        return appointments.saveAndFlush(appointment);
    }

}
