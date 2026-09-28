package com.uade.grandprixtracker.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.booking.dto.CheckoutRequestDto;
import com.uade.grandprixtracker.booking.dto.CheckoutResponseDto;
import com.uade.grandprixtracker.booking.service.CheckoutFacade;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CheckoutFacade checkoutFacade;

    @InjectMocks
    private BookingController bookingController;

    private UUID idCliente;
    private UUID idEvento;
    private UUID idEntrada;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        idCliente = UUID.randomUUID();
        idEvento = UUID.randomUUID();
        idEntrada = UUID.randomUUID();
    }

    private String body(int cantidad) {
        return """
                {
                  "idEvento": "%s",
                  "entradas": [{ "idEntrada": "%s", "cantidad": %d }],
                  "pago": { "tipo": "Credito", "ultimos4Digitos": "4242", "fechaExpiracion": "12/28", "proveedorToken": "tok_test" }
                }
                """.formatted(idEvento, idEntrada, cantidad);
    }

    @Test
    void checkout_RequestValida_Devuelve201ConLaReserva() throws Exception {
        CheckoutResponseDto respuesta = new CheckoutResponseDto(
                UUID.randomUUID(), "GP-ABCD2345", "Pagada", OffsetDateTime.parse("2026-10-01T12:00:00Z"),
                new BigDecimal("700.00"), true, false, false, UUID.randomUUID(),
                List.of(new CheckoutResponseDto.EntradaLinea(idEntrada, "Tribuna A", "General", 2,
                        new BigDecimal("350.00"), new BigDecimal("700.00"))),
                List.of(), List.of());
        when(checkoutFacade.checkout(eq(idCliente), any(CheckoutRequestDto.class))).thenReturn(respuesta);

        mockMvc.perform(post("/bookings")
                        .header("X-Cliente-Id", idCliente.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.codigoConfirmacion").value("GP-ABCD2345"))
                .andExpect(jsonPath("$.data.estado").value("Pagada"))
                .andExpect(jsonPath("$.data.totalUsd").value(700.00))
                .andExpect(jsonPath("$.data.entradas[0].cantidad").value(2));
    }

    @Test
    void checkout_SinHeaderCliente_Devuelve400() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(checkoutFacade);
    }

    @Test
    void checkout_CantidadInvalida_Devuelve400() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header("X-Cliente-Id", idCliente.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(checkoutFacade);
    }

    @Test
    void checkout_SinStock_Devuelve409() throws Exception {
        when(checkoutFacade.checkout(eq(idCliente), any(CheckoutRequestDto.class)))
                .thenThrow(new StockInsuficienteException("No hay stock suficiente para la tribuna Tribuna A"));

        mockMvc.perform(post("/bookings")
                        .header("X-Cliente-Id", idCliente.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No hay stock suficiente para la tribuna Tribuna A"));
    }
}
