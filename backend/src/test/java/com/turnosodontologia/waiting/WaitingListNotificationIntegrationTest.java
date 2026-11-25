package com.turnosodontologia.waiting;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentCancelledEvent;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.AppointmentStatus;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(WaitingListNotificationIntegrationTest.TestClock.class)
class WaitingListNotificationIntegrationTest {
    private static final Clock TEST_CLOCK = Clock.fixed(
        Instant.parse("2026-11-25T15:00:00Z"), ZoneOffset.UTC);

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
    @Autowired JdbcTemplate jdbcTemplate;

    private Dentist dentist;
    private Appointment cancelledAppointment;

    @BeforeEach
    void setUp() {
        outbox.deleteAll();
        jdbcTemplate.update("DELETE FROM appointment_events");
        jdbcTemplate.update("DELETE FROM appointment_tokens");
        waitingEntries.deleteAll();
        appointments.deleteAll();
        patients.deleteAll();
        dentist = AuthFixtures.createActive(dentists, passwordEncoder,
            AuthFixtures.uniqueEmail("waiting-notification"), "demo-ficticia-123");
        Patient appointmentPatient = patients.save(new Patient(
            "70123456", "Paciente Turno", "Ficticio", "turno@example.invalid"));
        cancelledAppointment = appointments.saveAndFlush(Appointment.create(dentist, appointmentPatient,
            TEST_CLOCK.instant().plusSeconds(48 * 3600L), 30, CreatedBy.DENTIST));
        cancelledAppointment.cancelForFutureDomainService(TEST_CLOCK.instant());
        appointments.saveAndFlush(cancelledAppointment);
    }

    @Test
    void marksOneFifoEntryAndCreatesUntypedPendingOutboxMessage() {
        Patient waitingPatient = patients.save(new Patient(
            "70123457", "Paciente Espera", "Ficticio", "espera@example.invalid"));
        WaitingListEntry entry = waitingEntries.saveAndFlush(new WaitingListEntry(
            waitingPatient, releasedDate(), releasedDate(), WaitingListStatus.WAITING));

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
        assertThat(message.getBody()).isEqualTo(
            "Aviso ficticio: se liberó un turno para el 27/11/2026 12:00. "
                + "El paciente debe reservarlo desde la disponibilidad pública.");
    }

    @Test
    void repeatedCancellationEventDoesNotNotifyAnotherEntryOrAssignTheReleasedAppointment() {
        Patient firstPatient = patients.save(new Patient(
            "70123462", "Paciente FIFO", "Primero", "primero.fifo@example.invalid"));
        Patient secondPatient = patients.save(new Patient(
            "70123463", "Paciente FIFO", "Segundo", "segundo.fifo@example.invalid"));
        WaitingListEntry firstEntry = waitingEntries.saveAndFlush(new WaitingListEntry(
            firstPatient, null, null, WaitingListStatus.WAITING));
        WaitingListEntry secondEntry = waitingEntries.saveAndFlush(new WaitingListEntry(
            secondPatient, null, null, WaitingListStatus.WAITING));
        jdbcTemplate.update("UPDATE waiting_list_entries SET created_at = ? WHERE id = ?",
            Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")), firstEntry.getId());
        jdbcTemplate.update("UPDATE waiting_list_entries SET created_at = ? WHERE id = ?",
            Timestamp.from(Instant.parse("2026-01-01T10:00:01Z")), secondEntry.getId());
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
            cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST);

        assertThat(notifications.notifyFirstEligible(event)).isTrue();
        assertThat(notifications.notifyFirstEligible(event)).isFalse();

        assertThat(waitingEntries.findById(firstEntry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
        assertThat(waitingEntries.findById(secondEntry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);
        assertThat(outbox.findAll()).singleElement()
            .extracting(OutboxMessage::getRecipientEmail)
            .isEqualTo("primero.fifo@example.invalid");
        assertThat(appointments.findAll()).singleElement().satisfies(appointment -> {
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment.getActiveSlot()).isNull();
        });
    }

    @Test
    void doesNotNotifyPatientWhoCancelledAndChoosesNextEligiblePatient() {
        Patient cancellingPatient = cancelledAppointment.getPatient();
        WaitingListEntry cancelledPatientEntry = waitingEntries.saveAndFlush(new WaitingListEntry(
            cancellingPatient, null, null, WaitingListStatus.WAITING));
        Patient otherPatient = patients.save(new Patient(
            "70123461", "Paciente Siguiente", "Ficticio", "siguiente@example.invalid"));
        WaitingListEntry otherEntry = waitingEntries.saveAndFlush(new WaitingListEntry(
            otherPatient, null, null, WaitingListStatus.WAITING));

        assertThat(notifications.notifyFirstEligible(new AppointmentCancelledEvent(
            cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST))).isTrue();

        assertThat(waitingEntries.findById(cancelledPatientEntry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);
        assertThat(waitingEntries.findById(otherEntry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.NOTIFIED);
        assertThat(outbox.findAll()).singleElement()
            .extracting(OutboxMessage::getRecipientEmail)
            .isEqualTo("siguiente@example.invalid");
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
            waitingPatient, releasedDate().plusDays(1), releasedDate().plusDays(2), WaitingListStatus.WAITING));

        boolean notified = notifications.notifyFirstEligible(new AppointmentCancelledEvent(
            cancelledAppointment.getId(), cancelledAppointment.getStartAt(), Actor.DENTIST));

        assertThat(notified).isFalse();
        assertThat(waitingEntries.findById(entry.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);
        assertThat(outbox.count()).isZero();
    }

    private LocalDate releasedDate() {
        return cancelledAppointment.getStartAt().atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toLocalDate();
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

    @TestConfiguration
    static class TestClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return TEST_CLOCK;
        }
    }
}
