package com.uade.grandprixtracker.flight.controller;

import com.uade.grandprixtracker.flight.dto.VueloResponseDto;
import com.uade.grandprixtracker.flight.service.FlightService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping("/events/{idEvento}/flights")
    public ResponseEntity<ApiResponse<List<VueloResponseDto>>> getFlightsByEvent(@PathVariable UUID idEvento) {
        List<VueloResponseDto> vuelos = flightService.listarPorEvento(idEvento);
        return ResponseEntity.ok(ApiResponse.success("Vuelos obtenidos correctamente", vuelos));
    }
}
