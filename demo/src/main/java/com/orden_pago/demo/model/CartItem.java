package com.orden_pago.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart; // Carrito asociado

    @Column(name = "service_id", nullable = false)
    private UUID serviceId; // Referencia al servicio del microservicio de marketplace

    @Column(name = "service_name", nullable = false)
    private String serviceName; // Nombre del servicio

    @Column(name = "service_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal servicePrice; // Precio del servicio

    @Column(nullable = false)
    private Integer quantity; // Cantidad del servicio

    @CreationTimestamp
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    // Calcula el subtotal
    public BigDecimal getSubtotal() {
        if (servicePrice == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return servicePrice.multiply(BigDecimal.valueOf(quantity));
    }

    // Actualiza la cantidad
    public void updateQuantity(Integer newQuantity) {
        this.quantity = newQuantity;
    }
}