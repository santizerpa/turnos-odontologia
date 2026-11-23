package com.turnosodontologia.appointment;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AppointmentService {
    private static final Duration PATIENT_MINIMUM_NOTICE = Duration.ofHours(24);

    private final AppointmentRepository appointments;
    private final AppointmentEventRepository appointmentEvents;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments,
                              AppointmentEventRepository appointmentEvents,
                              ApplicationEventPublisher eventPublisher,
                              Clock scheduleClock) {
        this.appointments = appointments;
        this.appointmentEvents = appointmentEvents;
        this.eventPublisher = eventPublisher;
        this.clock = scheduleClock;
    }

    @Transactional
    public CancelAppointmentResult cancel(UUID appointmentId, Actor actor, String reason) {
        Appointment appointment = appointments.findById(appointmentId)
            .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return CancelAppointmentResult.alreadyCancelled();
        }

        Instant now = clock.instant();
        if (actor == Actor.PATIENT && appointment.getStartAt().isBefore(now.plus(PATIENT_MINIMUM_NOTICE))) {
            throw new IllegalStateException("El turno solo puede cancelarse con al menos 24 horas de anticipación");
        }

        return applyTransition(appointment, AppointmentStatus.CANCELLED, actor, reason);
    }

    @Transactional
    public Appointment changeStatus(UUID appointmentId, AppointmentStatus target,
                                    Actor actor, String reason) {
        Appointment appointment = appointments.findById(appointmentId)
            .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        if (target == AppointmentStatus.CANCELLED) {
            cancel(appointmentId, actor, reason);
            return appointment;
        }

        AppointmentStatus from = appointment.getStatus();
        validateTransition(from, target);
        appointment.changeStatusForFutureDomainService(target);
        appointmentEvents.save(new AppointmentEvent(appointment,
            from, target, actor, reason, clock.instant()));
        return appointment;
    }

    private CancelAppointmentResult applyTransition(Appointment appointment,
                                                     AppointmentStatus target,
                                                     Actor actor,
                                                     String reason) {
        validateTransition(appointment.getStatus(), target);
        AppointmentStatus from = appointment.getStatus();
        appointment.cancelForFutureDomainService(clock.instant());
        appointmentEvents.save(new AppointmentEvent(appointment, from, target, actor, reason, clock.instant()));
        eventPublisher.publishEvent(new AppointmentCancelledEvent(
            appointment.getId(), appointment.getStartAt(), actor));
        return CancelAppointmentResult.completed();
    }

    private void validateTransition(AppointmentStatus from, AppointmentStatus target) {
        boolean valid = (from == AppointmentStatus.PENDING &&
            (target == AppointmentStatus.CONFIRMED || target == AppointmentStatus.CANCELLED
                || target == AppointmentStatus.ABSENT))
            || (from == AppointmentStatus.CONFIRMED &&
            (target == AppointmentStatus.CANCELLED || target == AppointmentStatus.ABSENT
                || target == AppointmentStatus.ATTENDED));
        if (!valid) throw new IllegalStateException("Transición de estado inválida");
    }

}
