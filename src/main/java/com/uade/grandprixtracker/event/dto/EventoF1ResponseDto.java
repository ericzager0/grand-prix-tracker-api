package com.uade.grandprixtracker.event.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EventoF1ResponseDto(
        UUID idEvento,
        Integer temporada,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado,
        CircuitoResponseDto circuito
) {}

