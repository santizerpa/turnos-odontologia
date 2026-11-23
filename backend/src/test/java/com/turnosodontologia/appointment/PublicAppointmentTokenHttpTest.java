package com.turnosodontologia.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicAppointmentTokenHttpTest {
    @Autowired MockMvc mockMvc;
    @MockBean AppointmentTokenService tokenService;

    @Test
    void missingTokenReturns404WithoutAppointmentInteraction() throws Exception {
        String plain = "A".repeat(43);
        when(tokenService.resolve(plain)).thenThrow(new PublicTokenInvalidException());

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isNotFound());
        verify(tokenService).resolve(plain);
    }

    @Test
    void expiredConfirmedTokenReturns410BeforeStatusChange() throws Exception {
        String plain = "B".repeat(43);
        when(tokenService.resolve(plain)).thenThrow(new PublicTokenExpiredException());

        mockMvc.perform(post("/api/public/appointments/{token}/confirm", plain).with(csrf()))
            .andExpect(status().isGone());
    }
}
