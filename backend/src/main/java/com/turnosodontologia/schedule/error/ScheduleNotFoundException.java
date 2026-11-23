package com.turnosodontologia.schedule.error;
public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException() { super("Bloqueo inexistente o ajeno"); }
}
