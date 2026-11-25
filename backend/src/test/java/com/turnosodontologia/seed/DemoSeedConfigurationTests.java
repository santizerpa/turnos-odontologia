package com.turnosodontologia.seed;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoSeedConfigurationTests {

    @Test
    void absentOptInIsDisabled() {
        DemoSeedConfiguration configuration = new DemoSeedConfiguration(new MockEnvironment());

        assertThat(configuration.isEnabled()).isFalse();
    }

    @Test
    void falseOptInIsDisabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "false");

        assertThat(new DemoSeedConfiguration(environment).isEnabled()).isFalse();
    }

    @Test
    void trueOptInIsEnabled() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");

        assertThat(new DemoSeedConfiguration(environment).isEnabled()).isTrue();
    }

    @Test
    void enabledDemoWithoutAdminSeedFailsWithActionableMessage() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");

        assertThatThrownBy(() -> new DemoSeedConfiguration(environment).requireAdminEmail())
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("APP_ADMIN_SEED");
    }

    @Test
    void enabledDemoWithoutAdminCredentialsFailsWithActionableMessage() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        environment.setProperty("APP_ADMIN_SEED", "true");

        assertThatThrownBy(() -> new DemoSeedConfiguration(environment).requireAdminEmail())
            .isInstanceOf(DemoSeedException.class)
            .hasMessageContaining("APP_ADMIN_EMAIL")
            .hasMessageContaining("APP_ADMIN_PASSWORD");
    }

    @Test
    void enabledDemoWithAdminConfigurationReturnsNormalizedEmail() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_DEMO_SEED", "true");
        environment.setProperty("APP_ADMIN_SEED", "true");
        environment.setProperty("APP_ADMIN_EMAIL", "  Demo.Odontologia@Example.INVALID  ");
        environment.setProperty("APP_ADMIN_PASSWORD", "demo-ficticia-123");

        assertThat(new DemoSeedConfiguration(environment).requireAdminEmail())
            .isEqualTo("demo.odontologia@example.invalid");
    }
}
