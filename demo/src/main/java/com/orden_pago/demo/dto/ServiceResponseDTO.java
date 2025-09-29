package com.orden_pago.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO SIMPLIFICADO para recibir respuestas de servicios vía Kafka
 * Contiene solo los campos esenciales que realmente usamos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDTO {
    // Campos para correlación de mensajes
    private String requestId;
    private String errorMessage;

    private UUID id;
    private String title;
    private String description;
    private BigDecimal price;
    private Double averageRating;

    private String categoryName;
    private Boolean isActive;
    private String countryName;
    private String countryCode;
    private String primaryImageUrl;

    public Boolean isAvailable() {
        return isActive != null && isActive;
    }

    public UUID getServiceId() {
        return id;
    }

    public String getName() {
        return title;
    }

    public Boolean getAvailable() {
        return isAvailable();
    }

    public String getPrimaryImageUrl() {
        return primaryImageUrl;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getCountryName() {
        return countryName;
    }
}