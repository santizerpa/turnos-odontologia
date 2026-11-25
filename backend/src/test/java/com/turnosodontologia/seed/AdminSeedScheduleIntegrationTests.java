package com.turnosodontologia.seed;

import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "APP_ADMIN_SEED=true",
    "APP_ADMIN_EMAIL=schedule.seed@example.invalid",
    "APP_ADMIN_PASSWORD=demo-ficticia-123"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSeedScheduleIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired DentistRepository dentists;
    @Autowired ScheduleConfigurationRepository schedules;
    @Autowired AdminDentistSeeder seeder;

    @Test
    void adminSeedCreatesDefaultScheduleAndPublicSlotsWorkWithoutReadingScheduleFirst() throws Exception {
        var dentist = dentists.findByEmail("schedule.seed@example.invalid").orElseThrow();
        mockMvc.perform(get("/api/public/slots").param("date", allowedWeekdayWithinWindow().toString()))
                .andExpect(status().isOk());

        assertThat(schedules.findByDentistId(dentist.getId())).isPresent();
        assertThat(schedules.count()).isEqualTo(1);

        seeder.run(null);
        assertThat(schedules.findByDentistId(dentist.getId())).isPresent();
        assertThat(schedules.count()).isEqualTo(1);
    }

    private LocalDate allowedWeekdayWithinWindow() {
        LocalDate date = LocalDate.now().plusDays(3);
        while (date.getDayOfWeek().getValue() > 5) date = date.plusDays(1);
        return date;
    }
}
