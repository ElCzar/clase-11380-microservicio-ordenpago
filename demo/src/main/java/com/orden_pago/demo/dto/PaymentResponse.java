package com.orden_pago.demo.dto;

import com.orden_pago.demo.enums.PaymentStatus;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String message;
    private LocalDateTime processedAt;
}