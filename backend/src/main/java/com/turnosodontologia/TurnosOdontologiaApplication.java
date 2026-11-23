package com.turnosodontologia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TurnosOdontologiaApplication {
    public static void main(String[] args) {
        SpringApplication.run(TurnosOdontologiaApplication.class, args);
    }
}
