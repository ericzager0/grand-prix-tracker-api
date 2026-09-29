package com.uade.grandprixtracker.auth.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.booking.controller.BookingController;
import com.uade.grandprixtracker.booking.service.CheckoutFacade;
import com.uade.grandprixtracker.booking.service.ReservaService;
import com.uade.grandprixtracker.event.controller.EventController;
import com.uade.grandprixtracker.event.service.EventService;
import com.uade.grandprixtracker.payment.controller.PaymentController;
import com.uade.grandprixtracker.payment.service.PaymentService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Pasa por la cadena de filtros real de SecurityConfig. El JwtDecoder es un mock: no se consulta el JWKS de Supabase.
@WebMvcTest(controllers = {BookingController.class, PaymentController.class, EventController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    private static final String SIN_TOKEN = "Se requiere iniciar sesión para acceder a este recurso";
    private static final String TOKEN_INVALIDO = "La sesión es inválida o expiró";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CheckoutFacade checkoutFacade;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private EventService eventService;

    @Test
    void getBookings_SinToken_Devuelve401ConApiResponse() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(SIN_TOKEN))
                .andExpect(jsonPath("$.data").value(nullValue()));

        verifyNoInteractions(reservaService);
    }

    @Test
    void getBooking_SinToken_Devuelve401() throws Exception {
        mockMvc.perform(get("/bookings/{idReserva}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(SIN_TOKEN));

        verifyNoInteractions(reservaService);
    }

    @Test
    void checkout_SinToken_Devuelve401() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(SIN_TOKEN));

        verifyNoInteractions(checkoutFacade);
    }

    @Test
    void getPaymentMethods_SinToken_Devuelve401() throws Exception {
        mockMvc.perform(get("/payment-methods"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(SIN_TOKEN));

        verifyNoInteractions(paymentService);
    }

    @Test
    void getBookings_TokenInvalidoOVencido_Devuelve401() throws Exception {
        when(jwtDecoder.decode("token-vencido")).thenThrow(new BadJwtException("Jwt expired"));

        mockMvc.perform(get("/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer token-vencido"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(TOKEN_INVALIDO));

        verifyNoInteractions(reservaService);
    }

    @Test
    void getBookings_TokenValido_UsaElSubComoCliente() throws Exception {
        UUID idCliente = UUID.randomUUID();
        Jwt token = Jwt.withTokenValue("token-ok")
                .header("alg", "ES256")
                .subject(idCliente.toString())
                .build();
        when(jwtDecoder.decode("token-ok")).thenReturn(token);
        when(reservaService.listarPorCliente(idCliente)).thenReturn(List.of());

        mockMvc.perform(get("/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer token-ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(reservaService).listarPorCliente(idCliente);
    }

    @Test
    void getEvents_SinToken_EsPublico() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getEvents_ConTokenVencido_SigueSiendoPublicoYNoValidaElToken() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of());

        mockMvc.perform(get("/events").header(HttpHeaders.AUTHORIZATION, "Bearer token-vencido"))
                .andExpect(status().isOk());

        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void preflight_SinToken_PasaConHeadersDeCors() throws Exception {
        mockMvc.perform(options("/bookings")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}
