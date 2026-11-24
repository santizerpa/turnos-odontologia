package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentTokenService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReminderPerAppointmentTransaction {
    private final AppointmentTokenService tokens;
    private final OutboxMessageRepository outbox;

    public ReminderPerAppointmentTransaction(AppointmentTokenService tokens, OutboxMessageRepository outbox) {
        this.tokens = tokens;
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean create(Appointment appointment) {
        if (outbox.existsByAppointmentIdAndType(appointment.getId(), OutboxMessageType.REMINDER)) {
            return false;
        }
        String token = tokens.issue(appointment);
        String confirmLink = "/api/public/appointments/" + token + "/confirm";
        String cancelLink = "/api/public/appointments/" + token + "/cancel";
        String body = "Recordatorio ficticio. Confirmar: " + confirmLink + " Cancelar: " + cancelLink;
        outbox.saveAndFlush(new OutboxMessage(
            appointment, appointment.getPatient().getEmail(), "Recordatorio de turno", body,
            OutboxStatus.PENDING, OutboxMessageType.REMINDER));
        return true;
    }
}
