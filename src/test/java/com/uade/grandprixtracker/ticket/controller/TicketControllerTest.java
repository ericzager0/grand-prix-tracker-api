package com.uade.grandprixtracker.ticket.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.uade.grandprixtracker.shared.exception.GlobalExceptionHandler;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.ticket.dto.EntradaResponseDto;
import com.uade.grandprixtracker.ticket.service.TicketService;
import java.math.BigDecimal;
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
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTicketsByEvent_Devuelve200ConLasEntradas() throws Exception {
        UUID idEvento = UUID.randomUUID();
        when(ticketService.listarPorEvento(idEvento)).thenReturn(List.of(
                new EntradaResponseDto(UUID.randomUUID(), "Setor G", "General", new BigDecimal("180.00"), 400)));

        mockMvc.perform(get("/events/" + idEvento + "/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nombreTribuna").value("Setor G"))
                .andExpect(jsonPath("$.data[0].stockDisponible").value(400));
    }

    @Test
    void getTicketsByEvent_EventoInexistente_Devuelve404() throws Exception {
        UUID idEvento = UUID.randomUUID();
        when(ticketService.listarPorEvento(idEvento)).thenThrow(new ResourceNotFoundException("Evento", "id", idEvento));

        mockMvc.perform(get("/events/" + idEvento + "/tickets"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getTicketsByEvent_IdInvalido_Devuelve400() throws Exception {
        mockMvc.perform(get("/events/no-es-un-uuid/tickets"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ticketService);
    }
}
