package com.turnosodontologia.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Plan determinista de horarios para el conjunto de demo de una fecha efectiva.
 */
public record DemoSchedulePlan(
    LocalDate effectiveDate,
    Instant blockStart,
    Instant blockEnd,
    Instant firstConfirmedStart,
    Instant secondConfirmedStart,
    Instant pendingStart,
    Instant cancelledStart
) {
    public List<Instant> activeStarts() {
        return List.of(firstConfirmedStart, secondConfirmedStart, pendingStart);
    }
}
