package com.turnosodontologia.seed;

import com.turnosodontologia.patient.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "APP_DEMO_SEED=true",
    "APP_ADMIN_SEED=true",
    "APP_ADMIN_EMAIL=demo.perfil.test@example.invalid",
    "APP_ADMIN_PASSWORD=demo-ficticia-123"
})
@ActiveProfiles("test")
class DemoDataSeederProfileGuardTests {

    @Autowired ApplicationContext context;
    @Autowired PatientRepository patients;

    @Test
    void testProfileDoesNotRegisterTheDemoSeeder() {
        assertThat(context.getBeansOfType(DemoDataSeeder.class)).isEmpty();
    }

    @Test
    void testProfileCreatesNoDemoFixturesEvenWhenTheOptInIsActive() {
        assertThat(patients.count()).isZero();
    }
}
