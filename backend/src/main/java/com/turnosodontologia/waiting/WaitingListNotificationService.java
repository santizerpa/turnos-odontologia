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

@Service
public class WaitingListNotificationService {

    private final AppointmentRepository appointments;
    private final WaitingListSelectionService selection;
    private final OutboxMessageRepository outbox;

    public WaitingListNotificationService(AppointmentRepository appointments,
                                          WaitingListSelectionService selection,
                                          OutboxMessageRepository outbox) {
        this.appointments = appointments;
        this.selection = selection;
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean notifyFirstEligible(AppointmentCancelledEvent event) {
        Appointment appointment = appointments.findById(event.appointmentId()).orElse(null);
        if (appointment == null) return false;

        return selection.selectFirstEligible(event.startAt()).map(entry -> {
            if (!entry.markNotified()) return false;
            String body = "Aviso ficticio: se liberó un turno para la fecha solicitada. "
                + "El paciente debe reservarlo desde la disponibilidad pública.";
            outbox.save(new OutboxMessage(appointment, entry.getPatient().getEmail(),
                "Turno disponible", body, OutboxStatus.PENDING));
            return true;
        }).orElse(false);
    }
}
