package com.turnosodontologia.appointment;

public record CancelAppointmentResult(boolean cancelled, String message) {
    public static CancelAppointmentResult completed() {
        return new CancelAppointmentResult(true, "cancelado");
    }

    public static CancelAppointmentResult alreadyCancelled() {
        return new CancelAppointmentResult(false, "ya cancelado");
    }
}
