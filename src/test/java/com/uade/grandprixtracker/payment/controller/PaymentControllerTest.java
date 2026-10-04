package com.uade.grandprixtracker.payment.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.auth.config.SecurityConfig;
import com.uade.grandprixtracker.payment.dto.MetodoPagoRequestDto;
import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
                new MetodoPagoResponseDto(UUID.randomUUID(), "Credito", "4242", "12/28", false, "Juan Perez", "+54 11 1234-5678", new java.math.BigDecimal("35123456"))));

        mockMvc.perform(get("/payment-methods").with(jwt().jwt(j -> j.subject(idCliente.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ultimos4Digitos").value("4242"))
                .andExpect(jsonPath("$.data[0].vencida").value(false))
                .andExpect(jsonPath("$.data[0].nombre_titular").value("Juan Perez"))
                .andExpect(jsonPath("$.data[0].telefono").value("+54 11 1234-5678"))
                .andExpect(jsonPath("$.data[0].dni").value(35123456))
                .andExpect(jsonPath("$.data[0].proveedorToken").doesNotExist());
    }

    @Test
    void getPaymentMethods_ClienteInexistente_Devuelve404() throws Exception {
        UUID idCliente = UUID.randomUUID();
        when(paymentService.listarPorCliente(idCliente)).thenThrow(new ResourceNotFoundException("Cliente", "id", idCliente));

        mockMvc.perform(get("/payment-methods").with(jwt().jwt(j -> j.subject(idCliente.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addPaymentMethod_RequestValida_Devuelve201() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UUID idMetodo = UUID.randomUUID();
        when(paymentService.agregar(eq(idCliente), any(MetodoPagoRequestDto.class))).thenReturn(
                new MetodoPagoResponseDto(idMetodo, "Credito", "1234", "12/26", false, "AYRTON SENNA", "+54 11 1234-5678", new java.math.BigDecimal("35123456")));

        mockMvc.perform(post("/payment-methods")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "Credito",
                                  "ultimos4Digitos": "1234",
                                  "fechaExpiracion": "12/26",
                                  "proveedorToken": "tok_simulado_123",
                                  "nombre_titular": "AYRTON SENNA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Método de pago agregado correctamente"))
                .andExpect(jsonPath("$.data.idMetodoPago").value(idMetodo.toString()))
                .andExpect(jsonPath("$.data.tipo").value("Credito"))
                .andExpect(jsonPath("$.data.ultimos4Digitos").value("1234"))
                .andExpect(jsonPath("$.data.fechaExpiracion").value("12/26"))
                .andExpect(jsonPath("$.data.nombre_titular").value("AYRTON SENNA"));
    }

    @Test
    void addPaymentMethod_DatosInvalidos_Devuelve400() throws Exception {
        UUID idCliente = UUID.randomUUID();

        mockMvc.perform(post("/payment-methods")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "Invalido",
                                  "ultimos4Digitos": "12",
                                  "fechaExpiracion": "99/99"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(paymentService);
    }

    @Test
    void updatePaymentMethod_RequestValida_Devuelve200() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UUID idMetodo = UUID.randomUUID();
        when(paymentService.actualizar(eq(idCliente), eq(idMetodo), any(MetodoPagoRequestDto.class))).thenReturn(
                new MetodoPagoResponseDto(idMetodo, "Credito", "1234", "12/26", false, "AYRTON SENNA", "+54 11 1234-5678", new java.math.BigDecimal("35123456")));

        mockMvc.perform(put("/payment-methods/{id}", idMetodo)
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "Credito",
                                  "ultimos4Digitos": "1234",
                                  "fechaExpiracion": "12/26",
                                  "proveedorToken": "tok_simulado_123",
                                  "nombre_titular": "AYRTON SENNA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Método de pago actualizado correctamente"))
                .andExpect(jsonPath("$.data.idMetodoPago").value(idMetodo.toString()))
                .andExpect(jsonPath("$.data.nombre_titular").value("AYRTON SENNA"));
    }

    @Test
    void updatePaymentMethod_MetodoNoExiste_Devuelve404() throws Exception {
        UUID idCliente = UUID.randomUUID();
        UUID idMetodo = UUID.randomUUID();
        when(paymentService.actualizar(eq(idCliente), eq(idMetodo), any(MetodoPagoRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Método de pago", "id", idMetodo));

        mockMvc.perform(put("/payment-methods/{id}", idMetodo)
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "Credito",
                                  "ultimos4Digitos": "1234",
                                  "fechaExpiracion": "12/26",
                                  "proveedorToken": "tok_simulado_123",
                                  "nombre_titular": "AYRTON SENNA"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updatePaymentMethod_IdInvalido_Devuelve400() throws Exception {
        UUID idCliente = UUID.randomUUID();

        mockMvc.perform(put("/payment-methods/{id}", "no-es-un-uuid")
                        .with(jwt().jwt(j -> j.subject(idCliente.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipo": "Credito",
                                  "ultimos4Digitos": "1234",
                                  "fechaExpiracion": "12/26",
                                  "proveedorToken": "tok_simulado_123",
                                  "nombre_titular": "AYRTON SENNA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(paymentService);
    }
}
