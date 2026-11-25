package com.turnosodontologia.common.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ErrorTestController.class)
@ExtendWith(OutputCaptureExtension.class)
class FinalErrorHandlerHttpTest {
    @Autowired MockMvc mockMvc;

    @Test
    void unexpectedRuntimeReturnsFixed500BodyAndLogsSafeExceptionAndRootCauseTypes(CapturedOutput output) throws Exception {
        String token = "runtime-token-sentinel";
        String dni = "runtime-dni-sentinel";
        var result = mockMvc.perform(get("/error-tests/runtime").with(user("dentista")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Error interno"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("Exception", "org.springframework", "org.hibernate", "SQL", "at com.", token, dni);
        assertThat(output.getAll())
            .contains("Unhandled runtime exception type=java.lang.IllegalStateException rootCauseType=java.sql.SQLException")
            .doesNotContain("org.hibernate", "SQL-value-sentinel", "at com.fake.Handler", "patient-DNI", token, dni);
    }

    @Test
    void responseStatusExceptionKeepsItsStatus() throws Exception {
        mockMvc.perform(get("/error-tests/status").with(user("dentista")))
            .andExpect(status().isIAmATeapot());
    }

    @Test
    void nonexistentRouteRemains404() throws Exception {
        mockMvc.perform(get("/error-tests/not-found").with(user("dentista")))
            .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethodRemains405() throws Exception {
        mockMvc.perform(get("/error-tests/post-only").with(user("dentista")))
            .andExpect(status().isMethodNotAllowed());
    }

}
