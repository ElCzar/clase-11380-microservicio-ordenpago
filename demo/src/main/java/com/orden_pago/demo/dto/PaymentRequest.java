package com.orden_pago.demo.dto;

import com.orden_pago.demo.enums.PaymentMethod;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Data
public class PaymentRequest {
    // Datos de la tarjeta

    // Cart ID será configurado automáticamente por el controlador de checkout
    private UUID cartId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;

    private String expiryMonth;
    private String expiryYear;
    private String cvv;
}