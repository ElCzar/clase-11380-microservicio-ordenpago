package com.orden_pago.demo.service;

import com.orden_pago.demo.dto.ServiceResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Configuración de consumers para Kafka
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaMessagingService kafkaMessagingService;

    /**
     * Consumer para respuestas de servicios
     */
    @Bean
    public Consumer<ServiceResponseDTO> serviceResponse() {
        return response -> {
            try {
                log.info("Recibida respuesta de servicio: requestId={}, serviceId={}",
                        response.getRequestId(), response.getServiceId());
                kafkaMessagingService.handleServiceResponse(response);
            } catch (Exception e) {
                log.error("Error procesando respuesta de servicio: {}", response, e);
            }
        };
    }
}