package com.orden_pago.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO para solicitar información de servicios vía Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {
    private UUID serviceId;
    private String requestId; // UUID para correlacionar request/response
    private String requesterService; // nombre del servicio que hace la solicitud
}