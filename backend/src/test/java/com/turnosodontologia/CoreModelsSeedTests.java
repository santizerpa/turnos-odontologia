package com.turnosodontologia;

import com.turnosodontologia.seed.AdminSeedConfiguration;
import com.turnosodontologia.seed.AdminSeedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreModelsSeedTests {
    @Test
    void absentAdminVariablesFailExplicitly() {
        AdminSeedConfiguration config = new AdminSeedConfiguration(new MockEnvironment());
        assertThatThrownBy(config::resolve).isInstanceOf(AdminSeedException.class);
    }

    @Test
    void presentAdminVariablesReturnFictitiousValuesAndPasswordsAreNotPlaintext() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("APP_ADMIN_EMAIL", "admin@demo.invalid");
        environment.setProperty("APP_ADMIN_PASSWORD", "DemoPasswordOnly");
        AdminSeedConfiguration config = new AdminSeedConfiguration(environment);
        Map<String, String> resolved = config.resolve();
        assertThat(resolved.get("email")).isEqualTo("admin@demo.invalid");
        assertThat(resolved.get("password")).isNotEqualTo("DemoPasswordOnly");
        assertThat(resolved.get("password")).startsWith("$2");
    }
}
