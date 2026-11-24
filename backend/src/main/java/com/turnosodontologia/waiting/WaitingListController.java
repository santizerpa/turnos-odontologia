package com.turnosodontologia.waiting;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WaitingListController {

    private final WaitingListRegistrationService registrationService;
    private final WaitingListQueryService queryService;

    public WaitingListController(WaitingListRegistrationService registrationService,
                                 WaitingListQueryService queryService) {
        this.registrationService = registrationService;
        this.queryService = queryService;
    }

    @GetMapping("/waiting-list")
    public java.util.List<WaitingListAdminResponse> list() {
        return queryService.list();
    }

    @PostMapping("/public/waiting-list")
    public ResponseEntity<WaitingListEntryResponse> register(
        @Valid @RequestBody WaitingListRegistrationRequest request) {
        WaitingListEntry entry = registrationService.register(request.dni(), request.nombre(),
            request.apellido(), request.email(), request.requestedDateFrom(), request.requestedDateTo());
        return ResponseEntity.status(HttpStatus.CREATED).body(WaitingListEntryResponse.from(entry));
    }
}
