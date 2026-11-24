package com.turnosodontologia.outbox;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.AppointmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ReminderGenerationService {
    private final AppointmentRepository appointments;
    private final OutboxMessageRepository outbox;
    private final ReminderPerAppointmentTransaction perAppointment;
    private final Clock clock;

    public ReminderGenerationService(AppointmentRepository appointments, OutboxMessageRepository outbox,
                                     ReminderPerAppointmentTransaction perAppointment, Clock scheduleClock) {
        this.appointments = appointments;
        this.outbox = outbox;
        this.perAppointment = perAppointment;
        this.clock = scheduleClock;
    }

    @Transactional(readOnly = true)
    public ReminderGenerationResult generate() {
        Instant now = clock.instant();
        Instant lower = now.plusSeconds(24 * 60 * 60);
        Instant upper = now.plusSeconds(48 * 60 * 60);
        List<Appointment> candidates = appointments.findAll().stream()
            .filter(a -> a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)
            .filter(a -> a.getActiveSlot() != null)
            .filter(a -> a.getStartAt().isAfter(lower) && !a.getStartAt().isAfter(upper))
            .toList();

        int created = 0;
        int failed = 0;
        for (Appointment appointment : candidates) {
            if (outbox.existsByAppointmentIdAndType(appointment.getId(), OutboxMessageType.REMINDER)) continue;
            try {
                if (perAppointment.create(appointment)) {
                    created++;
                }
            } catch (RuntimeException ignored) {
                failed++;
            }
        }
        return new ReminderGenerationResult(created, failed);
    }
}
