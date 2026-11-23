package com.turnosodontologia.appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(UUID id, UUID dentistId, UUID patientId, Instant startAt,
                                  int durationMinutes, AppointmentStatus status, Instant activeSlot) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getDentist().getId(),
            appointment.getPatient().getId(), appointment.getStartAt(), appointment.getDurationMinutes(),
            appointment.getStatus(), appointment.getActiveSlot());
    }
}
