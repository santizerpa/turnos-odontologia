package com.turnosodontologia.seed;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentService;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.outbox.OutboxDispatchService;
import com.turnosodontologia.outbox.OutboxMessageType;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.waiting.WaitingListEntry;
import com.turnosodontologia.waiting.WaitingListNotificationService;
import com.turnosodontologia.waiting.WaitingListStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DemoDataLoaderCancelledFixtureTests extends DemoLoaderTestSupport {

    @Test
    void cancelledFixtureIsAnInitialStateWithoutOperationalCancellation() {
        Dentist dentist = createDentist();
        Patient waitingPatient = patients.save(new Patient(
            "99000901", "Fuera", "DeDemo", "fuera.demo@example.invalid"));
        WaitingListEntry foreignWaiting = waitingEntries.saveAndFlush(new WaitingListEntry(
            waitingPatient, null, null, WaitingListStatus.WAITING));

        loader().loadOrReuse(dentist);

        Appointment cancelled = appointments.findAll().stream()
            .filter(appointment -> appointment.getStatus() == AppointmentStatus.CANCELLED)
            .findFirst().orElseThrow();
        assertThat(cancelled.getActiveSlot()).isNull();
        assertThat(cancelled.getCancelledAt()).isEqualTo(NOW);

        assertThat(waitingEntries.findAll()).allSatisfy(entry ->
            assertThat(entry.getStatus()).isEqualTo(WaitingListStatus.WAITING));
        assertThat(waitingEntries.findById(foreignWaiting.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);

        assertThat(appointmentEvents.count()).isZero();
        // One confirm/cancel token per booking message (the three active appointments), none for the cancelled one.
        assertThat(tokens.count()).isEqualTo(3);
        assertThat(tokens.findAll()).noneSatisfy(token ->
            assertThat(token.getAppointment().getActiveSlot()).isNull());
        assertThat(outbox.findAll()).allSatisfy(message -> {
            assertThat(message.getType()).isEqualTo(OutboxMessageType.BOOKING);
            assertThat(message.getType()).isNotEqualTo(OutboxMessageType.REMINDER);
        });
        assertThat(outbox.findAll()).extracting(message -> message.getAppointment().getStatus())
            .doesNotContain(AppointmentStatus.CANCELLED);
    }

    @Test
    void loaderDoesNotDependOnOperationalCancellationDispatchOrNotificationServices() {
        List<Class<?>> parameterTypes = Arrays.asList(
            DemoDataLoader.class.getDeclaredConstructors()[0].getParameterTypes());

        assertThat(parameterTypes).doesNotContain(
            AppointmentService.class,
            ApplicationEventPublisher.class,
            OutboxDispatchService.class,
            WaitingListNotificationService.class);
    }
}
