package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.*;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;


import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
class C10ReminderIsolationTests {
    @Autowired ReminderGenerationService reminders;
    @Autowired Clock scheduleClock;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired OutboxMessageRepository outbox;
    @Autowired AppointmentTokenRepository tokens;
    @SpyBean AppointmentTokenService tokenService;
    @SpyBean OutboxMessageRepository outboxSpy;

    private Dentist dentist;
    private Patient patient;
    private Appointment failed;
    private Appointment succeeded;

    @AfterEach
    void cleanCreatedData() {
        if (failed != null) outbox.findAll().stream()
            .filter(message -> failed.getId().equals(message.getAppointment().getId()))
            .forEach(message -> outbox.deleteById(message.getId()));
        if (succeeded != null) outbox.findAll().stream()
            .filter(message -> succeeded.getId().equals(message.getAppointment().getId()))
            .forEach(message -> outbox.deleteById(message.getId()));
        if (failed != null) {
            tokens.findByAppointmentId(failed.getId()).ifPresent(token -> tokens.deleteById(token.getId()));
            appointments.deleteById(failed.getId());
        }
        if (succeeded != null) {
            tokens.findByAppointmentId(succeeded.getId()).ifPresent(token -> tokens.deleteById(token.getId()));
            appointments.deleteById(succeeded.getId());
        }
        if (patient != null) patients.deleteById(patient.getId());
        if (dentist != null) dentists.deleteById(dentist.getId());
    }

    @Test
    void outboxSaveFailureRevertsReissueAndLeavesNoReminderWithoutTransactionalTest() {
        dentist = dentists.save(new Dentist("save.rollback@demo.invalid", "hash-ficticio", "Ana", "Demo"));
        patient = patients.save(new Patient("81002002", "Paciente", "Rollback", "save.rollback.patient@demo.invalid"));
        failed = appointments.saveAndFlush(Appointment.create(dentist, patient,
            scheduleClock.instant().plusSeconds(36 * 3600), 30, CreatedBy.DENTIST));

        String oldToken = tokenService.issue(failed);

        Mockito.doAnswer(invocation -> {
            OutboxMessage message = invocation.getArgument(0);
            if (message.getType() == OutboxMessageType.REMINDER) {
                throw new IllegalStateException("Fallo ficticio al guardar REMINDER");
            }
            return invocation.callRealMethod();
        }).when(outboxSpy).saveAndFlush(any(OutboxMessage.class));

        ReminderGenerationResult result = reminders.generate();

        assertThat(result.created()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(outbox.existsByAppointmentIdAndType(failed.getId(), OutboxMessageType.REMINDER)).isFalse();
        assertThat(tokenService.resolve(oldToken)).isNotNull();
    }

    @Test
    void isolatedIssueFailureKeepsOldTokenAndProcessesOtherAppointment() {
        dentist = dentists.save(new Dentist("isolation@demo.invalid", "hash-ficticio", "Ana", "Demo"));
        patient = patients.save(new Patient("81002001", "Paciente", "Ficticio", "isolation.patient@demo.invalid"));
        failed = appointments.saveAndFlush(Appointment.create(dentist, patient, scheduleClock.instant().plusSeconds(30 * 3600), 30, CreatedBy.DENTIST));
        succeeded = appointments.saveAndFlush(Appointment.create(dentist, patient, scheduleClock.instant().plusSeconds(36 * 3600), 30, CreatedBy.DENTIST));

        String oldToken = tokenService.issue(failed);

        Mockito.doAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            if (appointment.getId().equals(failed.getId())) {
                throw new IllegalStateException("Fallo ficticio de emisión");
            }
            return invocation.callRealMethod();
        }).when(tokenService).issue(any(Appointment.class));

        assertThat(failed.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(failed.getActiveSlot()).isNotNull();
        assertThat(failed.getStartAt()).isAfter(scheduleClock.instant().plusSeconds(24 * 3600));
        assertThat(failed.getStartAt()).isBeforeOrEqualTo(scheduleClock.instant().plusSeconds(48 * 3600));
        assertThat(succeeded.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(succeeded.getActiveSlot()).isNotNull();
        assertThat(succeeded.getStartAt()).isAfter(scheduleClock.instant().plusSeconds(24 * 3600));
        assertThat(succeeded.getStartAt()).isBeforeOrEqualTo(scheduleClock.instant().plusSeconds(48 * 3600));
        ReminderGenerationResult result = reminders.generate();

        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(outbox.existsByAppointmentIdAndType(failed.getId(), OutboxMessageType.REMINDER)).isFalse();
        assertThat(outbox.existsByAppointmentIdAndType(succeeded.getId(), OutboxMessageType.REMINDER)).isTrue();
        assertThat(outbox.findAll().stream()
            .filter(message -> succeeded.getId().equals(message.getAppointment().getId()))
            .findFirst()
            .orElseThrow()
            .getBody()).contains("/api/public/appointments/");
        assertThat(tokenService.resolve(oldToken)).isNotNull();
    }

}
