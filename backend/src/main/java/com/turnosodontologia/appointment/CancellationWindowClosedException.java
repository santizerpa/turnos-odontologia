package com.turnosodontologia.appointment;

public class CancellationWindowClosedException extends IllegalStateException {
    public CancellationWindowClosedException(String message) {
        super(message);
    }
}
