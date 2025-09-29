package com.orden_pago.demo.service;

import com.orden_pago.demo.dto.CartEventDTO;
import com.orden_pago.demo.dto.ServiceResponseDTO;
import com.orden_pago.demo.enums.CartStatus;
import com.orden_pago.demo.model.Cart;
import com.orden_pago.demo.model.CartItem;
import com.orden_pago.demo.repository.CartRepository;
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
    public Cart getCurrentCart(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Obteniendo carrito actual para usuario: {}", userId);

        Optional<Cart> existingCart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE);

        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        // Crear nuevo carrito si no existe uno activo
        Cart newCart = new Cart();
        newCart.setUserId(userId);
        newCart.setStatus(CartStatus.ACTIVE);
        newCart.setCreatedAt(LocalDateTime.now());
        newCart.setUpdatedAt(LocalDateTime.now());

        Cart savedCart = cartRepository.save(newCart);
        log.info("Nuevo carrito creado con ID: {}", savedCart.getId());

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
     * Método interno para agregar item después de validar el servicio
     */
    private CompletableFuture<CartItem> addItemToCartInternal(Cart cart, ServiceResponseDTO serviceResponse,
            Integer quantity, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            UUID serviceId = serviceResponse.getServiceId();

            // Verificar si el item ya existe en el carrito
            Optional<CartItem> existingItem = cartItemRepository.findByCartAndServiceId(cart, serviceId);

            CartItem item;
            if (existingItem.isPresent()) {
                // Actualizar cantidad del item existente
                item = existingItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                item = cartItemRepository.save(item);

                log.info("Cantidad actualizada para item existente. Nueva cantidad: {}", item.getQuantity());

                // Publicar evento de actualización
                kafkaMessagingService.publishCartEvent(
                        CartEventDTO.itemUpdated(cart.getId(), userId, serviceId, item.getServiceName(),
                                item.getQuantity(), item.getSubtotal()));
            } else {
                // Crear nuevo item con información completa del servicio
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setServiceId(serviceId);
                newItem.setServiceName(serviceResponse.getName());
                newItem.setServiceDescription(truncateDescription(serviceResponse.getDescription()));
                newItem.setServicePrice(serviceResponse.getPrice());
                newItem.setServiceCategory(serviceResponse.getCategoryName());
                newItem.setServiceImageUrl(serviceResponse.getPrimaryImageUrl());
                newItem.setAverageRating(serviceResponse.getAverageRating());
                newItem.setQuantity(quantity);
                newItem.setAddedAt(LocalDateTime.now());

                item = cartItemRepository.save(newItem);

                log.info("Nuevo item agregado al carrito con ID: {} - Servicio: {} ({})",
                        item.getId(), item.getServiceName(), item.getServiceCategory());

                // Publicar evento de item agregado con información completa
                kafkaMessagingService.publishCartEvent(
                        CartEventDTO.itemAdded(cart.getId(), userId, serviceId, item.getServiceName(),
                                item.getServiceCategory(), item.getServicePrice(), quantity, item.getSubtotal()));
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
        kafkaMessagingService.publishCartEvent(
                CartEventDTO.itemUpdated(cart.getId(), cart.getUserId(), item.getServiceId(),
                        item.getServiceName(), newQuantity, item.getSubtotal()));

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
        kafkaMessagingService.publishCartEvent(
                CartEventDTO.itemRemoved(cart.getId(), cart.getUserId(), serviceId, serviceName));

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
            kafkaMessagingService.publishCartEvent(
                    CartEventDTO.cartCleared(cart.getId(), userId));

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
     * Obtiene el historial de carritos del usuario
     */
    @Transactional(readOnly = true)
    public List<Cart> getUserCartHistory(Authentication authentication) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Obteniendo historial de carritos para usuario: {}", userId);

        return cartRepository.findByUserIdOrderByCreatedAtDesc(userId);
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
}