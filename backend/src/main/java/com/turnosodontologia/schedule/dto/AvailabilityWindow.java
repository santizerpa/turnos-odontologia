package com.turnosodontologia.schedule.dto;
import java.time.OffsetDateTime;
public record AvailabilityWindow(OffsetDateTime from, OffsetDateTime to, int minNoticeHours, int maxDays, String timezone) {}
