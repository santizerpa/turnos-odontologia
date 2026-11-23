package com.turnosodontologia.schedule.dto;
import java.util.List;
public record SchedulePatchRequest(String timezone, List<ScheduleDayRequest> days) {}
