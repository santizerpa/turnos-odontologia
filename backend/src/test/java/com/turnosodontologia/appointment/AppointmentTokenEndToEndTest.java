package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AppointmentTokenEndToEndTest.TestClock.class)
@Transactional
class AppointmentTokenEndToEndTest {
    private static final Instant NOW = Instant.now().plusSeconds(12 * 60 * 60);

    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentEventRepository appointmentEvents;
    @Autowired AppointmentTokenService tokenService;
    @Autowired AppointmentService appointmentService;
    @Autowired CancelledEventCounter cancelledEvents;

    @Test
    void realEndpointCancelOnceThenIdempotentAndConfirmTracksOnePatientEvent() throws Exception {
        Dentist dentist = dentists.save(new Dentist("endtoend@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("77654321", "Paciente", "Ficticio", "paciente.endtoend@odontologia.invalid"));

        Appointment cancellable = appointmentService.create(dentist.getId(), patient.getId(), NOW.plusSeconds(48 * 60 * 60), CreatedBy.DENTIST);
        String cancelToken = tokenService.issue(cancellable);

        mockMvc.perform(post("/api/public/appointments/{token}/cancel", cancelToken).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("cancelación exitosa"));

        Appointment cancelled = appointments.findById(cancellable.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(cancelled.getActiveSlot()).isNull();
        assertThat(cancelledEvents.countFor(cancelled.getId())).isEqualTo(1);

        mockMvc.perform(post("/api/public/appointments/{token}/cancel", cancelToken).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("ya cancelado"));
        assertThat(cancelledEvents.countFor(cancelled.getId())).isEqualTo(1);

        Appointment confirmable = appointmentService.create(dentist.getId(), patient.getId(), NOW.plusSeconds(72 * 60 * 60), CreatedBy.DENTIST);
        String confirmToken = tokenService.issue(confirmable);

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", confirmToken).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("confirmación exitosa"));

        Appointment confirmed = appointments.findById(confirmable.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        var confirmEvents = appointmentEvents.findAll().stream()
            .filter(event -> event.getAppointment().getId().equals(confirmable.getId()))
            .toList();
        assertThat(confirmEvents).singleElement().satisfies(event -> {
            assertThat(event.getActor()).isEqualTo(Actor.PATIENT);
            assertThat(event.getFromStatus()).isEqualTo(AppointmentStatus.PENDING);
            assertThat(event.getToStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        });
    }

    @Test
    void getConfirmAndCancelAreInformativeAndDoNotMutateStatusEventsOrServices() throws Exception {
        Dentist dentist = dentists.save(new Dentist("e2e.get@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        Patient patient = patients.save(new Patient("77654322", "Paciente", "Informativo", "get.endtoend@odontologia.invalid"));
        Appointment appointment = appointmentService.create(dentist.getId(), patient.getId(), NOW.plusSeconds(96 * 60 * 60), CreatedBy.DENTIST);
        String token = tokenService.issue(appointment);

        AppointmentStatus beforeStatus = appointment.getStatus();
        int beforeEvents = appointmentEvents.findAll().size();

        mockMvc.perform(get("/api/public/appointments/{token}/confirm", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startAt").value(NOW.plusSeconds(96 * 60 * 60).toString()))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.patient").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist());
        mockMvc.perform(get("/api/public/appointments/{token}/cancel", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startAt").value(NOW.plusSeconds(96 * 60 * 60).toString()))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.patient").doesNotExist())
            .andExpect(jsonPath("$.token").doesNotExist());

        Appointment after = appointments.findById(appointment.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(beforeStatus);
        assertThat(after.getActiveSlot()).isEqualTo(appointment.getStartAt());
        assertThat(appointmentEvents.findAll()).hasSize(beforeEvents);
    }

    @TestConfiguration
    static class TestClock {
        @Bean
        CancelledEventCounter cancelledEventCounter() {
            return new CancelledEventCounter();
        }
    }

    static class CancelledEventCounter {
        private final ConcurrentHashMap<UUID, AtomicInteger> counts = new ConcurrentHashMap<>();

        @EventListener
        void onCancelled(AppointmentCancelledEvent event) {
            counts.computeIfAbsent(event.appointmentId(), ignored -> new AtomicInteger()).incrementAndGet();
        }

        int countFor(UUID appointmentId) {
            AtomicInteger count = counts.get(appointmentId);
            return count == null ? 0 : count.get();
        }
    }
}
