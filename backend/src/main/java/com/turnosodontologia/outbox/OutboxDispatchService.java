package com.turnosodontologia.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class OutboxDispatchService {
    private final OutboxMessageRepository outbox;
    private final Clock scheduleClock;

    public OutboxDispatchService(OutboxMessageRepository outbox, Clock scheduleClock) {
        this.outbox = outbox;
        this.scheduleClock = scheduleClock;
    }

    @Transactional
    public int dispatchPending() {
        int dispatched = 0;
        for (OutboxMessage message : outbox.findAll()) {
            if (message.getStatus() != OutboxStatus.PENDING) continue;
            message.markSimulatedSent(scheduleClock.instant());
            dispatched++;
        }
        return dispatched;
    }
}
