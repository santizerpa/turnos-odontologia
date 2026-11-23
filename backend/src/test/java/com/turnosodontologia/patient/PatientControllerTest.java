package com.turnosodontologia.patient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@ActiveProfiles("test")
class PatientControllerTest {

    private static final UUID PACIENTE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ERROR_DETAIL = "Datos de paciente inválidos.";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PatientService patientService;

    @Test
    void listsPatientsWithPagingMetadata() throws Exception {
        when(patientService.list(null, 0, 2)).thenReturn(new PatientPageResponse(
            List.of(
                new PatientResponse(PACIENTE_ID, "27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"),
                new PatientResponse(UUID.fromString("00000000-0000-0000-0000-000000000002"), "27123457",
                    "Segundo", "Ficticio", "segundo.ficticio@example.com")),
            0, 2, 3, 2));

        mockMvc.perform(get("/api/patients")
                .param("page", "0")
                .param("size", "2")
                .with(user("dentista")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].dni").value("27123456"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));

        verify(patientService).list(null, 0, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101"})
    void rejectsSizeOutOfRange(String size) throws Exception {
        mockMvc.perform(get("/api/patients")
                .param("page", "0")
                .param("size", size)
                .with(user("dentista")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value(ERROR_DETAIL));

        verifyNoInteractions(patientService);
    }

    static Stream<Arguments> invalidNames() {
        return Stream.of(
            Arguments.of("nombre", null),
            Arguments.of("nombre", ""),
            Arguments.of("nombre", "   "),
            Arguments.of("nombre", "a".repeat(101)),
            Arguments.of("apellido", null),
            Arguments.of("apellido", ""),
            Arguments.of("apellido", "   "),
            Arguments.of("apellido", "a".repeat(101)));
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsInvalidNameOrLastNameOnCreate(String field, String value) throws Exception {
        String json = field.equals("nombre")
            ? patientJson("27123456", value, "Ficticio", "paciente.ficticio@example.com")
            : patientJson("27123456", "Paciente", value, "paciente.ficticio@example.com");

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value(ERROR_DETAIL))
            .andExpect(content().string(not(containsString("paciente.ficticio@example.com"))));

        verifyNoInteractions(patientService);
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsInvalidNameOrLastNameOnUpsertWithoutCallingService(String field, String value) throws Exception {
        String json = field.equals("nombre")
            ? patientJson("27.123.456", value, "Ficticio", "paciente.ficticio@example.com")
            : patientJson("27.123.456", "Paciente", value, "paciente.ficticio@example.com");

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value(ERROR_DETAIL))
            .andExpect(content().string(not(containsString("paciente.ficticio@example.com"))));

        verifyNoInteractions(patientService);
    }

    @Test
    void createsPatientReturns201WithResponseDto() throws Exception {
        when(patientService.upsert("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"))
            .thenReturn(new PatientUpsertResult(
                patientWithId("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), true));

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27123456","nombre":"Paciente","apellido":"Ficticio","email":"paciente.ficticio@example.com"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(PACIENTE_ID.toString()))
            .andExpect(jsonPath("$.dni").value("27123456"))
            .andExpect(jsonPath("$.nombre").value("Paciente"))
            .andExpect(jsonPath("$.apellido").value("Ficticio"))
            .andExpect(jsonPath("$.email").value("paciente.ficticio@example.com"))
            .andExpect(jsonPath("$.createdAt").doesNotExist())
            .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void reusesPatientReturns200() throws Exception {
        when(patientService.upsert("27.123.456", "Paciente", "Ficticio", "paciente.ficticio@example.com"))
            .thenReturn(new PatientUpsertResult(
                patientWithId("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), false));

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27.123.456","nombre":"Paciente","apellido":"Ficticio","email":"paciente.ficticio@example.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dni").value("27123456"))
            .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void rejectsInvalidDniWithBusinessError() throws Exception {
        when(patientService.upsert("27123A56", "Paciente", "Ficticio", "paciente.ficticio@example.com"))
            .thenThrow(new InvalidPatientDataException());

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27123A56","nombre":"Paciente","apellido":"Ficticio","email":"paciente.ficticio@example.com"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value(ERROR_DETAIL))
            .andExpect(content().string(not(containsString("27123A56"))));
    }

    @Test
    void passesRawEmailAndReturnsDomainNormalizedEmail() throws Exception {
        String rawEmail = "  Paciente.Ficticio@Example.COM  ";
        when(patientService.upsert("27123456", "Paciente", "Ficticio", rawEmail))
            .thenReturn(new PatientUpsertResult(
                patientWithId("27123456", "Paciente", "Ficticio", "paciente.ficticio@example.com"), true));

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientJson("27123456", "Paciente", "Ficticio", rawEmail)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("paciente.ficticio@example.com"));

        verify(patientService).upsert("27123456", "Paciente", "Ficticio", rawEmail);
    }

    @Test
    void acceptsEmailWithMultiLevelDomain() throws Exception {
        String email = "paciente.ficticio@consultorio.example.com";
        when(patientService.upsert("27123456", "Paciente", "Ficticio", email))
            .thenReturn(new PatientUpsertResult(
                patientWithId("27123456", "Paciente", "Ficticio", email), true));

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(patientJson("27123456", "Paciente", "Ficticio", email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email));

        verify(patientService).upsert("27123456", "Paciente", "Ficticio", email);
    }

    @Test
    void rejectsInvalidEmailWithBusinessError() throws Exception {
        when(patientService.upsert("27123456", "Paciente", "Ficticio", "paciente.ficticio@ejemplo"))
            .thenThrow(new InvalidPatientDataException());

        mockMvc.perform(post("/api/patients").with(user("dentista")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dni":"27123456","nombre":"Paciente","apellido":"Ficticio","email":"paciente.ficticio@ejemplo"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail").value(ERROR_DETAIL))
            .andExpect(content().string(not(containsString("paciente.ficticio@ejemplo"))));

        verify(patientService).upsert("27123456", "Paciente", "Ficticio", "paciente.ficticio@ejemplo");
    }

    private static String patientJson(String dni, String nombre, String apellido, String email) {
        StringBuilder json = new StringBuilder("{\"dni\":\"").append(dni).append('"');
        if (nombre != null) {
            json.append(",\"nombre\":\"").append(nombre).append('"');
        }
        if (apellido != null) {
            json.append(",\"apellido\":\"").append(apellido).append('"');
        }
        return json.append(",\"email\":\"").append(email).append("\"}").toString();
    }

    private static Patient patientWithId(String dni, String nombre, String apellido, String email) {
        Patient patient = new Patient(dni, nombre, apellido, email);
        ReflectionTestUtils.setField(patient, "id", PACIENTE_ID);
        return patient;
    }
}
