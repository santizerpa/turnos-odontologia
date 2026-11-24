package com.turnosodontologia.appointment;

import java.time.Instant;

public record PublicBookingResponse(Instant startAt, int durationMinutes, AppointmentStatus status) {
}
