package com.turnosodontologia.schedule;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.schedule.config.ScheduleClockConfiguration;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import com.turnosodontologia.schedule.domain.ScheduleSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableConfigurationProperties(ScheduleProperties.class)
@Import({ScheduleConfigurationService.class, AvailabilityService.class, ScheduleClockConfiguration.class})
class AvailabilityTests {
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired ScheduleBlockRepository blocks;
    @Autowired AppointmentRepository appointments;
    @Autowired ScheduleConfigurationService configurationService;
    @Autowired AvailabilityService availability;

    Dentist dentist;
    Dentist other;
    Patient patient;

    @BeforeEach
    void setup() {
        dentist = dentists.save(new Dentist("availability.one@odontologia.invalid", "hash", "Ana", "Ficticia"));
        other = dentists.save(new Dentist("availability.two@odontologia.invalid", "hash", "Bruno", "Ficticio"));
        patient = patients.save(new Patient("12345678", "Paciente", "Ficticio", "paciente.availability@odontologia.invalid"));
        configurationService.getOrCreate(dentist);
    }

    private Instant instant(int day, int hour, int minute) {
        return ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0,
                ZoneId.of("America/Argentina/Buenos_Aires")).toInstant();
    }

    @Test
    void freeSlotsHonorsClosedDaysAndInternalRange() {
        List<ScheduleSlot> monday = availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 19, 0));
        assertEquals(18, monday.size());

        List<ScheduleSlot> sunday = availability.freeSlots(dentist.getId(), instant(29, 9, 0), instant(29, 19, 0));
        assertTrue(sunday.isEmpty());

        Instant nearFrom = instant(23, 9, 0);
        Instant nearTo = instant(23, 11, 0);
        assertFalse(availability.freeSlots(dentist.getId(), nearFrom, nearTo).isEmpty(),
                "freeSlots no debe aplicar la ventana pública de 24 horas");
    }

    @Test
    void blocksExcludeHalfOpenAndDeletedAndOtherDentistAreIgnored() {
        ScheduleBlock full = blocks.save(new ScheduleBlock(dentist, instant(23, 0, 0), instant(23, 23, 59), "Día completo"));
        assertTrue(availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 19, 0)).isEmpty());

        full.markDeleted(instant(23, 8, 0));
        blocks.saveAndFlush(full);

        blocks.save(new ScheduleBlock(dentist, instant(23, 9, 0), instant(23, 10, 0), "Alineado"));
        List<ScheduleSlot> slots = availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 11, 0));
        assertFalse(slots.stream().anyMatch(s -> s.start().equals(instant(23, 9, 0))));
        assertTrue(slots.stream().anyMatch(s -> s.start().equals(instant(23, 10, 0))));

        blocks.save(new ScheduleBlock(dentist, instant(23, 10, 10), instant(23, 10, 20), "No alineado"));
        slots = availability.freeSlots(dentist.getId(), instant(23, 9, 30), instant(23, 11, 0));
        assertFalse(slots.stream().anyMatch(s -> s.start().equals(instant(23, 10, 0))));
        assertFalse(slots.stream().anyMatch(s -> s.start().equals(instant(23, 9, 30))));
        assertTrue(slots.stream().anyMatch(s -> s.start().equals(instant(23, 10, 30))));

        blocks.save(new ScheduleBlock(other, instant(23, 0, 0), instant(23, 23, 59), "Otro"));
        assertFalse(availability.freeSlots(dentist.getId(), instant(23, 14, 0), instant(23, 15, 0)).isEmpty());
    }

    @Test
    void appointmentsOccupyOnlyActiveStatusesAndLookbackWorks() {
        Appointment pending = Appointment.create(dentist, patient, instant(23, 9, 0), 30, CreatedBy.DENTIST);
        appointments.save(pending);
        assertFalse(availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 9, 30))
                .stream().anyMatch(s -> s.start().equals(instant(23, 9, 0))));

        Appointment confirmed = Appointment.create(dentist, patient, instant(23, 9, 30), 30, CreatedBy.DENTIST);
        confirmed.confirmForFutureDomainService();
        appointments.save(confirmed);

        Appointment cancelled = Appointment.create(dentist, patient, instant(23, 10, 0), 30, CreatedBy.DENTIST);
        cancelled.cancelForFutureDomainService(instant(23, 8, 0));
        appointments.save(cancelled);

        List<ScheduleSlot> slots = availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 11, 0));
        assertFalse(slots.stream().anyMatch(s -> s.start().equals(instant(23, 9, 0))));
        assertFalse(slots.stream().anyMatch(s -> s.start().equals(instant(23, 9, 30))));
        assertTrue(slots.stream().anyMatch(s -> s.start().equals(instant(23, 10, 0))));

        Appointment misaligned = Appointment.create(dentist, patient, instant(23, 10, 10), 30, CreatedBy.DENTIST);
        appointments.save(misaligned);
        assertFalse(availability.freeSlots(dentist.getId(), instant(23, 10, 0), instant(23, 10, 30))
                .stream().anyMatch(s -> s.start().equals(instant(23, 10, 0))));

        Appointment deleted = Appointment.create(dentist, patient, instant(23, 11, 0), 30, CreatedBy.DENTIST);
        deleted.markDeleted(instant(23, 8, 0));
        appointments.save(deleted);
        assertTrue(availability.isOpenSlot(dentist.getId(), instant(23, 11, 0)));

        Appointment otherAppointment = Appointment.create(other, patient, instant(23, 11, 30), 30, CreatedBy.DENTIST);
        appointments.save(otherAppointment);
        assertTrue(availability.isOpenSlot(dentist.getId(), instant(23, 11, 30)));
    }

    @Test
    void publicWindowIsSeparateFromInternalTwoHourRange() {
        ScheduleProperties properties = new ScheduleProperties();
        properties.setTimezone("America/Argentina/Buenos_Aires");
        properties.setPublicWindow(new ScheduleProperties.PublicWindow(24, 30));
        Clock fixed = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        PublicBookingWindow publicWindow = new PublicBookingWindow(properties, fixed);

        Instant now = fixed.instant();
        Instant twoHoursLater = now.plusSeconds(2 * 3600);
        assertDoesNotThrow(() -> availability.freeSlots(dentist.getId(), now, twoHoursLater));
        assertThrows(IllegalArgumentException.class,
                () -> publicWindow.validate(now, twoHoursLater, fixed));
    }

    @Test
    void isOpenSlotUsesExactGridAndRepeatedReadIsReadOnly() {
        assertTrue(availability.isOpenSlot(dentist.getId(), instant(23, 9, 0)));
        assertFalse(availability.isOpenSlot(dentist.getId(), instant(23, 9, 15)));
        assertFalse(availability.isOpenSlot(dentist.getId(), instant(29, 9, 0)));

        List<ScheduleSlot> first = availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 10, 0));
        List<ScheduleSlot> second = availability.freeSlots(dentist.getId(), instant(23, 9, 0), instant(23, 10, 0));
        assertEquals(first, second);
        assertEquals(0, appointments.count());
        assertEquals(0, blocks.count());
    }
}
