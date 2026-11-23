package com.turnosodontologia.appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCancelledEvent(UUID appointmentId, Instant startAt, Actor actor) {}
