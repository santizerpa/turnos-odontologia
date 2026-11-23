package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({AppointmentPersistenceTest.TestClock.class, AppointmentPersistenceTest.CancelledEventListenerConfig.class})
@Transactional
class AppointmentPersistenceTest {
    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");

    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentEventRepository appointmentEvents;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired Clock scheduleClock;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CancelledEventListenerConfig.CancelledEventCounter cancelledEventCounter;

    private final List<UUID> createdAppointmentIds = Collections.synchronizedList(new java.util.ArrayList<>());
    private final List<UUID> createdPatientIds = Collections.synchronizedList(new java.util.ArrayList<>());
    private final List<UUID> createdDentistIds = Collections.synchronizedList(new java.util.ArrayList<>());

    @Test
    void persistsThirtyMinutePendingAppointmentWithActiveSlot() {
        Dentist dentist = dentists.save(new Dentist("persist.pendiente@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("12345671", "Paciente", "Ficticio", "persist.paciente@odontologia.invalid"));

        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Appointment created = service.create(dentist.getId(), patient.getId(), NOW.plusSeconds(3600), CreatedBy.DENTIST);

        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(created.getDurationMinutes()).isEqualTo(30);
        assertThat(created.getActiveSlot()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(appointments.findById(created.getId())).isPresent();
    }

    @Test
    void rejectsSecondActiveAppointmentInSameSlot() {
        Dentist dentist = dentists.save(new Dentist("persist.conflicto@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient one = patients.save(new Patient("87654321", "Uno", "Ficticio", "persist.uno@odontologia.invalid"));
        Patient two = patients.save(new Patient("87654322", "Dos", "Ficticio", "persist.dos@odontologia.invalid"));
        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Instant slot = NOW.plusSeconds(7200);

        service.create(dentist.getId(), one.getId(), slot, CreatedBy.DENTIST);

        assertThatThrownBy(() -> service.create(dentist.getId(), two.getId(), slot, CreatedBy.DENTIST))
            .isInstanceOf(AppointmentConflictException.class);
        assertThat(appointments.count()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPersistencesCreateOnlyOneActiveAppointmentInSameSlot() throws Exception {
        Dentist dentist = dentists.saveAndFlush(new Dentist("persist.concurrent@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient one = patients.saveAndFlush(new Patient("67654321", "Uno", "Concurrente", "persist.concurrent1@odontologia.invalid"));
        Patient two = patients.saveAndFlush(new Patient("67654322", "Dos", "Concurrente", "persist.concurrent2@odontologia.invalid"));
        createdDentistIds.add(dentist.getId());
        createdPatientIds.add(one.getId());
        createdPatientIds.add(two.getId());

        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Instant slot = NOW.plusSeconds(14400);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        try {
            Future<?> first = executor.submit(() -> persistConcurrently(service, dentist, one, slot, ready, start, successes, conflicts));
            Future<?> second = executor.submit(() -> persistConcurrently(service, dentist, two, slot, ready, start, successes, conflicts));
            ready.await();
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(createdAppointmentIds).hasSize(1);
    }

    private void persistConcurrently(AppointmentService service, Dentist dentist, Patient patient, Instant slot,
                                     CountDownLatch ready, CountDownLatch start,
                                     AtomicInteger successes, AtomicInteger conflicts) {
        ready.countDown();
        try {
            start.await();
            Appointment created = service.create(dentist.getId(), patient.getId(), slot, CreatedBy.DENTIST);
            if (!createdAppointmentIds.contains(created.getId())) {
                createdAppointmentIds.add(created.getId());
            }
            successes.incrementAndGet();
        } catch (AppointmentConflictException | org.springframework.dao.DataIntegrityViolationException exception) {
            conflicts.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hilo concurrente interrumpido", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Fallo inesperado en persistencia concurrente", exception);
        }
    }

    @AfterEach
    void cleanupTrackedConcurrencyEntities() {
        for (UUID appointmentId : createdAppointmentIds) {
            jdbcTemplate.update("DELETE FROM appointment_events WHERE appointment_id = ?", appointmentId);
            jdbcTemplate.update("DELETE FROM appointments WHERE id = ?", appointmentId);
        }
        for (UUID patientId : createdPatientIds) {
            jdbcTemplate.update("DELETE FROM patients WHERE id = ?", patientId);
        }
        for (UUID dentistId : createdDentistIds) {
            jdbcTemplate.update("DELETE FROM dentists WHERE id = ?", dentistId);
        }
        createdAppointmentIds.clear();
        createdPatientIds.clear();
        createdDentistIds.clear();
    }

    @Test
    void confirmedAppointmentCancelPublishesOneEventAndClearsActiveSlotWithRealService() {
        Dentist dentist = dentists.save(new Dentist("persist.cancel.event@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("77654323", "Paciente", "Ficticio", "persist.cancel.event@odontologia.invalid"));
        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Appointment appointment = service.create(dentist.getId(), patient.getId(), NOW.plusSeconds(48 * 60 * 60), CreatedBy.DENTIST);
        service.changeStatus(appointment.getId(), AppointmentStatus.CONFIRMED, Actor.DENTIST, "confirmación ficticia");

        CancelAppointmentResult first = service.cancel(appointment.getId(), Actor.PATIENT, "cancelación ficticia");
        CancelAppointmentResult second = service.cancel(appointment.getId(), Actor.PATIENT, "cancelación repetida ficticia");

        Appointment persisted = appointments.findById(appointment.getId()).orElseThrow();
        assertThat(first.cancelled()).isTrue();
        assertThat(second.cancelled()).isFalse();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(persisted.getActiveSlot()).isNull();
        assertThat(cancelledEventCounter.countFor(appointment.getId())).isEqualTo(1);
    }

    @Test
    void cancelledSlotCanBeReusedAndNullSlotsAreUniqueNullable() {
        Dentist dentist = dentists.save(new Dentist("persist.reuse@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient one = patients.save(new Patient("77654321", "Uno", "Ficticio", "persist.reuse1@odontologia.invalid"));
        Patient two = patients.save(new Patient("77654322", "Dos", "Ficticio", "persist.reuse2@odontologia.invalid"));
        AppointmentService service = new AppointmentService(appointments, appointmentEvents, dentists, patients, eventPublisher, scheduleClock);
        Instant slot = NOW.plusSeconds(10800);

        Appointment first = service.create(dentist.getId(), one.getId(), slot, CreatedBy.DENTIST);
        service.cancel(first.getId(), Actor.DENTIST, "franja ficticia");
        Appointment replacement = service.create(dentist.getId(), two.getId(), slot, CreatedBy.DENTIST);

        assertThat(first.getActiveSlot()).isNull();
        assertThat(replacement.getActiveSlot()).isEqualTo(slot);
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class CancelledEventListenerConfig {
        @Bean
        CancelledEventCounter cancelledEventCounter() {
            return new CancelledEventCounter();
        }

        static class CancelledEventCounter {
            private final java.util.Map<java.util.UUID, java.util.concurrent.atomic.AtomicInteger> counts =
                new java.util.concurrent.ConcurrentHashMap<>();
            @org.springframework.context.event.EventListener
            void onCancelled(AppointmentCancelledEvent event) {
                counts.computeIfAbsent(event.appointmentId(), ignored -> new java.util.concurrent.atomic.AtomicInteger())
                    .incrementAndGet();
            }
            int countFor(java.util.UUID appointmentId) {
                java.util.concurrent.atomic.AtomicInteger count = counts.get(appointmentId);
                return count == null ? 0 : count.get();
            }
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestClock {
        @Bean
        Clock scheduleClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
