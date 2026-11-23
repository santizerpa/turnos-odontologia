package com.turnosodontologia.schedule.dto;
import java.time.DayOfWeek;
import java.util.List;
public record ScheduleDayResponse(DayOfWeek day, List<ScheduleWindowResponse> windows) {}
