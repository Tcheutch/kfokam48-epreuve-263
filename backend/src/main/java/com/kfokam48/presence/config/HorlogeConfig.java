package com.kfokam48.presence.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * L'horloge est un bean injectable, jamais {@code Instant.now()} au fil du code.
 *
 * <p>Sans cela, RG2 (expiration à 15 minutes) et RG4 (blocage de 2 minutes) ne
 * seraient testables qu'en faisant attendre la suite de tests.
 */
@Configuration
public class HorlogeConfig {

    @Bean
    public Clock horloge() {
        return Clock.systemUTC();
    }
}
