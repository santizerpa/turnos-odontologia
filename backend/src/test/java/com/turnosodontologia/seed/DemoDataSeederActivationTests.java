package com.turnosodontologia.seed;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.patient.PatientRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationRepository;
import com.turnosodontologia.schedule.ScheduleConfigurationService;
import com.turnosodontologia.schedule.config.ScheduleProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DataJpaTest
@ActiveProfiles("test")
@Import(ScheduleConfigurationService.class)
@EnableConfigurationProperties(ScheduleProperties.class)
class DemoDataSeederActivationTests {

    @Autowired DentistRepository dentists;
    @Autowired PatientRepository patients;
    @Autowired ScheduleConfigurationRepository schedules;
    @Autowired ScheduleConfigurationService scheduleConfigurations;

    @Test
    void disabledDemoKeepsAdminSeedBehaviorAndDoesNotLoadDemoData() {
        MockEnvironment environment = adminEnvironment();
        new AdminDentistSeeder(dentists, environment, scheduleConfigurations).run(null);
        assertThat(dentists.findByEmail("demo.odontologia@example.invalid")).isPresent();

        DemoDataLoader loader = mock(DemoDataLoader.class);
        new DemoDataSeeder(environment, dentists, loader).run(null);

        verifyNoInteractions(loader);
        assertThat(patients.count()).isZero();
    }

    @Test
    void adminSeedCreatesOneScheduleAndIsIdempotent() {
        MockEnvironment environment = adminEnvironment();
        AdminDentistSeeder seeder = new AdminDentistSeeder(dentists, environment, scheduleConfigurations);

        seeder.run(null);
        Dentist dentist = dentists.findByEmail("demo.odontologia@example.invalid").orElseThrow();
        assertThat(schedules.findByDentistId(dentist.getId())).isPresent();
        assertThat(schedules.count()).isEqualTo(1);

        seeder.run(null);

        assertThat(schedules.findByDentistId(dentist.getId())).isPresent();
        assertThat(schedules.count()).isEqualTo(1);
    }

    @Test
    void enabledDemoWithoutAdminSeedFailsWithoutDemoData() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        DemoDataLoader loader = mock(DemoDataLoader.class);

        assertThatThrownBy(() -> new DemoDataSeeder(environment, dentists, loader).run(null))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("APP_ADMIN_SEED");

        verifyNoInteractions(loader);
        assertThat(patients.count()).isZero();
    }

    @Test
    void enabledDemoWithoutAdminCredentialsFailsWithoutDemoData() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        environment.setProperty("APP_ADMIN_SEED", "true");
        DemoDataLoader loader = mock(DemoDataLoader.class);

        assertThatThrownBy(() -> new DemoDataSeeder(environment, dentists, loader).run(null))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("APP_ADMIN_EMAIL");

        verifyNoInteractions(loader);
        assertThat(patients.count()).isZero();
    }

    @Test
    void enabledDemoWithoutResolvableDentistFailsWithoutDemoData() {
        MockEnvironment environment = adminEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        DemoDataLoader loader = mock(DemoDataLoader.class);

        assertThatThrownBy(() -> new DemoDataSeeder(environment, dentists, loader).run(null))
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("odontólogo")
            .hasMessageContaining("APP_ADMIN_EMAIL");

        verifyNoInteractions(loader);
        assertThat(patients.count()).isZero();
    }

    @Test
    void enabledDemoWithValidConfigurationLoadsForTheConfiguredDentist() {
        MockEnvironment environment = adminEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        new AdminDentistSeeder(dentists, environment, scheduleConfigurations).run(null);
        DemoDataLoader loader = mock(DemoDataLoader.class);

        new DemoDataSeeder(environment, dentists, loader).run(null);

        verify(loader).loadOrReuse(argThat((Dentist dentist) ->
            "demo.odontologia@example.invalid".equals(dentist.getEmail())));
        verify(loader, never()).loadOrReuse(argThat((Dentist dentist) ->
            !"demo.odontologia@example.invalid".equals(dentist.getEmail())));
    }

    @Test
    void testProfileGuardSkipsDemoEvenWhenEnabled() {
        MockEnvironment environment = adminEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        environment.setActiveProfiles("test");
        DemoDataLoader loader = mock(DemoDataLoader.class);

        new DemoDataSeeder(environment, dentists, loader).run(null);

        verifyNoInteractions(loader);
    }

    private MockEnvironment adminEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_ADMIN_SEED", "true");
        environment.setProperty("APP_ADMIN_EMAIL", "demo.odontologia@example.invalid");
        environment.setProperty("APP_ADMIN_PASSWORD", "demo-ficticia-123");
        return environment;
    }
}
