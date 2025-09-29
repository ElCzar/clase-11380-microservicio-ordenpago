package com.orden_pago.demo.service;

import com.orden_pago.demo.dto.CartEventDTO;
import com.orden_pago.demo.dto.PaymentEventDTO;
import com.orden_pago.demo.dto.ServiceRequestDTO;
import com.orden_pago.demo.dto.ServiceResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para manejar mensajería Kafka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMessagingService {

    private final StreamBridge streamBridge;

    // Mapa para correlacionar requests/responses asíncronos
    private final Map<String, CompletableFuture<ServiceResponseDTO>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Solicita información de un servicio vía Kafka
     */
    public CompletableFuture<ServiceResponseDTO> requestServiceInfo(UUID serviceId) {
        String requestId = UUID.randomUUID().toString();
        ServiceRequestDTO request = new ServiceRequestDTO(serviceId, requestId, "carrito-compras-service");

        // Crear el CompletableFuture que se resolverá cuando llegue la respuesta
        CompletableFuture<ServiceResponseDTO> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        // Configurar timeout de 10 segundos
        future.orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    pendingRequests.remove(requestId);
                    log.error("Timeout en solicitud de servicio {}: {}", serviceId, throwable.getMessage());
                    ServiceResponseDTO errorResponse = new ServiceResponseDTO();
                    errorResponse.setRequestId(requestId);
                    errorResponse.setErrorMessage("Timeout en solicitud de servicio");
                    return errorResponse;
                });

        // Enviar el mensaje
        boolean sent = streamBridge.send("serviceRequest-out-0", request);
        if (!sent) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(new RuntimeException("No se pudo enviar la solicitud de servicio"));
        } else {
            log.info("Solicitud enviada para servicio {} con requestId {}", serviceId, requestId);
        }

        return future;
    }

    /**
     * Maneja la respuesta de un servicio (llamado por el consumer)
     */
    public void handleServiceResponse(ServiceResponseDTO response) {
        String requestId = response.getRequestId();
        CompletableFuture<ServiceResponseDTO> future = pendingRequests.remove(requestId);

        if (future != null) {
            if (response.getErrorMessage() != null) {
                log.error("Error en respuesta de servicio {}: {}", response.getServiceId(), response.getErrorMessage());
            } else {
                log.info("Respuesta recibida para servicio {} con requestId {}", response.getServiceId(), requestId);
            }
            future.complete(response);
        } else {
            log.warn("Respuesta recibida para requestId {} pero no hay solicitud pendiente", requestId);
        }
    }

    /**
     * Publica evento de carrito
     */
    public void publishCartEvent(CartEventDTO event) {
        boolean sent = streamBridge.send("cartEvent-out-0", event);
        if (sent) {
            log.info("Evento de carrito publicado: {} para carrito {}", event.getEventType(), event.getCartId());
        } else {
            log.error("Error al publicar evento de carrito: {}", event);
        }
    }

    /**
     * Publica evento de pago
     */
    public void publishPaymentEvent(PaymentEventDTO event) {
        boolean sent = streamBridge.send("paymentEvent-out-0", event);
        if (sent) {
            log.info("Evento de pago publicado: {} para pago {}", event.getEventType(), event.getPaymentId());
        } else {
            log.error("Error al publicar evento de pago: {}", event);
        }
    }
}