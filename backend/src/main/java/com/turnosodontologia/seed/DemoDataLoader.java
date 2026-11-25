package com.turnosodontologia.seed;

import com.turnosodontologia.appointment.Appointment;
import com.turnosodontologia.appointment.AppointmentRepository;
import com.turnosodontologia.appointment.AppointmentTokenService;
import com.turnosodontologia.appointment.BookingMessage;
import com.turnosodontologia.appointment.CreatedBy;
import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.outbox.OutboxMessageType;
import com.turnosodontologia.outbox.OutboxStatus;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.schedule.ScheduleBlock;
import com.turnosodontologia.schedule.ScheduleBlockRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationRepository;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import com.turnosodontologia.waiting.WaitingListEntry;
import com.turnosodontologia.waiting.WaitingListEntryRepository;
import com.turnosodontologia.waiting.WaitingListStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carga el conjunto ficticio de demo dentro de una única transacción.
 *
 * <p>Nunca escribe {@code active_slot} directamente: usa las fábricas y métodos
 * de dominio de {@link Appointment}. El turno cancelado se construye como estado
 * inicial, sin publicar eventos ni ejecutar la cancelación operativa.</p>
 */
@Service
public class DemoDataLoader {
    public enum Result { CREATED, REUSED }

    private final DentistRepository dentists;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final ScheduleBlockRepository blocks;
    private final ScheduleConfigurationRepository configurations;
    private final WaitingListEntryRepository waitingEntries;
    private final OutboxMessageRepository outbox;
    private final AppointmentTokenService tokens;
    private final ScheduleProperties properties;
    private final Clock clock;
    private final ZoneId zone;
    private final DemoSchedulePlanner planner = new DemoSchedulePlanner();

    public DemoDataLoader(DentistRepository dentists,
                          PatientRepository patients,
                          AppointmentRepository appointments,
                          ScheduleBlockRepository blocks,
                          ScheduleConfigurationRepository configurations,
                          WaitingListEntryRepository waitingEntries,
                          OutboxMessageRepository outbox,
                          AppointmentTokenService tokens,
                          ScheduleProperties properties,
                          Clock scheduleClock,
                          ZoneId scheduleZoneId) {
        this.dentists = dentists;
        this.patients = patients;
        this.appointments = appointments;
        this.blocks = blocks;
        this.configurations = configurations;
        this.waitingEntries = waitingEntries;
        this.outbox = outbox;
        this.tokens = tokens;
        this.properties = properties;
        this.clock = scheduleClock;
        this.zone = scheduleZoneId;
    }

    @Transactional
    public Result loadOrReuse(Dentist dentist) {
        if (dentist == null || dentist.getId() == null) {
            throw new DemoSeedException("El seed de demo requiere un odontólogo configurado.");
        }
        Dentist managed = dentists.findById(dentist.getId()).orElseThrow(() -> new DemoSeedException(
            "El seed de demo no encontró el odontólogo configurado. Verificá APP_ADMIN_EMAIL y APP_ADMIN_SEED."));

        LocalDate effectiveDate = clock.instant().atZone(zone).toLocalDate();
        var demoBlock = findDemoBlock();
        if (demoBlock.isPresent()) {
            LocalDate existingDate = DemoFixtures.parseBlockDate(demoBlock.get().getReason());
            if (!existingDate.equals(effectiveDate)) {
                throw new DemoSeedException(
                    "Ya existen datos de demo para la fecha efectiva " + existingDate
                        + " y la fecha actual es " + effectiveDate
                        + ". El seed no mueve, sobrescribe ni regenera fixtures: "
                        + "prepará una base local de demo limpia para cargar la nueva fecha efectiva.");
            }
            return Result.REUSED;
        }
        if (hasAnyDemoPatient()) {
            throw new DemoSeedException(
                "Existen datos de demo parciales sin bloqueo de referencia. "
                    + "Prepará una base local de demo limpia; no se modificó ningún registro.");
        }

        DemoSchedulePlan plan = planner.plan(clock.instant(), zone, windowsFor(managed));
        persist(managed, plan);
        return Result.CREATED;
    }

    private Optional<ScheduleBlock> findDemoBlock() {
        return blocks.findAll().stream()
            .filter(block -> !block.isDeleted())
            .filter(block -> DemoFixtures.isDemoReason(block.getReason()))
            .findFirst();
    }

    private boolean hasAnyDemoPatient() {
        for (DemoFixtures.DemoPatient fixture : DemoFixtures.PATIENTS) {
            if (patients.findByDni(fixture.dni()).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private List<DemoWindow> windowsFor(Dentist dentist) {
        return configurations.findByDentistId(dentist.getId())
            .map(configuration -> configuration.getWindows().stream()
                .map(window -> new DemoWindow(window.getDayOfWeek(), window.getStartTime(), window.getEndTime()))
                .toList())
            .orElseGet(() -> properties.getDefaultWindows().stream()
                .map(window -> new DemoWindow(DayOfWeek.valueOf(window.day()),
                    LocalTime.parse(window.start()), LocalTime.parse(window.end())))
                .toList());
    }

    private void persist(Dentist dentist, DemoSchedulePlan plan) {
        List<Patient> demoPatients = new ArrayList<>();
        for (DemoFixtures.DemoPatient fixture : DemoFixtures.PATIENTS) {
            demoPatients.add(patients.save(new Patient(
                fixture.dni(), fixture.firstName(), fixture.lastName(), fixture.email())));
        }

        Appointment firstConfirmed = Appointment.create(
            dentist, demoPatients.get(0), plan.firstConfirmedStart(), 30, CreatedBy.DENTIST);
        firstConfirmed.confirmForFutureDomainService();
        Appointment secondConfirmed = Appointment.create(
            dentist, demoPatients.get(1), plan.secondConfirmedStart(), 30, CreatedBy.DENTIST);
        secondConfirmed.confirmForFutureDomainService();
        Appointment pending = Appointment.create(
            dentist, demoPatients.get(2), plan.pendingStart(), 30, CreatedBy.DENTIST);
        Appointment cancelled = Appointment.create(
            dentist, demoPatients.get(3), plan.cancelledStart(), 30, CreatedBy.DENTIST);
        cancelled.cancelForFutureDomainService(clock.instant());

        try {
            appointments.saveAllAndFlush(List.of(firstConfirmed, secondConfirmed, pending, cancelled));
        } catch (DataIntegrityViolationException exception) {
            throw new DemoSeedException(
                "No se pudo cargar el conjunto de demo: uno de los horarios reservados ya está ocupado. "
                    + "Prepará una base local de demo limpia.", exception);
        }

        blocks.save(new ScheduleBlock(dentist, plan.blockStart(), plan.blockEnd(),
            DemoFixtures.blockReason(plan.effectiveDate())));

        waitingEntries.saveAll(List.of(
            new WaitingListEntry(demoPatients.get(4), null, null, WaitingListStatus.WAITING),
            new WaitingListEntry(demoPatients.get(5), null, null, WaitingListStatus.WAITING),
            new WaitingListEntry(demoPatients.get(6), null, null, WaitingListStatus.WAITING)));

        outbox.saveAll(List.of(
            booking(firstConfirmed, demoPatients.get(0)),
            booking(secondConfirmed, demoPatients.get(1)),
            booking(pending, demoPatients.get(2))));
    }

    // Same subject and links as a real public booking, so the demo inbox can view, confirm or cancel it.
    private OutboxMessage booking(Appointment appointment, Patient patient) {
        return new OutboxMessage(
            appointment,
            patient.getEmail(),
            BookingMessage.SUBJECT,
            BookingMessage.body(tokens.issue(appointment)),
            OutboxStatus.PENDING,
            OutboxMessageType.BOOKING);
    }
}
