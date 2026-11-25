package com.turnosodontologia.seed;

import org.springframework.core.env.Environment;

/**
 * Configuración del conjunto de demo. Es un opt-in separado del seed administrativo:
 * la demo requiere APP_ADMIN_SEED=true y credenciales ficticias para resolver al odontólogo.
 */
public class DemoSeedConfiguration {
    static final String DEMO_SEED = "APP_DEMO_SEED";
    static final String ADMIN_SEED = "APP_ADMIN_SEED";
    static final String ADMIN_EMAIL = "APP_ADMIN_EMAIL";
    static final String ADMIN_PASSWORD = "APP_ADMIN_PASSWORD";

    private final Environment environment;

    public DemoSeedConfiguration(Environment environment) {
        this.environment = environment;
    }

    public boolean isEnabled() {
        return flag(DEMO_SEED);
    }

    public String requireAdminEmail() {
        if (!flag(ADMIN_SEED)) {
            throw new DemoSeedException(
                "APP_DEMO_SEED=true requiere APP_ADMIN_SEED=true para crear el odontólogo administrativo antes del dataset de demo.");
        }
        String email = environment.getProperty(ADMIN_EMAIL);
        String password = environment.getProperty(ADMIN_PASSWORD);
        if (isBlank(email) || isBlank(password)) {
            throw new DemoSeedException(
                "APP_DEMO_SEED=true requiere APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD no vacíos con valores ficticios.");
        }
        return email.trim().toLowerCase();
    }

    private boolean flag(String name) {
        return environment.containsProperty(name) && Boolean.parseBoolean(environment.getProperty(name));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
