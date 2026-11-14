package com.uade.grandprixtracker.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateUserProfileRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        @JsonProperty("nombre")
        String nombre,

        // Opcional: el front arma nombre y apellido partiendo el nombre completo, y con una sola palabra
        // el apellido llega vacío. Se guarda como "" (la columna es NOT NULL).
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        @JsonProperty("apellido")
        String apellido,

        // Formato del front: código de país + número, por ejemplo "+54 11 1234-5678".
        @Size(max = 25, message = "El teléfono no puede superar los 25 caracteres")
        @Pattern(regexp = "^$|^\\+?[0-9][0-9 ()\\-]{5,24}$", message = "El teléfono solo puede tener dígitos, espacios, guiones, paréntesis y + inicial")
        @JsonProperty("telefono")
        String telefono,

        @Positive(message = "El DNI debe ser un número positivo")
        @Digits(integer = 9, fraction = 0, message = "El DNI debe ser un número entero de hasta 9 dígitos")
        @JsonProperty("dni")
        BigDecimal dni,

        // Color hexadecimal "#RRGGBB" (los del front salen de utils/tireColors.ts).
        @Pattern(regexp = "^$|^\\s*#[0-9A-Fa-f]{6}\\s*$", message = "El color debe tener formato #RRGGBB")
        @JsonProperty("color")
        String color
) {}
