package com.orden_pago.demo.client;

import com.orden_pago.demo.dto.ServiceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ServiceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${services.microservice.url:http://localhost:8083}")
    private String servicesBaseUrl;

    /**
     * Obtiene un servicio por su ID desde el microservicio de servicios
     */
    public Optional<ServiceDto> getServiceById(UUID serviceId) {
        try {
            String url = servicesBaseUrl + "/api/v1/services/" + serviceId;
            log.info("Consultando servicio con ID: {} en URL: {}", serviceId, url);

            ServiceDto service = restTemplate.getForObject(url, ServiceDto.class);

            if (service != null) {
                log.info("Servicio encontrado: {}", service.getName());
                return Optional.of(service);
            } else {
                log.warn("Servicio con ID {} no encontrado", serviceId);
                return Optional.empty();
            }
        } catch (RestClientException e) {
            log.error("Error al consultar servicio con ID {}: {}", serviceId, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error inesperado al consultar servicio con ID {}: {}", serviceId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Verifica si un servicio está disponible
     */
    public boolean isServiceAvailable(UUID serviceId) {
        Optional<ServiceDto> service = getServiceById(serviceId);
        return service.isPresent() && service.get().isAvailable();
    }

    /**
     * Valida si un servicio existe y está disponible
     */
    public boolean validateService(UUID serviceId) {
        try {
            Optional<ServiceDto> service = getServiceById(serviceId);
            if (service.isEmpty()) {
                log.warn("Servicio con ID {} no existe", serviceId);
                return false;
            }

            if (!service.get().isAvailable()) {
                log.warn("Servicio con ID {} no está disponible", serviceId);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error al validar servicio con ID {}: {}", serviceId, e.getMessage());
            return false;
        }
    }
}