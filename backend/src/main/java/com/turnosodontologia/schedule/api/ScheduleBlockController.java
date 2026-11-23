package com.turnosodontologia.schedule.api;

import com.turnosodontologia.auth.principal.DentistPrincipal;
import com.turnosodontologia.schedule.ScheduleBlockService;
import com.turnosodontologia.schedule.dto.ScheduleBlockCreateRequest;
import com.turnosodontologia.schedule.dto.ScheduleBlockResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/schedule/blocks")
public class ScheduleBlockController {
    private final ScheduleBlockService blocks;

    public ScheduleBlockController(ScheduleBlockService blocks) { this.blocks = blocks; }

    @PostMapping
    public ResponseEntity<ScheduleBlockResponse> create(@AuthenticationPrincipal DentistPrincipal principal,
                                                         @RequestBody ScheduleBlockCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blocks.create(principal.id(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal DentistPrincipal principal, @PathVariable UUID id) {
        blocks.delete(principal.id(), id);
    }
}
