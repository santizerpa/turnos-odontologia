package com.turnosodontologia.appointment;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.Patient;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.schedule.ScheduleBlock;
import com.turnosodontologia.schedule.ScheduleBlockRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationService;
import com.turnosodontologia.schedule.dto.AvailabilitySlot;
import com.turnosodontologia.outbox.OutboxMessage;
import com.turnosodontologia.outbox.OutboxMessageRepository;
import com.turnosodontologia.outbox.OutboxStatus;
import org.junit.jupiter.api.AfterEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PublicBookingApiTests.TestClock.class)
class PublicBookingApiTests {
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant NOW = Instant.parse("2026-11-23T15:00:00Z");
    private static final Instant SLOT_X = Instant.parse("2026-11-24T15:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired OutboxMessageRepository outbox;
    @Autowired AppointmentTokenRepository tokens;
    @Autowired ScheduleBlockRepository blocks;
    @Autowired ScheduleConfigurationRepository configurations;
    @Autowired ScheduleConfigurationService configurationService;

    private Dentist dentist;

    @BeforeEach
    void setup() {
        cleanTables();
        dentist = dentists.save(new Dentist("publicbooking.http@odontologia.invalid", "hash-ficticio", "Demo", "Ficticio"));
        configurationService.getOrCreate(dentist);
    }

    @AfterEach
    void teardown() {
        cleanTables();
    }

    private void cleanTables() {
        tokens.deleteAll();
        outbox.deleteAll();
        appointments.deleteAll();
        blocks.deleteAll();
        patients.deleteAll();
        configurations.deleteAll();
        dentists.deleteAll();
    }

    private ResultActions postBooking(String body) throws Exception {
        return mockMvc.perform(post("/api/public/appointments").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String bookingJson(String dni, String email, String startAt) {
        return "{\"nombre\":\"Paciente\",\"apellido\":\"Ficticio\",\"dni\":\"" + dni
                + "\",\"email\":\"" + email + "\",\"startAt\":\"" + startAt + "\"}";
    }

    // ===== Sección 1 — GET /api/public/slots =====

    @Test
    void slotsOnMañanaReturnsOnlySlotsWithinWindowBorders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/slots").param("date", "2026-11-24"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(slotsOf(result)).isNotEmpty();
        assertThat(slotsOf(result).get(0).start().toInstant()).isEqualTo(Instant.parse("2026-11-24T15:00:00Z"));
        for (AvailabilitySlot slot : slotsOf(result)) {
            assertThat(slot.start().toInstant()).isAfterOrEqualTo(Instant.parse("2026-11-24T15:00:00Z"));
            assertThat(slot.end().toInstant()).isBeforeOrEqualTo(Instant.parse("2026-11-25T03:00:00Z"));
        }
    }

    @Test
    void slotsTodayReturns422() throws Exception {
        mockMvc.perform(get("/api/public/slots").param("date", "2026-11-23"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La fecha solicitada está fuera de la ventana pública"));
    }

    @Test
    void slotsDaysOutsideWindowReturn422() throws Exception {
        for (String date : new String[]{"2026-11-22", "2026-12-24"}) {
            mockMvc.perform(get("/api/public/slots").param("date", date))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("La fecha solicitada está fuera de la ventana pública"));
        }
    }

    @Test
    void slotsOnLastDayIsBoundedBy30DaysLimit() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/slots").param("date", "2026-12-23"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(slotsOf(result)).isNotEmpty();
        for (AvailabilitySlot slot : slotsOf(result)) {
            assertThat(slot.start().toInstant()).isAfterOrEqualTo(Instant.parse("2026-12-23T03:00:00Z"));
            assertThat(slot.end().toInstant()).isBeforeOrEqualTo(Instant.parse("2026-12-23T15:00:00Z"));
        }
        assertThat(slotsOf(result).get(slotsOf(result).size() - 1).end().toInstant())
                .isEqualTo(Instant.parse("2026-12-23T15:00:00Z"));
    }

    @Test
    void slotsOnSundayReturnsEmptyList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/slots").param("date", "2026-11-29"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(slotsOf(result)).isEmpty();
    }

    @Test
    void slotsWithoutDentistRegisteredReturns503() throws Exception {
        cleanTables();
        mockMvc.perform(get("/api/public/slots").param("date", "2026-11-24"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Servicio temporalmente no disponible"));
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Servicio temporalmente no disponible"));
    }

    @Test
    void slotsWithoutScheduleReturnFixedInvalidRequestWithoutInternalMessage() throws Exception {
        configurations.deleteAll();

        MvcResult result = mockMvc.perform(get("/api/public/slots").param("date", "2026-11-24"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Solicitud inválida"))
                .andReturn();

        assertSanitizedErrorBody(result, "Configuración de agenda inexistente");
    }

    @Test
    void publicEndpointsReturn503WithMultipleDentists() throws Exception {
        dentists.save(new Dentist("publicbooking.second@odontologia.invalid", "hash-ficticio", "Ana", "Demo"));
        mockMvc.perform(get("/api/public/slots").param("date", "2026-11-24"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Servicio temporalmente no disponible"));
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Servicio temporalmente no disponible"));
    }

    // ===== Sección 2 — Handlers públicos y validación previa =====

    @Test
    void slotsWithoutDateOrUnparseableDateReturn422() throws Exception {
        mockMvc.perform(get("/api/public/slots"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        mockMvc.perform(get("/api/public/slots").param("date", "notadate"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        mockMvc.perform(get("/api/public/slots").param("date", "2026-11-32"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
    }

    @Test
    void bookValidatesRequestContractWith422() throws Exception {
        postBooking("{\"apellido\":\"Ficticio\",\"dni\":\"12345678\",\"email\":\"publicbooking.paciente@odontologia.invalid\",\"startAt\":\"2026-11-24T15:00:00Z\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        postBooking("{\"nombre\":\"Paciente\",\"dni\":\"12345678\",\"email\":\"publicbooking.paciente@odontologia.invalid\",\"startAt\":\"2026-11-24T15:00:00Z\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        postBooking("{\"nombre\":\"Paciente\",\"apellido\":\"Ficticio\",\"dni\":\"12345678\",\"email\":\"publicbooking.paciente@odontologia.invalid\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        assertThat(appointments.findAll()).isEmpty();
        assertThat(patients.findByDni("12345678")).isEmpty();
    }

    @Test
    void bookRejectsStartAtOutsideWindowWith422() throws Exception {
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", "2026-11-23T20:00:00Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La fecha solicitada está fuera de la ventana pública"));
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", "2026-12-24T00:00:00Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La fecha solicitada está fuera de la ventana pública"));
        assertThat(appointments.findAll()).isEmpty();
        assertThat(patients.findByDni("12345678")).isEmpty();
    }

    @Test
    void bookAcceptsExactWindowBoundariesAndRejectsAdjacentInstants() throws Exception {
        postBooking(bookingJson("1234567", "limite.inferior@odontologia.invalid", "2026-11-24T15:00:00Z"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startAt").value("2026-11-24T15:00:00Z"));

        postBooking(bookingJson("12345678", "antes.limite@odontologia.invalid", "2026-11-24T14:59:59Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La fecha solicitada está fuera de la ventana pública"));

        postBooking(bookingJson("87654321", "limite.superior@odontologia.invalid", "2026-12-23T14:30:00Z"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startAt").value("2026-12-23T14:30:00Z"));

        postBooking(bookingJson("87654320", "despues.limite@odontologia.invalid", "2026-12-23T14:30:01Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La fecha solicitada está fuera de la ventana pública"));

        assertThat(appointments.findAll()).hasSize(2);
        assertThat(patients.findAll()).extracting(Patient::getDni)
                .containsExactlyInAnyOrder("1234567", "87654321");
    }

    @Test
    void bookRejectsDniLengthsOutsideSevenOrEightDigitsWithoutPersistence() throws Exception {
        for (String[] invalidDniAndSlot : new String[][]{
                {"123456", "2026-11-24T15:00:00Z"},
                {"123456789", "2026-11-24T15:30:00Z"}}) {
            postBooking(bookingJson(invalidDniAndSlot[0], "dni.limite@odontologia.invalid", invalidDniAndSlot[1]))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        }

        assertThat(appointments.findAll()).isEmpty();
        assertThat(patients.findAll()).isEmpty();
    }

    @Test
    void bookReturns422WhenStartAtIsOutsideGridOrBlocked() throws Exception {
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", "2026-11-24T15:15:00Z"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La franja solicitada no está disponible"));
        assertThat(appointments.findAll()).isEmpty();
        assertThat(patients.findByDni("12345678")).isEmpty();

        blocks.save(new ScheduleBlock(dentist, SLOT_X, SLOT_X.plusSeconds(30 * 60L), "Ficticio"));
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("La franja solicitada no está disponible"));
        assertThat(appointments.findAll()).isEmpty();
        assertThat(patients.findByDni("12345678")).isEmpty();
    }

    // ===== Sección 3 — Flujo de reserva, token y Outbox =====

    @Test
    void bookCreatesPendingAppointmentAndReturnsMinimalResponse() throws Exception {
        assertThat(firstSlotStart("2026-11-24").toInstant()).isEqualTo(SLOT_X);

        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startAt").value("2026-11-24T15:00:00Z"))
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.dentistId").doesNotExist())
                .andExpect(jsonPath("$.patientId").doesNotExist())
                .andExpect(jsonPath("$.activeSlot").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        assertThat(appointments.findAll()).hasSize(1);
        Appointment appointment = appointments.findAll().get(0);
        assertThat(appointment.getStartAt()).isEqualTo(SLOT_X);
        assertThat(appointment.getDurationMinutes()).isEqualTo(30);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(appointment.getDentist().getId()).isEqualTo(dentist.getId());
        assertThat(patients.findByDni("12345678")).isPresent();

        AppointmentToken appointmentToken = tokens.findByAppointmentId(appointment.getId()).orElseThrow();
        assertThat(appointmentToken.getAppointment().getId()).isEqualTo(appointment.getId());
        assertThat(appointmentToken.getTokenHash()).isNotBlank();

        List<OutboxMessage> outboxMessages = outbox.findAll();
        assertThat(outboxMessages).hasSize(1);
        OutboxMessage outboxMessage = outboxMessages.get(0);
        assertThat(readField(outboxMessage, "status")).isEqualTo(OutboxStatus.PENDING);
        assertThat(readField(outboxMessage, "recipientEmail")).isEqualTo("publicbooking.paciente@odontologia.invalid");
        assertThat(readField(outboxMessage, "subject")).isEqualTo("Confirma tu turno");
        String token = extractToken((String) readField(outboxMessage, "body"));
        assertThat(token).hasSize(43);
        assertThat((String) readField(outboxMessage, "body")).contains("/api/public/appointments/" + token + "/confirm");
        assertThat((String) readField(outboxMessage, "body")).contains("/api/public/appointments/" + token + "/cancel");
        assertThat(outboxMessage.getAppointment().getId()).isEqualTo(appointment.getId());
    }

    private static Object readField(Object target, String name) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void bookReusesPatientByIdnWithoutCreatingNewIdentity() throws Exception {
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isCreated());
        postBooking(bookingJson("12345678", "publicbooking.paciente@odontologia.invalid", "2026-11-24T15:30:00Z"))
                .andExpect(status().isCreated());
        assertThat(patients.findByDni("12345678")).isPresent();
        assertThat(appointments.findAll()).hasSize(2);
    }

    @Test
    void bookRejectsInvalidPatientDataWith422() throws Exception {
        postBooking("{\"nombre\":\"Paciente\",\"apellido\":\"Ficticio\",\"email\":\"publicbooking.paciente@odontologia.invalid\",\"startAt\":\"2026-11-24T15:00:00Z\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        postBooking("{\"nombre\":\"Paciente\",\"apellido\":\"Ficticio\",\"dni\":\"12345678\",\"startAt\":\"2026-11-24T15:00:00Z\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        MvcResult invalidDni = postBooking(bookingJson("dni-sentinel-123", "publicbooking.paciente@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"))
                .andReturn();
        assertSanitizedErrorBody(invalidDni, "dni-sentinel-123");
        postBooking(bookingJson("12345678", "pepe", SLOT_X.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Datos de reserva inválidos"));
        assertThat(appointments.findAll()).isEmpty();
        assertThat(patients.findByDni("12345678")).isEmpty();
    }

    private void assertSanitizedErrorBody(MvcResult result, String... sentinels) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("Exception", "org.springframework", "org.hibernate", "SQL", "at com.");
        assertThat(body).doesNotContain(sentinels);
    }

    @Test
    void bookOverlapReturns409AndRollsBackWithoutNewRecords() throws Exception {
        Patient patient = patients.save(new Patient("12345679", "Ocupante", "Ficticio", "ocupante@odontologia.invalid"));
        Appointment blocker = appointments.save(Appointment.create(dentist, patient, SLOT_X, 30, CreatedBy.DENTIST));
        blocker.changeStatusForFutureDomainService(AppointmentStatus.ABSENT);
        appointments.saveAndFlush(blocker);

        postBooking(bookingJson("98765432", "publicbooking.ocupado@odontologia.invalid", SLOT_X.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("La franja solicitada ya está ocupada"));

        assertThat(patients.findByDni("98765432")).isEmpty();
        assertThat(appointments.findAll()).hasSize(1);
        assertThat(outbox.findAll()).isEmpty();
        assertThat(tokens.findAll()).isEmpty();
    }

    // ===== Sección C-07 — Concurrencia y mapeo de integridad =====

    @Test
    void concurrentReservationsSameSlotOne201One409() throws Exception {
        patients.save(new Patient("12345678", "Paciente", "Ficticio", "publicbooking.paciente@odontologia.invalid"));

        List<Integer> statuses = bookConcurrently(
                new String[][]{{"12345678", "publicbooking.paciente@odontologia.invalid"},
                        {"12345678", "publicbooking.paciente@odontologia.invalid"}});

        assertThat(statuses).hasSize(2);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409 || status == 422).hasSize(1);
        assertThat(statuses).allMatch(status -> status == 201 || status == 409 || status == 422);
        assertThat(appointments.findAll()).hasSize(1);
        assertThat(patients.findByDni("12345678")).isPresent();
        assertThat(outbox.findAll()).hasSize(1);
        assertThat(tokens.findAll()).hasSize(1);
    }

    @Test
    void concurrentReservationsSameSlotDifferentDniOne201One409() throws Exception {
        patients.save(new Patient("12345678", "Paciente", "Ficticio", "publicbooking.paciente@odontologia.invalid"));
        patients.save(new Patient("87654321", "Otra", "Persona", "otra.persona@odontologia.invalid"));

        List<Integer> statuses = bookConcurrently(
                new String[][]{{"12345678", "publicbooking.paciente@odontologia.invalid"},
                        {"87654321", "otra.persona@odontologia.invalid"}});

        assertThat(statuses).hasSize(2);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409 || status == 422).hasSize(1);
        assertThat(statuses).allMatch(status -> status == 201 || status == 409 || status == 422);
        assertThat(appointments.findAll()).hasSize(1);
        assertThat(patients.findAll()).hasSize(2);
        assertThat(outbox.findAll()).hasSize(1);
        assertThat(tokens.findAll()).hasSize(1);
    }

    private List<Integer> bookConcurrently(String[][] requests) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(requests.length);
        ExecutorService pool = Executors.newFixedThreadPool(requests.length);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (String[] request : requests) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    MvcResult result = mockMvc.perform(post("/api/public/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson(request[0], request[1], SLOT_X.toString())))
                            .andReturn();
                    return result.getResponse().getStatus();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void unrelatedDataIntegrityViolationMapsTo500WithFixedMessage() {
        PublicBookingExceptionHandler handler = new PublicBookingExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.genericIntegrity(
                new DataIntegrityViolationException(
                        "Could not execute statement [index appointments_active_slot_key]",
                        new RuntimeException("duplicate key value violates unique constraint \"appointments_active_slot_key\"")));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat((String) response.getBody().get("error")).isEqualTo("No se pudo procesar la reserva");
    }

    @Test
    void unrelatedDataIntegrityViolationDoesNotLeakDriverDetails() {
        PublicBookingExceptionHandler handler = new PublicBookingExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.genericIntegrity(
                new DataIntegrityViolationException("internal driver message A", new RuntimeException("internal driver message B")));
        assertThat((String) response.getBody().get("error")).isEqualTo("No se pudo procesar la reserva");
        assertThat((String) response.getBody().get("error"))
                .doesNotContain("internal driver message A")
                .doesNotContain("internal driver message B");
    }

    @Test
    void appointmentConflictExceptionMapsTo409FixedMessage() {
        PublicBookingExceptionHandler handler = new PublicBookingExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.conflict(new AppointmentConflictException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) response.getBody().get("error")).isEqualTo("La franja solicitada ya está ocupada");
    }

    private OffsetDateTime firstSlotStart(String date) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/slots").param("date", date))
                .andExpect(status().isOk())
                .andReturn();
        List<AvailabilitySlot> slots = slotsOf(result);
        assertThat(slots).isNotEmpty();
        return slots.get(0).start();
    }

    private String extractToken(String body) {
        Matcher matcher = Pattern.compile("/api/public/appointments/([A-Za-z0-9_-]{43})/confirm").matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private List<AvailabilitySlot> slotsOf(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), PublicSlotsResponse.class).slots();
    }

    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @TestConfiguration
    static class TestClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZONE);
        }
    }
}
