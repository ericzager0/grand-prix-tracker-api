package com.uade.grandprixtracker.payment.controller;

import com.uade.grandprixtracker.payment.dto.MetodoPagoResponseDto;
import com.uade.grandprixtracker.payment.service.PaymentService;
import com.uade.grandprixtracker.shared.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-methods")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Temporal hasta integrar Supabase Auth: el id del cliente pasa a salir del claim "sub" del JWT.
    @GetMapping
    public ResponseEntity<ApiResponse<List<MetodoPagoResponseDto>>> getPaymentMethods(
            @RequestHeader("X-Cliente-Id") UUID idCliente) {
        List<MetodoPagoResponseDto> metodos = paymentService.listarPorCliente(idCliente);
        return ResponseEntity.ok(ApiResponse.success("Métodos de pago obtenidos correctamente", metodos));
    }
}
