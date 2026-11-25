package com.turnosodontologia.seed;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(AdminDentistSeeder.ORDER)
public class AdminDentistSeeder implements ApplicationRunner {
    public static final int ORDER = 10;
    private final DentistRepository dentists;
    private final AdminSeedConfiguration configuration;
    private final Environment environment;

    public AdminDentistSeeder(DentistRepository dentists, Environment environment) {
        this.dentists = dentists;
        this.configuration = new AdminSeedConfiguration(environment);
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!configurationEnabled()) return;
        var resolved = configuration.resolve();
        String email = resolved.get("email");
        dentists.findByEmail(email).orElseGet(() -> dentists.save(new Dentist(email, resolved.get("password"), "Admin", "Demostración")));
    }

    private boolean configurationEnabled() {
        return environment.containsProperty("APP_ADMIN_SEED")
            && Boolean.parseBoolean(environment.getProperty("APP_ADMIN_SEED"));
    }
}
