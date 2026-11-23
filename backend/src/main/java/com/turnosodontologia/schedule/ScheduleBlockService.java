package com.turnosodontologia.schedule;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.schedule.dto.ScheduleBlockCreateRequest;
import com.turnosodontologia.schedule.dto.ScheduleBlockResponse;
import com.turnosodontologia.schedule.error.ScheduleNotFoundException;
import com.turnosodontologia.schedule.error.ScheduleValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ScheduleBlockService {
    private final ScheduleBlockRepository blocks;
    private final DentistRepository dentists;
    private final Clock clock;

    public ScheduleBlockService(ScheduleBlockRepository blocks, DentistRepository dentists, Clock scheduleClock) {
        this.blocks = blocks;
        this.dentists = dentists;
        this.clock = scheduleClock;
    }

    @Transactional
    public ScheduleBlockResponse create(UUID dentistId, ScheduleBlockCreateRequest request) {
        if (request == null || request.startAt() == null || request.endAt() == null || !request.endAt().toInstant().isAfter(request.startAt().toInstant())) {
            throw new ScheduleValidationException("El bloqueo debe tener fin posterior al inicio");
        }
        if (request.reason() != null && request.reason().length() > 255) {
            throw new ScheduleValidationException("El motivo no puede superar 255 caracteres");
        }
        Dentist dentist = dentists.findById(dentistId).orElseThrow(ScheduleNotFoundException::new);
        ScheduleBlock block = blocks.save(new ScheduleBlock(dentist, request.startAt().toInstant(), request.endAt().toInstant(), request.reason()));
        return response(block);
    }

    @Transactional
    public void delete(UUID dentistId, UUID blockId) {
        ScheduleBlock block = blocks.findById(blockId).orElseThrow(ScheduleNotFoundException::new);
        if (block.getDentist().getId() == null || !block.getDentist().getId().equals(dentistId) || block.getDeletedAt() != null) {
            if (block.getDentist().getId().equals(dentistId) && block.getDeletedAt() != null) return;
            throw new ScheduleNotFoundException();
        }
        block.markDeleted(clock.instant());
    }

    @Transactional(readOnly = true)
    public ScheduleBlock find(UUID id) {
        return blocks.findById(id).orElseThrow(ScheduleNotFoundException::new);
    }

    private ScheduleBlockResponse response(ScheduleBlock block) {
        return new ScheduleBlockResponse(block.getId(), block.getStartAt().atOffset(ZoneOffset.ofHours(-3)), block.getEndAt().atOffset(ZoneOffset.ofHours(-3)), block.getReason());
    }
}
