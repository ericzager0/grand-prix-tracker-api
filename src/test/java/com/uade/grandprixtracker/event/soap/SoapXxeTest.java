package com.uade.grandprixtracker.event.soap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.uade.grandprixtracker.event.service.EventService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * XXE en el canal SOAP: un request con DOCTYPE y entidades externas tiene que rechazarse sin resolver
 * la entidad (el contenido del archivo nunca puede aparecer en la respuesta).
 * Levanta el servidor real en un puerto aleatorio, sin base de datos (EventService es un mock).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:1/sin-base",
        "spring.datasource.password=sin-base",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false"
})
class SoapXxeTest {

    private static final String NS = "http://uade.com/grandprixtracker/soap/events";

    @LocalServerPort
    private int port;

    @MockitoBean
    private EventService eventService;

    @TempDir
    Path tempDir;

    private HttpResponse<String> enviar(String xml) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ws"))
                .header("Content-Type", "text/xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(xml))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String envelope(String doctype, String id) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                %s
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ev="%s">
                  <soapenv:Body>
                    <ev:getEventByIdRequest>
                      <ev:id>%s</ev:id>
                    </ev:getEventByIdRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(doctype, NS, id);
    }

    // Control: sin entidades, el valor del id vuelve en el Fault. Esto demuestra que, si el parser
    // resolviera la entidad del test siguiente, el contenido del archivo aparecería en la respuesta.
    @Test
    void requestSinEntidades_ElValorDelIdApareceEnElFault() throws Exception {
        HttpResponse<String> response = enviar(envelope("", "MARCADOR-VISIBLE"));

        assertTrue(response.body().contains("MARCADOR-VISIBLE"), response.body());
    }

    @Test
    void entidadExterna_NoSeResuelveYSeRechaza() throws Exception {
        Path secreto = Files.writeString(tempDir.resolve("secreto.txt"), "MARCADOR-SECRETO-XXE");
        String doctype = "<!DOCTYPE foo [ <!ENTITY xxe SYSTEM \"" + secreto.toUri() + "\"> ]>";

        HttpResponse<String> response = enviar(envelope(doctype, "&xxe;"));

        assertNotEquals(200, response.statusCode());
        assertFalse(response.body().contains("MARCADOR-SECRETO-XXE"), response.body());
        verify(eventService, never()).getEventById(any());
    }

    // "Billion laughs": entidades internas anidadas que se expanden exponencialmente.
    @Test
    void expansionDeEntidadesInternas_SeRechaza() throws Exception {
        String doctype = """
                <!DOCTYPE lolz [
                  <!ENTITY lol "lol">
                  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                ]>""";

        HttpResponse<String> response = enviar(envelope(doctype, "&lol3;"));

        assertNotEquals(200, response.statusCode());
        assertFalse(response.body().contains("lollollol"), response.body());
        verify(eventService, never()).getEventById(any());
    }
}
