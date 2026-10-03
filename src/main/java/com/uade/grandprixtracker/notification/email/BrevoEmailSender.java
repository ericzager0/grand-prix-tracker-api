package com.uade.grandprixtracker.notification.email;

import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP de la API transaccional de Brevo (POST /v3/smtp/email).
 * Se usa la API HTTP y no SMTP porque Render bloquea los puertos SMTP en el plan free.
 */
@Component
public class BrevoEmailSender {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final BrevoProperties properties;
    private final RestClient restClient;

    public BrevoEmailSender(BrevoProperties properties) {
        this.properties = properties;

        // Timeouts cortos: si Brevo no responde, no queremos hilos colgados.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .requestFactory(requestFactory)
                .defaultHeader("api-key", properties.apiKey() != null ? properties.apiKey() : "")
                .build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /**
     * Envía un email HTML. Lanza excepción si Brevo responde con error (4xx/5xx):
     * el manejo del error es responsabilidad de quien llama.
     */
    public void send(String toEmail, String toName, String subject, String htmlContent) {
        BrevoEmailRequest body = new BrevoEmailRequest(
                new Contact(properties.senderName(), properties.senderEmail()),
                List.of(new Contact(toName, toEmail)),
                subject,
                htmlContent
        );

        restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public record Contact(String name, String email) {}

    public record BrevoEmailRequest(Contact sender, List<Contact> to, String subject, String htmlContent) {}
}
