package com.orden_pago.demo.controller;

import com.orden_pago.demo.dto.PaymentRequest;
import com.orden_pago.demo.dto.PaymentResponse;
import com.orden_pago.demo.model.Payment;
import com.orden_pago.demo.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payment/simulate - Simular pago
     */
    @PostMapping("/simulate")
    public ResponseEntity<PaymentResponse> simulatePayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            log.info("Procesando solicitud de pago para carrito: {}", request.getCartId());

            PaymentResponse response = paymentService.processPayment(request, authentication);

            // Retornar status apropiado basado en el resultado
            HttpStatus status = switch (response.getStatus()) {
                case COMPLETED -> HttpStatus.OK;
                case FAILED -> HttpStatus.BAD_REQUEST;
                case PENDING -> HttpStatus.ACCEPTED;
            };

            return ResponseEntity.status(status).body(response);

        } catch (RuntimeException e) {
            log.error("Error procesando pago: {}", e.getMessage());

            PaymentResponse errorResponse = PaymentResponse.builder()
                    .status(com.orden_pago.demo.enums.PaymentStatus.FAILED)
                    .message(e.getMessage())
                    .processedAt(java.time.LocalDateTime.now())
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error interno procesando pago: {}", e.getMessage());

            PaymentResponse errorResponse = PaymentResponse.builder()
                    .status(com.orden_pago.demo.enums.PaymentStatus.FAILED)
                    .message("Error interno del servidor")
                    .processedAt(java.time.LocalDateTime.now())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /api/payment/history - Historial de pagos
     */
    @GetMapping("/history")
    public ResponseEntity<List<Payment>> getPaymentHistory(Authentication authentication) {
        try {
            log.info("Obteniendo historial de pagos");

            List<Payment> paymentHistory = paymentService.getPaymentHistory(authentication);
            return ResponseEntity.ok(paymentHistory);

        } catch (Exception e) {
            log.error("Error obteniendo historial de pagos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/payment/transaction/{transactionId} - Obtener pago por
     * transaction ID
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<Payment> getPaymentByTransactionId(@PathVariable String transactionId) {
        try {
            log.info("Obteniendo pago por transaction ID: {}", transactionId);

            Optional<Payment> payment = paymentService.getPaymentByTransactionId(transactionId);

            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error obteniendo pago por transaction ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}