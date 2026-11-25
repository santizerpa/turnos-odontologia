package com.turnosodontologia.outbox;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outbox")
public class OutboxController {
    private final OutboxMessageRepository outbox;
    private final ReminderGenerationService reminders;
    private final OutboxDispatchService dispatch;

    public OutboxController(OutboxMessageRepository outbox,
                            ReminderGenerationService reminders,
                            OutboxDispatchService dispatch) {
        this.outbox = outbox;
        this.reminders = reminders;
        this.dispatch = dispatch;
    }

    @GetMapping("/messages")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<OutboxMessageDto> messages() {
        return outbox.findAllByOrderByCreatedAtDesc().stream().map(OutboxMessageDto::from).toList();
    }

    @PostMapping("/reminders/run")
    public Map<String, Integer> runReminders() {
        ReminderGenerationResult result = reminders.generate();
        return Map.of("created", result.created(), "failed", result.failed());
    }

    @PostMapping("/dispatch")
    public Map<String, Integer> dispatch() {
        return Map.of("dispatched", this.dispatch.dispatchPending());
    }
}
