package com.uade.grandprixtracker.payment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MetodoPagoRequestDto(
        @NotBlank(message = "el tipo es obligatorio")
        @Pattern(regexp = "Credito|Debito", message = "debe ser Credito o Debito")
        String tipo,

        @NotBlank(message = "los últimos 4 dígitos son obligatorios")
        @Pattern(regexp = "\\d{4}", message = "debe tener exactamente 4 dígitos")
        @JsonProperty("ultimos4Digitos")
        @JsonAlias({"ultimos_4_digitos", "ultimos4_digitos"})
        String ultimos4Digitos,

        @NotBlank(message = "la fecha de expiración es obligatoria")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "debe tener formato MM/AA")
        @JsonProperty("fechaExpiracion")
        @JsonAlias("fecha_expiracion")
        String fechaExpiracion,

        @JsonProperty("proveedorToken")
        @JsonAlias("proveedor_token")
        String proveedorToken,

        @JsonProperty("nombre_titular")
        @JsonAlias("nombreTitular")
        String nombreTitular
) {
}
