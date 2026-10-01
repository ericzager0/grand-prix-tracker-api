package com.uade.grandprixtracker.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public record MetodoPagoResponseDto(
        UUID idMetodoPago,
        String tipo,
        String ultimos4Digitos,
        String fechaExpiracion,
        boolean vencida,
        @JsonProperty("nombre_titular")
        String nombre_titular,
        @JsonProperty("telefono")
        String telefono,
        @JsonProperty("dni")
        BigDecimal dni
) {
    public MetodoPagoResponseDto(UUID idMetodoPago, String tipo, String ultimos4Digitos, String fechaExpiracion, boolean vencida) {
        this(idMetodoPago, tipo, ultimos4Digitos, fechaExpiracion, vencida, null, null, null);
    }

    public MetodoPagoResponseDto(UUID idMetodoPago, String tipo, String ultimos4Digitos, String fechaExpiracion, boolean vencida, String nombre_titular) {
        this(idMetodoPago, tipo, ultimos4Digitos, fechaExpiracion, vencida, nombre_titular, null, null);
    }

    public MetodoPagoResponseDto(UUID idMetodoPago, String tipo, String ultimos4Digitos, String fechaExpiracion, boolean vencida, String nombre_titular, String telefono) {
        this(idMetodoPago, tipo, ultimos4Digitos, fechaExpiracion, vencida, nombre_titular, telefono, null);
    }
}
