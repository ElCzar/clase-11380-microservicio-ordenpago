package com.orden_pago.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para recibir respuestas de servicios vía Kafka desde el microservicio
 * marketplace
 * Estructura simple que coincide exactamente con el formato del marketplace
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponseDTO {

    // Campos para correlación de mensajes
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("averageRating")
    private Double averageRating;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("countryName")
    private String countryName;

    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("primaryImageUrl")
    private String primaryImageUrl;

    // Métodos de conveniencia para compatibilidad con código existente
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

    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Valida que todos los campos requeridos para el carrito estén presentes
     */
    public boolean isValidForCart() {
        return id != null &&
                title != null && !title.trim().isEmpty() &&
                price != null &&
                price.compareTo(BigDecimal.ZERO) >= 0 &&
                categoryName != null && !categoryName.trim().isEmpty();
    }

    /**
     * Obtiene una descripción segura (nunca null)
     */
    public String getSafeDescription() {
        return description != null ? description : "Sin descripción disponible";
    }

    /**
     * Obtiene un rating seguro (nunca null)
     */
    public Double getSafeAverageRating() {
        return averageRating != null ? averageRating : 0.0;
    }

    /**
     * Obtiene una URL de imagen segura (nunca null)
     */
    public String getSafePrimaryImageUrl() {
        return primaryImageUrl != null ? primaryImageUrl : "https://via.placeholder.com/300x200?text=Sin+Imagen";
    }
}