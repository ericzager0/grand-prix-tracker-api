package com.uade.grandprixtracker.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public record UserProfileResponseDto(
        UUID idCliente,
        String nombre,
        String apellido,
        String email,
        String telefono,
        BigDecimal dni
) {
    @JsonProperty("id_cliente")
    public UUID id_cliente() {
        return idCliente;
    }
}
