package com.orden_pago.demo.service;

import com.orden_pago.demo.dto.CartEventDTO;
import com.orden_pago.demo.dto.CartHistoryDTO;
import com.orden_pago.demo.dto.ServiceResponseDTO;
import com.orden_pago.demo.enums.CartStatus;
import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.CartItem;
import com.orden_pago.demo.repository.CartRepository;
import com.orden_pago.demo.service.kafka.KafkaMessagingService;
import com.orden_pago.demo.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final KafkaMessagingService kafkaMessagingService;

    /**
     * Obtiene el carrito actual del usuario
     */
    @Transactional
    public Cart getCurrentCart(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Obteniendo carrito actual para usuario: {}", userId);

        Optional<Cart> existingCart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE);

        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            log.debug("🛒 Carrito existente encontrado - ID: {} para usuario: {}", cart.getId(), userId);
            return cart;
        }

        // Crear nuevo carrito si no existe uno activo
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setStatus(CartStatus.ACTIVE);
        newCart.setCreatedAt(LocalDateTime.now());
        newCart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(newCart);

        // 🔍 VALIDACIÓN: Verificar que el carrito se guardó correctamente
        if (savedCart.getId() == null) {
            log.error("❌ Error crítico: Carrito no se pudo persistir para usuario: {}", userId);
            throw new RuntimeException("Error interno: No se pudo crear el carrito");
        }

        log.info("🆕 Nuevo carrito creado con ID: {} para usuario: {}", savedCart.getId(), userId);
        return savedCart;
    }

    /**
     * Agrega un item al carrito (comunicación asíncrona)
     */
    public CompletableFuture<CartItem> addItemToCartAsync(Authentication authentication, UUID serviceId,
            Integer quantity) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Agregando item al carrito para usuario: {}, servicio: {}, cantidad: {}",
                userId, serviceId, quantity);

        // Obtener carrito
        Cart cart = getCurrentCart(authentication);

        // Solicitar información del servicio a través de Kafka
        return kafkaMessagingService.requestServiceInfo(serviceId)
                .thenCompose(serviceResponse -> {

                    if (serviceResponse.getErrorMessage() != null) {
                        throw new RuntimeException(
                                "Error al obtener información del servicio: " + serviceResponse.getErrorMessage());
                    }

                    if (!serviceResponse.getAvailable()) {
                        throw new RuntimeException("Servicio no disponible: " + serviceId);
                    }

                    return addItemToCartInternal(cart, serviceResponse, quantity, userId);
                });

    }

    /**
     * Agrega un item al carrito
     */
    public CartItem addItemToCart(Authentication authentication, UUID serviceId, Integer quantity) {
        try {
            return addItemToCartAsync(authentication, serviceId, quantity).get();
        } catch (Exception e) {
            log.error("Error agregando item al carrito: {}", e.getMessage());
            throw new RuntimeException("Error agregando item al carrito: " + e.getMessage());
        }
    }

    /**
     * Agrega un item al carrito con información del servicio ya disponible (SIN
     * KAFKA)
     * Método para pruebas que bypasea la comunicación con marketplace
     */
    @Transactional
    public CartItem addItemToCartWithServiceInfo(Authentication authentication, ServiceResponseDTO serviceResponse,
            Integer quantity) {
        try {
            String userId = getUserIdFromAuth(authentication);
            log.info("🧪 MODO PRUEBA: Agregando item directamente con serviceInfo: {}", serviceResponse.getTitle());

            // 🔍 VALIDACIÓN: Verificar que el serviceResponse sea válido para el carrito
            if (!serviceResponse.isValidForCart()) {
                String error = String.format(
                        "ServiceResponse inválido para carrito - ID: %s, Title: %s, Price: %s, Category: %s",
                        serviceResponse.getServiceId(),
                        serviceResponse.getName(),
                        serviceResponse.getPrice(),
                        serviceResponse.getCategoryName());
                log.error("❌ {}", error);
                throw new IllegalArgumentException(error);
            }

            // Obtener carrito actual y asegurar que esté persistido
            Cart cart = getCurrentCart(authentication);

            // 🔍 VALIDACIÓN: Verificar que el carrito tenga un ID válido
            if (cart.getId() == null) {
                log.error("❌ Carrito sin ID válido para usuario: {}", userId);
                throw new RuntimeException("Error interno: carrito sin ID válido");
            }

            log.debug("🛒 Usando carrito ID: {} para usuario: {}", cart.getId(), userId);

            // Procesar directamente en el contexto transaccional actual
            return addItemToCartInternalSync(cart, serviceResponse, quantity, userId);
        } catch (Exception e) {
            log.error("❌ Error agregando item con serviceInfo: {}", e.getMessage());
            throw new RuntimeException("Error agregando item al carrito: " + e.getMessage());
        }
    }

    /**
     * Versión síncrona del método interno para agregar items al carrito
     * Usado por addItemToCartWithServiceInfo para evitar problemas de
     * transaccionalidad
     */
    private CartItem addItemToCartInternalSync(Cart cart, ServiceResponseDTO serviceResponse, Integer quantity,
            String userId) {
        UUID serviceId = serviceResponse.getServiceId();
        log.debug("🔄 Procesando item para carrito - ServiceId: {}, Quantity: {}", serviceId, quantity);

        // Verificar si el item ya existe en el carrito
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndServiceId(cart, serviceId);

        CartItem item;
        if (existingItem.isPresent()) {
            // Actualizar cantidad del item existente
            item = existingItem.get();
            Integer newQuantity = item.getQuantity() + quantity;
            item.setQuantity(newQuantity);
            item = cartItemRepository.save(item);

            log.info("✅ Cantidad actualizada para item existente '{}'. Nueva cantidad: {}",
                    item.getServiceName(), item.getQuantity());

        } else {
            // 🆕 Crear nuevo item con información completa del servicio usando métodos
            // seguros
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setServiceId(serviceId);
            newItem.setServiceName(serviceResponse.getName()); // title
            newItem.setServiceDescription(truncateDescription(serviceResponse.getSafeDescription()));
            newItem.setServicePrice(serviceResponse.getPrice());
            newItem.setServiceCategory(serviceResponse.getSafeCategoryName()); // Uso método seguro
            newItem.setServiceImageUrl(serviceResponse.getSafePrimaryImageUrl());
            newItem.setAverageRating(serviceResponse.getSafeAverageRating());
            newItem.setQuantity(quantity);
            newItem.setAddedAt(LocalDateTime.now());

            // 💾 Guardar en base de datos
            item = cartItemRepository.save(newItem);

            log.info("🆕 Nuevo item agregado al carrito - ID: {}, Servicio: '{}' ({}), Precio: ${}, Cantidad: {}",
                    item.getId(),
                    item.getServiceName(),
                    item.getServiceCategory(),
                    item.getServicePrice(),
                    item.getQuantity());
        }

        // Actualizar timestamp del carrito
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return item;
    }

    /**
     * Agrega item después de validar el servicio
     */
    private CompletableFuture<CartItem> addItemToCartInternal(Cart cart, ServiceResponseDTO serviceResponse,
            Integer quantity, String userId) {
        return CompletableFuture.supplyAsync(() -> {

            // 🔍 VALIDACIÓN: Verificar que el serviceResponse sea válido para el carrito
            if (!serviceResponse.isValidForCart()) {
                String error = String.format(
                        "ServiceResponse inválido para carrito - ID: %s, Title: %s, Price: %s, Category: %s",
                        serviceResponse.getServiceId(),
                        serviceResponse.getName(),
                        serviceResponse.getPrice(),
                        serviceResponse.getCategoryName());
                log.error("❌ {}", error);
                throw new RuntimeException(error);
            }

            UUID serviceId = serviceResponse.getServiceId();
            log.debug("🔄 Procesando item para carrito - ServiceId: {}, Quantity: {}", serviceId, quantity);

            // Verificar si el item ya existe en el carrito
            Optional<CartItem> existingItem = cartItemRepository.findByCartAndServiceId(cart, serviceId);

            CartItem item;
            if (existingItem.isPresent()) {
                // Actualizar cantidad del item existente
                item = existingItem.get();
                Integer newQuantity = item.getQuantity() + quantity;
                item.setQuantity(newQuantity);
                item = cartItemRepository.save(item);

                log.info("✅ Cantidad actualizada para item existente '{}'. Nueva cantidad: {}",
                        item.getServiceName(), item.getQuantity());

                // Publicar evento de actualización
                // kafkaMessagingService.publishCartEvent(
                // CartEventDTO.itemUpdated(cart.getId(), userId, serviceId,
                // item.getServiceName(),
                // item.getQuantity(), item.getSubtotal()));
            } else {
                // 🆕 Crear nuevo item con información completa del servicio usando métodos
                // seguros
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setServiceId(serviceId);
                newItem.setServiceName(serviceResponse.getName()); // title
                newItem.setServiceDescription(truncateDescription(serviceResponse.getSafeDescription()));
                newItem.setServicePrice(serviceResponse.getPrice());
                newItem.setServiceCategory(serviceResponse.getSafeCategoryName()); // Uso método seguro
                newItem.setServiceImageUrl(serviceResponse.getSafePrimaryImageUrl());
                newItem.setAverageRating(serviceResponse.getSafeAverageRating());
                newItem.setQuantity(quantity);
                newItem.setAddedAt(LocalDateTime.now());

                // 💾 Guardar en base de datos
                item = cartItemRepository.save(newItem);

                log.info("🆕 Nuevo item agregado al carrito - ID: {}, Servicio: '{}' ({}), Precio: ${}, Cantidad: {}",
                        item.getId(),
                        item.getServiceName(),
                        item.getServiceCategory(),
                        item.getServicePrice(),
                        item.getQuantity());

                // Publicar evento de item agregado con información completa
                // kafkaMessagingService.publishCartEvent(
                // CartEventDTO.itemAdded(cart.getId(), userId, serviceId,
                // item.getServiceName(),
                // item.getServiceCategory(), item.getServicePrice(), quantity,
                // item.getSubtotal()));
            }

            // Actualizar timestamp del carrito
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            return item;
        });
    }

    /**
     * Actualiza la cantidad de un item
     */
    public CartItem updateItemQuantity(UUID itemId, Integer newQuantity) {
        log.info("Actualizando cantidad del item: {} a: {}", itemId, newQuantity);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado: " + itemId));

        item.setQuantity(newQuantity);
        CartItem updatedItem = cartItemRepository.save(item);

        // Actualizar timestamp del carrito
        Cart cart = item.getCart();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Publicar evento de actualización
        // kafkaMessagingService.publishCartEvent(
        // CartEventDTO.itemUpdated(cart.getId(), cart.getUserId(), item.getServiceId(),
        // item.getServiceName(), newQuantity, item.getSubtotal()));

        log.info("Cantidad del item actualizada exitosamente");
        return updatedItem;
    }

    /**
     * Remueve un item del carrito
     */
    public void removeItemFromCart(UUID itemId) {
        log.info("Removiendo item del carrito: {}", itemId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item no encontrado: " + itemId));

        Cart cart = item.getCart();
        UUID serviceId = item.getServiceId();
        String serviceName = item.getServiceName();

        cartItemRepository.delete(item);

        // Actualizar timestamp del carrito
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        // Publicar evento de item removido
        // kafkaMessagingService.publishCartEvent(
        // CartEventDTO.itemRemoved(cart.getId(), cart.getUserId(), serviceId,
        // serviceName));

        log.info("Item removido del carrito exitosamente");
    }

    /**
     * Vacía completamente el carrito
     */
    public void clearCart(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Vaciando carrito para usuario: {}", userId);

        Optional<Cart> cartOpt = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cartItemRepository.deleteByCart(cart);

            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            // Publicar evento de carrito vaciado
            // kafkaMessagingService.publishCartEvent(
            // CartEventDTO.cartCleared(cart.getId(), userId));

            log.info("Carrito vaciado exitosamente");
        }
    }

    /**
     * Marca un carrito como completado
     */
    public void completeCart(Cart cart) {
        log.info("Marcando carrito como completado: {}", cart.getId());

        cart.setStatus(CartStatus.COMPLETED);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Carrito marcado como completado");
    }

    /**
     * Obtiene el historial de carritos del usuario como DTOs
     */
    @Transactional(readOnly = true)
    public List<CartHistoryDTO> getUserCartHistory(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Obteniendo historial de carritos para usuario: {}", userId);

        List<Cart> carts = cartRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return carts.stream()
                .map(this::convertToCartHistoryDTO)
                .toList();
    }

    /**
     * Valida si un carrito pertenece al usuario autenticado
     */
    public boolean validateCartOwnership(UUID cartId, Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);

        Optional<Cart> cart = cartRepository.findById(cartId);
        return cart.isPresent() && cart.get().getUserId().equals(userId);
    }

    /**
     * Extrae el user ID del token JWT
     */
    private String getUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("sub"); // o el claim que contenga el user ID
        }
        throw new RuntimeException("No se pudo obtener el user ID del token");
    }

    /**
     * Trunca la descripción del servicio para el carrito (máximo 1000 caracteres)
     */
    private String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() <= 1000) {
            return description;
        }
        return description.substring(0, 997) + "...";
    }

    /**
     * Actualiza la información de un servicio en los items del carrito
     * Se llama cuando se recibe una respuesta del microservicio marketplace via
     * Kafka
     * 
     * @param serviceResponse Información del servicio recibida del marketplace
     */
    public void updateServiceInfo(ServiceResponseDTO serviceResponse) {
        try {
            log.info("Actualizando información del servicio {} en carritos activos",
                    serviceResponse.getServiceId());

            // Buscar todos los items del carrito que contengan este servicio
            List<CartItem> allItems = cartItemRepository
                    .findByServiceId(serviceResponse.getServiceId());

            // Filtrar solo items de carritos activos
            List<CartItem> itemsToUpdate = allItems.stream()
                    .filter(item -> item.getCart().getStatus() == CartStatus.ACTIVE)
                    .toList();

            if (itemsToUpdate.isEmpty()) {
                log.debug("No se encontraron items activos para el servicio {}",
                        serviceResponse.getServiceId());
                return;
            }

            // Actualizar información en cada item
            for (CartItem item : itemsToUpdate) {
                updateCartItemWithServiceInfo(item, serviceResponse);
            }

            // Guardar todos los cambios
            cartItemRepository.saveAll(itemsToUpdate);

            log.info("Actualizada información del servicio {} en {} items del carrito",
                    serviceResponse.getServiceId(), itemsToUpdate.size());

        } catch (Exception e) {
            log.error("Error actualizando información del servicio {}: {}",
                    serviceResponse.getServiceId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Actualiza un item específico del carrito con la información del servicio
     */
    private void updateCartItemWithServiceInfo(CartItem item, ServiceResponseDTO serviceResponse) {
        // Actualizar campos de información del servicio
        item.setServiceName(serviceResponse.getName());
        item.setServiceDescription(truncateDescription(serviceResponse.getDescription()));
        item.setServicePrice(serviceResponse.getPrice());
        item.setAverageRating(serviceResponse.getAverageRating());
        item.setServiceCategory(serviceResponse.getSafeCategoryName()); // Uso método seguro
        item.setServiceImageUrl(serviceResponse.getPrimaryImageUrl());

        log.debug("Actualizado item del carrito {} con información del servicio {}",
                item.getId(), serviceResponse.getServiceId());
    }

    /**
     * Convierte una entidad Cart a CartHistoryDTO
     */
    private CartHistoryDTO convertToCartHistoryDTO(Cart cart) {
        List<CartHistoryDTO.CartItemSummary> itemSummaries = cart.getItems().stream()
                .map(this::convertToCartItemSummary)
                .toList();

        return CartHistoryDTO.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .totalAmount(cart.getTotalAmount())
                .totalItems(cart.getTotalItems())
                .items(itemSummaries)
                .build();
    }

    /**
     * Convierte un CartItem a CartItemSummary
     */
    private CartHistoryDTO.CartItemSummary convertToCartItemSummary(CartItem item) {
        return CartHistoryDTO.CartItemSummary.builder()
                .id(item.getId())
                .serviceId(item.getServiceId())
                .serviceName(item.getServiceName())
                .serviceCategory(item.getServiceCategory())
                .servicePrice(item.getServicePrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}