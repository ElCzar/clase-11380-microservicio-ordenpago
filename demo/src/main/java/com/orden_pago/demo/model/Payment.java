package com.orden_pago.demo.model;

import com.orden_pago.demo.enums.PaymentMethod;
import com.orden_pago.demo.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id; 

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart; // Carrito asociado

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // Monto del pago

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method; // Método de pago 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id")
    private String transactionId; // ID de la transacción del proveedor de pago

    @CreationTimestamp
    @Column(name = "processed_at")
    private LocalDateTime processedAt; // Fecha y hora del procesamiento

    @Column(name = "card_number")
    private String cardNumber; // numero de la tarjeta de crédito

    @Column(name = "card_holder_name")
    private String cardHolderName; // nombre del titular de la tarjeta

    @Column(name = "user_id")
    private String userId; // user id del JWT token

    // Simula el número de tarjeta
    public void maskCardNumber(String originalCardNumber) {
        if (originalCardNumber != null && originalCardNumber.length() >= 4) {
            String lastFour = originalCardNumber.substring(originalCardNumber.length() - 4);
            this.cardNumber = "**** **** **** " + lastFour;
        }
    }

    // Establece el pago como completado
    public void completePayment(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.transactionId = transactionId;
        this.processedAt = LocalDateTime.now();
    }

    // Establece el pago como fallido
    public void failPayment() {
        this.status = PaymentStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }
}