package com.turnosodontologia.schedule.dto;
import java.time.OffsetDateTime;
import java.util.UUID;
public record ScheduleBlockResponse(UUID id, OffsetDateTime startAt, OffsetDateTime endAt, String reason) {}
