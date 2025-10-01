package com.orden_pago.demo.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orden_pago.demo.dto.ServiceResponseDTO;
import com.orden_pago.demo.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * ServiceKafkaConsumer - Consumidor Kafka para respuestas del microservicio
 * marketplace
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
     */
    @Bean
    public Consumer<Message<String>> serviceResponse() {
        return message -> {
            try {
                log.info("Recibida respuesta de servicio del marketplace");

                String rawMessage = message.getPayload();
                log.debug("Mensaje crudo recibido: {}", rawMessage);

                String jsonPayload = decodeMessage(rawMessage);
                log.debug("JSON decodificado: {}", jsonPayload);

                ServiceResponseDTO serviceResponse = objectMapper.readValue(jsonPayload, ServiceResponseDTO.class);

                log.info("Servicio deserializado exitosamente: ID={}, Name={}",
                        serviceResponse.getServiceId(), serviceResponse.getName());

                if (serviceResponse == null || serviceResponse.getServiceId() == null) {
                    log.warn("Respuesta de servicio inválida o vacía recibida");
                    return;
                }

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
     * Decodifica el mensaje detectando si es Base64 o necesita otra conversión
     */
    private String decodeMessage(String rawMessage) {
        try {
            if (rawMessage.trim().startsWith("{")) {
                log.debug("Mensaje detectado como JSON directo");
                return rawMessage;
            }

            // Trim the "" at end and start if present
            if (rawMessage.startsWith("\"") && rawMessage.endsWith("\"")) {
                rawMessage = rawMessage.substring(1, rawMessage.length() - 1);
            }
            log.debug("Intentando decodificar como Base64...");
            byte[] decoded = Base64.getDecoder().decode(rawMessage);
            String decodedStr = new String(decoded, StandardCharsets.UTF_8);

            if (decodedStr.trim().startsWith("{")) {
                log.debug("Base64 decodificado exitosamente a JSON");
                return decodedStr;
            } else {
                log.warn("Base64 decodificado pero no es JSON válido: {}", decodedStr);
                return rawMessage;
            }

        } catch (IllegalArgumentException e) {
            log.debug("No es Base64 válido, usando como String directo");
            return rawMessage;
        } catch (Exception e) {
            log.warn("Error en decodificación, usando mensaje original: {}", e.getMessage());
            return rawMessage;
        }
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
     */
    public void handleServiceResponseError(Exception error, ServiceResponseDTO serviceResponse) {
        log.error("Error crítico procesando respuesta de servicio: {}", error.getMessage(), error);

        if (serviceResponse != null) {
            log.error("Datos del servicio que causó el error: ServiceId={}, ServiceName={}",
                    serviceResponse.getServiceId(), serviceResponse.getName());
        }
    }
}