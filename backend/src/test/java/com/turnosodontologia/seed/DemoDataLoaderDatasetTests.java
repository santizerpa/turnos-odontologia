package com.turnosodontologia.seed;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentStatus;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageType;
import com.turnosodontologia.outbox.OutboxStatus;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.schedule.ScheduleBlock;
import com.turnosodontologia.schedule.ScheduleConfiguration;
import com.turnosodontologia.schedule.ScheduleWindow;
import com.turnosodontologia.waiting.WaitingListEntry;
import com.turnosodontologia.waiting.WaitingListStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class DemoDataLoaderDatasetTests extends DemoLoaderTestSupport {

    @Test
    void loadsRelatedFictitiousDataset() {
        Dentist dentist = createDentist();

        DemoDataLoader.Result result = loader().loadOrReuse(dentist);

        assertThat(result).isEqualTo(DemoDataLoader.Result.CREATED);
        assertThat(patients.count()).isEqualTo(7);
        assertThat(patients.findAll()).extracting(Patient::getDni)
            .containsExactlyInAnyOrderElementsOf(DEMO_DNIS);
        assertThat(patients.findAll()).allSatisfy(patient -> {
            assertThat(patient.getEmail()).endsWith(".invalid");
            assertThat(patient.getFirstName()).isNotBlank();
            assertThat(patient.getLastName()).contains("Fictic");
        });

        List<Appointment> all = appointments.findAll();
        assertThat(all).hasSize(4);
        assertThat(all).filteredOn(a -> a.getStatus() == AppointmentStatus.CONFIRMED).hasSize(2);
        assertThat(all).filteredOn(a -> a.getStatus() == AppointmentStatus.PENDING).hasSize(1);
        assertThat(all).filteredOn(a -> a.getStatus() == AppointmentStatus.CANCELLED).hasSize(1);
        assertThat(all).allSatisfy(appointment -> {
            assertThat(appointment.getDentist().getId()).isEqualTo(dentist.getId());
            assertThat(appointment.getPatient().getDni()).isIn(DEMO_DNIS);
            assertThat(appointment.getDurationMinutes()).isEqualTo(30);
            assertThat(appointment.getCreatedBy()).isEqualTo(CreatedBy.DENTIST);
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                assertThat(appointment.getActiveSlot()).isNull();
                assertThat(appointment.getCancelledAt()).isEqualTo(NOW);
            } else {
                assertThat(appointment.getActiveSlot()).isEqualTo(appointment.getStartAt());
            }
        });

        List<WaitingListEntry> waiting = waitingEntries.findAll();
        assertThat(waiting).hasSize(3);
        assertThat(waiting).allSatisfy(entry -> {
            assertThat(entry.getStatus()).isEqualTo(WaitingListStatus.WAITING);
            assertThat(entry.getPatient().getDni()).isIn(DEMO_DNIS);
        });

        List<ScheduleBlock> demoBlocks = blocks.findAll();
        assertThat(demoBlocks).hasSize(1);
        assertThat(demoBlocks.get(0).getDentist().getId()).isEqualTo(dentist.getId());
        assertThat(demoBlocks.get(0).getReason()).startsWith("Demo seed ").contains("2026-11-24");
        assertThat(demoBlocks.get(0).getEndAt())
            .isEqualTo(demoBlocks.get(0).getStartAt().plusSeconds(30 * 60L));

        List<OutboxMessage> messages = outbox.findAll();
        assertThat(messages).hasSize(3);
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(message.getType()).isEqualTo(OutboxMessageType.BOOKING);
            assertThat(message.getRecipientEmail()).endsWith(".invalid");
            assertThat(message.getAppointment()).isNotNull();
            assertThat(message.getAppointment().getActiveSlot()).isNotNull();
        });

        assertThat(appointmentEvents.count()).isZero();
        // One confirm/cancel token per booking message (the three active appointments), none for the cancelled one.
        assertThat(tokens.count()).isEqualTo(3);
        assertThat(tokens.findAll()).noneSatisfy(token ->
            assertThat(token.getAppointment().getActiveSlot()).isNull());
    }

    @Test
    void activeAppointmentsRespectTheWindowTheAgendaAndTheBlock() {
        Dentist dentist = createDentist();

        loader().loadOrReuse(dentist);

        ScheduleBlock block = blocks.findAll().get(0);
        List<Appointment> active = appointments.findAll().stream()
            .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
            .toList();
        assertThat(active).hasSize(3);
        for (Appointment appointment : active) {
            Instant start = appointment.getStartAt();
            assertThat(start).isAfterOrEqualTo(NOW.plusSeconds(24 * 3600));
            assertThat(start).isBeforeOrEqualTo(NOW.plusSeconds(30 * 86_400L));
            assertThat(start.atZone(ZONE).getDayOfWeek())
                .isIn(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
            Instant end = start.plusSeconds(30 * 60L);
            assertThat(start.isBefore(block.getEndAt()) && block.getStartAt().isBefore(end)).isFalse();
        }
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                Instant firstStart = active.get(i).getStartAt();
                Instant firstEnd = firstStart.plusSeconds(30 * 60L);
                Instant secondStart = active.get(j).getStartAt();
                Instant secondEnd = secondStart.plusSeconds(30 * 60L);
                assertThat(firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd)).isFalse();
            }
        }
    }

    @Test
    void insufficientConfiguredAgendaFailsWithoutPartialDemoData() {
        Dentist dentist = createDentist();
        configurations.saveAndFlush(new ScheduleConfiguration(dentist, List.of(
            new ScheduleWindow(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(9, 30)))));

        assertThatThrownBy(() -> loader().loadOrReuse(dentist))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("slots");

        assertThat(patients.count()).isZero();
        assertThat(appointments.count()).isZero();
        assertThat(blocks.count()).isZero();
        assertThat(waitingEntries.count()).isZero();
        assertThat(outbox.count()).isZero();
    }
}
