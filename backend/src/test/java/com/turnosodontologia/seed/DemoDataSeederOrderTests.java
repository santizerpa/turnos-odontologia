package com.turnosodontologia.seed;

import com.turnosodontologia.dentist.DentistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
class DemoDataSeederOrderTests {

    @Autowired DentistRepository dentists;

    @Test
    void bothRunnersDeclareExplicitOrderWithAdminFirst() {
        Order admin = AnnotatedElementUtils.findMergedAnnotation(AdminDentistSeeder.class, Order.class);
        Order demo = AnnotatedElementUtils.findMergedAnnotation(DemoDataSeeder.class, Order.class);

        assertThat(admin).as("AdminDentistSeeder debe declarar @Order explícito").isNotNull();
        assertThat(demo).as("DemoDataSeeder debe declarar @Order explícito").isNotNull();
        assertThat(admin.value()).isLessThan(demo.value());
        assertThat(admin.value()).isLessThan(Ordered.LOWEST_PRECEDENCE);
        assertThat(demo.value()).isLessThan(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void springOrderCreatesTheDentistBeforeTheDemoDatasetLoads() throws Exception {
        MockEnvironment environment = adminEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        DemoDataLoader loader = mock(DemoDataLoader.class);

        AdminDentistSeeder admin = new AdminDentistSeeder(dentists, environment);
        DemoDataSeeder demo = new DemoDataSeeder(environment, dentists, loader);
        List<ApplicationRunner> runners = new ArrayList<>(List.of(demo, admin));
        AnnotationAwareOrderComparator.sort(runners);

        for (ApplicationRunner runner : runners) {
            runner.run(null);
        }

        assertThat(runners.get(0)).isInstanceOf(AdminDentistSeeder.class);
        assertThat(dentists.findByEmail("demo.odontologia@example.invalid")).isPresent();
        verify(loader).loadOrReuse(argThat(dentist ->
            "demo.odontologia@example.invalid".equals(dentist.getEmail())));
    }

    private MockEnvironment adminEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_ADMIN_SEED", "true");
        environment.setProperty("APP_ADMIN_EMAIL", "demo.odontologia@example.invalid");
        environment.setProperty("APP_ADMIN_PASSWORD", "demo-ficticia-123");
        return environment;
    }
}
