package com.turnosodontologia.waiting;

import com.turnosodontologia.appointment.AppointmentCancelledEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WaitingListCancelledListener {

    private final WaitingListNotificationService notifications;

    public WaitingListCancelledListener(WaitingListNotificationService notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        notifications.notifyFirstEligible(event);
    }
}
