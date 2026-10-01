package com.uade.grandprixtracker.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public record CircuitoResponseDto(
        UUID idCircuito,
        String nombre,
        BigDecimal longitudKm,
        Integer curvas,
        Integer vueltas,
        String mapaSvgUrl,
        CiudadResponseDto ciudad,
        @JsonProperty("record") String record,
        @JsonProperty("velocidad_maxima") String velocidad_maxima,
        @JsonProperty("maximo_ganador") String maximo_ganador,
        @JsonProperty("circuit_svg_url") String circuit_svg_url,
        @JsonProperty("capacidad") String capacidad
) {
    public CircuitoResponseDto(
            UUID idCircuito,
            String nombre,
            BigDecimal longitudKm,
            Integer curvas,
            Integer vueltas,
            String mapaSvgUrl,
            CiudadResponseDto ciudad
    ) {
        this(idCircuito, nombre, longitudKm, curvas, vueltas, mapaSvgUrl, ciudad, null, null, null, null, null);
    }
}

