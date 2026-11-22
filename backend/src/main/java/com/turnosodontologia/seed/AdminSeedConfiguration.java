package com.turnosodontologia.seed;

import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdminSeedConfiguration {
    private final Environment environment;

    public AdminSeedConfiguration(Environment environment) {
        this.environment = environment;
    }

    public Map<String, String> resolve() {
        String email = environment.getProperty("APP_ADMIN_EMAIL");
        String password = environment.getProperty("APP_ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AdminSeedException("Se requieren APP_ADMIN_EMAIL y APP_ADMIN_PASSWORD");
        }
        String hash = new BCryptPasswordEncoder().encode(password);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("email", email.trim().toLowerCase());
        values.put("password", hash);
        return values;
    }
}
