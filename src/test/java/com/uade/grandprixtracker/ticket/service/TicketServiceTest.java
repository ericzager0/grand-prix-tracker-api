package com.uade.grandprixtracker.ticket.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import com.uade.grandprixtracker.ticket.dto.EntradaResponseDto;
import com.uade.grandprixtracker.ticket.model.EntradaGrada;
import com.uade.grandprixtracker.ticket.repository.EntradaGradaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private EntradaGradaRepository entradaGradaRepository;

    @Mock
    private EventoF1Repository eventoF1Repository;

    @InjectMocks
    private TicketService ticketService;

    private UUID idEvento;
    private EntradaGrada entrada;

    @BeforeEach
    void setUp() {
        idEvento = UUID.randomUUID();
        entrada = new EntradaGrada(UUID.randomUUID(), idEvento, "Tribuna A", new BigDecimal("350.00"), 10, "General");
    }

    @Test
    void listarPorEvento_DevuelveEntradasDelEvento() {
        when(eventoF1Repository.existsById(idEvento)).thenReturn(true);
        when(entradaGradaRepository.findByIdEventoOrderByPrecioUsdAsc(idEvento)).thenReturn(List.of(entrada));

        List<EntradaResponseDto> entradas = ticketService.listarPorEvento(idEvento);

        assertEquals(1, entradas.size());
        assertEquals("Tribuna A", entradas.getFirst().nombreTribuna());
        assertEquals(10, entradas.getFirst().stockDisponible());
    }

    @Test
    void listarPorEvento_EventoInexistente_LanzaNotFound() {
        when(eventoF1Repository.existsById(idEvento)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> ticketService.listarPorEvento(idEvento));
        verifyNoInteractions(entradaGradaRepository);
    }

    @Test
    void reservar_ConStock_DescuentaYDevuelveEntrada() {
        when(entradaGradaRepository.findById(entrada.getIdEntrada())).thenReturn(Optional.of(entrada));
        when(entradaGradaRepository.descontarStock(entrada.getIdEntrada(), 2)).thenReturn(1);

        assertSame(entrada, ticketService.reservar(idEvento, entrada.getIdEntrada(), 2));
    }

    @Test
    void reservar_EntradaDeOtroEvento_LanzaIllegalArgumentSinDescontar() {
        when(entradaGradaRepository.findById(entrada.getIdEntrada())).thenReturn(Optional.of(entrada));

        assertThrows(IllegalArgumentException.class,
                () -> ticketService.reservar(UUID.randomUUID(), entrada.getIdEntrada(), 1));
        verify(entradaGradaRepository, never()).descontarStock(any(), anyInt());
    }

    @Test
    void reservar_SinStock_LanzaStockInsuficiente() {
        when(entradaGradaRepository.findById(entrada.getIdEntrada())).thenReturn(Optional.of(entrada));
        when(entradaGradaRepository.descontarStock(entrada.getIdEntrada(), 11)).thenReturn(0);

        assertThrows(StockInsuficienteException.class,
                () -> ticketService.reservar(idEvento, entrada.getIdEntrada(), 11));
    }

    @Test
    void reservar_EntradaInexistente_LanzaNotFound() {
        UUID idInexistente = UUID.randomUUID();
        when(entradaGradaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.reservar(idEvento, idInexistente, 1));
    }
}
