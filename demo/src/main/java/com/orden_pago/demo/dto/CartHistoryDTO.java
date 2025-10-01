package com.orden_pago.demo.dto;

import com.orden_pago.demo.enums.CartStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartHistoryDTO {
    private UUID id;
    private CartStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalAmount;
    private int totalItems;

    @Data
    @Builder
    public static class CartItemSummary {
        private UUID id;
        private UUID serviceId;
        private String serviceName;
        private String serviceCategory;
        private BigDecimal servicePrice;
        private Integer quantity;
        private BigDecimal subtotal;
    }

    private List<CartItemSummary> items;
}