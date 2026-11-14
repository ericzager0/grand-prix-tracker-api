package com.uade.grandprixtracker.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record UpdateUserProfileRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @JsonProperty("nombre")
        String nombre,

        // Opcional: el front arma nombre y apellido partiendo el nombre completo, y con una sola palabra
        // el apellido llega vacío. Se guarda como "" (la columna es NOT NULL).
        @JsonProperty("apellido")
        String apellido,

        @JsonProperty("telefono")
        String telefono,

        @JsonProperty("dni")
        BigDecimal dni,

        @JsonProperty("color")
        String color
) {}
