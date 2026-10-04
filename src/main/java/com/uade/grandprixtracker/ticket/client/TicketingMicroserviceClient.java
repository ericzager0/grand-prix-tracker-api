package com.uade.grandprixtracker.ticket.client;

import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cliente HTTP para interactuar con el microservicio de Ticketing de F1.
 * Valida la disponibilidad de entradas y descuenta el stock en el sistema SOAP legado.
 */
@Component
public class TicketingMicroserviceClient {

    private static final Logger log = LoggerFactory.getLogger(TicketingMicroserviceClient.class);

    private final RestClient restClient;
    private final String endpointUrl;
    private final JsonMapper jsonMapper;

    @Autowired
    public TicketingMicroserviceClient(
            @Value("${ticketing.microservice.url:https://microservice-ticketing-gp.onrender.com/api/microservicios/tickets/reservar}")
            String endpointUrl,
            @Value("${ticketing.microservice.connect-timeout-seconds:15}")
            int connectTimeoutSeconds,
            @Value("${ticketing.microservice.read-timeout-seconds:30}")
            int readTimeoutSeconds,
            JsonMapper jsonMapper) {
        this(buildRestClient(connectTimeoutSeconds, readTimeoutSeconds), endpointUrl, jsonMapper);
    }

    public TicketingMicroserviceClient(
            RestClient restClient,
            String endpointUrl,
            JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.endpointUrl = endpointUrl;
        this.jsonMapper = jsonMapper;
    }

    private static RestClient buildRestClient(int connectTimeoutSeconds, int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Realiza la reserva de entradas en el microservicio externo.
     * En caso de stock insuficiente en el sistema SOAP legado, el microservicio devuelve HTTP 409
     * y este método lanza {@link StockInsuficienteException} con el mensaje detallado.
     *
     * @param idEntrada Identificador UUID de la entrada en Supabase
     * @param cantidad  Cantidad de entradas a reservar
     * @return Respuesta de confirmación emitida por el sistema legado SOAP
     */
    public TicketingMicroserviceResponse reservar(UUID idEntrada, int cantidad) {
        log.info("Llamando al microservicio de ticketing para reservar: idEntrada={}, cantidad={}", idEntrada, cantidad);

        TicketingMicroserviceRequest request = new TicketingMicroserviceRequest(idEntrada, cantidad);

        try {
            TicketingApiResponse<TicketingMicroserviceResponse> response = restClient.post()
                    .uri(endpointUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (clientReq, clientResp) -> {
                        HttpStatusCode status = clientResp.getStatusCode();
                        String errorMessage = extractErrorMessage(clientResp.getBody());
                        log.warn("El microservicio de ticketing respondió con error {}: {}", status.value(), errorMessage);

                        if (status.value() == HttpStatus.CONFLICT.value()) {
                            throw new StockInsuficienteException(
                                    errorMessage != null && !errorMessage.isBlank()
                                            ? errorMessage
                                            : "Stock insuficiente para realizar la reserva");
                        }
                        if (status.value() == HttpStatus.NOT_FOUND.value()) {
                            throw new ResourceNotFoundException("Entrada", "id", idEntrada);
                        }
                        if (status.value() == HttpStatus.BAD_REQUEST.value()) {
                            throw new IllegalArgumentException(
                                    errorMessage != null && !errorMessage.isBlank()
                                            ? errorMessage
                                            : "Parámetros de reserva inválidos");
                        }
                        throw new IllegalStateException(
                                "Error en el microservicio de tickets (" + status.value() + "): " + errorMessage);
                    })
                    .body(new ParameterizedTypeReference<TicketingApiResponse<TicketingMicroserviceResponse>>() {});

            if (response != null && response.data() != null) {
                log.info("Reserva confirmada en SOAP con código: {}", response.data().codigoConfirmacion());
                return response.data();
            }

            throw new IllegalStateException("El microservicio de ticketing no devolvió información de la reserva.");
        } catch (ResourceAccessException ex) {
            log.error("Fallo de conexión con el microservicio de tickets: {}", ex.getMessage());
            throw new IllegalStateException("No se pudo conectar con el microservicio de tickets: " + ex.getMessage(), ex);
        }
    }

    private String extractErrorMessage(InputStream bodyStream) {
        if (bodyStream == null) {
            return null;
        }
        try {
            JsonNode root = jsonMapper.readTree(bodyStream);
            if (root.has("message") && !root.get("message").isNull()) {
                return root.get("message").asText();
            }
        } catch (Exception e) {
            log.debug("No se pudo parsear el cuerpo del error como JSON: {}", e.getMessage());
        }
        return null;
    }
}
