package com.orden_pago.demo.dto;

import com.orden_pago.demo.enums.PaymentStatus;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResult {
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime processedAt;
    private String message;
}