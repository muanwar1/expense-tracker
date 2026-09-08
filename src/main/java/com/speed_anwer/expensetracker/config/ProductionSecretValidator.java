package com.speed_anwer.expensetracker.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Startup guard that fails fast when the application is running under the {@code prod}
 * Spring profile but {@code JWT_SECRET} has not been overridden from the dev-default value.
 *
 * <p>This intentionally causes an {@link IllegalStateException} during application startup
 * so that a misconfigured deployment is caught immediately rather than silently running with
 * an insecure, publicly-known secret key.
 */
@Configuration
public class ProductionSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecretValidator.class);

    /**
     * The exact dev-default value hard-coded in {@code application.properties}.
     * If {@code JWT_SECRET} resolves to this string while the {@code prod} profile is active,
     * the application refuses to start.
     */
    private static final String DEV_DEFAULT_SECRET =
            "dev-only-secret-key-change-me-must-be-at-least-32-chars";

    private final String jwtSecret;
    private final Environment environment;

    public ProductionSecretValidator(
            @Value("${jwt.secret}") String jwtSecret,
            Environment environment) {
        this.jwtSecret = jwtSecret;
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!isProd) {
            return;
        }

        boolean secretIsDefault = DEV_DEFAULT_SECRET.equals(jwtSecret);
        boolean secretIsBlank   = jwtSecret == null || jwtSecret.isBlank();

        if (secretIsBlank || secretIsDefault) {
            String msg = "STARTUP FAILURE: The 'prod' Spring profile is active but " +
                    "JWT_SECRET is " + (secretIsBlank ? "blank" : "still set to the dev-default value") + ". " +
                    "Set the JWT_SECRET environment variable to a securely generated secret " +
                    "of at least 32 characters before starting the application in production.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("ProductionSecretValidator: JWT_SECRET is set and is not the dev default — OK.");
    }
}
