package com.uade.grandprixtracker.flight.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VueloResponseDto(
        UUID idVuelo,
        String aerolinea,
        Sentido sentido,
        CiudadResumen origen,
        CiudadResumen destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaLlegada,
        BigDecimal precioUsd,
        Integer stockAsientos
) {

    public enum Sentido { IDA, VUELTA }

    public record CiudadResumen(UUID idCiudad, String nombre) {}
}
