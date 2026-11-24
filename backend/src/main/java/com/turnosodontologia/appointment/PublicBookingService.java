package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.outbox.OutboxStatus;
import com.turnosodontologia.patient.PatientService;
import com.turnosodontologia.patient.PatientUpsertResult;
import com.turnosodontologia.schedule.AvailabilityService;
import com.turnosodontologia.schedule.PublicBookingWindow;
import com.turnosodontologia.schedule.dto.AvailabilitySlot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PublicBookingService {
    private static final String SUBJECT = "Confirma tu turno";

    private final DentistRepository dentists;
    private final AvailabilityService availability;
    private final PublicBookingWindow publicWindow;
    private final PatientService patients;
    private final AppointmentService appointments;
    private final AppointmentTokenService tokenService;
    private final OutboxMessageRepository outbox;
    private final Clock clock;
    private final ZoneId zone;

    public PublicBookingService(DentistRepository dentists, AvailabilityService availability,
                                PublicBookingWindow publicWindow, PatientService patients,
                                AppointmentService appointments, AppointmentTokenService tokenService,
                                OutboxMessageRepository outbox, Clock scheduleClock, ZoneId scheduleZoneId) {
        this.dentists = dentists;
        this.availability = availability;
        this.publicWindow = publicWindow;
        this.patients = patients;
        this.appointments = appointments;
        this.tokenService = tokenService;
        this.outbox = outbox;
        this.clock = scheduleClock;
        this.zone = scheduleZoneId;
    }

    public PublicSlotsResponse slots(LocalDate date) {
        if (date == null) throw new PublicBookingWindowException();
        Dentist dentist = publicDentist();
        ZonedDateTime dayStart = date.atStartOfDay(zone);
        Instant from = max(dayStart.toInstant(), publicWindow.earliest(clock));
        Instant to = min(dayStart.plusDays(1).toInstant(), publicWindow.latest(clock));
        if (!from.isBefore(to)) throw new PublicBookingWindowException();
        List<AvailabilitySlot> slots = availability.freeSlots(dentist.getId(), from, to).stream()
                .map(slot -> new AvailabilitySlot(slot.start().atZone(zone).toOffsetDateTime(),
                        slot.end().atZone(zone).toOffsetDateTime()))
                .toList();
        return new PublicSlotsResponse(slots);
    }

    @Transactional
    public PublicBookingResponse book(PublicBookingRequest request) {
        Dentist dentist = publicDentist();
        validateWindow(request.startAt());
        if (!availability.isOpenSlot(dentist.getId(), request.startAt())) {
            throw new PublicBookingSlotUnavailableException();
        }
        PatientUpsertResult upsert = patients.upsert(request.dni(), request.nombre(), request.apellido(), request.email());
        Appointment appointment = appointments.create(dentist.getId(), upsert.patient().getId(),
                request.startAt(), CreatedBy.PATIENT);
        String token = tokenService.issue(appointment);
        outbox.save(new OutboxMessage(appointment, appointment.getPatient().getEmail(),
                SUBJECT, confirmAndCancelLinks(token), OutboxStatus.PENDING));
        return minimalResponse(appointment);
    }

    private PublicBookingResponse minimalResponse(Appointment appointment) {
        return new PublicBookingResponse(appointment.getStartAt(), appointment.getDurationMinutes(), appointment.getStatus());
    }

    private String confirmAndCancelLinks(String token) {
        return "Para confirmar tu turno, visita /api/public/appointments/" + token + "/confirm. "
                + "Para cancelarlo, visita /api/public/appointments/" + token + "/cancel.";
    }

    public Dentist publicDentist() {
        List<Dentist> found = dentists.findAll();
        if (found.size() != 1) throw new PublicBookingDentistUnavailableException();
        return found.get(0);
    }

    private void validateWindow(Instant startAt) {
        if (startAt == null) throw new PublicBookingWindowException();
        try {
            publicWindow.validate(startAt, startAt.plusSeconds(30 * 60L), clock);
        } catch (IllegalArgumentException exception) {
            throw new PublicBookingWindowException();
        }
    }

    private static Instant max(Instant first, Instant second) {
        return first.isAfter(second) ? first : second;
    }

    private static Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }
}
