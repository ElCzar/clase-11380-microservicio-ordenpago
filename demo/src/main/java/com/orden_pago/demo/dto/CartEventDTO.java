package com.orden_pago.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para carrito de compras
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartEventDTO {
    private String eventType; // ITEM_ADDED, ITEM_REMOVED, ITEM_UPDATED, CART_CLEARED
    private UUID cartId;
    private String username;
    private UUID serviceId;
    private String serviceName;
    private String serviceCategory;
    private BigDecimal servicePrice;
    private Integer quantity;
    private BigDecimal amount;
    private LocalDateTime timestamp;

    public CartEventDTO(String eventType, UUID cartId, String username, UUID serviceId,
            Integer quantity, BigDecimal amount, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.cartId = cartId;
        this.username = username;
        this.serviceId = serviceId;
        this.quantity = quantity;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public static CartEventDTO itemAdded(UUID cartId, String username, UUID serviceId, Integer quantity,
            BigDecimal amount) {
        return new CartEventDTO("ITEM_ADDED", cartId, username, serviceId, quantity, amount, LocalDateTime.now());
    }

    public static CartEventDTO itemAdded(UUID cartId, String username, UUID serviceId, String serviceName,
            String serviceCategory, BigDecimal servicePrice, Integer quantity, BigDecimal amount) {
        return new CartEventDTO("ITEM_ADDED", cartId, username, serviceId, serviceName, serviceCategory,
                servicePrice, quantity, amount, LocalDateTime.now());
    }

    public static CartEventDTO itemRemoved(UUID cartId, String username, UUID serviceId) {
        return new CartEventDTO("ITEM_REMOVED", cartId, username, serviceId, null, null, LocalDateTime.now());
    }

    public static CartEventDTO itemRemoved(UUID cartId, String username, UUID serviceId, String serviceName) {
        CartEventDTO event = new CartEventDTO("ITEM_REMOVED", cartId, username, serviceId, null, null,
                LocalDateTime.now());
        event.setServiceName(serviceName);
        return event;
    }

    public static CartEventDTO itemUpdated(UUID cartId, String username, UUID serviceId, Integer quantity,
            BigDecimal amount) {
        return new CartEventDTO("ITEM_UPDATED", cartId, username, serviceId, quantity, amount, LocalDateTime.now());
    }

    public static CartEventDTO itemUpdated(UUID cartId, String username, UUID serviceId, String serviceName,
            Integer quantity, BigDecimal amount) {
        CartEventDTO event = new CartEventDTO("ITEM_UPDATED", cartId, username, serviceId, quantity, amount,
                LocalDateTime.now());
        event.setServiceName(serviceName);
        return event;
    }

    public static CartEventDTO cartCleared(UUID cartId, String username) {
        return new CartEventDTO("CART_CLEARED", cartId, username, null, null, null, LocalDateTime.now());
    }
}