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
import com.turnosodontologia.waiting.WaitingListEntry;
import com.turnosodontologia.waiting.WaitingListStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class DemoDataLoaderIdempotencyTests extends DemoLoaderTestSupport {
    private static final Instant MONDAY = Instant.parse("2026-11-23T12:00:00Z");

    @Test
    void secondRunOnTheSameEffectiveDateReusesTheSameRecords() {
        Dentist dentist = createDentist();
        assertThat(loader().loadOrReuse(dentist)).isEqualTo(DemoDataLoader.Result.CREATED);
        Snapshot first = snapshot(dentist);

        DemoDataLoader.Result second = loader(NOW.plusSeconds(5 * 3600)).loadOrReuse(dentist);

        assertThat(second).isEqualTo(DemoDataLoader.Result.REUSED);
        assertThat(snapshot(dentist)).isEqualTo(first);
        assertThat(patients.count()).isEqualTo(7);
        assertThat(appointments.count()).isEqualTo(4);
        assertThat(blocks.count()).isEqualTo(1);
        assertThat(waitingEntries.count()).isEqualTo(3);
        assertThat(outbox.count()).isEqualTo(3);
    }

    @Test
    void mondayToTuesdayRestartFailsWithoutMovingOrOverwritingDemoData() {
        Dentist dentist = createDentist();
        assertThat(loader(MONDAY).loadOrReuse(dentist)).isEqualTo(DemoDataLoader.Result.CREATED);
        Snapshot monday = snapshot(dentist);

        assertThatThrownBy(() -> loader(MONDAY.plusSeconds(24 * 3600)).loadOrReuse(dentist))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("base local de demo limpia");

        assertThat(snapshot(dentist)).isEqualTo(monday);
        assertThat(patients.count()).isEqualTo(7);
        assertThat(appointments.count()).isEqualTo(4);
        assertThat(blocks.count()).isEqualTo(1);
        assertThat(waitingEntries.count()).isEqualTo(3);
        assertThat(outbox.count()).isEqualTo(3);
    }

    @Test
    void recordsOutsideTheReservedFixturesRemainUntouched() {
        Dentist dentist = createDentist();
        Patient foreignPatient = patients.saveAndFlush(new Patient(
            "98000901", "Ajena", "Ficticia", "ajena@example.invalid"));
        Instant foreignStart = ZonedDateTime.of(2027, 3, 1, 20, 0, 0, 0, ZONE).toInstant();
        Appointment foreignAppointment = appointments.saveAndFlush(Appointment.create(
            dentist, foreignPatient, foreignStart, 30, CreatedBy.PATIENT));
        ScheduleBlock foreignBlock = blocks.saveAndFlush(new ScheduleBlock(
            dentist, foreignStart.plusSeconds(3600), foreignStart.plusSeconds(7200), "Feriado ficticio"));
        WaitingListEntry foreignWaiting = waitingEntries.saveAndFlush(new WaitingListEntry(
            foreignPatient, null, null, WaitingListStatus.WAITING));
        OutboxMessage foreignMessage = outbox.saveAndFlush(new OutboxMessage(
            foreignAppointment, foreignPatient.getEmail(), "Aviso ficticio", "Cuerpo ficticio",
            OutboxStatus.PENDING, null));

        loader().loadOrReuse(dentist);

        assertThat(patients.findById(foreignPatient.getId()).orElseThrow())
            .extracting(Patient::getDni, Patient::getFirstName, Patient::getEmail)
            .containsExactly("98000901", "Ajena", "ajena@example.invalid");
        assertThat(appointments.findById(foreignAppointment.getId()).orElseThrow())
            .extracting(Appointment::getStartAt, Appointment::getStatus, Appointment::getActiveSlot)
            .containsExactly(foreignStart, AppointmentStatus.PENDING, foreignStart);
        assertThat(blocks.findById(foreignBlock.getId()).orElseThrow())
            .extracting(ScheduleBlock::getStartAt, ScheduleBlock::getEndAt, ScheduleBlock::getReason)
            .containsExactly(foreignStart.plusSeconds(3600), foreignStart.plusSeconds(7200), "Feriado ficticio");
        assertThat(waitingEntries.findById(foreignWaiting.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitingListStatus.WAITING);
        assertThat(outbox.findById(foreignMessage.getId()).orElseThrow())
            .extracting(OutboxMessage::getStatus, OutboxMessage::getType)
            .containsExactly(OutboxStatus.PENDING, null);
        assertThat(patients.count()).isEqualTo(8);
        assertThat(appointments.count()).isEqualTo(5);
        assertThat(outbox.count()).isEqualTo(4);
    }

    private Snapshot snapshot(Dentist dentist) {
        return new Snapshot(
            dentist.getId(),
            patients.findAll().stream()
                .sorted(Comparator.comparing(Patient::getDni))
                .map(patient -> patient.getId() + "|" + patient.getDni() + "|" + patient.getEmail())
                .toList(),
            appointments.findAll().stream()
                .sorted(Comparator.comparing(Appointment::getStartAt))
                .map(appointment -> appointment.getId() + "|" + appointment.getStartAt() + "|"
                    + appointment.getStatus() + "|" + appointment.getActiveSlot())
                .toList(),
            blocks.findAll().stream()
                .sorted(Comparator.comparing(ScheduleBlock::getStartAt))
                .map(block -> block.getId() + "|" + block.getStartAt() + "|" + block.getEndAt()
                    + "|" + block.getReason())
                .toList(),
            waitingEntries.findAll().stream()
                .sorted(Comparator.comparing(WaitingListEntry::getId))
                .map(entry -> entry.getId() + "|" + entry.getStatus())
                .toList(),
            outbox.findAll().stream()
                .sorted(Comparator.comparing(OutboxMessage::getId))
                .map(message -> message.getId() + "|" + message.getStatus() + "|" + message.getType())
                .toList()
        );
    }

    private record Snapshot(
        java.util.UUID dentistId,
        List<String> patients,
        List<String> appointments,
        List<String> blocks,
        List<String> waiting,
        List<String> outbox
    ) {}
}
