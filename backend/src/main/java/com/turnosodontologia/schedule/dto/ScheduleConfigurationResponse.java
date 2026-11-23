package com.turnosodontologia.schedule.dto;
import java.util.List;
public record ScheduleConfigurationResponse(String timezone, int slotDurationMinutes,
        SchedulePublicWindowResponse publicWindow, List<ScheduleDayResponse> days) {}
