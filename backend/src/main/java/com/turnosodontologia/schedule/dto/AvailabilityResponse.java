package com.turnosodontologia.schedule.dto;
import java.util.List;
public record AvailabilityResponse(String timezone, int durationMinutes, AvailabilityWindow window, List<AvailabilitySlot> slots) {}
