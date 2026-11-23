package com.turnosodontologia.appointment;

public class PublicAppointmentCancelIllegalStateException extends IllegalStateException {
    public PublicAppointmentCancelIllegalStateException(IllegalStateException cause) {
        super(cause.getMessage(), cause);
    }
}
