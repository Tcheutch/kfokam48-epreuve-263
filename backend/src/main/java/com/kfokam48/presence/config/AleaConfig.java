package com.kfokam48.presence.config;

import java.security.SecureRandom;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Le hasard est un bean, comme l'horloge : sans cela, RG7 (« le système, au
 * hasard ») ne serait vérifiable que par des tests qui passent parfois.
 */
@Configuration
public class AleaConfig {

    @Bean
    public Random alea() {
        return new SecureRandom();
    }
}
