package com.turnosodontologia.waiting;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentCancelledEvent;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.outbox.OutboxStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.turnosodontologia.schedule.PublicBookingWindow;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class WaitingListNotificationService {

    private final AppointmentRepository appointments;
    private final WaitingListSelectionService selection;
    private final OutboxMessageRepository outbox;
    private final PublicBookingWindow publicBookingWindow;
    private final Clock scheduleClock;

    public WaitingListNotificationService(AppointmentRepository appointments,
                                          WaitingListSelectionService selection,
                                           OutboxMessageRepository outbox,
                                           PublicBookingWindow publicBookingWindow,
                                           Clock scheduleClock) {
        this.appointments = appointments;
        this.selection = selection;
        this.outbox = outbox;
        this.publicBookingWindow = publicBookingWindow;
        this.scheduleClock = scheduleClock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean notifyFirstEligible(AppointmentCancelledEvent event) {
        Appointment appointment = appointments.findById(event.appointmentId()).orElse(null);
        if (appointment == null) return false;
        if (!publicBookingWindow.isWithin(appointment.getStartAt(), scheduleClock)) return false;

        return selection.selectFirstEligible(event.startAt(), appointment.getPatient().getId()).map(entry -> {
            if (!entry.markNotified()) return false;
            String released = appointment.getStartAt().atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String body = "Aviso ficticio: se liberó un turno para el " + released + ". "
                + "El paciente debe reservarlo desde la disponibilidad pública.";
            outbox.save(new OutboxMessage(appointment, entry.getPatient().getEmail(),
                "Turno disponible", body, OutboxStatus.PENDING));
            return true;
        }).orElse(false);
    }
}
