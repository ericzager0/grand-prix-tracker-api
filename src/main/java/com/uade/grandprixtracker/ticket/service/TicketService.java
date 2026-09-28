package com.uade.grandprixtracker.ticket.service;

import com.uade.grandprixtracker.event.repository.EventoF1Repository;
import com.uade.grandprixtracker.shared.exception.ResourceNotFoundException;
import com.uade.grandprixtracker.shared.exception.StockInsuficienteException;
import com.uade.grandprixtracker.ticket.dto.EntradaResponseDto;
import com.uade.grandprixtracker.ticket.model.EntradaGrada;
import com.uade.grandprixtracker.ticket.repository.EntradaGradaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final EntradaGradaRepository entradaGradaRepository;
    private final EventoF1Repository eventoF1Repository;

    public TicketService(EntradaGradaRepository entradaGradaRepository, EventoF1Repository eventoF1Repository) {
        this.entradaGradaRepository = entradaGradaRepository;
        this.eventoF1Repository = eventoF1Repository;
    }

    @Transactional(readOnly = true)
    public List<EntradaResponseDto> listarPorEvento(UUID idEvento) {
        if (!eventoF1Repository.existsById(idEvento)) {
            throw new ResourceNotFoundException("Evento", "id", idEvento);
        }

        return entradaGradaRepository.findByIdEventoOrderByPrecioUsdAsc(idEvento)
                .stream()
                .map(e -> new EntradaResponseDto(e.getIdEntrada(), e.getNombreTribuna(), e.getTipo(), e.getPrecioUsd(), e.getStockDisponible()))
                .toList();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public EntradaGrada reservar(UUID idEvento, UUID idEntrada, int cantidad) {
        EntradaGrada entrada = entradaGradaRepository.findById(idEntrada)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada", "id", idEntrada));

        if (!entrada.getIdEvento().equals(idEvento)) {
            throw new IllegalArgumentException("La entrada " + idEntrada + " no pertenece al evento seleccionado");
        }

        if (entradaGradaRepository.descontarStock(idEntrada, cantidad) == 0) {
            throw new StockInsuficienteException("No hay stock suficiente para la tribuna " + entrada.getNombreTribuna());
        }

        return entrada;
    }
}
