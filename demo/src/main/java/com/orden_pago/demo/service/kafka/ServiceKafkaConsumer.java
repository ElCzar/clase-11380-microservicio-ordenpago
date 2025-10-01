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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // Static list to store services received from Kafka
    private static final Map<UUID, ServiceResponseDTO> AVAILABLE_SERVICES = new ConcurrentHashMap<>();
    private static final List<ServiceResponseDTO> SERVICES_LIST = new CopyOnWriteArrayList<>();

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

            // Guardar el servicio en la lista estática
            saveServiceToStaticList(serviceResponse);

            // Integrar con CartService para actualizar información del item (mantener funcionalidad existente)
            cartService.updateServiceInfo(serviceResponse);

            log.info("Información del servicio {} actualizada en el carrito y guardada en lista estática",
                    serviceResponse.getServiceId());

        } catch (Exception e) {
            log.error("Error al procesar información del servicio {}: {}",
                    serviceResponse.getServiceId(), e.getMessage(), e);
            throw e; // Re-lanzar para que sea manejado por el consumer principal
        }
    }

    /**
     * Guarda un servicio en la lista estática de servicios disponibles
     */
    private void saveServiceToStaticList(ServiceResponseDTO serviceResponse) {
        try {
            UUID serviceId = serviceResponse.getServiceId();
            
            // Guardar en el Map para acceso rápido por ID
            AVAILABLE_SERVICES.put(serviceId, serviceResponse);
            
            // Actualizar la lista, removiendo duplicados si existen
            SERVICES_LIST.removeIf(existing -> 
                existing.getServiceId().equals(serviceResponse.getServiceId()));
            SERVICES_LIST.add(serviceResponse);
            
            log.info("✅ Servicio {} guardado en lista estática. Total servicios: {}", 
                    serviceId, AVAILABLE_SERVICES.size());
                    
        } catch (Exception e) {
            log.error("❌ Error guardando servicio en lista estática: {}", e.getMessage());
        }
    }

    /**
     * Obtiene un servicio específico de la lista estática por ID
     */
    public static ServiceResponseDTO getServiceById(UUID serviceId) {
        ServiceResponseDTO service = AVAILABLE_SERVICES.get(serviceId);
        if (service != null) {
            log.debug("✅ Servicio {} encontrado en lista estática", serviceId);
        } else {
            log.debug("❌ Servicio {} no encontrado en lista estática", serviceId);
        }
        return service;
    }

    /**
     * Obtiene todos los servicios disponibles de la lista estática
     */
    public static List<ServiceResponseDTO> getAllAvailableServices() {
        log.info("📋 Obteniendo {} servicios desde lista estática", SERVICES_LIST.size());
        return List.copyOf(SERVICES_LIST); // Retornar copia inmutable
    }

    /**
     * Verifica si un servicio existe en la lista estática
     */
    public static boolean isServiceAvailable(UUID serviceId) {
        boolean available = AVAILABLE_SERVICES.containsKey(serviceId);
        log.debug("🔍 Servicio {} disponible en lista estática: {}", serviceId, available);
        return available;
    }

    /**
     * Obtiene el tamaño de la lista de servicios disponibles
     */
    public static int getAvailableServicesCount() {
        return AVAILABLE_SERVICES.size();
    }

    /**
     * Limpia la lista estática de servicios (para testing)
     */
    public static void clearServicesList() {
        AVAILABLE_SERVICES.clear();
        SERVICES_LIST.clear();
        log.info("🧹 Lista estática de servicios limpiada");
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