package com.turnosodontologia.schedule.dto;
import java.time.OffsetDateTime;
public record ScheduleBlockCreateRequest(OffsetDateTime startAt, OffsetDateTime endAt, String reason) {}
