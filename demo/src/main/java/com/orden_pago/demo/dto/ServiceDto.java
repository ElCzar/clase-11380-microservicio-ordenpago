package com.orden_pago.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ServiceDto {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private boolean available;
}