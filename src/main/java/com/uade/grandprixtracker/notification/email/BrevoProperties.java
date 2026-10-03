package com.uade.grandprixtracker.notification.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Brevo leída de application.properties (prefijo "brevo").
 * Los valores reales vienen de variables de entorno (en Render), nunca hardcodeados.
 */
@ConfigurationProperties(prefix = "brevo")
public record BrevoProperties(
        String apiKey,
        String senderEmail,
        String senderName
) {

    /** Si falta la API key o el remitente, el envío de emails queda deshabilitado (útil en local). */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && senderEmail != null && !senderEmail.isBlank();
    }
}
