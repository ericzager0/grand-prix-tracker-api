package com.uade.grandprixtracker.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record UpdateUserProfileRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @JsonProperty("nombre")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @JsonProperty("apellido")
        String apellido,

        @JsonProperty("telefono")
        String telefono,

        @JsonProperty("dni")
        BigDecimal dni,

        @JsonProperty("color")
        String color
) {}
