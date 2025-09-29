package com.orden_pago.demo.service;

import com.orden_pago.demo.dto.PaymentRequest;
import com.orden_pago.demo.dto.PaymentResult;
import com.orden_pago.demo.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
public class PaymentSimulationService {

    private final Random random = new Random();

    @Value("${payment.simulation.success-rate:0.85}")
    private double successRate;

    @Value("${payment.simulation.min-delay:2000}")
    private int minDelay;

    @Value("${payment.simulation.max-delay:5000}")
    private int maxDelay;

    /**
     * Simula un procesamiento de pago
     */
    public PaymentResult simulatePayment(PaymentRequest request) {
        log.info("Iniciando simulación de pago para carrito: {}", request.getCartId());

        // Simula la latencia de procesamiento
        simulateProcessingDelay();

        // Simula diferentes escenarios de pago
        boolean isSuccessful = random.nextDouble() < successRate;

        if (isSuccessful) {
            String transactionId = generateTransactionId();
            log.info("Pago simulado exitosamente con ID de transacción: {}", transactionId);

            return PaymentResult.builder()
                    .status(PaymentStatus.COMPLETED)
                    .transactionId(transactionId)
                    .processedAt(LocalDateTime.now())
                    .message("Pago procesado exitosamente")
                    .build();
        } else {
            String failureReason = getRandomFailureReason();
            log.warn("Pago simulado falló: {}", failureReason);

            return PaymentResult.builder()
                    .status(PaymentStatus.FAILED)
                    .processedAt(LocalDateTime.now())
                    .message(failureReason)
                    .build();
        }
    }

    /**
     * Simula la latencia de procesamiento del pago
     */
    private void simulateProcessingDelay() {
        try {
            int delay = minDelay + random.nextInt(maxDelay - minDelay);
            log.debug("Simulando delay de procesamiento: {} ms", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulación de delay interrumpida");
        }
    }

    /**
     * Genera un ID de transacción único para el pago simulado
     */
    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
    }

    /**
     * Obtiene una razón aleatoria de falla de pago
     */
    private String getRandomFailureReason() {
        String[] reasons = {
                "Fondos insuficientes",
                "Tarjeta expirada",
                "Error de red del banco",
                "Transacción rechazada por el emisor",
                "Límite de transacción excedido",
                "Tarjeta bloqueada temporalmente",
                "Error en la validación de datos",
                "Servicio bancario no disponible"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    /**
     * Simula validación de datos de tarjeta
     */
    public boolean validateCardData(PaymentRequest request) {
        // Simulación básica de validación
        if (request.getCardNumber() == null || request.getCardNumber().length() < 13) {
            return false;
        }

        if (request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
            return false;
        }

        // Simula 95% de éxito en validación
        return random.nextDouble() < 0.95;
    }
}