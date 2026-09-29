package com.uade.grandprixtracker.payment.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.auth.config.SecurityConfig;
import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Pasa por la cadena de filtros de SecurityConfig; el JwtDecoder es un mock y el usuario se simula con jwt().
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getPaymentMethods_Devuelve200SinExponerElToken() throws Exception {
        UUID idCliente = UUID.randomUUID();
        when(paymentService.listarPorCliente(idCliente)).thenReturn(List.of(
                new MetodoPagoResponseDto(UUID.randomUUID(), "Credito", "4242", "12/28", false)));

        mockMvc.perform(get("/payment-methods").with(jwt().jwt(j -> j.subject(idCliente.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ultimos4Digitos").value("4242"))
                .andExpect(jsonPath("$.data[0].vencida").value(false))
                .andExpect(jsonPath("$.data[0].proveedorToken").doesNotExist());
    }

    @Test
    void getPaymentMethods_ClienteInexistente_Devuelve404() throws Exception {
        UUID idCliente = UUID.randomUUID();
        when(paymentService.listarPorCliente(idCliente)).thenThrow(new ResourceNotFoundException("Cliente", "id", idCliente));

        mockMvc.perform(get("/payment-methods").with(jwt().jwt(j -> j.subject(idCliente.toString()))))
                .andExpect(status().isNotFound());
    }
}
