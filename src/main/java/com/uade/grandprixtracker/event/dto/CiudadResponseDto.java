package com.uade.grandprixtracker.event.dto;

import java.util.UUID;

public record CiudadResponseDto(
        UUID idCiudad,
        String nombre,
        PaisResponseDto pais
) {}

