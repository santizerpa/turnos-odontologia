package com.turnosodontologia.seed;

import com.turnosodontologia.dentist.Dentist;
import com.turnosodontologia.dentist.DentistRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(DemoDataSeeder.ORDER)
public class DemoDataSeeder implements ApplicationRunner {
    public static final int ORDER = 20;

    private final DentistRepository dentists;
    private final DemoSeedConfiguration configuration;
    private final Environment environment;
    private final DemoDataLoader loader;

    public DemoDataSeeder(Environment environment, DentistRepository dentists, DemoDataLoader loader) {
        this.dentists = dentists;
        this.configuration = new DemoSeedConfiguration(environment);
        this.environment = environment;
        this.loader = loader;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!configuration.isEnabled()) return;
        if (environment.matchesProfiles("test")) return;

        String email = configuration.requireAdminEmail();
        Dentist dentist = dentists.findByEmail(email).orElseThrow(() -> new DemoSeedException(
            "APP_DEMO_SEED=true no encontró el odontólogo configurado en APP_ADMIN_EMAIL. "
                + "Verificá APP_ADMIN_SEED=true y las credenciales ficticias; no se cargaron datos de demo."));
        loader.loadOrReuse(dentist);
    }
}
