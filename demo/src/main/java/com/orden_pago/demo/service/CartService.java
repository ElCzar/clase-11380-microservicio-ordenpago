package com.orden_pago.demo.service;

import com.orden_pago.demo.client.ServiceClient;
import com.orden_pago.demo.dto.ServiceDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ServiceClient serviceClient;

    /**
     * Obtiene el carrito actual del usuario (activo)
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
     * Agrega un item al carrito
     */
    public CartItem addItemToCart(Authentication authentication, UUID serviceId, Integer quantity) {
        String userId = getUserIdFromAuth(authentication);
        log.info("Agregando item al carrito para usuario: {}, servicio: {}, cantidad: {}",
                userId, serviceId, quantity);

        // Obtener o crear carrito
        Cart cart = getCurrentCart(authentication);

        // Validar que el servicio existe y está disponible
        Optional<ServiceDto> serviceDto = serviceClient.getServiceById(serviceId);
        if (serviceDto.isEmpty()) {
            throw new RuntimeException("Servicio no encontrado: " + serviceId);
        }

        if (!serviceDto.get().isAvailable()) {
            throw new RuntimeException("Servicio no disponible: " + serviceId);
        }

        // Verificar si el item ya existe en el carrito
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndServiceId(cart, serviceId);

        if (existingItem.isPresent()) {
            // Actualizar cantidad del item existente
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item = cartItemRepository.save(item);

            log.info("Cantidad actualizada para item existente. Nueva cantidad: {}", item.getQuantity());
            return item;
        } else {
            // Crear nuevo item
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setServiceId(serviceId);
            newItem.setServiceName(serviceDto.get().getName());
            newItem.setServicePrice(serviceDto.get().getPrice());
            newItem.setQuantity(quantity);
            newItem.setAddedAt(LocalDateTime.now());

            CartItem savedItem = cartItemRepository.save(newItem);

            // Actualizar timestamp del carrito
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);

            log.info("Nuevo item agregado al carrito con ID: {}", savedItem.getId());
            return savedItem;
        }
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
        cartItemRepository.delete(item);

        // Actualizar timestamp del carrito
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

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
}