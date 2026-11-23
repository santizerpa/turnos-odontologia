package com.turnosodontologia.schedule.api;

import com.turnosodontologia.auth.principal.DentistPrincipal;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationService;
import com.turnosodontologia.schedule.dto.ScheduleConfigurationResponse;
import com.turnosodontologia.schedule.dto.SchedulePatchRequest;
import com.turnosodontologia.schedule.error.ScheduleNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final ScheduleConfigurationService configurations;
    private final DentistRepository dentists;

    public ScheduleController(ScheduleConfigurationService configurations, DentistRepository dentists) {
        this.configurations = configurations;
        this.dentists = dentists;
    }

    @GetMapping
    public ScheduleConfigurationResponse get(@AuthenticationPrincipal DentistPrincipal principal) {
        return configurations.get(dentists.findById(principal.id()).orElseThrow(ScheduleNotFoundException::new));
    }

    @PatchMapping
    public ScheduleConfigurationResponse patch(@AuthenticationPrincipal DentistPrincipal principal,
                                               @RequestBody SchedulePatchRequest request) {
        return configurations.patch(dentists.findById(principal.id()).orElseThrow(ScheduleNotFoundException::new), request);
    }
}
