package com.turnosodontologia.seed;

public class DemoSeedException extends RuntimeException {
    public DemoSeedException(String message) {
        super(message);
    }

    public DemoSeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
