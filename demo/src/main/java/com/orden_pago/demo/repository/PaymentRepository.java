package com.orden_pago.demo.repository;

import com.orden_pago.demo.enums.PaymentStatus;
import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByCart(Cart cart);
    List<Payment> findByUserIdOrderByProcessedAtDesc(String userId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
    boolean existsByCart(Cart cart);

}