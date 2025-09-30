package com.orden_pago.demo.service;

import com.orden_pago.demo.dto.PaymentEventDTO;
import com.orden_pago.demo.dto.PaymentRequest;
import com.orden_pago.demo.dto.PaymentResponse;
import com.orden_pago.demo.dto.PaymentResult;
import com.orden_pago.demo.enums.PaymentStatus;
import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.Payment;
import com.orden_pago.demo.repository.CartRepository;
import com.orden_pago.demo.repository.PaymentRepository;
import com.orden_pago.demo.service.kafka.KafkaMessagingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final PaymentSimulationService paymentSimulationService;
    private final KafkaMessagingService kafkaMessagingService;

    /**
     * Procesa un pago simulado
     */
    public PaymentResponse processPayment(PaymentRequest request, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Procesando pago para usuario: {}, carrito: {}", userId, request.getCartId());

        // Validar que el carrito existe y pertenece al usuario
        Cart cart = validateCartForPayment(request.getCartId(), userId);

        // Verificar que no existe un pago previo para este carrito
        if (paymentRepository.existsByCart(cart)) {
            throw new RuntimeException("Ya existe un pago para este carrito");
        }

        // Verificar que el carrito no esté vacío
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("No se puede procesar pago de un carrito vacío");
        }

        // Validar datos de la tarjeta
        if (!paymentSimulationService.validateCardData(request)) {
            return PaymentResponse.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Datos de tarjeta inválidos")
                    .processedAt(LocalDateTime.now())
                    .build();
        }

        // Crear registro de pago
        Payment payment = createPaymentRecord(request, cart, userId);

        // Publicar evento de pago iniciado
        kafkaMessagingService.publishPaymentEvent(
                PaymentEventDTO.paymentInitiated(payment.getId(), cart.getId(), userId, payment.getAmount()));

        try {
            // Simular procesamiento de pago
            PaymentResult result = paymentSimulationService.simulatePayment(request);

            // Actualizar payment con el resultado
            updatePaymentWithResult(payment, result);

            // Si el pago fue exitoso, marcar carrito como completado
            if (result.getStatus() == PaymentStatus.COMPLETED) {
                cartService.completeCart(cart);

                // Publicar evento de pago exitoso
                kafkaMessagingService.publishPaymentEvent(
                        PaymentEventDTO.paymentSuccess(payment.getId(), cart.getId(), userId,
                                payment.getAmount(), payment.getCardNumber()));

                log.info("Pago procesado exitosamente con transacción: {}", result.getTransactionId());
            } else {
                // Publicar evento de pago fallido
                kafkaMessagingService.publishPaymentEvent(
                        PaymentEventDTO.paymentFailed(payment.getId(), cart.getId(), userId,
                                payment.getAmount(), result.getMessage()));
            }

            return PaymentResponse.builder()
                    .transactionId(result.getTransactionId())
                    .status(result.getStatus())
                    .amount(payment.getAmount())
                    .message(result.getMessage())
                    .processedAt(result.getProcessedAt())
                    .build();

        } catch (Exception e) {
            log.error("Error procesando pago: {}", e.getMessage());
            payment.failPayment();
            paymentRepository.save(payment);

            // Publicar evento de pago fallido
            kafkaMessagingService.publishPaymentEvent(
                    PaymentEventDTO.paymentFailed(payment.getId(), cart.getId(), userId,
                            payment.getAmount(), "Error interno procesando el pago"));

            return PaymentResponse.builder()
                    .status(PaymentStatus.FAILED)
                    .message("Error interno procesando el pago")
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Obtiene el historial de pagos del usuario
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentHistory(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Obteniendo historial de pagos para usuario: {}", userId);

        return paymentRepository.findByUserIdOrderByProcessedAtDesc(userId);
    }

    /**
     * Obtiene un pago por transaction ID
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    /**
     * Valida que el carrito existe y pertenece al usuario
     */
    private Cart validateCartForPayment(UUID cartId, String userId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado: " + cartId));

        if (!cart.getUserId().equals(userId)) {
            throw new RuntimeException("El carrito no pertenece al usuario autenticado");
        }

        return cart;
    }

    /**
     * Crea el registro inicial de pago
     */
    private Payment createPaymentRecord(PaymentRequest request, Cart cart, String userId) {
        Payment payment = new Payment();
        payment.setCart(cart);
        payment.setAmount(cart.getTotalAmount());
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setUserId(userId);
        payment.setCardHolderName(request.getCardHolderName());
        payment.maskCardNumber(request.getCardNumber());
        payment.setProcessedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    /**
     * Actualiza el pago con el resultado de la simulación
     */
    private void updatePaymentWithResult(Payment payment, PaymentResult result) {
        payment.setStatus(result.getStatus());
        payment.setTransactionId(result.getTransactionId());
        payment.setProcessedAt(result.getProcessedAt());

        paymentRepository.save(payment);
    }

    /**
     * Extrae el user ID del token JWT
     */
    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("sub");
        }
        throw new RuntimeException("No se pudo obtener el user ID del token");
    }

}