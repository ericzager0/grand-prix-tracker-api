package com.uade.grandprixtracker.payment.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPaymentMethods_Devuelve200SinExponerElToken() throws Exception {
        UUID idCliente = UUID.randomUUID();
        when(paymentService.listarPorCliente(idCliente)).thenReturn(List.of(
                new MetodoPagoResponseDto(UUID.randomUUID(), "Credito", "4242", "12/28", false)));

        mockMvc.perform(get("/payment-methods").header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ultimos4Digitos").value("4242"))
                .andExpect(jsonPath("$.data[0].vencida").value(false))
                .andExpect(jsonPath("$.data[0].proveedorToken").doesNotExist());
    }

    @Test
    void getPaymentMethods_SinHeader_Devuelve400() throws Exception {
        mockMvc.perform(get("/payment-methods"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void getPaymentMethods_ClienteInexistente_Devuelve404() throws Exception {
        UUID idCliente = UUID.randomUUID();
        when(paymentService.listarPorCliente(idCliente)).thenThrow(new ResourceNotFoundException("Cliente", "id", idCliente));

        mockMvc.perform(get("/payment-methods").header("X-Cliente-Id", idCliente.toString()))
                .andExpect(status().isNotFound());
    }
}
