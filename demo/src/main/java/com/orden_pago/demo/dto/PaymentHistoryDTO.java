package com.orden_pago.demo.dto;

import com.orden_pago.demo.enums.PaymentMethod;
import com.orden_pago.demo.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentHistoryDTO {
    private UUID id;
    private UUID cartId;
    private String transactionId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String cardHolderName;
    private String maskedCardNumber;
    private LocalDateTime processedAt;
    private String message;

    private int itemCount;
    private LocalDateTime cartCreatedAt;
}