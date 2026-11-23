package com.turnosodontologia.schedule.dto;
import java.time.OffsetDateTime;
public record AvailabilitySlot(OffsetDateTime start, OffsetDateTime end) {}
