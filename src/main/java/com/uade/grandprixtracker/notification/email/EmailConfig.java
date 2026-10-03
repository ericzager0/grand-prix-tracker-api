package com.uade.grandprixtracker.notification.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita la configuración de Brevo y la ejecución asíncrona (@Async),
 * para que el envío del email corra en otro hilo y no demore la respuesta del checkout.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(BrevoProperties.class)
public class EmailConfig {
}
