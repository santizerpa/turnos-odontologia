package com.turnosodontologia.waiting;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentCancelledEvent;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.appointment.Actor;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.outbox.OutboxMessageType;
import com.turnosodontologia.outbox.OutboxStatus;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WaitingListNotificationIntegrationTest {

    @Autowired WaitingListNotificationService notifications;
    @Autowired WaitingListCancelledListener listener;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired WaitingListEntryRepository waitingEntries;
    @Autowired OutboxMessageRepository outbox;
    @Autowired AppointmentRepository appointments;
    @Autowired PatientRepository patients;
    @Autowired DentistRepository dentists;
    @Autowired PasswordEncoder passwordEncoder;

    private Dentist dentist;
    private Appointment cancelledAppointment;

    @BeforeEach
    void setUp() {
        outbox.deleteAll();
        waitingEntries.deleteAll();
        appointments.deleteAll();
        patients.deleteAll();
        dentists.deleteAll();
        dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("waiting-notification"), "demo-ficticia-123");
        Patient appointmentPatient = patients.save(new Patient(
            "70123456", "Paciente Turno", "Ficticio", "turno@example.invalid"));
        cancelledAppointment = appointments.saveAndFlush(Appointment.create(dentist, appointmentPatient,
            Instant.parse("2040-09-12T15:00:00Z"), 30, CreatedBy.DENTIST));
        cancelledAppointment.cancelForFutureDomainService(Instant.parse("2040-09-01T12:00:00Z"));
        appointments.saveAndFlush(cancelledAppointment);
    }

    @Test
    void marksOneFifoEntryAndCreatesUntypedPendingOutboxMessage() {
        Patient waitingPatient = patients.save(new Patient(
            "70123457", "Paciente Espera", "Ficticio", "espera@example.invalid"));
        WaitingListEntry entry = waitingEntries.saveAndFlush(new WaitingListEntry(
            waitingPatient, LocalDate.of(2040, 9, 12), LocalDate.of(2040, 9, 12), WaitingListStatus.WAITING));

        boolean notified = notifications.notifyFirstEligible(new AppointmentCancelledEvent(
            cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST));

        assertThat(notified).isTrue();
        assertThat(waitingEntries.findById(entry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
        OutboxMessage message = outbox.findAll().stream().findFirst().orElseThrow();
        assertThat(message.getAppointment().getId()).isEqualTo(cancelledAppointment.getId());
        assertThat(message.getRecipientEmail()).isEqualTo("espera@example.invalid");
        assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(message.getType()).isNull();
    }

    @Test
    void listenerConsumesManuallyPublishedEventAfterCommit() {
        Patient waitingPatient = patients.save(new Patient(
            "70123460", "Paciente Evento", "Ficticio", "evento@example.invalid"));
        WaitingListEntry entry = waitingEntries.saveAndFlush(new WaitingListEntry(
            waitingPatient, null, null, WaitingListStatus.WAITING));

        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new AppointmentCancelledEvent(
                cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST));
            return null;
        });

        assertThat(waitingEntries.findById(entry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    void doesNotNotifyWhenNoEntryIsEligible() {
        Patient waitingPatient = patients.save(new Patient(
            "70123458", "Paciente Fuera", "Ficticio", "fuera@example.invalid"));
        WaitingListEntry entry = waitingEntries.saveAndFlush(new WaitingListEntry(
            waitingPatient, LocalDate.of(2040, 9, 13), LocalDate.of(2040, 9, 14), WaitingListStatus.WAITING));

        boolean notified = notifications.notifyFirstEligible(new AppointmentCancelledEvent(
            cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST));

        assertThat(notified).isFalse();
        assertThat(waitingEntries.findById(entry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);
        assertThat(outbox.count()).isZero();
    }

    @Test
    void nullTypedWaitingNoticeCoexistsWithReminder() {
        Patient waitingPatient = patients.save(new Patient(
            "70123459", "Paciente Aviso", "Ficticio", "aviso@example.invalid"));
        WaitingListEntry entry = waitingEntries.saveAndFlush(new WaitingListEntry(
            waitingPatient, null, null, WaitingListStatus.WAITING));
        outbox.saveAndFlush(new OutboxMessage(cancelledAppointment, "recordatorio@example.invalid",
            "Recordatorio", "Recordatorio ficticio", OutboxStatus.PENDING, OutboxMessageType.REMINDER));

        assertThat(notifications.notifyFirstEligible(new AppointmentCancelledEvent(
            cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST))).isTrue();

        assertThat(waitingEntries.findById(entry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
        assertThat(outbox.findAll()).hasSize(2);
        assertThat(outbox.findAll().stream().filter(message -> message.getType() == OutboxMessageType.REMINDER))
            .hasSize(1);
        assertThat(outbox.findAll().stream().filter(message -> message.getType() == null)).hasSize(1);
    }
}
