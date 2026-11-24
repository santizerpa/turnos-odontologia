package com.turnosodontologia.waiting;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentCancelledEvent;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.Actor;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.schedule.PublicBookingWindow;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WaitingListNotificationServiceTest {
    private static final Instant NOW = Instant.parse("2040-09-10T12:00:00Z");

    @Test
    void doesNotNotifyWhenReleasedAppointmentIsOutsidePublicWindow() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        WaitingListSelectionService selection = mock(WaitingListSelectionService.class);
        OutboxMessageRepository outbox = mock(OutboxMessageRepository.class);
        PublicBookingWindow window = mock(PublicBookingWindow.class);
        Appointment appointment = Appointment.create(mock(Dentist.class), new Patient("70123456", "Paciente", "Ficticio", "p@example.invalid"),
            NOW.plusSeconds(31L * 86400), 30, CreatedBy.DENTIST);
        java.util.UUID appointmentId = java.util.UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(appointment, "id", appointmentId);
        when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(window.isWithin(NOW.plusSeconds(31L * 86400), Clock.fixed(NOW, ZoneOffset.UTC))).thenReturn(false);
        WaitingListNotificationService service = new WaitingListNotificationService(appointments, selection, outbox,
            window, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.notifyFirstEligible(new AppointmentCancelledEvent(
            appointment.getId(), appointment.getStartAt(), Actor.DENTIST))).isFalse();
        verifyNoInteractions(selection, outbox);
    }
}
