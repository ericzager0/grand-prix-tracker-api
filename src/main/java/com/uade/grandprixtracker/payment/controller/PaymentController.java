package com.uade.grandprixtracker.payment.controller;

import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-methods")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // El claim "sub" del JWT de Supabase es el id del usuario, que coincide con clientes.id_cliente.
    @GetMapping
    public ResponseEntity<ApiResponse<List<MetodoPagoResponseDto>>> getPaymentMethods(
            @AuthenticationPrincipal Jwt jwt) {
        List<MetodoPagoResponseDto> metodos = paymentService.listarPorCliente(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok(ApiResponse.success("Métodos de pago obtenidos correctamente", metodos));
    }
}
