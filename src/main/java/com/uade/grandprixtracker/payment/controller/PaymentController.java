package com.uade.grandprixtracker.payment.controller;

import com.uade.grandprixtracker.payment.dto.MetodoPagoRequestDto;
import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/payment-methods", "/users/me/payment-methods"})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // El claim "sub" del JWT de Supabase es el id del usuario, que coincide con clientes.id_cliente.
    @GetMapping
    public ResponseEntity<ApiResponse<List<MetodoPagoResponseDto>>> getPaymentMethods(
            @AuthenticationPrincipal Jwt jwt) {
        List<MetodoPagoResponseDto> metodos = paymentService.listarPorCliente(idCliente(jwt));
        return ResponseEntity.ok(ApiResponse.success("Métodos de pago obtenidos correctamente", metodos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MetodoPagoResponseDto>> addPaymentMethod(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MetodoPagoRequestDto request) {
        MetodoPagoResponseDto metodo = paymentService.agregar(idCliente(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Método de pago agregado correctamente", metodo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MetodoPagoResponseDto>> updatePaymentMethod(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody MetodoPagoRequestDto request) {
        MetodoPagoResponseDto metodo = paymentService.actualizar(idCliente(jwt), id, request);
        return ResponseEntity.ok(ApiResponse.success("Método de pago actualizado correctamente", metodo));
    }

    private static UUID idCliente(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
