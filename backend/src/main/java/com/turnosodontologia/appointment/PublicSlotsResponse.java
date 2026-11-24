package com.turnosodontologia.appointment;

import com.turnosodontologia.schedule.dto.AvailabilitySlot;
import java.util.List;

public record PublicSlotsResponse(List<AvailabilitySlot> slots) {
}
