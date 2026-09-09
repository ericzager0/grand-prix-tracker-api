package com.uade.grandprixtracker.event.dto;

import java.util.UUID;

public record PaisResponseDto(
        UUID idPais,
        String nombre,
        String codigoIso,
        String continente
) {}

