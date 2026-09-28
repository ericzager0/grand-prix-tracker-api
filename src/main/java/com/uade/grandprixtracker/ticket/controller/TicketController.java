package com.uade.grandprixtracker.ticket.controller;

import com.uade.grandprixtracker.shared.response.ApiResponse;
import com.uade.grandprixtracker.ticket.dto.EntradaResponseDto;
import com.uade.grandprixtracker.ticket.service.TicketService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/events/{idEvento}/tickets")
    public ResponseEntity<ApiResponse<List<EntradaResponseDto>>> getTicketsByEvent(@PathVariable UUID idEvento) {
        List<EntradaResponseDto> entradas = ticketService.listarPorEvento(idEvento);
        return ResponseEntity.ok(ApiResponse.success("Entradas obtenidas correctamente", entradas));
    }
}
