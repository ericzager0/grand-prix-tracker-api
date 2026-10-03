package com.uade.grandprixtracker.notification.email;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Motor de templates mínimo: lee un HTML de src/main/resources/templates/email/
 * y reemplaza los marcadores {{clave}} por sus valores.
 * Los valores ya deben venir escapados (ver HtmlUtils.htmlEscape) si provienen del usuario.
 */
@Component
public class EmailTemplateRenderer {

    private static final String TEMPLATES_PATH = "templates/email/";

    public String render(String templateName, Map<String, String> variables) {
        String html = load(templateName);
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            html = html.replace("{{" + variable.getKey() + "}}", variable.getValue() != null ? variable.getValue() : "");
        }
        return html;
    }

    private String load(String templateName) {
        ClassPathResource resource = new ClassPathResource(TEMPLATES_PATH + templateName);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el template de email " + templateName, e);
        }
    }
}
