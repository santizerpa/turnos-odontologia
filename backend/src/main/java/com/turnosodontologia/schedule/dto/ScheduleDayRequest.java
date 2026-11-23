package com.turnosodontologia.schedule.dto;
import java.util.List;
public record ScheduleDayRequest(String day, List<ScheduleWindowRequest> windows) {}
