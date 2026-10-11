package com.example.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClaimsMigrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimsMigrationApplication.class, args);
    }
}
