package com.turnosodontologia.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class LocalProfileEnvironmentValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (!environment.matchesProfiles("local")) {
            return;
        }

        List<String> missing = new ArrayList<>();
        for (String name : List.of("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD")) {
            if (!environment.containsProperty(name)) {
                missing.add(name);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("Perfil local requiere variables obligatorias faltantes: " + String.join(", ", missing));
        }
    }
}
