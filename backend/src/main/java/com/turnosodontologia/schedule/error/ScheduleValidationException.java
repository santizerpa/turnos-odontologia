package com.turnosodontologia.schedule.error;
import java.util.List;
public class ScheduleValidationException extends RuntimeException {
    private final List<String> details;
    public ScheduleValidationException(String message) { this(message, List.of(message)); }
    public ScheduleValidationException(String message, List<String> details) { super(message); this.details = details; }
    public List<String> getDetails() { return details; }
}
