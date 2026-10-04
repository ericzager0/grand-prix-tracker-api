package com.uade.grandprixtracker.ticket.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class TicketingMicroserviceClientTest {

    private static final String URL = "https://microservice-ticketing-gp.onrender.com/api/microservicios/tickets/reservar";

    private MockRestServiceServer mockServer;
    private TicketingMicroserviceClient client;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new TicketingMicroserviceClient(builder.build(), URL, jsonMapper);
    }

    @Test
    void reservar_Exitoso_RetornaDatosConfirmacionSoap() {
        UUID idEntrada = UUID.randomUUID();
        String jsonResponse = """
                {
                  "success": true,
                  "message": "Reserva confirmada en el sistema SOAP con código 'TKT-99812'",
                  "data": {
                    "codigoConfirmacion": "TKT-99812",
                    "idEntrada": "%s",
                    "idEvento": "992ae124-3d59-4adb-9fd2-f0e825c605e8",
                    "codigoEvento": "F1-2026-MAD",
                    "carrera": "Madrid",
                    "nombreTribuna": "Paddock Club Madrid",
                    "tipo": "VIP",
                    "cantidad": 2,
                    "precioUnitarioUsd": 3500.00,
                    "precioTotalUsd": 7000.00,
                    "estado": "SUCCESS",
                    "mensaje": "Reserva confirmada exitosamente",
                    "fechaReserva": "2026-10-04T12:00:00Z"
                  }
                }
                """.formatted(idEntrada);

        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.idEntrada").value(idEntrada.toString()))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        TicketingMicroserviceResponse response = client.reservar(idEntrada, 2);

        assertNotNull(response);
        assertEquals("TKT-99812", response.codigoConfirmacion());
        assertEquals("F1-2026-MAD", response.codigoEvento());
        assertEquals(2, response.cantidad());
        assertEquals(new BigDecimal("7000.00"), response.precioTotalUsd());
        mockServer.verify();
    }

    @Test
    void reservar_StockInsuficiente_LanzaStockInsuficienteExceptionConMensajeMicroservicio() {
        UUID idEntrada = UUID.randomUUID();
        String jsonError = """
                {
                  "success": false,
                  "message": "Stock insuficiente para la tribuna 'Paddock Club Madrid' en el evento 'F1-2026-MAD'. Stock disponible: 1, cantidad solicitada: 5.",
                  "data": null
                }
                """;

        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonError));

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
                () -> client.reservar(idEntrada, 5));

        assertTrue(ex.getMessage().contains("Stock insuficiente para la tribuna 'Paddock Club Madrid'"));
        mockServer.verify();
    }

    @Test
    void reservar_StockInsuficienteSinMensaje_LanzaStockInsuficienteExceptionConDefault() {
        UUID idEntrada = UUID.randomUUID();
        String jsonError = """
                {
                  "success": false,
                  "message": "",
                  "data": null
                }
                """;

        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonError));

        StockInsuficienteException ex = assertThrows(StockInsuficienteException.class,
                () -> client.reservar(idEntrada, 5));

        assertEquals("Stock insuficiente para realizar la reserva", ex.getMessage());
        mockServer.verify();
    }

    @Test
    void reservar_EntradaNoEncontrada_LanzaResourceNotFoundException() {
        UUID idEntrada = UUID.randomUUID();
        String jsonError = """
                {
                  "success": false,
                  "message": "Entrada no encontrado con id: '%s'",
                  "data": null
                }
                """.formatted(idEntrada);

        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonError));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> client.reservar(idEntrada, 1));

        assertTrue(ex.getMessage().contains("Entrada"));
        assertTrue(ex.getMessage().contains(idEntrada.toString()));
        mockServer.verify();
    }

    @Test
    void reservar_ParametrosInvalidos_LanzaIllegalArgumentException() {
        UUID idEntrada = UUID.randomUUID();
        String jsonError = """
                {
                  "success": false,
                  "message": "Error de validación: cantidad: La cantidad debe ser mayor a 0",
                  "data": null
                }
                """;

        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonError));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> client.reservar(idEntrada, 0));

        assertTrue(ex.getMessage().contains("Error de validación"));
        mockServer.verify();
    }

    @Test
    void reservar_ErrorInterno_LanzaIllegalStateException() {
        UUID idEntrada = UUID.randomUUID();
        String jsonError = """
                {
                  "success": false,
                  "message": "Error al comunicarse con el servicio legado SOAP F1 Ticketing",
                  "data": null
                }
                """;

        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonError));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.reservar(idEntrada, 2));

        assertTrue(ex.getMessage().contains("Error en el microservicio de tickets"));
        mockServer.verify();
    }
}
