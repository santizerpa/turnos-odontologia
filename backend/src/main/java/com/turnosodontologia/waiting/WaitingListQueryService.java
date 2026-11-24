package com.turnosodontologia.waiting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WaitingListQueryService {

    private final WaitingListEntryRepository waitingEntries;

    public WaitingListQueryService(WaitingListEntryRepository waitingEntries) {
        this.waitingEntries = waitingEntries;
    }

    @Transactional(readOnly = true)
    public List<WaitingListAdminResponse> list() {
        return waitingEntries.findAllByOrderByCreatedAtAscIdAsc().stream()
            .map(WaitingListAdminResponse::from)
            .toList();
    }
}
