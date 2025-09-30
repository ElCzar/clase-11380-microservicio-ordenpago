package com.orden_pago.demo.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orden_pago.demo.dto.ServiceResponseDTO;
import com.orden_pago.demo.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * ServiceKafkaConsumer - Consumidor Kafka para respuestas del microservicio
 * marketplace
 * 
 * Este servicio recibe respuestas del microservicio de marketplace (servicios)
 * a través del topic 'service-response-topic' cuando se solicita información
 * de servicios para el carrito de compras.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceKafkaConsumer {

    private final CartService cartService;
    private final KafkaMessagingService kafkaMessagingService; 
    private final ObjectMapper objectMapper;

    /**
     * Consumidor para respuestas de servicios del marketplace
     * Binding: serviceResponse-in-0 -> service-response-topic
     */
    @Bean
    public Consumer<Message<ServiceResponseDTO>> serviceResponse() {
        return message -> {
            try {
                log.info("Recibida respuesta de servicio del marketplace");

                ServiceResponseDTO serviceResponse = message.getPayload();

                // Log de la información recibida
                log.debug("Información del servicio recibida: {}",
                        objectMapper.writeValueAsString(serviceResponse));

                // Validar que la respuesta contiene información válida
                if (serviceResponse == null || serviceResponse.getServiceId() == null) {
                    log.warn("Respuesta de servicio inválida o vacía recibida");
                    return;
                }

                // Procesar la respuesta del servicio
                processServiceResponse(serviceResponse);

                if (serviceResponse.getRequestId() != null) {
                    kafkaMessagingService.handleServiceResponse(serviceResponse);
                }

                log.info("Respuesta de servicio procesada exitosamente. ServiceId: {}",
                        serviceResponse.getServiceId());

            } catch (Exception e) {
                log.error("Error procesando respuesta de servicio del marketplace: {}",
                        e.getMessage(), e);
            }
        };
    }

    /**
     * Procesa la respuesta del servicio recibida del marketplace
     * 
     * @param serviceResponse Respuesta del servicio con toda la información
     */
    private void processServiceResponse(ServiceResponseDTO serviceResponse) {
        try {
            log.debug("Procesando información del servicio: {} - {}",
                    serviceResponse.getServiceId(), serviceResponse.getName());

            if (!serviceResponse.isAvailable()) {
                log.warn("Servicio {} no está activo, ignorando respuesta",
                        serviceResponse.getServiceId());
                return;
            }

            // Integrar con CartService para actualizar información del item
            cartService.updateServiceInfo(serviceResponse);

            log.info("Información del servicio {} actualizada en el carrito",
                    serviceResponse.getServiceId());

        } catch (Exception e) {
            log.error("Error al procesar información del servicio {}: {}",
                    serviceResponse.getServiceId(), e.getMessage(), e);
            throw e; // Re-lanzar para que sea manejado por el consumer principal
        }
    }

    /**
     * Maneja errores en el procesamiento de mensajes
     * Puede ser extendido para implementar lógica de retry o dead letter queue
     */
    public void handleServiceResponseError(Exception error, ServiceResponseDTO serviceResponse) {
        log.error("Error crítico procesando respuesta de servicio: {}", error.getMessage(), error);

        if (serviceResponse != null) {
            log.error("Datos del servicio que causó el error: ServiceId={}, ServiceName={}",
                    serviceResponse.getServiceId(), serviceResponse.getName());
        }
    }
}