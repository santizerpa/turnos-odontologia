package com.turnosodontologia.schedule.api;

import com.turnosodontologia.auth.principal.DentistPrincipal;
import com.turnosodontologia.schedule.AvailabilityService;
import com.turnosodontologia.schedule.PublicBookingWindow;
import com.turnosodontologia.schedule.dto.AvailabilityResponse;
import com.turnosodontologia.schedule.dto.AvailabilitySlot;
import com.turnosodontologia.schedule.dto.AvailabilityWindow;
import com.turnosodontologia.schedule.error.ScheduleBadRequestException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleAvailabilityController {
    private final AvailabilityService availability;
    private final PublicBookingWindow publicWindow;
    private final Clock clock;

    public ScheduleAvailabilityController(AvailabilityService availability, PublicBookingWindow publicWindow,
                                          Clock scheduleClock) {
        this.availability = availability;
        this.publicWindow = publicWindow;
        this.clock = scheduleClock;
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(@AuthenticationPrincipal DentistPrincipal principal,
                                             @RequestParam(required = false) OffsetDateTime from,
                                             @RequestParam(required = false) OffsetDateTime to) {
        if (from == null || to == null) throw new ScheduleBadRequestException("Parámetros from y to obligatorios");
        try {
            publicWindow.validate(from.toInstant(), to.toInstant(), clock);
        } catch (IllegalArgumentException exception) {
            throw new ScheduleBadRequestException(exception.getMessage());
        }
        var slots = availability.freeSlots(principal.id(), from.toInstant(), to.toInstant()).stream()
                .map(slot -> new AvailabilitySlot(slot.start().atOffset(ZoneOffset.ofHours(-3)), slot.end().atOffset(ZoneOffset.ofHours(-3))))
                .toList();
        AvailabilityWindow window = new AvailabilityWindow(from.withOffsetSameInstant(ZoneOffset.ofHours(-3)),
                to.withOffsetSameInstant(ZoneOffset.ofHours(-3)), publicWindow.minNoticeHours(), publicWindow.maxDays(),
                "America/Argentina/Buenos_Aires");
        return new AvailabilityResponse("America/Argentina/Buenos_Aires", 30, window, slots);
    }
}
