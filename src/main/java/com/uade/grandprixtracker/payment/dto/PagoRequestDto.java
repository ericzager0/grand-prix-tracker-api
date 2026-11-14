package com.uade.grandprixtracker.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Se envía {@code idMetodoPago} para pagar con una tarjeta guardada, o los datos de una tarjeta nueva.
 */
public record PagoRequestDto(
        UUID idMetodoPago,
        @Pattern(regexp = "Credito|Debito", message = "debe ser Credito o Debito")
        String tipo,
        @Pattern(regexp = "\\d{4}", message = "debe tener exactamente 4 dígitos")
        String ultimos4Digitos,
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "debe tener formato MM/AA")
        String fechaExpiracion,
        @Size(max = 255, message = "no puede superar los 255 caracteres")
        String proveedorToken,
        @Size(max = 100, message = "no puede superar los 100 caracteres")
        @JsonProperty("nombre_titular")
        String nombreTitular
) {
    public PagoRequestDto(UUID idMetodoPago, String tipo, String ultimos4Digitos, String fechaExpiracion, String proveedorToken) {
        this(idMetodoPago, tipo, ultimos4Digitos, fechaExpiracion, proveedorToken, null);
    }
}
