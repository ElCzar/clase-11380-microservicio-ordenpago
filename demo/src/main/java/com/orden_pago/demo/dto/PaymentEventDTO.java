package com.orden_pago.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDTO {
    private String eventType; // PAYMENT_INITIATED, PAYMENT_SUCCESS, PAYMENT_FAILED
    private UUID paymentId;
    private UUID cartId;
    private String username;
    private BigDecimal amount;
    private String cardNumber; 
    private String status;
    private String errorMessage;
    private LocalDateTime timestamp;

    public static PaymentEventDTO paymentInitiated(UUID paymentId, UUID cartId, String username, BigDecimal amount) {
        return new PaymentEventDTO("PAYMENT_INITIATED", paymentId, cartId, username, amount, null, "PENDING", null,
                LocalDateTime.now());
    }

    public static PaymentEventDTO paymentSuccess(UUID paymentId, UUID cartId, String username, BigDecimal amount,
            String maskedCardNumber) {
        return new PaymentEventDTO("PAYMENT_SUCCESS", paymentId, cartId, username, amount, maskedCardNumber, "SUCCESS",
                null, LocalDateTime.now());
    }

    public static PaymentEventDTO paymentFailed(UUID paymentId, UUID cartId, String username, BigDecimal amount,
            String errorMessage) {
        return new PaymentEventDTO("PAYMENT_FAILED", paymentId, cartId, username, amount, null, "FAILED", errorMessage,
                LocalDateTime.now());
    }
}