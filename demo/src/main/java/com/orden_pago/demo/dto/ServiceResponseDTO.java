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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponseDTO {

    // Campos para correlación de mensajes (internos del sistema)
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("errorMessage")
    private String errorMessage;

    // Campos principales del servicio que vienen de Kafka del marketplace
    @JsonProperty("serviceId")
    private String serviceId;

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

    // Campos del evento Kafka
    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("userId")
    private String userId;

    // IDs de entidades relacionadas
    @JsonProperty("categoryId")
    private UUID categoryId;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("statusId")
    private UUID statusId;

    @JsonProperty("statusName")
    private String statusName;

    @JsonProperty("countryId")
    private UUID countryId;

    @JsonProperty("countryName")
    private String countryName;

    // Campos adicionales que pueden venir del marketplace
    @JsonProperty("countryCode")
    private String countryCode;

    @JsonProperty("primaryImageUrl")
    private String primaryImageUrl;

    @JsonProperty("isActive")
    private Boolean isActive;

    // Métodos de conveniencia para compatibilidad con código existente
    public Boolean isAvailable() {
        return isActive != null && isActive;
    }

    public UUID getServiceId() {
        // Priorizar serviceId (String) sobre id (UUID)
        if (serviceId != null) {
            try {
                return UUID.fromString(serviceId);
            } catch (IllegalArgumentException e) {
                // Si serviceId no es un UUID válido, usar id como fallback
                return id;
            }
        }
        return id;
    }

    public String getServiceIdAsString() {
        if (serviceId != null) {
            return serviceId;
        }
        return id != null ? id.toString() : null;
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
     * ACTUALIZADO: categoryName puede ser null, usamos "General" como fallback
     * ACTUALIZADO: Considera tanto serviceId como id para la validación
     */
    public boolean isValidForCart() {
        boolean hasValidId = (serviceId != null && !serviceId.trim().isEmpty()) || id != null;
        return hasValidId &&
                title != null && !title.trim().isEmpty() &&
                price != null &&
                price.compareTo(BigDecimal.ZERO) >= 0;
        // Removido categoryName como requerido porque puede venir null desde Kafka
    }

    /**
     * Obtiene un nombre de categoría seguro (nunca null)
     */
    public String getSafeCategoryName() {
        return categoryName != null && !categoryName.trim().isEmpty()
                ? categoryName
                : "General";
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

    /**
     * Obtiene el userId de forma segura
     */
    public String getSafeUserId() {
        return userId != null ? userId : "unknown";
    }

    /**
     * Obtiene el eventType de forma segura
     */
    public String getSafeEventType() {
        return eventType != null ? eventType : "UNKNOWN";
    }

    /**
     * Obtiene el timestamp de forma segura
     */
    public String getSafeTimestamp() {
        return timestamp != null ? timestamp : "";
    }

    /**
     * Verifica si es un evento de creación
     */
    public boolean isCreatedEvent() {
        return "CREATED".equalsIgnoreCase(eventType);
    }

    /**
     * Verifica si es un evento de actualización
     */
    public boolean isUpdatedEvent() {
        return "UPDATED".equalsIgnoreCase(eventType);
    }

    /**
     * Verifica si es un evento de eliminación
     */
    public boolean isDeletedEvent() {
        return "DELETED".equalsIgnoreCase(eventType);
    }
}