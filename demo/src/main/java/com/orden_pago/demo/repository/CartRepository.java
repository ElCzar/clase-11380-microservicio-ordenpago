package com.orden_pago.demo.repository;

import com.orden_pago.demo.enums.CartStatus;
import com.orden_pago.demo.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);
    List<Cart> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Cart> findByStatus(CartStatus status);
    boolean existsByUserIdAndStatus(String userId, CartStatus status);

}