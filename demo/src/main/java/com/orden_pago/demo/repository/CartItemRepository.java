package com.orden_pago.demo.repository;

import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartOrderByAddedAtAsc(Cart cart);
    Optional<CartItem> findByCartAndServiceId(Cart cart, UUID serviceId);
    boolean existsByCartAndServiceId(Cart cart, UUID serviceId);
    List<CartItem> findByServiceId(UUID serviceId);
    void deleteByCart(Cart cart);
}