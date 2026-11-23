package com.turnosodontologia.schedule.domain;

import java.time.Instant;

public record ScheduleSlot(Instant start, Instant end) {}
