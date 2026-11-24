package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AppointmentTokenRealHttpTest.TestClock.class)
class AppointmentTokenRealHttpTest {
    private static final Instant NOW = Instant.parse("2026-12-01T12:00:00Z");

    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentTokenRepository tokens;
    @Autowired AppointmentEventRepository appointmentEvents;
    @Autowired AppointmentService appointmentService;
    @Autowired AppointmentTokenService tokenService;

    @Value("${local.server.port}") int port;

    private final List<UUID> createdAppointmentIds = new ArrayList<>();
    private final List<UUID> createdPatientIds = new ArrayList<>();
    private final List<UUID> createdDentistIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (UUID appointmentId : createdAppointmentIds) {
            appointmentEvents.findAll().stream()
                .filter(event -> event.getAppointment().getId().equals(appointmentId))
                .forEach(appointmentEvents::delete);
            tokens.findByAppointmentId(appointmentId).ifPresent(tokens::delete);
            appointments.findById(appointmentId).ifPresent(appointments::delete);
        }
        createdPatientIds.forEach(patientId -> patients.findById(patientId).ifPresent(patients::delete));
        createdDentistIds.forEach(dentistId -> dentists.findById(dentistId).ifPresent(dentists::delete));
        createdAppointmentIds.clear();
        createdPatientIds.clear();
        createdDentistIds.clear();
    }

    @Test
    void confirmThenCancelAndRepeatViaRealHttpWithoutMocksOrTransaction() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Dentist dentist = dentists.save(new Dentist(
            "real-http-" + suffix + "@odontologia.invalid", "hash-ficticio", "Ana", "Real"));
        createdDentistIds.add(dentist.getId());

        String dni = String.valueOf(81000000 + Math.abs(suffix.hashCode()) % 1000000);
        Patient patient = patients.save(new Patient(
            dni, "Paciente", "Real", "paciente.real-http-" + suffix + "@odontologia.invalid"));
        createdPatientIds.add(patient.getId());

        Appointment appointment = appointmentService.create(
            dentist.getId(), patient.getId(), NOW.plusSeconds(72 * 60 * 60), CreatedBy.DENTIST);
        createdAppointmentIds.add(appointment.getId());
        String token = tokenService.issue(appointment);

        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> getConfirm = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl(token) + "/confirm")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(getConfirm.statusCode()).isEqualTo(200);
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus())
            .isEqualTo(AppointmentStatus.PENDING);

        HttpResponse<String> postConfirm = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl(token) + "/confirm")).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(postConfirm.statusCode()).isEqualTo(200);
        assertThat(postConfirm.body()).contains("CONFIRMED");
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus())
            .isEqualTo(AppointmentStatus.CONFIRMED);

        HttpResponse<String> postCancel = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl(token) + "/cancel")).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(postCancel.statusCode()).isEqualTo(200);
        assertThat(postCancel.body()).contains("CANCELLED");
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus())
            .isEqualTo(AppointmentStatus.CANCELLED);

        HttpResponse<String> repeatedCancel = client.send(
            HttpRequest.newBuilder().uri(URI.create(baseUrl(token) + "/cancel")).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(repeatedCancel.statusCode()).isEqualTo(200);
        assertThat(repeatedCancel.body()).contains("ya cancelado");
        assertThat(appointments.findById(appointment.getId()).orElseThrow().getStatus())
            .isEqualTo(AppointmentStatus.CANCELLED);
    }

    private String baseUrl(String token) {
        return "http://localhost:" + port + "/api/public/appointments/" + token;
    }

    @TestConfiguration
    static class TestClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
